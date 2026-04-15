package se.sundsvall.document.service.storage;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.Blob;
import java.sql.SQLException;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.dept44.problem.ThrowableProblem;
import se.sundsvall.document.integration.db.DocumentDataBinaryRepository;
import se.sundsvall.document.integration.db.model.DocumentDataBinaryEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.NOT_FOUND;

/**
 * Mock-based coverage for the branches of {@link JdbcBinaryStore} that the IT suite doesn't reach
 * (the not-found paths and {@link JdbcBinaryStore#delete(StorageRef)}, which {@code DocumentService}
 * never calls because revision-style deletes orphan rather than delete bytes).
 *
 * Happy-path coverage for {@code put}/{@code streamTo}/{@code copy} comes from {@code DocumentIT}
 * running against the JDBC default backend.
 */
@ExtendWith(MockitoExtension.class)
class JdbcBinaryStoreTest {

	@Mock
	private DocumentDataBinaryRepository repositoryMock;

	@InjectMocks
	private JdbcBinaryStore store;

	@Test
	void delete_callsRepositoryDeleteById() {
		final var ref = StorageRef.jdbc("some-id");

		store.delete(ref);

		verify(repositoryMock).deleteById("some-id");
	}

	@Test
	void streamTo_onMissingRow_throwsNotFound() {
		final var ref = StorageRef.jdbc("missing-id");
		when(repositoryMock.findById("missing-id")).thenReturn(Optional.empty());

		assertThatThrownBy(() -> store.streamTo(ref, new ByteArrayOutputStream()))
			.isInstanceOf(ThrowableProblem.class)
			.satisfies(ex -> {
				final var problem = (ThrowableProblem) ex;
				assertThat(problem.getStatus()).isEqualTo(NOT_FOUND);
				assertThat(problem.getMessage()).contains("missing-id");
			});

		verify(repositoryMock).findById("missing-id");
	}

	@Test
	void copy_onMissingRow_throwsNotFound() {
		final var ref = StorageRef.jdbc("missing-id");
		when(repositoryMock.findById("missing-id")).thenReturn(Optional.empty());

		assertThatThrownBy(() -> store.copy(ref))
			.isInstanceOf(ThrowableProblem.class)
			.satisfies(ex -> {
				final var problem = (ThrowableProblem) ex;
				assertThat(problem.getStatus()).isEqualTo(NOT_FOUND);
				assertThat(problem.getMessage()).contains("missing-id");
			});

		verify(repositoryMock).findById("missing-id");
	}

	@Test
	void streamTo_whenBlobReadFails_throwsIOException(@Mock Blob blobMock) throws SQLException {
		final var ref = StorageRef.jdbc("id");
		final var entity = DocumentDataBinaryEntity.create().withBinaryFile(blobMock);
		when(repositoryMock.findById("id")).thenReturn(Optional.of(entity));
		when(blobMock.getBinaryStream()).thenThrow(new SQLException("blob read failed"));

		assertThatThrownBy(() -> store.streamTo(ref, new ByteArrayOutputStream()))
			.isInstanceOf(IOException.class)
			.hasMessageContaining("id")
			.hasCauseInstanceOf(SQLException.class);
	}
}
