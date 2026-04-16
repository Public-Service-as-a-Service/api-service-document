package se.sundsvall.document.integration.db;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import se.sundsvall.document.api.model.DocumentParameters;
import se.sundsvall.document.integration.db.model.DocumentEntity;

import static se.sundsvall.document.integration.db.specification.SearchSpecification.withSearchParameters;
import static se.sundsvall.document.integration.db.specification.SearchSpecification.withSearchQuery;

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

	/**
	 * Performs a search in DocumentEntities.
	 *
	 * @param  municipalityId      of the DocumentEntity.
	 * @param  query               the string to search for.
	 * @param  includeConfidential option if confidential documents should be included or not.
	 * @param  onlyLatestRevision  option if only latest revision should be included or not.
	 * @param  pageable            the pageable object.
	 * @return                     a Page of DocumentEntity objects that matches the search string.
	 */
	default Page<DocumentEntity> search(String query, boolean includeConfidential, boolean onlyLatestRevision, Pageable pageable, String municipalityId) {
		return this.findAll(withSearchQuery(query, includeConfidential, onlyLatestRevision, municipalityId), pageable);
	}

	default Page<DocumentEntity> searchByParameters(final DocumentParameters documentParameters, final Pageable pageable) {
		return this.findAll(withSearchParameters(documentParameters), pageable);
	}

}
