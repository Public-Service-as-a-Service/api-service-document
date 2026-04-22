package se.sundsvall.document.api.model;

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

class DocumentTest {

	@BeforeAll
	static void setup() {
		registerValueGenerator(() -> now().plusDays(new Random().nextInt()), OffsetDateTime.class);
		registerValueGenerator(() -> LocalDate.now().plusDays(new Random().nextInt()), LocalDate.class);
	}

	@Test
	void testBean() {
		assertThat(Document.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testBuilderMethods() {

		final var archive = true;
		final var confidentiality = Confidentiality.create().withConfidential(true).withLegalCitation("legalCitation");
		final var created = now(systemDefault());
		final var createdBy = "b0000000-0000-0000-0000-000000000099";
		final var updatedBy = "b4444444-4444-4444-4444-444444444444";
		final var title = "title";
		final var description = "description";
		final var documentData = List.of(DocumentData.create());
		final var id = randomUUID().toString();
		final var metadataList = List.of(DocumentMetadata.create());
		final var municipalityId = "municipalityId";
		final var registrationNumber = "12345";
		final var responsibilities = List.of(DocumentResponsibility.create().withPersonId("6b8d4a1c-34e2-4f73-a5f1-b7c2e9a0d8c4"));
		final var revision = 5;
		final var type = "type";
		final var validFrom = LocalDate.of(2026, 4, 15);
		final var validTo = LocalDate.of(2027, 4, 15);
		final var status = DocumentStatus.ACTIVE;

		final var bean = Document.create()
			.withArchive(archive)
			.withConfidentiality(confidentiality)
			.withCreated(created)
			.withCreatedBy(createdBy)
			.withUpdatedBy(updatedBy)
			.withTitle(title)
			.withDescription(description)
			.withDocumentData(documentData)
			.withId(id)
			.withMetadataList(metadataList)
			.withMunicipalityId(municipalityId)
			.withRegistrationNumber(registrationNumber)
			.withResponsibilities(responsibilities)
			.withRevision(revision)
			.withStatus(status)
			.withType(type)
			.withValidFrom(validFrom)
			.withValidTo(validTo);

		assertThat(bean).isNotNull().hasNoNullFieldsOrProperties();
		assertThat(bean.isArchive()).isEqualTo(archive);
		assertThat(bean.getConfidentiality()).isEqualTo(confidentiality);
		assertThat(bean.getCreated()).isEqualTo(created);
		assertThat(bean.getCreatedBy()).isEqualTo(createdBy);
		assertThat(bean.getUpdatedBy()).isEqualTo(updatedBy);
		assertThat(bean.getTitle()).isEqualTo(title);
		assertThat(bean.getDescription()).isEqualTo(description);
		assertThat(bean.getDocumentData()).isEqualTo(documentData);
		assertThat(bean.getId()).isEqualTo(id);
		assertThat(bean.getMetadataList()).isEqualTo(metadataList);
		assertThat(bean.getMunicipalityId()).isEqualTo(municipalityId);
		assertThat(bean.getRegistrationNumber()).isEqualTo(registrationNumber);
		assertThat(bean.getResponsibilities()).isEqualTo(responsibilities);
		assertThat(bean.getRevision()).isEqualTo(revision);
		assertThat(bean.getStatus()).isEqualTo(status);
		assertThat(bean.getType()).isEqualTo(type);
		assertThat(bean.getValidFrom()).isEqualTo(validFrom);
		assertThat(bean.getValidTo()).isEqualTo(validTo);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(Document.create()).hasAllNullFieldsOrPropertiesExcept("revision", "archive")
			.hasFieldOrPropertyWithValue("revision", 0)
			.hasFieldOrPropertyWithValue("archive", false);
		assertThat(new Document()).hasAllNullFieldsOrPropertiesExcept("revision", "archive")
			.hasFieldOrPropertyWithValue("revision", 0)
			.hasFieldOrPropertyWithValue("archive", false);
	}
}
