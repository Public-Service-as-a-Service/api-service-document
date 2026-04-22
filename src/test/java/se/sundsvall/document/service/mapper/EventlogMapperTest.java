package se.sundsvall.document.service.mapper;

import generated.se.sundsvall.eventlog.Metadata;
import org.junit.jupiter.api.Test;

import static generated.se.sundsvall.eventlog.EventType.UPDATE;
import static java.time.OffsetDateTime.now;
import static java.time.ZoneId.systemDefault;
import static java.time.temporal.ChronoUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.assertj.core.api.Assertions.within;

class EventlogMapperTest {

	@Test
	void toEvent() {

		// Arrange
		final var eventType = UPDATE;
		final var message = "This is a message";
		final var executedBy = "b0000000-0000-0000-0000-0000000000ac";
		final var registrationNumber = "2023-2281-1";

		// Act
		final var result = EventlogMapper.toEvent(eventType, registrationNumber, message, executedBy);

		// Assert
		assertThat(result).isNotNull();
		assertThat(result.getExpires()).isCloseTo(now(systemDefault()).plusYears(10), within(2, SECONDS));
		assertThat(result.getType()).isEqualTo(eventType);
		assertThat(result.getMessage()).isEqualTo(message);
		assertThat(result.getOwner()).isEqualTo("Document");
		assertThat(result.getMetadata())
			.extracting(Metadata::getKey, Metadata::getValue)
			.containsExactlyInAnyOrder(
				tuple("RegistrationNumber", registrationNumber),
				tuple("ExecutedBy", executedBy));
	}
}
