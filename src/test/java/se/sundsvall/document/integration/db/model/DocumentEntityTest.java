package se.sundsvall.document.integration.db.model;

import java.time.LocalDate;
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
import static java.time.ZoneId.systemDefault;
import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.MatcherAssert.assertThat;

class DocumentEntityTest {

	@BeforeAll
	static void setup() {
		registerValueGenerator(() -> now().plusDays(new Random().nextInt()), OffsetDateTime.class);
		registerValueGenerator(() -> LocalDate.now().plusDays(new Random().nextInt()), LocalDate.class);
	}

	@Test
	void testBean() {
		assertThat(DocumentEntity.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testBuilderMethods() {

		final var archive = true;
		final var confidentiality = ConfidentialityEmbeddable.create().withConfidential(true).withLegalCitation("legalCitation");
		final var created = now(systemDefault());
		final var createdBy = "user";
		final var updatedBy = "updater";
		final var description = "description";
		final var documentDatas = List.of(DocumentDataEntity.create());
		final var id = randomUUID().toString();
		final var metadata = List.of(DocumentMetadataEmbeddable.create());
		final var municipalityId = "municipalityId";
		final var registrationNumber = "12345";
		final var revision = 5;
		final var type = DocumentTypeEntity.create();
		final var validFrom = LocalDate.of(2026, 4, 15);
		final var validTo = LocalDate.of(2027, 4, 15);

		final var bean = DocumentEntity.create()
			.withArchive(archive)
			.withConfidentiality(confidentiality)
			.withCreated(created)
			.withCreatedBy(createdBy)
			.withUpdatedBy(updatedBy)
			.withDescription(description)
			.withDocumentData(documentDatas)
			.withId(id)
			.withMetadata(metadata)
			.withMunicipalityId(municipalityId)
			.withRegistrationNumber(registrationNumber)
			.withRevision(revision)
			.withType(type)
			.withValidFrom(validFrom)
			.withValidTo(validTo);

		assertThat(bean).isNotNull().hasNoNullFieldsOrProperties();
		assertThat(bean.isArchive()).isEqualTo(archive);
		assertThat(bean.getConfidentiality()).isEqualTo(confidentiality);
		assertThat(bean.getCreated()).isEqualTo(created);
		assertThat(bean.getCreatedBy()).isEqualTo(createdBy);
		assertThat(bean.getUpdatedBy()).isEqualTo(updatedBy);
		assertThat(bean.getDescription()).isEqualTo(description);
		assertThat(bean.getDocumentData()).isEqualTo(documentDatas);
		assertThat(bean.getId()).isEqualTo(id);
		assertThat(bean.getMetadata()).isEqualTo(metadata);
		assertThat(bean.getMunicipalityId()).isEqualTo(municipalityId);
		assertThat(bean.getRegistrationNumber()).isEqualTo(registrationNumber);
		assertThat(bean.getRevision()).isEqualTo(revision);
		assertThat(bean.getType()).isEqualTo(type);
		assertThat(bean.getValidFrom()).isEqualTo(validFrom);
		assertThat(bean.getValidTo()).isEqualTo(validTo);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(DocumentEntity.create()).hasAllNullFieldsOrPropertiesExcept("revision", "archive")
			.hasFieldOrPropertyWithValue("revision", 0)
			.hasFieldOrPropertyWithValue("archive", false);
		assertThat(new DocumentEntity()).hasAllNullFieldsOrPropertiesExcept("revision", "archive")
			.hasFieldOrPropertyWithValue("revision", 0)
			.hasFieldOrPropertyWithValue("archive", false);
	}
}
