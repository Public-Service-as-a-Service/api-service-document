package se.sundsvall.document.api;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.dept44.problem.violations.ConstraintViolationProblem;
import se.sundsvall.document.Application;
import se.sundsvall.document.api.model.DocumentTypeCreateRequest;
import se.sundsvall.document.api.model.DocumentTypeUpdateRequest;
import se.sundsvall.document.service.DocumentTypeService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON;

@SpringBootTest(classes = Application.class, webEnvironment = RANDOM_PORT)
@ActiveProfiles("junit")
@AutoConfigureWebTestClient
class DocumentTypeAdministrationResourceFailuresTest {

	private static final String BASE_PATH = "/{municipalityId}/admin/documenttypes";

	@MockitoBean
	private DocumentTypeService serviceMock;

	@Autowired
	private WebTestClient webTestClient;

	@Test
	void createWithInvalidMunicipalityId() {
		// Act
		final var response = webTestClient.post()
			.uri(uribuilder -> uribuilder.path(BASE_PATH).build(Map.of("municipalityId", "invalid")))
			.contentType(APPLICATION_JSON)
			.bodyValue(DocumentTypeCreateRequest.create()
				.withType("type")
				.withDisplayName("displayName")
				.withCreatedBy("b0000000-0000-0000-0000-0000000000c1"))
			.exchange()
			.expectStatus().isBadRequest()
			.expectHeader().contentType(APPLICATION_PROBLEM_JSON)
			.expectBody(ConstraintViolationProblem.class)
			.returnResult()
			.getResponseBody();

		// Assert
		assertThat(response).isNotNull();
		assertThat(response.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(response.getViolations()).isNotEmpty().satisfiesExactlyInAnyOrder(violation -> {
			assertThat(violation.field()).isEqualTo("createDocumentType.municipalityId");
			assertThat(violation.message()).isEqualTo("not a valid municipality ID");
		});

		verifyNoInteractions(serviceMock);
	}

	@Test
	void createWithMissingBody() {
		// Act
		final var response = webTestClient.post()
			.uri(uribuilder -> uribuilder.path(BASE_PATH).build(Map.of("municipalityId", "2281")))
			.contentType(APPLICATION_JSON)
			.exchange()
			.expectStatus().isBadRequest()
			.expectHeader().contentType(APPLICATION_PROBLEM_JSON)
			.expectBody(Problem.class)
			.returnResult()
			.getResponseBody();

		// Assert
		assertThat(response).isNotNull();
		assertThat(response.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(response.getDetail()).isEqualTo("Failed to read request");

		verifyNoInteractions(serviceMock);
	}

	@Test
	void createWithEmptyBody() {
		// Act
		final var response = webTestClient.post()
			.uri(uribuilder -> uribuilder.path(BASE_PATH).build(Map.of("municipalityId", "2281")))
			.contentType(APPLICATION_JSON)
			.bodyValue(DocumentTypeCreateRequest.create())
			.exchange()
			.expectStatus().isBadRequest()
			.expectHeader().contentType(APPLICATION_PROBLEM_JSON)
			.expectBody(ConstraintViolationProblem.class)
			.returnResult()
			.getResponseBody();

		// Assert
		assertThat(response).isNotNull();
		assertThat(response.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(response.getViolations()).isNotEmpty().satisfiesExactlyInAnyOrder(violation -> {
			assertThat(violation.field()).isEqualTo("createdBy");
			assertThat(violation.message()).isEqualTo("must not be blank");
		}, violation -> {
			assertThat(violation.field()).isEqualTo("createdBy");
			assertThat(violation.message()).isEqualTo("not a valid UUID");
		}, violation -> {
			assertThat(violation.field()).isEqualTo("displayName");
			assertThat(violation.message()).isEqualTo("must not be blank");
		}, violation -> {
			assertThat(violation.field()).isEqualTo("type");
			assertThat(violation.message()).isEqualTo("must not be blank");
		});

		verifyNoInteractions(serviceMock);
	}

	@Test
	void readAllWithInvalidMunicipalityId() {
		// Act
		final var response = webTestClient.get()
			.uri(uribuilder -> uribuilder.path(BASE_PATH).build(Map.of("municipalityId", "invalid")))
			.exchange()
			.expectStatus().isBadRequest()
			.expectHeader().contentType(APPLICATION_PROBLEM_JSON)
			.expectBody(ConstraintViolationProblem.class)
			.returnResult()
			.getResponseBody();

		// Assert
		assertThat(response).isNotNull();
		assertThat(response.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(response.getViolations()).isNotEmpty().satisfiesExactlyInAnyOrder(violation -> {
			assertThat(violation.field()).isEqualTo("readDocumentTypes.municipalityId");
			assertThat(violation.message()).isEqualTo("not a valid municipality ID");
		});

		verifyNoInteractions(serviceMock);
	}

	@Test
	void readOneWithInvalidMunicipalityId() {
		// Act
		final var response = webTestClient.get()
			.uri(uribuilder -> uribuilder.path(BASE_PATH + "/type").build(Map.of("municipalityId", "invalid")))
			.exchange()
			.expectStatus().isBadRequest()
			.expectHeader().contentType(APPLICATION_PROBLEM_JSON)
			.expectBody(ConstraintViolationProblem.class)
			.returnResult()
			.getResponseBody();

		// Assert
		assertThat(response).isNotNull();
		assertThat(response.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(response.getViolations()).isNotEmpty().satisfiesExactlyInAnyOrder(violation -> {
			assertThat(violation.field()).isEqualTo("readDocumentType.municipalityId");
			assertThat(violation.message()).isEqualTo("not a valid municipality ID");
		});

		verifyNoInteractions(serviceMock);
	}

	@Test
	void updateWithInvalidMunicipalityId() {
		// Act
		final var response = webTestClient.patch()
			.uri(uribuilder -> uribuilder.path(BASE_PATH + "/type").build(Map.of("municipalityId", "invalid")))
			.contentType(APPLICATION_JSON)
			.bodyValue(DocumentTypeUpdateRequest.create()
				.withDisplayName("displayName")
				.withUpdatedBy("b0000000-0000-0000-0000-0000000000d1"))
			.exchange()
			.expectStatus().isBadRequest()
			.expectHeader().contentType(APPLICATION_PROBLEM_JSON)
			.expectBody(ConstraintViolationProblem.class)
			.returnResult()
			.getResponseBody();

		// Assert
		assertThat(response).isNotNull();
		assertThat(response.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(response.getViolations()).isNotEmpty().satisfiesExactlyInAnyOrder(violation -> {
			assertThat(violation.field()).isEqualTo("updateDocumentType.municipalityId");
			assertThat(violation.message()).isEqualTo("not a valid municipality ID");
		});

		verifyNoInteractions(serviceMock);
	}

	@Test
	void updateWithMissingBody() {
		// Act
		final var response = webTestClient.patch()
			.uri(uribuilder -> uribuilder.path(BASE_PATH + "/type").build(Map.of("municipalityId", "2281")))
			.contentType(APPLICATION_JSON)
			.exchange()
			.expectStatus().isBadRequest()
			.expectHeader().contentType(APPLICATION_PROBLEM_JSON)
			.expectBody(Problem.class)
			.returnResult()
			.getResponseBody();

		// Assert
		assertThat(response).isNotNull();
		assertThat(response.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(response.getDetail()).isEqualTo("Failed to read request");

		verifyNoInteractions(serviceMock);
	}

	@Test
	void updateWithMissingMandatoryData() {
		// Act
		final var response = webTestClient.patch()
			.uri(uribuilder -> uribuilder.path(BASE_PATH + "/type").build(Map.of("municipalityId", "2281")))
			.contentType(APPLICATION_JSON)
			.bodyValue(DocumentTypeUpdateRequest.create())
			.exchange()
			.expectStatus().isBadRequest()
			.expectHeader().contentType(APPLICATION_PROBLEM_JSON)
			.expectBody(ConstraintViolationProblem.class)
			.returnResult()
			.getResponseBody();

		// Assert
		assertThat(response).isNotNull();
		assertThat(response.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(response.getViolations()).isNotEmpty().satisfiesExactlyInAnyOrder(violation -> {
			assertThat(violation.field()).isEqualTo("updatedBy");
			assertThat(violation.message()).isEqualTo("must not be blank");
		}, violation -> {
			assertThat(violation.field()).isEqualTo("updatedBy");
			assertThat(violation.message()).isEqualTo("not a valid UUID");
		});

		verifyNoInteractions(serviceMock);
	}

	@Test
	void deleteWithInvalidMunicipalityId() {
		// Act
		final var response = webTestClient.delete()
			.uri(uribuilder -> uribuilder.path(BASE_PATH + "/type").build(Map.of("municipalityId", "invalid")))
			.exchange()
			.expectStatus().isBadRequest()
			.expectHeader().contentType(APPLICATION_PROBLEM_JSON)
			.expectBody(ConstraintViolationProblem.class)
			.returnResult()
			.getResponseBody();

		// Assert
		assertThat(response).isNotNull();
		assertThat(response.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(response.getViolations()).isNotEmpty().satisfiesExactlyInAnyOrder(violation -> {
			assertThat(violation.field()).isEqualTo("deleteDocumentType.municipalityId");
			assertThat(violation.message()).isEqualTo("not a valid municipality ID");
		});

		verifyNoInteractions(serviceMock);
	}
}
