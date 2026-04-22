package se.sundsvall.document.service.mapper;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import se.sundsvall.document.api.model.DocumentFiles;
import se.sundsvall.document.integration.db.DocumentDataRepository;
import se.sundsvall.document.integration.db.model.DocumentDataEntity;
import se.sundsvall.document.service.extraction.ExtractionStatus;
import se.sundsvall.document.service.extraction.TextExtractor;
import se.sundsvall.document.service.storage.BinaryStore;
import se.sundsvall.document.service.storage.PutResult;
import se.sundsvall.document.service.storage.StorageRef;

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

@ExtendWith(MockitoExtension.class)
class DocumentDataMapperTest {

	private static final String MUNICIPALITY_ID = "2281";

	@Mock
	private BinaryStore binaryStoreMock;

	@Mock
	private TextExtractor textExtractorMock;

	@Mock
	private DocumentDataRepository documentDataRepositoryMock;

	@Test
	void toDocumentDataEntitiesFromMultipart() throws IOException {

		// Arrange
		final var newLocator = randomUUID().toString();
		final var mimeType = "image/png";
		final var file = new File("src/test/resources/files/image.png");
		final var fileName = file.getName();
		final var multipartFile = (MultipartFile) new MockMultipartFile("file", fileName, mimeType, toByteArray(new FileInputStream(file)));
		final var documents = DocumentFiles.create().withFiles(List.of(multipartFile));

		when(binaryStoreMock.put(any(InputStream.class), anyLong(), anyString(), anyMap())).thenReturn(new PutResult(StorageRef.s3(newLocator), "deadbeef"));
		when(textExtractorMock.extract(any(InputStream.class), anyString(), anyLong())).thenReturn(TextExtractor.ExtractedText.unsupported(mimeType));

		// Act
		final var result = DocumentDataMapper.toDocumentDataEntities(documents, binaryStoreMock, textExtractorMock, documentDataRepositoryMock, MUNICIPALITY_ID);

		// Assert
		assertThat(result)
			.isNotNull()
			.isNotEmpty()
			.extracting(
				DocumentDataEntity::getFileName,
				DocumentDataEntity::getMimeType,
				DocumentDataEntity::getFileSizeInBytes,
				DocumentDataEntity::getStorageLocator,
				DocumentDataEntity::getContentHash,
				DocumentDataEntity::getExtractionStatus)
			.containsExactly(tuple(
				fileName,
				mimeType,
				file.length(),
				newLocator,
				"deadbeef",
				ExtractionStatus.UNSUPPORTED));

		verify(binaryStoreMock).put(
			any(InputStream.class),
			eq(file.length()),
			eq(mimeType),
			eq(Map.of("original-filename", fileName, "municipality-id", MUNICIPALITY_ID)));
	}

	@Test
	void toDocumentDataEntitiesFromMultipartWhenInputIsNull() {

		// Act
		final var result = DocumentDataMapper.toDocumentDataEntities(null, binaryStoreMock, textExtractorMock, documentDataRepositoryMock, MUNICIPALITY_ID);

		// Assert
		assertThat(result).isNull();
	}
}
