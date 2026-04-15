package se.sundsvall.document.integration.db.model.listener;

import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import se.sundsvall.document.integration.db.model.DocumentResponsibilityEntity;

import static java.time.OffsetDateTime.now;
import static java.time.ZoneId.systemDefault;
import static java.time.temporal.ChronoUnit.MILLIS;

public class DocumentResponsibilityEntityListener {

	@PrePersist
	void prePersist(final DocumentResponsibilityEntity entity) {
		entity.setCreated(now(systemDefault()).truncatedTo(MILLIS));
	}

	@PreUpdate
	void preUpdate(final DocumentResponsibilityEntity entity) {
		entity.setUpdated(now(systemDefault()).truncatedTo(MILLIS));
	}
}
