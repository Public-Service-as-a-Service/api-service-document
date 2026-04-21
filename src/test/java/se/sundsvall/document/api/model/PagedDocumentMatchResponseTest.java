package se.sundsvall.document.api.model;

import java.util.List;
import org.junit.jupiter.api.Test;
import se.sundsvall.dept44.models.api.paging.PagingMetaData;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.MatcherAssert.assertThat;

class PagedDocumentMatchResponseTest {

	@Test
	void testBean() {
		assertThat(PagedDocumentMatchResponse.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testBuilderMethods() {

		final var documents = List.of(DocumentMatch.create());
		final var pagingMetadata = PagingMetaData.create();

		final var bean = PagedDocumentMatchResponse.create()
			.withDocuments(documents)
			.withMetaData(pagingMetadata);

		assertThat(bean).isNotNull().hasNoNullFieldsOrProperties();
		assertThat(bean.getDocuments()).isEqualTo(documents);
		assertThat(bean.getMetadata()).isEqualTo(pagingMetadata);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(PagedDocumentMatchResponse.create()).hasAllNullFieldsOrProperties();
		assertThat(new PagedDocumentMatchResponse()).hasAllNullFieldsOrProperties();
	}
}
