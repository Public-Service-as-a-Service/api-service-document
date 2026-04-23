package se.sundsvall.document.api.model;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

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

class DocumentStatisticsTest {

	@BeforeAll
	static void setup() {
		registerValueGenerator(() -> now().plusDays(new Random().nextInt()), OffsetDateTime.class);
	}

	@Test
	void testBean() {
		assertThat(DocumentStatistics.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testBuilderMethods() {
		final var from = OffsetDateTime.parse("2026-01-01T00:00:00Z");
		final var to = OffsetDateTime.parse("2026-04-17T00:00:00Z");
		final var perRevision = List.of(RevisionStatistics.create().withRevision(1));

		final var bean = DocumentStatistics.create()
			.withMunicipalityId("2281")
			.withRegistrationNumber("2023-2281-1337")
			.withFrom(from)
			.withTo(to)
			.withTotalAccesses(42L)
			.withPerRevision(perRevision);

		assertThat(bean).isNotNull().hasNoNullFieldsOrProperties();
		assertThat(bean.getMunicipalityId()).isEqualTo("2281");
		assertThat(bean.getRegistrationNumber()).isEqualTo("2023-2281-1337");
		assertThat(bean.getFrom()).isEqualTo(from);
		assertThat(bean.getTo()).isEqualTo(to);
		assertThat(bean.getTotalAccesses()).isEqualTo(42L);
		assertThat(bean.getPerRevision()).isEqualTo(perRevision);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(DocumentStatistics.create()).hasAllNullFieldsOrPropertiesExcept("totalAccesses");
		assertThat(new DocumentStatistics()).hasAllNullFieldsOrPropertiesExcept("totalAccesses");
	}
}
