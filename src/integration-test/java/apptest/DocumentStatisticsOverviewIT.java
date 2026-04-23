package apptest;

import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.jdbc.Sql;
import se.sundsvall.dept44.test.annotation.wiremock.WireMockAppTestSuite;
import se.sundsvall.document.Application;

import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpStatus.OK;

@WireMockAppTestSuite(files = "classpath:/DocumentStatisticsOverviewIT/", classes = Application.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
@Sql({
	"/db/scripts/truncate.sql",
	"/db/scripts/testdata-it.sql"
})
class DocumentStatisticsOverviewIT extends AbstractDocumentAppTest {

	// Targets the isolated stats-only block in testdata-it.sql (municipality 2283):
	//   7 distinct documents, 1 of which has 2 revisions.
	//   USER_A owns first revisions of docs 1, 2, 3.
	//   USER_B owns first revisions of docs 4, 5, 6, 7. doc-6's rev 2 was made by USER_A —
	//   the stats endpoint still counts doc-6 in USER_B's scope because scope filters on rev 1.
	private static final String BASE_PATH = "/2283/documents/statistics";
	private static final String USER_A = "a2283001-0000-0000-0000-000000000001";
	private static final String USER_B = "a2283002-0000-0000-0000-000000000002";
	private static final String UNKNOWN_USER = "aa000000-0000-0000-0000-000000000000";
	private static final String RESPONSE_FILE = "response.json";

	@Test
	@Order(1)
	void test01_municipalityScope() {
		setupCall()
			.withServicePath(BASE_PATH)
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	@Order(2)
	void test02_userScope_userA() {
		setupCall()
			.withServicePath(BASE_PATH + "?createdBy=" + USER_A)
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	@Order(3)
	void test03_userScope_userB() {
		setupCall()
			.withServicePath(BASE_PATH + "?createdBy=" + USER_B)
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	@Order(4)
	void test04_userScope_unknownUser_returnsZeros() {
		setupCall()
			.withServicePath(BASE_PATH + "?createdBy=" + UNKNOWN_USER)
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}
}
