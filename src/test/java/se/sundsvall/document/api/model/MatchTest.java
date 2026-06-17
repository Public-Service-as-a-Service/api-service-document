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

class MatchTest {

	@Test
	void testBean() {
		assertThat(Match.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testBuilderMethods() {
		final var bean = Match.create()
			.withField("extractedText")
			.withStart(10)
			.withEnd(18)
			.withPage(3);

		assertThat(bean).isNotNull().hasNoNullFieldsOrProperties();
		assertThat(bean.getField()).isEqualTo("extractedText");
		assertThat(bean.getStart()).isEqualTo(10);
		assertThat(bean.getEnd()).isEqualTo(18);
		assertThat(bean.getPage()).isEqualTo(3);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(Match.create()).hasAllNullFieldsOrProperties();
		assertThat(new Match()).hasAllNullFieldsOrProperties();
	}
}
