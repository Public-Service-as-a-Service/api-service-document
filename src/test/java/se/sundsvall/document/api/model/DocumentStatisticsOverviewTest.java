package se.sundsvall.document.api.model;

import java.time.OffsetDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import se.sundsvall.document.api.model.DocumentStatisticsOverview.ConfidentialityCounts;
import se.sundsvall.document.api.model.DocumentStatisticsOverview.DocumentTypeCount;
import se.sundsvall.document.api.model.DocumentStatisticsOverview.ExpiringSoon;
import se.sundsvall.document.api.model.DocumentStatisticsOverview.RevisionDistribution;
import se.sundsvall.document.api.model.DocumentStatisticsOverview.Scope;
import se.sundsvall.document.api.model.DocumentStatisticsOverview.YearCount;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static com.google.code.beanmatchers.BeanMatchers.registerValueGenerator;
import static java.time.OffsetDateTime.now;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.MatcherAssert.assertThat;

class DocumentStatisticsOverviewTest {

	@BeforeAll
	static void setup() {
		registerValueGenerator(() -> now().plusDays(new Random().nextInt()), OffsetDateTime.class);
	}

	@Test
	void testBean() {
		assertThat(DocumentStatisticsOverview.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testConfidentialityCountsBean() {
		assertThat(ConfidentialityCounts.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testDocumentTypeCountBean() {
		assertThat(DocumentTypeCount.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testYearCountBean() {
		assertThat(YearCount.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testRevisionDistributionBean() {
		assertThat(RevisionDistribution.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testExpiringSoonBean() {
		assertThat(ExpiringSoon.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testBuilderMethods() {
		final var generatedAt = OffsetDateTime.parse("2026-04-23T10:00:00Z");
		final Map<DocumentStatus, Long> byStatus = new EnumMap<>(DocumentStatus.class);
		byStatus.put(DocumentStatus.ACTIVE, 980L);
		final var byConfidentiality = ConfidentialityCounts.create().withConfidential(83L).withNonConfidential(1340L);
		final var byType = List.of(DocumentTypeCount.create().withType("INVOICE").withCount(540L));
		final var byYear = List.of(YearCount.create().withYear(2025).withCount(500L));
		final var revisions = RevisionDistribution.create().withSingle(980L).withTwo(300L).withThreeOrMore(143L).withMaxRevision(14);
		final var expiring = ExpiringSoon.create().withWithinDays(30).withCount(42L);

		final var bean = DocumentStatisticsOverview.create()
			.withMunicipalityId("2281")
			.withScope(Scope.USER)
			.withCreatedBy("6c3e4f5a-7b8d-4e9c-a1f2-d3e4b5c6a7f8")
			.withGeneratedAt(generatedAt)
			.withTotalDocuments(1423L)
			.withByStatus(byStatus)
			.withByConfidentiality(byConfidentiality)
			.withByDocumentType(byType)
			.withByRegistrationYear(byYear)
			.withRevisionDistribution(revisions)
			.withDocumentsWithoutFiles(7L)
			.withExpiringSoon(expiring);

		assertThat(bean).isNotNull().hasNoNullFieldsOrProperties();
		assertThat(bean.getMunicipalityId()).isEqualTo("2281");
		assertThat(bean.getScope()).isEqualTo(Scope.USER);
		assertThat(bean.getCreatedBy()).isEqualTo("6c3e4f5a-7b8d-4e9c-a1f2-d3e4b5c6a7f8");
		assertThat(bean.getGeneratedAt()).isEqualTo(generatedAt);
		assertThat(bean.getTotalDocuments()).isEqualTo(1423L);
		assertThat(bean.getByStatus()).isEqualTo(byStatus);
		assertThat(bean.getByConfidentiality()).isEqualTo(byConfidentiality);
		assertThat(bean.getByDocumentType()).isEqualTo(byType);
		assertThat(bean.getByRegistrationYear()).isEqualTo(byYear);
		assertThat(bean.getRevisionDistribution()).isEqualTo(revisions);
		assertThat(bean.getDocumentsWithoutFiles()).isEqualTo(7L);
		assertThat(bean.getExpiringSoon()).isEqualTo(expiring);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(DocumentStatisticsOverview.create()).hasAllNullFieldsOrPropertiesExcept("totalDocuments", "documentsWithoutFiles");
		assertThat(new DocumentStatisticsOverview()).hasAllNullFieldsOrPropertiesExcept("totalDocuments", "documentsWithoutFiles");
	}
}
