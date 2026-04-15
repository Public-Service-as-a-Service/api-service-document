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
import org.springframework.web.multipart.MultipartFile;
import se.sundsvall.document.Application;
import se.sundsvall.document.api.model.Confidentiality;
import se.sundsvall.document.api.model.ConfidentialityUpdateRequest;
import se.sundsvall.document.api.model.Document;
import se.sundsvall.document.api.model.DocumentCreateRequest;
import se.sundsvall.document.api.model.DocumentDataCreateRequest;
import se.sundsvall.document.api.model.DocumentFiles;
import se.sundsvall.document.api.model.DocumentMetadata;
import se.sundsvall.document.api.model.DocumentUpdateRequest;
import se.sundsvall.document.api.model.PagedDocumentResponse;
import se.sundsvall.document.api.validation.DocumentTypeValidator;
import se.sundsvall.document.service.DocumentService;

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
	private DocumentTypeValidator validationUtilityMock;

	@Autowired
	private WebTestClient webTestClient;

	@Test
	void create() {

		// Arrange
		final var documentCreateRequest = DocumentCreateRequest.create()
			.withConfidentiality(Confidentiality.create().withConfidential(true).withLegalCitation("legalCitation"))
			.withCreatedBy("user")
			.withDescription("description")
			.withMetadataList(List.of(DocumentMetadata.create()
				.withKey("key")
				.withValue("value")))
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
	void update() {

		// Arrange
		final var registrationNumber = "2023-1337";
		final var documentUpdateRequest = DocumentUpdateRequest.create()
			.withCreatedBy("user")
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
			.withChangedBy("user")
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
		verify(documentServiceMock).updateConfidentiality(registrationNumber, confidentialityUpdateRequest, "2281");
	}

	@Test
	void updateWithIncludeConfidential() {

		// Arrange
		final var includeConfidential = true;
		final var registrationNumber = "2023-1337";
		final var documentUpdateRequest = DocumentUpdateRequest.create()
			.withCreatedBy("user")
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

		when(documentServiceMock.search(any(), anyBoolean(), anyBoolean(), any(), any())).thenReturn(PagedDocumentResponse.create().withDocuments(List.of(Document.create())));

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
		verify(documentServiceMock).search(query, false, false, PageRequest.of(page, size, Sort.by(asc("created"))), "2281");
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

		when(documentServiceMock.search(any(), anyBoolean(), anyBoolean(), any(), any())).thenReturn(PagedDocumentResponse.create().withDocuments(List.of(Document.create())));

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
		verify(documentServiceMock).search(query, includeConfidential, false, PageRequest.of(page, size, Sort.by(asc("created"))), "2281");
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

		when(documentServiceMock.search(any(), anyBoolean(), anyBoolean(), any(), any())).thenReturn(PagedDocumentResponse.create().withDocuments(List.of(Document.create())));

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
		verify(documentServiceMock).search(query, false, onlyLatestRevision, PageRequest.of(page, size, Sort.by(asc("created"))), "2281");
	}

	@Test
	void read() {

		// Arrange
		final var registrationNumber = "2023-1337";

		when(documentServiceMock.read(any(), anyBoolean(), any())).thenReturn(Document.create());

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
		verify(documentServiceMock).read(registrationNumber, false, "2281");
	}

	@Test
	void readWithIncludeConfidential() {

		// Arrange
		final var includeConfidential = true;
		final var registrationNumber = "2023-1337";

		when(documentServiceMock.read(any(), anyBoolean(), any())).thenReturn(Document.create());

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
		verify(documentServiceMock).read(registrationNumber, includeConfidential, "2281");
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
		verify(documentServiceMock).readFile(eq(registrationNumber), eq(documentDataId), eq(false), any(HttpServletResponse.class), eq("2281"));
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
		verify(documentServiceMock).readFile(eq(registrationNumber), eq(documentDataId), eq(includeConfidential), any(HttpServletResponse.class), eq("2281"));
	}

	@Test
	void addFile() {

		// Arrange
		final var registrationNumber = "2023-1337";

		// Arrange
		final var documentDataCreateRequest = DocumentDataCreateRequest.create()
			.withCreatedBy("user");
		final var multipartBodyBuilder = new MultipartBodyBuilder();
		multipartBodyBuilder.part("documentFile", "file-content").filename("test1.txt").contentType(TEXT_PLAIN);
		multipartBodyBuilder.part("document", documentDataCreateRequest);

		when(documentServiceMock.addOrReplaceFile(any(), any(), any(), any())).thenReturn(Document.create());

		// Act
		webTestClient.put()
			.uri("/2281/documents/" + registrationNumber + "/files")
			.contentType(MULTIPART_FORM_DATA)
			.body(fromMultipartData(multipartBodyBuilder.build()))
			.exchange()
			.expectStatus().isNoContent()
			.expectBody()
			.isEmpty();

		// Assert
		verify(documentServiceMock).addOrReplaceFile(eq(registrationNumber), eq(documentDataCreateRequest), ArgumentMatchers.<MultipartFile>any(), eq("2281"));
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
		verify(documentServiceMock).deleteFile(registrationNumber, documentDataId, "2281");
	}
}
