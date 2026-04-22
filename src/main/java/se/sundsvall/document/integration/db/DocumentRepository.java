package se.sundsvall.document.integration.db;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import se.sundsvall.document.api.model.DocumentParameters;
import se.sundsvall.document.api.model.DocumentStatus;
import se.sundsvall.document.integration.db.model.DocumentEntity;

import static se.sundsvall.document.integration.db.specification.SearchSpecification.withSearchParameters;

@CircuitBreaker(name = "documentRepository")
public interface DocumentRepository extends JpaRepository<DocumentEntity, String>, JpaSpecificationExecutor<DocumentEntity> {

	/**
	 * Find latest document by registrationNumber.
	 *
	 * @param  municipalityId     of the DocumentEntity.
	 * @param  registrationNumber of the DocumentEntity.
	 * @param  confidentialValues values of confidentiality for the documents that should be included in the result where
	 *                            true equals confidential document, false equals public document.
	 * @return                    an Optional of DocumentEntity object.
	 */
	Optional<DocumentEntity> findTopByMunicipalityIdAndRegistrationNumberAndConfidentialityConfidentialInOrderByRevisionDesc(String municipalityId, String registrationNumber, List<Boolean> confidentialValues);

	/**
	 * Find latest document by registrationNumber, excluding revisions with statuses in the
	 * provided exclusion list (typically {@code [DRAFT, REVOKED]} for public reads).
	 */
	Optional<DocumentEntity> findTopByMunicipalityIdAndRegistrationNumberAndConfidentialityConfidentialInAndStatusNotInOrderByRevisionDesc(
		String municipalityId,
		String registrationNumber,
		List<Boolean> confidentialValues,
		List<DocumentStatus> excludedStatuses);

	/**
	 * Find all revisions of a document by registrationNumber.
	 *
	 * @param  municipalityId     of the DocumentEntity.
	 * @param  registrationNumber of the DocumentEntity.
	 * @param  confidentialValues values of confidentiality for the documents that should be included in the result where
	 *                            true equals confidential document, false equals public document.
	 * @param  pageable           the pageable object.
	 * @return                    a Page of DocumentEntity objects.
	 */
	Page<DocumentEntity> findByMunicipalityIdAndRegistrationNumberAndConfidentialityConfidentialIn(String municipalityId, String registrationNumber, List<Boolean> confidentialValues, Pageable pageable);

	/**
	 * Find all revisions of a document by registrationNumber.
	 *
	 * @param  municipalityId     of the DocumentEntity.
	 * @param  registrationNumber of the DocumentEntity.
	 * @param  confidentialValues values of confidentiality for the documents that should be included in the result where
	 *                            true equals confidential document, false equals public document.
	 * @return                    a List of DocumentEntity objects.
	 */
	List<DocumentEntity> findByMunicipalityIdAndRegistrationNumberAndConfidentialityConfidentialIn(String municipalityId, String registrationNumber, List<Boolean> confidentialValues);

	/**
	 * Find document by registrationNumber and revision.
	 *
	 * @param  municipalityId     of the DocumentEntity.
	 * @param  registrationNumber of the DocumentEntity.
	 * @param  revision           Document revision number.
	 * @param  confidentialValues values of confidentiality for the documents that should be included in the result where
	 *                            true equals confidential document, false equals public document.
	 * @return                    an Optional of DocumentEntity object.
	 */
	Optional<DocumentEntity> findByMunicipalityIdAndRegistrationNumberAndRevisionAndConfidentialityConfidentialIn(String municipalityId, String registrationNumber, int revision, List<Boolean> confidentialValues);

	boolean existsByMunicipalityIdAndRegistrationNumber(String municipalityId, String registrationNumber);

	default Page<DocumentEntity> searchByParameters(final DocumentParameters documentParameters, final Pageable pageable, final List<DocumentStatus> effectiveStatuses) {
		return this.findAll(withSearchParameters(documentParameters, effectiveStatuses), pageable);
	}

	/**
	 * Bulk transition of documents whose validFrom has been reached:
	 * SCHEDULED → ACTIVE when validFrom is null or on/before today and validTo permits.
	 *
	 * @return number of affected rows.
	 */
	@Modifying
	@Query("""
		update DocumentEntity d
		   set d.status = se.sundsvall.document.api.model.DocumentStatus.ACTIVE
		 where d.status = se.sundsvall.document.api.model.DocumentStatus.SCHEDULED
		   and (d.validFrom is null or d.validFrom <= :today)
		   and (d.validTo is null or d.validTo >= :today)
		""")
	int bulkScheduledToActive(@Param("today") LocalDate today);

	/**
	 * Bulk transition of documents whose validTo has passed:
	 * ACTIVE / SCHEDULED → EXPIRED.
	 *
	 * @return number of affected rows.
	 */
	@Modifying
	@Query("""
		update DocumentEntity d
		   set d.status = se.sundsvall.document.api.model.DocumentStatus.EXPIRED
		 where d.status in (
		           se.sundsvall.document.api.model.DocumentStatus.ACTIVE,
		           se.sundsvall.document.api.model.DocumentStatus.SCHEDULED)
		   and d.validTo is not null
		   and d.validTo < :today
		""")
	int bulkExpire(@Param("today") LocalDate today);

}
