package se.sundsvall.document.service.statistics;

import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.document.api.model.DocumentAccessType;
import se.sundsvall.document.integration.db.DocumentAccessLogRepository;
import se.sundsvall.document.integration.db.model.DocumentAccessLogEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentAccessLoggerTest {

	@Mock
	private DocumentAccessLogRepository repositoryMock;

	@InjectMocks
	private DocumentAccessLogger logger;

	@Test
	void onDocumentAccessed_persistsEntityWithEventFields() {
		// Arrange
		final var accessedAt = OffsetDateTime.parse("2026-04-17T10:15:00+02:00");
		final var event = new DocumentAccessedEvent("2281", "doc-1", "2023-2281-1337", 2, "file-1",
			DocumentAccessType.DOWNLOAD, "user@example.com", accessedAt);

		// Act
		logger.onDocumentAccessed(event);

		// Assert
		final var captor = ArgumentCaptor.forClass(DocumentAccessLogEntity.class);
		verify(repositoryMock).save(captor.capture());
		final var saved = captor.getValue();
		assertThat(saved.getMunicipalityId()).isEqualTo("2281");
		assertThat(saved.getDocumentId()).isEqualTo("doc-1");
		assertThat(saved.getRegistrationNumber()).isEqualTo("2023-2281-1337");
		assertThat(saved.getRevision()).isEqualTo(2);
		assertThat(saved.getDocumentDataId()).isEqualTo("file-1");
		assertThat(saved.getAccessType()).isEqualTo(DocumentAccessType.DOWNLOAD);
		assertThat(saved.getAccessedBy()).isEqualTo("user@example.com");
		assertThat(saved.getAccessedAt()).isEqualTo(accessedAt);
	}

	@Test
	void onDocumentAccessed_repositoryFailureIsSwallowed() {
		// Arrange — repository throws to simulate DB outage; statistics must never propagate to caller.
		when(repositoryMock.save(org.mockito.ArgumentMatchers.any(DocumentAccessLogEntity.class)))
			.thenThrow(new RuntimeException("db down"));
		final var event = new DocumentAccessedEvent("2281", "doc-1", "reg", 1, "file-1",
			DocumentAccessType.VIEW, null, OffsetDateTime.now());

		// Act + Assert
		assertThatNoException().isThrownBy(() -> logger.onDocumentAccessed(event));
	}
}
