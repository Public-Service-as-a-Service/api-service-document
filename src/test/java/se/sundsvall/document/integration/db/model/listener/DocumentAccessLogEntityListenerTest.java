package se.sundsvall.document.integration.db.model.listener;

import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;
import se.sundsvall.document.integration.db.model.DocumentAccessLogEntity;

import static java.time.OffsetDateTime.now;
import static java.time.temporal.ChronoUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class DocumentAccessLogEntityListenerTest {

	@Test
	void prePersist_setsAccessedAtWhenMissing() {
		// Arrange
		final var listener = new DocumentAccessLogEntityListener();
		final var entity = new DocumentAccessLogEntity();

		// Act
		listener.prePersist(entity);

		// Assert
		assertThat(entity.getAccessedAt()).isCloseTo(now(), within(2, SECONDS));
	}

	@Test
	void prePersist_preservesExplicitAccessedAt() {
		// Arrange — caller may set the timestamp at event-publish time so the recorded
		// value reflects when the access happened, not when the row hits the DB.
		final var listener = new DocumentAccessLogEntityListener();
		final var explicit = OffsetDateTime.parse("2026-04-17T08:30:00+02:00");
		final var entity = new DocumentAccessLogEntity().withAccessedAt(explicit);

		// Act
		listener.prePersist(entity);

		// Assert
		assertThat(entity.getAccessedAt()).isEqualTo(explicit);
	}
}
