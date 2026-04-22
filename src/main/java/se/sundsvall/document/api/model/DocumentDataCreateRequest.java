package se.sundsvall.document.api.model;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.Objects;
import se.sundsvall.dept44.common.validators.annotation.ValidUuid;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

@Schema(description = "DocumentDataCreateRequest model.")
public class DocumentDataCreateRequest {

	@NotBlank
	@ValidUuid
	@Schema(description = "PersonId of the actor that created this revision.", examples = "6c3e4f5a-7b8d-4e9c-a1f2-d3e4b5c6a7f8", requiredMode = REQUIRED)
	private String createdBy;

	@ArraySchema(schema = @Schema(description = "Document data IDs (from the current revision) to drop in the new revision. Combined with any uploaded files to produce a single revision bump. Unknown IDs → 404.",
		implementation = String.class,
		examples = "082ba08f-03c7-409f-b8a6-940a1397ba38"))
	private List<@ValidUuid String> filesToDelete;

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

	public List<String> getFilesToDelete() {
		return filesToDelete;
	}

	public void setFilesToDelete(List<String> filesToDelete) {
		this.filesToDelete = filesToDelete;
	}

	public DocumentDataCreateRequest withFilesToDelete(List<String> filesToDelete) {
		this.filesToDelete = filesToDelete;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(createdBy, filesToDelete);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) { return true; }
		if (!(obj instanceof final DocumentDataCreateRequest other)) { return false; }
		return Objects.equals(createdBy, other.createdBy) && Objects.equals(filesToDelete, other.filesToDelete);
	}

	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		builder.append("DocumentDataCreateRequest [createdBy=").append(createdBy).append(", filesToDelete=").append(filesToDelete).append("]");
		return builder.toString();
	}
}
