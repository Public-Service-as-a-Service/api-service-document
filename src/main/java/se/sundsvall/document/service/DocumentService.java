package se.sundsvall.document.service;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.commons.lang3.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ContentDisposition;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.document.api.model.ConfidentialityUpdateRequest;
import se.sundsvall.document.api.model.Document;
import se.sundsvall.document.api.model.DocumentCreateRequest;
import se.sundsvall.document.api.model.DocumentDataCreateRequest;
import se.sundsvall.document.api.model.DocumentFiles;
import se.sundsvall.document.api.model.DocumentParameters;
import se.sundsvall.document.api.model.DocumentResponsibilitiesUpdateRequest;
import se.sundsvall.document.api.model.DocumentStatus;
import se.sundsvall.document.api.model.DocumentUpdateRequest;
import se.sundsvall.document.api.model.PagedDocumentResponse;
import se.sundsvall.document.integration.db.DocumentRepository;
import se.sundsvall.document.integration.db.DocumentResponsibilityRepository;
import se.sundsvall.document.integration.db.DocumentTypeRepository;
import se.sundsvall.document.integration.db.model.DocumentDataEntity;
import se.sundsvall.document.integration.db.model.DocumentEntity;
import se.sundsvall.document.integration.db.model.DocumentResponsibilityEntity;
import se.sundsvall.document.integration.eventlog.EventLogClient;
import se.sundsvall.document.integration.eventlog.configuration.EventlogProperties;
import se.sundsvall.document.service.mapper.DocumentMapper;
import se.sundsvall.document.service.statistics.AccessContext;
import se.sundsvall.document.service.statistics.DocumentAccessedEvent;
import se.sundsvall.document.service.storage.BinaryStore;
import se.sundsvall.document.service.storage.StorageRef;

import static generated.se.sundsvall.eventlog.EventType.UPDATE;
import static java.time.OffsetDateTime.now;
import static java.time.ZoneId.systemDefault;
import static java.time.temporal.ChronoUnit.MILLIS;
import static java.util.Objects.nonNull;
import static org.springframework.http.HttpHeaders.CONTENT_DISPOSITION;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.util.CollectionUtils.isEmpty;
import static se.sundsvall.document.service.Constants.ERROR_DOCUMENT_BY_REGISTRATION_NUMBER_AND_REVISION_NOT_FOUND;
import static se.sundsvall.document.service.Constants.ERROR_DOCUMENT_BY_REGISTRATION_NUMBER_NOT_FOUND;
import static se.sundsvall.document.service.Constants.ERROR_DOCUMENT_FILE_BY_ID_NOT_FOUND;
import static se.sundsvall.document.service.Constants.ERROR_DOCUMENT_FILE_BY_REGISTRATION_NUMBER_AND_REVISION_NOT_FOUND;
import static se.sundsvall.document.service.Constants.ERROR_DOCUMENT_FILE_BY_REGISTRATION_NUMBER_COULD_NOT_READ;
import static se.sundsvall.document.service.Constants.ERROR_DOCUMENT_FILE_BY_REGISTRATION_NUMBER_NOT_FOUND;
import static se.sundsvall.document.service.Constants.ERROR_STATUS_TRANSITION_NOT_ALLOWED;
import static se.sundsvall.document.service.Constants.ERROR_VALID_FROM_AFTER_VALID_TO;
import static se.sundsvall.document.service.Constants.TEMPLATE_EVENTLOG_MESSAGE_CONFIDENTIALITY_UPDATED_ON_DOCUMENT;
import static se.sundsvall.document.service.Constants.TEMPLATE_EVENTLOG_MESSAGE_RESPONSIBILITIES_UPDATED_ON_DOCUMENT;
import static se.sundsvall.document.service.Constants.TEMPLATE_EVENTLOG_MESSAGE_STATUS_UPDATED_ON_DOCUMENT;
import static se.sundsvall.document.service.InclusionFilter.CONFIDENTIAL_AND_PUBLIC;
import static se.sundsvall.document.service.mapper.DocumentMapper.applyUpdate;
import static se.sundsvall.document.service.mapper.DocumentMapper.copyDocumentEntity;
import static se.sundsvall.document.service.mapper.DocumentMapper.toConfidentialityEmbeddable;
import static se.sundsvall.document.service.mapper.DocumentMapper.toDocument;
import static se.sundsvall.document.service.mapper.DocumentMapper.toDocumentDataEntities;
import static se.sundsvall.document.service.mapper.DocumentMapper.toDocumentEntity;
import static se.sundsvall.document.service.mapper.DocumentMapper.toDocumentResponsibilities;
import static se.sundsvall.document.service.mapper.DocumentMapper.toDocumentResponsibilityEntities;
import static se.sundsvall.document.service.mapper.DocumentMapper.toInclusionFilter;
import static se.sundsvall.document.service.mapper.DocumentMapper.toPagedDocumentResponse;
import static se.sundsvall.document.service.mapper.EventlogMapper.toEvent;

@Service
@Transactional
public class DocumentService {

	private static final String ERROR_DOCUMENT_TYPE_NOT_FOUND = "Document type with identifier %s was not found within municipality with id %s";
	private static final Logger LOGGER = LoggerFactory.getLogger(DocumentService.class);

	private final BinaryStore binaryStore;
	private final DocumentRepository documentRepository;
	private final DocumentResponsibilityRepository documentResponsibilityRepository;
	private final DocumentTypeRepository documentTypeRepository;
	private final RegistrationNumberService registrationNumberService;
	private final DocumentStatusPolicy statusPolicy;
	private final Optional<EventLogClient> eventLogClient;
	private final Optional<EventlogProperties> eventLogProperties;
	private final ApplicationEventPublisher eventPublisher;

	public DocumentService(
		final BinaryStore binaryStore,
		final DocumentRepository documentRepository,
		final DocumentResponsibilityRepository documentResponsibilityRepository,
		final DocumentTypeRepository documentTypeRepository,
		final RegistrationNumberService registrationNumberService,
		final DocumentStatusPolicy statusPolicy,
		final Optional<EventLogClient> eventLogClient,
		final Optional<EventlogProperties> eventLogProperties,
		final ApplicationEventPublisher eventPublisher) {

		this.binaryStore = binaryStore;
		this.documentRepository = documentRepository;
		this.documentResponsibilityRepository = documentResponsibilityRepository;
		this.documentTypeRepository = documentTypeRepository;
		this.registrationNumberService = registrationNumberService;
		this.statusPolicy = statusPolicy;
		this.eventLogClient = eventLogClient;
		this.eventLogProperties = eventLogProperties;
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

	public void readFile(String registrationNumber, String documentDataId, boolean includeConfidential, boolean includeNonPublic, AccessContext accessContext, HttpServletResponse response, String municipalityId) {

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
		publishAccessEvent(documentEntity, documentDataId, accessContext);
	}

	public void readFile(String registrationNumber, int revision, String documentDataId, boolean includeConfidential, AccessContext accessContext, HttpServletResponse response, String municipalityId) {

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
		publishAccessEvent(documentEntity, documentDataId, accessContext);
	}

	public Document addOrReplaceFile(String registrationNumber, DocumentDataCreateRequest documentDataCreateRequest, MultipartFile documentFile, String municipalityId) {
		return addOrReplaceFiles(registrationNumber, documentDataCreateRequest, DocumentFiles.create().withFiles(List.of(documentFile)), municipalityId);
	}

	public Document addOrReplaceFiles(String registrationNumber, DocumentDataCreateRequest documentDataCreateRequest, DocumentFiles documentFiles, String municipalityId) {

		final var documentEntity = documentRepository.findTopByMunicipalityIdAndRegistrationNumberAndConfidentialityConfidentialInOrderByRevisionDesc(municipalityId, registrationNumber, CONFIDENTIAL_AND_PUBLIC.getValue())
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, ERROR_DOCUMENT_BY_REGISTRATION_NUMBER_NOT_FOUND.formatted(registrationNumber)));

		// Materialize all incoming files into documentData entities up front — one storage write per file.
		final var newDocumentDataEntities = toDocumentDataEntities(documentFiles, binaryStore, municipalityId);

		// Do not update existing entity, create a new revision instead.
		final var newDocumentEntity = copyDocumentEntity(documentEntity, binaryStore)
			.withRevision(documentEntity.getRevision() + 1)
			.withCreatedBy(documentDataCreateRequest.getCreatedBy());

		// Add/replace each new documentData element against the single new revision so N files produce 1 revision.
		newDocumentDataEntities.forEach(entity -> addOrReplaceDocumentDataEntity(newDocumentEntity, entity));

		return toDocumentWithResponsibilities(documentRepository.save(newDocumentEntity));
	}

	public void deleteFile(String registrationNumber, String documentDataId, String municipalityId) {

		final var documentEntity = documentRepository.findTopByMunicipalityIdAndRegistrationNumberAndConfidentialityConfidentialInOrderByRevisionDesc(municipalityId, registrationNumber, toInclusionFilter(true))
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, ERROR_DOCUMENT_BY_REGISTRATION_NUMBER_NOT_FOUND.formatted(registrationNumber)));

		if (isEmpty(documentEntity.getDocumentData())) {
			throw Problem.valueOf(NOT_FOUND, ERROR_DOCUMENT_FILE_BY_REGISTRATION_NUMBER_NOT_FOUND.formatted(registrationNumber));
		}

		// Do not update existing entity, create a new revision instead.
		final var newDocumentEntity = copyDocumentEntity(documentEntity, binaryStore)
			.withRevision(documentEntity.getRevision() + 1)
			.withDocumentData(documentEntity.getDocumentData().stream()
				.filter(docDataEntity -> !docDataEntity.getId().equals(documentDataId)) // Create a new documentData list without the "deleted" object.
				.map(d -> DocumentMapper.copyDocumentDataEntity(d, binaryStore))
				.toList());

		// If size on new list is the same as the old list, nothing was removed in new revision.
		if (documentEntity.getDocumentData().size() == newDocumentEntity.getDocumentData().size()) {
			throw Problem.valueOf(NOT_FOUND, ERROR_DOCUMENT_FILE_BY_ID_NOT_FOUND.formatted(documentDataId));
		}

		documentRepository.save(newDocumentEntity);
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

	public Document publish(String registrationNumber, String changedBy, String municipalityId) {
		return transitionStatus(registrationNumber, changedBy, municipalityId, "publish", entity -> {
			if (entity.getStatus() != DocumentStatus.DRAFT) {
				throw Problem.valueOf(CONFLICT, ERROR_STATUS_TRANSITION_NOT_ALLOWED.formatted(entity.getStatus(), "publish", registrationNumber));
			}
			return statusPolicy.resolvePublishedStatus(entity.getValidFrom(), entity.getValidTo(), registrationNumber);
		});
	}

	public Document revoke(String registrationNumber, String changedBy, String municipalityId) {
		return transitionStatus(registrationNumber, changedBy, municipalityId, "revoke", entity -> {
			if (entity.getStatus() != DocumentStatus.ACTIVE && entity.getStatus() != DocumentStatus.SCHEDULED) {
				throw Problem.valueOf(CONFLICT, ERROR_STATUS_TRANSITION_NOT_ALLOWED.formatted(entity.getStatus(), "revoke", registrationNumber));
			}
			return DocumentStatus.REVOKED;
		});
	}

	public Document unrevoke(String registrationNumber, String changedBy, String municipalityId) {
		return transitionStatus(registrationNumber, changedBy, municipalityId, "unrevoke", entity -> {
			if (entity.getStatus() != DocumentStatus.REVOKED) {
				throw Problem.valueOf(CONFLICT, ERROR_STATUS_TRANSITION_NOT_ALLOWED.formatted(entity.getStatus(), "unrevoke", registrationNumber));
			}
			return statusPolicy.resolvePublishedStatus(entity.getValidFrom(), entity.getValidTo(), registrationNumber);
		});
	}

	public void updateConfidentiality(String registrationNumber, ConfidentialityUpdateRequest confidentialityUpdateRequest, String municipalityId) {

		final var documentEntities = documentRepository.findByMunicipalityIdAndRegistrationNumberAndConfidentialityConfidentialIn(municipalityId, registrationNumber, CONFIDENTIAL_AND_PUBLIC.getValue());

		final var newConfidentialitySettings = toConfidentialityEmbeddable(confidentialityUpdateRequest);

		// Set confidentiality settings on document-level.
		documentEntities.forEach(documentEntity -> documentEntity.setConfidentiality(newConfidentialitySettings));

		// Send info to Eventlog.
		eventLogForDocument(registrationNumber, confidentialityUpdateRequest, municipalityId);

		documentRepository.saveAll(documentEntities);
	}

	public void updateResponsibilities(final String registrationNumber, final DocumentResponsibilitiesUpdateRequest request, final String municipalityId) {

		if (!documentRepository.existsByMunicipalityIdAndRegistrationNumber(municipalityId, registrationNumber)) {
			throw Problem.valueOf(NOT_FOUND, ERROR_DOCUMENT_BY_REGISTRATION_NUMBER_NOT_FOUND.formatted(registrationNumber));
		}

		final var oldResponsibilities = documentResponsibilityRepository.findByMunicipalityIdAndRegistrationNumberOrderByUsernameAsc(municipalityId, registrationNumber);
		final var newResponsibilities = toDocumentResponsibilityEntities(request.getResponsibilities(), municipalityId, registrationNumber, request.getChangedBy());

		documentResponsibilityRepository.deleteByMunicipalityIdAndRegistrationNumber(municipalityId, registrationNumber);
		// Force DELETE before INSERT so the (municipality_id, registration_number, username) unique constraint
		// isn't violated when a username is both removed and re-added in the same call.
		documentResponsibilityRepository.flush();
		documentResponsibilityRepository.saveAll(newResponsibilities);

		eventLogForResponsibilities(registrationNumber, request.getChangedBy(), oldResponsibilities, newResponsibilities, municipalityId);
	}

	public PagedDocumentResponse searchByParameters(final DocumentParameters parameters) {
		var pageable = PageRequest.of(parameters.getPage() - 1, parameters.getLimit(), parameters.sort());
		final var effectiveStatuses = statusPolicy.effectivePublishedStatuses(parameters.getStatuses());
		return toPagedDocumentResponseWithResponsibilities(documentRepository.searchByParameters(parameters, pageable, effectiveStatuses));
	}

	private void addFileContentToResponse(DocumentDataEntity documentDataEntity, HttpServletResponse response) {
		response.addHeader(CONTENT_TYPE, documentDataEntity.getMimeType());
		response.addHeader(CONTENT_DISPOSITION, ContentDisposition.attachment()
			.filename(documentDataEntity.getFileName(), StandardCharsets.UTF_8)
			.build()
			.toString());
		response.setContentLength((int) documentDataEntity.getFileSizeInBytes());

		final var ref = new StorageRef(documentDataEntity.getStorageBackend(), documentDataEntity.getStorageLocator());
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

	private void publishAccessEvent(DocumentEntity documentEntity, String documentDataId, AccessContext accessContext) {
		if (accessContext == null || !accessContext.countStats()) {
			return;
		}
		eventPublisher.publishEvent(new DocumentAccessedEvent(
			documentEntity.getMunicipalityId(),
			documentEntity.getId(),
			documentEntity.getRegistrationNumber(),
			documentEntity.getRevision(),
			documentDataId,
			accessContext.accessType(),
			accessContext.sentBy(),
			now(systemDefault()).truncatedTo(MILLIS)));
	}

	private Document toDocumentWithResponsibilities(final DocumentEntity documentEntity) {
		final var responsibilities = documentResponsibilityRepository.findByMunicipalityIdAndRegistrationNumberOrderByUsernameAsc(documentEntity.getMunicipalityId(), documentEntity.getRegistrationNumber());
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

	private void eventLogForDocument(String registrationNumber, ConfidentialityUpdateRequest confidentialityUpdateRequest, String municipalityId) {
		eventLogProperties.ifPresent(props -> eventLogClient.ifPresent(client -> client.createEvent(municipalityId, props.logKeyUuid(), toEvent(
			UPDATE,
			registrationNumber,
			TEMPLATE_EVENTLOG_MESSAGE_CONFIDENTIALITY_UPDATED_ON_DOCUMENT
				.formatted(confidentialityUpdateRequest.getConfidential(), confidentialityUpdateRequest.getLegalCitation(), registrationNumber, confidentialityUpdateRequest.getChangedBy()),
			confidentialityUpdateRequest.getChangedBy()))));
	}

	private void eventLogForResponsibilities(final String registrationNumber, final String changedBy, final List<DocumentResponsibilityEntity> oldResponsibilities, final List<DocumentResponsibilityEntity> newResponsibilities,
		final String municipalityId) {
		final var sortedNewResponsibilities = newResponsibilities.stream()
			.sorted(Comparator.comparing(DocumentResponsibilityEntity::getUsername))
			.toList();
		eventLogProperties.ifPresent(props -> eventLogClient.ifPresent(client -> client.createEvent(municipalityId, props.logKeyUuid(), toEvent(
			UPDATE,
			registrationNumber,
			TEMPLATE_EVENTLOG_MESSAGE_RESPONSIBILITIES_UPDATED_ON_DOCUMENT.formatted(toDocumentResponsibilities(oldResponsibilities), toDocumentResponsibilities(sortedNewResponsibilities), registrationNumber, changedBy),
			changedBy))));
	}

	private void eventLogForStatusChange(String registrationNumber, DocumentStatus from, DocumentStatus to, String changedBy, String municipalityId) {
		eventLogProperties.ifPresent(props -> eventLogClient.ifPresent(client -> client.createEvent(municipalityId, props.logKeyUuid(), toEvent(
			UPDATE,
			registrationNumber,
			TEMPLATE_EVENTLOG_MESSAGE_STATUS_UPDATED_ON_DOCUMENT.formatted(from, to, registrationNumber, changedBy),
			changedBy))));
	}

	private Document transitionStatus(String registrationNumber, String changedBy, String municipalityId, String action,
		java.util.function.Function<DocumentEntity, DocumentStatus> nextStatusResolver) {

		final var documentEntity = documentRepository.findTopByMunicipalityIdAndRegistrationNumberAndConfidentialityConfidentialInOrderByRevisionDesc(municipalityId, registrationNumber, CONFIDENTIAL_AND_PUBLIC.getValue())
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, ERROR_DOCUMENT_BY_REGISTRATION_NUMBER_NOT_FOUND.formatted(registrationNumber)));

		final var previousStatus = documentEntity.getStatus();
		final var newStatus = nextStatusResolver.apply(documentEntity);
		documentEntity.setStatus(newStatus);

		eventLogForStatusChange(registrationNumber, previousStatus, newStatus, changedBy, municipalityId);

		return toDocumentWithResponsibilities(documentRepository.save(documentEntity));
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

	private void addOrReplaceDocumentDataEntity(DocumentEntity documentEntity, DocumentDataEntity documentDataEntity) {

		final var documentDataList = Optional.ofNullable(documentEntity.getDocumentData()).orElse(new ArrayList<>());

		// Remove existing documentData element, if the filename already exists.
		documentDataList.removeIf(documentData -> Strings.CI.equals(documentData.getFileName(), documentDataEntity.getFileName()));

		// Add new documentData element.
		documentDataList.add(documentDataEntity);

		documentEntity.setDocumentData(documentDataList);
	}
}
