package se.sundsvall.document.service.statistics;

import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.dept44.problem.ThrowableProblem;
import se.sundsvall.document.api.model.DocumentAccessType;
import se.sundsvall.document.api.model.FileStatistics;
import se.sundsvall.document.api.model.RevisionStatistics;
import se.sundsvall.document.integration.db.DocumentAccessLogRepository;
import se.sundsvall.document.integration.db.DocumentRepository;
import se.sundsvall.document.integration.db.model.DocumentDataEntity;
import se.sundsvall.document.integration.db.model.DocumentEntity;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static se.sundsvall.document.service.InclusionFilter.CONFIDENTIAL_AND_PUBLIC;

@ExtendWith(MockitoExtension.class)
class DocumentStatisticsServiceTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String REGISTRATION_NUMBER = "2023-2281-1337";
	private static final String FILE_A = "file-a";
	private static final String FILE_B = "file-b";

	@Mock
	private DocumentRepository documentRepositoryMock;

	@Mock
	private DocumentAccessLogRepository accessLogRepositoryMock;

	@InjectMocks
	private DocumentStatisticsService service;

	@Test
	void getStatistics_aggregatesPerRevisionAndPerFile() {
		// Arrange
		when(documentRepositoryMock.existsByMunicipalityIdAndRegistrationNumber(MUNICIPALITY_ID, REGISTRATION_NUMBER)).thenReturn(true);
		when(accessLogRepositoryMock.aggregate(eq(MUNICIPALITY_ID), eq(REGISTRATION_NUMBER), any(), any())).thenReturn(List.of(
			new AccessCountProjection(1, FILE_A, DocumentAccessType.DOWNLOAD, 5L),
			new AccessCountProjection(1, FILE_A, DocumentAccessType.VIEW, 2L),
			new AccessCountProjection(1, FILE_B, DocumentAccessType.DOWNLOAD, 3L),
			new AccessCountProjection(2, FILE_A, DocumentAccessType.VIEW, 4L)));
		when(documentRepositoryMock.findByMunicipalityIdAndRegistrationNumberAndConfidentialityConfidentialIn(
			MUNICIPALITY_ID, REGISTRATION_NUMBER, CONFIDENTIAL_AND_PUBLIC.getValue())).thenReturn(List.of(
				DocumentEntity.create().withDocumentData(List.of(
					DocumentDataEntity.create().withId(FILE_A).withFileName("rapport.pdf"),
					DocumentDataEntity.create().withId(FILE_B).withFileName("bilaga.pdf")))));

		// Act
		final var result = service.getStatistics(MUNICIPALITY_ID, REGISTRATION_NUMBER, null, null);

		// Assert
		assertThat(result.getMunicipalityId()).isEqualTo(MUNICIPALITY_ID);
		assertThat(result.getRegistrationNumber()).isEqualTo(REGISTRATION_NUMBER);
		assertThat(result.getTotalAccesses()).isEqualTo(14L);
		assertThat(result.getPerRevision()).extracting(RevisionStatistics::getRevision, RevisionStatistics::getDownloads, RevisionStatistics::getViews)
			.containsExactly(tuple(1, 8L, 2L), tuple(2, 0L, 4L));

		final var revision1 = result.getPerRevision().get(0);
		assertThat(revision1.getPerFile()).extracting(FileStatistics::getDocumentDataId, FileStatistics::getFileName, FileStatistics::getDownloads, FileStatistics::getViews)
			.containsExactlyInAnyOrder(
				tuple(FILE_B, "bilaga.pdf", 3L, 0L),
				tuple(FILE_A, "rapport.pdf", 5L, 2L));
	}

	@Test
	void getStatistics_unknownDocument_throwsNotFound() {
		// Arrange
		when(documentRepositoryMock.existsByMunicipalityIdAndRegistrationNumber(MUNICIPALITY_ID, REGISTRATION_NUMBER)).thenReturn(false);

		// Act + Assert
		assertThatThrownBy(() -> service.getStatistics(MUNICIPALITY_ID, REGISTRATION_NUMBER, null, null))
			.isInstanceOf(ThrowableProblem.class)
			.hasMessageContaining(REGISTRATION_NUMBER);
		verify(accessLogRepositoryMock, never()).aggregate(any(), any(), any(), any());
	}

	@Test
	void getStatistics_emptyLog_returnsZeroAccesses() {
		// Arrange
		when(documentRepositoryMock.existsByMunicipalityIdAndRegistrationNumber(MUNICIPALITY_ID, REGISTRATION_NUMBER)).thenReturn(true);
		when(accessLogRepositoryMock.aggregate(eq(MUNICIPALITY_ID), eq(REGISTRATION_NUMBER), any(), any())).thenReturn(emptyList());
		when(documentRepositoryMock.findByMunicipalityIdAndRegistrationNumberAndConfidentialityConfidentialIn(
			MUNICIPALITY_ID, REGISTRATION_NUMBER, CONFIDENTIAL_AND_PUBLIC.getValue())).thenReturn(emptyList());

		// Act
		final var result = service.getStatistics(MUNICIPALITY_ID, REGISTRATION_NUMBER, null, null);

		// Assert
		assertThat(result.getTotalAccesses()).isZero();
		assertThat(result.getPerRevision()).isEmpty();
	}

	@Test
	void getStatistics_passesDateBoundsToRepository() {
		// Arrange
		final var from = OffsetDateTime.parse("2026-01-01T00:00:00+01:00");
		final var to = OffsetDateTime.parse("2026-04-17T00:00:00+02:00");
		when(documentRepositoryMock.existsByMunicipalityIdAndRegistrationNumber(MUNICIPALITY_ID, REGISTRATION_NUMBER)).thenReturn(true);
		when(accessLogRepositoryMock.aggregate(MUNICIPALITY_ID, REGISTRATION_NUMBER, from, to)).thenReturn(emptyList());
		when(documentRepositoryMock.findByMunicipalityIdAndRegistrationNumberAndConfidentialityConfidentialIn(
			MUNICIPALITY_ID, REGISTRATION_NUMBER, CONFIDENTIAL_AND_PUBLIC.getValue())).thenReturn(emptyList());

		// Act
		final var result = service.getStatistics(MUNICIPALITY_ID, REGISTRATION_NUMBER, from, to);

		// Assert
		assertThat(result.getFrom()).isEqualTo(from);
		assertThat(result.getTo()).isEqualTo(to);
		verify(accessLogRepositoryMock).aggregate(MUNICIPALITY_ID, REGISTRATION_NUMBER, from, to);
	}
}
