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

class DocumentUpdateRequestTest {

	@Test
	void testBean() {
		assertThat(DocumentUpdateRequest.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testBuilderMethods() {

		final var archive = true;
		final var updatedBy = "b0000000-0000-0000-0000-000000000099";
		final var description = "description";
		final var metadataList = List.of(DocumentMetadata.create());
		final var type = "type";
		final var validFrom = LocalDate.of(2026, 4, 15);
		final var validTo = LocalDate.of(2027, 4, 15);

		final var bean = DocumentUpdateRequest.create()
			.withArchive(archive)
			.withUpdatedBy(updatedBy)
			.withDescription(description)
			.withMetadataList(metadataList)
			.withType(type)
			.withValidFrom(validFrom)
			.withValidTo(validTo);

		assertThat(bean).isNotNull().hasNoNullFieldsOrProperties();
		assertThat(bean.getArchive()).isEqualTo(archive);
		assertThat(bean.getUpdatedBy()).isEqualTo(updatedBy);
		assertThat(bean.getDescription()).isEqualTo(description);
		assertThat(bean.getMetadataList()).isEqualTo(metadataList);
		assertThat(bean.getType()).isEqualTo(type);
		assertThat(bean.getValidFrom()).isEqualTo(validFrom);
		assertThat(bean.getValidTo()).isEqualTo(validTo);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(DocumentUpdateRequest.create()).hasAllNullFieldsOrProperties();
		assertThat(new DocumentUpdateRequest()).hasAllNullFieldsOrProperties();
	}
}
