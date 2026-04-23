package se.sundsvall.document.service;

import java.util.EnumSet;
import java.util.Set;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.document.api.model.ConfidentialityUpdateRequest;
import se.sundsvall.document.api.model.Document;
import se.sundsvall.document.api.model.DocumentStatus;
import se.sundsvall.document.integration.db.DocumentRepository;
import se.sundsvall.document.integration.db.model.DocumentEntity;
import se.sundsvall.document.service.indexing.DocumentIndexingEvent;

import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static se.sundsvall.document.service.Constants.ERROR_DOCUMENT_BY_REGISTRATION_NUMBER_AND_REVISION_NOT_FOUND;
import static se.sundsvall.document.service.Constants.ERROR_DOCUMENT_BY_REGISTRATION_NUMBER_NOT_FOUND;
import static se.sundsvall.document.service.Constants.ERROR_STATUS_TRANSITION_NOT_ALLOWED;
import static se.sundsvall.document.service.InclusionFilter.CONFIDENTIAL_AND_PUBLIC;
import static se.sundsvall.document.service.mapper.DocumentMapper.toConfidentialityEmbeddable;

/**
 * Status lifecycle + confidentiality changes — things that flip a visibility/state flag on an
 * existing revision without touching files or responsibilities.
 * <ul>
 * <li>{@link #publish}/{@link #revoke}/{@link #unrevoke} — single-revision status transitions,
 * validated against the allowed from-statuses. Audited via EventLog and re-indexed.</li>
 * <li>{@link #updateConfidentiality} — bulk updates every revision of the document so the ES
 * {@code confidential} filter stays consistent.</li>
 * </ul>
 */
@Service
@Transactional
public class DocumentStatusService {

	private static final Logger LOGGER = LoggerFactory.getLogger(DocumentStatusService.class);

	private final DocumentRepository documentRepository;
	private final DocumentResponseHydrator responseHydrator;
	private final DocumentStatusPolicy statusPolicy;
	private final DocumentEventPublisher eventPublisher;
	private final ApplicationEventPublisher applicationEventPublisher;

	public DocumentStatusService(
		final DocumentRepository documentRepository,
		final DocumentResponseHydrator responseHydrator,
		final DocumentStatusPolicy statusPolicy,
		final DocumentEventPublisher eventPublisher,
		final ApplicationEventPublisher applicationEventPublisher) {

		this.documentRepository = documentRepository;
		this.responseHydrator = responseHydrator;
		this.statusPolicy = statusPolicy;
		this.eventPublisher = eventPublisher;
		this.applicationEventPublisher = applicationEventPublisher;
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

		LOGGER.info("Updating confidentiality (registrationNumber='{}', revisions={}, confidential={}, legalCitation='{}', updatedBy='{}')",
			registrationNumber, documentEntities.size(),
			confidentialityUpdateRequest.getConfidential(), confidentialityUpdateRequest.getLegalCitation(),
			confidentialityUpdateRequest.getUpdatedBy());

		documentEntities.forEach(documentEntity -> documentEntity.setConfidentiality(newConfidentialitySettings));

		eventPublisher.logConfidentialityChange(registrationNumber, confidentialityUpdateRequest, municipalityId);

		documentRepository.saveAll(documentEntities);
		// Re-index every revision — the confidential flag is part of ES filters, so all must stay in sync.
		documentEntities.forEach(documentEntity -> applicationEventPublisher.publishEvent(DocumentIndexingEvent.reindex(documentEntity.getId())));
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
		LOGGER.info("Status transition '{}' (registrationNumber='{}', revision={}, {}→{}, updatedBy='{}')",
			action, registrationNumber, documentEntity.getRevision(), previousStatus, newStatus, updatedBy);

		eventPublisher.logStatusChange(registrationNumber, documentEntity.getRevision(), previousStatus, newStatus, updatedBy, municipalityId);

		final var saved = documentRepository.save(documentEntity);
		applicationEventPublisher.publishEvent(DocumentIndexingEvent.reindex(saved.getId()));
		return responseHydrator.hydrate(saved);
	}

	private DocumentEntity findDocumentForStatusTransition(String municipalityId, String registrationNumber, Integer revision) {
		if (revision == null) {
			return documentRepository.findTopByMunicipalityIdAndRegistrationNumberAndConfidentialityConfidentialInOrderByRevisionDesc(municipalityId, registrationNumber, CONFIDENTIAL_AND_PUBLIC.getValue())
				.orElseThrow(() -> Problem.valueOf(NOT_FOUND, ERROR_DOCUMENT_BY_REGISTRATION_NUMBER_NOT_FOUND.formatted(registrationNumber)));
		}
		return documentRepository.findByMunicipalityIdAndRegistrationNumberAndRevisionAndConfidentialityConfidentialIn(municipalityId, registrationNumber, revision, CONFIDENTIAL_AND_PUBLIC.getValue())
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, ERROR_DOCUMENT_BY_REGISTRATION_NUMBER_AND_REVISION_NOT_FOUND.formatted(registrationNumber, revision)));
	}
}
