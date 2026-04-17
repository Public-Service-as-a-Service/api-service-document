package se.sundsvall.document.integration.db.model;

import java.time.OffsetDateTime;
import java.util.Random;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import se.sundsvall.document.api.model.DocumentAccessType;

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

class DocumentAccessLogEntityTest {

	@BeforeAll
	static void setup() {
		registerValueGenerator(() -> now().plusDays(new Random().nextInt()), OffsetDateTime.class);
	}

	@Test
	void testBean() {
		assertThat(DocumentAccessLogEntity.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testBuilderMethods() {
		final var id = randomUUID().toString();
		final var accessedAt = now(systemDefault());

		final var bean = DocumentAccessLogEntity.create()
			.withId(id)
			.withMunicipalityId("2281")
			.withDocumentId("doc-1")
			.withRegistrationNumber("2023-2281-1337")
			.withRevision(2)
			.withDocumentDataId("file-1")
			.withAccessType(DocumentAccessType.DOWNLOAD)
			.withAccessedBy("user@example.com")
			.withAccessedAt(accessedAt);

		assertThat(bean).isNotNull().hasNoNullFieldsOrProperties();
		assertThat(bean.getId()).isEqualTo(id);
		assertThat(bean.getMunicipalityId()).isEqualTo("2281");
		assertThat(bean.getDocumentId()).isEqualTo("doc-1");
		assertThat(bean.getRegistrationNumber()).isEqualTo("2023-2281-1337");
		assertThat(bean.getRevision()).isEqualTo(2);
		assertThat(bean.getDocumentDataId()).isEqualTo("file-1");
		assertThat(bean.getAccessType()).isEqualTo(DocumentAccessType.DOWNLOAD);
		assertThat(bean.getAccessedBy()).isEqualTo("user@example.com");
		assertThat(bean.getAccessedAt()).isEqualTo(accessedAt);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(DocumentAccessLogEntity.create()).hasAllNullFieldsOrPropertiesExcept("revision");
		assertThat(new DocumentAccessLogEntity()).hasAllNullFieldsOrPropertiesExcept("revision");
	}
}
