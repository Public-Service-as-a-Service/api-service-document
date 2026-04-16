package se.sundsvall.document.service;

import java.time.OffsetDateTime;
import java.time.Year;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.document.integration.db.RegistrationNumberSequenceRepository;
import se.sundsvall.document.integration.db.model.RegistrationNumberSequenceEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegistrationNumberServiceTest {

	private static final ZoneId CANONICAL_ZONE = ZoneId.of("Europe/Stockholm");

	@Mock
	private RegistrationNumberSequenceRepository registrationNumberSequenceRepositoryMock;

	@InjectMocks
	private RegistrationNumberService registrationNumberService;

	@Captor
	private ArgumentCaptor<RegistrationNumberSequenceEntity> registrationNumberSequenceEntityCaptor;

	@Test
	void generateRegistrationNumber() {

		// Arrange
		final var created = OffsetDateTime.now();
		final var id = "id";
		final var modified = OffsetDateTime.now();
		final var municipalityId = "2281";
		final var sequenceNumber = 666;
		final var sequenceEntity = RegistrationNumberSequenceEntity.create()
			.withCreated(created)
			.withId(id)
			.withModified(modified)
			.withMunicipalityId(municipalityId)
			.withSequenceNumber(sequenceNumber);

		when(registrationNumberSequenceRepositoryMock.insertIfMissing(anyString(), any(), any())).thenReturn(0); // row already existed
		when(registrationNumberSequenceRepositoryMock.findByMunicipalityId(municipalityId)).thenReturn(Optional.of(sequenceEntity));
		when(registrationNumberSequenceRepositoryMock.save(any(RegistrationNumberSequenceEntity.class))).thenReturn(sequenceEntity);

		// Act
		final var result = registrationNumberService.generateRegistrationNumber(municipalityId);

		// Assert
		assertThat(result).isEqualTo("%s-%s-%s".formatted(Year.now(CANONICAL_ZONE).getValue(), municipalityId, sequenceNumber + 1));

		verify(registrationNumberSequenceRepositoryMock).insertIfMissing(anyString(), any(), any());
		verify(registrationNumberSequenceRepositoryMock).findByMunicipalityId(municipalityId);
		verify(registrationNumberSequenceRepositoryMock).save(registrationNumberSequenceEntityCaptor.capture());

		final var capturedRegistrationNumberSequenceEntity = registrationNumberSequenceEntityCaptor.getValue();
		assertThat(capturedRegistrationNumberSequenceEntity).isNotNull();
		assertThat(capturedRegistrationNumberSequenceEntity.getMunicipalityId()).isEqualTo("2281");
		assertThat(capturedRegistrationNumberSequenceEntity.getSequenceNumber()).isEqualTo(667); // sequenceNumber incremented.
	}

	@Test
	void generateRegistrationNumberWhenNoSequenceEntityExistsYet() {

		// Arrange
		final var municipalityId = "2281";

		// Simulate the seed: insertIfMissing inserts a row (seq=0, created=now), findByMunicipalityId then returns it.
		final var seededEntity = RegistrationNumberSequenceEntity.create()
			.withId(UUID.randomUUID().toString())
			.withCreated(OffsetDateTime.now(CANONICAL_ZONE))
			.withMunicipalityId(municipalityId)
			.withSequenceNumber(0);

		when(registrationNumberSequenceRepositoryMock.insertIfMissing(anyString(), any(), any())).thenReturn(1); // newly inserted
		when(registrationNumberSequenceRepositoryMock.findByMunicipalityId(municipalityId)).thenReturn(Optional.of(seededEntity));
		when(registrationNumberSequenceRepositoryMock.save(any(RegistrationNumberSequenceEntity.class))).thenAnswer(inv -> inv.getArgument(0));

		// Act
		final var result = registrationNumberService.generateRegistrationNumber(municipalityId);

		// Assert
		assertThat(result).isEqualTo("%s-%s-%s".formatted(Year.now(CANONICAL_ZONE).getValue(), "2281", 1));

		verify(registrationNumberSequenceRepositoryMock).insertIfMissing(anyString(), any(), any());
		verify(registrationNumberSequenceRepositoryMock).findByMunicipalityId(municipalityId);
		verify(registrationNumberSequenceRepositoryMock).save(registrationNumberSequenceEntityCaptor.capture());

		final var capturedRegistrationNumberSequenceEntity = registrationNumberSequenceEntityCaptor.getValue();
		assertThat(capturedRegistrationNumberSequenceEntity).isNotNull();
		assertThat(capturedRegistrationNumberSequenceEntity.getMunicipalityId()).isEqualTo("2281");
		assertThat(capturedRegistrationNumberSequenceEntity.getSequenceNumber()).isEqualTo(1); // freshly seeded row: 0 + 1.
	}

	@Test
	void generateRegistrationNumberWhenNewYearHasBegun() {

		// Arrange
		final var created = OffsetDateTime.parse("2022-06-28T12:01:00.000+02:00");
		final var id = "id";
		final var modified = OffsetDateTime.parse("2022-06-28T12:01:00.000+02:00");
		final var municipalityId = "2281";
		final var sequenceNumber = 666;
		final var sequenceEntity = RegistrationNumberSequenceEntity.create()
			.withCreated(created)
			.withId(id)
			.withModified(modified)
			.withMunicipalityId(municipalityId)
			.withSequenceNumber(sequenceNumber);

		when(registrationNumberSequenceRepositoryMock.insertIfMissing(anyString(), any(), any())).thenReturn(0); // row already existed
		when(registrationNumberSequenceRepositoryMock.findByMunicipalityId(municipalityId)).thenReturn(Optional.of(sequenceEntity));
		when(registrationNumberSequenceRepositoryMock.save(any(RegistrationNumberSequenceEntity.class))).thenReturn(sequenceEntity);

		// Act
		final var result = registrationNumberService.generateRegistrationNumber(municipalityId);

		// Assert
		assertThat(result).isEqualTo("%s-%s-%s".formatted(Year.now(CANONICAL_ZONE).getValue(), "2281", 1));

		verify(registrationNumberSequenceRepositoryMock).insertIfMissing(anyString(), any(), any());
		verify(registrationNumberSequenceRepositoryMock).findByMunicipalityId(municipalityId);
		verify(registrationNumberSequenceRepositoryMock).save(registrationNumberSequenceEntityCaptor.capture());

		final var capturedRegistrationNumberSequenceEntity = registrationNumberSequenceEntityCaptor.getValue();
		assertThat(capturedRegistrationNumberSequenceEntity).isNotNull();
		assertThat(capturedRegistrationNumberSequenceEntity.getMunicipalityId()).isEqualTo("2281");
		assertThat(capturedRegistrationNumberSequenceEntity.getSequenceNumber()).isEqualTo(1); // sequenceNumber 1 due to new year.
	}
}
