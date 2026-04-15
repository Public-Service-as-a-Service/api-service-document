package se.sundsvall.document.integration.db;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import se.sundsvall.document.integration.db.model.RegistrationNumberSequenceEntity;

import static jakarta.persistence.LockModeType.PESSIMISTIC_WRITE;

@CircuitBreaker(name = "registrationNumberSequenceRepository")
public interface RegistrationNumberSequenceRepository extends JpaRepository<RegistrationNumberSequenceEntity, String> {

	/**
	 * Find current registrationNumber sequence by municipalityId.
	 * <p>
	 * Lock-note: Lock rows in transaction. Other threads will wait until lock is released.
	 *
	 * @param  municipalityId the municipalityId
	 * @return                An Optional RegistrationNumberSequenceEntity for the provided municipalityId.
	 */
	@Lock(PESSIMISTIC_WRITE)
	Optional<RegistrationNumberSequenceEntity> findByMunicipalityId(String municipalityId);
}
