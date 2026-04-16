package se.sundsvall.document.integration.db;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import se.sundsvall.document.integration.db.model.DocumentResponsibilityEntity;

@CircuitBreaker(name = "documentResponsibilityRepository")
public interface DocumentResponsibilityRepository extends JpaRepository<DocumentResponsibilityEntity, String> {

	List<DocumentResponsibilityEntity> findByMunicipalityIdAndRegistrationNumberOrderByUsernameAsc(String municipalityId, String registrationNumber);

	List<DocumentResponsibilityEntity> findByMunicipalityIdAndRegistrationNumberIn(String municipalityId, Collection<String> registrationNumbers);

	void deleteByMunicipalityIdAndRegistrationNumber(String municipalityId, String registrationNumber);
}
