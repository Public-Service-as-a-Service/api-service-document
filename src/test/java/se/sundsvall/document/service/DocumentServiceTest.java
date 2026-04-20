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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
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
import se.sundsvall.document.integration.db.DocumentRepository;
import se.sundsvall.document.integration.db.DocumentResponsibilityRepository;
import se.sundsvall.document.integration.db.DocumentTypeRepository;
import se.sundsvall.document.integration.db.model.ConfidentialityEmbeddable;
import se.sundsvall.document.integration.db.model.DocumentDataEntity;
import se.sundsvall.document.integration.db.model.DocumentEntity;
import se.sundsvall.document.integration.db.model.DocumentMetadataEmbeddable;
import se.sundsvall.document.integration.db.model.DocumentResponsibilityEntity;
import se.sundsvall.document.integration.db.model.DocumentTypeEntity;
import se.sundsvall.document.integration.eventlog.EventLogClient;
import se.sundsvall.document.integration.eventlog.configuration.EventlogProperties;
import se.sundsvall.document.service.storage.BinaryStore;
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
import static org.mockito.ArgumentMatchers.anyBoolean;
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
	private static final String CREATED_BY = "User";
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
			registrationNumberServiceMock,
			statusPolicyMock,
			new DocumentEventPublisher(Optional.of(eventLogClient), Optional.of(eventlogPropertiesMock)));
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
		when(binaryStoreMock.put(any(InputStream.class), anyLong(), anyString(), anyMap())).thenReturn(StorageRef.s3(newLocator));
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
		when(binaryStoreMock.put(any(InputStream.class), anyLong(), anyString(), anyMap())).thenAnswer(invocation -> StorageRef.s3(randomUUID().toString()));
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

	@ParameterizedTest
	@ValueSource(booleans = {
		true, false
	})
	void searchConfidential(boolean includeConfidential) {

		// Arrange
		final var search = "search-string";
		final var pageRequest = PageRequest.of(0, 10, Sort.by(DESC, "revision"));

		when(pageMock.getContent()).thenReturn(List.of(createDocumentEntity()));
		when(pageMock.getPageable()).thenReturn(pageRequest);
		when(documentRepositoryMock.search(any(), anyBoolean(), anyBoolean(), any(), any(), any())).thenReturn(pageMock);
		when(statusPolicyMock.effectivePublishedStatuses(any())).thenReturn(java.util.List.of(DocumentStatus.SCHEDULED, DocumentStatus.ACTIVE, DocumentStatus.EXPIRED));

		// Act
		final var result = documentService.search(search, includeConfidential, false, pageRequest, MUNICIPALITY_ID);

		// Assert
		assertThat(result).isNotNull();
		assertThat(result.getDocuments())
			.extracting(Document::getCreated, Document::getCreatedBy, Document::getId, Document::getMunicipalityId, Document::getRegistrationNumber, Document::getRevision)
			.containsExactly(tuple(CREATED, CREATED_BY, ID, MUNICIPALITY_ID, REGISTRATION_NUMBER, REVISION));
		assertThat(result.getDocuments().getFirst().getDocumentData()).hasSize(1);

		verify(documentRepositoryMock).search(eq(search), eq(includeConfidential), eq(false), eq(pageRequest), eq(MUNICIPALITY_ID), any());
	}

	@ParameterizedTest
	@ValueSource(booleans = {
		true, false
	})
	void searchLatestRevision(boolean onlyLatestRevision) {

		// Arrange
		final var search = "search-string";
		final var pageRequest = PageRequest.of(0, 10, Sort.by(DESC, "revision"));

		when(pageMock.getContent()).thenReturn(List.of(createDocumentEntity()));
		when(pageMock.getPageable()).thenReturn(pageRequest);
		when(documentRepositoryMock.search(any(), anyBoolean(), anyBoolean(), any(), any(), any())).thenReturn(pageMock);
		when(statusPolicyMock.effectivePublishedStatuses(any())).thenReturn(java.util.List.of(DocumentStatus.SCHEDULED, DocumentStatus.ACTIVE, DocumentStatus.EXPIRED));

		// Act
		final var result = documentService.search(search, false, onlyLatestRevision, pageRequest, MUNICIPALITY_ID);

		// Assert
		assertThat(result).isNotNull();
		assertThat(result.getDocuments())
			.extracting(Document::getCreated, Document::getCreatedBy, Document::getId, Document::getMunicipalityId, Document::getRegistrationNumber, Document::getRevision)
			.containsExactly(tuple(CREATED, CREATED_BY, ID, MUNICIPALITY_ID, REGISTRATION_NUMBER, REVISION));
		assertThat(result.getDocuments().getFirst().getDocumentData()).hasSize(1);

		verify(documentRepositoryMock).search(eq(search), eq(false), eq(onlyLatestRevision), eq(pageRequest), eq(MUNICIPALITY_ID), any());
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
			.withChangedBy(CREATED_BY)
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
		assertThat(capturedEvent.getMessage()).isEqualTo("Confidentiality flag updated to: 'true' with legal citation: 'legalCitation' for document with registrationNumber: '2023-2281-4'. Action performed by: 'User'");
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
			.withChangedBy(CREATED_BY)
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
			.withChangedBy(CREATED_BY)
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
