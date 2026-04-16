package se.sundsvall.document.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Objects;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

@Schema(description = "Document responsibility model.")
public class DocumentResponsibility {

	@NotBlank
	@Size(max = 255)
	@Schema(description = "Username. Case-insensitive; stored lowercased.", examples = "username123", requiredMode = REQUIRED)
	private String username;

	public static DocumentResponsibility create() {
		return new DocumentResponsibility();
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(final String username) {
		this.username = username;
	}

	public DocumentResponsibility withUsername(final String username) {
		this.username = username;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(username);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof final DocumentResponsibility other)) {
			return false;
		}
		return Objects.equals(username, other.username);
	}

	@Override
	public String toString() {
		final var builder = new StringBuilder();
		builder.append("DocumentResponsibility [username=").append(username).append("]");
		return builder.toString();
	}
}
