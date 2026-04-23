package se.sundsvall.document.service.statistics.overview;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.document.api.model.DocumentStatisticsOverview.Scope;
import se.sundsvall.document.api.model.DocumentStatus;
import se.sundsvall.document.integration.db.DocumentStatisticsOverviewRepository;
import se.sundsvall.document.service.DocumentStatusPolicy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentStatisticsOverviewServiceTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String CREATED_BY = "6c3e4f5a-7b8d-4e9c-a1f2-d3e4b5c6a7f8";
	private static final LocalDate TODAY = LocalDate.of(2026, 4, 23);

	@Mock
	private DocumentStatisticsOverviewRepository repository;

	@Mock
	private DocumentStatusPolicy statusPolicy;

	private Clock clock;

	private DocumentStatisticsOverviewService service;

	@BeforeEach
	void setUp() {
		clock = Clock.fixed(Instant.parse("2026-04-23T10:00:00Z"), ZoneId.of("Europe/Stockholm"));
		service = new DocumentStatisticsOverviewService(repository, statusPolicy, clock);
	}

	@Test
	void getOverview_municipalityScope_wiresUnfilteredQueries() {
		when(statusPolicy.today()).thenReturn(TODAY);
		when(repository.countTotalDocuments(MUNICIPALITY_ID, null)).thenReturn(1423L);
		when(repository.countByStatus(MUNICIPALITY_ID, null)).thenReturn(List.of(
			new StatusCountProjection(DocumentStatus.DRAFT, 12L),
			new StatusCountProjection(DocumentStatus.ACTIVE, 980L),
			new StatusCountProjection(DocumentStatus.SCHEDULED, 40L),
			new StatusCountProjection(DocumentStatus.EXPIRED, 381L),
			new StatusCountProjection(DocumentStatus.REVOKED, 10L)));
		when(repository.countByConfidentiality(MUNICIPALITY_ID, null)).thenReturn(List.of(
			new ConfidentialityCountProjection(true, 83L),
			new ConfidentialityCountProjection(false, 1340L)));
		when(repository.countByDocumentType(MUNICIPALITY_ID, null)).thenReturn(List.of(
			new TypeCountProjection("INVOICE", 540L),
			new TypeCountProjection("CONTRACT", 200L),
			new TypeCountProjection("MEMO", 540L)));
		when(repository.countByRegistrationYear(MUNICIPALITY_ID, null)).thenReturn(List.of(
			new YearCountProjection("2025", 500L),
			new YearCountProjection("2023", 410L),
			new YearCountProjection("2024", 513L)));
		when(repository.latestRevisionPerDocument(MUNICIPALITY_ID, null)).thenReturn(List.of(1, 1, 2, 3, 5, 1, 1));
		when(repository.countDocumentsWithoutFiles(MUNICIPALITY_ID, null)).thenReturn(7L);
		when(repository.countExpiringSoon(eq(MUNICIPALITY_ID), isNull(), any(), any())).thenReturn(42L);

		final var result = service.getOverview(MUNICIPALITY_ID, null);

		assertThat(result.getMunicipalityId()).isEqualTo(MUNICIPALITY_ID);
		assertThat(result.getScope()).isEqualTo(Scope.MUNICIPALITY);
		assertThat(result.getCreatedBy()).isNull();
		assertThat(result.getGeneratedAt()).isNotNull();
		assertThat(result.getTotalDocuments()).isEqualTo(1423L);

		assertThat(result.getByStatus())
			.containsEntry(DocumentStatus.DRAFT, 12L)
			.containsEntry(DocumentStatus.ACTIVE, 980L)
			.containsEntry(DocumentStatus.SCHEDULED, 40L)
			.containsEntry(DocumentStatus.EXPIRED, 381L)
			.containsEntry(DocumentStatus.REVOKED, 10L);

		assertThat(result.getByConfidentiality().getConfidential()).isEqualTo(83L);
		assertThat(result.getByConfidentiality().getNonConfidential()).isEqualTo(1340L);

		// By count descending, then name ascending for ties.
		assertThat(result.getByDocumentType()).extracting("type", "count")
			.containsExactly(
				org.assertj.core.groups.Tuple.tuple("INVOICE", 540L),
				org.assertj.core.groups.Tuple.tuple("MEMO", 540L),
				org.assertj.core.groups.Tuple.tuple("CONTRACT", 200L));

		// Ordered by year ascending.
		assertThat(result.getByRegistrationYear()).extracting("year", "count")
			.containsExactly(
				org.assertj.core.groups.Tuple.tuple(2023, 410L),
				org.assertj.core.groups.Tuple.tuple(2024, 513L),
				org.assertj.core.groups.Tuple.tuple(2025, 500L));

		// Revisions: [1,1,2,3,5,1,1] → single=4, two=1, threeOrMore=2, max=5
		assertThat(result.getRevisionDistribution().getSingle()).isEqualTo(4L);
		assertThat(result.getRevisionDistribution().getTwo()).isEqualTo(1L);
		assertThat(result.getRevisionDistribution().getThreeOrMore()).isEqualTo(2L);
		assertThat(result.getRevisionDistribution().getMaxRevision()).isEqualTo(5);

		assertThat(result.getDocumentsWithoutFiles()).isEqualTo(7L);
		assertThat(result.getExpiringSoon().getWithinDays()).isEqualTo(30);
		assertThat(result.getExpiringSoon().getCount()).isEqualTo(42L);

		verify(repository).countExpiringSoon(MUNICIPALITY_ID, null, TODAY, TODAY.plusDays(30));
	}

	@Test
	void getOverview_userScope_passesCreatedByAndSetsScope() {
		when(statusPolicy.today()).thenReturn(TODAY);
		when(repository.countTotalDocuments(MUNICIPALITY_ID, CREATED_BY)).thenReturn(5L);
		when(repository.countByStatus(MUNICIPALITY_ID, CREATED_BY)).thenReturn(List.of());
		when(repository.countByConfidentiality(MUNICIPALITY_ID, CREATED_BY)).thenReturn(List.of());
		when(repository.countByDocumentType(MUNICIPALITY_ID, CREATED_BY)).thenReturn(List.of());
		when(repository.countByRegistrationYear(MUNICIPALITY_ID, CREATED_BY)).thenReturn(List.of());
		when(repository.latestRevisionPerDocument(MUNICIPALITY_ID, CREATED_BY)).thenReturn(List.of());
		when(repository.countDocumentsWithoutFiles(MUNICIPALITY_ID, CREATED_BY)).thenReturn(0L);
		when(repository.countExpiringSoon(MUNICIPALITY_ID, CREATED_BY, TODAY, TODAY.plusDays(30))).thenReturn(0L);

		final var result = service.getOverview(MUNICIPALITY_ID, CREATED_BY);

		assertThat(result.getScope()).isEqualTo(Scope.USER);
		assertThat(result.getCreatedBy()).isEqualTo(CREATED_BY);
		assertThat(result.getTotalDocuments()).isEqualTo(5L);

		// Every DocumentStatus value is present in byStatus even when the repo returned nothing.
		assertThat(result.getByStatus()).hasSize(DocumentStatus.values().length);
		assertThat(result.getByStatus().values()).containsOnly(0L);

		assertThat(result.getByConfidentiality().getConfidential()).isZero();
		assertThat(result.getByConfidentiality().getNonConfidential()).isZero();
		assertThat(result.getByDocumentType()).isEmpty();
		assertThat(result.getByRegistrationYear()).isEmpty();
		assertThat(result.getRevisionDistribution().getMaxRevision()).isZero();
	}

	@Test
	void getOverview_treatsBlankCreatedByAsMunicipalityScope() {
		when(statusPolicy.today()).thenReturn(TODAY);
		when(repository.countTotalDocuments(MUNICIPALITY_ID, null)).thenReturn(0L);
		when(repository.countByStatus(MUNICIPALITY_ID, null)).thenReturn(List.of());
		when(repository.countByConfidentiality(MUNICIPALITY_ID, null)).thenReturn(List.of());
		when(repository.countByDocumentType(MUNICIPALITY_ID, null)).thenReturn(List.of());
		when(repository.countByRegistrationYear(MUNICIPALITY_ID, null)).thenReturn(List.of());
		when(repository.latestRevisionPerDocument(MUNICIPALITY_ID, null)).thenReturn(List.of());
		when(repository.countDocumentsWithoutFiles(MUNICIPALITY_ID, null)).thenReturn(0L);
		when(repository.countExpiringSoon(MUNICIPALITY_ID, null, TODAY, TODAY.plusDays(30))).thenReturn(0L);

		final var result = service.getOverview(MUNICIPALITY_ID, "   ");

		assertThat(result.getScope()).isEqualTo(Scope.MUNICIPALITY);
		assertThat(result.getCreatedBy()).isNull();
		verifyNoMoreInteractions(repository);
	}
}
