package se.sundsvall.document.integration.db;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace.NONE;

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
class DocumentTypeRepositoryTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String TYPE = "EMPLOYEE_CERTIFICATE";

	@Autowired
	private DocumentTypeRepository repository;

	@Test
	void findByMunicipalityId() {
		final var matches = repository.findAllByMunicipalityId(MUNICIPALITY_ID);

		assertThat(matches).hasSize(2).satisfiesExactlyInAnyOrder(match -> {
			assertThat(match.getId()).isEqualTo("86b9efc9-c649-40d5-ade0-ac415ea146f1");
			assertThat(match.getMunicipalityId()).isEqualTo("2281");
			assertThat(match.getType()).isEqualTo("EMPLOYEE_CERTIFICATE");
			assertThat(match.getDisplayName()).isEqualTo("Anställningsbevis");
			assertThat(match.getCreatedBy()).isEqualTo("a0000001-0000-0000-0000-000000000001");
		}, match -> {
			assertThat(match.getId()).isEqualTo("3fdecd8b-d295-4222-b60c-e95ba5f5075a");
			assertThat(match.getMunicipalityId()).isEqualTo("2281");
			assertThat(match.getType()).isEqualTo("HOLIDAY_EXCHANGE");
			assertThat(match.getDisplayName()).isEqualTo("Semesterväxlingsdokument");
			assertThat(match.getCreatedBy()).isEqualTo("a0000002-0000-0000-0000-000000000002");
		});
	}

	@Test
	void findByMunicipalityIdAndType() {
		final var result = repository.findByMunicipalityIdAndType(MUNICIPALITY_ID, TYPE);

		assertThat(result).isPresent().hasValueSatisfying(match -> {
			assertThat(match.getId()).isEqualTo("86b9efc9-c649-40d5-ade0-ac415ea146f1");
			assertThat(match.getMunicipalityId()).isEqualTo("2281");
			assertThat(match.getType()).isEqualTo("EMPLOYEE_CERTIFICATE");
			assertThat(match.getDisplayName()).isEqualTo("Anställningsbevis");
			assertThat(match.getCreatedBy()).isEqualTo("a0000001-0000-0000-0000-000000000001");
		});
	}

	@Test
	void existsByMunicipalityIdAndType() {
		assertThat(repository.existsByMunicipalityIdAndType(MUNICIPALITY_ID, TYPE)).isTrue();
		assertThat(repository.existsByMunicipalityIdAndType("NOT_FOUND", TYPE)).isFalse();
		assertThat(repository.existsByMunicipalityIdAndType(MUNICIPALITY_ID, "NOT_FOUND")).isFalse();
	}
}
