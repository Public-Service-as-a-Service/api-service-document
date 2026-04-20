package se.sundsvall.document.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;
import se.sundsvall.dept44.common.validators.annotation.ValidUuid;
import se.sundsvall.document.api.validation.ValidValidityWindow;

@ValidValidityWindow
@Schema(description = "DocumentUpdateRequest model.")
public class DocumentUpdateRequest {

	@ValidUuid(nullable = true)
	@Schema(description = "PersonId of the actor that performed the update.", examples = "6c3e4f5a-7b8d-4e9c-a1f2-d3e4b5c6a7f8")
	private String updatedBy;

	@Size(max = 8192)
	@Schema(description = "Document description", examples = "A brief description of this object. Maximum 8192 characters.")
	private String description;

	@Schema(description = "Tells if the document is eligible for archiving", examples = "false")
	private Boolean archive;

	@Schema(description = "List of DocumentMetadata objects.")
	private List<@Valid DocumentMetadata> metadataList;

	@Schema(description = "The type of document (validated against a defined list of document types).", examples = "EMPLOYMENT_CERTIFICATE")
	private String type;

	@DateTimeFormat(iso = ISO.DATE)
	@Schema(description = "Start of validity period (inclusive). ISO date (yyyy-MM-dd). Omit to leave unchanged.", examples = "2026-04-15")
	private LocalDate validFrom;

	@DateTimeFormat(iso = ISO.DATE)
	@Schema(description = "End of validity period (inclusive). ISO date (yyyy-MM-dd). Omit to leave unchanged.", examples = "2027-04-15")
	private LocalDate validTo;

	public static DocumentUpdateRequest create() {
		return new DocumentUpdateRequest();
	}

	public String getUpdatedBy() {
		return updatedBy;
	}

	public void setUpdatedBy(String updatedBy) {
		this.updatedBy = updatedBy;
	}

	public DocumentUpdateRequest withUpdatedBy(String updatedBy) {
		this.updatedBy = updatedBy;
		return this;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public DocumentUpdateRequest withDescription(String description) {
		this.description = description;
		return this;
	}

	public Boolean getArchive() {
		return archive;
	}

	public void setArchive(Boolean archive) {
		this.archive = archive;
	}

	public DocumentUpdateRequest withArchive(Boolean archive) {
		this.archive = archive;
		return this;
	}

	public List<DocumentMetadata> getMetadataList() {
		return metadataList;
	}

	public void setMetadataList(List<DocumentMetadata> metadataList) {
		this.metadataList = metadataList;
	}

	public DocumentUpdateRequest withMetadataList(List<DocumentMetadata> metadataList) {
		this.metadataList = metadataList;
		return this;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public DocumentUpdateRequest withType(String type) {
		this.type = type;
		return this;
	}

	public LocalDate getValidFrom() {
		return validFrom;
	}

	public void setValidFrom(LocalDate validFrom) {
		this.validFrom = validFrom;
	}

	public DocumentUpdateRequest withValidFrom(LocalDate validFrom) {
		this.validFrom = validFrom;
		return this;
	}

	public LocalDate getValidTo() {
		return validTo;
	}

	public void setValidTo(LocalDate validTo) {
		this.validTo = validTo;
	}

	public DocumentUpdateRequest withValidTo(LocalDate validTo) {
		this.validTo = validTo;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(archive, updatedBy, description, metadataList, type, validFrom, validTo);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof final DocumentUpdateRequest other)) {
			return false;
		}
		return Objects.equals(archive, other.archive) && Objects.equals(updatedBy, other.updatedBy) && Objects.equals(description, other.description) && Objects.equals(metadataList, other.metadataList) && Objects.equals(type, other.type)
			&& Objects.equals(validFrom, other.validFrom) && Objects.equals(validTo, other.validTo);
	}

	@Override
	public String toString() {
		final var builder = new StringBuilder();
		builder.append("DocumentUpdateRequest [updatedBy=").append(updatedBy).append(", description=").append(description).append(", archive=").append(archive).append(", metadataList=").append(metadataList).append(", type=").append(type).append(
			", validFrom=").append(validFrom).append(", validTo=").append(validTo).append("]");
		return builder.toString();
	}

}
