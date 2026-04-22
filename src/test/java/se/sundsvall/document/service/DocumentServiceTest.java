package se.sundsvall.document.service;

import generated.se.sundsvall.eventlog.Event;
import generated.se.sundsvall.eventlog.Metadata;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import se.sundsvall.dept44.problem.ThrowableProblem;
import se.sundsvall.document.api.model.ConfidentialityUpdateRequest;
import se.sundsvall.document.api.model.Document;
import se.sundsvall.document.api.model.DocumentCreateRequest;
import se.sundsvall.document.api.model.DocumentFiles;
import se.sundsvall.document.api.model.DocumentMetadata;
import se.sundsvall.document.api.model.DocumentResponsibilitiesUpdateRequest;
import se.sundsvall.document.api.model.DocumentResponsibility;
import se.sundsvall.document.api.model.DocumentStatus;
import se.sundsvall.document.api.model.DocumentUpdateRequest;
import se.sundsvall.document.integration.db.DocumentDataRepository;
import se.sundsvall.document.integration.db.DocumentRepository;
import se.sundsvall.document.integration.db.DocumentResponsibilityRepository;
import se.sundsvall.document.integration.db.DocumentTypeRepository;
import se.sundsvall.document.integration.db.model.ConfidentialityEmbeddable;
import se.sundsvall.document.integration.db.model.DocumentDataEntity;
import se.sundsvall.document.integration.db.model.DocumentEntity;
import se.sundsvall.document.integration.db.model.DocumentMetadataEmbeddable;
import se.sundsvall.document.integration.db.model.DocumentResponsibilityEntity;
import se.sundsvall.document.integration.db.model.DocumentTypeEntity;
import se.sundsvall.document.integration.elasticsearch.DocumentIndexEntity;
import se.sundsvall.document.integration.eventlog.EventLogClient;
import se.sundsvall.document.integration.eventlog.configuration.EventlogProperties;
import se.sundsvall.document.service.extraction.TextExtractor;
import se.sundsvall.document.service.storage.BinaryStore;
import se.sundsvall.document.service.storage.PutResult;
import se.sundsvall.document.service.storage.StorageRef;

import static generated.se.sundsvall.eventlog.EventType.UPDATE;
import static java.time.OffsetDateTime.now;
import static java.time.ZoneId.systemDefault;
import static java.util.Collections.emptyList;
import static java.util.Optional.empty;
import static java.util.UUID.randomUUID;
import static org.apache.commons.io.IOUtils.toByteArray;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.assertj.core.api.Assertions.within;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.data.domain.Sort.Direction.DESC;
import static se.sundsvall.document.service.InclusionFilter.CONFIDENTIAL_AND_PUBLIC;
import static se.sundsvall.document.service.InclusionFilter.PUBLIC;

@ExtendWith(MockitoExtension.class)
class DocumentServiceTest {

	private static final String FILE_NAME = "image.png";
	private static final String MIME_TYPE = "image/png";
	private static final long FILE_SIZE_IN_BYTES = 227546L;
	private static final OffsetDateTime CREATED = now(systemDefault());
	private static final boolean CONFIDENTIAL = true;
	private static final String CREATED_BY = "b0000000-0000-0000-0000-000000000099";
	private static final String PERSON_ID = "6b8d4a1c-34e2-4f73-a5f1-b7c2e9a0d8c4";
	private static final String DESCRIPTION = "Description";
	private static final String ID = randomUUID().toString();
	private static final String LEGAL_CITATION = "legalCitation";
	private static final String METADATA_KEY = "key";
	private static final String METADATA_VALUE = "value";
	private static final String MUNICIPALITY_ID = "2281";
	private static final String REGISTRATION_NUMBER = "2023-2281-4";
	private static final String DOCUMENT_DATA_ID = randomUUID().toString();
	private static final String STORAGE_LOCATOR = randomUUID().toString();
	private static final String DOCUMENT_TYPE = "documentType";
	private static final String DOCUMENT_TYPE_DISPLAYNAME = "documentTypeDisplayname";
	private static final int REVISION = 1;
	private static final LocalDate VALID_FROM = LocalDate.of(2026, 4, 15);
	private static final LocalDate VALID_TO = LocalDate.of(2027, 4, 15);

	@Mock
	private EventlogProperties eventlogPropertiesMock;

	@Mock
	private DocumentRepository documentRepositoryMock;

	@Mock
	private DocumentResponsibilityRepository documentResponsibilityRepositoryMock;

	@Mock
	private DocumentTypeRepository documentTypeRepositoryMock;

	@Mock
	private RegistrationNumberService registrationNumberServiceMock;

	@Mock
	private DocumentStatusPolicy statusPolicyMock;

	@Mock
	private BinaryStore binaryStoreMock;

	@Mock
	private DocumentDataRepository documentDataRepositoryMock;

	@Mock
	private TextExtractor textExtractorMock;

	@Mock
	private ApplicationEventPublisher applicationEventPublisherMock;

	@Mock
	private ElasticsearchOperations elasticsearchOperationsMock;

	@Mock
	private SearchHits<DocumentIndexEntity> searchHitsMock;

	@Mock
	private HttpServletResponse httpServletResponseMock;

	@Mock
	private ServletOutputStream servletOutputStreamMock;

	@Mock
	private Page<DocumentEntity> pageMock;

	private DocumentService documentService;

	@Captor
	private ArgumentCaptor<DocumentEntity> documentEntityCaptor;

	@Captor
	private ArgumentCaptor<List<DocumentEntity>> documentEntitiesCaptor;

	@Captor
	private ArgumentCaptor<List<DocumentResponsibilityEntity>> responsibilityEntitiesCaptor;

	private TestEventLogClient eventLogClient;

	@BeforeEach
	void setUp() {
		eventLogClient = new TestEventLogClient();
		documentService = new DocumentService(
			binaryStoreMock,
			documentRepositoryMock,
			documentResponsibilityRepositoryMock,
			documentTypeRepositoryMock,
			documentDataRepositoryMock,
			registrationNumberServiceMock,
			statusPolicyMock,
			new DocumentEventPublisher(Optional.of(eventLogClient), Optional.of(eventlogPropertiesMock)),
			textExtractorMock,
			applicationEventPublisherMock,
			Optional.of(elasticsearchOperationsMock));
	}

	@Test
	void create() throws IOException {

		// Arrange
		final var documentCreateRequest = DocumentCreateRequest.create()
			.withCreatedBy(CREATED_BY)
			.withMetadataList(List.of(DocumentMetadata.create().withKey(METADATA_KEY).withValue(METADATA_VALUE)))
			.withResponsibilities(List.of(DocumentResponsibility.create().withPersonId(PERSON_ID)))
			.withType(DOCUMENT_TYPE)
			.withValidFrom(VALID_FROM)
			.withValidTo(VALID_TO);

		final var file = new File("src/test/resources/files/image.png");
		final var multipartFile = (MultipartFile) new MockMultipartFile("file", file.getName(), "text/plain", toByteArray(new FileInputStream(file)));
		final var documentFiles = DocumentFiles.create().withFiles(List.of(multipartFile));
		final var newLocator = randomUUID().toString();

		when(documentTypeRepositoryMock.findByMunicipalityIdAndType(MUNICIPALITY_ID, DOCUMENT_TYPE)).thenReturn(Optional.of(DocumentTypeEntity.create().withType(DOCUMENT_TYPE)));
		when(registrationNumberServiceMock.generateRegistrationNumber(MUNICIPALITY_ID)).thenReturn(REGISTRATION_NUMBER);
		when(binaryStoreMock.put(any(InputStream.class), anyLong(), anyString(), anyMap())).thenReturn(new PutResult(StorageRef.s3(newLocator), "hash"));
		when(textExtractorMock.extract(any(InputStream.class), anyString(), anyLong())).thenReturn(TextExtractor.ExtractedText.unsupported("text/plain"));
		when(documentRepositoryMock.save(any(DocumentEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(documentResponsibilityRepositoryMock.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

		// Act
		final var result = documentService.create(documentCreateRequest, documentFiles, MUNICIPALITY_ID);

		// Assert
		assertThat(result).isNotNull();
		assertThat(result.getResponsibilities()).containsExactly(DocumentResponsibility.create().withPersonId(PERSON_ID));

		verify(documentTypeRepositoryMock).findByMunicipalityIdAndType(MUNICIPALITY_ID, DOCUMENT_TYPE);
		verify(registrationNumberServiceMock).generateRegistrationNumber(MUNICIPALITY_ID);
		verify(binaryStoreMock).put(any(InputStream.class), eq(file.length()), eq("text/plain"), eq(Map.of("original-filename", file.getName(), "municipality-id", MUNICIPALITY_ID)));
		verify(documentRepositoryMock).save(documentEntityCaptor.capture());
		verify(documentResponsibilityRepositoryMock).saveAll(responsibilityEntitiesCaptor.capture());

		final var capturedDocumentEntity = documentEntityCaptor.getValue();
		assertThat(capturedDocumentEntity).isNotNull();
		assertThat(capturedDocumentEntity.getCreatedBy()).isEqualTo(CREATED_BY);
		assertThat(capturedDocumentEntity.getDocumentData())
			.hasSize(1)
			.extracting(DocumentDataEntity::getStorageLocator, DocumentDataEntity::getFileSizeInBytes)
			.containsExactly(tuple(newLocator, file.length()));
		assertThat(capturedDocumentEntity.getMetadata()).isEqualTo(List.of(DocumentMetadataEmbeddable.create().withKey(METADATA_KEY).withValue(METADATA_VALUE)));
		assertThat(capturedDocumentEntity.getMunicipalityId()).isEqualTo(MUNICIPALITY_ID);
		assertThat(capturedDocumentEntity.getRegistrationNumber()).isEqualTo(REGISTRATION_NUMBER);
		assertThat(capturedDocumentEntity.getType()).isNotNull().satisfies(type -> {
			assertThat(type.getType()).isEqualTo(DOCUMENT_TYPE);
		});
		assertThat(capturedDocumentEntity.getValidFrom()).isEqualTo(VALID_FROM);
		assertThat(capturedDocumentEntity.getValidTo()).isEqualTo(VALID_TO);

		assertThat(responsibilityEntitiesCaptor.getValue())
			.extracting(DocumentResponsibilityEntity::getMunicipalityId, DocumentResponsibilityEntity::getRegistrationNumber, DocumentResponsibilityEntity::getPersonId,
				DocumentResponsibilityEntity::getCreatedBy)
			.containsExactly(tuple(MUNICIPALITY_ID, REGISTRATION_NUMBER, PERSON_ID, CREATED_BY));
	}

	@Test
	void createWithMultipleFiles() throws IOException {

		// Arrange
		final var documentCreateRequest = DocumentCreateRequest.create()
			.withCreatedBy(CREATED_BY)
			.withMetadataList(List.of(DocumentMetadata.create().withKey(METADATA_KEY).withValue(METADATA_VALUE)))
			.withType(DOCUMENT_TYPE);

		final var file1 = new File("src/test/resources/files/image.png");
		final var file2 = new File("src/test/resources/files/readme.txt");
		final var multipartFile1 = (MultipartFile) new MockMultipartFile("file1", file1.getName(), "image/png", toByteArray(new FileInputStream(file1)));
		final var multipartFile2 = (MultipartFile) new MockMultipartFile("file2", file2.getName(), "text/plain", toByteArray(new FileInputStream(file2)));
		final var documentFiles = DocumentFiles.create().withFiles(List.of(multipartFile1, multipartFile2));

		when(documentTypeRepositoryMock.findByMunicipalityIdAndType(MUNICIPALITY_ID, DOCUMENT_TYPE)).thenReturn(Optional.of(DocumentTypeEntity.create().withType(DOCUMENT_TYPE)));
		when(registrationNumberServiceMock.generateRegistrationNumber(MUNICIPALITY_ID)).thenReturn(REGISTRATION_NUMBER);
		when(binaryStoreMock.put(any(InputStream.class), anyLong(), anyString(), anyMap())).thenAnswer(invocation -> new PutResult(StorageRef.s3(randomUUID().toString()), "hash"));
		when(textExtractorMock.extract(any(InputStream.class), anyString(), anyLong())).thenReturn(TextExtractor.ExtractedText.unsupported("text/plain"));
		when(documentRepositoryMock.save(any(DocumentEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

		// Act
		final var result = documentService.create(documentCreateRequest, documentFiles, MUNICIPALITY_ID);

		// Assert
		assertThat(result).isNotNull();

		verify(documentTypeRepositoryMock).findByMunicipalityIdAndType(MUNICIPALITY_ID, DOCUMENT_TYPE);
		verify(registrationNumberServiceMock).generateRegistrationNumber(MUNICIPALITY_ID);
		verify(binaryStoreMock).put(any(InputStream.class), eq(file1.length()), eq("image/png"), anyMap());
		verify(binaryStoreMock).put(any(InputStream.class), eq(file2.length()), eq("text/plain"), anyMap());
		verify(documentRepositoryMock).save(documentEntityCaptor.capture());

		final var capturedDocumentEntity = documentEntityCaptor.getValue();
		assertThat(capturedDocumentEntity).isNotNull();
		assertThat(capturedDocumentEntity.getDocumentData())
			.hasSize(2)
			.extracting(DocumentDataEntity::getMimeType, DocumentDataEntity::getFileName, DocumentDataEntity::getFileSizeInBytes)
			.containsExactlyInAnyOrder(
				tuple("text/plain", "readme.txt", 17L),
				tuple("image/png", "image.png", 227546L));
		assertThat(capturedDocumentEntity.getDocumentData())
			.extracting(DocumentDataEntity::getStorageLocator)
			.doesNotContainNull();
		assertThat(capturedDocumentEntity.getCreatedBy()).isEqualTo(CREATED_BY);
		assertThat(capturedDocumentEntity.getMetadata()).isEqualTo(List.of(DocumentMetadataEmbeddable.create().withKey(METADATA_KEY).withValue(METADATA_VALUE)));
		assertThat(capturedDocumentEntity.getMunicipalityId()).isEqualTo(MUNICIPALITY_ID);
		assertThat(capturedDocumentEntity.getRegistrationNumber()).isEqualTo(REGISTRATION_NUMBER);
		assertThat(capturedDocumentEntity.getType()).isNotNull().satisfies(type -> {
			assertThat(type.getType()).isEqualTo(DOCUMENT_TYPE);
		});
	}

	@Test
	void readByRegistrationNumber() {

		// Arrange
		final var includeConfidential = false;

		when(documentRepositoryMock.findTopByMunicipalityIdAndRegistrationNumberAndConfidentialityConfidentialInOrderByRevisionDesc(MUNICIPALITY_ID, REGISTRATION_NUMBER, PUBLIC.getValue())).thenReturn(Optional.of(createDocumentEntity()));

		// Act
		final var result = documentService.read(REGISTRATION_NUMBER, includeConfidential, true, MUNICIPALITY_ID);

		// Assert
		assertThat(result).isNotNull();
		assertThat(result.getCreated()).isEqualTo(CREATED);
		assertThat(result.getCreatedBy()).isEqualTo(CREATED_BY);
		assertThat(result.getId()).isEqualTo(ID);
		assertThat(result.getMetadataList()).isEqualTo(List.of(DocumentMetadata.create().withKey(METADATA_KEY).withValue(METADATA_VALUE)));
		assertThat(result.getMunicipalityId()).isEqualTo(MUNICIPALITY_ID);
		assertThat(result.getRegistrationNumber()).isEqualTo(REGISTRATION_NUMBER);
		assertThat(result.getRevision()).isEqualTo(REVISION);

		verify(documentRepositoryMock).findTopByMunicipalityIdAndRegistrationNumberAndConfidentialityConfidentialInOrderByRevisionDesc(MUNICIPALITY_ID, REGISTRATION_NUMBER, PUBLIC.getValue());
		verifyNoInteractions(binaryStoreMock);
	}

	@Test
	void readByRegistrationNumberNotFound() {

		// Arrange
		final var includeConfidential = false;

		when(documentRepositoryMock.findTopByMunicipalityIdAndRegistrationNumberAndConfidentialityConfidentialInOrderByRevisionDesc(MUNICIPALITY_ID, REGISTRATION_NUMBER, PUBLIC.getValue())).thenReturn(empty());

		// Act
		final var exception = assertThrows(ThrowableProblem.class, () -> documentService.read(REGISTRATION_NUMBER, includeConfidential, true, MUNICIPALITY_ID));

		// Assert
		assertThat(exception).isNotNull();
		assertThat(exception.getMessage()).isEqualTo("Not Found: No document with registrationNumber: '2023-2281-4' could be found!");

		verify(documentRepositoryMock).findTopByMunicipalityIdAndRegistrationNumberAndConfidentialityConfidentialInOrderByRevisionDesc(MUNICIPALITY_ID, REGISTRATION_NUMBER, PUBLIC.getValue());
	}

	@Test
	void readByRegistrationNumberAndRevision() {

		// Arrange
		final var includeConfidential = false;

		when(documentRepositoryMock.findByMunicipalityIdAndRegistrationNumberAndRevisionAndConfidentialityConfidentialIn(MUNICIPALITY_ID, REGISTRATION_NUMBER, REVISION, PUBLIC.getValue())).thenReturn(Optional.of(createDocumentEntity()));

		// Act
		final var result = documentService.read(REGISTRATION_NUMBER, REVISION, includeConfidential, MUNICIPALITY_ID);

		// Assert
		assertThat(result).isNotNull();
		assertThat(result.getCreated()).isEqualTo(CREATED);
		assertThat(result.getCreatedBy()).isEqualTo(CREATED_BY);
		assertThat(result.getId()).isEqualTo(ID);
		assertThat(result.getMetadataList()).isEqualTo(List.of(DocumentMetadata.create().withKey(METADATA_KEY).withValue(METADATA_VALUE)));
		assertThat(result.getMunicipalityId()).isEqualTo(MUNICIPALITY_ID);
		assertThat(result.getRegistrationNumber()).isEqualTo(REGISTRATION_NUMBER);
		assertThat(result.getRevision()).isEqualTo(REVISION);

		verify(documentRepositoryMock).findByMunicipalityIdAndRegistrationNumberAndRevisionAndConfidentialityConfidentialIn(MUNICIPALITY_ID, REGISTRATION_NUMBER, REVISION, PUBLIC.getValue());
	}

	@Test
	void readByRegistrationNumberAndRevisionWhenNotFound() {

		// Arrange
		final var includeConfidential = false;

		when(documentRepositoryMock.findByMunicipalityIdAndRegistrationNumberAndRevisionAndConfidentialityConfidentialIn(MUNICIPALITY_ID, REGISTRATION_NUMBER, REVISION, PUBLIC.getValue())).thenReturn(empty());

		// Act
		final var exception = assertThrows(ThrowableProblem.class, () -> documentService.read(REGISTRATION_NUMBER, REVISION, includeConfidential, MUNICIPALITY_ID));

		// Assert
		assertThat(exception).isNotNull();
		assertThat(exception.getMessage()).isEqualTo("Not Found: No document with registrationNumber: '2023-2281-4' and revision: '1' could be found!");

		verify(documentRepositoryMock).findByMunicipalityIdAndRegistrationNumberAndRevisionAndConfidentialityConfidentialIn(MUNICIPALITY_ID, REGISTRATION_NUMBER, REVISION, PUBLIC.getValue());
	}

	@Test
	void readAll() {

		// Arrange
		final var includeConfidential = false;

		final var pageRequest = PageRequest.of(0, 10, Sort.by(DESC, "revision"));

		when(pageMock.getContent()).thenReturn(List.of(createDocumentEntity()));
		when(pageMock.getPageable()).thenReturn(pageRequest);
		when(documentRepositoryMock.findByMunicipalityIdAndRegistrationNumberAndConfidentialityConfidentialIn(MUNICIPALITY_ID, REGISTRATION_NUMBER, PUBLIC.getValue(), pageRequest)).thenReturn(pageMock);

		// Act
		final var result = documentService.readAll(REGISTRATION_NUMBER, includeConfidential, pageRequest, MUNICIPALITY_ID);

		// Assert
		assertThat(result).isNotNull();
		assertThat(result.getDocuments())
			.extracting(Document::getCreated, Document::getCreatedBy, Document::getId, Document::getMunicipalityId, Document::getRegistrationNumber, Document::getRevision)
			.containsExactly(tuple(CREATED, CREATED_BY, ID, MUNICIPALITY_ID, REGISTRATION_NUMBER, REVISION));

		verify(documentRepositoryMock).findByMunicipalityIdAndRegistrationNumberAndConfidentialityConfidentialIn(MUNICIPALITY_ID, REGISTRATION_NUMBER, PUBLIC.getValue(), pageRequest);
	}

	@Test
	void readAllNotFound() {

		// Arrange
		final var includeConfidential = false;
		final var pageRequest = PageRequest.of(0, 10, Sort.by(DESC, "revision"));

		when(pageMock.getContent()).thenReturn(emptyList());
		when(pageMock.getPageable()).thenReturn(pageRequest);
		when(documentRepositoryMock.findByMunicipalityIdAndRegistrationNumberAndConfidentialityConfidentialIn(MUNICIPALITY_ID, REGISTRATION_NUMBER, PUBLIC.getValue(), pageRequest)).thenReturn(pageMock);

		// Act
		final var result = documentService.readAll(REGISTRATION_NUMBER, includeConfidential, pageRequest, MUNICIPALITY_ID);

		// Assert
		assertThat(result).isNotNull();
		assertThat(result.getDocuments()).isEmpty();

		verify(documentRepositoryMock).findByMunicipalityIdAndRegistrationNumberAndConfidentialityConfidentialIn(MUNICIPALITY_ID, REGISTRATION_NUMBER, PUBLIC.getValue(), pageRequest);
	}

	@Test
	void search_whenEsReturnsNoHits_returnsEmptyPage() {

		// Arrange
		final var pageRequest = PageRequest.of(0, 10, Sort.by(DESC, "revision"));
		org.mockito.Mockito.lenient().when(statusPolicyMock.effectivePublishedStatuses(any())).thenReturn(java.util.List.of(DocumentStatus.SCHEDULED, DocumentStatus.ACTIVE, DocumentStatus.EXPIRED));
		when(elasticsearchOperationsMock.search(any(org.springframework.data.elasticsearch.core.query.Query.class), eq(DocumentIndexEntity.class))).thenReturn(searchHitsMock);
		when(searchHitsMock.getSearchHits()).thenReturn(java.util.List.of());

		// Act
		final var result = documentService.search("no-hits", false, false, pageRequest, MUNICIPALITY_ID);

		// Assert
		assertThat(result).isNotNull();
		assertThat(result.getDocuments()).isEmpty();
		verify(elasticsearchOperationsMock).search(any(org.springframework.data.elasticsearch.core.query.Query.class), eq(DocumentIndexEntity.class));
	}

	@Test
	void searchFileMatches_whenEsReturnsNoHits_returnsEmptyPage() {

		// Arrange
		final var pageRequest = PageRequest.of(0, 10);
		org.mockito.Mockito.lenient().when(statusPolicyMock.effectivePublishedStatuses(any())).thenReturn(java.util.List.of(DocumentStatus.SCHEDULED, DocumentStatus.ACTIVE, DocumentStatus.EXPIRED));
		when(elasticsearchOperationsMock.search(any(org.springframework.data.elasticsearch.core.query.Query.class), eq(DocumentIndexEntity.class))).thenReturn(searchHitsMock);
		when(searchHitsMock.getSearchHits()).thenReturn(java.util.List.of());
		when(searchHitsMock.getTotalHits()).thenReturn(0L);

		// Act
		final var result = documentService.searchFileMatches(java.util.List.of("no-hits"), false, false, pageRequest, MUNICIPALITY_ID);

		// Assert
		assertThat(result).isNotNull();
		assertThat(result.getDocuments()).isEmpty();
		assertThat(result.getMetadata().getTotalRecords()).isZero();
		verifyNoInteractions(documentRepositoryMock);
	}

	@Test
	void searchFileMatches_groupsFilesByDocumentIdPreservingOrder() {

		// Arrange
		final var pageRequest = PageRequest.of(0, 10);
		final var docA = "doc-a";
		final var docB = "doc-b";
		final var hits = java.util.List.of(
			fileHit(docA, "reg-a", 1, "file-a1", "alpha.pdf"),
			fileHit(docB, "reg-b", 1, "file-b1", "beta.pdf"),
			fileHit(docA, "reg-a", 1, "file-a2", "alpha-2.pdf"));
		org.mockito.Mockito.lenient().when(statusPolicyMock.effectivePublishedStatuses(any())).thenReturn(java.util.List.of(DocumentStatus.SCHEDULED, DocumentStatus.ACTIVE, DocumentStatus.EXPIRED));
		when(elasticsearchOperationsMock.search(any(org.springframework.data.elasticsearch.core.query.Query.class), eq(DocumentIndexEntity.class))).thenReturn(searchHitsMock);
		when(searchHitsMock.getSearchHits()).thenReturn(hits);
		when(searchHitsMock.getTotalHits()).thenReturn(3L);

		// Act
		final var result = documentService.searchFileMatches(java.util.List.of("any"), false, false, pageRequest, MUNICIPALITY_ID);

		// Assert
		assertThat(result.getDocuments()).hasSize(2);
		assertThat(result.getDocuments().get(0).getId()).isEqualTo(docA);
		assertThat(result.getDocuments().get(0).getFiles())
			.extracting("id", "fileName")
			.containsExactly(tuple("file-a1", "alpha.pdf"), tuple("file-a2", "alpha-2.pdf"));
		assertThat(result.getDocuments().get(1).getId()).isEqualTo(docB);
		assertThat(result.getDocuments().get(1).getFiles())
			.extracting("id", "fileName")
			.containsExactly(tuple("file-b1", "beta.pdf"));
		verifyNoInteractions(documentRepositoryMock);
	}

	@Test
	void searchFileMatches_onlyLatestRevision_dropsOlderRevisionsOfSameRegistrationNumber() {

		// Arrange
		final var pageRequest = PageRequest.of(0, 10);
		final var hits = java.util.List.of(
			fileHit("doc-rev1", "reg-shared", 1, "file-1", "v1.pdf"),
			fileHit("doc-rev2", "reg-shared", 2, "file-2", "v2.pdf"),
			fileHit("doc-other", "reg-other", 1, "file-3", "other.pdf"));
		org.mockito.Mockito.lenient().when(statusPolicyMock.effectivePublishedStatuses(any())).thenReturn(java.util.List.of(DocumentStatus.SCHEDULED, DocumentStatus.ACTIVE, DocumentStatus.EXPIRED));
		when(elasticsearchOperationsMock.search(any(org.springframework.data.elasticsearch.core.query.Query.class), eq(DocumentIndexEntity.class))).thenReturn(searchHitsMock);
		when(searchHitsMock.getSearchHits()).thenReturn(hits);
		when(searchHitsMock.getTotalHits()).thenReturn(3L);

		// Act
		final var result = documentService.searchFileMatches(java.util.List.of("any"), false, true, pageRequest, MUNICIPALITY_ID);

		// Assert — doc-rev1 (revision 1 of reg-shared) should be dropped because reg-shared has a revision 2 on the page.
		assertThat(result.getDocuments()).extracting("id").containsExactly("doc-rev2", "doc-other");
	}

	@Test
	void searchFileMatches_propagatesHighlightsPerFile() {

		// Arrange
		final var pageRequest = PageRequest.of(0, 10);
		final var highlightsForA = java.util.Map.of(
			"extractedText", java.util.List.of("The max <em>bandwidth</em> of this router is 10Gbit/s"),
			"title", java.util.List.of("Router <em>bandwidth</em> spec"));
		final var highlightsForB = java.util.Map.of(
			"extractedText", java.util.List.of("total <em>bandwidth</em> per node"));
		final var hits = java.util.List.of(
			fileHitWithHighlights("doc-a", "reg-a", 1, "file-a1", "alpha.pdf", highlightsForA),
			fileHitWithHighlights("doc-b", "reg-b", 1, "file-b1", "beta.pdf", highlightsForB));
		org.mockito.Mockito.lenient().when(statusPolicyMock.effectivePublishedStatuses(any())).thenReturn(java.util.List.of(DocumentStatus.SCHEDULED, DocumentStatus.ACTIVE, DocumentStatus.EXPIRED));
		when(elasticsearchOperationsMock.search(any(org.springframework.data.elasticsearch.core.query.Query.class), eq(DocumentIndexEntity.class))).thenReturn(searchHitsMock);
		when(searchHitsMock.getSearchHits()).thenReturn(hits);
		when(searchHitsMock.getTotalHits()).thenReturn(2L);

		// Act
		final var result = documentService.searchFileMatches(java.util.List.of("bandwidth"), false, false, pageRequest, MUNICIPALITY_ID);

		// Assert
		assertThat(result.getDocuments()).hasSize(2);
		assertThat(result.getDocuments().get(0).getFiles().get(0).getHighlights()).isEqualTo(highlightsForA);
		assertThat(result.getDocuments().get(1).getFiles().get(0).getHighlights()).isEqualTo(highlightsForB);
	}

	@Test
	void searchFileMatches_omitsHighlightsWhenNoneMatched() {

		// Arrange — existing fileHit() helper doesn't stub getHighlightFields(), so it returns null.
		final var pageRequest = PageRequest.of(0, 10);
		final var hits = java.util.List.of(fileHit("doc-a", "reg-a", 1, "file-a1", "alpha.pdf"));
		org.mockito.Mockito.lenient().when(statusPolicyMock.effectivePublishedStatuses(any())).thenReturn(java.util.List.of(DocumentStatus.SCHEDULED, DocumentStatus.ACTIVE, DocumentStatus.EXPIRED));
		when(elasticsearchOperationsMock.search(any(org.springframework.data.elasticsearch.core.query.Query.class), eq(DocumentIndexEntity.class))).thenReturn(searchHitsMock);
		when(searchHitsMock.getSearchHits()).thenReturn(hits);
		when(searchHitsMock.getTotalHits()).thenReturn(1L);

		// Act
		final var result = documentService.searchFileMatches(java.util.List.of("any"), false, false, pageRequest, MUNICIPALITY_ID);

		// Assert
		assertThat(result.getDocuments().get(0).getFiles().get(0).getHighlights()).isNull();
	}

	@Test
	void searchFileMatches_multipleQueriesProduceOrOfPhraseClausesInEsQuery() {

		// Arrange
		final var pageRequest = PageRequest.of(0, 10);
		final var queryCaptor = ArgumentCaptor.forClass(org.springframework.data.elasticsearch.core.query.Query.class);
		org.mockito.Mockito.lenient().when(statusPolicyMock.effectivePublishedStatuses(any())).thenReturn(java.util.List.of(DocumentStatus.ACTIVE));
		when(elasticsearchOperationsMock.search(queryCaptor.capture(), eq(DocumentIndexEntity.class))).thenReturn(searchHitsMock);
		when(searchHitsMock.getSearchHits()).thenReturn(java.util.List.of());
		when(searchHitsMock.getTotalHits()).thenReturn(0L);

		// Act
		documentService.searchFileMatches(java.util.List.of("alpha", "beta", "gamma"), false, false, pageRequest, MUNICIPALITY_ID);

		// Assert — walk the captured NativeQuery tree: outer bool must → inner bool (the OR) with
		// three should(multiMatch(phrase)) clauses and minimum_should_match=1.
		final var captured = (org.springframework.data.elasticsearch.client.elc.NativeQuery) queryCaptor.getValue();
		final var outerBool = captured.getQuery().bool();
		assertThat(outerBool.must()).hasSize(1);
		final var innerBool = outerBool.must().get(0).bool();
		assertThat(innerBool.minimumShouldMatch()).isEqualTo("1");
		assertThat(innerBool.should()).hasSize(3)
			.extracting(c -> c.multiMatch().query())
			.containsExactly("alpha", "beta", "gamma");
		assertThat(innerBool.should().get(0).multiMatch().type())
			.isEqualTo(co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType.Phrase);
	}

	@Test
	void searchFileMatches_propagatesHitTotalAsMetaTotalRecords() {

		// Arrange
		final var pageRequest = PageRequest.of(2, 5);
		final var hits = java.util.List.of(
			fileHit("doc-a", "reg-a", 1, "file-a1", "alpha.pdf"),
			fileHit("doc-a", "reg-a", 1, "file-a2", "alpha-2.pdf"));
		org.mockito.Mockito.lenient().when(statusPolicyMock.effectivePublishedStatuses(any())).thenReturn(java.util.List.of(DocumentStatus.SCHEDULED, DocumentStatus.ACTIVE, DocumentStatus.EXPIRED));
		when(elasticsearchOperationsMock.search(any(org.springframework.data.elasticsearch.core.query.Query.class), eq(DocumentIndexEntity.class))).thenReturn(searchHitsMock);
		when(searchHitsMock.getSearchHits()).thenReturn(hits);
		when(searchHitsMock.getTotalHits()).thenReturn(42L);

		// Act
		final var result = documentService.searchFileMatches(java.util.List.of("any"), false, false, pageRequest, MUNICIPALITY_ID);

		// Assert — file-level total (same as existing search endpoint), page + size reflect pageable.
		assertThat(result.getMetadata().getTotalRecords()).isEqualTo(42L);
		assertThat(result.getMetadata().getPage()).isEqualTo(2);
		assertThat(result.getMetadata().getLimit()).isEqualTo(5);
		assertThat(result.getMetadata().getCount()).isEqualTo(1); // one grouped DocumentMatch
		assertThat(result.getMetadata().getTotalPages()).isEqualTo(9); // ceil(42 / 5)
	}

	@SuppressWarnings("unchecked")
	private static SearchHit<DocumentIndexEntity> fileHit(String documentId, String registrationNumber, int revision, String fileId, String fileName) {
		final var entity = new DocumentIndexEntity();
		entity.setId(fileId);
		entity.setDocumentId(documentId);
		entity.setRegistrationNumber(registrationNumber);
		entity.setRevision(revision);
		entity.setFileName(fileName);
		final var hit = (SearchHit<DocumentIndexEntity>) org.mockito.Mockito.mock(SearchHit.class);
		org.mockito.Mockito.when(hit.getContent()).thenReturn(entity);
		return hit;
	}

	private static SearchHit<DocumentIndexEntity> fileHitWithHighlights(String documentId, String registrationNumber, int revision, String fileId, String fileName, java.util.Map<String, java.util.List<String>> highlights) {
		final var hit = fileHit(documentId, registrationNumber, revision, fileId, fileName);
		org.mockito.Mockito.when(hit.getHighlightFields()).thenReturn(highlights);
		return hit;
	}

	@Test
	void update() {

		// Arrange
		final var includeConfidential = false;
		final var existingEntity = createDocumentEntity();
		final var documentUpdateRequest = DocumentUpdateRequest.create()
			.withDescription("changedDescription")
			.withType("changedDocumentType")
			.withMetadataList(List.of(DocumentMetadata.create().withKey("changedKey").withValue("changedValue")));

		when(documentTypeRepositoryMock.findByMunicipalityIdAndType(MUNICIPALITY_ID, "changedDocumentType")).thenReturn(Optional.of(DocumentTypeEntity.create().withType("changedDocumentType")));
		when(documentRepositoryMock.findTopByMunicipalityIdAndRegistrationNumberAndConfidentialityConfidentialInOrderByRevisionDesc(MUNICIPALITY_ID, REGISTRATION_NUMBER, PUBLIC.getValue())).thenReturn(Optional.of(existingEntity));
		when(documentRepositoryMock.save(any(DocumentEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

		// Act
		final var result = documentService.update(REGISTRATION_NUMBER, includeConfidential, documentUpdateRequest, MUNICIPALITY_ID);

		// Assert
		assertThat(result).isNotNull();

		verify(documentTypeRepositoryMock).findByMunicipalityIdAndType(MUNICIPALITY_ID, "changedDocumentType");
		verify(documentRepositoryMock).save(documentEntityCaptor.capture());
		verifyNoInteractions(registrationNumberServiceMock, binaryStoreMock);

		final var capturedDocumentEntity = documentEntityCaptor.getValue();
		assertThat(capturedDocumentEntity).isNotNull();
		assertThat(capturedDocumentEntity.getRevision()).isEqualTo(REVISION); // Revision unchanged — no file modification
		assertThat(capturedDocumentEntity.getConfidentiality()).isEqualTo(ConfidentialityEmbeddable.create().withConfidential(CONFIDENTIAL).withLegalCitation(LEGAL_CITATION));
		assertThat(capturedDocumentEntity.getCreatedBy()).isEqualTo(CREATED_BY); // createdBy unchanged
		assertThat(capturedDocumentEntity.getDescription()).isEqualTo("changedDescription");
		assertThat(capturedDocumentEntity.getDocumentData())
			.hasSize(1)
			.allSatisfy(data -> {
				assertThat(data.getFileName()).isEqualTo(FILE_NAME);
				assertThat(data.getStorageLocator()).isEqualTo(STORAGE_LOCATOR); // same locator — no file copy
			});
		assertThat(capturedDocumentEntity.getMetadata()).isEqualTo(List.of(DocumentMetadataEmbeddable.create().withKey("changedKey").withValue("changedValue")));
		assertThat(capturedDocumentEntity.getMunicipalityId()).isEqualTo(existingEntity.getMunicipalityId());
		assertThat(capturedDocumentEntity.getRegistrationNumber()).isEqualTo(existingEntity.getRegistrationNumber());
		assertThat(capturedDocumentEntity.getType()).isNotNull().satisfies(type -> {
			assertThat(type.getType()).isEqualTo("changedDocumentType");
		});
	}

	@Test
	void updateWhenNotFound() {

		// Arrange
		final var includeConfidential = false;
		final var documentUpdateRequest = DocumentUpdateRequest.create()
			.withUpdatedBy("changedUser")
			.withMetadataList(List.of(DocumentMetadata.create().withKey("changedKey").withValue("changedValue")));

		when(documentRepositoryMock.findTopByMunicipalityIdAndRegistrationNumberAndConfidentialityConfidentialInOrderByRevisionDesc(MUNICIPALITY_ID, REGISTRATION_NUMBER, PUBLIC.getValue())).thenReturn(empty());

		// Act
		final var exception = assertThrows(ThrowableProblem.class, () -> documentService.update(REGISTRATION_NUMBER, includeConfidential, documentUpdateRequest, MUNICIPALITY_ID));

		// Assert
		assertThat(exception).isNotNull();
		assertThat(exception.getMessage()).isEqualTo("Not Found: No document with registrationNumber: '2023-2281-4' could be found!");

		verify(documentRepositoryMock).findTopByMunicipalityIdAndRegistrationNumberAndConfidentialityConfidentialInOrderByRevisionDesc(MUNICIPALITY_ID, REGISTRATION_NUMBER, PUBLIC.getValue());
		verify(documentRepositoryMock, never()).save(any());
		verifyNoInteractions(registrationNumberServiceMock, binaryStoreMock);
	}

	@Test
	void updateConfidentiality() {

		// Arrange
		final var eventLogKey = UUID.randomUUID().toString();
		final var newConfidentialValue = true;
		final var existingEntities = List.of(createDocumentEntity(), createDocumentEntity().withRevision(REVISION + 1));
		final var confidentialityUpdateRequest = ConfidentialityUpdateRequest.create()
			.withUpdatedBy(CREATED_BY)
			.withConfidential(newConfidentialValue)
			.withLegalCitation(LEGAL_CITATION);

		when(eventlogPropertiesMock.logKeyUuid()).thenReturn(eventLogKey);
		when(documentRepositoryMock.findByMunicipalityIdAndRegistrationNumberAndConfidentialityConfidentialIn(MUNICIPALITY_ID, REGISTRATION_NUMBER, CONFIDENTIAL_AND_PUBLIC.getValue())).thenReturn(existingEntities);
		when(documentRepositoryMock.saveAll(any())).thenReturn(existingEntities);

		// Act
		documentService.updateConfidentiality(REGISTRATION_NUMBER, confidentialityUpdateRequest, MUNICIPALITY_ID);

		// Assert
		verify(documentRepositoryMock).findByMunicipalityIdAndRegistrationNumberAndConfidentialityConfidentialIn(MUNICIPALITY_ID, REGISTRATION_NUMBER, CONFIDENTIAL_AND_PUBLIC.getValue());
		verify(documentRepositoryMock).saveAll(documentEntitiesCaptor.capture());
		assertThat(eventLogClient.requests).hasSize(1);
		verifyNoInteractions(registrationNumberServiceMock, documentTypeRepositoryMock, binaryStoreMock);

		// Assert captured DocumentEntity-objects.
		final var capturedDocumentEntities = documentEntitiesCaptor.getValue();
		assertThat(capturedDocumentEntities)
			.isNotNull()
			.hasSize(2)
			.extracting(DocumentEntity::getConfidentiality, DocumentEntity::getRegistrationNumber, DocumentEntity::getRevision)
			.containsExactlyInAnyOrder(
				tuple(ConfidentialityEmbeddable.create().withConfidential(newConfidentialValue).withLegalCitation(LEGAL_CITATION), REGISTRATION_NUMBER, REVISION),
				tuple(ConfidentialityEmbeddable.create().withConfidential(newConfidentialValue).withLegalCitation(LEGAL_CITATION), REGISTRATION_NUMBER, REVISION + 1));

		// Assert captured Eventlog-event.
		final var capturedEvent = eventLogClient.requests.getFirst().event();
		assertThat(eventLogClient.requests.getFirst().municipalityId()).isEqualTo(MUNICIPALITY_ID);
		assertThat(eventLogClient.requests.getFirst().logKey()).isEqualTo(eventLogKey);
		assertThat(capturedEvent).isNotNull();
		assertThat(capturedEvent.getExpires()).isCloseTo(now(systemDefault()).plusYears(10), within(2, ChronoUnit.SECONDS));
		assertThat(capturedEvent.getType()).isEqualTo(UPDATE);
		assertThat(capturedEvent.getMessage()).isEqualTo("Confidentiality flag updated to: 'true' with legal citation: 'legalCitation' for document with registrationNumber: '2023-2281-4'. Action performed by: 'b0000000-0000-0000-0000-000000000099'");
		assertThat(capturedEvent.getOwner()).isEqualTo("Document");
		assertThat(capturedEvent.getMetadata())
			.extracting(Metadata::getKey, Metadata::getValue)
			.containsExactlyInAnyOrder(
				tuple("RegistrationNumber", REGISTRATION_NUMBER),
				tuple("ExecutedBy", CREATED_BY));
	}

	@Test
	void updateResponsibilities() {

		// Arrange
		final var eventLogKey = UUID.randomUUID().toString();
		final var otherPersonId = "11111111-2222-3333-4444-555555555555";
		final var oldPersonId = "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee";
		final var request = DocumentResponsibilitiesUpdateRequest.create()
			.withUpdatedBy(CREATED_BY)
			.withResponsibilities(List.of(
				DocumentResponsibility.create().withPersonId(PERSON_ID),
				DocumentResponsibility.create().withPersonId(otherPersonId)));
		final var oldResponsibilities = List.of(DocumentResponsibilityEntity.create()
			.withMunicipalityId(MUNICIPALITY_ID)
			.withRegistrationNumber(REGISTRATION_NUMBER)
			.withPersonId(oldPersonId));

		when(eventlogPropertiesMock.logKeyUuid()).thenReturn(eventLogKey);
		when(documentRepositoryMock.existsByMunicipalityIdAndRegistrationNumber(MUNICIPALITY_ID, REGISTRATION_NUMBER)).thenReturn(true);
		when(documentResponsibilityRepositoryMock.findByMunicipalityIdAndRegistrationNumberOrderByPersonIdAsc(MUNICIPALITY_ID, REGISTRATION_NUMBER)).thenReturn(oldResponsibilities);

		// Act
		documentService.updateResponsibilities(REGISTRATION_NUMBER, request, MUNICIPALITY_ID);

		// Assert
		verify(documentRepositoryMock).existsByMunicipalityIdAndRegistrationNumber(MUNICIPALITY_ID, REGISTRATION_NUMBER);
		verify(documentResponsibilityRepositoryMock).findByMunicipalityIdAndRegistrationNumberOrderByPersonIdAsc(MUNICIPALITY_ID, REGISTRATION_NUMBER);
		verify(documentResponsibilityRepositoryMock).deleteByMunicipalityIdAndRegistrationNumber(MUNICIPALITY_ID, REGISTRATION_NUMBER);
		verify(documentResponsibilityRepositoryMock).flush();
		verify(documentResponsibilityRepositoryMock).saveAll(responsibilityEntitiesCaptor.capture());
		assertThat(eventLogClient.requests).hasSize(1);
		verifyNoInteractions(registrationNumberServiceMock, documentTypeRepositoryMock, binaryStoreMock);

		assertThat(responsibilityEntitiesCaptor.getValue())
			.extracting(DocumentResponsibilityEntity::getMunicipalityId, DocumentResponsibilityEntity::getRegistrationNumber, DocumentResponsibilityEntity::getPersonId,
				DocumentResponsibilityEntity::getCreatedBy)
			.containsExactly(
				tuple(MUNICIPALITY_ID, REGISTRATION_NUMBER, PERSON_ID, CREATED_BY),
				tuple(MUNICIPALITY_ID, REGISTRATION_NUMBER, otherPersonId, CREATED_BY));

		final var capturedEvent = eventLogClient.requests.getFirst().event();
		assertThat(eventLogClient.requests.getFirst().municipalityId()).isEqualTo(MUNICIPALITY_ID);
		assertThat(eventLogClient.requests.getFirst().logKey()).isEqualTo(eventLogKey);
		assertThat(capturedEvent.getType()).isEqualTo(UPDATE);
		assertThat(capturedEvent.getMessage()).contains("Responsibilities updated from:", oldPersonId, CREATED_BY, otherPersonId, REGISTRATION_NUMBER);
		assertThat(capturedEvent.getMetadata())
			.extracting(Metadata::getKey, Metadata::getValue)
			.containsExactlyInAnyOrder(
				tuple("RegistrationNumber", REGISTRATION_NUMBER),
				tuple("ExecutedBy", CREATED_BY));
	}

	@Test
	void updateResponsibilitiesWhenDocumentIsNotFound() {

		// Arrange
		final var request = DocumentResponsibilitiesUpdateRequest.create()
			.withUpdatedBy(CREATED_BY)
			.withResponsibilities(List.of(DocumentResponsibility.create().withPersonId(PERSON_ID)));

		when(documentRepositoryMock.existsByMunicipalityIdAndRegistrationNumber(MUNICIPALITY_ID, REGISTRATION_NUMBER)).thenReturn(false);

		// Act
		final var exception = assertThrows(ThrowableProblem.class, () -> documentService.updateResponsibilities(REGISTRATION_NUMBER, request, MUNICIPALITY_ID));

		// Assert
		assertThat(exception).isNotNull();
		assertThat(exception.getMessage()).isEqualTo("Not Found: No document with registrationNumber: '2023-2281-4' could be found!");

		verify(documentRepositoryMock).existsByMunicipalityIdAndRegistrationNumber(MUNICIPALITY_ID, REGISTRATION_NUMBER);
		assertThat(eventLogClient.requests).isEmpty();
		verifyNoInteractions(documentResponsibilityRepositoryMock, registrationNumberServiceMock, documentTypeRepositoryMock, binaryStoreMock);
	}

	private DocumentEntity createDocumentEntity() {

		try {
			return DocumentEntity.create()
				.withCreated(CREATED)
				.withCreatedBy(CREATED_BY)
				.withConfidentiality(ConfidentialityEmbeddable.create()
					.withConfidential(CONFIDENTIAL)
					.withLegalCitation(LEGAL_CITATION))
				.withDescription(DESCRIPTION)
				.withDocumentData(List.of(createDocumentDataEntity()))
				.withId(ID)
				.withMetadata(new ArrayList<>(List.of(DocumentMetadataEmbeddable.create().withKey(METADATA_KEY).withValue(METADATA_VALUE))))
				.withMunicipalityId(MUNICIPALITY_ID)
				.withRegistrationNumber(REGISTRATION_NUMBER)
				.withRevision(REVISION)
				.withType(createDocumentTypeEntity());
		} catch (final Exception e) {
			fail("Entity could not be created!");
		}
		return null;
	}

	private DocumentTypeEntity createDocumentTypeEntity() {
		return DocumentTypeEntity.create()
			.withMunicipalityId(MUNICIPALITY_ID)
			.withType(DOCUMENT_TYPE)
			.withDisplayName(DOCUMENT_TYPE_DISPLAYNAME);
	}

	private DocumentDataEntity createDocumentDataEntity() {
		return DocumentDataEntity.create()
			.withId(DOCUMENT_DATA_ID)
			.withStorageLocator(STORAGE_LOCATOR)
			.withFileName(FILE_NAME)
			.withMimeType(MIME_TYPE)
			.withFileSizeInBytes(FILE_SIZE_IN_BYTES);
	}

	@Test
	void publishFromDraft_setsStatusAndSaves() {
		final var entity = createDocumentEntity().withStatus(DocumentStatus.DRAFT);
		when(documentRepositoryMock.findTopByMunicipalityIdAndRegistrationNumberAndConfidentialityConfidentialInOrderByRevisionDesc(MUNICIPALITY_ID, REGISTRATION_NUMBER, CONFIDENTIAL_AND_PUBLIC.getValue())).thenReturn(Optional.of(entity));
		when(statusPolicyMock.resolvePublishedStatus(any(), any(), eq(REGISTRATION_NUMBER))).thenReturn(DocumentStatus.ACTIVE);
		when(documentRepositoryMock.save(any(DocumentEntity.class))).thenAnswer(i -> i.getArgument(0));

		final var result = documentService.publish(REGISTRATION_NUMBER, "actor", MUNICIPALITY_ID, null);

		assertThat(result.getStatus()).isEqualTo(DocumentStatus.ACTIVE);
		verify(documentRepositoryMock).save(documentEntityCaptor.capture());
		assertThat(documentEntityCaptor.getValue().getStatus()).isEqualTo(DocumentStatus.ACTIVE);
	}

	@Test
	void publishFromActive_throwsConflict() {
		final var entity = createDocumentEntity().withStatus(DocumentStatus.ACTIVE);
		when(documentRepositoryMock.findTopByMunicipalityIdAndRegistrationNumberAndConfidentialityConfidentialInOrderByRevisionDesc(MUNICIPALITY_ID, REGISTRATION_NUMBER, CONFIDENTIAL_AND_PUBLIC.getValue())).thenReturn(Optional.of(entity));

		final var ex = assertThrows(ThrowableProblem.class, () -> documentService.publish(REGISTRATION_NUMBER, "actor", MUNICIPALITY_ID, null));

		assertThat(ex.getMessage()).contains("not allowed");
		verify(documentRepositoryMock, never()).save(any(DocumentEntity.class));
	}

	@Test
	void revokeFromActive_setsRevoked() {
		final var entity = createDocumentEntity().withStatus(DocumentStatus.ACTIVE);
		when(documentRepositoryMock.findTopByMunicipalityIdAndRegistrationNumberAndConfidentialityConfidentialInOrderByRevisionDesc(MUNICIPALITY_ID, REGISTRATION_NUMBER, CONFIDENTIAL_AND_PUBLIC.getValue())).thenReturn(Optional.of(entity));
		when(documentRepositoryMock.save(any(DocumentEntity.class))).thenAnswer(i -> i.getArgument(0));

		final var result = documentService.revoke(REGISTRATION_NUMBER, "actor", MUNICIPALITY_ID, null);

		assertThat(result.getStatus()).isEqualTo(DocumentStatus.REVOKED);
	}

	@Test
	void revokeFromDraft_throwsConflict() {
		final var entity = createDocumentEntity().withStatus(DocumentStatus.DRAFT);
		when(documentRepositoryMock.findTopByMunicipalityIdAndRegistrationNumberAndConfidentialityConfidentialInOrderByRevisionDesc(MUNICIPALITY_ID, REGISTRATION_NUMBER, CONFIDENTIAL_AND_PUBLIC.getValue())).thenReturn(Optional.of(entity));

		assertThrows(ThrowableProblem.class, () -> documentService.revoke(REGISTRATION_NUMBER, "actor", MUNICIPALITY_ID, null));

		verify(documentRepositoryMock, never()).save(any(DocumentEntity.class));
	}

	@Test
	void unrevokeFromRevoked_restoresPublishedStatus() {
		final var entity = createDocumentEntity().withStatus(DocumentStatus.REVOKED);
		when(documentRepositoryMock.findTopByMunicipalityIdAndRegistrationNumberAndConfidentialityConfidentialInOrderByRevisionDesc(MUNICIPALITY_ID, REGISTRATION_NUMBER, CONFIDENTIAL_AND_PUBLIC.getValue())).thenReturn(Optional.of(entity));
		when(statusPolicyMock.resolvePublishedStatus(any(), any(), eq(REGISTRATION_NUMBER))).thenReturn(DocumentStatus.SCHEDULED);
		when(documentRepositoryMock.save(any(DocumentEntity.class))).thenAnswer(i -> i.getArgument(0));

		final var result = documentService.unrevoke(REGISTRATION_NUMBER, "actor", MUNICIPALITY_ID, null);

		assertThat(result.getStatus()).isEqualTo(DocumentStatus.SCHEDULED);
	}

	@Test
	void unrevokeFromActive_throwsConflict() {
		final var entity = createDocumentEntity().withStatus(DocumentStatus.ACTIVE);
		when(documentRepositoryMock.findTopByMunicipalityIdAndRegistrationNumberAndConfidentialityConfidentialInOrderByRevisionDesc(MUNICIPALITY_ID, REGISTRATION_NUMBER, CONFIDENTIAL_AND_PUBLIC.getValue())).thenReturn(Optional.of(entity));

		assertThrows(ThrowableProblem.class, () -> documentService.unrevoke(REGISTRATION_NUMBER, "actor", MUNICIPALITY_ID, null));
	}

	@Test
	void publishWhenDocumentMissing_throwsNotFound() {
		when(documentRepositoryMock.findTopByMunicipalityIdAndRegistrationNumberAndConfidentialityConfidentialInOrderByRevisionDesc(MUNICIPALITY_ID, REGISTRATION_NUMBER, CONFIDENTIAL_AND_PUBLIC.getValue())).thenReturn(empty());

		final var ex = assertThrows(ThrowableProblem.class, () -> documentService.publish(REGISTRATION_NUMBER, "actor", MUNICIPALITY_ID, null));
		assertThat(ex.getMessage()).contains("could be found");
	}

	@Test
	void publishWithSpecificRevision_usesRevisionFinderAndSaves() {
		final var targetRevision = 5;
		final var entity = createDocumentEntity().withRevision(targetRevision).withStatus(DocumentStatus.DRAFT);
		when(documentRepositoryMock.findByMunicipalityIdAndRegistrationNumberAndRevisionAndConfidentialityConfidentialIn(MUNICIPALITY_ID, REGISTRATION_NUMBER, targetRevision, CONFIDENTIAL_AND_PUBLIC.getValue())).thenReturn(Optional.of(entity));
		when(statusPolicyMock.resolvePublishedStatus(any(), any(), eq(REGISTRATION_NUMBER))).thenReturn(DocumentStatus.ACTIVE);
		when(documentRepositoryMock.save(any(DocumentEntity.class))).thenAnswer(i -> i.getArgument(0));

		final var result = documentService.publish(REGISTRATION_NUMBER, "actor", MUNICIPALITY_ID, targetRevision);

		assertThat(result.getStatus()).isEqualTo(DocumentStatus.ACTIVE);
		verify(documentRepositoryMock, never()).findTopByMunicipalityIdAndRegistrationNumberAndConfidentialityConfidentialInOrderByRevisionDesc(any(), any(), any());
		verify(documentRepositoryMock).save(documentEntityCaptor.capture());
		assertThat(documentEntityCaptor.getValue().getRevision()).isEqualTo(targetRevision);
		assertThat(documentEntityCaptor.getValue().getStatus()).isEqualTo(DocumentStatus.ACTIVE);
	}

	@Test
	void revokeWithSpecificRevision_usesRevisionFinderAndSaves() {
		final var targetRevision = 3;
		final var entity = createDocumentEntity().withRevision(targetRevision).withStatus(DocumentStatus.ACTIVE);
		when(documentRepositoryMock.findByMunicipalityIdAndRegistrationNumberAndRevisionAndConfidentialityConfidentialIn(MUNICIPALITY_ID, REGISTRATION_NUMBER, targetRevision, CONFIDENTIAL_AND_PUBLIC.getValue())).thenReturn(Optional.of(entity));
		when(documentRepositoryMock.save(any(DocumentEntity.class))).thenAnswer(i -> i.getArgument(0));

		final var result = documentService.revoke(REGISTRATION_NUMBER, "actor", MUNICIPALITY_ID, targetRevision);

		assertThat(result.getStatus()).isEqualTo(DocumentStatus.REVOKED);
		verify(documentRepositoryMock, never()).findTopByMunicipalityIdAndRegistrationNumberAndConfidentialityConfidentialInOrderByRevisionDesc(any(), any(), any());
		verify(documentRepositoryMock).save(documentEntityCaptor.capture());
		assertThat(documentEntityCaptor.getValue().getRevision()).isEqualTo(targetRevision);
	}

	@Test
	void unrevokeWithSpecificRevision_usesRevisionFinderAndSaves() {
		final var targetRevision = 2;
		final var entity = createDocumentEntity().withRevision(targetRevision).withStatus(DocumentStatus.REVOKED);
		when(documentRepositoryMock.findByMunicipalityIdAndRegistrationNumberAndRevisionAndConfidentialityConfidentialIn(MUNICIPALITY_ID, REGISTRATION_NUMBER, targetRevision, CONFIDENTIAL_AND_PUBLIC.getValue())).thenReturn(Optional.of(entity));
		when(statusPolicyMock.resolvePublishedStatus(any(), any(), eq(REGISTRATION_NUMBER))).thenReturn(DocumentStatus.SCHEDULED);
		when(documentRepositoryMock.save(any(DocumentEntity.class))).thenAnswer(i -> i.getArgument(0));

		final var result = documentService.unrevoke(REGISTRATION_NUMBER, "actor", MUNICIPALITY_ID, targetRevision);

		assertThat(result.getStatus()).isEqualTo(DocumentStatus.SCHEDULED);
		verify(documentRepositoryMock, never()).findTopByMunicipalityIdAndRegistrationNumberAndConfidentialityConfidentialInOrderByRevisionDesc(any(), any(), any());
	}

	@Test
	void publishWithUnknownRevision_throwsNotFound() {
		final var targetRevision = 99;
		when(documentRepositoryMock.findByMunicipalityIdAndRegistrationNumberAndRevisionAndConfidentialityConfidentialIn(MUNICIPALITY_ID, REGISTRATION_NUMBER, targetRevision, CONFIDENTIAL_AND_PUBLIC.getValue())).thenReturn(empty());

		final var ex = assertThrows(ThrowableProblem.class, () -> documentService.publish(REGISTRATION_NUMBER, "actor", MUNICIPALITY_ID, targetRevision));

		assertThat(ex.getMessage()).contains("revision: '%d'".formatted(targetRevision));
		verify(documentRepositoryMock, never()).save(any(DocumentEntity.class));
	}

	@Test
	void readPreReadReconcile_staleActiveBecomesExpired() {
		final var entity = createDocumentEntity().withStatus(DocumentStatus.ACTIVE).withValidTo(LocalDate.of(2020, 1, 1));
		when(documentRepositoryMock.findTopByMunicipalityIdAndRegistrationNumberAndConfidentialityConfidentialInAndStatusNotInOrderByRevisionDesc(
			eq(MUNICIPALITY_ID), eq(REGISTRATION_NUMBER), any(), any())).thenReturn(Optional.of(entity));
		when(statusPolicyMock.reconcile(DocumentStatus.ACTIVE, null, LocalDate.of(2020, 1, 1))).thenReturn(Optional.of(DocumentStatus.EXPIRED));
		when(documentRepositoryMock.save(any(DocumentEntity.class))).thenAnswer(i -> i.getArgument(0));

		final var result = documentService.read(REGISTRATION_NUMBER, false, false, MUNICIPALITY_ID);

		assertThat(result.getStatus()).isEqualTo(DocumentStatus.EXPIRED);
		verify(documentRepositoryMock).save(any(DocumentEntity.class));
	}

	@Test
	void readPreReadReconcile_noChangeDoesNotSave() {
		final var entity = createDocumentEntity().withStatus(DocumentStatus.ACTIVE);
		when(documentRepositoryMock.findTopByMunicipalityIdAndRegistrationNumberAndConfidentialityConfidentialInAndStatusNotInOrderByRevisionDesc(
			eq(MUNICIPALITY_ID), eq(REGISTRATION_NUMBER), any(), any())).thenReturn(Optional.of(entity));
		when(statusPolicyMock.reconcile(any(), any(), any())).thenReturn(Optional.empty());

		documentService.read(REGISTRATION_NUMBER, false, false, MUNICIPALITY_ID);

		verify(documentRepositoryMock, never()).save(any(DocumentEntity.class));
	}

	@Test
	void readNoPublishedRevision_throwsNotFound() {
		when(documentRepositoryMock.findTopByMunicipalityIdAndRegistrationNumberAndConfidentialityConfidentialInAndStatusNotInOrderByRevisionDesc(
			eq(MUNICIPALITY_ID), eq(REGISTRATION_NUMBER), any(), any())).thenReturn(empty());

		final var ex = assertThrows(ThrowableProblem.class, () -> documentService.read(REGISTRATION_NUMBER, false, false, MUNICIPALITY_ID));

		assertThat(ex.getMessage()).contains("could be found");
	}

	@Test
	void createWithInvalidValidityWindow_throwsConflict() {
		final var request = DocumentCreateRequest.create()
			.withCreatedBy(CREATED_BY)
			.withType(DOCUMENT_TYPE)
			.withMetadataList(List.of(DocumentMetadata.create().withKey("k").withValue("v")))
			.withValidFrom(LocalDate.of(2027, 1, 1))
			.withValidTo(LocalDate.of(2026, 1, 1));

		final var ex = assertThrows(ThrowableProblem.class, () -> documentService.create(request, DocumentFiles.create(), MUNICIPALITY_ID));
		assertThat(ex.getMessage()).contains("must not be after");
	}

	private static final class TestEventLogClient implements EventLogClient {

		private final List<EventLogRequest> requests = new java.util.ArrayList<>();

		@Override
		public ResponseEntity<Void> createEvent(final String municipalityId, final String logKey, final Event event) {
			requests.add(new EventLogRequest(municipalityId, logKey, event));
			return ResponseEntity.noContent().build();
		}
	}

	private record EventLogRequest(String municipalityId, String logKey, Event event) {
	}
}
