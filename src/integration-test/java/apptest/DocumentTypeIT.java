package apptest;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.jdbc.Sql;
import se.sundsvall.dept44.test.annotation.wiremock.WireMockAppTestSuite;
import se.sundsvall.document.Application;
import se.sundsvall.document.integration.db.DocumentTypeRepository;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpHeaders.LOCATION;
import static org.springframework.http.HttpMethod.DELETE;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.PATCH;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.NO_CONTENT;
import static org.springframework.http.HttpStatus.OK;

@WireMockAppTestSuite(files = "classpath:/DocumentTypeIT/", classes = Application.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
@Sql({
	"/db/scripts/truncate.sql",
	"/db/scripts/testdata-it.sql"
})
class DocumentTypeIT extends AbstractDocumentAppTest {

	private static final String PATH = "/%s/admin/documenttypes";
	private static final String REQUEST_FILE = "request.json";
	private static final String RESPONSE_FILE = "response.json";

	@Autowired
	private DocumentTypeRepository repository;

	@Test
	void test01_createDocumentType() {
		assertThat(repository.existsByMunicipalityIdAndType("2260", "APARTMENT_AGREEMENT")).isFalse();

		setupCall()
			.withServicePath(PATH.formatted("2260"))
			.withHttpMethod(POST)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(CREATED)
			.withExpectedResponseHeader(LOCATION, List.of(PATH.formatted("2260") + "/APARTMENT_AGREEMENT"))
			.withExpectedResponseBodyIsNull()
			.sendRequestAndVerifyResponse();

		assertThat(repository.existsByMunicipalityIdAndType("2260", "APARTMENT_AGREEMENT")).isTrue();

		setupCall()
			.withServicePath(PATH.formatted("2260") + "/APARTMENT_AGREEMENT")
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();

	}

	@Test
	void test02_updateDocumentType() {
		setupCall()
			.withServicePath(PATH.formatted("2262") + "/MISSPELLED")
			.withHttpMethod(PATCH)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(NO_CONTENT)
			.withExpectedResponseBodyIsNull()
			.sendRequestAndVerifyResponse();

		assertThat(repository.existsByMunicipalityIdAndType("2262", "MISSPELLED")).isFalse();
		assertThat(repository.findByMunicipalityIdAndType("2262", "CONFIDENTIALITY_AGREEMENT")).isPresent().hasValueSatisfying(entity -> {
			assertThat(entity.getDisplayName()).isEqualTo("Sekretessavtal");
			assertThat(entity.getLastUpdatedBy()).isEqualTo("abc123");
			assertThat(entity.getType()).isEqualTo("CONFIDENTIALITY_AGREEMENT");
		});
	}

	@Test
	void test03_readDocumentTypes() {
		setupCall()
			.withServicePath(PATH.formatted("2281"))
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test04_readDocumentType() {
		setupCall()
			.withServicePath(PATH.formatted("2281") + "/EMPLOYEE_CERTIFICATE")
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test05_deleteDocumentType() {
		assertThat(repository.existsByMunicipalityIdAndType("2260", "TYPE_TO_DELETE")).isTrue();

		setupCall()
			.withServicePath(PATH.formatted("2260") + "/TYPE_TO_DELETE")
			.withHttpMethod(DELETE)
			.withExpectedResponseStatus(NO_CONTENT)
			.withExpectedResponseBodyIsNull()
			.sendRequestAndVerifyResponse();

		assertThat(repository.existsByMunicipalityIdAndType("2260", "TYPE_TO_DELETE")).isFalse();
	}
}
