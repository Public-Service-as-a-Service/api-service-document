package se.sundsvall.document.service;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ContentDisposition;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import se.sundsvall.dept44.problem.ThrowableProblem;
import se.sundsvall.document.api.model.DocumentDataCreateRequest;
import se.sundsvall.document.api.model.DocumentFiles;
import se.sundsvall.document.integration.db.DocumentRepository;
import se.sundsvall.document.integration.db.DocumentResponsibilityRepository;
import se.sundsvall.document.integration.db.model.ConfidentialityEmbeddable;
import se.sundsvall.document.integration.db.model.DocumentDataEntity;
import se.sundsvall.document.integration.db.model.DocumentEntity;
import se.sundsvall.document.integration.db.model.DocumentMetadataEmbeddable;
import se.sundsvall.document.integration.db.model.DocumentTypeEntity;
import se.sundsvall.document.service.storage.BinaryStore;
import se.sundsvall.document.service.storage.StorageRef;

import static java.time.OffsetDateTime.now;
import static java.time.ZoneId.systemDefault;
import static java.util.Collections.emptyList;
import static java.util.Optional.empty;
import static java.util.UUID.randomUUID;
import static org.apache.commons.io.IOUtils.toByteArray;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpHeaders.CONTENT_DISPOSITION;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static se.sundsvall.document.service.InclusionFilter.CONFIDENTIAL_AND_PUBLIC;
import static se.sundsvall.document.service.InclusionFilter.PUBLIC;

@ExtendWith(MockitoExtension.class)
class DocumentFileServiceTest {

	private static final String FILE_NAME = "image.png";
	private static final String MIME_TYPE = "image/png";
	private static final long FILE_SIZE_IN_BYTES = 227546L;
	private static final OffsetDateTime CREATED = now(systemDefault());
	private static final boolean CONFIDENTIAL = true;
	private static final String CREATED_BY = "User";
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

	@Mock
	private DocumentRepository documentRepositoryMock;

	@Mock
	private DocumentResponsibilityRepository documentResponsibilityRepositoryMock;

	@Mock
	private DocumentStatusPolicy statusPolicyMock;

	@Mock
	private BinaryStore binaryStoreMock;

	@Mock
	private HttpServletResponse httpServletResponseMock;

	@Mock
	private ServletOutputStream servletOutputStreamMock;

	private DocumentFileService documentFileService;

	@Captor
	private ArgumentCaptor<DocumentEntity> documentEntityCaptor;

	@BeforeEach
	void setUp() {
		documentFileService = new DocumentFileService(
			binaryStoreMock,
			documentRepositoryMock,
			documentResponsibilityRepositoryMock,
			statusPolicyMock);
	}

	@Test
	void readFileByRegistrationNumber() throws IOException {

		final var includeConfidential = false;
		final var documentEntity = createDocumentEntity();

		when(documentRepositoryMock.findTopByMunicipalityIdAndRegistrationNumberAndConfidentialityConfidentialInOrderByRevisionDesc(MUNICIPALITY_ID, REGISTRATION_NUMBER, PUBLIC.getValue())).thenReturn(Optional.of(documentEntity));
		when(httpServletResponseMock.getOutputStream()).thenReturn(servletOutputStreamMock);

		documentFileService.readFile(REGISTRATION_NUMBER, DOCUMENT_DATA_ID, includeConfidential, true, httpServletResponseMock, MUNICIPALITY_ID);

		verify(documentRepositoryMock).findTopByMunicipalityIdAndRegistrationNumberAndConfidentialityConfidentialInOrderByRevisionDesc(MUNICIPALITY_ID, REGISTRATION_NUMBER, PUBLIC.getValue());
		verify(httpServletResponseMock).addHeader(CONTENT_TYPE, MIME_TYPE);
		verify(httpServletResponseMock).addHeader(CONTENT_DISPOSITION, ContentDisposition.attachment().filename(FILE_NAME, StandardCharsets.UTF_8).build().toString());
		verify(httpServletResponseMock).setContentLength((int) FILE_SIZE_IN_BYTES);
		verify(httpServletResponseMock).getOutputStream();
		verify(binaryStoreMock).streamTo(eq(StorageRef.s3(STORAGE_LOCATOR)), any(OutputStream.class));
	}

	@Test
	void readFileByRegistrationNumber_withNonAsciiFilename_emitsRfc5987ContentDisposition() throws IOException {

		final var includeConfidential = false;
		final var documentEntity = createDocumentEntity();
		documentEntity.getDocumentData().getFirst().setFileName("fet säl.jpg");

		when(documentRepositoryMock.findTopByMunicipalityIdAndRegistrationNumberAndConfidentialityConfidentialInOrderByRevisionDesc(MUNICIPALITY_ID, REGISTRATION_NUMBER, PUBLIC.getValue())).thenReturn(Optional.of(documentEntity));
		when(httpServletResponseMock.getOutputStream()).thenReturn(servletOutputStreamMock);

		final var dispositionCaptor = ArgumentCaptor.forClass(String.class);

		documentFileService.readFile(REGISTRATION_NUMBER, DOCUMENT_DATA_ID, includeConfidential, true, httpServletResponseMock, MUNICIPALITY_ID);

		verify(httpServletResponseMock).addHeader(eq(CONTENT_DISPOSITION), dispositionCaptor.capture());
		assertThat(dispositionCaptor.getValue())
			.startsWith("attachment;")
			.contains("filename*=UTF-8''fet%20s%C3%A4l.jpg");
	}

	@Test
	void readFileByRegistrationNumberWhenNotFound() {

		final var includeConfidential = false;

		when(documentRepositoryMock.findTopByMunicipalityIdAndRegistrationNumberAndConfidentialityConfidentialInOrderByRevisionDesc(MUNICIPALITY_ID, REGISTRATION_NUMBER, PUBLIC.getValue())).thenReturn(empty());

		final var exception = assertThrows(ThrowableProblem.class, () -> documentFileService.readFile(REGISTRATION_NUMBER, DOCUMENT_DATA_ID, includeConfidential, true, httpServletResponseMock, MUNICIPALITY_ID));

		assertThat(exception).isNotNull();
		assertThat(exception.getMessage()).isEqualTo("Not Found: No document with registrationNumber: '2023-2281-4' could be found!");

		verify(documentRepositoryMock).findTopByMunicipalityIdAndRegistrationNumberAndConfidentialityConfidentialInOrderByRevisionDesc(MUNICIPALITY_ID, REGISTRATION_NUMBER, PUBLIC.getValue());
		verifyNoInteractions(httpServletResponseMock, binaryStoreMock);
	}

	@Test
	void readFileByRegistrationNumberWhenDocumentDataIdNotFound() {

		final var includeConfidential = false;
		final var documentEntity = createDocumentEntity();

		documentEntity.getDocumentData().getFirst().setId("Something else");

		when(documentRepositoryMock.findTopByMunicipalityIdAndRegistrationNumberAndConfidentialityConfidentialInOrderByRevisionDesc(MUNICIPALITY_ID, REGISTRATION_NUMBER, PUBLIC.getValue())).thenReturn(Optional.of(documentEntity));

		final var exception = assertThrows(ThrowableProblem.class, () -> documentFileService.readFile(REGISTRATION_NUMBER, DOCUMENT_DATA_ID, includeConfidential, true, httpServletResponseMock, MUNICIPALITY_ID));

		assertThat(exception).isNotNull();
		assertThat(exception.getMessage()).isEqualTo("Not Found: No document file content with ID: '" + DOCUMENT_DATA_ID + "' could be found!");

		verify(documentRepositoryMock).findTopByMunicipalityIdAndRegistrationNumberAndConfidentialityConfidentialInOrderByRevisionDesc(MUNICIPALITY_ID, REGISTRATION_NUMBER, PUBLIC.getValue());
		verifyNoInteractions(httpServletResponseMock, binaryStoreMock);
	}

	@Test
	void readFileByRegistrationNumberWhenFileContentNotFound() {

		final var includeConfidential = false;
		final var documentEntity = createDocumentEntity().withDocumentData(null);

		when(documentRepositoryMock.findTopByMunicipalityIdAndRegistrationNumberAndConfidentialityConfidentialInOrderByRevisionDesc(MUNICIPALITY_ID, REGISTRATION_NUMBER, PUBLIC.getValue())).thenReturn(Optional.of(documentEntity));

		final var exception = assertThrows(ThrowableProblem.class, () -> documentFileService.readFile(REGISTRATION_NUMBER, DOCUMENT_DATA_ID, includeConfidential, true, httpServletResponseMock, MUNICIPALITY_ID));

		assertThat(exception).isNotNull();
		assertThat(exception.getMessage()).isEqualTo("Not Found: No document file for registrationNumber: '2023-2281-4' could be found!");

		verify(documentRepositoryMock).findTopByMunicipalityIdAndRegistrationNumberAndConfidentialityConfidentialInOrderByRevisionDesc(MUNICIPALITY_ID, REGISTRATION_NUMBER, PUBLIC.getValue());
		verifyNoInteractions(httpServletResponseMock, binaryStoreMock);
	}

	@Test
	void readFileByRegistrationNumberResponseProcessingFailed() throws IOException {

		final var includeConfidential = false;
		final var documentEntity = createDocumentEntity();

		when(documentRepositoryMock.findTopByMunicipalityIdAndRegistrationNumberAndConfidentialityConfidentialInOrderByRevisionDesc(MUNICIPALITY_ID, REGISTRATION_NUMBER, PUBLIC.getValue())).thenReturn(Optional.of(documentEntity));
		when(httpServletResponseMock.getOutputStream()).thenReturn(servletOutputStreamMock);
		doThrow(new IOException("An error occured during byte array copy"))
			.when(binaryStoreMock).streamTo(any(StorageRef.class), any(OutputStream.class));

		final var exception = assertThrows(ThrowableProblem.class, () -> documentFileService.readFile(REGISTRATION_NUMBER, DOCUMENT_DATA_ID, includeConfidential, true, httpServletResponseMock, MUNICIPALITY_ID));

		assertThat(exception).isNotNull();
		assertThat(exception.getMessage()).isEqualTo("Internal Server Error: Could not read file content for document data with ID: '" + DOCUMENT_DATA_ID + "'!");

		verify(documentRepositoryMock).findTopByMunicipalityIdAndRegistrationNumberAndConfidentialityConfidentialInOrderByRevisionDesc(MUNICIPALITY_ID, REGISTRATION_NUMBER, PUBLIC.getValue());
		verify(httpServletResponseMock).addHeader(CONTENT_TYPE, MIME_TYPE);
		verify(httpServletResponseMock).addHeader(CONTENT_DISPOSITION, ContentDisposition.attachment().filename(FILE_NAME, StandardCharsets.UTF_8).build().toString());
		verify(httpServletResponseMock).setContentLength((int) FILE_SIZE_IN_BYTES);
		verify(httpServletResponseMock).getOutputStream();
		verify(binaryStoreMock).streamTo(eq(StorageRef.s3(STORAGE_LOCATOR)), any(OutputStream.class));
	}

	@Test
	void readFileByRegistrationNumberAndRevision() throws IOException {

		final var includeConfidential = false;
		final var documentEntity = createDocumentEntity();

		when(documentRepositoryMock.findByMunicipalityIdAndRegistrationNumberAndRevisionAndConfidentialityConfidentialIn(MUNICIPALITY_ID, REGISTRATION_NUMBER, REVISION, PUBLIC.getValue())).thenReturn(Optional.of(documentEntity));
		when(httpServletResponseMock.getOutputStream()).thenReturn(servletOutputStreamMock);

		documentFileService.readFile(REGISTRATION_NUMBER, REVISION, DOCUMENT_DATA_ID, includeConfidential, httpServletResponseMock, MUNICIPALITY_ID);

		verify(documentRepositoryMock).findByMunicipalityIdAndRegistrationNumberAndRevisionAndConfidentialityConfidentialIn(MUNICIPALITY_ID, REGISTRATION_NUMBER, REVISION, PUBLIC.getValue());
		verify(httpServletResponseMock).addHeader(CONTENT_TYPE, MIME_TYPE);
		verify(httpServletResponseMock).addHeader(CONTENT_DISPOSITION, ContentDisposition.attachment().filename(FILE_NAME, StandardCharsets.UTF_8).build().toString());
		verify(httpServletResponseMock).setContentLength((int) FILE_SIZE_IN_BYTES);
		verify(httpServletResponseMock).getOutputStream();
		verify(binaryStoreMock).streamTo(eq(StorageRef.s3(STORAGE_LOCATOR)), any(OutputStream.class));
	}

	@Test
	void readFileByRegistrationNumberAndRevisionWhenNotFound() {

		final var includeConfidential = false;

		when(documentRepositoryMock.findByMunicipalityIdAndRegistrationNumberAndRevisionAndConfidentialityConfidentialIn(MUNICIPALITY_ID, REGISTRATION_NUMBER, REVISION, PUBLIC.getValue())).thenReturn(empty());

		final var exception = assertThrows(ThrowableProblem.class, () -> documentFileService.readFile(REGISTRATION_NUMBER, REVISION, DOCUMENT_DATA_ID, includeConfidential, httpServletResponseMock, MUNICIPALITY_ID));

		assertThat(exception).isNotNull();
		assertThat(exception.getMessage()).isEqualTo("Not Found: No document with registrationNumber: '2023-2281-4' and revision: '1' could be found!");

		verify(documentRepositoryMock).findByMunicipalityIdAndRegistrationNumberAndRevisionAndConfidentialityConfidentialIn(MUNICIPALITY_ID, REGISTRATION_NUMBER, REVISION, PUBLIC.getValue());
		verifyNoInteractions(httpServletResponseMock, binaryStoreMock);
	}

	@Test
	void readFileByRegistrationNumberAndRevisionFileWhenContentNotFound() {

		final var includeConfidential = false;
		final var documentEntity = createDocumentEntity().withDocumentData(null);

		when(documentRepositoryMock.findByMunicipalityIdAndRegistrationNumberAndRevisionAndConfidentialityConfidentialIn(MUNICIPALITY_ID, REGISTRATION_NUMBER, REVISION, PUBLIC.getValue())).thenReturn(Optional.of(documentEntity));

		final var exception = assertThrows(ThrowableProblem.class, () -> documentFileService.readFile(REGISTRATION_NUMBER, REVISION, DOCUMENT_DATA_ID, includeConfidential, httpServletResponseMock, MUNICIPALITY_ID));

		assertThat(exception).isNotNull();
		assertThat(exception.getMessage()).isEqualTo("Not Found: No document file content with registrationNumber: '2023-2281-4' and revision: '1' could be found!");

		verify(documentRepositoryMock).findByMunicipalityIdAndRegistrationNumberAndRevisionAndConfidentialityConfidentialIn(MUNICIPALITY_ID, REGISTRATION_NUMBER, REVISION, PUBLIC.getValue());
		verifyNoInteractions(httpServletResponseMock, binaryStoreMock);
	}

	@Test
	void readFileByRegistrationNumberAndRevisionWhenDocumentDataIdNotFound() {

		final var includeConfidential = false;
		final var documentEntity = createDocumentEntity();

		documentEntity.getDocumentData().get(0).setId("Something else");

		when(documentRepositoryMock.findByMunicipalityIdAndRegistrationNumberAndRevisionAndConfidentialityConfidentialIn(MUNICIPALITY_ID, REGISTRATION_NUMBER, REVISION, PUBLIC.getValue())).thenReturn(Optional.of(documentEntity));

		final var exception = assertThrows(ThrowableProblem.class, () -> documentFileService.readFile(REGISTRATION_NUMBER, REVISION, DOCUMENT_DATA_ID, includeConfidential, httpServletResponseMock, MUNICIPALITY_ID));

		assertThat(exception).isNotNull();
		assertThat(exception.getMessage()).isEqualTo("Not Found: No document file content with ID: '" + DOCUMENT_DATA_ID + "' could be found!");

		verify(documentRepositoryMock).findByMunicipalityIdAndRegistrationNumberAndRevisionAndConfidentialityConfidentialIn(MUNICIPALITY_ID, REGISTRATION_NUMBER, REVISION, PUBLIC.getValue());
		verifyNoInteractions(httpServletResponseMock, binaryStoreMock);
	}

	@Test
	void addFile() throws IOException {

		final var existingEntity = createDocumentEntity();
		final var documentDataCreateRequest = DocumentDataCreateRequest.create()
			.withCreatedBy("changedUser");

		final var file = new File("src/test/resources/files/image2.png");
		final var multipartFile = (MultipartFile) new MockMultipartFile("file", file.getName(), "image/png", toByteArray(new FileInputStream(file)));

		when(documentRepositoryMock.findTopByMunicipalityIdAndRegistrationNumberAndConfidentialityConfidentialInOrderByRevisionDesc(MUNICIPALITY_ID, REGISTRATION_NUMBER, CONFIDENTIAL_AND_PUBLIC.getValue())).thenReturn(Optional.of(existingEntity));
		when(binaryStoreMock.put(any(InputStream.class), anyLong(), anyString(), anyMap())).thenReturn(StorageRef.s3(randomUUID().toString()));
		when(binaryStoreMock.copy(any(StorageRef.class))).thenAnswer(invocation -> StorageRef.s3(randomUUID().toString()));
		when(documentRepositoryMock.save(any(DocumentEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

		final var result = documentFileService.addOrReplaceFile(REGISTRATION_NUMBER, documentDataCreateRequest, multipartFile, MUNICIPALITY_ID);

		assertThat(result).isNotNull();

		verify(binaryStoreMock).put(any(InputStream.class), eq(file.length()), eq("image/png"), anyMap());
		verify(documentRepositoryMock).save(documentEntityCaptor.capture());

		final var capturedDocumentEntity = documentEntityCaptor.getValue();
		assertThat(capturedDocumentEntity).isNotNull();
		assertThat(capturedDocumentEntity.getConfidentiality()).isEqualTo(existingEntity.getConfidentiality());
		assertThat(capturedDocumentEntity.getCreatedBy()).isEqualTo("changedUser");
		assertThat(capturedDocumentEntity.getDescription()).isEqualTo(existingEntity.getDescription());
		assertThat(capturedDocumentEntity.getDocumentData())
			.hasSize(2)
			.extracting(DocumentDataEntity::getFileName)
			.containsExactlyInAnyOrder(
				"image.png",
				"image2.png");
		assertThat(capturedDocumentEntity.getMetadata()).isEqualTo(existingEntity.getMetadata());
		assertThat(capturedDocumentEntity.getMunicipalityId()).isEqualTo(existingEntity.getMunicipalityId());
		assertThat(capturedDocumentEntity.getRegistrationNumber()).isEqualTo(existingEntity.getRegistrationNumber());
		assertThat(capturedDocumentEntity.getType()).isNotNull().satisfies(type -> {
			assertThat(type.getType()).isEqualTo(DOCUMENT_TYPE);
		});
	}

	@Test
	void addFileWithSameName() throws IOException {

		final var existingEntity = createDocumentEntity();
		final var documentDataCreateRequest = DocumentDataCreateRequest.create()
			.withCreatedBy("changedUser");

		final var file = new File("src/test/resources/files/image2.png");
		final var multipartFile = (MultipartFile) new MockMultipartFile("file", FILE_NAME, "image/png", toByteArray(new FileInputStream(file)));

		when(documentRepositoryMock.findTopByMunicipalityIdAndRegistrationNumberAndConfidentialityConfidentialInOrderByRevisionDesc(MUNICIPALITY_ID, REGISTRATION_NUMBER, CONFIDENTIAL_AND_PUBLIC.getValue())).thenReturn(Optional.of(existingEntity));
		when(binaryStoreMock.put(any(InputStream.class), anyLong(), anyString(), anyMap())).thenReturn(StorageRef.s3(randomUUID().toString()));
		when(binaryStoreMock.copy(any(StorageRef.class))).thenAnswer(invocation -> StorageRef.s3(randomUUID().toString()));
		when(documentRepositoryMock.save(any(DocumentEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

		final var result = documentFileService.addOrReplaceFile(REGISTRATION_NUMBER, documentDataCreateRequest, multipartFile, MUNICIPALITY_ID);

		assertThat(result).isNotNull();

		verify(binaryStoreMock).put(any(InputStream.class), eq(file.length()), eq("image/png"), anyMap());
		verify(documentRepositoryMock).save(documentEntityCaptor.capture());

		final var capturedDocumentEntity = documentEntityCaptor.getValue();
		assertThat(capturedDocumentEntity).isNotNull();
		assertThat(capturedDocumentEntity.getConfidentiality()).isEqualTo(existingEntity.getConfidentiality());
		assertThat(capturedDocumentEntity.getCreatedBy()).isEqualTo("changedUser");
		assertThat(capturedDocumentEntity.getDescription()).isEqualTo(existingEntity.getDescription());
		assertThat(capturedDocumentEntity.getDocumentData())
			.hasSize(1)
			.extracting(DocumentDataEntity::getFileName)
			.containsExactlyInAnyOrder("image.png");
		assertThat(capturedDocumentEntity.getMetadata()).isEqualTo(existingEntity.getMetadata());
		assertThat(capturedDocumentEntity.getMunicipalityId()).isEqualTo(existingEntity.getMunicipalityId());
		assertThat(capturedDocumentEntity.getRegistrationNumber()).isEqualTo(existingEntity.getRegistrationNumber());
	}

	@Test
	void addFileWhenNotFound() throws IOException {

		final var documentDataCreateRequest = DocumentDataCreateRequest.create()
			.withCreatedBy("changedUser");

		final var file = new File("src/test/resources/files/image2.png");
		final var multipartFile = (MultipartFile) new MockMultipartFile("file", file.getName(), "image/png", toByteArray(new FileInputStream(file)));

		when(documentRepositoryMock.findTopByMunicipalityIdAndRegistrationNumberAndConfidentialityConfidentialInOrderByRevisionDesc(any(), any(), any())).thenReturn(Optional.empty());

		final var exception = assertThrows(ThrowableProblem.class, () -> documentFileService.addOrReplaceFile(REGISTRATION_NUMBER, documentDataCreateRequest, multipartFile, MUNICIPALITY_ID));

		assertThat(exception).isNotNull();
		assertThat(exception.getMessage()).isEqualTo("Not Found: No document with registrationNumber: '2023-2281-4' could be found!");

		verify(documentRepositoryMock).findTopByMunicipalityIdAndRegistrationNumberAndConfidentialityConfidentialInOrderByRevisionDesc(MUNICIPALITY_ID, REGISTRATION_NUMBER, CONFIDENTIAL_AND_PUBLIC.getValue());
		verifyNoMoreInteractions(documentRepositoryMock);
		verifyNoInteractions(binaryStoreMock);
	}

	@Test
	void addMultipleFilesProducesSingleRevision() throws IOException {

		final var existingEntity = createDocumentEntity();
		final var documentDataCreateRequest = DocumentDataCreateRequest.create()
			.withCreatedBy("changedUser");

		final var file2 = new File("src/test/resources/files/image2.png");
		final var file3 = new File("src/test/resources/files/readme.txt");
		final var multipartFile2 = (MultipartFile) new MockMultipartFile("file", file2.getName(), "image/png", toByteArray(new FileInputStream(file2)));
		final var multipartFile3 = (MultipartFile) new MockMultipartFile("file", file3.getName(), "text/plain", toByteArray(new FileInputStream(file3)));
		final var documentFiles = DocumentFiles.create().withFiles(List.of(multipartFile2, multipartFile3));

		when(documentRepositoryMock.findTopByMunicipalityIdAndRegistrationNumberAndConfidentialityConfidentialInOrderByRevisionDesc(MUNICIPALITY_ID, REGISTRATION_NUMBER, CONFIDENTIAL_AND_PUBLIC.getValue())).thenReturn(Optional.of(existingEntity));
		when(binaryStoreMock.put(any(InputStream.class), anyLong(), anyString(), anyMap())).thenReturn(StorageRef.s3(randomUUID().toString()));
		when(binaryStoreMock.copy(any(StorageRef.class))).thenAnswer(invocation -> StorageRef.s3(randomUUID().toString()));
		when(documentRepositoryMock.save(any(DocumentEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

		final var result = documentFileService.addOrReplaceFiles(REGISTRATION_NUMBER, documentDataCreateRequest, documentFiles, MUNICIPALITY_ID);

		assertThat(result).isNotNull();

		verify(documentRepositoryMock).save(documentEntityCaptor.capture());
		verify(binaryStoreMock, org.mockito.Mockito.times(2)).put(any(InputStream.class), anyLong(), anyString(), anyMap());

		final var capturedDocumentEntity = documentEntityCaptor.getValue();
		assertThat(capturedDocumentEntity).isNotNull();
		assertThat(capturedDocumentEntity.getRevision()).isEqualTo(existingEntity.getRevision() + 1);
		assertThat(capturedDocumentEntity.getCreatedBy()).isEqualTo("changedUser");
		assertThat(capturedDocumentEntity.getDocumentData())
			.hasSize(3)
			.extracting(DocumentDataEntity::getFileName)
			.containsExactlyInAnyOrder(
				"image.png",
				"image2.png",
				"readme.txt");
	}

	@Test
	void deleteFileByRegistrationNumberAndDocumentDataId() {

		final var documentEntity = createDocumentEntity();

		when(documentRepositoryMock.findTopByMunicipalityIdAndRegistrationNumberAndConfidentialityConfidentialInOrderByRevisionDesc(MUNICIPALITY_ID, REGISTRATION_NUMBER, CONFIDENTIAL_AND_PUBLIC.getValue())).thenReturn(Optional.of(documentEntity));
		when(binaryStoreMock.copy(any(StorageRef.class))).thenAnswer(invocation -> StorageRef.s3(randomUUID().toString()));

		documentFileService.deleteFile(REGISTRATION_NUMBER, DOCUMENT_DATA_ID, MUNICIPALITY_ID);

		verify(documentRepositoryMock).findTopByMunicipalityIdAndRegistrationNumberAndConfidentialityConfidentialInOrderByRevisionDesc(MUNICIPALITY_ID, REGISTRATION_NUMBER, CONFIDENTIAL_AND_PUBLIC.getValue());
		verify(documentRepositoryMock).save(documentEntityCaptor.capture());

		final var capturedEntity = documentEntityCaptor.getValue();
		assertThat(capturedEntity).isNotNull();
		assertThat(capturedEntity.getCreatedBy()).isEqualTo(CREATED_BY);
		assertThat(capturedEntity.getDocumentData()).isEmpty();
		assertThat(capturedEntity.getMetadata()).isEqualTo(List.of(DocumentMetadataEmbeddable.create().withKey(METADATA_KEY).withValue(METADATA_VALUE)));
		assertThat(capturedEntity.getMunicipalityId()).isEqualTo(MUNICIPALITY_ID);
		assertThat(capturedEntity.getRegistrationNumber()).isEqualTo(REGISTRATION_NUMBER);
		assertThat(capturedEntity.getRevision()).isEqualTo(REVISION + 1);
	}

	@Test
	void deleteFileByRegistrationNumberAndDocumentDataIdWhenNotFound() {

		when(documentRepositoryMock.findTopByMunicipalityIdAndRegistrationNumberAndConfidentialityConfidentialInOrderByRevisionDesc(MUNICIPALITY_ID, REGISTRATION_NUMBER, CONFIDENTIAL_AND_PUBLIC.getValue())).thenReturn(empty());

		final var exception = assertThrows(ThrowableProblem.class, () -> documentFileService.deleteFile(REGISTRATION_NUMBER, DOCUMENT_DATA_ID, MUNICIPALITY_ID));

		assertThat(exception).isNotNull();
		assertThat(exception.getMessage()).isEqualTo("Not Found: No document with registrationNumber: '2023-2281-4' could be found!");

		verify(documentRepositoryMock).findTopByMunicipalityIdAndRegistrationNumberAndConfidentialityConfidentialInOrderByRevisionDesc(MUNICIPALITY_ID, REGISTRATION_NUMBER, CONFIDENTIAL_AND_PUBLIC.getValue());
		verifyNoMoreInteractions(documentRepositoryMock);
		verifyNoInteractions(binaryStoreMock);
	}

	@Test
	void deleteFileByRegistrationNumberAndDocumentDataIdWhenDocumentDataIsEmpty() {

		final var documentEntity = createDocumentEntity().withDocumentData(emptyList());

		when(documentRepositoryMock.findTopByMunicipalityIdAndRegistrationNumberAndConfidentialityConfidentialInOrderByRevisionDesc(MUNICIPALITY_ID, REGISTRATION_NUMBER, CONFIDENTIAL_AND_PUBLIC.getValue())).thenReturn(Optional.of(documentEntity));

		final var exception = assertThrows(ThrowableProblem.class, () -> documentFileService.deleteFile(REGISTRATION_NUMBER, DOCUMENT_DATA_ID, MUNICIPALITY_ID));

		assertThat(exception).isNotNull();
		assertThat(exception.getMessage()).isEqualTo("Not Found: No document file for registrationNumber: '2023-2281-4' could be found!");

		verify(documentRepositoryMock).findTopByMunicipalityIdAndRegistrationNumberAndConfidentialityConfidentialInOrderByRevisionDesc(MUNICIPALITY_ID, REGISTRATION_NUMBER, CONFIDENTIAL_AND_PUBLIC.getValue());
		verifyNoMoreInteractions(documentRepositoryMock);
	}

	@Test
	void deleteFileByRegistrationNumberAndDocumentDataIdWhenDocumentDataIdIsNotFound() {

		final var documentEntity = createDocumentEntity();

		documentEntity.getDocumentData().getFirst().withId("some-id-that-will-not-be-found");

		when(documentRepositoryMock.findTopByMunicipalityIdAndRegistrationNumberAndConfidentialityConfidentialInOrderByRevisionDesc(MUNICIPALITY_ID, REGISTRATION_NUMBER, CONFIDENTIAL_AND_PUBLIC.getValue())).thenReturn(Optional.of(documentEntity));
		when(binaryStoreMock.copy(any(StorageRef.class))).thenAnswer(invocation -> StorageRef.s3(randomUUID().toString()));

		final var exception = assertThrows(ThrowableProblem.class, () -> documentFileService.deleteFile(REGISTRATION_NUMBER, DOCUMENT_DATA_ID, MUNICIPALITY_ID));

		assertThat(exception).isNotNull();
		assertThat(exception.getMessage()).isEqualTo("Not Found: No document file content with ID: '" + DOCUMENT_DATA_ID + "' could be found!");

		verify(documentRepositoryMock).findTopByMunicipalityIdAndRegistrationNumberAndConfidentialityConfidentialInOrderByRevisionDesc(MUNICIPALITY_ID, REGISTRATION_NUMBER, CONFIDENTIAL_AND_PUBLIC.getValue());
		verifyNoMoreInteractions(documentRepositoryMock);
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
				.withType(DocumentTypeEntity.create()
					.withMunicipalityId(MUNICIPALITY_ID)
					.withType(DOCUMENT_TYPE)
					.withDisplayName(DOCUMENT_TYPE_DISPLAYNAME));
		} catch (final Exception e) {
			fail("Entity could not be created!");
		}
		return null;
	}

	private DocumentDataEntity createDocumentDataEntity() {
		return DocumentDataEntity.create()
			.withId(DOCUMENT_DATA_ID)
			.withStorageLocator(STORAGE_LOCATOR)
			.withFileName(FILE_NAME)
			.withMimeType(MIME_TYPE)
			.withFileSizeInBytes(FILE_SIZE_IN_BYTES);
	}
}
