package se.sundsvall.document.service.mapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.web.multipart.MultipartFile;
import se.sundsvall.dept44.models.api.paging.PagingMetaData;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.document.api.model.Confidentiality;
import se.sundsvall.document.api.model.ConfidentialityUpdateRequest;
import se.sundsvall.document.api.model.Document;
import se.sundsvall.document.api.model.DocumentCreateRequest;
import se.sundsvall.document.api.model.DocumentData;
import se.sundsvall.document.api.model.DocumentFiles;
import se.sundsvall.document.api.model.DocumentMetadata;
import se.sundsvall.document.api.model.DocumentResponsibility;
import se.sundsvall.document.api.model.DocumentUpdateRequest;
import se.sundsvall.document.api.model.PagedDocumentResponse;
import se.sundsvall.document.integration.db.model.ConfidentialityEmbeddable;
import se.sundsvall.document.integration.db.model.DocumentDataEntity;
import se.sundsvall.document.integration.db.model.DocumentEntity;
import se.sundsvall.document.integration.db.model.DocumentMetadataEmbeddable;
import se.sundsvall.document.integration.db.model.DocumentResponsibilityEntity;
import se.sundsvall.document.service.storage.BinaryStore;
import se.sundsvall.document.service.storage.StorageRef;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toCollection;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static se.sundsvall.document.service.InclusionFilter.CONFIDENTIAL_AND_PUBLIC;
import static se.sundsvall.document.service.InclusionFilter.PUBLIC;

public class DocumentMapper {

	private DocumentMapper() {}

	/**
	 * API to Database mappings.
	 */

	public static DocumentEntity toDocumentEntity(DocumentCreateRequest documentCreateRequest, String municipalityId) {
		return DocumentEntity.create()
			.withArchive(documentCreateRequest.isArchive())
			.withConfidentiality(toConfidentialityEmbeddable(documentCreateRequest.getConfidentiality()))
			.withCreatedBy(documentCreateRequest.getCreatedBy())
			.withDescription(documentCreateRequest.getDescription())
			.withMetadata(toDocumentMetadataEmbeddableList(documentCreateRequest.getMetadataList()))
			.withMunicipalityId(municipalityId)
			.withValidFrom(documentCreateRequest.getValidFrom())
			.withValidTo(documentCreateRequest.getValidTo());
	}

	public static List<DocumentDataEntity> toDocumentDataEntities(final DocumentFiles documentFiles, final BinaryStore binaryStore, final String municipalityId) {
		return Optional.ofNullable(documentFiles).map(DocumentFiles::getFiles)
			.map(files -> files.stream()
				.map(file -> toDocumentDataEntity(file, binaryStore, municipalityId))
				.toList())
			.orElse(null);
	}

	public static DocumentDataEntity toDocumentDataEntity(MultipartFile multipartFile, BinaryStore binaryStore, String municipalityId) {
		if (multipartFile == null) {
			return null;
		}
		final StorageRef ref;
		try {
			ref = binaryStore.put(
				multipartFile.getInputStream(),
				multipartFile.getSize(),
				multipartFile.getContentType(),
				buildUserMetadata(multipartFile.getOriginalFilename(), municipalityId));
		} catch (final IOException e) {
			throw Problem.valueOf(INTERNAL_SERVER_ERROR, "Failed to read upload for " + multipartFile.getOriginalFilename() + ": " + e.getMessage());
		}
		return DocumentDataEntity.create()
			.withStorageBackend(ref.backend())
			.withStorageLocator(ref.locator())
			.withMimeType(multipartFile.getContentType())
			.withFileName(multipartFile.getOriginalFilename())
			.withFileSizeInBytes(multipartFile.getSize());
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

	public static List<Boolean> toInclusionFilter(boolean includeConfidential) {
		if (includeConfidential) {
			return CONFIDENTIAL_AND_PUBLIC.getValue();
		}
		return PUBLIC.getValue();
	}

	public static DocumentEntity toDocumentEntity(DocumentUpdateRequest documentUpdateRequest, DocumentEntity existingDocumentEntity, BinaryStore binaryStore) {
		return DocumentEntity.create()
			.withCreatedBy(documentUpdateRequest.getCreatedBy())
			.withMunicipalityId(existingDocumentEntity.getMunicipalityId())
			.withRegistrationNumber(existingDocumentEntity.getRegistrationNumber())
			.withRevision(existingDocumentEntity.getRevision() + 1)
			.withConfidentiality(existingDocumentEntity.getConfidentiality())
			.withArchive(Optional.ofNullable(documentUpdateRequest.getArchive()).orElse(existingDocumentEntity.isArchive()))
			.withDescription(Optional.ofNullable(documentUpdateRequest.getDescription()).orElse(existingDocumentEntity.getDescription()))
			.withMetadata(Optional.ofNullable(documentUpdateRequest.getMetadataList())
				.map(DocumentMapper::toDocumentMetadataEmbeddableList)
				.orElse(copyDocumentMetadataEmbeddableList(existingDocumentEntity.getMetadata())))
			.withDocumentData(copyDocumentDataEntities(existingDocumentEntity.getDocumentData(), binaryStore))
			.withType(existingDocumentEntity.getType())
			.withValidFrom(Optional.ofNullable(documentUpdateRequest.getValidFrom()).orElse(existingDocumentEntity.getValidFrom()))
			.withValidTo(Optional.ofNullable(documentUpdateRequest.getValidTo()).orElse(existingDocumentEntity.getValidTo()));
	}

	public static ConfidentialityEmbeddable toConfidentialityEmbeddable(Confidentiality confidentiality) {
		return Optional.ofNullable(confidentiality)
			.map(c -> ConfidentialityEmbeddable.create()
				.withConfidential(c.isConfidential())
				.withLegalCitation(c.getLegalCitation()))
			.orElse(ConfidentialityEmbeddable.create());
	}

	public static ConfidentialityEmbeddable toConfidentialityEmbeddable(ConfidentialityUpdateRequest confidentialityUpdateRequest) {
		return Optional.ofNullable(confidentialityUpdateRequest)
			.map(c -> ConfidentialityEmbeddable.create()
				.withConfidential(c.getConfidential())
				.withLegalCitation(c.getLegalCitation()))
			.orElse(ConfidentialityEmbeddable.create());
	}

	public static List<DocumentResponsibilityEntity> toDocumentResponsibilityEntities(final List<DocumentResponsibility> responsibilities, final String municipalityId, final String registrationNumber, final String createdBy) {
		return Optional.ofNullable(responsibilities).orElse(emptyList()).stream()
			.map(responsibility -> toDocumentResponsibilityEntity(responsibility, municipalityId, registrationNumber, createdBy))
			.distinct()
			.toList();
	}

	public static DocumentResponsibilityEntity toDocumentResponsibilityEntity(final DocumentResponsibility responsibility, final String municipalityId, final String registrationNumber, final String createdBy) {
		return Optional.ofNullable(responsibility)
			.map(value -> DocumentResponsibilityEntity.create()
				.withMunicipalityId(municipalityId)
				.withRegistrationNumber(registrationNumber)
				.withUsername(normalizeUsername(value.getUsername()))
				.withCreatedBy(createdBy))
			.orElse(null);
	}

	private static String normalizeUsername(final String username) {
		return username == null ? null : username.trim().toLowerCase(Locale.ROOT);
	}

	/**
	 * Database to API mappings.
	 */

	public static List<Document> toDocumentList(List<DocumentEntity> documentEntityList) {
		return Optional.ofNullable(documentEntityList).orElse(emptyList()).stream()
			.map(DocumentMapper::toDocument)
			.toList();
	}

	public static PagedDocumentResponse toPagedDocumentResponse(Page<DocumentEntity> documentEntityPage) {
		return Optional.ofNullable(documentEntityPage)
			.map(page -> PagedDocumentResponse.create()
				.withDocuments(toDocumentList(page.getContent()))
				.withMetaData(PagingMetaData.create()
					.withPage(page.getPageable().getPageNumber())
					.withLimit(page.getPageable().getPageSize())
					.withCount(page.getNumberOfElements())
					.withTotalRecords(page.getTotalElements())
					.withTotalPages(page.getTotalPages())))
			.orElse(null);
	}

	public static Document toDocument(DocumentEntity documentEntity) {
		return toDocument(documentEntity, emptyList());
	}

	public static Document toDocument(DocumentEntity documentEntity, List<DocumentResponsibilityEntity> responsibilities) {
		return Optional.ofNullable(documentEntity)
			.map(docEntity -> Document.create()
				.withConfidentiality(toConfidentiality(docEntity.getConfidentiality()))
				.withArchive(docEntity.isArchive())
				.withCreated(docEntity.getCreated())
				.withCreatedBy(docEntity.getCreatedBy())
				.withDescription(docEntity.getDescription())
				.withDocumentData(toDocumentDataList(docEntity.getDocumentData()))
				.withId(docEntity.getId())
				.withMetadataList(toDocumentMetadataList(docEntity.getMetadata()))
				.withMunicipalityId(docEntity.getMunicipalityId())
				.withRegistrationNumber(docEntity.getRegistrationNumber())
				.withResponsibilities(toDocumentResponsibilities(responsibilities))
				.withRevision(docEntity.getRevision())
				.withType(docEntity.getType().getType())
				.withValidFrom(docEntity.getValidFrom())
				.withValidTo(docEntity.getValidTo()))
			.orElse(null);
	}

	public static Confidentiality toConfidentiality(ConfidentialityEmbeddable confidentialityEmbedded) {
		return Optional.ofNullable(confidentialityEmbedded)
			.map(c -> Confidentiality.create()
				.withConfidential(c.isConfidential())
				.withLegalCitation(c.getLegalCitation()))
			.orElse(null);
	}

	public static List<DocumentResponsibility> toDocumentResponsibilities(final List<DocumentResponsibilityEntity> responsibilities) {
		return Optional.ofNullable(responsibilities).orElse(emptyList()).stream()
			.map(DocumentMapper::toDocumentResponsibility)
			.toList();
	}

	public static DocumentResponsibility toDocumentResponsibility(final DocumentResponsibilityEntity responsibility) {
		return Optional.ofNullable(responsibility)
			.map(value -> DocumentResponsibility.create()
				.withUsername(value.getUsername()))
			.orElse(null);
	}

	/**
	 * Database to Database mappings.
	 */

	public static DocumentEntity copyDocumentEntity(DocumentEntity documentEntity, BinaryStore binaryStore) {
		return Optional.ofNullable(documentEntity)
			.map(docEntity -> DocumentEntity.create()
				.withConfidentiality(docEntity.getConfidentiality())
				.withArchive(documentEntity.isArchive())
				.withCreatedBy(docEntity.getCreatedBy())
				.withDescription(docEntity.getDescription())
				.withDocumentData(copyDocumentDataEntities(docEntity.getDocumentData(), binaryStore))
				.withMetadata(copyDocumentMetadataEmbeddableList(docEntity.getMetadata()))
				.withMunicipalityId(docEntity.getMunicipalityId())
				.withRegistrationNumber(docEntity.getRegistrationNumber())
				.withRevision(docEntity.getRevision())
				.withType(docEntity.getType())
				.withValidFrom(docEntity.getValidFrom())
				.withValidTo(docEntity.getValidTo()))
			.orElse(null);
	}

	public static DocumentDataEntity copyDocumentDataEntity(DocumentDataEntity documentDataEntity, BinaryStore binaryStore) {
		if (documentDataEntity == null) {
			return null;
		}
		final var sourceRef = new StorageRef(documentDataEntity.getStorageBackend(), documentDataEntity.getStorageLocator());
		final var newRef = binaryStore.copy(sourceRef);
		return DocumentDataEntity.create()
			.withMimeType(documentDataEntity.getMimeType())
			.withFileName(documentDataEntity.getFileName())
			.withFileSizeInBytes(documentDataEntity.getFileSizeInBytes())
			.withStorageBackend(newRef.backend())
			.withStorageLocator(newRef.locator());
	}

	/**
	 * Private methods
	 */

	private static List<DocumentDataEntity> copyDocumentDataEntities(List<DocumentDataEntity> documentDataEntityList, BinaryStore binaryStore) {
		return Optional.ofNullable(documentDataEntityList)
			.map(list -> list.stream()
				.map(d -> copyDocumentDataEntity(d, binaryStore))
				.collect(toCollection(ArrayList::new)))
			.orElse(null);
	}

	private static List<DocumentMetadataEmbeddable> copyDocumentMetadataEmbeddableList(List<DocumentMetadataEmbeddable> documentMetadataEmbeddableList) {
		return Optional.ofNullable(documentMetadataEmbeddableList).orElse(emptyList()).stream()
			.map(docMetadataEmbeddable -> DocumentMetadataEmbeddable.create()
				.withKey(docMetadataEmbeddable.getKey())
				.withValue(docMetadataEmbeddable.getValue()))
			.toList();
	}

	private static List<DocumentMetadataEmbeddable> toDocumentMetadataEmbeddableList(List<DocumentMetadata> documentMetadataList) {
		return Optional.ofNullable(documentMetadataList).orElse(emptyList()).stream()
			.map(documentMetadata -> DocumentMetadataEmbeddable.create()
				.withKey(documentMetadata.getKey())
				.withValue(documentMetadata.getValue()))
			.toList();
	}

	private static List<DocumentMetadata> toDocumentMetadataList(List<DocumentMetadataEmbeddable> documentMetadataEmbeddableList) {
		return Optional.ofNullable(documentMetadataEmbeddableList).orElse(emptyList()).stream()
			.map(docMetadataEmbeddable -> DocumentMetadata.create()
				.withKey(docMetadataEmbeddable.getKey())
				.withValue(docMetadataEmbeddable.getValue()))
			.toList();
	}

	private static List<DocumentData> toDocumentDataList(List<DocumentDataEntity> documentDataEntityList) {
		return Optional.ofNullable(documentDataEntityList)
			.map(list -> list.stream()
				.map(DocumentMapper::toDocumentData)
				.collect(toCollection(ArrayList::new)))
			.orElse(null);
	}

	private static DocumentData toDocumentData(DocumentDataEntity documentDataEntity) {
		return Optional.ofNullable(documentDataEntity)
			.map(docDataEntity -> DocumentData.create()
				.withFileName(docDataEntity.getFileName())
				.withFileSizeInBytes(docDataEntity.getFileSizeInBytes())
				.withId(docDataEntity.getId())
				.withMimeType(docDataEntity.getMimeType()))
			.orElse(null);
	}
}
