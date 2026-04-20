package se.sundsvall.document.integration.db;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.document.api.model.DocumentParameters;
import se.sundsvall.document.api.model.DocumentResponsibility;
import se.sundsvall.document.api.model.DocumentStatus;
import se.sundsvall.document.integration.db.model.ConfidentialityEmbeddable;
import se.sundsvall.document.integration.db.model.DocumentDataEntity;
import se.sundsvall.document.integration.db.model.DocumentEntity;
import se.sundsvall.document.integration.db.model.DocumentMetadataEmbeddable;
import se.sundsvall.document.service.InclusionFilter;

import static java.time.OffsetDateTime.now;
import static java.time.temporal.ChronoUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.assertj.core.api.Assertions.within;
import static org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace.NONE;
import static org.springframework.data.domain.Sort.Direction.ASC;
import static org.springframework.data.domain.Sort.Direction.DESC;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static se.sundsvall.document.service.InclusionFilter.PUBLIC;

/**
 * DocumentRepository tests.
 *
 * @see /src/test/resources/db/testdata-junit.sql for data setup.
 */
@DataJpaTest
@Transactional
@AutoConfigureTestDatabase(replace = NONE)
@ActiveProfiles("junit")
@Sql(scripts = {
	"/db/scripts/truncate.sql",
	"/db/scripts/testdata-junit.sql"
})
class DocumentRepositoryTest {

	private static final String CREATED_BY = "User123";
	private static final String MUNICIPALITY_ID = "2281";
	private static final String DOCUMENT_TYPE = "HOLIDAY_EXCHANGE";

	@Autowired
	private DocumentRepository documentRepository;

	@Autowired
	private DocumentTypeRepository documentTypeRepository;

	@Test
	void create() {
		// Arrange
		final var registrationNumber = "2023-1337";
		final var entity = createDocumentEntity(registrationNumber);

		// Act
		final var result = documentRepository.save(entity);

		// Assert
		assertThat(result).isNotNull();
		assertThat(isValidUUID(result.getId())).isTrue();
		assertThat(result.getCreated()).isCloseTo(now(), within(2, SECONDS));
		assertThat(result.getRegistrationNumber()).isEqualTo(registrationNumber);
		assertThat(result.getCreatedBy()).isEqualTo(CREATED_BY);
		assertThat(result.getType()).isNotNull().satisfies(documentType -> {
			assertThat(documentType.getType()).isEqualTo(DOCUMENT_TYPE);
		});
		assertThat(result.getMetadata())
			.extracting(DocumentMetadataEmbeddable::getKey, DocumentMetadataEmbeddable::getValue)
			.containsExactly(
				tuple("key1", "value1"),
				tuple("key2", "value2"));
	}

	@Test
	void createWithMultipleDocumentData() {

		// Arrange
		final var registrationNumber = "2023-1338";
		final var entity = createDocumentEntity(registrationNumber);

		entity.setDocumentData(List.of(createDocumentDataEntity("file1.txt"), createDocumentDataEntity("file2.txt")));

		// Act
		final var result = documentRepository.save(entity);

		// Assert
		assertThat(result).isNotNull();
		assertThat(isValidUUID(result.getId())).isTrue();
		assertThat(result.getCreated()).isCloseTo(now(), within(2, SECONDS));
		assertThat(result.getRegistrationNumber()).isEqualTo(registrationNumber);
		assertThat(result.getCreatedBy()).isEqualTo(CREATED_BY);
		assertThat(result.getType()).isNotNull().satisfies(documentType -> {
			assertThat(documentType.getType()).isEqualTo(DOCUMENT_TYPE);
		});
		assertThat(result.getMetadata())
			.extracting(DocumentMetadataEmbeddable::getKey, DocumentMetadataEmbeddable::getValue)
			.containsExactly(
				tuple("key1", "value1"),
				tuple("key2", "value2"));
		assertThat(result.getDocumentData()).extracting(DocumentDataEntity::getFileName)
			.containsExactly("file1.txt", "file2.txt");
	}

	@Test
	void update() {

		// Arrange
		final var entity = documentRepository.findById("159c10bf-1b32-471b-b2d3-c4b4b13ea152").orElseThrow();
		assertThat(entity).isNotNull();
		assertThat(entity.getCreated()).isEqualTo(OffsetDateTime.parse("2023-06-28T12:01:00.000+02:00"));
		assertThat(entity.getRegistrationNumber()).isEqualTo("2023-2281-123");
		assertThat(entity.getCreatedBy()).isEqualTo("User1");
		assertThat(entity.getType()).isNotNull().satisfies(documentType -> {
			assertThat(documentType.getType()).isEqualTo(DOCUMENT_TYPE);
		});
		assertThat(entity.getMetadata())
			.extracting(DocumentMetadataEmbeddable::getKey, DocumentMetadataEmbeddable::getValue)
			.containsExactly(tuple("document1-key1", "value-1"));

		// Act
		entity.withMetadata(new ArrayList<>(List.of(DocumentMetadataEmbeddable.create().withKey("UpdatedKey").withValue("UpdatedValue"))));
		final var result = documentRepository.save(entity);

		// Assert
		assertThat(result).isNotNull();
		assertThat(isValidUUID(result.getId())).isTrue();
		assertThat(entity.getCreated()).isEqualTo(OffsetDateTime.parse("2023-06-28T12:01:00.000+02:00"));
		assertThat(result.getRegistrationNumber()).isEqualTo("2023-2281-123");
		assertThat(result.getCreatedBy()).isEqualTo("User1");
		assertThat(result.getMetadata())
			.extracting(DocumentMetadataEmbeddable::getKey, DocumentMetadataEmbeddable::getValue)
			.containsExactly(tuple("UpdatedKey", "UpdatedValue"));
	}

	@ParameterizedTest
	@MethodSource("publicConfidentialTestsArgumentsProvider")
	void findTopByRegistrationNumberOrderByRevisionDesc(String registrationNumber, InclusionFilter filter, boolean shouldHaveMatch) {

		// Act
		final var result = documentRepository.findTopByMunicipalityIdAndRegistrationNumberAndConfidentialityConfidentialInOrderByRevisionDesc(MUNICIPALITY_ID, registrationNumber, filter.getValue());

		// Assert
		if (shouldHaveMatch) {
			assertThat(result).isPresent();
		} else {
			assertThat(result).isNotPresent();
		}
	}

	@Test
	void findTopByRegistrationNumberOrderByRevisionDesc() {

		// Arrange
		final var registrationNumber = "2023-2281-123"; // Document 1 (public)

		// Act
		final var result = documentRepository.findTopByMunicipalityIdAndRegistrationNumberAndConfidentialityConfidentialInOrderByRevisionDesc(MUNICIPALITY_ID, registrationNumber, PUBLIC.getValue()).orElseThrow();

		// Assert
		assertThat(result).isNotNull();
		assertThat(result.getId()).isEqualTo("612dc8d0-e6b7-426c-abcc-c9b49ae1e7e2");
		assertThat(result.getRevision()).isEqualTo(3);
		assertThat(result.getCreated()).isEqualTo(OffsetDateTime.parse("2023-06-28T12:03:00.000+02:00"));
		assertThat(result.getRegistrationNumber()).isEqualTo(registrationNumber);
		assertThat(result.getCreatedBy()).isEqualTo("User3");
		assertThat(result.getMetadata())
			.extracting(DocumentMetadataEmbeddable::getKey, DocumentMetadataEmbeddable::getValue)
			.containsExactly(
				tuple("document1-key1", "value-1"),
				tuple("document1-key2", "value-2"),
				tuple("document1-key3", "value-3"),
				tuple("document1-key4", "value-4"));
	}

	@Test
	void findByRegistrationNumberAndConfidentialInReturningList() {

		// Arrange
		final var registrationNumber = "2023-2281-123"; // Document 1 (public)

		// Act
		final var result = documentRepository.findByMunicipalityIdAndRegistrationNumberAndConfidentialityConfidentialIn(MUNICIPALITY_ID, registrationNumber, PUBLIC.getValue());

		// Assert
		assertThat(result)
			.isNotNull()
			.extracting(DocumentEntity::getRegistrationNumber, DocumentEntity::getId, DocumentEntity::getRevision, DocumentEntity::getCreatedBy)
			.containsExactlyInAnyOrder(
				tuple("2023-2281-123", "159c10bf-1b32-471b-b2d3-c4b4b13ea152", 1, "User1"),
				tuple("2023-2281-123", "8efd63a3-b525-4581-8b0b-9759f381a5a5", 2, "User2"),
				tuple("2023-2281-123", "612dc8d0-e6b7-426c-abcc-c9b49ae1e7e2", 3, "User3"));
	}

	@ParameterizedTest
	@MethodSource("publicConfidentialTestsArgumentsProvider")
	void findByRegistrationNumberAndConfidentialInReturningPage(String registrationNumber, InclusionFilter filter, boolean shouldHaveMatch) {

		// Arrange
		final var pageRequest = PageRequest.of(0, 10, Sort.by(DESC, "revision"));

		// Act
		final var result = documentRepository.findByMunicipalityIdAndRegistrationNumberAndConfidentialityConfidentialIn(MUNICIPALITY_ID, registrationNumber, filter.getValue(), pageRequest);

		// Assert
		if (shouldHaveMatch) {
			assertThat(result).isNotEmpty();
		} else {
			assertThat(result).isNullOrEmpty();
		}
	}

	@Test
	void findByRegistrationNumberAndConfidentialInReturningPage() {

		// Arrange
		final var registrationNumber = "2023-2281-123";
		final var pageRequest = PageRequest.of(0, 10, Sort.by(DESC, "revision"));

		// Act
		final var result = documentRepository.findByMunicipalityIdAndRegistrationNumberAndConfidentialityConfidentialIn(MUNICIPALITY_ID, registrationNumber, PUBLIC.getValue(), pageRequest);

		// Assert
		assertThat(result)
			.hasSize(3)
			.extracting(DocumentEntity::getId, DocumentEntity::getRevision, DocumentEntity::getRegistrationNumber, DocumentEntity::getCreatedBy)
			.containsExactly(
				tuple("612dc8d0-e6b7-426c-abcc-c9b49ae1e7e2", 3, "2023-2281-123", "User3"),
				tuple("8efd63a3-b525-4581-8b0b-9759f381a5a5", 2, "2023-2281-123", "User2"),
				tuple("159c10bf-1b32-471b-b2d3-c4b4b13ea152", 1, "2023-2281-123", "User1"));
	}

	@ParameterizedTest
	@MethodSource("publicConfidentialTestsArgumentsProvider")
	void findByRegistrationNumberAndConfidentialInReturningPageReversedOrder(String registrationNumber, InclusionFilter filter, boolean shouldHaveMatch) {
		// Arrange
		final var pageRequest = PageRequest.of(0, 10, Sort.by(ASC, "revision"));

		// Act
		final var result = documentRepository.findByMunicipalityIdAndRegistrationNumberAndConfidentialityConfidentialIn(MUNICIPALITY_ID, registrationNumber, filter.getValue(), pageRequest);

		// Assert
		if (shouldHaveMatch) {
			assertThat(result).isNotEmpty();
		} else {
			assertThat(result).isNullOrEmpty();
		}
	}

	@Test
	void findByRegistrationNumberAndConfidentialInReturningPageReversedOrder() {

		// Arrange
		final var registrationNumber = "2023-2281-123";
		final var pageRequest = PageRequest.of(0, 10, Sort.by(ASC, "revision"));

		// Act
		final var result = documentRepository.findByMunicipalityIdAndRegistrationNumberAndConfidentialityConfidentialIn(MUNICIPALITY_ID, registrationNumber, PUBLIC.getValue(), pageRequest);

		// Assert
		assertThat(result)
			.hasSize(3)
			.extracting(DocumentEntity::getId, DocumentEntity::getRevision, DocumentEntity::getRegistrationNumber, DocumentEntity::getCreatedBy)
			.containsExactly(
				tuple("159c10bf-1b32-471b-b2d3-c4b4b13ea152", 1, "2023-2281-123", "User1"),
				tuple("8efd63a3-b525-4581-8b0b-9759f381a5a5", 2, "2023-2281-123", "User2"),
				tuple("612dc8d0-e6b7-426c-abcc-c9b49ae1e7e2", 3, "2023-2281-123", "User3"));
	}

	@ParameterizedTest
	@MethodSource("publicConfidentialTestsArgumentsProvider")
	void findByRegistrationNumberAndRevisionAndConfidentialIn(String registrationNumber, InclusionFilter filter, boolean shouldHaveMatch) {
		final var revision = 1;

		// Act
		final var result = documentRepository.findByMunicipalityIdAndRegistrationNumberAndRevisionAndConfidentialityConfidentialIn(MUNICIPALITY_ID, registrationNumber, revision, filter.getValue());

		// Assert
		if (shouldHaveMatch) {
			assertThat(result).isPresent();
		} else {
			assertThat(result).isNotPresent();
		}
	}

	@Test
	void findByRegistrationNumberAndRevisionAndConfidentialIn() {

		// Arrange
		final var registrationNumber = "2023-2281-123";
		final var revision = 2;

		// Act
		final var result = documentRepository.findByMunicipalityIdAndRegistrationNumberAndRevisionAndConfidentialityConfidentialIn(MUNICIPALITY_ID, registrationNumber, revision, PUBLIC.getValue()).orElseThrow();

		// Assert
		assertThat(result)
			.isNotNull()
			.extracting(DocumentEntity::getId, DocumentEntity::getRevision, DocumentEntity::getRegistrationNumber, DocumentEntity::getCreatedBy)
			.containsExactly("8efd63a3-b525-4581-8b0b-9759f381a5a5", 2, "2023-2281-123", "User2");
	}

	@Test
	void searchByKeyWithConfidentialIncluded() {

		// Arrange
		final var search = "*key3";
		final var pageRequest = PageRequest.of(0, 10, Sort.by(ASC, "created"));

		// Act
		final var result = documentRepository.search(search, true, false, pageRequest, MUNICIPALITY_ID, List.of(DocumentStatus.SCHEDULED, DocumentStatus.ACTIVE, DocumentStatus.EXPIRED));

		// Assert
		assertThat(result).isNotNull();
		assertThat(result.getContent())
			.extracting(DocumentEntity::getId, DocumentEntity::getRevision, DocumentEntity::getRegistrationNumber, DocumentEntity::getCreatedBy)
			.containsExactly(
				tuple("03d33a6a-bc8c-410c-95f6-2c890822967d", 1, "2024-2281-999", "User4"),
				tuple("612dc8d0-e6b7-426c-abcc-c9b49ae1e7e2", 3, "2023-2281-123", "User3"));
	}

	@Test
	void searchByKeyWithConfidentialExcluded() {

		// Arrange
		final var search = "*key3";
		final var pageRequest = PageRequest.of(0, 10, Sort.by(ASC, "created"));

		// Act
		final var result = documentRepository.search(search, false, false, pageRequest, MUNICIPALITY_ID, List.of(DocumentStatus.SCHEDULED, DocumentStatus.ACTIVE, DocumentStatus.EXPIRED));

		// Assert
		assertThat(result).isNotNull();
		assertThat(result.getContent())
			.extracting(DocumentEntity::getId, DocumentEntity::getRevision, DocumentEntity::getRegistrationNumber, DocumentEntity::getCreatedBy)
			.containsExactly(
				tuple("612dc8d0-e6b7-426c-abcc-c9b49ae1e7e2", 3, "2023-2281-123", "User3"));
	}

	@Test
	void searchByKeyInAllRevisions() {

		// Arrange
		final var search = "document1-key1";
		final var pageRequest = PageRequest.of(0, 10, Sort.by(ASC, "revision"));

		// Act
		final var result = documentRepository.search(search, false, false, pageRequest, MUNICIPALITY_ID, List.of(DocumentStatus.SCHEDULED, DocumentStatus.ACTIVE, DocumentStatus.EXPIRED));

		// Assert
		assertThat(result).isNotNull();
		assertThat(result.getContent())
			.extracting(DocumentEntity::getId, DocumentEntity::getRevision, DocumentEntity::getRegistrationNumber, DocumentEntity::getCreatedBy)
			.containsExactly(
				tuple("159c10bf-1b32-471b-b2d3-c4b4b13ea152", 1, "2023-2281-123", "User1"),
				tuple("8efd63a3-b525-4581-8b0b-9759f381a5a5", 2, "2023-2281-123", "User2"),
				tuple("612dc8d0-e6b7-426c-abcc-c9b49ae1e7e2", 3, "2023-2281-123", "User3"));
	}

	@Test
	void searchByKeyInLatestRevision() {

		// Arrange
		final var search = "document1-key1";
		final var pageRequest = PageRequest.of(0, 10, Sort.by(ASC, "revision"));

		// Act
		final var result = documentRepository.search(search, false, true, pageRequest, MUNICIPALITY_ID, List.of(DocumentStatus.SCHEDULED, DocumentStatus.ACTIVE, DocumentStatus.EXPIRED));

		// Assert
		assertThat(result).isNotNull();
		assertThat(result.getContent())
			.extracting(DocumentEntity::getId, DocumentEntity::getRevision, DocumentEntity::getRegistrationNumber, DocumentEntity::getCreatedBy)
			.containsExactly(
				tuple("612dc8d0-e6b7-426c-abcc-c9b49ae1e7e2", 3, "2023-2281-123", "User3"));
	}

	@Test
	void searchByFilename() {

		// Arrange
		final var search = "file*";
		final var pageRequest = PageRequest.of(0, 10, Sort.by(ASC, "registrationNumber", "revision"));

		// Act
		final var result = documentRepository.search(search, true, false, pageRequest, MUNICIPALITY_ID, List.of(DocumentStatus.SCHEDULED, DocumentStatus.ACTIVE, DocumentStatus.EXPIRED));

		// Assert
		assertThat(result).isNotNull();
		assertThat(result.getContent())
			.extracting(DocumentEntity::getId, DocumentEntity::getRevision, DocumentEntity::getRegistrationNumber, DocumentEntity::getCreatedBy)
			.containsExactly(

				tuple("159c10bf-1b32-471b-b2d3-c4b4b13ea152", 1, "2023-2281-123", "User1"),
				tuple("8efd63a3-b525-4581-8b0b-9759f381a5a5", 2, "2023-2281-123", "User2"),
				tuple("612dc8d0-e6b7-426c-abcc-c9b49ae1e7e2", 3, "2023-2281-123", "User3"),
				tuple("1901694b-8e3a-46b7-83ea-cd351ccc0f52", 1, "2024-2281-601", "User5"),
				tuple("2901694b-8e3a-46b7-83ea-cd351ccc0f52", 1, "2024-2281-602", "User5"),
				tuple("3901694b-8e3a-46b7-83ea-cd351ccc0f52", 1, "2024-2281-603", "User5"),
				tuple("8901694b-8e3a-46b7-83ea-cd351ccc0f52", 1, "2024-2281-666", "User5"),
				tuple("03d33a6a-bc8c-410c-95f6-2c890822967d", 1, "2024-2281-999", "User4"));
	}

	@Test
	void searchNoMatches() {

		// Arrange
		final var search = "this-string-does-not-exists";
		final var pageRequest = PageRequest.of(0, 10, Sort.by(ASC, "created"));

		// Act
		final var result = documentRepository.search(search, true, false, pageRequest, MUNICIPALITY_ID, List.of(DocumentStatus.SCHEDULED, DocumentStatus.ACTIVE, DocumentStatus.EXPIRED));

		// Assert
		assertThat(result).isNotNull();
		assertThat(result.getContent()).isEmpty();
	}

	private static Stream<Arguments> searchByParametersArgumentsProvider() {
		return Stream.of(
			Arguments.of("2281", true, true, List.of("EMPLOYEE_CERTIFICATE"), List.of(new DocumentParameters.MetaData().withKey("EMPLOYEE_TYPE").withMatchesAny(List.of("Vikarie"))), 2, null),
			Arguments.of("2281", true, true, List.of("EMPLOYEE_CERTIFICATE"), List.of(new DocumentParameters.MetaData().withKey("EMPLOYEE_TYPE").withMatchesAny(List.of("Vaktmästare"))), 1, null),
			Arguments.of("2281", true, true, List.of("EMPLOYEE_CERTIFICATE"), List.of(new DocumentParameters.MetaData().withKey("EMPLOYEE_TYPE")), 3, null),
			Arguments.of("2281", true, true, List.of("EMPLOYEE_CERTIFICATE"), null, 5, null),
			Arguments.of("2281", true, true, null, null, 6, null),
			Arguments.of("2281", true, false, List.of("HOLIDAY_EXCHANGE"), null, 3, null),
			Arguments.of("2281", true, true, null, List.of(new DocumentParameters.MetaData().withMatchesAny(List.of("Vikarie"))), 2, null),
			Arguments.of("2281", false, true, List.of("EMPLOYEE_CERTIFICATE"), List.of(new DocumentParameters.MetaData().withKey("EMPLOYEE_TYPE").withMatchesAll(List.of("Vikarie", "Lärare"))), 0, null),
			Arguments.of("2281", false, false, List.of("HOLIDAY_EXCHANGE"), List.of(new DocumentParameters.MetaData().withKey("document1-key1").withMatchesAll(List.of("value-1"))), 3, null),
			Arguments.of("2281", true, true, null, List.of(new DocumentParameters.MetaData().withMatchesAny(List.of("Vaktmästare", "Vikarie"))), 3, null),
			Arguments.of("2281", true, false, List.of("EMPLOYEE_CERTIFICATE"), List.of(new DocumentParameters.MetaData().withMatchesAll(List.of("Sidsjö skola", "Vikarie"))), 1, null),
			Arguments.of("2281", true, true, List.of("EMPLOYEE_CERTIFICATE"), List.of(new DocumentParameters.MetaData().withKey("EMPLOYEE_TYPE")), 3, null),
			Arguments.of("2281", true, true, List.of("EMPLOYEE_CERTIFICATE"), List.of(
				new DocumentParameters.MetaData().withKey("EMPLOYEE_TYPE").withMatchesAny(List.of("Vaktmästare")),
				new DocumentParameters.MetaData().withKey("EMPLOYEE_UNIT").withMatchesAll(List.of("Sidsjö skola"))), 1, null),
			Arguments.of("2281", false, false, List.of("HOLIDAY_EXCHANGE"), null, 3, null),
			Arguments.of(null, true, true, List.of("EMPLOYEE_CERTIFICATE"), List.of(new DocumentParameters.MetaData().withKey("EMPLOYEE_TYPE").withMatchesAny(List.of("Vikarie"))), 0, null),
			Arguments.of("2281", true, true, List.of("EMPLOYEE_CERTIFICATE"), List.of(), 5, null),
			Arguments.of("2281", true, true, null, List.of(new DocumentParameters.MetaData().withKey("document1-key1").withMatchesAny(List.of("value-1"))), 1, null),
			// createdBy filter tests
			Arguments.of("2281", true, false, null, null, 4, "User5"),
			Arguments.of("2281", false, false, null, null, 1, "User1"),
			Arguments.of("2281", true, true, null, null, 0, "User1"),
			Arguments.of("2281", true, true, null, null, 0, "NonExistentUser"));
	}

	@ParameterizedTest
	@MethodSource("searchByParametersArgumentsProvider")
	void searchByParameters(String municipalityId, boolean includeConfidential, boolean onlyLatestRevision, List<String> documentTypes, List<DocumentParameters.MetaData> metaData, int expectedSize, String createdBy) {
		var parameters = new DocumentParameters()
			.withMunicipalityId(municipalityId)
			.withCreatedBy(createdBy)
			.withIncludeConfidential(includeConfidential)
			.withOnlyLatestRevision(onlyLatestRevision)
			.withDocumentTypes(documentTypes)
			.withMetaData(metaData);
		var pageable = PageRequest.of(parameters.getPage() - 1, parameters.getLimit(), parameters.sort());

		var result = documentRepository.searchByParameters(parameters, pageable, List.of(DocumentStatus.SCHEDULED, DocumentStatus.ACTIVE, DocumentStatus.EXPIRED));

		assertThat(result).isNotNull();
		assertThat(result.getContent()).hasSize(expectedSize);
		assertThat(result.getSize()).isEqualTo(parameters.getLimit());
		assertThat(result.getTotalElements()).isEqualTo(expectedSize);
	}

	@Test
	void searchByParametersWithCreatedBy() {
		var parameters = new DocumentParameters()
			.withMunicipalityId(MUNICIPALITY_ID)
			.withCreatedBy("user5")
			.withIncludeConfidential(true);
		var pageable = PageRequest.of(0, 10, Sort.by(ASC, "registrationNumber"));

		var result = documentRepository.searchByParameters(parameters, pageable, List.of(DocumentStatus.SCHEDULED, DocumentStatus.ACTIVE, DocumentStatus.EXPIRED));

		assertThat(result).isNotNull();
		assertThat(result.getContent())
			.extracting(DocumentEntity::getRegistrationNumber, DocumentEntity::getCreatedBy)
			.containsExactly(
				tuple("2024-2281-601", "User5"),
				tuple("2024-2281-602", "User5"),
				tuple("2024-2281-603", "User5"),
				tuple("2024-2281-666", "User5"));
		assertThat(result.getTotalElements()).isEqualTo(4);
	}

	@Test
	void searchByParametersWithResponsibilities() {
		var parameters = new DocumentParameters()
			.withMunicipalityId(MUNICIPALITY_ID)
			.withIncludeConfidential(true)
			.withOnlyLatestRevision(true)
			.withResponsibilities(List.of(
				DocumentResponsibility.create().withPersonId("11111111-1111-1111-1111-111111111111"),
				DocumentResponsibility.create().withPersonId("55555555-5555-5555-5555-555555555555")));
		var pageable = PageRequest.of(0, 10, Sort.by(ASC, "registrationNumber"));

		var result = documentRepository.searchByParameters(parameters, pageable, null);

		assertThat(result).isNotNull();
		assertThat(result.getContent())
			.extracting(DocumentEntity::getRegistrationNumber, DocumentEntity::getRevision)
			.containsExactly(
				tuple("2023-2281-123", 3),
				tuple("2024-2281-601", 1),
				tuple("2024-2281-602", 1),
				tuple("2024-2281-603", 1));
		assertThat(result.getTotalElements()).isEqualTo(4);
	}

	@Test
	void searchByParametersWithResponsibilitiesAndMetadata() {
		var parameters = new DocumentParameters()
			.withMunicipalityId(MUNICIPALITY_ID)
			.withIncludeConfidential(true)
			.withOnlyLatestRevision(true)
			.withResponsibilities(List.of(DocumentResponsibility.create().withPersonId("55555555-5555-5555-5555-555555555555")))
			.withMetaData(List.of(DocumentParameters.MetaData.create().withKey("EMPLOYEE_UNIT").withMatchesAny(List.of("Sidsjö skola"))));
		var pageable = PageRequest.of(0, 10, Sort.by(ASC, "registrationNumber"));

		var result = documentRepository.searchByParameters(parameters, pageable, null);

		assertThat(result).isNotNull();
		assertThat(result.getContent())
			.extracting(DocumentEntity::getRegistrationNumber, DocumentEntity::getRevision)
			.containsExactly(
				tuple("2024-2281-601", 1),
				tuple("2024-2281-602", 1));
		assertThat(result.getTotalElements()).isEqualTo(2);
	}

	private DocumentEntity createDocumentEntity(String registrationNumber) {

		final var documentType = documentTypeRepository.findByMunicipalityIdAndType(MUNICIPALITY_ID, DOCUMENT_TYPE)
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, "Document type '%s' not found within municipality %s".formatted(DOCUMENT_TYPE, MUNICIPALITY_ID)));

		return DocumentEntity.create()
			.withCreatedBy(CREATED_BY)
			.withConfidentiality(ConfidentialityEmbeddable.create())
			.withDescription("description")
			.withMetadata(List.of(
				DocumentMetadataEmbeddable.create().withKey("key1").withValue("value1"),
				DocumentMetadataEmbeddable.create().withKey("key2").withValue("value2")))
			.withRegistrationNumber(registrationNumber)
			.withRevision(0)
			.withType(documentType);
	}

	private static DocumentDataEntity createDocumentDataEntity(String filename) {
		final var fileContent = "fileContent";
		return DocumentDataEntity.create()
			.withStorageLocator(UUID.randomUUID().toString())
			.withFileName(filename)
			.withFileSizeInBytes(fileContent.length())
			.withMimeType("text/plain");
	}

	private boolean isValidUUID(final String value) {
		try {
			UUID.fromString(String.valueOf(value));
		} catch (final Exception e) {
			return false;
		}

		return true;
	}

	private static Stream<Arguments> publicConfidentialTestsArgumentsProvider() {
		final var publicDocument = "2023-2281-123"; // Document 1 (public)
		final var confidentialDocument = "2024-2281-999"; // Document 2 (confidential)

		return Stream.of(
			Arguments.of(publicDocument, InclusionFilter.CONFIDENTIAL_AND_PUBLIC, true),
			Arguments.of(publicDocument, InclusionFilter.PUBLIC, true),
			Arguments.of(confidentialDocument, InclusionFilter.CONFIDENTIAL_AND_PUBLIC, true),
			Arguments.of(confidentialDocument, InclusionFilter.PUBLIC, false));
	}
}
