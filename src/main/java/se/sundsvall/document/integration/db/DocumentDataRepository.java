package se.sundsvall.document.integration.db;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import se.sundsvall.document.integration.db.model.DocumentDataEntity;
import se.sundsvall.document.service.extraction.ExtractionStatus;

@CircuitBreaker(name = "documentDataRepository")
public interface DocumentDataRepository extends JpaRepository<DocumentDataEntity, String> {

	/**
	 * Looks up any previously persisted {@link DocumentDataEntity} with the same content hash and
	 * a successful extraction status, so we can reuse its {@code extractedText} instead of running
	 * Tika again on identical bytes.
	 */
	Optional<DocumentDataEntity> findFirstByContentHashAndExtractionStatus(String contentHash, ExtractionStatus extractionStatus);

	/**
	 * Backfill candidates for the reindex scheduler. Returns rows in either state:
	 * <ul>
	 * <li>{@code extraction_status = PENDING_REINDEX} — pre-V1_11 rows that never had Tika run
	 * against them (any MIME).</li>
	 * <li>{@code extraction_status = SUCCESS} and {@code page_count IS NULL} and the MIME is a
	 * paged format — rows with text but without the page-offset breakdown that V1_15 added.</li>
	 * </ul>
	 * Native query because it joins two orthogonal predicates (status-based vs. missing-page +
	 * paged-MIME) that JPQL can express but a derived method name cannot. Pageable drives the
	 * batch size.
	 */
	@Query(value = """
		select dd.* from document_data dd
		 where dd.extraction_status = 'PENDING_REINDEX'
		    or (dd.extraction_status = 'SUCCESS'
		        and dd.page_count is null
		        and dd.mime_type in (:pagedMimeTypes))
		""", nativeQuery = true)
	List<DocumentDataEntity> findReindexCandidates(@Param("pagedMimeTypes") List<String> pagedMimeTypes, Pageable pageable);

	/**
	 * Count of backfill candidates; fed to a Micrometer gauge so ops can watch backlog drain.
	 * Same predicate as {@link #findReindexCandidates}, no pagination.
	 */
	@Query(value = """
		select count(*) from document_data dd
		 where dd.extraction_status = 'PENDING_REINDEX'
		    or (dd.extraction_status = 'SUCCESS'
		        and dd.page_count is null
		        and dd.mime_type in (:pagedMimeTypes))
		""", nativeQuery = true)
	long countReindexCandidates(@Param("pagedMimeTypes") List<String> pagedMimeTypes);
}
