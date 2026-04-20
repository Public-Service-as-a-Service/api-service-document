package se.sundsvall.document.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import java.util.Objects;
import se.sundsvall.dept44.common.validators.annotation.ValidUuid;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

@Schema(description = "Document responsibility model.")
public class DocumentResponsibility {

	@NotBlank
	@ValidUuid
	@Schema(description = "Person ID of the responsible party.", examples = "6b8d4a1c-34e2-4f73-a5f1-b7c2e9a0d8c4", requiredMode = REQUIRED)
	private String personId;

	public static DocumentResponsibility create() {
		return new DocumentResponsibility();
	}

	public String getPersonId() {
		return personId;
	}

	public void setPersonId(final String personId) {
		this.personId = personId;
	}

	public DocumentResponsibility withPersonId(final String personId) {
		this.personId = personId;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(personId);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof final DocumentResponsibility other)) {
			return false;
		}
		return Objects.equals(personId, other.personId);
	}

	@Override
	public String toString() {
		final var builder = new StringBuilder();
		builder.append("DocumentResponsibility [personId=").append(personId).append("]");
		return builder.toString();
	}
}
