package se.sundsvall.document.integration.db;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import se.sundsvall.document.integration.db.model.DocumentAccessLogEntity;
import se.sundsvall.document.service.statistics.AccessCountProjection;

@CircuitBreaker(name = "documentAccessLogRepository")
public interface DocumentAccessLogRepository extends JpaRepository<DocumentAccessLogEntity, String> {

	/**
	 * Aggregate access counts grouped by revision, file and access type for a document.
	 * Optional date range bounds: {@code from} is inclusive, {@code to} is exclusive. Pass {@code null}
	 * for either bound to leave it open.
	 */
	@Query("""
		select new se.sundsvall.document.service.statistics.AccessCountProjection(
		    log.revision, log.documentDataId, log.accessType, count(log))
		from DocumentAccessLogEntity log
		where log.municipalityId = :municipalityId
		  and log.registrationNumber = :registrationNumber
		  and (:from is null or log.accessedAt >= :from)
		  and (:to is null or log.accessedAt < :to)
		group by log.revision, log.documentDataId, log.accessType
		""")
	List<AccessCountProjection> aggregate(
		@Param("municipalityId") String municipalityId,
		@Param("registrationNumber") String registrationNumber,
		@Param("from") OffsetDateTime from,
		@Param("to") OffsetDateTime to);
}
