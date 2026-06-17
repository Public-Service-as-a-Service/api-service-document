package apptest;

import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.jdbc.Sql;
import se.sundsvall.dept44.test.annotation.wiremock.WireMockAppTestSuite;
import se.sundsvall.document.Application;

import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpStatus.OK;

/**
 * End-to-end coverage of the page-aware search flow: seeded {@code document_data} with known
 * {@code extracted_text} + {@code page_offsets}, indexed via the real {@code DocumentIndexingService}
 * in {@link AbstractDocumentAppTest#primeElasticsearchFromSeededData()}, then hit through
 * {@code /file-matches}. Asserts the response carries exact {@code matches} offsets with resolved
 * page numbers — the piece that's hard to cover at unit level because it spans
 * {@code DocumentSearchService} → real Elasticsearch → {@code DocumentSearchMapper}.
 * <p>
 * Isolated from {@link DocumentIT}'s test data (own {@code @Sql}) so the hardcoded
 * {@code totalRecords} assertions in that suite don't need to be updated every time we extend
 * the paged seed.
 */
@WireMockAppTestSuite(files = "classpath:/PagedSearchIT/", classes = Application.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
@Sql({
	"/db/scripts/truncate.sql",
	"/db/scripts/testdata-paged-search-it.sql"
})
class PagedSearchIT extends AbstractDocumentAppTest {

	private static final String PATH_FILE_MATCHES = "/2281/documents/file-matches";
	private static final String RESPONSE_FILE = "response.json";

	@Test
	@Order(1)
	void test01_searchFileMatchesResolvesPerMatchPageNumber() {
		setupCall()
			.withServicePath(PATH_FILE_MATCHES + "?query=bandwidth")
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}
}
