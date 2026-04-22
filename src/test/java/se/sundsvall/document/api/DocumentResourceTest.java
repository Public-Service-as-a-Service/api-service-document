package se.sundsvall.document.api;

import jakarta.servlet.http.HttpServletResponse;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import se.sundsvall.document.Application;
import se.sundsvall.document.api.model.Confidentiality;
import se.sundsvall.document.api.model.ConfidentialityUpdateRequest;
import se.sundsvall.document.api.model.Document;
import se.sundsvall.document.api.model.DocumentCreateRequest;
import se.sundsvall.document.api.model.DocumentDataCreateRequest;
import se.sundsvall.document.api.model.DocumentFiles;
import se.sundsvall.document.api.model.DocumentMatch;
import se.sundsvall.document.api.model.DocumentMetadata;
import se.sundsvall.document.api.model.DocumentResponsibilitiesUpdateRequest;
import se.sundsvall.document.api.model.DocumentResponsibility;
import se.sundsvall.document.api.model.DocumentUpdateRequest;
import se.sundsvall.document.api.model.PagedDocumentMatchResponse;
import se.sundsvall.document.api.model.PagedDocumentResponse;
import se.sundsvall.document.api.validation.DocumentTypeValidator;
import se.sundsvall.document.service.DocumentFileService;
import se.sundsvall.document.service.DocumentResponsibilityService;
import se.sundsvall.document.service.DocumentSearchService;
import se.sundsvall.document.service.DocumentService;
import se.sundsvall.document.service.DocumentStatusService;

import static java.util.Collections.emptyList;
import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.data.domain.Sort.Order.asc;
import static org.springframework.http.HttpHeaders.LOCATION;
import static org.springframework.http.MediaType.ALL_VALUE;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.MULTIPART_FORM_DATA;
import static org.springframework.http.MediaType.TEXT_PLAIN;
import static org.springframework.web.reactive.function.BodyInserters.fromMultipartData;

@SpringBootTest(classes = Application.class, webEnvironment = RANDOM_PORT)
@ActiveProfiles("junit")
@AutoConfigureWebTestClient
class DocumentResourceTest {

	@MockitoBean
	private DocumentService documentServiceMock;

	@MockitoBean
	private DocumentFileService documentFileServiceMock;

	@MockitoBean
	private DocumentSearchService documentSearchServiceMock;

	@MockitoBean
	private DocumentStatusService documentStatusServiceMock;

	@MockitoBean
	private DocumentResponsibilityService documentResponsibilityServiceMock;

	@MockitoBean
	private DocumentTypeValidator validationUtilityMock;

	@Autowired
	private WebTestClient webTestClient;

	@Test
	void create() {

		// Arrange
		final var documentCreateRequest = DocumentCreateRequest.create()
			.withConfidentiality(Confidentiality.create().withConfidential(true).withLegalCitation("legalCitation"))
			.withCreatedBy("b0000000-0000-0000-0000-000000000099")
			.withTitle("title")
			.withDescription("description")
			.withMetadataList(List.of(DocumentMetadata.create()
				.withKey("key")
				.withValue("value")))
			.withResponsibilities(List.of(DocumentResponsibility.create()
				.withPersonId("6b8d4a1c-34e2-4f73-a5f1-b7c2e9a0d8c4")))
			.withType("type")
			.withValidFrom(LocalDate.of(2026, 4, 15))
			.withValidTo(LocalDate.of(2027, 4, 15));

		final var multipartBodyBuilder = new MultipartBodyBuilder();
		multipartBodyBuilder.part("documentFiles", "file-content").filename("test1.txt").contentType(TEXT_PLAIN);
		multipartBodyBuilder.part("documentFiles", "file-content").filename("tesst2.txt").contentType(TEXT_PLAIN);
		multipartBodyBuilder.part("document", documentCreateRequest);

		when(documentServiceMock.create(any(), any(), any())).thenReturn(Document.create());

		// Act
		final var response = webTestClient.post()
			.uri("/2281/documents")
			.contentType(MULTIPART_FORM_DATA)
			.body(fromMultipartData(multipartBodyBuilder.build()))
			.exchange()
			.expectStatus().isCreated()
			.expectHeader().contentType(ALL_VALUE)
			.expectHeader().exists(LOCATION)
			.expectHeader().valuesMatch(LOCATION, "^/2281/documents/(.*)$")
			.expectBody().isEmpty();

		// Assert
		assertThat(response).isNotNull();
		verify(documentServiceMock).create(eq(documentCreateRequest), ArgumentMatchers.<DocumentFiles>any(), eq("2281"));
	}

	@Test
	void createWithoutMetadata() {

		// Arrange
		final var documentCreateRequest = DocumentCreateRequest.create()
			.withCreatedBy("b0000000-0000-0000-0000-000000000099")
			.withTitle("title")
			.withDescription("description")
			.withType("type");

		final var multipartBodyBuilder = new MultipartBodyBuilder();
		multipartBodyBuilder.part("documentFiles", "file-content").filename("test.txt").contentType(TEXT_PLAIN);
		multipartBodyBuilder.part("document", documentCreateRequest);

		when(documentServiceMock.create(any(), any(), any())).thenReturn(Document.create());

		// Act
		final var response = webTestClient.post()
			.uri("/2281/documents")
			.contentType(MULTIPART_FORM_DATA)
			.body(fromMultipartData(multipartBodyBuilder.build()))
			.exchange()
			.expectStatus().isCreated()
			.expectHeader().contentType(ALL_VALUE)
			.expectHeader().exists(LOCATION)
			.expectHeader().valuesMatch(LOCATION, "^/2281/documents/(.*)$")
			.expectBody().isEmpty();

		// Assert
		assertThat(response).isNotNull();
		verify(documentServiceMock).create(eq(documentCreateRequest), ArgumentMatchers.<DocumentFiles>any(), eq("2281"));
	}

	@Test
	void createWithEmptyMetadata() {

		// Arrange
		final var documentCreateRequest = DocumentCreateRequest.create()
			.withCreatedBy("b0000000-0000-0000-0000-000000000099")
			.withTitle("title")
			.withDescription("description")
			.withType("type")
			.withMetadataList(emptyList());

		final var multipartBodyBuilder = new MultipartBodyBuilder();
		multipartBodyBuilder.part("documentFiles", "file-content").filename("test.txt").contentType(TEXT_PLAIN);
		multipartBodyBuilder.part("document", documentCreateRequest);

		when(documentServiceMock.create(any(), any(), any())).thenReturn(Document.create());

		// Act
		final var response = webTestClient.post()
			.uri("/2281/documents")
			.contentType(MULTIPART_FORM_DATA)
			.body(fromMultipartData(multipartBodyBuilder.build()))
			.exchange()
			.expectStatus().isCreated()
			.expectHeader().contentType(ALL_VALUE)
			.expectHeader().exists(LOCATION)
			.expectHeader().valuesMatch(LOCATION, "^/2281/documents/(.*)$")
			.expectBody().isEmpty();

		// Assert
		assertThat(response).isNotNull();
		verify(documentServiceMock).create(eq(documentCreateRequest), ArgumentMatchers.<DocumentFiles>any(), eq("2281"));
	}

	@Test
	void update() {

		// Arrange
		final var registrationNumber = "2023-1337";
		final var documentUpdateRequest = DocumentUpdateRequest.create()
			.withUpdatedBy("b0000000-0000-0000-0000-000000000099")
			.withMetadataList(List.of(DocumentMetadata.create()
				.withKey("key")
				.withValue("value")));

		when(documentServiceMock.update(any(), anyBoolean(), any(), any())).thenReturn(Document.create());

		// Act
		final var response = webTestClient.patch()
			.uri("/2281/documents/" + registrationNumber)
			.contentType(APPLICATION_JSON)
			.bodyValue(documentUpdateRequest)
			.exchange()
			.expectStatus().isOk()
			.expectHeader().contentType(APPLICATION_JSON)
			.expectBody()
			.returnResult()
			.getResponseBody();

		// Assert
		assertThat(response).isNotNull();
		verify(documentServiceMock).update(registrationNumber, false, documentUpdateRequest, "2281");
	}

	@Test
	void updateConfidentiality() {

		// Arrange
		final var registrationNumber = "2023-1337";
		final var confidentialityUpdateRequest = ConfidentialityUpdateRequest.create()
			.withUpdatedBy("b0000000-0000-0000-0000-000000000099")
			.withConfidential(true)
			.withLegalCitation("Lorum ipsum");

		// Act
		webTestClient.patch()
			.uri("/2281/documents/" + registrationNumber + "/confidentiality")
			.contentType(APPLICATION_JSON)
			.bodyValue(confidentialityUpdateRequest)
			.exchange()
			.expectStatus().isNoContent()
			.expectBody()
			.isEmpty();

		// Assert
		verify(documentStatusServiceMock).updateConfidentiality(registrationNumber, confidentialityUpdateRequest, "2281");
	}

	@Test
	void updateResponsibilities() {

		// Arrange
		final var registrationNumber = "2023-1337";
		final var responsibilitiesUpdateRequest = DocumentResponsibilitiesUpdateRequest.create()
			.withUpdatedBy("b0000000-0000-0000-0000-000000000099")
			.withResponsibilities(List.of(DocumentResponsibility.create()
				.withPersonId("6b8d4a1c-34e2-4f73-a5f1-b7c2e9a0d8c4")));

		// Act
		webTestClient.put()
			.uri("/2281/documents/" + registrationNumber + "/responsibilities")
			.contentType(APPLICATION_JSON)
			.bodyValue(responsibilitiesUpdateRequest)
			.exchange()
			.expectStatus().isNoContent()
			.expectBody()
			.isEmpty();

		// Assert
		verify(documentResponsibilityServiceMock).updateResponsibilities(registrationNumber, responsibilitiesUpdateRequest, "2281");
	}

	@Test
	void publishLatestRevision() {

		// Arrange
		final var registrationNumber = "2023-1337";
		when(documentStatusServiceMock.publish(any(), any(), any(), any())).thenReturn(Document.create());

		// Act
		webTestClient.post()
			.uri("/2281/documents/" + registrationNumber + "/publish?updatedBy=b0000000-0000-0000-0000-000000000099")
			.exchange()
			.expectStatus().isOk()
			.expectHeader().contentType(APPLICATION_JSON);

		// Assert
		verify(documentStatusServiceMock).publish(registrationNumber, "b0000000-0000-0000-0000-000000000099", "2281", null);
	}

	@Test
	void publishSpecificRevision() {

		// Arrange
		final var registrationNumber = "2023-1337";
		when(documentStatusServiceMock.publish(any(), any(), any(), any())).thenReturn(Document.create());

		// Act
		webTestClient.post()
			.uri("/2281/documents/" + registrationNumber + "/publish?updatedBy=b0000000-0000-0000-0000-000000000099&revision=5")
			.exchange()
			.expectStatus().isOk()
			.expectHeader().contentType(APPLICATION_JSON);

		// Assert
		verify(documentStatusServiceMock).publish(registrationNumber, "b0000000-0000-0000-0000-000000000099", "2281", 5);
	}

	@Test
	void revokeLatestRevision() {

		// Arrange
		final var registrationNumber = "2023-1337";
		when(documentStatusServiceMock.revoke(any(), any(), any(), any())).thenReturn(Document.create());

		// Act
		webTestClient.post()
			.uri("/2281/documents/" + registrationNumber + "/revoke?updatedBy=b0000000-0000-0000-0000-000000000099")
			.exchange()
			.expectStatus().isOk()
			.expectHeader().contentType(APPLICATION_JSON);

		// Assert
		verify(documentStatusServiceMock).revoke(registrationNumber, "b0000000-0000-0000-0000-000000000099", "2281", null);
	}

	@Test
	void revokeSpecificRevision() {

		// Arrange
		final var registrationNumber = "2023-1337";
		when(documentStatusServiceMock.revoke(any(), any(), any(), any())).thenReturn(Document.create());

		// Act
		webTestClient.post()
			.uri("/2281/documents/" + registrationNumber + "/revoke?updatedBy=b0000000-0000-0000-0000-000000000099&revision=5")
			.exchange()
			.expectStatus().isOk()
			.expectHeader().contentType(APPLICATION_JSON);

		// Assert
		verify(documentStatusServiceMock).revoke(registrationNumber, "b0000000-0000-0000-0000-000000000099", "2281", 5);
	}

	@Test
	void unrevokeLatestRevision() {

		// Arrange
		final var registrationNumber = "2023-1337";
		when(documentStatusServiceMock.unrevoke(any(), any(), any(), any())).thenReturn(Document.create());

		// Act
		webTestClient.post()
			.uri("/2281/documents/" + registrationNumber + "/unrevoke?updatedBy=b0000000-0000-0000-0000-000000000099")
			.exchange()
			.expectStatus().isOk()
			.expectHeader().contentType(APPLICATION_JSON);

		// Assert
		verify(documentStatusServiceMock).unrevoke(registrationNumber, "b0000000-0000-0000-0000-000000000099", "2281", null);
	}

	@Test
	void unrevokeSpecificRevision() {

		// Arrange
		final var registrationNumber = "2023-1337";
		when(documentStatusServiceMock.unrevoke(any(), any(), any(), any())).thenReturn(Document.create());

		// Act
		webTestClient.post()
			.uri("/2281/documents/" + registrationNumber + "/unrevoke?updatedBy=b0000000-0000-0000-0000-000000000099&revision=5")
			.exchange()
			.expectStatus().isOk()
			.expectHeader().contentType(APPLICATION_JSON);

		// Assert
		verify(documentStatusServiceMock).unrevoke(registrationNumber, "b0000000-0000-0000-0000-000000000099", "2281", 5);
	}

	@Test
	void updateWithIncludeConfidential() {

		// Arrange
		final var includeConfidential = true;
		final var registrationNumber = "2023-1337";
		final var documentUpdateRequest = DocumentUpdateRequest.create()
			.withUpdatedBy("b0000000-0000-0000-0000-000000000099")
			.withMetadataList(List.of(DocumentMetadata.create()
				.withKey("key")
				.withValue("value")));

		when(documentServiceMock.update(any(), anyBoolean(), any(), any())).thenReturn(Document.create());

		// Act
		final var response = webTestClient.patch()
			.uri(uriBuilder -> uriBuilder.path("/2281/documents/" + registrationNumber)
				.queryParam("includeConfidential", includeConfidential)
				.build())
			.contentType(APPLICATION_JSON)
			.bodyValue(documentUpdateRequest)
			.exchange()
			.expectStatus().isOk()
			.expectHeader().contentType(APPLICATION_JSON)
			.expectBody()
			.returnResult()
			.getResponseBody();

		// Assert
		assertThat(response).isNotNull();
		verify(documentServiceMock).update(registrationNumber, includeConfidential, documentUpdateRequest, "2281");
	}

	@Test
	void search() {

		// Arrange
		final var query = "string";
		final var page = 1;
		final var size = 10;
		final var sort = "created,asc";

		when(documentSearchServiceMock.search(any(), anyBoolean(), anyBoolean(), any(), any())).thenReturn(PagedDocumentResponse.create().withDocuments(List.of(Document.create())));

		// Act
		final var response = webTestClient.get()
			.uri(uriBuilder -> uriBuilder.path("/2281/documents")
				.queryParam("query", query)
				.queryParam("page", page)
				.queryParam("size", size)
				.queryParam("sort", sort)
				.build())
			.exchange()
			.expectStatus().isOk()
			.expectHeader().contentType(APPLICATION_JSON)
			.expectBody()
			.returnResult()
			.getResponseBody();

		// Assert
		assertThat(response).isNotNull();
		verify(documentSearchServiceMock).search(query, false, false, PageRequest.of(page, size, Sort.by(asc("created"))), "2281");
	}

	@ParameterizedTest
	@ValueSource(booleans = {
		true, false
	})
	void searchWithIncludeConfidential(boolean includeConfidential) {

		// Arrange
		final var query = "string";
		final var page = 1;
		final var size = 10;
		final var sort = "created,asc";

		when(documentSearchServiceMock.search(any(), anyBoolean(), anyBoolean(), any(), any())).thenReturn(PagedDocumentResponse.create().withDocuments(List.of(Document.create())));

		// Act
		final var response = webTestClient.get()
			.uri(uriBuilder -> uriBuilder.path("/2281/documents")
				.queryParam("query", query)
				.queryParam("page", page)
				.queryParam("size", size)
				.queryParam("sort", sort)
				.queryParam("includeConfidential", includeConfidential)
				.build())
			.exchange()
			.expectStatus().isOk()
			.expectHeader().contentType(APPLICATION_JSON)
			.expectBody()
			.returnResult()
			.getResponseBody();

		// Assert
		assertThat(response).isNotNull();
		verify(documentSearchServiceMock).search(query, includeConfidential, false, PageRequest.of(page, size, Sort.by(asc("created"))), "2281");
	}

	@ParameterizedTest
	@ValueSource(booleans = {
		true, false
	})
	void searchWithOnlyLatestRevision(boolean onlyLatestRevision) {

		// Arrange
		final var query = "string";
		final var page = 1;
		final var size = 10;
		final var sort = "created,asc";

		when(documentSearchServiceMock.search(any(), anyBoolean(), anyBoolean(), any(), any())).thenReturn(PagedDocumentResponse.create().withDocuments(List.of(Document.create())));

		// Act
		final var response = webTestClient.get()
			.uri(uriBuilder -> uriBuilder.path("/2281/documents")
				.queryParam("query", query)
				.queryParam("page", page)
				.queryParam("size", size)
				.queryParam("sort", sort)
				.queryParam("onlyLatestRevision", onlyLatestRevision)
				.build())
			.exchange()
			.expectStatus().isOk()
			.expectHeader().contentType(APPLICATION_JSON)
			.expectBody()
			.returnResult()
			.getResponseBody();

		// Assert
		assertThat(response).isNotNull();
		verify(documentSearchServiceMock).search(query, false, onlyLatestRevision, PageRequest.of(page, size, Sort.by(asc("created"))), "2281");
	}

	@Test
	void searchFileMatches() {

		// Arrange
		final var query = "string";
		final var page = 1;
		final var size = 10;
		final var sort = "created,asc";

		when(documentSearchServiceMock.searchFileMatches(any(), anyBoolean(), anyBoolean(), any(), any())).thenReturn(PagedDocumentMatchResponse.create().withDocuments(List.of(DocumentMatch.create())));

		// Act
		final var response = webTestClient.get()
			.uri(uriBuilder -> uriBuilder.path("/2281/documents/file-matches")
				.queryParam("query", query)
				.queryParam("page", page)
				.queryParam("size", size)
				.queryParam("sort", sort)
				.build())
			.exchange()
			.expectStatus().isOk()
			.expectHeader().contentType(APPLICATION_JSON)
			.expectBody()
			.returnResult()
			.getResponseBody();

		// Assert
		assertThat(response).isNotNull();
		verify(documentSearchServiceMock).searchFileMatches(List.of(query), false, false, PageRequest.of(page, size, Sort.by(asc("created"))), "2281");
	}

	@Test
	void searchFileMatchesWithMultipleQueries() {

		// Arrange
		final var q1 = "alpha";
		final var q2 = "beta";
		final var q3 = "gamma";
		final var page = 1;
		final var size = 10;
		final var sort = "created,asc";

		when(documentSearchServiceMock.searchFileMatches(any(), anyBoolean(), anyBoolean(), any(), any())).thenReturn(PagedDocumentMatchResponse.create().withDocuments(List.of(DocumentMatch.create())));

		// Act
		webTestClient.get()
			.uri(uriBuilder -> uriBuilder.path("/2281/documents/file-matches")
				.queryParam("query", q1)
				.queryParam("query", q2)
				.queryParam("query", q3)
				.queryParam("page", page)
				.queryParam("size", size)
				.queryParam("sort", sort)
				.build())
			.exchange()
			.expectStatus().isOk();

		// Assert — preserves order from the request.
		verify(documentSearchServiceMock).searchFileMatches(List.of(q1, q2, q3), false, false, PageRequest.of(page, size, Sort.by(asc("created"))), "2281");
	}

	@ParameterizedTest
	@ValueSource(booleans = {
		true, false
	})
	void searchFileMatchesWithIncludeConfidential(boolean includeConfidential) {

		// Arrange
		final var query = "string";
		final var page = 1;
		final var size = 10;
		final var sort = "created,asc";

		when(documentSearchServiceMock.searchFileMatches(any(), anyBoolean(), anyBoolean(), any(), any())).thenReturn(PagedDocumentMatchResponse.create().withDocuments(List.of(DocumentMatch.create())));

		// Act
		webTestClient.get()
			.uri(uriBuilder -> uriBuilder.path("/2281/documents/file-matches")
				.queryParam("query", query)
				.queryParam("page", page)
				.queryParam("size", size)
				.queryParam("sort", sort)
				.queryParam("includeConfidential", includeConfidential)
				.build())
			.exchange()
			.expectStatus().isOk();

		// Assert
		verify(documentSearchServiceMock).searchFileMatches(List.of(query), includeConfidential, false, PageRequest.of(page, size, Sort.by(asc("created"))), "2281");
	}

	@ParameterizedTest
	@ValueSource(booleans = {
		true, false
	})
	void searchFileMatchesWithOnlyLatestRevision(boolean onlyLatestRevision) {

		// Arrange
		final var query = "string";
		final var page = 1;
		final var size = 10;
		final var sort = "created,asc";

		when(documentSearchServiceMock.searchFileMatches(any(), anyBoolean(), anyBoolean(), any(), any())).thenReturn(PagedDocumentMatchResponse.create().withDocuments(List.of(DocumentMatch.create())));

		// Act
		webTestClient.get()
			.uri(uriBuilder -> uriBuilder.path("/2281/documents/file-matches")
				.queryParam("query", query)
				.queryParam("page", page)
				.queryParam("size", size)
				.queryParam("sort", sort)
				.queryParam("onlyLatestRevision", onlyLatestRevision)
				.build())
			.exchange()
			.expectStatus().isOk();

		// Assert
		verify(documentSearchServiceMock).searchFileMatches(List.of(query), false, onlyLatestRevision, PageRequest.of(page, size, Sort.by(asc("created"))), "2281");
	}

	@Test
	void read() {

		// Arrange
		final var registrationNumber = "2023-1337";

		when(documentServiceMock.read(any(), anyBoolean(), anyBoolean(), any())).thenReturn(Document.create());

		// Act
		final var response = webTestClient.get()
			.uri("/2281/documents/" + registrationNumber)
			.exchange()
			.expectStatus().isOk()
			.expectHeader().contentType(APPLICATION_JSON)
			.expectBody()
			.returnResult()
			.getResponseBody();

		// Assert
		assertThat(response).isNotNull();
		verify(documentServiceMock).read(registrationNumber, false, false, "2281");
	}

	@Test
	void readWithIncludeConfidential() {

		// Arrange
		final var includeConfidential = true;
		final var registrationNumber = "2023-1337";

		when(documentServiceMock.read(any(), anyBoolean(), anyBoolean(), any())).thenReturn(Document.create());

		// Act
		final var response = webTestClient.get()
			.uri(uriBuilder -> uriBuilder.path("/2281/documents/" + registrationNumber)
				.queryParam("includeConfidential", includeConfidential)
				.build())
			.exchange()
			.expectStatus().isOk()
			.expectHeader().contentType(APPLICATION_JSON)
			.expectBody()
			.returnResult()
			.getResponseBody();

		// Assert
		assertThat(response).isNotNull();
		verify(documentServiceMock).read(registrationNumber, includeConfidential, false, "2281");
	}

	@Test
	void readFile() {

		// Arrange
		final var documentDataId = randomUUID().toString();
		final var registrationNumber = "2023-1337";

		// Act
		webTestClient.get()
			.uri("/2281/documents/" + registrationNumber + "/files/" + documentDataId)
			.exchange()
			.expectStatus().isOk()
			.expectBody()
			.isEmpty();

		// Assert
		verify(documentFileServiceMock).readFile(eq(registrationNumber), eq(documentDataId), eq(false), eq(false), any(HttpServletResponse.class), eq("2281"));
	}

	@Test
	void readFileWithIncludeConfidential() {

		// Arrange
		final var documentDataId = randomUUID().toString();
		final var includeConfidential = true;
		final var registrationNumber = "2023-1337";

		// Act
		webTestClient.get()
			.uri(uriBuilder -> uriBuilder.path("/2281/documents/" + registrationNumber + "/files/" + documentDataId)
				.queryParam("includeConfidential", includeConfidential)
				.build())
			.exchange()
			.expectStatus().isOk()
			.expectBody()
			.isEmpty();

		// Assert
		verify(documentFileServiceMock).readFile(eq(registrationNumber), eq(documentDataId), eq(includeConfidential), eq(false), any(HttpServletResponse.class), eq("2281"));
	}

	@Test
	void addFile() {

		// Arrange
		final var registrationNumber = "2023-1337";

		// Arrange
		final var documentDataCreateRequest = DocumentDataCreateRequest.create()
			.withCreatedBy("b0000000-0000-0000-0000-000000000099");
		final var multipartBodyBuilder = new MultipartBodyBuilder();
		multipartBodyBuilder.part("documentFile", "file-content").filename("test1.txt").contentType(TEXT_PLAIN);
		multipartBodyBuilder.part("document", documentDataCreateRequest);

		when(documentFileServiceMock.addOrReplaceFiles(any(), any(), any(), any())).thenReturn(Document.create());

		// Act
		webTestClient.put()
			.uri("/2281/documents/" + registrationNumber + "/files")
			.contentType(MULTIPART_FORM_DATA)
			.body(fromMultipartData(multipartBodyBuilder.build()))
			.exchange()
			.expectStatus().isNoContent()
			.expectBody()
			.isEmpty();

		// Assert — backwards-compatible singular upload is wrapped into a one-element DocumentFiles.
		verify(documentFileServiceMock).addOrReplaceFiles(eq(registrationNumber), eq(documentDataCreateRequest), ArgumentMatchers.<DocumentFiles>any(), eq("2281"));
	}

	@Test
	void addMultipleFiles() {

		// Arrange
		final var registrationNumber = "2023-1337";

		final var documentDataCreateRequest = DocumentDataCreateRequest.create()
			.withCreatedBy("b0000000-0000-0000-0000-000000000099");
		final var multipartBodyBuilder = new MultipartBodyBuilder();
		multipartBodyBuilder.part("documentFiles", "file-content-1").filename("test1.txt").contentType(TEXT_PLAIN);
		multipartBodyBuilder.part("documentFiles", "file-content-2").filename("test2.txt").contentType(TEXT_PLAIN);
		multipartBodyBuilder.part("documentFiles", "file-content-3").filename("test3.txt").contentType(TEXT_PLAIN);
		multipartBodyBuilder.part("document", documentDataCreateRequest);

		when(documentFileServiceMock.addOrReplaceFiles(any(), any(), any(), any())).thenReturn(Document.create());

		// Act
		webTestClient.put()
			.uri("/2281/documents/" + registrationNumber + "/files")
			.contentType(MULTIPART_FORM_DATA)
			.body(fromMultipartData(multipartBodyBuilder.build()))
			.exchange()
			.expectStatus().isNoContent()
			.expectBody()
			.isEmpty();

		// Assert — resource delegates to the batch service method; the service is responsible for single-revision semantics.
		verify(documentFileServiceMock).addOrReplaceFiles(eq(registrationNumber), eq(documentDataCreateRequest), ArgumentMatchers.<DocumentFiles>any(), eq("2281"));
	}

	@Test
	void deleteFile() {

		// Arrange
		final var documentDataId = randomUUID().toString();
		final var registrationNumber = "2023-1337";

		// Act
		webTestClient.delete()
			.uri("/2281/documents/" + registrationNumber + "/files/" + documentDataId)
			.exchange()
			.expectStatus().isNoContent()
			.expectBody()
			.isEmpty();

		// Assert
		verify(documentFileServiceMock).deleteFile(registrationNumber, documentDataId, "2281");
	}
}
