package se.sundsvall.document.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import java.util.Objects;
import se.sundsvall.dept44.common.validators.annotation.ValidUuid;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

@Schema(description = "DocumentDataCreateRequest model.")
public class DocumentDataCreateRequest {

	@NotBlank
	@ValidUuid
	@Schema(description = "PersonId of the actor that created this revision.", examples = "6c3e4f5a-7b8d-4e9c-a1f2-d3e4b5c6a7f8", requiredMode = REQUIRED)
	private String createdBy;

	public static DocumentDataCreateRequest create() {
		return new DocumentDataCreateRequest();
	}

	public String getCreatedBy() {
		return createdBy;
	}

	public void setCreatedBy(String createdBy) {
		this.createdBy = createdBy;
	}

	public DocumentDataCreateRequest withCreatedBy(String createdBy) {
		this.createdBy = createdBy;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(createdBy);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) { return true; }
		if (!(obj instanceof final DocumentDataCreateRequest other)) { return false; }
		return Objects.equals(createdBy, other.createdBy);
	}

	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		builder.append("DocumentDataCreateRequest [createdBy=").append(createdBy).append("]");
		return builder.toString();
	}
}
