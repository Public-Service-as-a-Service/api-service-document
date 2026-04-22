package se.sundsvall.document.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.document.api.model.DocumentResponsibilitiesUpdateRequest;
import se.sundsvall.document.integration.db.DocumentRepository;
import se.sundsvall.document.integration.db.DocumentResponsibilityRepository;

import static org.springframework.http.HttpStatus.NOT_FOUND;
import static se.sundsvall.document.service.Constants.ERROR_DOCUMENT_BY_REGISTRATION_NUMBER_NOT_FOUND;
import static se.sundsvall.document.service.mapper.DocumentMapper.toDocumentResponsibilityEntities;

/**
 * Replace-all semantics for a document's responsibilities. Responsibilities are revision-agnostic
 * — keyed on registration number — so this does not create a new revision or trigger re-indexing.
 */
@Service
@Transactional
public class DocumentResponsibilityService {

	private static final Logger LOGGER = LoggerFactory.getLogger(DocumentResponsibilityService.class);

	private final DocumentRepository documentRepository;
	private final DocumentResponsibilityRepository documentResponsibilityRepository;
	private final DocumentEventPublisher eventPublisher;

	public DocumentResponsibilityService(
		final DocumentRepository documentRepository,
		final DocumentResponsibilityRepository documentResponsibilityRepository,
		final DocumentEventPublisher eventPublisher) {

		this.documentRepository = documentRepository;
		this.documentResponsibilityRepository = documentResponsibilityRepository;
		this.eventPublisher = eventPublisher;
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
		LOGGER.info("Updated responsibilities (registrationNumber='{}', removed={}, added={}, updatedBy='{}')",
			registrationNumber, oldResponsibilities.size(), newResponsibilities.size(), request.getUpdatedBy());

		eventPublisher.logResponsibilitiesChange(registrationNumber, request.getUpdatedBy(), oldResponsibilities, newResponsibilities, municipalityId);
	}
}
