package se.sundsvall.document.service;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;
import se.sundsvall.document.api.model.ConfidentialityUpdateRequest;
import se.sundsvall.document.api.model.DocumentStatus;
import se.sundsvall.document.integration.db.model.DocumentResponsibilityEntity;
import se.sundsvall.document.integration.eventlog.EventLogClient;
import se.sundsvall.document.integration.eventlog.configuration.EventlogProperties;

import static generated.se.sundsvall.eventlog.EventType.UPDATE;
import static se.sundsvall.document.service.Constants.TEMPLATE_EVENTLOG_MESSAGE_CONFIDENTIALITY_UPDATED_ON_DOCUMENT;
import static se.sundsvall.document.service.Constants.TEMPLATE_EVENTLOG_MESSAGE_RESPONSIBILITIES_UPDATED_ON_DOCUMENT;
import static se.sundsvall.document.service.Constants.TEMPLATE_EVENTLOG_MESSAGE_STATUS_UPDATED_ON_DOCUMENT;
import static se.sundsvall.document.service.mapper.DocumentMapper.toDocumentResponsibilities;
import static se.sundsvall.document.service.mapper.EventlogMapper.toEvent;

@Component
public class DocumentEventPublisher {

	private final Optional<EventLogClient> eventLogClient;
	private final Optional<EventlogProperties> eventLogProperties;

	public DocumentEventPublisher(
		final Optional<EventLogClient> eventLogClient,
		final Optional<EventlogProperties> eventLogProperties) {

		this.eventLogClient = eventLogClient;
		this.eventLogProperties = eventLogProperties;
	}

	public void logConfidentialityChange(final String registrationNumber, final ConfidentialityUpdateRequest request, final String municipalityId) {
		publish(municipalityId, registrationNumber, request.getChangedBy(),
			TEMPLATE_EVENTLOG_MESSAGE_CONFIDENTIALITY_UPDATED_ON_DOCUMENT
				.formatted(request.getConfidential(), request.getLegalCitation(), registrationNumber, request.getChangedBy()));
	}

	public void logResponsibilitiesChange(final String registrationNumber, final String changedBy,
		final List<DocumentResponsibilityEntity> oldResponsibilities, final List<DocumentResponsibilityEntity> newResponsibilities,
		final String municipalityId) {

		final var sortedNewResponsibilities = newResponsibilities.stream()
			.sorted(Comparator.comparing(DocumentResponsibilityEntity::getUsername))
			.toList();
		publish(municipalityId, registrationNumber, changedBy,
			TEMPLATE_EVENTLOG_MESSAGE_RESPONSIBILITIES_UPDATED_ON_DOCUMENT
				.formatted(toDocumentResponsibilities(oldResponsibilities), toDocumentResponsibilities(sortedNewResponsibilities), registrationNumber, changedBy));
	}

	public void logStatusChange(final String registrationNumber, final int revision, final DocumentStatus from, final DocumentStatus to,
		final String changedBy, final String municipalityId) {

		publish(municipalityId, registrationNumber, changedBy,
			TEMPLATE_EVENTLOG_MESSAGE_STATUS_UPDATED_ON_DOCUMENT.formatted(from, to, registrationNumber, revision, changedBy));
	}

	private void publish(final String municipalityId, final String registrationNumber, final String changedBy, final String message) {
		eventLogProperties.ifPresent(props -> eventLogClient.ifPresent(client -> client.createEvent(
			municipalityId, props.logKeyUuid(), toEvent(UPDATE, registrationNumber, message, changedBy))));
	}
}
