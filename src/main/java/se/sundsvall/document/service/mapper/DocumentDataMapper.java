package se.sundsvall.document.service.mapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.multipart.MultipartFile;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.document.api.model.DocumentData;
import se.sundsvall.document.api.model.DocumentFiles;
import se.sundsvall.document.integration.db.DocumentDataRepository;
import se.sundsvall.document.integration.db.model.DocumentDataEntity;
import se.sundsvall.document.service.extraction.ExtractionStatus;
import se.sundsvall.document.service.extraction.TextExtractor;
import se.sundsvall.document.service.storage.BinaryStore;
import se.sundsvall.document.service.storage.PutResult;
import se.sundsvall.document.service.storage.StorageRef;

import static java.util.stream.Collectors.toCollection;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

/**
 * File/attachment mapping: uploads (storage + text extraction + dedup), copy-on-write, and
 * DocumentDataEntity → DocumentData response mapping. Kept separate from {@link DocumentMapper}
 * because the upload path carries real I/O concerns (BinaryStore, Tika) that the rest of the
 * document mapping does not.
 */
public final class DocumentDataMapper {

	private static final Logger LOGGER = LoggerFactory.getLogger(DocumentDataMapper.class);

	private DocumentDataMapper() {}

	public static List<DocumentDataEntity> toDocumentDataEntities(final DocumentFiles documentFiles, final BinaryStore binaryStore,
		final TextExtractor textExtractor, final DocumentDataRepository documentDataRepository, final String municipalityId) {
		return Optional.ofNullable(documentFiles).map(DocumentFiles::getFiles)
			.map(files -> files.stream()
				.map(file -> toDocumentDataEntity(file, binaryStore, textExtractor, documentDataRepository, municipalityId))
				.toList())
			.orElse(null);
	}

	public static DocumentDataEntity toDocumentDataEntity(MultipartFile multipartFile, BinaryStore binaryStore,
		TextExtractor textExtractor, DocumentDataRepository documentDataRepository, String municipalityId) {
		if (multipartFile == null) {
			return null;
		}
		final var putResult = writeAndHash(multipartFile, binaryStore, municipalityId);

		// Dedupe: if the same bytes have already been extracted successfully on a previous upload
		// (e.g. same file attached to a new revision) reuse that text instead of running Tika again.
		final var dedup = documentDataRepository.findFirstByContentHashAndExtractionStatus(putResult.sha256(), ExtractionStatus.SUCCESS);

		final String extractedText;
		final ExtractionStatus extractionStatus;
		if (dedup.isPresent()) {
			extractedText = dedup.get().getExtractedText();
			extractionStatus = dedup.get().getExtractionStatus();
			LOGGER.debug("Text-extraction dedup hit (fileName='{}', contentHash='{}', size={}B) — reused from existing record",
				multipartFile.getOriginalFilename(), putResult.sha256(), multipartFile.getSize());
		} else {
			LOGGER.debug("Text-extraction dedup miss (fileName='{}', contentHash='{}', size={}B) — running Tika",
				multipartFile.getOriginalFilename(), putResult.sha256(), multipartFile.getSize());
			final var extraction = runExtraction(multipartFile, textExtractor);
			extractedText = extraction.text();
			extractionStatus = extraction.status();
		}

		return DocumentDataEntity.create()
			.withStorageLocator(putResult.ref().locator())
			.withMimeType(multipartFile.getContentType())
			.withFileName(multipartFile.getOriginalFilename())
			.withFileSizeInBytes(multipartFile.getSize())
			.withContentHash(putResult.sha256())
			.withExtractedText(extractedText)
			.withExtractionStatus(extractionStatus);
	}

	public static DocumentDataEntity copyDocumentDataEntity(DocumentDataEntity documentDataEntity, BinaryStore binaryStore) {
		if (documentDataEntity == null) {
			return null;
		}
		final var sourceRef = StorageRef.s3(documentDataEntity.getStorageLocator());
		final var newRef = binaryStore.copy(sourceRef);
		// Copy-on-write preserves the bytes → the content hash, extracted text and extraction status
		// are identical on the new revision. Don't re-run Tika.
		return DocumentDataEntity.create()
			.withMimeType(documentDataEntity.getMimeType())
			.withFileName(documentDataEntity.getFileName())
			.withFileSizeInBytes(documentDataEntity.getFileSizeInBytes())
			.withStorageLocator(newRef.locator())
			.withContentHash(documentDataEntity.getContentHash())
			.withExtractedText(documentDataEntity.getExtractedText())
			.withExtractionStatus(documentDataEntity.getExtractionStatus());
	}

	public static List<DocumentDataEntity> copyDocumentDataEntities(List<DocumentDataEntity> documentDataEntityList, BinaryStore binaryStore) {
		return Optional.ofNullable(documentDataEntityList)
			.map(list -> list.stream()
				.map(d -> copyDocumentDataEntity(d, binaryStore))
				.collect(toCollection(ArrayList::new)))
			.orElse(null);
	}

	public static List<DocumentData> toDocumentDataList(List<DocumentDataEntity> documentDataEntityList) {
		return Optional.ofNullable(documentDataEntityList)
			.map(list -> list.stream()
				.map(DocumentDataMapper::toDocumentData)
				.collect(toCollection(ArrayList::new)))
			.orElse(null);
	}

	public static DocumentData toDocumentData(DocumentDataEntity documentDataEntity) {
		return Optional.ofNullable(documentDataEntity)
			.map(docDataEntity -> DocumentData.create()
				.withFileName(docDataEntity.getFileName())
				.withFileSizeInBytes(docDataEntity.getFileSizeInBytes())
				.withId(docDataEntity.getId())
				.withMimeType(docDataEntity.getMimeType()))
			.orElse(null);
	}

	private static PutResult writeAndHash(MultipartFile multipartFile, BinaryStore binaryStore, String municipalityId) {
		try {
			return binaryStore.put(
				multipartFile.getInputStream(),
				multipartFile.getSize(),
				multipartFile.getContentType(),
				buildUserMetadata(multipartFile.getOriginalFilename(), municipalityId));
		} catch (final IOException e) {
			throw Problem.valueOf(INTERNAL_SERVER_ERROR, "Failed to read upload for " + multipartFile.getOriginalFilename() + ": " + e.getMessage());
		}
	}

	private static TextExtractor.ExtractedText runExtraction(MultipartFile multipartFile, TextExtractor textExtractor) {
		try (var stream = multipartFile.getInputStream()) {
			return textExtractor.extract(stream, multipartFile.getContentType(), multipartFile.getSize());
		} catch (final IOException e) {
			// Re-reading a Spring StandardMultipartFile gives a fresh temp-file stream, so this is
			// unexpected. Fail the extraction but keep the upload — the file is already persisted.
			return TextExtractor.ExtractedText.failed(multipartFile.getContentType(), "Failed to reopen upload for extraction: " + e.getMessage());
		}
	}

	private static Map<String, String> buildUserMetadata(String originalFilename, String municipalityId) {
		final var metadata = new LinkedHashMap<String, String>();
		if (originalFilename != null && !originalFilename.isBlank()) {
			metadata.put("original-filename", originalFilename);
		}
		if (municipalityId != null && !municipalityId.isBlank()) {
			metadata.put("municipality-id", municipalityId);
		}
		return metadata;
	}
}
