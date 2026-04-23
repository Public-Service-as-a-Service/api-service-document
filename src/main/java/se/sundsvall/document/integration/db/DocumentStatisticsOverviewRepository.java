package se.sundsvall.document.integration.db;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;
import se.sundsvall.document.integration.db.model.DocumentEntity;
import se.sundsvall.document.service.statistics.overview.ConfidentialityCountProjection;
import se.sundsvall.document.service.statistics.overview.StatusCountProjection;
import se.sundsvall.document.service.statistics.overview.TypeCountProjection;
import se.sundsvall.document.service.statistics.overview.YearCountProjection;

/**
 * Aggregation queries that power {@code GET /documents/statistics}.
 *
 * <p>
 * All methods aggregate over the <b>latest revision of each in-scope document</b>. Scope is
 * defined by the caller:
 * <ul>
 * <li>{@code createdBy == null} — every document in the municipality.</li>
 * <li>{@code createdBy != null} — only documents whose <b>first revision</b> (revision = 1) was
 * created by that user. This keeps a user's "my documents" view stable when a colleague later
 * edits the document and bumps the revision.</li>
 * </ul>
 */
@CircuitBreaker(name = "documentStatisticsOverviewRepository")
public interface DocumentStatisticsOverviewRepository extends Repository<DocumentEntity, String> {

	/**
	 * Total number of distinct documents (unique registration numbers) in scope.
	 */
	@Query("""
		select count(distinct d.registrationNumber) from DocumentEntity d
		where d.municipalityId = :municipalityId
		  and d.revision = 1
		  and (:createdBy is null or d.createdBy = :createdBy)
		""")
	long countTotalDocuments(
		@Param("municipalityId") String municipalityId,
		@Param("createdBy") String createdBy);

	/**
	 * Count of latest-revision documents grouped by status.
	 */
	@Query("""
		select new se.sundsvall.document.service.statistics.overview.StatusCountProjection(d.status, count(d))
		from DocumentEntity d
		where d.municipalityId = :municipalityId
		  and d.registrationNumber in (
		      select d1.registrationNumber from DocumentEntity d1
		      where d1.municipalityId = :municipalityId
		        and d1.revision = 1
		        and (:createdBy is null or d1.createdBy = :createdBy)
		  )
		  and d.revision = (
		      select max(d2.revision) from DocumentEntity d2
		      where d2.municipalityId = :municipalityId
		        and d2.registrationNumber = d.registrationNumber
		  )
		group by d.status
		""")
	List<StatusCountProjection> countByStatus(
		@Param("municipalityId") String municipalityId,
		@Param("createdBy") String createdBy);

	/**
	 * Count of latest-revision documents grouped by confidentiality flag.
	 */
	@Query("""
		select new se.sundsvall.document.service.statistics.overview.ConfidentialityCountProjection(d.confidentiality.confidential, count(d))
		from DocumentEntity d
		where d.municipalityId = :municipalityId
		  and d.registrationNumber in (
		      select d1.registrationNumber from DocumentEntity d1
		      where d1.municipalityId = :municipalityId
		        and d1.revision = 1
		        and (:createdBy is null or d1.createdBy = :createdBy)
		  )
		  and d.revision = (
		      select max(d2.revision) from DocumentEntity d2
		      where d2.municipalityId = :municipalityId
		        and d2.registrationNumber = d.registrationNumber
		  )
		group by d.confidentiality.confidential
		""")
	List<ConfidentialityCountProjection> countByConfidentiality(
		@Param("municipalityId") String municipalityId,
		@Param("createdBy") String createdBy);

	/**
	 * Count of latest-revision documents grouped by document type.
	 */
	@Query("""
		select new se.sundsvall.document.service.statistics.overview.TypeCountProjection(d.type.type, count(d))
		from DocumentEntity d
		where d.municipalityId = :municipalityId
		  and d.registrationNumber in (
		      select d1.registrationNumber from DocumentEntity d1
		      where d1.municipalityId = :municipalityId
		        and d1.revision = 1
		        and (:createdBy is null or d1.createdBy = :createdBy)
		  )
		  and d.revision = (
		      select max(d2.revision) from DocumentEntity d2
		      where d2.municipalityId = :municipalityId
		        and d2.registrationNumber = d.registrationNumber
		  )
		group by d.type.type
		""")
	List<TypeCountProjection> countByDocumentType(
		@Param("municipalityId") String municipalityId,
		@Param("createdBy") String createdBy);

	/**
	 * Count of documents grouped by registration-number year. Year is parsed from the
	 * {@code YYYY-} prefix of the registration number. Scoped on the first revision so each
	 * document contributes once.
	 */
	@Query("""
		select new se.sundsvall.document.service.statistics.overview.YearCountProjection(substring(d.registrationNumber, 1, 4), count(d))
		from DocumentEntity d
		where d.municipalityId = :municipalityId
		  and d.revision = 1
		  and (:createdBy is null or d.createdBy = :createdBy)
		group by substring(d.registrationNumber, 1, 4)
		""")
	List<YearCountProjection> countByRegistrationYear(
		@Param("municipalityId") String municipalityId,
		@Param("createdBy") String createdBy);

	/**
	 * For each in-scope document, the highest revision number it has. The service turns this
	 * list into bucket counts (single / two / three-or-more) and tracks the max observed.
	 */
	@Query("""
		select max(d.revision) from DocumentEntity d
		where d.municipalityId = :municipalityId
		  and d.registrationNumber in (
		      select d1.registrationNumber from DocumentEntity d1
		      where d1.municipalityId = :municipalityId
		        and d1.revision = 1
		        and (:createdBy is null or d1.createdBy = :createdBy)
		  )
		group by d.registrationNumber
		""")
	List<Integer> latestRevisionPerDocument(
		@Param("municipalityId") String municipalityId,
		@Param("createdBy") String createdBy);

	/**
	 * Count of latest-revision documents with no attached files.
	 */
	@Query("""
		select count(d) from DocumentEntity d
		where d.municipalityId = :municipalityId
		  and d.registrationNumber in (
		      select d1.registrationNumber from DocumentEntity d1
		      where d1.municipalityId = :municipalityId
		        and d1.revision = 1
		        and (:createdBy is null or d1.createdBy = :createdBy)
		  )
		  and d.revision = (
		      select max(d2.revision) from DocumentEntity d2
		      where d2.municipalityId = :municipalityId
		        and d2.registrationNumber = d.registrationNumber
		  )
		  and size(d.documentData) = 0
		""")
	long countDocumentsWithoutFiles(
		@Param("municipalityId") String municipalityId,
		@Param("createdBy") String createdBy);

	/**
	 * Count of ACTIVE latest-revision documents whose {@code validTo} falls within
	 * {@code [today, windowEnd]} (both inclusive).
	 */
	@Query("""
		select count(d) from DocumentEntity d
		where d.municipalityId = :municipalityId
		  and d.registrationNumber in (
		      select d1.registrationNumber from DocumentEntity d1
		      where d1.municipalityId = :municipalityId
		        and d1.revision = 1
		        and (:createdBy is null or d1.createdBy = :createdBy)
		  )
		  and d.revision = (
		      select max(d2.revision) from DocumentEntity d2
		      where d2.municipalityId = :municipalityId
		        and d2.registrationNumber = d.registrationNumber
		  )
		  and d.status = se.sundsvall.document.api.model.DocumentStatus.ACTIVE
		  and d.validTo is not null
		  and d.validTo between :today and :windowEnd
		""")
	long countExpiringSoon(
		@Param("municipalityId") String municipalityId,
		@Param("createdBy") String createdBy,
		@Param("today") LocalDate today,
		@Param("windowEnd") LocalDate windowEnd);
}
