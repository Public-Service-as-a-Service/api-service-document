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

class FileStatisticsTest {

	@Test
	void testBean() {
		assertThat(FileStatistics.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testBuilderMethods() {
		final var bean = FileStatistics.create()
			.withDocumentDataId("doc-1")
			.withFileName("rapport.pdf")
			.withDownloads(20L)
			.withViews(35L);

		assertThat(bean).isNotNull().hasNoNullFieldsOrProperties();
		assertThat(bean.getDocumentDataId()).isEqualTo("doc-1");
		assertThat(bean.getFileName()).isEqualTo("rapport.pdf");
		assertThat(bean.getDownloads()).isEqualTo(20L);
		assertThat(bean.getViews()).isEqualTo(35L);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(FileStatistics.create()).hasAllNullFieldsOrPropertiesExcept("downloads", "views");
		assertThat(new FileStatistics()).hasAllNullFieldsOrPropertiesExcept("downloads", "views");
	}
}
