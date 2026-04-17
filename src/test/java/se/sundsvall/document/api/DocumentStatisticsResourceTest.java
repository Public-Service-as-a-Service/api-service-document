package se.sundsvall.document.api;

import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import se.sundsvall.document.Application;
import se.sundsvall.document.api.model.DocumentStatistics;
import se.sundsvall.document.api.model.FileStatistics;
import se.sundsvall.document.api.model.RevisionStatistics;
import se.sundsvall.document.service.statistics.DocumentStatisticsService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.MediaType.APPLICATION_JSON;

@SpringBootTest(classes = Application.class, webEnvironment = RANDOM_PORT)
@ActiveProfiles("junit")
@AutoConfigureWebTestClient
class DocumentStatisticsResourceTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String REGISTRATION_NUMBER = "2023-2281-1337";

	@MockitoBean
	private DocumentStatisticsService statisticsServiceMock;

	@Autowired
	private WebTestClient webTestClient;

	@Test
	void readStatistics_returnsAggregatedResponse() {
		// Arrange
		final var stats = DocumentStatistics.create()
			.withMunicipalityId(MUNICIPALITY_ID)
			.withRegistrationNumber(REGISTRATION_NUMBER)
			.withTotalAccesses(8L)
			.withPerRevision(List.of(RevisionStatistics.create()
				.withRevision(1)
				.withDownloads(5L)
				.withViews(3L)
				.withPerFile(List.of(FileStatistics.create()
					.withDocumentDataId("file-1")
					.withFileName("rapport.pdf")
					.withDownloads(5L)
					.withViews(3L)))));

		when(statisticsServiceMock.getStatistics(eq(MUNICIPALITY_ID), eq(REGISTRATION_NUMBER), any(), any())).thenReturn(stats);

		// Act
		final var response = webTestClient.get()
			.uri("/" + MUNICIPALITY_ID + "/documents/" + REGISTRATION_NUMBER + "/statistics")
			.exchange()
			.expectStatus().isOk()
			.expectHeader().contentType(APPLICATION_JSON)
			.expectBody(DocumentStatistics.class)
			.returnResult()
			.getResponseBody();

		// Assert
		assertThat(response).isNotNull();
		assertThat(response.getTotalAccesses()).isEqualTo(8L);
		assertThat(response.getPerRevision()).hasSize(1);
		assertThat(response.getPerRevision().get(0).getPerFile()).hasSize(1);
	}

	@Test
	void readStatistics_passesDateBoundsThrough() {
		// Arrange — use UTC (Z) to avoid '+' encoding pitfalls in query strings.
		final var from = OffsetDateTime.parse("2026-01-01T00:00:00Z");
		final var to = OffsetDateTime.parse("2026-04-17T00:00:00Z");
		when(statisticsServiceMock.getStatistics(eq(MUNICIPALITY_ID), eq(REGISTRATION_NUMBER), eq(from), eq(to)))
			.thenReturn(DocumentStatistics.create().withRegistrationNumber(REGISTRATION_NUMBER));

		// Act
		webTestClient.get()
			.uri(uriBuilder -> uriBuilder.path("/" + MUNICIPALITY_ID + "/documents/" + REGISTRATION_NUMBER + "/statistics")
				.queryParam("from", from)
				.queryParam("to", to)
				.build())
			.exchange()
			.expectStatus().isOk();

		// Assert
		verify(statisticsServiceMock).getStatistics(MUNICIPALITY_ID, REGISTRATION_NUMBER, from, to);
	}
}
