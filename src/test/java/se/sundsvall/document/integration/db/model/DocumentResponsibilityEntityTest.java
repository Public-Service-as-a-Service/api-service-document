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
import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.MatcherAssert.assertThat;

class DocumentResponsibilityEntityTest {

	@BeforeAll
	static void setup() {
		registerValueGenerator(() -> now().plusDays(new Random().nextInt()), OffsetDateTime.class);
	}

	@Test
	void testBean() {
		assertThat(DocumentResponsibilityEntity.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testBuilderMethods() {

		final var id = randomUUID().toString();
		final var municipalityId = "2281";
		final var registrationNumber = "2026-2281-1";
		final var personId = "6b8d4a1c-34e2-4f73-a5f1-b7c2e9a0d8c4";
		final var createdBy = "b0000000-0000-0000-0000-0000000000c1";
		final var created = now(systemDefault());
		final var updatedBy = "b0000000-0000-0000-0000-0000000000d1";
		final var updated = now(systemDefault()).plusDays(1);

		final var bean = DocumentResponsibilityEntity.create()
			.withId(id)
			.withMunicipalityId(municipalityId)
			.withRegistrationNumber(registrationNumber)
			.withPersonId(personId)
			.withCreatedBy(createdBy)
			.withCreated(created)
			.withUpdatedBy(updatedBy)
			.withUpdated(updated);

		assertThat(bean).isNotNull().hasNoNullFieldsOrProperties();
		assertThat(bean.getId()).isEqualTo(id);
		assertThat(bean.getMunicipalityId()).isEqualTo(municipalityId);
		assertThat(bean.getRegistrationNumber()).isEqualTo(registrationNumber);
		assertThat(bean.getPersonId()).isEqualTo(personId);
		assertThat(bean.getCreatedBy()).isEqualTo(createdBy);
		assertThat(bean.getCreated()).isEqualTo(created);
		assertThat(bean.getUpdatedBy()).isEqualTo(updatedBy);
		assertThat(bean.getUpdated()).isEqualTo(updated);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(DocumentResponsibilityEntity.create()).hasAllNullFieldsOrProperties();
		assertThat(new DocumentResponsibilityEntity()).hasAllNullFieldsOrProperties();
	}
}
