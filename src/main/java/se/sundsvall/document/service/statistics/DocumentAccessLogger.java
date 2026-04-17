package se.sundsvall.document.service.statistics;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import se.sundsvall.document.integration.db.DocumentAccessLogRepository;
import se.sundsvall.document.integration.db.model.DocumentAccessLogEntity;

@Component
public class DocumentAccessLogger {

	private static final Logger LOGGER = LoggerFactory.getLogger(DocumentAccessLogger.class);

	private final DocumentAccessLogRepository repository;

	public DocumentAccessLogger(final DocumentAccessLogRepository repository) {
		this.repository = repository;
	}

	@Async(StatisticsConfiguration.STATISTICS_EXECUTOR)
	@EventListener
	public void onDocumentAccessed(final DocumentAccessedEvent event) {
		try {
			repository.save(DocumentAccessLogEntity.create()
				.withMunicipalityId(event.municipalityId())
				.withDocumentId(event.documentId())
				.withRegistrationNumber(event.registrationNumber())
				.withRevision(event.revision())
				.withDocumentDataId(event.documentDataId())
				.withAccessType(event.accessType())
				.withAccessedBy(event.accessedBy())
				.withAccessedAt(event.accessedAt()));
		} catch (final RuntimeException e) {
			// Statistics must never affect the user-facing response — swallow and log.
			LOGGER.warn("Failed to persist document access log for registrationNumber={}, revision={}, documentDataId={}",
				event.registrationNumber(), event.revision(), event.documentDataId(), e);
		}
	}
}
