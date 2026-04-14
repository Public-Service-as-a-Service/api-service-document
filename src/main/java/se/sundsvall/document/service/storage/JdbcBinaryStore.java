package se.sundsvall.document.service.storage;

import jakarta.persistence.EntityManager;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.SQLException;
import java.util.Map;
import org.hibernate.Session;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.document.integration.db.DocumentDataBinaryRepository;
import se.sundsvall.document.integration.db.model.DocumentDataBinaryEntity;

import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.NOT_FOUND;

/**
 * {@link BinaryStore} implementation that persists bytes as LONGBLOB rows in MariaDB via
 * {@code document_data_binary}. This is the legacy path; Stage 2 retires it.
 */
@Component
@ConditionalOnProperty(name = "document.storage.backend", havingValue = "jdbc", matchIfMissing = true)
public class JdbcBinaryStore implements BinaryStore {

	private final EntityManager entityManager;
	private final DocumentDataBinaryRepository repository;

	public JdbcBinaryStore(EntityManager entityManager, DocumentDataBinaryRepository repository) {
		this.entityManager = entityManager;
		this.repository = repository;
	}

	@Override
	public StorageRef put(InputStream in, long sizeInBytes, String contentType, Map<String, String> userMetadata) {
		// userMetadata is ignored by the JDBC backend — LONGBLOB has nowhere to put it.
		try {
			final var blob = entityManager.unwrap(Session.class).getLobHelper().createBlob(in, sizeInBytes);
			final var entity = DocumentDataBinaryEntity.create().withBinaryFile(blob);
			final var saved = repository.saveAndFlush(entity);
			return StorageRef.jdbc(saved.getId());
		} catch (final Exception e) {
			throw Problem.valueOf(INTERNAL_SERVER_ERROR, "Failed to store binary: " + e.getMessage());
		}
	}

	@Override
	public void streamTo(StorageRef ref, OutputStream out) throws IOException {
		final var entity = repository.findById(ref.locator())
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, "No binary content found for locator: " + ref.locator()));
		try {
			StreamUtils.copy(entity.getBinaryFile().getBinaryStream(), out);
		} catch (final SQLException e) {
			throw new IOException("Failed to read binary content for locator: " + ref.locator(), e);
		}
	}

	@Override
	public void delete(StorageRef ref) {
		repository.deleteById(ref.locator());
	}

	@Override
	public StorageRef copy(StorageRef ref) {
		final var source = repository.findById(ref.locator())
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, "No binary content found for locator: " + ref.locator()));
		final var copy = DocumentDataBinaryEntity.create().withBinaryFile(source.getBinaryFile());
		final var saved = repository.saveAndFlush(copy);
		return StorageRef.jdbc(saved.getId());
	}
}
