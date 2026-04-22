package se.sundsvall.document.service;

import java.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.document.api.model.Document;
import se.sundsvall.document.api.model.DocumentCreateRequest;
import se.sundsvall.document.api.model.DocumentFiles;
import se.sundsvall.document.api.model.DocumentStatus;
import se.sundsvall.document.api.model.DocumentUpdateRequest;
import se.sundsvall.document.api.model.PagedDocumentResponse;
import se.sundsvall.document.integration.db.DocumentDataRepository;
import se.sundsvall.document.integration.db.DocumentRepository;
import se.sundsvall.document.integration.db.DocumentResponsibilityRepository;
import se.sundsvall.document.integration.db.DocumentTypeRepository;
import se.sundsvall.document.integration.db.model.DocumentEntity;
import se.sundsvall.document.service.extraction.TextExtractor;
import se.sundsvall.document.service.indexing.DocumentIndexingEvent;
import se.sundsvall.document.service.storage.BinaryStore;

import static java.util.Objects.nonNull;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static se.sundsvall.document.service.Constants.ERROR_DOCUMENT_BY_REGISTRATION_NUMBER_AND_REVISION_NOT_FOUND;
import static se.sundsvall.document.service.Constants.ERROR_DOCUMENT_BY_REGISTRATION_NUMBER_NOT_FOUND;
import static se.sundsvall.document.service.Constants.ERROR_VALID_FROM_AFTER_VALID_TO;
import static se.sundsvall.document.service.mapper.DocumentDataMapper.toDocumentDataEntities;
import static se.sundsvall.document.service.mapper.DocumentMapper.applyUpdate;
import static se.sundsvall.document.service.mapper.DocumentMapper.toDocument;
import static se.sundsvall.document.service.mapper.DocumentMapper.toDocumentEntity;
import static se.sundsvall.document.service.mapper.DocumentMapper.toDocumentResponsibilityEntities;
import static se.sundsvall.document.service.mapper.DocumentMapper.toInclusionFilter;

/**
 * Core document CRUD: create / read / list-revisions / update-in-place. File operations live in
 * {@link DocumentFileService}; status/confidentiality in {@link DocumentStatusService}; search in
 * {@link DocumentSearchService}; responsibilities in {@link DocumentResponsibilityService}.
 * <p>
 * {@link #update} modifies the current revision in place and flips status to {@code DRAFT}; it
 * does <em>not</em> bump the revision. Revision bumps are exclusive to file add/replace/delete.
 */
@Service
@Transactional
public class DocumentService {

	private static final Logger LOGGER = LoggerFactory.getLogger(DocumentService.class);

	private static final String ERROR_DOCUMENT_TYPE_NOT_FOUND = "Document type with identifier %s was not found within municipality with id %s";

	private final BinaryStore binaryStore;
	private final DocumentRepository documentRepository;
	private final DocumentResponsibilityRepository documentResponsibilityRepository;
	private final DocumentTypeRepository documentTypeRepository;
	private final DocumentDataRepository documentDataRepository;
	private final RegistrationNumberService registrationNumberService;
	private final DocumentStatusPolicy statusPolicy;
	private final TextExtractor textExtractor;
	private final ApplicationEventPublisher applicationEventPublisher;
	private final DocumentResponseHydrator responseHydrator;

	public DocumentService(
		final BinaryStore binaryStore,
		final DocumentRepository documentRepository,
		final DocumentResponsibilityRepository documentResponsibilityRepository,
		final DocumentTypeRepository documentTypeRepository,
		final DocumentDataRepository documentDataRepository,
		final RegistrationNumberService registrationNumberService,
		final DocumentStatusPolicy statusPolicy,
		final TextExtractor textExtractor,
		final ApplicationEventPublisher applicationEventPublisher,
		final DocumentResponseHydrator responseHydrator) {

		this.binaryStore = binaryStore;
		this.documentRepository = documentRepository;
		this.documentResponsibilityRepository = documentResponsibilityRepository;
		this.documentTypeRepository = documentTypeRepository;
		this.documentDataRepository = documentDataRepository;
		this.registrationNumberService = registrationNumberService;
		this.statusPolicy = statusPolicy;
		this.textExtractor = textExtractor;
		this.applicationEventPublisher = applicationEventPublisher;
		this.responseHydrator = responseHydrator;
	}

	public Document create(final DocumentCreateRequest documentCreateRequest, final DocumentFiles documentFiles, final String municipalityId) {

		validateValidityWindow(documentCreateRequest.getValidFrom(), documentCreateRequest.getValidTo());

		final var documentDataEntities = toDocumentDataEntities(documentFiles, binaryStore, textExtractor, documentDataRepository, municipalityId);
		final var registrationNumber = registrationNumberService.generateRegistrationNumber(municipalityId);
		final var documentTypeEntity = documentTypeRepository.findByMunicipalityIdAndType(municipalityId, documentCreateRequest.getType())
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, ERROR_DOCUMENT_TYPE_NOT_FOUND.formatted(documentCreateRequest.getType(), municipalityId)));

		final var documentEntity = toDocumentEntity(documentCreateRequest, municipalityId)
			.withRegistrationNumber(registrationNumber)
			.withDocumentData(documentDataEntities)
			.withType(documentTypeEntity);

		final var savedDocumentEntity = documentRepository.save(documentEntity);
		final var responsibilities = documentResponsibilityRepository.saveAll(toDocumentResponsibilityEntities(documentCreateRequest.getResponsibilities(), municipalityId, registrationNumber, documentCreateRequest.getCreatedBy()));

		LOGGER.info("Created document (registrationNumber='{}', type='{}', files={}, responsibilities={}, status={}, createdBy='{}')",
			registrationNumber, documentCreateRequest.getType(),
			documentDataEntities != null ? documentDataEntities.size() : 0,
			responsibilities != null ? responsibilities.size() : 0,
			savedDocumentEntity.getStatus(), documentCreateRequest.getCreatedBy());

		applicationEventPublisher.publishEvent(DocumentIndexingEvent.reindex(savedDocumentEntity.getId()));

		return toDocument(savedDocumentEntity, responsibilities);
	}

	public Document read(String registrationNumber, boolean includeConfidential, boolean includeNonPublic, String municipalityId) {

		final var documentEntity = findLatestRevisionForRead(municipalityId, registrationNumber, includeConfidential, includeNonPublic);
		reconcileStatusIfStale(documentEntity);

		return responseHydrator.hydrate(documentEntity);
	}

	public Document read(String registrationNumber, int revision, boolean includeConfidential, String municipalityId) {

		final var documentEntity = documentRepository.findByMunicipalityIdAndRegistrationNumberAndRevisionAndConfidentialityConfidentialIn(municipalityId, registrationNumber, revision, toInclusionFilter(includeConfidential))
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, ERROR_DOCUMENT_BY_REGISTRATION_NUMBER_AND_REVISION_NOT_FOUND.formatted(registrationNumber, revision)));

		return responseHydrator.hydrate(documentEntity);
	}

	public PagedDocumentResponse readAll(String registrationNumber, boolean includeConfidential, Pageable pageable, String municipalityId) {
		return responseHydrator.hydrate(documentRepository.findByMunicipalityIdAndRegistrationNumberAndConfidentialityConfidentialIn(municipalityId, registrationNumber, toInclusionFilter(includeConfidential), pageable));
	}

	public Document update(String registrationNumber, boolean includeConfidential, DocumentUpdateRequest documentUpdateRequest, String municipalityId) {

		final var existingDocumentEntity = documentRepository.findTopByMunicipalityIdAndRegistrationNumberAndConfidentialityConfidentialInOrderByRevisionDesc(municipalityId, registrationNumber, toInclusionFilter(includeConfidential))
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, ERROR_DOCUMENT_BY_REGISTRATION_NUMBER_NOT_FOUND.formatted(registrationNumber)));

		applyUpdate(documentUpdateRequest, existingDocumentEntity);
		validateValidityWindow(existingDocumentEntity.getValidFrom(), existingDocumentEntity.getValidTo());
		existingDocumentEntity.setStatus(DocumentStatus.DRAFT);

		if (nonNull(documentUpdateRequest.getType())) {
			final var documentTypeEntity = documentTypeRepository.findByMunicipalityIdAndType(municipalityId, documentUpdateRequest.getType())
				.orElseThrow(() -> Problem.valueOf(NOT_FOUND, ERROR_DOCUMENT_TYPE_NOT_FOUND.formatted(documentUpdateRequest.getType(), municipalityId)));

			existingDocumentEntity.setType(documentTypeEntity);
		}

		final var saved = documentRepository.save(existingDocumentEntity);
		LOGGER.info("Updated document in place (registrationNumber='{}', revision={}, status→DRAFT, type='{}')",
			registrationNumber, saved.getRevision(),
			saved.getType() != null ? saved.getType().getType() : null);
		applicationEventPublisher.publishEvent(DocumentIndexingEvent.reindex(saved.getId()));
		return responseHydrator.hydrate(saved);
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
				final var oldStatus = documentEntity.getStatus();
				documentEntity.setStatus(newStatus);
				documentRepository.save(documentEntity);
				LOGGER.info("Status auto-reconciled on read (registrationNumber='{}', revision={}, {}→{}, validFrom={}, validTo={})",
					documentEntity.getRegistrationNumber(), documentEntity.getRevision(),
					oldStatus, newStatus, documentEntity.getValidFrom(), documentEntity.getValidTo());
			});
	}

	private static void validateValidityWindow(LocalDate validFrom, LocalDate validTo) {
		if (validFrom != null && validTo != null && validFrom.isAfter(validTo)) {
			throw Problem.valueOf(CONFLICT, ERROR_VALID_FROM_AFTER_VALID_TO.formatted(validFrom, validTo));
		}
	}
}
