package se.sundsvall.document.api.model;

import org.junit.jupiter.api.Test;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.MatcherAssert.assertThat;

class FileMatchTest {

	@Test
	void testBean() {
		assertThat(FileMatch.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testBuilderMethods() {

		final var id = randomUUID().toString();
		final var fileName = "report.pdf";

		final var bean = FileMatch.create()
			.withId(id)
			.withFileName(fileName);

		assertThat(bean).isNotNull().hasNoNullFieldsOrProperties();
		assertThat(bean.getId()).isEqualTo(id);
		assertThat(bean.getFileName()).isEqualTo(fileName);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(FileMatch.create()).hasAllNullFieldsOrProperties();
		assertThat(new FileMatch()).hasAllNullFieldsOrProperties();
	}
}
