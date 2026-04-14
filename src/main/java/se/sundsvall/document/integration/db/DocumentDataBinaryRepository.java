package se.sundsvall.document.integration.db;

import org.springframework.data.jpa.repository.JpaRepository;
import se.sundsvall.document.integration.db.model.DocumentDataBinaryEntity;

public interface DocumentDataBinaryRepository extends JpaRepository<DocumentDataBinaryEntity, String> {
}
