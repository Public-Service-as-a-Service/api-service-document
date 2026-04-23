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

class RevisionStatisticsTest {

	@Test
	void testBean() {
		assertThat(RevisionStatistics.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testBuilderMethods() {
		final var perFile = List.of(FileStatistics.create().withDocumentDataId("doc-1"));
		final var bean = RevisionStatistics.create()
			.withRevision(2)
			.withDownloads(10L)
			.withViews(5L)
			.withPerFile(perFile);

		assertThat(bean).isNotNull().hasNoNullFieldsOrProperties();
		assertThat(bean.getRevision()).isEqualTo(2);
		assertThat(bean.getDownloads()).isEqualTo(10L);
		assertThat(bean.getViews()).isEqualTo(5L);
		assertThat(bean.getPerFile()).isEqualTo(perFile);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(RevisionStatistics.create()).hasAllNullFieldsOrPropertiesExcept("revision", "downloads", "views");
		assertThat(new RevisionStatistics()).hasAllNullFieldsOrPropertiesExcept("revision", "downloads", "views");
	}
}
