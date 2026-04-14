package se.sundsvall.document.service;

import java.time.Year;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.sundsvall.document.integration.db.RegistrationNumberSequenceRepository;
import se.sundsvall.document.integration.db.model.RegistrationNumberSequenceEntity;

import static java.time.ZoneId.systemDefault;
import static se.sundsvall.document.service.Constants.TEMPLATE_REGISTRATION_NUMBER;

/**
 * Class responsible for generating unique registration numbers.
 *
 * Registration numbers are created with the following format: [YYYY-MUNICIPALITY_ID-SEQUENCE]
 *
 * Example:
 * If a registrationNumber is created on date 2022-10-26 for "Sundsvall municipality" (municipalityID: 2281), for the
 * first time, the registrationNumber will be: 2022-2281-1. The next generated number will be 2022-2281-2 and so on.
 *
 * Every new year, the sequence will be reset to 1. (e.g. 2023-2281-1).
 */
@Service
@Transactional
public class RegistrationNumberService {

	private static final int SEQUENCE_START = 1;

	private final RegistrationNumberSequenceRepository registrationNumberSequenceRepository;

	public RegistrationNumberService(RegistrationNumberSequenceRepository registrationNumberSequenceRepository) {
		this.registrationNumberSequenceRepository = registrationNumberSequenceRepository;
	}

	public String generateRegistrationNumber(String municipalityId) {
		final var currentYear = Year.now(systemDefault()).getValue();

		final var sequenceEntity = registrationNumberSequenceRepository.findByMunicipalityId(municipalityId)
			.map(existing -> existing.withSequenceNumber(nextSequenceNumber(existing, currentYear)))
			.orElseGet(() -> RegistrationNumberSequenceEntity.create()
				.withMunicipalityId(municipalityId)
				.withSequenceNumber(SEQUENCE_START));

		final var saved = registrationNumberSequenceRepository.save(sequenceEntity);
		return TEMPLATE_REGISTRATION_NUMBER.formatted(currentYear, saved.getMunicipalityId(), saved.getSequenceNumber());
	}

	private static int nextSequenceNumber(RegistrationNumberSequenceEntity existing, int currentYear) {
		final var lastTouched = existing.getModified() != null ? existing.getModified() : existing.getCreated();
		return lastTouched.getYear() < currentYear ? SEQUENCE_START : existing.getSequenceNumber() + 1;
	}
}
