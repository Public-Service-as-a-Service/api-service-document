package se.sundsvall.document.service;

import java.time.LocalDate;
import java.util.EnumSet;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.document.api.model.ConfidentialityUpdateRequest;
import se.sundsvall.document.api.model.Document;
import se.sundsvall.document.api.model.DocumentCreateRequest;
import se.sundsvall.document.api.model.DocumentFiles;
import se.sundsvall.document.api.model.DocumentParameters;
import se.sundsvall.document.api.model.DocumentResponsibilitiesUpdateRequest;
import se.sundsvall.document.api.model.DocumentStatus;
import se.sundsvall.document.api.model.DocumentUpdateRequest;
import se.sundsvall.document.api.model.PagedDocumentResponse;
import se.sundsvall.document.integration.db.DocumentRepository;
import se.sundsvall.document.integration.db.DocumentResponsibilityRepository;
import se.sundsvall.document.integration.db.DocumentTypeRepository;
import se.sundsvall.document.integration.db.model.DocumentEntity;
import se.sundsvall.document.integration.db.model.DocumentResponsibilityEntity;
import se.sundsvall.document.service.storage.BinaryStore;

import static java.util.Objects.nonNull;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.util.CollectionUtils.isEmpty;
import static se.sundsvall.document.service.Constants.ERROR_DOCUMENT_BY_REGISTRATION_NUMBER_AND_REVISION_NOT_FOUND;
import static se.sundsvall.document.service.Constants.ERROR_DOCUMENT_BY_REGISTRATION_NUMBER_NOT_FOUND;
import static se.sundsvall.document.service.Constants.ERROR_STATUS_TRANSITION_NOT_ALLOWED;
import static se.sundsvall.document.service.Constants.ERROR_VALID_FROM_AFTER_VALID_TO;
import static se.sundsvall.document.service.InclusionFilter.CONFIDENTIAL_AND_PUBLIC;
import static se.sundsvall.document.service.mapper.DocumentMapper.applyUpdate;
import static se.sundsvall.document.service.mapper.DocumentMapper.toConfidentialityEmbeddable;
import static se.sundsvall.document.service.mapper.DocumentMapper.toDocument;
import static se.sundsvall.document.service.mapper.DocumentMapper.toDocumentDataEntities;
import static se.sundsvall.document.service.mapper.DocumentMapper.toDocumentEntity;
import static se.sundsvall.document.service.mapper.DocumentMapper.toDocumentResponsibilities;
import static se.sundsvall.document.service.mapper.DocumentMapper.toDocumentResponsibilityEntities;
import static se.sundsvall.document.service.mapper.DocumentMapper.toInclusionFilter;
import static se.sundsvall.document.service.mapper.DocumentMapper.toPagedDocumentResponse;

@Service
@Transactional
public class DocumentService {

	private static final String ERROR_DOCUMENT_TYPE_NOT_FOUND = "Document type with identifier %s was not found within municipality with id %s";

	private final BinaryStore binaryStore;
	private final DocumentRepository documentRepository;
	private final DocumentResponsibilityRepository documentResponsibilityRepository;
	private final DocumentTypeRepository documentTypeRepository;
	private final RegistrationNumberService registrationNumberService;
	private final DocumentStatusPolicy statusPolicy;
	private final DocumentEventPublisher eventPublisher;

	public DocumentService(
		final BinaryStore binaryStore,
		final DocumentRepository documentRepository,
		final DocumentResponsibilityRepository documentResponsibilityRepository,
		final DocumentTypeRepository documentTypeRepository,
		final RegistrationNumberService registrationNumberService,
		final DocumentStatusPolicy statusPolicy,
		final DocumentEventPublisher eventPublisher) {

		this.binaryStore = binaryStore;
		this.documentRepository = documentRepository;
		this.documentResponsibilityRepository = documentResponsibilityRepository;
		this.documentTypeRepository = documentTypeRepository;
		this.registrationNumberService = registrationNumberService;
		this.statusPolicy = statusPolicy;
		this.eventPublisher = eventPublisher;
	}

	public Document create(final DocumentCreateRequest documentCreateRequest, final DocumentFiles documentFiles, final String municipalityId) {

		validateValidityWindow(documentCreateRequest.getValidFrom(), documentCreateRequest.getValidTo());

		final var documentDataEntities = toDocumentDataEntities(documentFiles, binaryStore, municipalityId);
		final var registrationNumber = registrationNumberService.generateRegistrationNumber(municipalityId);
		final var documentTypeEntity = documentTypeRepository.findByMunicipalityIdAndType(municipalityId, documentCreateRequest.getType())
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, ERROR_DOCUMENT_TYPE_NOT_FOUND.formatted(documentCreateRequest.getType(), municipalityId)));

		final var documentEntity = toDocumentEntity(documentCreateRequest, municipalityId)
			.withRegistrationNumber(registrationNumber)
			.withDocumentData(documentDataEntities)
			.withType(documentTypeEntity);

		final var savedDocumentEntity = documentRepository.save(documentEntity);
		final var responsibilities = documentResponsibilityRepository.saveAll(toDocumentResponsibilityEntities(documentCreateRequest.getResponsibilities(), municipalityId, registrationNumber, documentCreateRequest.getCreatedBy()));

		return toDocument(savedDocumentEntity, responsibilities);
	}

	public Document read(String registrationNumber, boolean includeConfidential, boolean includeNonPublic, String municipalityId) {

		final var documentEntity = findLatestRevisionForRead(municipalityId, registrationNumber, includeConfidential, includeNonPublic);
		reconcileStatusIfStale(documentEntity);

		return toDocumentWithResponsibilities(documentEntity);
	}

	public Document read(String registrationNumber, int revision, boolean includeConfidential, String municipalityId) {

		final var documentEntity = documentRepository.findByMunicipalityIdAndRegistrationNumberAndRevisionAndConfidentialityConfidentialIn(municipalityId, registrationNumber, revision, toInclusionFilter(includeConfidential))
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, ERROR_DOCUMENT_BY_REGISTRATION_NUMBER_AND_REVISION_NOT_FOUND.formatted(registrationNumber, revision)));

		return toDocumentWithResponsibilities(documentEntity);
	}

	public PagedDocumentResponse readAll(String registrationNumber, boolean includeConfidential, Pageable pageable, String municipalityId) {
		return toPagedDocumentResponseWithResponsibilities(documentRepository.findByMunicipalityIdAndRegistrationNumberAndConfidentialityConfidentialIn(municipalityId, registrationNumber, toInclusionFilter(includeConfidential), pageable));
	}

	public PagedDocumentResponse search(String query, boolean includeConfidential, boolean onlyLatestRevision, Pageable pageable, String municipalityId) {
		final var effectiveStatuses = statusPolicy.effectivePublishedStatuses(null);
		return toPagedDocumentResponseWithResponsibilities(documentRepository.search(query, includeConfidential, onlyLatestRevision, pageable, municipalityId, effectiveStatuses));
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

		return toDocumentWithResponsibilities(documentRepository.save(existingDocumentEntity));
	}

	public Document publish(String registrationNumber, String updatedBy, String municipalityId, Integer revision) {
		return transitionStatus(registrationNumber, updatedBy, municipalityId, revision, "publish",
			EnumSet.of(DocumentStatus.DRAFT),
			entity -> statusPolicy.resolvePublishedStatus(entity.getValidFrom(), entity.getValidTo(), registrationNumber));
	}

	public Document revoke(String registrationNumber, String updatedBy, String municipalityId, Integer revision) {
		return transitionStatus(registrationNumber, updatedBy, municipalityId, revision, "revoke",
			EnumSet.of(DocumentStatus.ACTIVE, DocumentStatus.SCHEDULED),
			entity -> DocumentStatus.REVOKED);
	}

	public Document unrevoke(String registrationNumber, String updatedBy, String municipalityId, Integer revision) {
		return transitionStatus(registrationNumber, updatedBy, municipalityId, revision, "unrevoke",
			EnumSet.of(DocumentStatus.REVOKED),
			entity -> statusPolicy.resolvePublishedStatus(entity.getValidFrom(), entity.getValidTo(), registrationNumber));
	}

	public void updateConfidentiality(String registrationNumber, ConfidentialityUpdateRequest confidentialityUpdateRequest, String municipalityId) {

		final var documentEntities = documentRepository.findByMunicipalityIdAndRegistrationNumberAndConfidentialityConfidentialIn(municipalityId, registrationNumber, CONFIDENTIAL_AND_PUBLIC.getValue());

		final var newConfidentialitySettings = toConfidentialityEmbeddable(confidentialityUpdateRequest);

		documentEntities.forEach(documentEntity -> documentEntity.setConfidentiality(newConfidentialitySettings));

		eventPublisher.logConfidentialityChange(registrationNumber, confidentialityUpdateRequest, municipalityId);

		documentRepository.saveAll(documentEntities);
	}

	public void updateResponsibilities(final String registrationNumber, final DocumentResponsibilitiesUpdateRequest request, final String municipalityId) {

		if (!documentRepository.existsByMunicipalityIdAndRegistrationNumber(municipalityId, registrationNumber)) {
			throw Problem.valueOf(NOT_FOUND, ERROR_DOCUMENT_BY_REGISTRATION_NUMBER_NOT_FOUND.formatted(registrationNumber));
		}

		final var oldResponsibilities = documentResponsibilityRepository.findByMunicipalityIdAndRegistrationNumberOrderByPersonIdAsc(municipalityId, registrationNumber);
		final var newResponsibilities = toDocumentResponsibilityEntities(request.getResponsibilities(), municipalityId, registrationNumber, request.getUpdatedBy());

		documentResponsibilityRepository.deleteByMunicipalityIdAndRegistrationNumber(municipalityId, registrationNumber);
		// Force DELETE before INSERT so the (municipality_id, registration_number, person_id) unique constraint
		// isn't violated when a personId is both removed and re-added in the same call.
		documentResponsibilityRepository.flush();
		documentResponsibilityRepository.saveAll(newResponsibilities);

		eventPublisher.logResponsibilitiesChange(registrationNumber, request.getUpdatedBy(), oldResponsibilities, newResponsibilities, municipalityId);
	}

	public PagedDocumentResponse searchByParameters(final DocumentParameters parameters) {
		var pageable = PageRequest.of(parameters.getPage() - 1, parameters.getLimit(), parameters.sort());
		final var effectiveStatuses = statusPolicy.effectivePublishedStatuses(parameters.getStatuses());
		return toPagedDocumentResponseWithResponsibilities(documentRepository.searchByParameters(parameters, pageable, effectiveStatuses));
	}

	private Document toDocumentWithResponsibilities(final DocumentEntity documentEntity) {
		final var responsibilities = documentResponsibilityRepository.findByMunicipalityIdAndRegistrationNumberOrderByPersonIdAsc(documentEntity.getMunicipalityId(), documentEntity.getRegistrationNumber());
		return toDocument(documentEntity, responsibilities);
	}

	private PagedDocumentResponse toPagedDocumentResponseWithResponsibilities(final org.springframework.data.domain.Page<DocumentEntity> documentEntityPage) {
		final var response = toPagedDocumentResponse(documentEntityPage);
		if (response == null || isEmpty(response.getDocuments())) {
			return response;
		}

		final var documents = response.getDocuments();
		final var responsibilitiesByRegistrationNumber = documentResponsibilityRepository.findByMunicipalityIdAndRegistrationNumberIn(
			documents.get(0).getMunicipalityId(),
			documents.stream()
				.map(Document::getRegistrationNumber)
				.distinct()
				.toList()).stream()
			.collect(Collectors.groupingBy(DocumentResponsibilityEntity::getRegistrationNumber));

		documents.forEach(document -> document.setResponsibilities(toDocumentResponsibilities(responsibilitiesByRegistrationNumber.get(document.getRegistrationNumber()))));

		return response;
	}

	private Document transitionStatus(String registrationNumber, String updatedBy, String municipalityId, Integer revision, String action,
		Set<DocumentStatus> allowedFromStatuses, Function<DocumentEntity, DocumentStatus> nextStatusResolver) {

		final var documentEntity = findDocumentForStatusTransition(municipalityId, registrationNumber, revision);

		final var previousStatus = documentEntity.getStatus();
		if (!allowedFromStatuses.contains(previousStatus)) {
			throw Problem.valueOf(CONFLICT, ERROR_STATUS_TRANSITION_NOT_ALLOWED.formatted(previousStatus, action, registrationNumber));
		}

		final var newStatus = nextStatusResolver.apply(documentEntity);
		documentEntity.setStatus(newStatus);

		eventPublisher.logStatusChange(registrationNumber, documentEntity.getRevision(), previousStatus, newStatus, updatedBy, municipalityId);

		return toDocumentWithResponsibilities(documentRepository.save(documentEntity));
	}

	private DocumentEntity findDocumentForStatusTransition(String municipalityId, String registrationNumber, Integer revision) {
		if (revision == null) {
			return documentRepository.findTopByMunicipalityIdAndRegistrationNumberAndConfidentialityConfidentialInOrderByRevisionDesc(municipalityId, registrationNumber, CONFIDENTIAL_AND_PUBLIC.getValue())
				.orElseThrow(() -> Problem.valueOf(NOT_FOUND, ERROR_DOCUMENT_BY_REGISTRATION_NUMBER_NOT_FOUND.formatted(registrationNumber)));
		}
		return documentRepository.findByMunicipalityIdAndRegistrationNumberAndRevisionAndConfidentialityConfidentialIn(municipalityId, registrationNumber, revision, CONFIDENTIAL_AND_PUBLIC.getValue())
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, ERROR_DOCUMENT_BY_REGISTRATION_NUMBER_AND_REVISION_NOT_FOUND.formatted(registrationNumber, revision)));
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

	private static void validateValidityWindow(LocalDate validFrom, LocalDate validTo) {
		if (validFrom != null && validTo != null && validFrom.isAfter(validTo)) {
			throw Problem.valueOf(CONFLICT, ERROR_VALID_FROM_AFTER_VALID_TO.formatted(validFrom, validTo));
		}
	}
}
