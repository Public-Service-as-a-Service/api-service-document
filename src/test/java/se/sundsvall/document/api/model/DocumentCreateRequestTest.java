package se.sundsvall.document.api.model;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.MatcherAssert.assertThat;

class DocumentCreateRequestTest {

	@Test
	void testBean() {
		assertThat(DocumentCreateRequest.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testBuilderMethods() {

		final var archive = true;
		final var confidentiality = Confidentiality.create().withConfidential(true).withLegalCitation("legalCitation");
		final var createdBy = "b0000000-0000-0000-0000-000000000099";
		final var description = "description";
		final var metadataList = List.of(DocumentMetadata.create());
		final var responsibilities = List.of(DocumentResponsibility.create().withPersonId("6b8d4a1c-34e2-4f73-a5f1-b7c2e9a0d8c4"));
		final var type = "type";
		final var validFrom = LocalDate.of(2026, 4, 15);
		final var validTo = LocalDate.of(2027, 4, 15);

		final var bean = DocumentCreateRequest.create()
			.withArchive(archive)
			.withConfidentiality(confidentiality)
			.withCreatedBy(createdBy)
			.withDescription(description)
			.withMetadataList(metadataList)
			.withResponsibilities(responsibilities)
			.withType(type)
			.withValidFrom(validFrom)
			.withValidTo(validTo);

		assertThat(bean).isNotNull().hasNoNullFieldsOrProperties();
		assertThat(bean.isArchive()).isEqualTo(archive);
		assertThat(bean.getConfidentiality()).isEqualTo(confidentiality);
		assertThat(bean.getCreatedBy()).isEqualTo(createdBy);
		assertThat(bean.getDescription()).isEqualTo(description);
		assertThat(bean.getMetadataList()).isEqualTo(metadataList);
		assertThat(bean.getResponsibilities()).isEqualTo(responsibilities);
		assertThat(bean.getType()).isEqualTo(type);
		assertThat(bean.getValidFrom()).isEqualTo(validFrom);
		assertThat(bean.getValidTo()).isEqualTo(validTo);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(DocumentCreateRequest.create()).hasAllNullFieldsOrPropertiesExcept("archive")
			.hasFieldOrPropertyWithValue("archive", false);
		assertThat(new DocumentCreateRequest()).hasAllNullFieldsOrPropertiesExcept("archive")
			.hasFieldOrPropertyWithValue("archive", false);
	}
}
