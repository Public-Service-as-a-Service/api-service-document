package se.sundsvall.document.integration.db.model;

import java.time.OffsetDateTime;
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
import static java.time.ZoneId.systemDefault;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.MatcherAssert.assertThat;

class DocumentTypeEntityTest {

	@BeforeAll
	static void setup() {
		registerValueGenerator(() -> now().plusDays(new Random().nextInt()), OffsetDateTime.class);
	}

	@Test
	void testBean() {
		assertThat(DocumentTypeEntity.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testBuilderMethods() {
		final var created = now(systemDefault()).minusWeeks(1);
		final var createdBy = "b0000000-0000-0000-0000-0000000000c1";
		final var displayName = "displayName";
		final var id = "id";
		final var lastUpdated = now(systemDefault());
		final var lastUpdatedBy = "b0000000-0000-0000-0000-0000000000d1";
		final var municipalityId = "municipalityId";
		final var type = "type";

		final var bean = DocumentTypeEntity.create()
			.withCreated(created)
			.withCreatedBy(createdBy)
			.withDisplayName(displayName)
			.withId(id)
			.withLastUpdated(lastUpdated)
			.withLastUpdatedBy(lastUpdatedBy)
			.withMunicipalityId(municipalityId)
			.withType(type);

		assertThat(bean).isNotNull().hasNoNullFieldsOrProperties();
		assertThat(bean.getCreated()).isEqualTo(created);
		assertThat(bean.getCreatedBy()).isEqualTo(createdBy);
		assertThat(bean.getDisplayName()).isEqualTo(displayName);
		assertThat(bean.getId()).isEqualTo(id);
		assertThat(bean.getLastUpdated()).isEqualTo(lastUpdated);
		assertThat(bean.getLastUpdatedBy()).isEqualTo(lastUpdatedBy);
		assertThat(bean.getMunicipalityId()).isEqualTo(municipalityId);
		assertThat(bean.getType()).isEqualTo(type);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(DocumentTypeEntity.create()).hasAllNullFieldsOrProperties();
		assertThat(new DocumentTypeEntity()).hasAllNullFieldsOrProperties();
	}
}
