package se.sundsvall.document.api;

import java.time.OffsetDateTime;
import java.util.EnumMap;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import se.sundsvall.document.Application;
import se.sundsvall.document.api.model.DocumentStatisticsOverview;
import se.sundsvall.document.api.model.DocumentStatisticsOverview.ConfidentialityCounts;
import se.sundsvall.document.api.model.DocumentStatisticsOverview.DocumentTypeCount;
import se.sundsvall.document.api.model.DocumentStatisticsOverview.ExpiringSoon;
import se.sundsvall.document.api.model.DocumentStatisticsOverview.RevisionDistribution;
import se.sundsvall.document.api.model.DocumentStatisticsOverview.Scope;
import se.sundsvall.document.api.model.DocumentStatisticsOverview.YearCount;
import se.sundsvall.document.api.model.DocumentStatus;
import se.sundsvall.document.service.statistics.overview.DocumentStatisticsOverviewService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.MediaType.APPLICATION_JSON;

@SpringBootTest(classes = Application.class, webEnvironment = RANDOM_PORT)
@ActiveProfiles("junit")
@AutoConfigureWebTestClient
class DocumentStatisticsOverviewResourceTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String CREATED_BY = "6c3e4f5a-7b8d-4e9c-a1f2-d3e4b5c6a7f8";

	@MockitoBean
	private DocumentStatisticsOverviewService serviceMock;

	@Autowired
	private WebTestClient webTestClient;

	@Test
	void statistics_municipalityScope() {
		final var byStatus = new EnumMap<DocumentStatus, Long>(DocumentStatus.class);
		byStatus.put(DocumentStatus.DRAFT, 12L);
		byStatus.put(DocumentStatus.ACTIVE, 980L);
		byStatus.put(DocumentStatus.SCHEDULED, 40L);
		byStatus.put(DocumentStatus.EXPIRED, 381L);
		byStatus.put(DocumentStatus.REVOKED, 10L);

		final var payload = DocumentStatisticsOverview.create()
			.withMunicipalityId(MUNICIPALITY_ID)
			.withScope(Scope.MUNICIPALITY)
			.withGeneratedAt(OffsetDateTime.parse("2026-04-23T10:00:00Z"))
			.withTotalDocuments(1423L)
			.withByStatus(byStatus)
			.withByConfidentiality(ConfidentialityCounts.create().withConfidential(83L).withNonConfidential(1340L))
			.withByDocumentType(List.of(DocumentTypeCount.create().withType("INVOICE").withCount(540L)))
			.withByRegistrationYear(List.of(YearCount.create().withYear(2025).withCount(500L)))
			.withRevisionDistribution(RevisionDistribution.create().withSingle(980L).withTwo(300L).withThreeOrMore(143L).withMaxRevision(14))
			.withDocumentsWithoutFiles(7L)
			.withExpiringSoon(ExpiringSoon.create().withWithinDays(30).withCount(42L));

		when(serviceMock.getOverview(eq(MUNICIPALITY_ID), isNull())).thenReturn(payload);

		final var response = webTestClient.get()
			.uri("/" + MUNICIPALITY_ID + "/documents/statistics")
			.exchange()
			.expectStatus().isOk()
			.expectHeader().contentType(APPLICATION_JSON)
			.expectBody(DocumentStatisticsOverview.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getScope()).isEqualTo(Scope.MUNICIPALITY);
		assertThat(response.getCreatedBy()).isNull();
		assertThat(response.getTotalDocuments()).isEqualTo(1423L);
		assertThat(response.getExpiringSoon().getWithinDays()).isEqualTo(30);

		verify(serviceMock).getOverview(MUNICIPALITY_ID, null);
	}

	@Test
	void statistics_userScope_passesCreatedBy() {
		final var payload = DocumentStatisticsOverview.create()
			.withMunicipalityId(MUNICIPALITY_ID)
			.withScope(Scope.USER)
			.withCreatedBy(CREATED_BY)
			.withTotalDocuments(5L)
			.withByStatus(new EnumMap<>(DocumentStatus.class))
			.withByConfidentiality(ConfidentialityCounts.create())
			.withByDocumentType(List.of())
			.withByRegistrationYear(List.of())
			.withRevisionDistribution(RevisionDistribution.create())
			.withExpiringSoon(ExpiringSoon.create().withWithinDays(30).withCount(0L));

		when(serviceMock.getOverview(MUNICIPALITY_ID, CREATED_BY)).thenReturn(payload);

		webTestClient.get()
			.uri(uriBuilder -> uriBuilder.path("/" + MUNICIPALITY_ID + "/documents/statistics")
				.queryParam("createdBy", CREATED_BY)
				.build())
			.exchange()
			.expectStatus().isOk();

		verify(serviceMock).getOverview(MUNICIPALITY_ID, CREATED_BY);
	}

	@Test
	void statistics_rejectsMalformedCreatedBy() {
		webTestClient.get()
			.uri(uriBuilder -> uriBuilder.path("/" + MUNICIPALITY_ID + "/documents/statistics")
				.queryParam("createdBy", "not-a-uuid")
				.build())
			.exchange()
			.expectStatus().isBadRequest();
	}
}
