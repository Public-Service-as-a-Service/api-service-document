package se.sundsvall.document.service;

import java.time.OffsetDateTime;
import java.time.Year;
import java.time.ZoneId;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.sundsvall.document.integration.db.RegistrationNumberSequenceRepository;
import se.sundsvall.document.integration.db.model.RegistrationNumberSequenceEntity;

import static java.time.temporal.ChronoUnit.MILLIS;
import static se.sundsvall.document.service.Constants.TEMPLATE_REGISTRATION_NUMBER;

/**
 * Class responsible for generating unique registration numbers.
 * <p>
 * Registration numbers are created with the following format: [YYYY-MUNICIPALITY_ID-SEQUENCE]
 * <p>
 * Example:
 * If a registrationNumber is created on date 2022-10-26 for "Sundsvall municipality" (municipalityID: 2281), for the
 * first time, the registrationNumber will be: 2022-2281-1. The next generated number will be 2022-2281-2 and so on.
 * <p>
 * Every new year, the sequence will be reset to 1. (e.g. 2023-2281-1).
 */
@Service
@Transactional
public class RegistrationNumberService {

	private static final Logger LOGGER = LoggerFactory.getLogger(RegistrationNumberService.class);

	private static final int SEQUENCE_START = 1;

	// Year rollover is decided in the service's canonical zone so behavior is independent of the
	// JVM's default zone. Picking Stockholm explicitly avoids off-by-one resets around Dec 31 / Jan 1
	// when instances run with different TZ environments.
	private static final ZoneId CANONICAL_ZONE = ZoneId.of("Europe/Stockholm");

	private final RegistrationNumberSequenceRepository registrationNumberSequenceRepository;

	public RegistrationNumberService(RegistrationNumberSequenceRepository registrationNumberSequenceRepository) {
		this.registrationNumberSequenceRepository = registrationNumberSequenceRepository;
	}

	public String generateRegistrationNumber(String municipalityId) {
		final var currentYear = Year.now(CANONICAL_ZONE).getValue();

		// Guarantee a row exists before the pessimistic lock. Without this, concurrent first-time
		// callers both observe empty, both INSERT, and the unique constraint fails one of them.
		registrationNumberSequenceRepository.insertIfMissing(UUID.randomUUID().toString(), municipalityId, OffsetDateTime.now(CANONICAL_ZONE).truncatedTo(MILLIS));

		final var existing = registrationNumberSequenceRepository.findByMunicipalityId(municipalityId)
			.orElseThrow(() -> new IllegalStateException("Sequence row missing for municipalityId=" + municipalityId + " after seed"));

		final var next = nextSequenceNumber(existing, currentYear);
		if (next == SEQUENCE_START && existing.getSequenceNumber() >= SEQUENCE_START) {
			final var lastTouched = existing.getModified() != null ? existing.getModified() : existing.getCreated();
			final var lastTouchedYear = lastTouched.atZoneSameInstant(CANONICAL_ZONE).getYear();
			LOGGER.info("Registration number sequence reset on year rollover (municipalityId='{}', {}→{}, previousSequence={})",
				municipalityId, lastTouchedYear, currentYear, existing.getSequenceNumber());
		}
		final var updated = registrationNumberSequenceRepository.save(existing.withSequenceNumber(next));
		LOGGER.debug("Generated registration number (municipalityId='{}', year={}, sequence={})",
			municipalityId, currentYear, updated.getSequenceNumber());

		return TEMPLATE_REGISTRATION_NUMBER.formatted(currentYear, updated.getMunicipalityId(), updated.getSequenceNumber());
	}

	private static int nextSequenceNumber(RegistrationNumberSequenceEntity existing, int currentYear) {
		final var lastTouched = existing.getModified() != null ? existing.getModified() : existing.getCreated();
		final var lastTouchedYear = lastTouched.atZoneSameInstant(CANONICAL_ZONE).getYear();
		return lastTouchedYear < currentYear ? SEQUENCE_START : existing.getSequenceNumber() + 1;
	}
}
