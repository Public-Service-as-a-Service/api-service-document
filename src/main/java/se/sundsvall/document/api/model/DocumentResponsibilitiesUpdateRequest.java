package se.sundsvall.document.api.model;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Objects;
import org.hibernate.validator.constraints.UniqueElements;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

@Schema(description = "Document responsibilities update request model.")
public class DocumentResponsibilitiesUpdateRequest {

	@NotBlank
	@Schema(description = "Actor that performed this change.", examples = "username123", requiredMode = REQUIRED)
	private String changedBy;

	@NotNull
	@UniqueElements
	@ArraySchema(schema = @Schema(description = "List of document responsibilities.", implementation = DocumentResponsibility.class, requiredMode = REQUIRED))
	private List<@Valid DocumentResponsibility> responsibilities;

	public static DocumentResponsibilitiesUpdateRequest create() {
		return new DocumentResponsibilitiesUpdateRequest();
	}

	public String getChangedBy() {
		return changedBy;
	}

	public void setChangedBy(final String changedBy) {
		this.changedBy = changedBy;
	}

	public DocumentResponsibilitiesUpdateRequest withChangedBy(final String changedBy) {
		this.changedBy = changedBy;
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
		return Objects.hash(changedBy, responsibilities);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof final DocumentResponsibilitiesUpdateRequest other)) {
			return false;
		}
		return Objects.equals(changedBy, other.changedBy) && Objects.equals(responsibilities, other.responsibilities);
	}

	@Override
	public String toString() {
		final var builder = new StringBuilder();
		builder.append("DocumentResponsibilitiesUpdateRequest [changedBy=").append(changedBy).append(", responsibilities=").append(responsibilities).append("]");
		return builder.toString();
	}
}
