package se.sundsvall.document.integration.db;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.time.OffsetDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

	/**
	 * Seed a sequence row for the given municipality if none exists yet.
	 * <p>
	 * Uses {@code INSERT IGNORE} so concurrent first-time calls race safely on the
	 * {@code uq_municipality_id} unique constraint — the loser's insert is silently skipped
	 * instead of surfacing a {@code DataIntegrityViolationException}. Callers can then rely on
	 * {@link #findByMunicipalityId(String)} always returning a lockable row.
	 * <p>
	 * Inserts with {@code sequence_number = 0} so the first {@code +1} increment yields 1.
	 *
	 * @return 1 if a row was inserted, 0 if a row already existed.
	 */
	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query(value = """
		INSERT IGNORE INTO registration_number_sequence (id, municipality_id, sequence_number, created)
		VALUES (:id, :municipalityId, 0, :now)
		""", nativeQuery = true)
	int insertIfMissing(@Param("id") String id, @Param("municipalityId") String municipalityId, @Param("now") OffsetDateTime now);
}
