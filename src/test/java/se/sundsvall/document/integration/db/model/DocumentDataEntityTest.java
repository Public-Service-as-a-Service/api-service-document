package se.sundsvall.document.integration.db.model;

import java.util.List;
import org.junit.jupiter.api.Test;
import se.sundsvall.document.service.extraction.ExtractionStatus;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.MatcherAssert.assertThat;

class DocumentDataEntityTest {

	@Test
	void testBean() {
		assertThat(DocumentDataEntity.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testBuilderMethods() {

		final var fileName = "filename.jpg";
		final var id = randomUUID().toString();
		final var mimeType = "image/jpeg";
		final var fileSizeInBytes = 100;
		final var storageLocator = randomUUID().toString();
		final var contentHash = "abc123";
		final var extractedText = "extracted text content";
		final var extractionStatus = ExtractionStatus.SUCCESS;
		final var pageCount = 3;
		final var pageOffsets = List.of(0, 1234, 5678);
		final var documentId = randomUUID().toString();

		final var bean = DocumentDataEntity.create()
			.withFileName(fileName)
			.withId(id)
			.withMimeType(mimeType)
			.withFileSizeInBytes(fileSizeInBytes)
			.withStorageLocator(storageLocator)
			.withContentHash(contentHash)
			.withExtractedText(extractedText)
			.withExtractionStatus(extractionStatus)
			.withPageCount(pageCount)
			.withPageOffsets(pageOffsets);
		bean.setDocumentId(documentId);

		assertThat(bean).isNotNull().hasNoNullFieldsOrProperties();
		assertThat(bean.getFileName()).isEqualTo(fileName);
		assertThat(bean.getId()).isEqualTo(id);
		assertThat(bean.getMimeType()).isEqualTo(mimeType);
		assertThat(bean.getFileSizeInBytes()).isEqualTo(fileSizeInBytes);
		assertThat(bean.getStorageLocator()).isEqualTo(storageLocator);
		assertThat(bean.getContentHash()).isEqualTo(contentHash);
		assertThat(bean.getExtractedText()).isEqualTo(extractedText);
		assertThat(bean.getExtractionStatus()).isEqualTo(extractionStatus);
		assertThat(bean.getPageCount()).isEqualTo(pageCount);
		assertThat(bean.getPageOffsets()).isEqualTo(pageOffsets);
		assertThat(bean.getDocumentId()).isEqualTo(documentId);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(DocumentDataEntity.create()).hasAllNullFieldsOrPropertiesExcept("fileSizeInBytes")
			.hasFieldOrPropertyWithValue("fileSizeInBytes", 0L);
		assertThat(new DocumentDataEntity()).hasAllNullFieldsOrPropertiesExcept("fileSizeInBytes")
			.hasFieldOrPropertyWithValue("fileSizeInBytes", 0L);
	}
}
