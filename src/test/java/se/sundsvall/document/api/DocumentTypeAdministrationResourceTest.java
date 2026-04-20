package se.sundsvall.document.api;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import se.sundsvall.document.Application;
import se.sundsvall.document.api.model.DocumentType;
import se.sundsvall.document.api.model.DocumentTypeCreateRequest;
import se.sundsvall.document.api.model.DocumentTypeUpdateRequest;
import se.sundsvall.document.service.DocumentTypeService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.HttpHeaders.LOCATION;
import static org.springframework.http.MediaType.ALL_VALUE;
import static org.springframework.http.MediaType.APPLICATION_JSON;

@SpringBootTest(classes = Application.class, webEnvironment = RANDOM_PORT)
@ActiveProfiles("junit")
@AutoConfigureWebTestClient
class DocumentTypeAdministrationResourceTest {

	private static final String BASE_PATH = "/{municipalityId}/admin/documenttypes";
	private static final String MUNICIPALITY_ID = "2281";

	@MockitoBean
	private DocumentTypeService serviceMock;

	@Autowired
	private WebTestClient webTestClient;

	@Test
	void create() {
		// Arrange
		final var displayName = "displayName";
		final var type = "type";
		final var createdBy = "b0000000-0000-0000-0000-0000000000c1";
		final var request = DocumentTypeCreateRequest.create()
			.withType(type)
			.withDisplayName(displayName)
			.withCreatedBy(createdBy);
		when(serviceMock.create(MUNICIPALITY_ID, request)).thenReturn(DocumentType.create()
			.withDisplayName(displayName)
			.withType(type));

		// Act
		webTestClient.post()
			.uri(uribuilder -> uribuilder.path(BASE_PATH).build(Map.of("municipalityId", MUNICIPALITY_ID)))
			.contentType(APPLICATION_JSON)
			.bodyValue(request)
			.exchange()
			.expectStatus().isCreated()
			.expectHeader().contentType(ALL_VALUE)
			.expectHeader().valuesMatch(LOCATION, "^/2281/admin/documenttypes/type$")
			.expectBody().isEmpty();

		// Assert and verify
		verify(serviceMock).create(MUNICIPALITY_ID, request);
		verifyNoMoreInteractions(serviceMock);
	}

	@Test
	void update() {
		// Arrange
		final var type = "type";
		final var request = DocumentTypeUpdateRequest.create()
			.withDisplayName("displayName")
			.withUpdatedBy("b0000000-0000-0000-0000-0000000000d1");

		// Act
		webTestClient.patch()
			.uri(uribuilder -> uribuilder.path(BASE_PATH + "/" + type).build(Map.of("municipalityId", MUNICIPALITY_ID)))
			.contentType(APPLICATION_JSON)
			.bodyValue(request)
			.exchange()
			.expectStatus().isNoContent()
			.expectHeader().contentType(ALL_VALUE)
			.expectBody().isEmpty();

		// Assert and verify
		verify(serviceMock).update(MUNICIPALITY_ID, type, request);
		verifyNoMoreInteractions(serviceMock);
	}

	@Test
	void readAll() {
		// Arrange
		final var documentTypes = List.of(DocumentType.create()
			.withDisplayName("displayName")
			.withType("type"));
		when(serviceMock.read(MUNICIPALITY_ID)).thenReturn(documentTypes);

		// Act
		final var response = webTestClient.get()
			.uri(uribuilder -> uribuilder.path(BASE_PATH).build(Map.of("municipalityId", MUNICIPALITY_ID)))
			.exchange()
			.expectStatus().isOk()
			.expectHeader().contentType(APPLICATION_JSON)
			.expectBodyList(DocumentType.class)
			.returnResult()
			.getResponseBody();

		// Assert and verify
		assertThat(response).hasSize(1).satisfiesExactly(type -> {
			assertThat(type.getDisplayName()).isEqualTo("displayName");
			assertThat(type.getType()).isEqualTo("type");
		});

		verify(serviceMock).read(MUNICIPALITY_ID);
		verifyNoMoreInteractions(serviceMock);
	}

	@Test
	void readOne() {
		// Arrange
		final var type = "type";
		final var documentType = DocumentType.create()
			.withDisplayName("displayName")
			.withType("type");
		when(serviceMock.read(MUNICIPALITY_ID, type)).thenReturn(documentType);

		// Act
		final var response = webTestClient.get()
			.uri(uribuilder -> uribuilder.path(BASE_PATH + "/" + type).build(Map.of("municipalityId", MUNICIPALITY_ID)))
			.exchange()
			.expectStatus().isOk()
			.expectHeader().contentType(APPLICATION_JSON)
			.expectBody(DocumentType.class)
			.returnResult()
			.getResponseBody();

		// Assert and verify
		assertThat(response).isEqualTo(documentType);
		verify(serviceMock).read(MUNICIPALITY_ID, type);
		verifyNoMoreInteractions(serviceMock);
	}

	@Test
	void delete() {
		// Act
		webTestClient.delete()
			.uri(uribuilder -> uribuilder.path(BASE_PATH + "/type").build(Map.of("municipalityId", "2281")))
			.exchange()
			.expectStatus().isNoContent()
			.expectHeader().contentType(ALL_VALUE)
			.expectBody().isEmpty();

		// Assert and verify
		// TODO: Verify interactions with service mock
	}
}
