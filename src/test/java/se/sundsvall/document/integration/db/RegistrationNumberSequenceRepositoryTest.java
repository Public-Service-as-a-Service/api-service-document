package se.sundsvall.document.integration.db;

import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace.NONE;

/**
 * RegistrationNumberSequenceRepositoryTest tests.
 *
 * @see /src/test/resources/db/testdata-junit.sql for data setup.
 */
@DataJpaTest
@Transactional
@AutoConfigureTestDatabase(replace = NONE)
@ActiveProfiles("junit")
@Sql(scripts = {
	"/db/scripts/truncate.sql",
	"/db/scripts/testdata-junit.sql"
})
class RegistrationNumberSequenceRepositoryTest {

	private static final String MUNICIPALITY_ID = "2321";

	@Autowired
	private RegistrationNumberSequenceRepository registrationNumberSequenceRepository;

	@Test
	void findByMunicipalityId() {

		// Act
		final var result = registrationNumberSequenceRepository.findByMunicipalityId(MUNICIPALITY_ID).orElseThrow();

		// Assert
		assertThat(result).isNotNull();
		assertThat(result.getCreated()).isEqualTo(OffsetDateTime.parse("2023-06-28T12:01:00.000+02:00"));
		assertThat(isValidUUID(result.getId())).isTrue();
		assertThat(result.getModified()).isEqualTo(OffsetDateTime.parse("2023-06-28T12:01:00.000+02:00"));
		assertThat(result.getMunicipalityId()).isEqualTo(MUNICIPALITY_ID);
		assertThat(result.getSequenceNumber()).isEqualTo(665);
	}

	@Test
	void insertIfMissingCreatesRowWhenAbsent() {

		// Arrange
		final var newMunicipalityId = "9999";
		assertThat(registrationNumberSequenceRepository.findByMunicipalityId(newMunicipalityId)).isEmpty();

		// Act
		final var inserted = registrationNumberSequenceRepository.insertIfMissing(UUID.randomUUID().toString(), newMunicipalityId, OffsetDateTime.now());

		// Assert
		assertThat(inserted).isEqualTo(1);
		final var seeded = registrationNumberSequenceRepository.findByMunicipalityId(newMunicipalityId).orElseThrow();
		assertThat(seeded.getSequenceNumber()).isZero();
		assertThat(seeded.getMunicipalityId()).isEqualTo(newMunicipalityId);
	}

	@Test
	void insertIfMissingIsIdempotentWhenRowAlreadyExists() {

		// Arrange: MUNICIPALITY_ID (2321) is pre-seeded with sequenceNumber=665 from testdata-junit.sql.

		// Act
		final var inserted = registrationNumberSequenceRepository.insertIfMissing(UUID.randomUUID().toString(), MUNICIPALITY_ID, OffsetDateTime.now());

		// Assert
		assertThat(inserted).isZero(); // INSERT IGNORE silently skipped.
		final var untouched = registrationNumberSequenceRepository.findByMunicipalityId(MUNICIPALITY_ID).orElseThrow();
		assertThat(untouched.getSequenceNumber()).isEqualTo(665); // unchanged.
	}

	private boolean isValidUUID(final String value) {
		try {
			UUID.fromString(String.valueOf(value));
		} catch (final Exception e) {
			return false;
		}

		return true;
	}
}
