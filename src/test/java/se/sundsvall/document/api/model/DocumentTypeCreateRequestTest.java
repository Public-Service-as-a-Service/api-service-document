package se.sundsvall.document.api.model;

import org.junit.jupiter.api.Test;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.MatcherAssert.assertThat;

class DocumentTypeCreateRequestTest {

	@Test
	void testBean() {
		assertThat(DocumentTypeCreateRequest.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testBuilderMethods() {
		final var createdBy = "b0000000-0000-0000-0000-0000000000c1";
		final var displayName = "displayName";
		final var type = "type";

		final var bean = DocumentTypeCreateRequest.create()
			.withCreatedBy(createdBy)
			.withDisplayName(displayName)
			.withType(type);

		assertThat(bean).hasNoNullFieldsOrProperties();
		assertThat(bean.getCreatedBy()).isEqualTo(createdBy);
		assertThat(bean.getDisplayName()).isEqualTo(displayName);
		assertThat(bean.getType()).isEqualTo(type);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(DocumentTypeCreateRequest.create()).hasAllNullFieldsOrProperties();
		assertThat(new DocumentTypeCreateRequest()).hasAllNullFieldsOrProperties();
	}
}
