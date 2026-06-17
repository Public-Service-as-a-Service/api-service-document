package se.sundsvall.document.api.model;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import se.sundsvall.document.service.extraction.ExtractionStatus;

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
		final var highlights = Map.of("extractedText", List.of("...<em>bandwidth</em>..."));
		final var pageCount = 42;
		final var matches = List.of(Match.create().withField("extractedText").withStart(12).withEnd(21).withPage(3));
		final var extractionStatus = ExtractionStatus.SUCCESS;
		final var score = 7.42f;
		final var confidential = Boolean.FALSE;

		final var bean = FileMatch.create()
			.withId(id)
			.withFileName(fileName)
			.withHighlights(highlights)
			.withPageCount(pageCount)
			.withMatches(matches)
			.withExtractionStatus(extractionStatus)
			.withScore(score)
			.withConfidential(confidential);

		assertThat(bean).isNotNull().hasNoNullFieldsOrProperties();
		assertThat(bean.getId()).isEqualTo(id);
		assertThat(bean.getFileName()).isEqualTo(fileName);
		assertThat(bean.getHighlights()).isEqualTo(highlights);
		assertThat(bean.getPageCount()).isEqualTo(pageCount);
		assertThat(bean.getMatches()).isEqualTo(matches);
		assertThat(bean.getExtractionStatus()).isEqualTo(extractionStatus);
		assertThat(bean.getScore()).isEqualTo(score);
		assertThat(bean.getConfidential()).isEqualTo(confidential);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(FileMatch.create()).hasAllNullFieldsOrProperties();
		assertThat(new FileMatch()).hasAllNullFieldsOrProperties();
	}
}
