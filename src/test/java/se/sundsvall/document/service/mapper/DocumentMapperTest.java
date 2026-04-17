package se.sundsvall.document.service.mapper;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import se.sundsvall.dept44.models.api.paging.PagingMetaData;
import se.sundsvall.document.api.model.Confidentiality;
import se.sundsvall.document.api.model.ConfidentialityUpdateRequest;
import se.sundsvall.document.api.model.Document;
import se.sundsvall.document.api.model.DocumentCreateRequest;
import se.sundsvall.document.api.model.DocumentData;
import se.sundsvall.document.api.model.DocumentFiles;
import se.sundsvall.document.api.model.DocumentMetadata;
import se.sundsvall.document.api.model.DocumentResponsibility;
import se.sundsvall.document.api.model.DocumentStatus;
import se.sundsvall.document.api.model.DocumentUpdateRequest;
import se.sundsvall.document.integration.db.model.ConfidentialityEmbeddable;
import se.sundsvall.document.integration.db.model.DocumentDataEntity;
import se.sundsvall.document.integration.db.model.DocumentEntity;
import se.sundsvall.document.integration.db.model.DocumentMetadataEmbeddable;
import se.sundsvall.document.integration.db.model.DocumentResponsibilityEntity;
import se.sundsvall.document.integration.db.model.DocumentTypeEntity;
import se.sundsvall.document.service.storage.BinaryStore;
import se.sundsvall.document.service.storage.StorageRef;

import static java.time.OffsetDateTime.now;
import static java.time.ZoneId.systemDefault;
import static java.util.Collections.emptyList;
import static java.util.UUID.randomUUID;
import static org.apache.commons.io.IOUtils.toByteArray;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.data.domain.Sort.Direction.ASC;

@ExtendWith(MockitoExtension.class)
class DocumentMapperTest {

	private static final boolean ARCHIVE = true;
	private static final boolean CONFIDENTIAL = true;
	private static final String DESCRIPTION = "Description";
	private static final OffsetDateTime CREATED = now(systemDefault());
	private static final String CREATED_BY = "createdBy";
	private static final String FILE_1_NAME = "filename1.png";
	private static final String FILE_2_NAME = "filename2.txt";
	private static final long FILE_1_SIZE_IN_BYTES = 1000;
	private static final long FILE_2_SIZE_IN_BYTES = 2000;
	private static final String LEGAL_CITATION = "legalCitation";
	private static final String MIME_TYPE_1 = "image/png";
	private static final String MIME_TYPE_2 = "text/plain";
	private static final String ID = "id";
	private static final String STORAGE_BACKEND = "jdbc";
	private static final String STORAGE_LOCATOR_1 = "locator-1";
	private static final String STORAGE_LOCATOR_2 = "locator-2";
	private static final String METADATA_KEY = "key";
	private static final String METADATA_VALUE = "value";
	private static final String MUNICIPALITY_ID = "2281";
	private static final String REGISTRATION_NUMBER = "reistrationNumber";
	private static final int REVISION = 666;
	private static final String DOCUMENT_TYPE = "documentType";
	private static final OffsetDateTime DOCUMENT_TYPE_CREATED = now(systemDefault()).minusDays(7);
	private static final String DOCUMENT_TYPE_CREATED_BY = "documentTypeCreatedBy";
	private static final String DOCUMENT_TYPE_DISPLAY_NAME = "documentTypeDisplayName";
	private static final String DOCUMENT_TYPE_ID = "documentTypeId";
	private static final OffsetDateTime DOCUMENT_TYPE_UPDATED = now(systemDefault()).minusDays(6);
	private static final String DOCUMENT_TYPE_UPDATED_BY = "documentTypeUpdatedBy";
	private static final LocalDate VALID_FROM = LocalDate.of(2026, 4, 15);
	private static final LocalDate VALID_TO = LocalDate.of(2027, 4, 15);

	@Mock
	private BinaryStore binaryStoreMock;

	@Test
	void toDocumentEntityFromDocumentCreateRequest() {

		// Arrange
		final var documentCreateRequest = DocumentCreateRequest.create()
			.withConfidentiality(Confidentiality.create()
				.withConfidential(CONFIDENTIAL)
				.withLegalCitation(LEGAL_CITATION))
			.withCreatedBy(CREATED_BY)
			.withDescription(DESCRIPTION)
			.withMetadataList(List.of(DocumentMetadata.create()
				.withKey(METADATA_KEY)
				.withValue(METADATA_VALUE)))
			.withValidFrom(VALID_FROM)
			.withValidTo(VALID_TO);

		// Act
		final var result = DocumentMapper.toDocumentEntity(documentCreateRequest, MUNICIPALITY_ID);

		// Assert
		assertThat(result)
			.isNotNull()
			.isEqualTo(DocumentEntity.create()
				.withConfidentiality(ConfidentialityEmbeddable.create()
					.withConfidential(CONFIDENTIAL)
					.withLegalCitation(LEGAL_CITATION))
				.withCreatedBy(CREATED_BY)
				.withDescription(DESCRIPTION)
				.withMetadata(List.of(DocumentMetadataEmbeddable.create()
					.withKey(METADATA_KEY)
					.withValue(METADATA_VALUE)))
				.withMunicipalityId(MUNICIPALITY_ID)
				.withStatus(DocumentStatus.DRAFT)
				.withValidFrom(VALID_FROM)
				.withValidTo(VALID_TO));
	}

	@ParameterizedTest
	@ValueSource(booleans = {
		true, false
	})
	@NullSource
	void applyUpdateMutatesEntityInPlace(Boolean archive) {

		// Arrange
		final var mimeType = "image/png";
		final var fileName1 = "image1.png";
		final var fileName2 = "image2.png";
		final var originalDocumentData = List.of(
			DocumentDataEntity.create()
				.withFileName(fileName1)
				.withFileSizeInBytes(1000)
				.withMimeType(mimeType)
				.withStorageBackend(STORAGE_BACKEND)
				.withStorageLocator(STORAGE_LOCATOR_1),
			DocumentDataEntity.create()
				.withFileName(fileName2)
				.withFileSizeInBytes(2000)
				.withMimeType(mimeType)
				.withStorageBackend(STORAGE_BACKEND)
				.withStorageLocator(STORAGE_LOCATOR_2));

		final var documentUpdateRequest = DocumentUpdateRequest.create()
			.withArchive(archive)
			.withDescription("Updated text")
			.withMetadataList(List.of(DocumentMetadata.create()
				.withKey("Updated-key")
				.withValue("Updated-value")));

		final var existingDocumentEntity = DocumentEntity.create()
			.withArchive(ARCHIVE)
			.withConfidentiality(ConfidentialityEmbeddable.create()
				.withConfidential(CONFIDENTIAL)
				.withLegalCitation(LEGAL_CITATION))
			.withCreatedBy(CREATED_BY)
			.withDescription(DESCRIPTION)
			.withDocumentData(originalDocumentData)
			.withMetadata(new ArrayList<>(List.of(DocumentMetadataEmbeddable.create()
				.withKey(METADATA_KEY)
				.withValue(METADATA_VALUE))))
			.withMunicipalityId(MUNICIPALITY_ID)
			.withRegistrationNumber(REGISTRATION_NUMBER)
			.withRevision(REVISION)
			.withType(DocumentTypeEntity.create()
				.withCreated(DOCUMENT_TYPE_CREATED)
				.withCreatedBy(DOCUMENT_TYPE_CREATED_BY)
				.withDisplayName(DOCUMENT_TYPE_DISPLAY_NAME)
				.withId(DOCUMENT_TYPE_ID)
				.withLastUpdated(DOCUMENT_TYPE_UPDATED)
				.withLastUpdatedBy(DOCUMENT_TYPE_UPDATED_BY)
				.withMunicipalityId(MUNICIPALITY_ID)
				.withType(DOCUMENT_TYPE))
			.withValidFrom(VALID_FROM)
			.withValidTo(VALID_TO);

		// Act
		DocumentMapper.applyUpdate(documentUpdateRequest, existingDocumentEntity);

		// Assert — fields from request are applied
		assertThat(existingDocumentEntity.isArchive()).isEqualTo(archive == null ? ARCHIVE : archive);
		assertThat(existingDocumentEntity.getDescription()).isEqualTo("Updated text");
		assertThat(existingDocumentEntity.getMetadata()).isEqualTo(List.of(DocumentMetadataEmbeddable.create()
			.withKey("Updated-key")
			.withValue("Updated-value")));

		// Assert — revision, createdBy, documentData, and other fields are NOT changed
		assertThat(existingDocumentEntity.getRevision()).isEqualTo(REVISION);
		assertThat(existingDocumentEntity.getCreatedBy()).isEqualTo(CREATED_BY);
		assertThat(existingDocumentEntity.getDocumentData()).isSameAs(originalDocumentData);
		assertThat(existingDocumentEntity.getMunicipalityId()).isEqualTo(MUNICIPALITY_ID);
		assertThat(existingDocumentEntity.getRegistrationNumber()).isEqualTo(REGISTRATION_NUMBER);
		assertThat(existingDocumentEntity.getConfidentiality()).isEqualTo(ConfidentialityEmbeddable.create()
			.withConfidential(CONFIDENTIAL)
			.withLegalCitation(LEGAL_CITATION));
		assertThat(existingDocumentEntity.getValidFrom()).isEqualTo(VALID_FROM);
		assertThat(existingDocumentEntity.getValidTo()).isEqualTo(VALID_TO);
	}

	@Test
	void applyUpdateOverridesValidityWhenSupplied() {

		// Arrange
		final var newValidFrom = VALID_FROM.plusYears(1);
		final var newValidTo = VALID_TO.plusYears(1);

		final var documentUpdateRequest = DocumentUpdateRequest.create()
			.withValidFrom(newValidFrom)
			.withValidTo(newValidTo);

		final var existingDocumentEntity = DocumentEntity.create()
			.withCreatedBy(CREATED_BY)
			.withMunicipalityId(MUNICIPALITY_ID)
			.withRegistrationNumber(REGISTRATION_NUMBER)
			.withRevision(REVISION)
			.withType(DocumentTypeEntity.create().withType(DOCUMENT_TYPE))
			.withValidFrom(VALID_FROM)
			.withValidTo(VALID_TO);

		// Act
		DocumentMapper.applyUpdate(documentUpdateRequest, existingDocumentEntity);

		// Assert
		assertThat(existingDocumentEntity.getValidFrom()).isEqualTo(newValidFrom);
		assertThat(existingDocumentEntity.getValidTo()).isEqualTo(newValidTo);
		assertThat(existingDocumentEntity.getRevision()).isEqualTo(REVISION);
	}

	@Test
	void toConfidentialityEmbeddableFromConfidentiality() {

		// Arrange
		final var confidentiality = Confidentiality.create()
			.withConfidential(CONFIDENTIAL)
			.withLegalCitation(LEGAL_CITATION);

		// Act
		final var result = DocumentMapper.toConfidentialityEmbeddable(confidentiality);

		// Assert
		assertThat(result)
			.isNotNull()
			.isEqualTo(ConfidentialityEmbeddable.create()
				.withConfidential(CONFIDENTIAL)
				.withLegalCitation(LEGAL_CITATION));
	}

	@Test
	void toConfidentialityEmbeddableFromConfidentialityWhenInputIsNull() {

		// Act
		final var result = DocumentMapper.toConfidentialityEmbeddable((Confidentiality) null);

		// Assert
		assertThat(result)
			.isNotNull()
			.isEqualTo(ConfidentialityEmbeddable.create()
				.withConfidential(false)
				.withLegalCitation(null));
	}

	@Test
	void toConfidentialityEmbeddableFromConfidentialityUpdateRequest() {

		// Arrange
		final var confidentiality = ConfidentialityUpdateRequest.create()
			.withConfidential(CONFIDENTIAL)
			.withLegalCitation(LEGAL_CITATION)
			.withChangedBy(CREATED_BY);

		// Act
		final var result = DocumentMapper.toConfidentialityEmbeddable(confidentiality);

		// Assert
		assertThat(result)
			.isNotNull()
			.isEqualTo(ConfidentialityEmbeddable.create()
				.withConfidential(CONFIDENTIAL)
				.withLegalCitation(LEGAL_CITATION));
	}

	@Test
	void toConfidentialityEmbeddableFromConfidentialityUpdateRequestWhenInputIsNull() {

		// Act
		final var result = DocumentMapper.toConfidentialityEmbeddable((ConfidentialityUpdateRequest) null);

		// Assert
		assertThat(result)
			.isNotNull()
			.isEqualTo(ConfidentialityEmbeddable.create()
				.withConfidential(false)
				.withLegalCitation(null));
	}

	@Test
	void toConfidentiality() {

		// Arrange
		final var confidentialityEmbeddable = ConfidentialityEmbeddable.create()
			.withConfidential(CONFIDENTIAL)
			.withLegalCitation(LEGAL_CITATION);

		// Act
		final var result = DocumentMapper.toConfidentiality(confidentialityEmbeddable);

		// Assert
		assertThat(result)
			.isNotNull()
			.isEqualTo(Confidentiality.create()
				.withConfidential(CONFIDENTIAL)
				.withLegalCitation(LEGAL_CITATION));
	}

	@Test
	void toConfidentialityWhenInputIsNull() {

		// Act
		final var result = DocumentMapper.toConfidentiality(null);

		// Assert
		assertThat(result).isNull();
	}

	@Test
	void toDocumentList() {

		// Arrange
		final var documentEntity = DocumentEntity.create()
			.withArchive(ARCHIVE)
			.withConfidentiality(ConfidentialityEmbeddable.create()
				.withConfidential(CONFIDENTIAL)
				.withLegalCitation(LEGAL_CITATION))
			.withCreated(CREATED)
			.withCreatedBy(CREATED_BY)
			.withDescription(DESCRIPTION)
			.withDocumentData(List.of(
				DocumentDataEntity.create()
					.withFileName(FILE_1_NAME)
					.withFileSizeInBytes(FILE_1_SIZE_IN_BYTES)
					.withId(ID)
					.withMimeType(MIME_TYPE_1),
				DocumentDataEntity.create()
					.withFileName(FILE_2_NAME)
					.withFileSizeInBytes(FILE_2_SIZE_IN_BYTES)
					.withId(ID)
					.withMimeType(MIME_TYPE_2)))
			.withId(ID)
			.withMetadata(List.of(DocumentMetadataEmbeddable.create()
				.withKey(METADATA_KEY)
				.withValue(METADATA_VALUE)))
			.withMunicipalityId(MUNICIPALITY_ID)
			.withRegistrationNumber(REGISTRATION_NUMBER)
			.withRevision(REVISION)
			.withType(DocumentTypeEntity.create()
				.withType(DOCUMENT_TYPE));

		// Act
		final var result = DocumentMapper.toDocumentList(List.of(documentEntity));

		// Assert
		assertThat(result)
			.hasSize(1)
			.containsExactly(Document.create()
				.withArchive(ARCHIVE)
				.withConfidentiality(Confidentiality.create()
					.withConfidential(CONFIDENTIAL)
					.withLegalCitation(LEGAL_CITATION))
				.withCreated(CREATED)
				.withCreatedBy(CREATED_BY)
				.withDescription(DESCRIPTION)
				.withDocumentData(List.of(
					DocumentData.create()
						.withFileName(FILE_1_NAME)
						.withFileSizeInBytes(FILE_1_SIZE_IN_BYTES)
						.withId(ID)
						.withMimeType(MIME_TYPE_1),
					DocumentData.create()
						.withFileName(FILE_2_NAME)
						.withFileSizeInBytes(FILE_2_SIZE_IN_BYTES)
						.withId(ID)
						.withMimeType(MIME_TYPE_2)))
				.withId(ID)
				.withMetadataList(List.of(DocumentMetadata.create()
					.withKey(METADATA_KEY)
					.withValue(METADATA_VALUE)))
				.withMunicipalityId(MUNICIPALITY_ID)
				.withRegistrationNumber(REGISTRATION_NUMBER)
				.withResponsibilities(emptyList())
				.withRevision(REVISION)
				.withType(DOCUMENT_TYPE));
	}

	@Test
	void toDocumentListWhenInputIsNull() {
		assertThat(DocumentMapper.toDocumentList(null)).isEmpty();
	}

	@Test
	void toDocument() {

		// Arrange
		final var documentEntity = DocumentEntity.create()
			.withArchive(ARCHIVE)
			.withConfidentiality(ConfidentialityEmbeddable.create()
				.withConfidential(CONFIDENTIAL)
				.withLegalCitation(LEGAL_CITATION))
			.withCreated(CREATED)
			.withCreatedBy(CREATED_BY)
			.withDescription(DESCRIPTION)
			.withDocumentData(List.of(
				DocumentDataEntity.create()
					.withFileName(FILE_1_NAME)
					.withFileSizeInBytes(FILE_1_SIZE_IN_BYTES)
					.withId(ID)
					.withMimeType(MIME_TYPE_1),
				DocumentDataEntity.create()
					.withFileName(FILE_2_NAME)
					.withFileSizeInBytes(FILE_2_SIZE_IN_BYTES)
					.withId(ID)
					.withMimeType(MIME_TYPE_2)))
			.withId(ID)
			.withMetadata(List.of(DocumentMetadataEmbeddable.create()
				.withKey(METADATA_KEY)
				.withValue(METADATA_VALUE)))
			.withMunicipalityId(MUNICIPALITY_ID)
			.withRegistrationNumber(REGISTRATION_NUMBER)
			.withRevision(REVISION)
			.withType(DocumentTypeEntity.create()
				.withType(DOCUMENT_TYPE))
			.withValidFrom(VALID_FROM)
			.withValidTo(VALID_TO);

		// Act
		final var result = DocumentMapper.toDocument(documentEntity);

		// Assert
		assertThat(result)
			.isNotNull()
			.isEqualTo(Document.create()
				.withArchive(ARCHIVE)
				.withConfidentiality(Confidentiality.create()
					.withConfidential(CONFIDENTIAL)
					.withLegalCitation(LEGAL_CITATION))
				.withCreated(CREATED)
				.withCreatedBy(CREATED_BY)
				.withDescription(DESCRIPTION)
				.withDocumentData(List.of(
					DocumentData.create()
						.withFileName(FILE_1_NAME)
						.withFileSizeInBytes(FILE_1_SIZE_IN_BYTES)
						.withId(ID)
						.withMimeType(MIME_TYPE_1),
					DocumentData.create()
						.withFileName(FILE_2_NAME)
						.withFileSizeInBytes(FILE_2_SIZE_IN_BYTES)
						.withId(ID)
						.withMimeType(MIME_TYPE_2)))
				.withId(ID)
				.withMetadataList(List.of(DocumentMetadata.create()
					.withKey(METADATA_KEY)
					.withValue(METADATA_VALUE)))
				.withMunicipalityId(MUNICIPALITY_ID)
				.withRegistrationNumber(REGISTRATION_NUMBER)
				.withResponsibilities(emptyList())
				.withRevision(REVISION)
				.withType(DOCUMENT_TYPE)
				.withValidFrom(VALID_FROM)
				.withValidTo(VALID_TO));
	}

	@Test
	void toDocumentWithResponsibilities() {

		// Arrange
		final var documentEntity = DocumentEntity.create()
			.withType(DocumentTypeEntity.create().withType(DOCUMENT_TYPE));
		final var responsibilityEntity = DocumentResponsibilityEntity.create()
			.withUsername(CREATED_BY);

		// Act
		final var result = DocumentMapper.toDocument(documentEntity, List.of(responsibilityEntity));

		// Assert
		assertThat(result).isNotNull();
		assertThat(result.getResponsibilities())
			.containsExactly(DocumentResponsibility.create()
				.withUsername(CREATED_BY));
	}

	@Test
	void toDocumentWhenInputIsNull() {
		assertThat(DocumentMapper.toDocument(null)).isNull();
	}

	@Test
	void toDocumentResponsibilityEntities() {

		// Arrange
		final var responsibilities = List.of(DocumentResponsibility.create()
			.withUsername(" Username123 "));

		// Act
		final var result = DocumentMapper.toDocumentResponsibilityEntities(responsibilities, MUNICIPALITY_ID, REGISTRATION_NUMBER, CREATED_BY);

		// Assert
		assertThat(result)
			.hasSize(1)
			.extracting(DocumentResponsibilityEntity::getMunicipalityId, DocumentResponsibilityEntity::getRegistrationNumber, DocumentResponsibilityEntity::getUsername,
				DocumentResponsibilityEntity::getCreatedBy)
			.containsExactly(tuple(MUNICIPALITY_ID, REGISTRATION_NUMBER, "username123", CREATED_BY));
	}

	@Test
	void toDocumentDataEntitiesFromMultipart() throws IOException {

		// Arrange
		final var newLocator = randomUUID().toString();
		final var mimeType = "image/png";
		final var file = new File("src/test/resources/files/image.png");
		final var fileName = file.getName();
		final var multipartFile = (MultipartFile) new MockMultipartFile("file", fileName, mimeType, toByteArray(new FileInputStream(file)));
		final var documents = DocumentFiles.create().withFiles(List.of(multipartFile));

		when(binaryStoreMock.put(any(InputStream.class), anyLong(), anyString(), anyMap())).thenReturn(StorageRef.jdbc(newLocator));

		// Act
		final var result = DocumentMapper.toDocumentDataEntities(documents, binaryStoreMock, MUNICIPALITY_ID);

		// Assert
		assertThat(result)
			.isNotNull()
			.isNotEmpty()
			.extracting(
				DocumentDataEntity::getFileName,
				DocumentDataEntity::getMimeType,
				DocumentDataEntity::getFileSizeInBytes,
				DocumentDataEntity::getStorageBackend,
				DocumentDataEntity::getStorageLocator)
			.containsExactly(tuple(
				fileName,
				mimeType,
				file.length(),
				"jdbc",
				newLocator));

		verify(binaryStoreMock).put(
			any(InputStream.class),
			eq(file.length()),
			eq(mimeType),
			eq(Map.of("original-filename", fileName, "municipality-id", MUNICIPALITY_ID)));
	}

	@Test
	void toDocumentDataEntitiesFromMultipartWhenInputIsNull() {

		// Act
		final var result = DocumentMapper.toDocumentDataEntities(null, binaryStoreMock, MUNICIPALITY_ID);

		// Assert
		assertThat(result).isNull();
	}

	@Test
	void copyDocumentEntity() {

		// Arrange
		final var documentEntity = DocumentEntity.create()
			.withArchive(ARCHIVE)
			.withConfidentiality(ConfidentialityEmbeddable.create()
				.withConfidential(CONFIDENTIAL)
				.withLegalCitation(LEGAL_CITATION))
			.withCreated(CREATED)
			.withCreatedBy(CREATED_BY)
			.withDescription(DESCRIPTION)
			.withDocumentData(List.of(
				DocumentDataEntity.create()
					.withFileName(FILE_1_NAME)
					.withFileSizeInBytes(FILE_1_SIZE_IN_BYTES)
					.withId(ID)
					.withMimeType(MIME_TYPE_1)
					.withStorageBackend(STORAGE_BACKEND)
					.withStorageLocator(STORAGE_LOCATOR_1),
				DocumentDataEntity.create()
					.withFileName(FILE_2_NAME)
					.withFileSizeInBytes(FILE_2_SIZE_IN_BYTES)
					.withId(ID)
					.withMimeType(MIME_TYPE_2)
					.withStorageBackend(STORAGE_BACKEND)
					.withStorageLocator(STORAGE_LOCATOR_2)))
			.withId(ID)
			.withMetadata(List.of(DocumentMetadataEmbeddable.create()
				.withKey(METADATA_KEY)
				.withValue(METADATA_VALUE)))
			.withMunicipalityId(MUNICIPALITY_ID)
			.withRegistrationNumber(REGISTRATION_NUMBER)
			.withRevision(REVISION)
			.withType(DocumentTypeEntity.create()
				.withCreated(DOCUMENT_TYPE_CREATED)
				.withCreatedBy(DOCUMENT_TYPE_CREATED_BY)
				.withDisplayName(DOCUMENT_TYPE_DISPLAY_NAME)
				.withId(DOCUMENT_TYPE_ID)
				.withLastUpdated(DOCUMENT_TYPE_UPDATED)
				.withLastUpdatedBy(DOCUMENT_TYPE_UPDATED_BY)
				.withMunicipalityId(MUNICIPALITY_ID)
				.withType(DOCUMENT_TYPE))
			.withValidFrom(VALID_FROM)
			.withValidTo(VALID_TO);

		when(binaryStoreMock.copy(any(StorageRef.class))).thenAnswer(invocation -> invocation.getArgument(0));

		// Act
		final var result = DocumentMapper.copyDocumentEntity(documentEntity, binaryStoreMock);

		// Assert
		assertThat(result)
			.isNotNull()
			.isNotSameAs(documentEntity)
			.isEqualTo(DocumentEntity.create()
				.withArchive(ARCHIVE)
				.withConfidentiality(ConfidentialityEmbeddable.create()
					.withConfidential(CONFIDENTIAL)
					.withLegalCitation(LEGAL_CITATION))
				.withCreatedBy(CREATED_BY)
				.withDescription(DESCRIPTION)
				.withDocumentData(List.of(
					DocumentDataEntity.create()
						.withFileName(FILE_1_NAME)
						.withFileSizeInBytes(FILE_1_SIZE_IN_BYTES)
						.withMimeType(MIME_TYPE_1)
						.withStorageBackend(STORAGE_BACKEND)
						.withStorageLocator(STORAGE_LOCATOR_1),
					DocumentDataEntity.create()
						.withFileName(FILE_2_NAME)
						.withFileSizeInBytes(FILE_2_SIZE_IN_BYTES)
						.withMimeType(MIME_TYPE_2)
						.withStorageBackend(STORAGE_BACKEND)
						.withStorageLocator(STORAGE_LOCATOR_2)))
				.withMetadata(List.of(DocumentMetadataEmbeddable.create()
					.withKey(METADATA_KEY)
					.withValue(METADATA_VALUE)))
				.withMunicipalityId(MUNICIPALITY_ID)
				.withRegistrationNumber(REGISTRATION_NUMBER)
				.withRevision(REVISION)
				.withStatus(DocumentStatus.DRAFT)
				.withType(DocumentTypeEntity.create()
					.withCreated(DOCUMENT_TYPE_CREATED)
					.withCreatedBy(DOCUMENT_TYPE_CREATED_BY)
					.withDisplayName(DOCUMENT_TYPE_DISPLAY_NAME)
					.withId(DOCUMENT_TYPE_ID)
					.withLastUpdated(DOCUMENT_TYPE_UPDATED)
					.withLastUpdatedBy(DOCUMENT_TYPE_UPDATED_BY)
					.withMunicipalityId(MUNICIPALITY_ID)
					.withType(DOCUMENT_TYPE))
				.withValidFrom(VALID_FROM)
				.withValidTo(VALID_TO));

		verify(binaryStoreMock).copy(new StorageRef(STORAGE_BACKEND, STORAGE_LOCATOR_1));
		verify(binaryStoreMock).copy(new StorageRef(STORAGE_BACKEND, STORAGE_LOCATOR_2));
	}

	@Test
	void copyDocumentEntityWhenInputIsNull() {

		// Act
		final var result = DocumentMapper.copyDocumentEntity(null, binaryStoreMock);

		// Assert
		assertThat(result).isNull();
	}

	@Test
	void toPagedDocumentResponse(@Mock Page<DocumentEntity> pageMock) {

		// Arrange
		final var page = 1;
		final var pageSize = 20;
		final var sort = Sort.by(ASC, "property");
		final var pageable = PageRequest.of(page, pageSize, sort);

		final var documentEntity = DocumentEntity.create()
			.withConfidentiality(ConfidentialityEmbeddable.create()
				.withConfidential(CONFIDENTIAL)
				.withLegalCitation(LEGAL_CITATION))
			.withCreated(CREATED)
			.withCreatedBy(CREATED_BY)
			.withDescription(DESCRIPTION)
			.withId(ID)
			.withMetadata(List.of(DocumentMetadataEmbeddable.create()
				.withKey(METADATA_KEY)
				.withValue(METADATA_VALUE)))
			.withMunicipalityId(MUNICIPALITY_ID)
			.withRegistrationNumber(REGISTRATION_NUMBER)
			.withRevision(REVISION)
			.withType(DocumentTypeEntity.create()
				.withCreated(DOCUMENT_TYPE_CREATED)
				.withCreatedBy(DOCUMENT_TYPE_CREATED_BY)
				.withDisplayName(DOCUMENT_TYPE_DISPLAY_NAME)
				.withId(DOCUMENT_TYPE_ID)
				.withLastUpdated(DOCUMENT_TYPE_UPDATED)
				.withLastUpdatedBy(DOCUMENT_TYPE_UPDATED_BY)
				.withMunicipalityId(MUNICIPALITY_ID)
				.withType(DOCUMENT_TYPE));

		when(pageMock.getPageable()).thenReturn(pageable);
		when(pageMock.getNumberOfElements()).thenReturn(11);
		when(pageMock.getTotalElements()).thenReturn(22L);
		when(pageMock.getTotalPages()).thenReturn(33);
		when(pageMock.getContent()).thenReturn(List.of(documentEntity));

		// Act
		final var result = DocumentMapper.toPagedDocumentResponse(pageMock);

		// Assert
		assertThat(result).isNotNull();
		assertThat(result.getMetadata())
			.extracting(PagingMetaData::getPage, PagingMetaData::getLimit, PagingMetaData::getCount, PagingMetaData::getTotalRecords, PagingMetaData::getTotalPages)
			.containsExactly(page, pageSize, 11, 22L, 33);
		assertThat(result.getDocuments())
			.hasSize(1)
			.containsExactly(Document.create()
				.withConfidentiality(Confidentiality.create()
					.withConfidential(CONFIDENTIAL)
					.withLegalCitation(LEGAL_CITATION))
				.withCreated(CREATED)
				.withCreatedBy(CREATED_BY)
				.withDescription(DESCRIPTION)
				.withId(ID)
				.withMetadataList(List.of(DocumentMetadata.create()
					.withKey(METADATA_KEY)
					.withValue(METADATA_VALUE)))
				.withMunicipalityId(MUNICIPALITY_ID)
				.withRegistrationNumber(REGISTRATION_NUMBER)
				.withResponsibilities(emptyList())
				.withRevision(REVISION)
				.withType(DOCUMENT_TYPE));
	}

	@Test
	void toPagedDocumentResponseWhenInputIsNull() {

		// Act
		final var result = DocumentMapper.toPagedDocumentResponse(null);

		// Assert
		assertThat(result).isNull();
	}

	@Test
	void toInclusionFilter() {
		// Act and assert
		assertThat(DocumentMapper.toInclusionFilter(false)).containsExactly(false);
		assertThat(DocumentMapper.toInclusionFilter(true)).containsExactlyInAnyOrder(true, false);
	}
}
