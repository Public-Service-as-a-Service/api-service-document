package se.sundsvall.document.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Objects;
import se.sundsvall.dept44.common.validators.annotation.ValidUuid;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

@Schema(description = "ConfidentialityUpdateRequest model.")
public class ConfidentialityUpdateRequest {

	@NotNull
	@Schema(description = """
		A flag that can be set to alert administrative users handling the information that there are some special privacy policies to follow for the person in question.
		If there are special privacy policies to follow for this record, this flag should be set to 'true', otherwise 'false'.
		Please note: This will affect all revisions, not just the latest revision.
		""", examples = "false", requiredMode = REQUIRED)
	private Boolean confidential;

	@Schema(description = "Legal citation", examples = "25 kap. 1 § OSL")
	private String legalCitation;

	@NotBlank
	@ValidUuid
	@Schema(description = "PersonId of the actor that performed this change.", examples = "6c3e4f5a-7b8d-4e9c-a1f2-d3e4b5c6a7f8", requiredMode = REQUIRED)
	private String updatedBy;

	public static ConfidentialityUpdateRequest create() {
		return new ConfidentialityUpdateRequest();
	}

	public Boolean getConfidential() {
		return confidential;
	}

	public void setConfidential(Boolean confidential) {
		this.confidential = confidential;
	}

	public ConfidentialityUpdateRequest withConfidential(Boolean confidential) {
		this.confidential = confidential;
		return this;
	}

	public String getLegalCitation() {
		return legalCitation;
	}

	public void setLegalCitation(String legalCitation) {
		this.legalCitation = legalCitation;
	}

	public ConfidentialityUpdateRequest withLegalCitation(String legalCitation) {
		this.legalCitation = legalCitation;
		return this;
	}

	public String getUpdatedBy() {
		return updatedBy;
	}

	public void setUpdatedBy(String updatedBy) {
		this.updatedBy = updatedBy;
	}

	public ConfidentialityUpdateRequest withUpdatedBy(String updatedBy) {
		this.updatedBy = updatedBy;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(updatedBy, confidential, legalCitation);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) { return true; }
		if (!(obj instanceof final ConfidentialityUpdateRequest other)) { return false; }
		return Objects.equals(updatedBy, other.updatedBy) && Objects.equals(confidential, other.confidential) && Objects.equals(legalCitation, other.legalCitation);
	}

	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		builder.append("ConfidentialityUpdateRequest [confidential=").append(confidential).append(", legalCitation=").append(legalCitation).append(", updatedBy=").append(updatedBy).append("]");
		return builder.toString();
	}
}
