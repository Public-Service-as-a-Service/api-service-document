package se.sundsvall.document.service;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.apache.commons.lang3.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.ContentDisposition;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.document.api.model.Document;
import se.sundsvall.document.api.model.DocumentDataCreateRequest;
import se.sundsvall.document.api.model.DocumentFiles;
import se.sundsvall.document.integration.db.DocumentDataRepository;
import se.sundsvall.document.integration.db.DocumentRepository;
import se.sundsvall.document.integration.db.DocumentResponsibilityRepository;
import se.sundsvall.document.integration.db.model.DocumentDataEntity;
import se.sundsvall.document.integration.db.model.DocumentEntity;
import se.sundsvall.document.service.extraction.TextExtractor;
import se.sundsvall.document.service.indexing.DocumentIndexingEvent;
import se.sundsvall.document.service.mapper.DocumentMapper;
import se.sundsvall.document.service.storage.BinaryStore;
import se.sundsvall.document.service.storage.StorageRef;

import static org.springframework.http.HttpHeaders.CONTENT_DISPOSITION;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.util.CollectionUtils.isEmpty;
import static se.sundsvall.document.service.Constants.ERROR_DOCUMENT_BY_REGISTRATION_NUMBER_AND_REVISION_NOT_FOUND;
import static se.sundsvall.document.service.Constants.ERROR_DOCUMENT_BY_REGISTRATION_NUMBER_NOT_FOUND;
import static se.sundsvall.document.service.Constants.ERROR_DOCUMENT_FILE_BY_ID_NOT_FOUND;
import static se.sundsvall.document.service.Constants.ERROR_DOCUMENT_FILE_BY_REGISTRATION_NUMBER_AND_REVISION_NOT_FOUND;
import static se.sundsvall.document.service.Constants.ERROR_DOCUMENT_FILE_BY_REGISTRATION_NUMBER_COULD_NOT_READ;
import static se.sundsvall.document.service.Constants.ERROR_DOCUMENT_FILE_BY_REGISTRATION_NUMBER_NOT_FOUND;
import static se.sundsvall.document.service.InclusionFilter.CONFIDENTIAL_AND_PUBLIC;
import static se.sundsvall.document.service.mapper.DocumentMapper.copyDocumentEntity;
import static se.sundsvall.document.service.mapper.DocumentMapper.toDocument;
import static se.sundsvall.document.service.mapper.DocumentMapper.toDocumentDataEntities;
import static se.sundsvall.document.service.mapper.DocumentMapper.toInclusionFilter;

@Service
@Transactional
public class DocumentFileService {

	private static final Logger LOGGER = LoggerFactory.getLogger(DocumentFileService.class);

	private final BinaryStore binaryStore;
	private final DocumentRepository documentRepository;
	private final DocumentResponsibilityRepository documentResponsibilityRepository;
	private final DocumentDataRepository documentDataRepository;
	private final DocumentStatusPolicy statusPolicy;
	private final TextExtractor textExtractor;
	private final ApplicationEventPublisher applicationEventPublisher;

	public DocumentFileService(
		final BinaryStore binaryStore,
		final DocumentRepository documentRepository,
		final DocumentResponsibilityRepository documentResponsibilityRepository,
		final DocumentDataRepository documentDataRepository,
		final DocumentStatusPolicy statusPolicy,
		final TextExtractor textExtractor,
		final ApplicationEventPublisher applicationEventPublisher) {

		this.binaryStore = binaryStore;
		this.documentRepository = documentRepository;
		this.documentResponsibilityRepository = documentResponsibilityRepository;
		this.documentDataRepository = documentDataRepository;
		this.statusPolicy = statusPolicy;
		this.textExtractor = textExtractor;
		this.applicationEventPublisher = applicationEventPublisher;
	}

	public void readFile(String registrationNumber, String documentDataId, boolean includeConfidential, boolean includeNonPublic, HttpServletResponse response, String municipalityId) {

		final var documentEntity = findLatestRevisionForRead(municipalityId, registrationNumber, includeConfidential, includeNonPublic);
		reconcileStatusIfStale(documentEntity);

		if (isEmpty(documentEntity.getDocumentData())) {
			throw Problem.valueOf(NOT_FOUND, ERROR_DOCUMENT_FILE_BY_REGISTRATION_NUMBER_NOT_FOUND.formatted(registrationNumber));
		}

		final var documentDataEntity = documentEntity.getDocumentData().stream()
			.filter(docData -> docData.getId().equals(documentDataId))
			.findFirst()
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, ERROR_DOCUMENT_FILE_BY_ID_NOT_FOUND.formatted(documentDataId)));

		addFileContentToResponse(documentDataEntity, response);
	}

	public void readFile(String registrationNumber, int revision, String documentDataId, boolean includeConfidential, HttpServletResponse response, String municipalityId) {

		final var documentEntity = documentRepository.findByMunicipalityIdAndRegistrationNumberAndRevisionAndConfidentialityConfidentialIn(municipalityId, registrationNumber, revision, toInclusionFilter(includeConfidential))
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, ERROR_DOCUMENT_BY_REGISTRATION_NUMBER_AND_REVISION_NOT_FOUND.formatted(registrationNumber, revision)));

		if (isEmpty(documentEntity.getDocumentData())) {
			throw Problem.valueOf(NOT_FOUND, ERROR_DOCUMENT_FILE_BY_REGISTRATION_NUMBER_AND_REVISION_NOT_FOUND.formatted(registrationNumber, revision));
		}

		final var documentDataEntity = documentEntity.getDocumentData().stream()
			.filter(docData -> docData.getId().equals(documentDataId))
			.findFirst()
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, ERROR_DOCUMENT_FILE_BY_ID_NOT_FOUND.formatted(documentDataId)));

		addFileContentToResponse(documentDataEntity, response);
	}

	public Document addOrReplaceFile(String registrationNumber, DocumentDataCreateRequest documentDataCreateRequest, MultipartFile documentFile, String municipalityId) {
		return addOrReplaceFiles(registrationNumber, documentDataCreateRequest, DocumentFiles.create().withFiles(List.of(documentFile)), municipalityId);
	}

	public Document addOrReplaceFiles(String registrationNumber, DocumentDataCreateRequest documentDataCreateRequest, DocumentFiles documentFiles, String municipalityId) {

		final var documentEntity = documentRepository.findTopByMunicipalityIdAndRegistrationNumberAndConfidentialityConfidentialInOrderByRevisionDesc(municipalityId, registrationNumber, CONFIDENTIAL_AND_PUBLIC.getValue())
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, ERROR_DOCUMENT_BY_REGISTRATION_NUMBER_NOT_FOUND.formatted(registrationNumber)));

		final var newDocumentDataEntities = toDocumentDataEntities(documentFiles, binaryStore, textExtractor, documentDataRepository, municipalityId);

		final var newDocumentEntity = copyDocumentEntity(documentEntity, binaryStore)
			.withRevision(documentEntity.getRevision() + 1)
			.withCreatedBy(documentDataCreateRequest.getCreatedBy());

		newDocumentDataEntities.forEach(entity -> addOrReplaceDocumentDataEntity(newDocumentEntity, entity));

		final var saved = documentRepository.save(newDocumentEntity);
		applicationEventPublisher.publishEvent(DocumentIndexingEvent.reindex(saved.getId()));
		return toDocumentWithResponsibilities(saved);
	}

	public void deleteFile(String registrationNumber, String documentDataId, String municipalityId) {

		final var documentEntity = documentRepository.findTopByMunicipalityIdAndRegistrationNumberAndConfidentialityConfidentialInOrderByRevisionDesc(municipalityId, registrationNumber, toInclusionFilter(true))
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, ERROR_DOCUMENT_BY_REGISTRATION_NUMBER_NOT_FOUND.formatted(registrationNumber)));

		if (isEmpty(documentEntity.getDocumentData())) {
			throw Problem.valueOf(NOT_FOUND, ERROR_DOCUMENT_FILE_BY_REGISTRATION_NUMBER_NOT_FOUND.formatted(registrationNumber));
		}

		final var newDocumentEntity = copyDocumentEntity(documentEntity, binaryStore)
			.withRevision(documentEntity.getRevision() + 1)
			.withDocumentData(documentEntity.getDocumentData().stream()
				.filter(docDataEntity -> !docDataEntity.getId().equals(documentDataId))
				.map(d -> DocumentMapper.copyDocumentDataEntity(d, binaryStore))
				.toList());

		if (documentEntity.getDocumentData().size() == newDocumentEntity.getDocumentData().size()) {
			throw Problem.valueOf(NOT_FOUND, ERROR_DOCUMENT_FILE_BY_ID_NOT_FOUND.formatted(documentDataId));
		}

		final var saved = documentRepository.save(newDocumentEntity);
		// The removed file's ES doc keeps referencing the old revision; drop it so search doesn't
		// return the deleted file. The new revision's remaining files are indexed by the reindex.
		applicationEventPublisher.publishEvent(
			DocumentIndexingEvent.reindexAfterDelete(saved.getId(), List.of(documentDataId)));
	}

	private void addFileContentToResponse(DocumentDataEntity documentDataEntity, HttpServletResponse response) {
		response.addHeader(CONTENT_TYPE, documentDataEntity.getMimeType());
		response.addHeader(CONTENT_DISPOSITION, ContentDisposition.attachment()
			.filename(documentDataEntity.getFileName(), StandardCharsets.UTF_8)
			.build()
			.toString());
		response.setContentLength((int) documentDataEntity.getFileSizeInBytes());

		final var ref = StorageRef.s3(documentDataEntity.getStorageLocator());
		try {
			binaryStore.streamTo(ref, response.getOutputStream());
		} catch (final IOException e) {
			LOGGER.warn(ERROR_DOCUMENT_FILE_BY_REGISTRATION_NUMBER_COULD_NOT_READ.formatted(documentDataEntity.getId()), e);
			resetIfUncommitted(response);
			throw Problem.valueOf(INTERNAL_SERVER_ERROR, ERROR_DOCUMENT_FILE_BY_REGISTRATION_NUMBER_COULD_NOT_READ.formatted(documentDataEntity.getId()));
		} catch (final RuntimeException e) {
			// Stale Content-Length/Content-Type headers would make clients wait for bytes the error handler never writes.
			resetIfUncommitted(response);
			throw e;
		}
	}

	private static void resetIfUncommitted(HttpServletResponse response) {
		if (!response.isCommitted()) {
			response.reset();
		}
	}

	private void addOrReplaceDocumentDataEntity(DocumentEntity documentEntity, DocumentDataEntity documentDataEntity) {

		final var documentDataList = Optional.ofNullable(documentEntity.getDocumentData()).orElse(new ArrayList<>());

		documentDataList.removeIf(documentData -> Strings.CI.equals(documentData.getFileName(), documentDataEntity.getFileName()));

		documentDataList.add(documentDataEntity);

		documentEntity.setDocumentData(documentDataList);
	}

	private DocumentEntity findLatestRevisionForRead(String municipalityId, String registrationNumber, boolean includeConfidential, boolean includeNonPublic) {

		if (includeNonPublic) {
			return documentRepository.findTopByMunicipalityIdAndRegistrationNumberAndConfidentialityConfidentialInOrderByRevisionDesc(municipalityId, registrationNumber, toInclusionFilter(includeConfidential))
				.orElseThrow(() -> Problem.valueOf(NOT_FOUND, ERROR_DOCUMENT_BY_REGISTRATION_NUMBER_NOT_FOUND.formatted(registrationNumber)));
		}

		return documentRepository.findTopByMunicipalityIdAndRegistrationNumberAndConfidentialityConfidentialInAndStatusNotInOrderByRevisionDesc(
			municipalityId, registrationNumber, toInclusionFilter(includeConfidential), DocumentStatusPolicy.nonPublicStatuses())
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, ERROR_DOCUMENT_BY_REGISTRATION_NUMBER_NOT_FOUND.formatted(registrationNumber)));
	}

	private void reconcileStatusIfStale(DocumentEntity documentEntity) {
		statusPolicy.reconcile(documentEntity.getStatus(), documentEntity.getValidFrom(), documentEntity.getValidTo())
			.ifPresent(newStatus -> {
				documentEntity.setStatus(newStatus);
				documentRepository.save(documentEntity);
			});
	}

	private Document toDocumentWithResponsibilities(final DocumentEntity documentEntity) {
		final var responsibilities = documentResponsibilityRepository.findByMunicipalityIdAndRegistrationNumberOrderByPersonIdAsc(documentEntity.getMunicipalityId(), documentEntity.getRegistrationNumber());
		return toDocument(documentEntity, responsibilities);
	}
}
