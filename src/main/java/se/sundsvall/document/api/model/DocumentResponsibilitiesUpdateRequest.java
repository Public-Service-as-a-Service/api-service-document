package se.sundsvall.document.api.model;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Objects;
import org.hibernate.validator.constraints.UniqueElements;
import se.sundsvall.dept44.common.validators.annotation.ValidUuid;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

@Schema(description = "Document responsibilities update request model.")
public class DocumentResponsibilitiesUpdateRequest {

	@NotBlank
	@ValidUuid
	@Schema(description = "PersonId of the actor that performed this change.", examples = "6c3e4f5a-7b8d-4e9c-a1f2-d3e4b5c6a7f8", requiredMode = REQUIRED)
	private String updatedBy;

	@NotNull
	@UniqueElements
	@ArraySchema(schema = @Schema(description = "List of document responsibilities.", implementation = DocumentResponsibility.class, requiredMode = REQUIRED))
	private List<@Valid DocumentResponsibility> responsibilities;

	public static DocumentResponsibilitiesUpdateRequest create() {
		return new DocumentResponsibilitiesUpdateRequest();
	}

	public String getUpdatedBy() {
		return updatedBy;
	}

	public void setUpdatedBy(final String updatedBy) {
		this.updatedBy = updatedBy;
	}

	public DocumentResponsibilitiesUpdateRequest withUpdatedBy(final String updatedBy) {
		this.updatedBy = updatedBy;
		return this;
	}

	public List<DocumentResponsibility> getResponsibilities() {
		return responsibilities;
	}

	public void setResponsibilities(final List<DocumentResponsibility> responsibilities) {
		this.responsibilities = responsibilities;
	}

	public DocumentResponsibilitiesUpdateRequest withResponsibilities(final List<DocumentResponsibility> responsibilities) {
		this.responsibilities = responsibilities;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(updatedBy, responsibilities);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof final DocumentResponsibilitiesUpdateRequest other)) {
			return false;
		}
		return Objects.equals(updatedBy, other.updatedBy) && Objects.equals(responsibilities, other.responsibilities);
	}

	@Override
	public String toString() {
		final var builder = new StringBuilder();
		builder.append("DocumentResponsibilitiesUpdateRequest [updatedBy=").append(updatedBy).append(", responsibilities=").append(responsibilities).append("]");
		return builder.toString();
	}
}
