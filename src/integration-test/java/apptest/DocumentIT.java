package apptest;

import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.jdbc.Sql;
import se.sundsvall.dept44.test.AbstractAppTest;
import se.sundsvall.dept44.test.annotation.wiremock.WireMockAppTestSuite;
import se.sundsvall.document.Application;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

import static org.springframework.http.HttpHeaders.ACCEPT;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.HttpMethod.DELETE;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.PATCH;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpMethod.PUT;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.NO_CONTENT;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.IMAGE_PNG;
import static org.springframework.http.MediaType.MULTIPART_FORM_DATA;
import static org.springframework.util.ResourceUtils.getFile;

@WireMockAppTestSuite(files = "classpath:/DocumentIT/", classes = Application.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
@Sql({
	"/db/scripts/truncate.sql",
	"/db/scripts/testdata-it.sql"
})
class DocumentIT extends AbstractAppTest {

	private static final String FILTER_PATH = "/2281/documents/filter";
	private static final String PATH_SUNDSVALL = "/2281/documents";
	private static final String PATH_ANGE = "/2282/documents";
	private static final String REQUEST_FILE = "request.json";
	private static final String RESPONSE_FILE = "response.json";
	private static final String RESPONSE_FILE_BINARY = "image.jpg";

	/**
	 * # includeConfidential - false (default)
	 * # onlyLatestRevision - false (default)
	 * # limit - 3
	 */
	@Test
	@Order(1)
	void test01_searchByParameters() {
		setupCall()
			.withServicePath(FILTER_PATH + "?limit=3&sortBy=id")
			.withHttpMethod(POST)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	/**
	 * #includeConfidential - true
	 * # onlyLatestRevision - false (default)
	 * # limit - 3
	 */
	@Test
	@Order(2)
	void test02_searchByParameters() {
		setupCall()
			.withServicePath(FILTER_PATH + "?includeConfidential=true&limit=3&sortBy=id")
			.withHttpMethod(POST)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	/**
	 * # includeConfidential - false (default)
	 * # onlyLatestRevision - true
	 * # limit - 3
	 */
	@Test
	@Order(3)
	void test03_searchByParameters() {
		setupCall()
			.withServicePath(FILTER_PATH + "?onlyLatestRevision=true&limit=3&sortBy=id")
			.withHttpMethod(POST)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	/**
	 * # includeConfidential - true
	 * # onlyLatestRevision - true
	 * # limit - 3
	 */
	@Test
	@Order(4)
	void test04_searchByParameters() {
		setupCall()
			.withServicePath(FILTER_PATH + "?includeConfidential=true&onlyLatestRevision=true&limit=3&sortBy=id")
			.withHttpMethod(POST)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	/**
	 * # includeConfidential - true
	 * # onlyLatestRevision - true
	 * # documentTypes - EMPLOYEE_CERTIFICATE
	 * # limit - 3
	 */
	@Test
	@Order(5)
	void test05_searchByParameters() {
		setupCall()
			.withServicePath(FILTER_PATH + "?includeConfidential=true&onlyLatestRevision=true&documentTypes=EMPLOYEE_CERTIFICATE&limit=3&sortBy=id")
			.withHttpMethod(POST)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	/**
	 * # includeConfidential - true
	 * # onlyLatestRevision - true
	 * # documentTypes - EMPLOYEE_CERTIFICATE,HOLIDAY_EXCHANGE
	 * # limit - 3
	 */
	@Test
	@Order(6)
	void test06_searchByParameters() {
		setupCall()
			.withServicePath(FILTER_PATH + "?includeConfidential=true&onlyLatestRevision=true&documentTypes=EMPLOYEE_CERTIFICATE,HOLIDAY_EXCHANGE&limit=3&sortBy=id")
			.withHttpMethod(POST)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	/**
	 * # includeConfidential - true
	 * # onlyLatestRevision - true
	 * # metaData { key = null, matchesAny = Company B }
	 * # limit - 3
	 */
	@Test
	@Order(7)
	void test07_searchByParameters() {
		setupCall()
			.withServicePath(FILTER_PATH + "?includeConfidential=true&onlyLatestRevision=true&metaData[0].matchesAny=Company B&limit=3&sortBy=id")
			.withHttpMethod(POST)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	/**
	 * # includeConfidential - true
	 * # onlyLatestRevision - true
	 * # metaData { key = null, matchesAny = Company B,Company C }
	 * # limit - 3
	 */
	@Test
	@Order(8)
	void test08_searchByParameters() {
		setupCall()
			.withServicePath(FILTER_PATH + "?includeConfidential=true&onlyLatestRevision=true&metaData[0].matchesAny=Company B,Company C&limit=3&sortBy=id")
			.withHttpMethod(POST)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	/**
	 * # includeConfidential - true
	 * # onlyLatestRevision - true
	 * # metaData { key = null, matchesAll = Startup Y,Analyst }
	 * # limit - 3
	 */
	@Test
	@Order(9)
	void test09_searchByParameters() {
		setupCall()
			.withServicePath(FILTER_PATH + "?includeConfidential=true&onlyLatestRevision=true&metaData[0].matchesAll=Startup Y,Analyst&limit=3&sortBy=id")
			.withHttpMethod(POST)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	/**
	 * # includeConfidential - true
	 * # onlyLatestRevision - true
	 * # metaData { key = EMPLOYEE_UNIT }
	 * # limit - 3
	 */
	@Test
	@Order(10)
	void test10_searchByParameters() {
		setupCall()
			.withServicePath(FILTER_PATH + "?includeConfidential=true&onlyLatestRevision=true&metaData[0].key=EMPLOYEE_UNIT&limit=3&sortBy=id")
			.withHttpMethod(POST)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	/**
	 * # includeConfidential - true
	 * # onlyLatestRevision - true
	 * # metaData1 { key = EMPLOYEE_UNIT }
	 * # metaData2 { key = EMPLOYEE_TYPE }
	 * # limit - 3
	 */
	@Test
	@Order(11)
	void test11_searchByParameters() {
		setupCall()
			.withServicePath(FILTER_PATH + "?includeConfidential=true&onlyLatestRevision=true&metaData[0].key=EMPLOYEE_UNIT&metaData[1].key=EMPLOYEE_TYPE&limit=3&sortBy=id")
			.withHttpMethod(POST)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	/**
	 * # includeConfidential - true
	 * # onlyLatestRevision - true
	 * # metaData1 { key = EMPLOYEE_UNIT, matchesAny = Company B}
	 * # limit - 3
	 */
	@Test
	@Order(12)
	void test12_searchByParameters() {
		setupCall()
			.withServicePath(FILTER_PATH + "?includeConfidential=true&onlyLatestRevision=true&metaData[0].key=EMPLOYEE_UNIT&metaData[0].matchesAny=Company B&limit=3&sortBy=id")
			.withHttpMethod(POST)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	/**
	 * # includeConfidential - true
	 * # onlyLatestRevision - true
	 * # metaData1 { key = EMPLOYEE_UNIT, matchesAny = Company B}
	 * # metaData2 { key = EMPLOYEE_TYPE, matchesAny = Developer}
	 * # limit - 3
	 */
	@Test
	@Order(13)
	void test13_searchByParameters() {
		setupCall()
			.withServicePath(FILTER_PATH + "?includeConfidential=true&onlyLatestRevision=true&metaData[0].key=EMPLOYEE_UNIT&metaData[0].matchesAny=Company B&metaData[1].key=EMPLOYEE_TYPE&metaData[2].matchesAny=Developer&limit=3&sortBy=id")
			.withHttpMethod(POST)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	@Order(14)
	void test14_createDocument() throws FileNotFoundException {

		final var testFile = getFile(this.setupPaths().getTestDirectoryPath() + "image.png");
		final var multipartBodyBuilder = new MultipartBodyBuilder();
		multipartBodyBuilder.part("documentFiles", new FileSystemResource(testFile)).filename(testFile.getName()).contentType(IMAGE_PNG);
		multipartBodyBuilder.part("document", fromTestFile(REQUEST_FILE));

		final var location = setupCall()
			.withServicePath(PATH_SUNDSVALL)
			.withHttpMethod(POST)
			.withContentType(MULTIPART_FORM_DATA)
			.withRequest(multipartBodyBuilder.build())
			.withExpectedResponseStatus(CREATED)
			.sendRequestAndVerifyResponse()
			.getResponseHeaders().get("Location").getFirst();

		setupCall()
			.withServicePath(location.substring(location.indexOf(PATH_SUNDSVALL)) + "?includeNonPublic=true")
			.withHttpMethod(GET)
			.withHeader(ACCEPT, APPLICATION_JSON_VALUE)
			.withExpectedResponseStatus(OK)
			.withExpectedResponseHeader(CONTENT_TYPE, List.of(APPLICATION_JSON_VALUE))
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	@Order(15)
	void test15_updateDocument() {
		setupCall()
			.withServicePath(PATH_SUNDSVALL + "/2023-2281-123")
			.withHttpMethod(PATCH)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	@Order(16)
	void test16_readDocument() {
		setupCall()
			.withServicePath(PATH_SUNDSVALL + "/2023-2281-123")
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	@Order(17)
	void test17_readDocumentConfidentialFail() {
		setupCall()
			.withServicePath(PATH_SUNDSVALL + "/2024-2281-999")
			.withHttpMethod(GET)
			.withExpectedResponseStatus(NOT_FOUND)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	@Order(18)
	void test18_readDocumentConfidentialSuccess() {
		setupCall()
			.withServicePath(PATH_SUNDSVALL + "/2024-2281-999?includeConfidential=true")
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	@Order(19)
	void test19_readDocumentFile() throws IOException {
		setupCall()
			.withServicePath(PATH_SUNDSVALL + "/2023-2281-123/files/4f0a04af-942d-4ad2-b2d9-151887fc995c")
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedBinaryResponse(RESPONSE_FILE_BINARY)
			.sendRequestAndVerifyResponse();
	}

	@Test
	@Order(20)
	void test20_readDocumentFileConfidentialFail() {
		setupCall()
			.withServicePath(PATH_SUNDSVALL + "/2024-2281-999/files/bd239ee1-27b8-43e7-bb0d-e4ba09b7220e")
			.withHttpMethod(GET)
			.withExpectedResponseStatus(NOT_FOUND)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	@Order(21)
	void test21_readDocumentFileConfidentialSuccess() throws IOException {
		setupCall()
			.withServicePath(PATH_SUNDSVALL + "/2024-2281-999/files/bd239ee1-27b8-43e7-bb0d-e4ba09b7220e?includeConfidential=true")
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedBinaryResponse(RESPONSE_FILE_BINARY)
			.sendRequestAndVerifyResponse();
	}

	@Test
	@Order(22)
	void test22_search() {
		setupCall()
			.withServicePath(PATH_SUNDSVALL + "?query=value-3")
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	@Order(23)
	void test23_searchConfidential() {
		setupCall()
			.withServicePath(PATH_SUNDSVALL + "?query=value-3&includeConfidential=true")
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	@Order(24)
	void test24_searchWithWildCardAndText() {
		setupCall()
			.withServicePath(PATH_SUNDSVALL + "?query=*key2")
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	@Order(25)
	void test25_searchWithWildCardAndTextConfidential() {
		setupCall()
			.withServicePath(PATH_SUNDSVALL + "?query=*key2&includeConfidential=true")
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	@Order(26)
	void test26_searchWithWildCardOnly() {
		setupCall()
			.withServicePath(PATH_SUNDSVALL + "?query=*&sort=id,desc")
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	@Order(27)
	void test27_searchInLatestRevisionWithWildCardOnly() {
		setupCall()
			.withServicePath(PATH_SUNDSVALL + "?query=*&onlyLatestRevision=true")
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	@Order(28)
	void test28_searchWithWildCardOnlyConfidential() {
		setupCall()
			.withServicePath(PATH_SUNDSVALL + "?query=*&includeConfidential=true")
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	@Order(29)
	void test29_searchInLatestRevisionWithWildCardOnlyConfidential() {
		setupCall()
			.withServicePath(PATH_SUNDSVALL + "?query=*&includeConfidential=true&onlyLatestRevision=true")
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	@Order(30)
	void test30_deleteFile() {
		setupCall()
			.withServicePath(PATH_SUNDSVALL + "/2024-2281-139/files/abc078aa-9335-4b21-b04c-630e27ade51e")
			.withHttpMethod(DELETE)
			.withExpectedResponseStatus(NO_CONTENT)
			.sendRequestAndVerifyResponse();
	}

	@Test
	@Order(31)
	void test31_deleteFileNotFound() {
		setupCall()
			.withServicePath(PATH_SUNDSVALL + "/2023-2281-123/files/6619a286-a6cc-4001-9f55-945734805e7d") // ID doesn't exist
			.withHttpMethod(DELETE)
			.withExpectedResponseStatus(NOT_FOUND)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	@Order(32)
	void test32_addFileToDocument() throws FileNotFoundException {
		final var testFile = getFile(this.setupPaths().getTestDirectoryPath() + "image.png");
		final var multipartBodyBuilder = new MultipartBodyBuilder();
		multipartBodyBuilder.part("documentFile", new FileSystemResource(testFile)).filename(testFile.getName()).contentType(IMAGE_PNG);
		multipartBodyBuilder.part("document", fromTestFile(REQUEST_FILE));

		setupCall()
			.withServicePath(PATH_SUNDSVALL + "/2024-2281-991/files")
			.withHttpMethod(PUT)
			.withContentType(MULTIPART_FORM_DATA)
			.withRequest(multipartBodyBuilder.build())
			.withExpectedResponseStatus(NO_CONTENT)
			.sendRequestAndVerifyResponse();

		setupCall()
			.withServicePath(PATH_SUNDSVALL + "/2024-2281-991?includeNonPublic=true")
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	@Order(33)
	void test33_updateConfidentialityFlag() {
		setupCall()
			.withServicePath(PATH_SUNDSVALL + "/2023-2281-123/confidentiality")
			.withHttpMethod(PATCH)
			.withContentType(APPLICATION_JSON)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(NO_CONTENT)
			.sendRequestAndVerifyResponse();
	}

	@Test
	@Order(34)
	void test34_updateDocumentType() {
		setupCall()
			.withServicePath(PATH_ANGE + "/2024-2282-666?includeConfidential=true")
			.withHttpMethod(PATCH)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	/**
	 * Filters documents in municipality 2999 by validOn=2026-06-01. Of the five seeded rows
	 * only the open-ended ("from 2026") and the always-valid one should match; rows limited to
	 * 2025 or 2027 are excluded.
	 */
	@Test
	@Order(35)
	void test35_searchByParametersValidOn() {
		setupCall()
			.withServicePath("/2999/documents/filter?sortBy=id")
			.withHttpMethod(POST)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

}
