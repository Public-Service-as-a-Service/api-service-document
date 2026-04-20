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

class ConfidentialityUpdateRequestTest {

	@Test
	void testBean() {
		assertThat(ConfidentialityUpdateRequest.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testBuilderMethods() {

		final var updatedBy = "b0000000-0000-0000-0000-000000000099";
		final var confidential = true;
		final var legalCitation = "legalCitation";

		final var bean = ConfidentialityUpdateRequest.create()
			.withUpdatedBy(updatedBy)
			.withConfidential(confidential)
			.withLegalCitation(legalCitation);

		assertThat(bean).isNotNull().hasNoNullFieldsOrProperties();
		assertThat(bean.getUpdatedBy()).isEqualTo(updatedBy);
		assertThat(bean.getConfidential()).isTrue();
		assertThat(bean.getLegalCitation()).isEqualTo(legalCitation);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(ConfidentialityUpdateRequest.create()).hasAllNullFieldsOrProperties();
		assertThat(new ConfidentialityUpdateRequest()).hasAllNullFieldsOrProperties();
	}
}
