package se.sundsvall.document.service;

import co.elastic.clients.elasticsearch._types.FieldValue;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
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
import se.sundsvall.document.api.model.PagedDocumentMatchResponse;
import se.sundsvall.document.api.model.PagedDocumentResponse;
import se.sundsvall.document.integration.db.DocumentDataRepository;
import se.sundsvall.document.integration.db.DocumentRepository;
import se.sundsvall.document.integration.db.DocumentResponsibilityRepository;
import se.sundsvall.document.integration.db.DocumentTypeRepository;
import se.sundsvall.document.integration.db.model.DocumentEntity;
import se.sundsvall.document.integration.db.model.DocumentResponsibilityEntity;
import se.sundsvall.document.integration.elasticsearch.DocumentIndexEntity;
import se.sundsvall.document.service.extraction.TextExtractor;
import se.sundsvall.document.service.indexing.DocumentIndexingEvent;
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
import static se.sundsvall.document.service.mapper.DocumentMapper.toPagedDocumentMatchResponse;
import static se.sundsvall.document.service.mapper.DocumentMapper.toPagedDocumentResponse;

@Service
@Transactional
public class DocumentService {

	private static final String ERROR_DOCUMENT_TYPE_NOT_FOUND = "Document type with identifier %s was not found within municipality with id %s";

	private final BinaryStore binaryStore;
	private final DocumentRepository documentRepository;
	private final DocumentResponsibilityRepository documentResponsibilityRepository;
	private final DocumentTypeRepository documentTypeRepository;
	private final DocumentDataRepository documentDataRepository;
	private final RegistrationNumberService registrationNumberService;
	private final DocumentStatusPolicy statusPolicy;
	private final DocumentEventPublisher eventPublisher;
	private final TextExtractor textExtractor;
	private final ApplicationEventPublisher applicationEventPublisher;
	// Optional so the junit profile (which excludes the ES auto-configurations) can still boot
	// the Spring context for WebTestClient tests that don't exercise search. In production ES is
	// always present; {@link #search(String, boolean, boolean, Pageable, String)} will throw if
	// it is ever called without ES wired.
	private final java.util.Optional<ElasticsearchOperations> elasticsearchOperations;

	public DocumentService(
		final BinaryStore binaryStore,
		final DocumentRepository documentRepository,
		final DocumentResponsibilityRepository documentResponsibilityRepository,
		final DocumentTypeRepository documentTypeRepository,
		final DocumentDataRepository documentDataRepository,
		final RegistrationNumberService registrationNumberService,
		final DocumentStatusPolicy statusPolicy,
		final DocumentEventPublisher eventPublisher,
		final TextExtractor textExtractor,
		final ApplicationEventPublisher applicationEventPublisher,
		final java.util.Optional<ElasticsearchOperations> elasticsearchOperations) {

		this.binaryStore = binaryStore;
		this.documentRepository = documentRepository;
		this.documentResponsibilityRepository = documentResponsibilityRepository;
		this.documentTypeRepository = documentTypeRepository;
		this.documentDataRepository = documentDataRepository;
		this.registrationNumberService = registrationNumberService;
		this.statusPolicy = statusPolicy;
		this.eventPublisher = eventPublisher;
		this.textExtractor = textExtractor;
		this.applicationEventPublisher = applicationEventPublisher;
		this.elasticsearchOperations = elasticsearchOperations;
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

		applicationEventPublisher.publishEvent(DocumentIndexingEvent.reindex(savedDocumentEntity.getId()));

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
		final var hits = runFulltextSearch(query, includeConfidential, municipalityId, pageable);
		return hydrateHitsToPagedDocumentResponse(hits, pageable, onlyLatestRevision);
	}

	public PagedDocumentMatchResponse searchFileMatches(String query, boolean includeConfidential, boolean onlyLatestRevision, Pageable pageable, String municipalityId) {
		final var hits = runFulltextSearch(query, includeConfidential, municipalityId, pageable);
		return toPagedDocumentMatchResponse(hits, pageable, onlyLatestRevision);
	}

	private PagedDocumentResponse hydrateHitsToPagedDocumentResponse(org.springframework.data.elasticsearch.core.SearchHits<DocumentIndexEntity> hits, Pageable pageable, boolean onlyLatestRevision) {
		// Collapse the per-file hits into unique documents, preserving ES relevance order.
		final var orderedDocumentIds = hits.getSearchHits().stream()
			.map(h -> h.getContent().getDocumentId())
			.distinct()
			.toList();

		if (orderedDocumentIds.isEmpty()) {
			return toPagedDocumentResponseWithResponsibilities(new PageImpl<>(List.of(), pageable, 0));
		}

		final var byId = documentRepository.findAllById(orderedDocumentIds).stream()
			.collect(Collectors.toMap(DocumentEntity::getId, e -> e, (a, b) -> a));

		var hydrated = orderedDocumentIds.stream()
			.map(byId::get)
			.filter(Objects::nonNull)
			.toList();

		if (onlyLatestRevision) {
			// Page-local: we only compare revisions among hits on this page, so a document whose
			// latest revision lives on another page will still survive the filter here. This keeps
			// _meta.totalRecords (file-level ES hit total) coherent with what the page actually
			// shows — a global filter would require a second ES roundtrip per registrationNumber.
			hydrated = hydrated.stream()
				.collect(Collectors.groupingBy(DocumentEntity::getRegistrationNumber)).values().stream()
				.map(group -> group.stream().max(Comparator.comparingInt(DocumentEntity::getRevision)).orElseThrow())
				.toList();
		}

		return toPagedDocumentResponseWithResponsibilities(new PageImpl<>(hydrated, pageable, hits.getTotalHits()));
	}

	private org.springframework.data.elasticsearch.core.SearchHits<DocumentIndexEntity> runFulltextSearch(String query, boolean includeConfidential, String municipalityId, Pageable pageable) {
		final var effectiveStatuses = statusPolicy.effectivePublishedStatuses(null);

		final var esQuery = NativeQuery.builder()
			.withQuery(q -> q.bool(b -> {
				if (query != null && !query.isBlank()) {
					// Phrase match: the user's query must appear as adjacent tokens in at least one
					// of the listed fields. No wildcard support — callers type the phrase they want
					// and it's matched literally (modulo the standard analyzer). This is narrower
					// than the retired SQL LIKE path on purpose; wildcard / partial-token matching
					// can be reintroduced later if there's demand.
					b.must(m -> m.multiMatch(mm -> mm
						.query(query)
						.type(co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType.Phrase)
						.fields("title", "extractedText", "description", "fileName", "mimeType",
							"registrationNumber", "createdBy", "metadataKeys", "metadataValues")));
				}
				b.filter(f -> f.term(t -> t.field("municipalityId").value(municipalityId)));
				if (!includeConfidential) {
					b.filter(f -> f.term(t -> t.field("confidential").value(false)));
				}
				if (effectiveStatuses != null && !effectiveStatuses.isEmpty()) {
					b.filter(f -> f.terms(t -> t.field("status").terms(tt -> tt.value(
						effectiveStatuses.stream().map(s -> FieldValue.of(s.name())).toList()))));
				}
				return b;
			}))
			.withPageable(pageable)
			.build();

		return elasticsearchOperations
			.orElseThrow(() -> Problem.valueOf(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE, "Search is not available — Elasticsearch is not configured"))
			.search(esQuery, DocumentIndexEntity.class);
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
		applicationEventPublisher.publishEvent(DocumentIndexingEvent.reindex(saved.getId()));
		return toDocumentWithResponsibilities(saved);
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
		// Re-index every revision — the confidential flag is part of ES filters, so all must stay in sync.
		documentEntities.forEach(documentEntity -> applicationEventPublisher.publishEvent(DocumentIndexingEvent.reindex(documentEntity.getId())));
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

		final var saved = documentRepository.save(documentEntity);
		applicationEventPublisher.publishEvent(DocumentIndexingEvent.reindex(saved.getId()));
		return toDocumentWithResponsibilities(saved);
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
