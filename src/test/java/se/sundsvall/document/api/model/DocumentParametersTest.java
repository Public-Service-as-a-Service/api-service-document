package se.sundsvall.document.api.model;

import java.util.List;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.MatcherAssert.assertThat;

class DocumentParametersTest {

	@Test
	void testBean() {
		assertThat(DocumentParameters.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testBuilderMethods() {

		var municipalityId = "2281";
		var createdBy = "User1";
		var includeConfidential = true;
		var onlyLatestRevision = true;
		var documentTypes = List.of("type1", "type2");
		var metaData = List.of(
			DocumentParameters.MetaData.create()
				.withKey("key1")
				.withMatchesAny(List.of("value1", "value2"))
				.withMatchesAll(List.of("value1", "value2")));

		final var bean = DocumentParameters.create()
			.withMunicipalityId(municipalityId)
			.withCreatedBy(createdBy)
			.withIncludeConfidential(includeConfidential)
			.withOnlyLatestRevision(onlyLatestRevision)
			.withDocumentTypes(documentTypes)
			.withMetaData(metaData);

		Assertions.assertThat(bean).isNotNull().hasNoNullFieldsOrPropertiesExcept("sortBy");
		Assertions.assertThat(bean.getMunicipalityId()).isEqualTo(municipalityId);
		Assertions.assertThat(bean.getCreatedBy()).isEqualTo(createdBy);
		Assertions.assertThat(bean.isIncludeConfidential()).isEqualTo(includeConfidential);
		Assertions.assertThat(bean.isOnlyLatestRevision()).isEqualTo(onlyLatestRevision);
		Assertions.assertThat(bean.getDocumentTypes()).isEqualTo(documentTypes);
		Assertions.assertThat(bean.getMetaData()).isEqualTo(metaData);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		Assertions.assertThat(DocumentParameters.create()).hasAllNullFieldsOrPropertiesExcept("includeConfidential", "onlyLatestRevision", "sortDirection", "page", "limit");
		Assertions.assertThat(new DocumentParameters()).hasAllNullFieldsOrPropertiesExcept("includeConfidential", "onlyLatestRevision", "sortDirection", "page", "limit");
	}
}
