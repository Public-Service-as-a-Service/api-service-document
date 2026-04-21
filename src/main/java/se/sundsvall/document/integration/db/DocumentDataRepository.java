package se.sundsvall.document.integration.db;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
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
}
