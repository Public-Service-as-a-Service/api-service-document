package se.sundsvall.document.integration.db.model.listener;

import jakarta.persistence.PrePersist;
import se.sundsvall.document.integration.db.model.DocumentAccessLogEntity;

import static java.time.OffsetDateTime.now;
import static java.time.ZoneId.systemDefault;
import static java.time.temporal.ChronoUnit.MILLIS;

public class DocumentAccessLogEntityListener {

	@PrePersist
	void prePersist(final DocumentAccessLogEntity entity) {
		if (entity.getAccessedAt() == null) {
			entity.setAccessedAt(now(systemDefault()).truncatedTo(MILLIS));
		}
	}
}
