package se.sundsvall.document.integration.db.model.listener;

import org.junit.jupiter.api.Test;
import se.sundsvall.document.integration.db.model.DocumentResponsibilityEntity;

import static java.time.OffsetDateTime.now;
import static java.time.temporal.ChronoUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class DocumentResponsibilityEntityListenerTest {

	@Test
	void prePersist() {

		// Arrange
		final var listener = new DocumentResponsibilityEntityListener();
		final var entity = new DocumentResponsibilityEntity();

		// Act
		listener.prePersist(entity);

		// Assert
		assertThat(entity).hasAllNullFieldsOrPropertiesExcept("created");
		assertThat(entity.getCreated()).isCloseTo(now(), within(2, SECONDS));
	}

	@Test
	void preUpdate() {

		// Arrange
		final var listener = new DocumentResponsibilityEntityListener();
		final var entity = new DocumentResponsibilityEntity();

		// Act
		listener.preUpdate(entity);

		// Assert
		assertThat(entity).hasAllNullFieldsOrPropertiesExcept("updated");
		assertThat(entity.getUpdated()).isCloseTo(now(), within(2, SECONDS));
	}
}
