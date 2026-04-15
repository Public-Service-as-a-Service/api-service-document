package se.sundsvall.document.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Objects;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

@Schema(description = "Document responsibility model.")
public class DocumentResponsibility {

	@NotNull
	@Schema(description = "Principal type.", examples = "USER", requiredMode = REQUIRED)
	private PrincipalType principalType;

	@NotBlank
	@Size(max = 255)
	@Schema(description = "Principal identifier. Username for USER, group name for GROUP. Case-insensitive; stored lowercased.", examples = "username123", requiredMode = REQUIRED)
	private String principalId;

	public static DocumentResponsibility create() {
		return new DocumentResponsibility();
	}

	public PrincipalType getPrincipalType() {
		return principalType;
	}

	public void setPrincipalType(final PrincipalType principalType) {
		this.principalType = principalType;
	}

	public DocumentResponsibility withPrincipalType(final PrincipalType principalType) {
		this.principalType = principalType;
		return this;
	}

	public String getPrincipalId() {
		return principalId;
	}

	public void setPrincipalId(final String principalId) {
		this.principalId = principalId;
	}

	public DocumentResponsibility withPrincipalId(final String principalId) {
		this.principalId = principalId;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(principalId, principalType);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof final DocumentResponsibility other)) {
			return false;
		}
		return Objects.equals(principalId, other.principalId) && principalType == other.principalType;
	}

	@Override
	public String toString() {
		final var builder = new StringBuilder();
		builder.append("DocumentResponsibility [principalType=").append(principalType).append(", principalId=").append(principalId).append("]");
		return builder.toString();
	}
}
