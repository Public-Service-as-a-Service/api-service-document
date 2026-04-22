package se.sundsvall.document.api.model;

import java.util.List;
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

class DocumentMatchTest {

	@Test
	void testBean() {
		assertThat(DocumentMatch.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testBuilderMethods() {

		final var id = randomUUID().toString();
		final var files = List.of(FileMatch.create());

		final var bean = DocumentMatch.create()
			.withId(id)
			.withFiles(files);

		assertThat(bean).isNotNull().hasNoNullFieldsOrProperties();
		assertThat(bean.getId()).isEqualTo(id);
		assertThat(bean.getFiles()).isEqualTo(files);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(DocumentMatch.create()).hasAllNullFieldsOrProperties();
		assertThat(new DocumentMatch()).hasAllNullFieldsOrProperties();
	}
}
