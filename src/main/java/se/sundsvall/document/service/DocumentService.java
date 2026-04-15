package se.sundsvall.document.service;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.apache.commons.lang3.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import se.sundsvall.document.service.storage.BinaryStore;
import se.sundsvall.document.service.storage.StorageRef;

import static generated.se.sundsvall.eventlog.EventType.UPDATE;
import static java.util.Objects.nonNull;
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
import static se.sundsvall.document.service.Constants.TEMPLATE_EVENTLOG_MESSAGE_CONFIDENTIALITY_UPDATED_ON_DOCUMENT;
import static se.sundsvall.document.service.Constants.TEMPLATE_EVENTLOG_MESSAGE_RESPONSIBILITIES_UPDATED_ON_DOCUMENT;
import static se.sundsvall.document.service.InclusionFilter.CONFIDENTIAL_AND_PUBLIC;
import static se.sundsvall.document.service.mapper.DocumentMapper.copyDocumentEntity;
import static se.sundsvall.document.service.mapper.DocumentMapper.toConfidentialityEmbeddable;
import static se.sundsvall.document.service.mapper.DocumentMapper.toDocument;
import static se.sundsvall.document.service.mapper.DocumentMapper.toDocumentDataEntities;
import static se.sundsvall.document.service.mapper.DocumentMapper.toDocumentDataEntity;
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
	private final Optional<EventLogClient> eventLogClient;
	private final Optional<EventlogProperties> eventLogProperties;

	public DocumentService(
		final BinaryStore binaryStore,
		final DocumentRepository documentRepository,
		final DocumentResponsibilityRepository documentResponsibilityRepository,
		final DocumentTypeRepository documentTypeRepository,
		final RegistrationNumberService registrationNumberService,
		final Optional<EventLogClient> eventLogClient,
		final Optional<EventlogProperties> eventLogProperties) {

		this.binaryStore = binaryStore;
		this.documentRepository = documentRepository;
		this.documentResponsibilityRepository = documentResponsibilityRepository;
		this.documentTypeRepository = documentTypeRepository;
		this.registrationNumberService = registrationNumberService;
		this.eventLogClient = eventLogClient;
		this.eventLogProperties = eventLogProperties;
	}

	public Document create(final DocumentCreateRequest documentCreateRequest, final DocumentFiles documentFiles, final String municipalityId) {

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

	public Document read(String registrationNumber, boolean includeConfidential, String municipalityId) {

		final var documentEntity = documentRepository.findTopByMunicipalityIdAndRegistrationNumberAndConfidentialityConfidentialInOrderByRevisionDesc(municipalityId, registrationNumber, toInclusionFilter(includeConfidential))
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, ERROR_DOCUMENT_BY_REGISTRATION_NUMBER_NOT_FOUND.formatted(registrationNumber)));

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
		return toPagedDocumentResponseWithResponsibilities(documentRepository.search(query, includeConfidential, onlyLatestRevision, pageable, municipalityId));
	}

	public void readFile(String registrationNumber, String documentDataId, boolean includeConfidential, HttpServletResponse response, String municipalityId) {

		final var documentEntity = documentRepository.findTopByMunicipalityIdAndRegistrationNumberAndConfidentialityConfidentialInOrderByRevisionDesc(municipalityId, registrationNumber, toInclusionFilter(includeConfidential))
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, ERROR_DOCUMENT_BY_REGISTRATION_NUMBER_NOT_FOUND.formatted(registrationNumber)));

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

		final var documentEntity = documentRepository.findTopByMunicipalityIdAndRegistrationNumberAndConfidentialityConfidentialInOrderByRevisionDesc(municipalityId, registrationNumber, CONFIDENTIAL_AND_PUBLIC.getValue())
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, ERROR_DOCUMENT_BY_REGISTRATION_NUMBER_NOT_FOUND.formatted(registrationNumber)));

		// Create documentData element to add/replace.
		final var newDocumentDataEntity = toDocumentDataEntity(documentFile, binaryStore, municipalityId);

		// Do not update existing entity, create a new revision instead.
		final var newDocumentEntity = copyDocumentEntity(documentEntity, binaryStore)
			.withRevision(documentEntity.getRevision() + 1)
			.withCreatedBy(documentDataCreateRequest.getCreatedBy());

		// Adds the new documentData element if the file name doesn't exist already, otherwise the old element is replaced.
		addOrReplaceDocumentDataEntity(newDocumentEntity, newDocumentDataEntity);

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

		// Do not update existing entity, create a new revision instead.
		final var newDocumentEntity = toDocumentEntity(documentUpdateRequest, existingDocumentEntity, binaryStore);
		if (nonNull(documentUpdateRequest.getType())) {
			final var documentTypeEntity = documentTypeRepository.findByMunicipalityIdAndType(municipalityId, documentUpdateRequest.getType())
				.orElseThrow(() -> Problem.valueOf(NOT_FOUND, ERROR_DOCUMENT_TYPE_NOT_FOUND.formatted(documentUpdateRequest.getType(), municipalityId)));

			newDocumentEntity.setType(documentTypeEntity);
		}

		return toDocumentWithResponsibilities(documentRepository.save(newDocumentEntity));
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

		final var oldResponsibilities = documentResponsibilityRepository.findByMunicipalityIdAndRegistrationNumberOrderByPrincipalTypeAscPrincipalIdAsc(municipalityId, registrationNumber);
		final var newResponsibilities = toDocumentResponsibilityEntities(request.getResponsibilities(), municipalityId, registrationNumber, request.getChangedBy());

		documentResponsibilityRepository.deleteByMunicipalityIdAndRegistrationNumber(municipalityId, registrationNumber);
		documentResponsibilityRepository.saveAll(newResponsibilities);

		eventLogForResponsibilities(registrationNumber, request.getChangedBy(), oldResponsibilities, newResponsibilities, municipalityId);
	}

	public PagedDocumentResponse searchByParameters(final DocumentParameters parameters) {
		var pageable = PageRequest.of(parameters.getPage() - 1, parameters.getLimit(), parameters.sort());
		return toPagedDocumentResponseWithResponsibilities(documentRepository.searchByParameters(parameters, pageable));
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

	private Document toDocumentWithResponsibilities(final DocumentEntity documentEntity) {
		final var responsibilities = documentResponsibilityRepository.findByMunicipalityIdAndRegistrationNumberOrderByPrincipalTypeAscPrincipalIdAsc(documentEntity.getMunicipalityId(), documentEntity.getRegistrationNumber());
		return toDocument(documentEntity, responsibilities);
	}

	private PagedDocumentResponse toPagedDocumentResponseWithResponsibilities(final org.springframework.data.domain.Page<DocumentEntity> documentEntityPage) {
		final var response = toPagedDocumentResponse(documentEntityPage);
		if (response == null || isEmpty(response.getDocuments())) {
			return response;
		}

		response.getDocuments().forEach(document -> document.setResponsibilities(toDocumentResponsibilities(documentResponsibilityRepository.findByMunicipalityIdAndRegistrationNumberOrderByPrincipalTypeAscPrincipalIdAsc(document.getMunicipalityId(),
			document.getRegistrationNumber()))));

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
			.sorted(Comparator.comparing((DocumentResponsibilityEntity r) -> r.getPrincipalType().name())
				.thenComparing(DocumentResponsibilityEntity::getPrincipalId))
			.toList();
		eventLogProperties.ifPresent(props -> eventLogClient.ifPresent(client -> client.createEvent(municipalityId, props.logKeyUuid(), toEvent(
			UPDATE,
			registrationNumber,
			TEMPLATE_EVENTLOG_MESSAGE_RESPONSIBILITIES_UPDATED_ON_DOCUMENT.formatted(toDocumentResponsibilities(oldResponsibilities), toDocumentResponsibilities(sortedNewResponsibilities), registrationNumber, changedBy),
			changedBy))));
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
