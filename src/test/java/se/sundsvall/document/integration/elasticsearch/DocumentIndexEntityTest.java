package se.sundsvall.document.integration.elasticsearch;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import se.sundsvall.document.api.model.DocumentStatus;
import se.sundsvall.document.service.extraction.ExtractionStatus;

import static org.assertj.core.api.Assertions.assertThat;

class DocumentIndexEntityTest {

	@Test
	void fluentSetters_populateAllFields_andGettersReturnThem() {
		final var metadataKeys = List.of("employeeType", "unit");
		final var metadataValues = List.of("Developer", "Company A");
		final var validFrom = LocalDate.of(2026, 1, 1);
		final var validTo = LocalDate.of(2027, 1, 1);

		final var bean = new DocumentIndexEntity()
			.setId("data-id")
			.setDocumentId("doc-id")
			.setRegistrationNumber("2026-2281-1")
			.setRevision(5)
			.setMunicipalityId("2281")
			.setDocumentType("EMPLOYEE_CERTIFICATE")
			.setStatus(DocumentStatus.ACTIVE)
			.setConfidential(true)
			.setValidFrom(validFrom)
			.setValidTo(validTo)
			.setFileName("invoice.pdf")
			.setMimeType("application/pdf")
			.setTitle("Invoice March 2026")
			.setDescription("Monthly invoice")
			.setCreatedBy("martin")
			.setMetadataKeys(metadataKeys)
			.setMetadataValues(metadataValues)
			.setExtractedText("invoice content")
			.setExtractionStatus(ExtractionStatus.SUCCESS);

		assertThat(bean.getId()).isEqualTo("data-id");
		assertThat(bean.getDocumentId()).isEqualTo("doc-id");
		assertThat(bean.getRegistrationNumber()).isEqualTo("2026-2281-1");
		assertThat(bean.getRevision()).isEqualTo(5);
		assertThat(bean.getMunicipalityId()).isEqualTo("2281");
		assertThat(bean.getDocumentType()).isEqualTo("EMPLOYEE_CERTIFICATE");
		assertThat(bean.getStatus()).isEqualTo(DocumentStatus.ACTIVE);
		assertThat(bean.isConfidential()).isTrue();
		assertThat(bean.getValidFrom()).isEqualTo(validFrom);
		assertThat(bean.getValidTo()).isEqualTo(validTo);
		assertThat(bean.getFileName()).isEqualTo("invoice.pdf");
		assertThat(bean.getMimeType()).isEqualTo("application/pdf");
		assertThat(bean.getTitle()).isEqualTo("Invoice March 2026");
		assertThat(bean.getDescription()).isEqualTo("Monthly invoice");
		assertThat(bean.getCreatedBy()).isEqualTo("martin");
		assertThat(bean.getMetadataKeys()).isEqualTo(metadataKeys);
		assertThat(bean.getMetadataValues()).isEqualTo(metadataValues);
		assertThat(bean.getExtractedText()).isEqualTo("invoice content");
		assertThat(bean.getExtractionStatus()).isEqualTo(ExtractionStatus.SUCCESS);
	}

	@Test
	void defaultBean_hasDefaultValues() {
		final var bean = new DocumentIndexEntity();
		assertThat(bean.getId()).isNull();
		assertThat(bean.getRevision()).isZero();
		assertThat(bean.isConfidential()).isFalse();
		assertThat(bean.getMetadataKeys()).isNull();
		assertThat(bean.getExtractionStatus()).isNull();
	}
}
