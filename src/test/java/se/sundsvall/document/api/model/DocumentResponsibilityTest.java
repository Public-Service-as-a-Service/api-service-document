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

class DocumentResponsibilityTest {

	@Test
	void testBean() {
		assertThat(DocumentResponsibility.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testBuilderMethods() {

		final var principalType = PrincipalType.USER;
		final var principalId = "username123";

		final var bean = DocumentResponsibility.create()
			.withPrincipalType(principalType)
			.withPrincipalId(principalId);

		assertThat(bean).isNotNull().hasNoNullFieldsOrProperties();
		assertThat(bean.getPrincipalType()).isEqualTo(principalType);
		assertThat(bean.getPrincipalId()).isEqualTo(principalId);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(DocumentResponsibility.create()).hasAllNullFieldsOrProperties();
		assertThat(new DocumentResponsibility()).hasAllNullFieldsOrProperties();
	}
}
