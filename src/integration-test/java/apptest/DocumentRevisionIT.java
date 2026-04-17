package apptest;

import org.junit.jupiter.api.Test;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.jdbc.Sql;
import se.sundsvall.dept44.test.annotation.wiremock.WireMockAppTestSuite;
import se.sundsvall.document.Application;

import java.io.IOException;

import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.OK;

@WireMockAppTestSuite(files = "classpath:/DocumentRevisionIT/", classes = Application.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
@Sql({
	"/db/scripts/truncate.sql",
	"/db/scripts/testdata-it.sql"
})
class DocumentRevisionIT extends AbstractDocumentAppTest {

	private static final String PATH = "/2281/documents";
	private static final String RESPONSE_FILE = "response.json";
	private static final String RESPONSE_FILE_BINARY = "image.jpg";

	@Test
	void test01_readRevisions() {
		setupCall()
			.withServicePath(PATH + "/2023-2281-123/revisions")
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test02_readRevisionsConfidentialFail() {
		setupCall()
			.withServicePath(PATH + "/2024-2281-999/revisions")
			.withHttpMethod(GET)
			.withExpectedResponse(RESPONSE_FILE)
			.withExpectedResponseStatus(OK)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test03_readRevisionsConfidentialSuccess() {
		setupCall()
			.withServicePath(PATH + "/2024-2281-999/revisions?includeConfidential=true")
			.withHttpMethod(GET)
			.withExpectedResponse(RESPONSE_FILE)
			.withExpectedResponseStatus(OK)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test04_readRevision() {
		setupCall()
			.withServicePath(PATH + "/2023-2281-123/revisions/2")
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test05_readRevisionConfidentialFail() {
		setupCall()
			.withServicePath(PATH + "/2024-2281-999/revisions/2")
			.withHttpMethod(GET)
			.withExpectedResponse(RESPONSE_FILE)
			.withExpectedResponseStatus(NOT_FOUND)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test06_readRevisionConfidentialSuccess() {
		setupCall()
			.withServicePath(PATH + "/2024-2281-999/revisions/2?includeConfidential=true")
			.withHttpMethod(GET)
			.withExpectedResponse(RESPONSE_FILE)
			.withExpectedResponseStatus(OK)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test07_readRevisionFile() throws IOException {
		setupCall()
			.withServicePath(PATH + "/2023-2281-123/revisions/3/files/4f0a04af-942d-4ad2-b2d9-151887fc995c")
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedBinaryResponse(RESPONSE_FILE_BINARY)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test08_readRevisionFileConfidentialFail() {
		setupCall()
			.withServicePath(PATH + "/2024-2281-999/revisions/2/files/bd239ee1-27b8-43e7-bb0d-e4ba09b7220e")
			.withHttpMethod(GET)
			.withExpectedResponseStatus(NOT_FOUND)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test09_readRevisionFileConfidentialSuccess() throws IOException {
		setupCall()
			.withServicePath(PATH + "/2024-2281-999/revisions/2/files/bd239ee1-27b8-43e7-bb0d-e4ba09b7220e?includeConfidential=true")
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedBinaryResponse(RESPONSE_FILE_BINARY)
			.sendRequestAndVerifyResponse();
	}
}
