package se.sundsvall.document.api.model;

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

class DocumentResponsibilitiesUpdateRequestTest {

	@Test
	void testBean() {
		assertThat(DocumentResponsibilitiesUpdateRequest.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testBuilderMethods() {

		final var changedBy = "username123";
		final var responsibilities = List.of(DocumentResponsibility.create().withPersonId("6b8d4a1c-34e2-4f73-a5f1-b7c2e9a0d8c4"));

		final var bean = DocumentResponsibilitiesUpdateRequest.create()
			.withChangedBy(changedBy)
			.withResponsibilities(responsibilities);

		assertThat(bean).isNotNull().hasNoNullFieldsOrProperties();
		assertThat(bean.getChangedBy()).isEqualTo(changedBy);
		assertThat(bean.getResponsibilities()).isEqualTo(responsibilities);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(DocumentResponsibilitiesUpdateRequest.create()).hasAllNullFieldsOrProperties();
		assertThat(new DocumentResponsibilitiesUpdateRequest()).hasAllNullFieldsOrProperties();
	}
}
