package se.sundsvall.document.integration.db.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.Objects;

@Embeddable
public class DocumentMetadataEmbeddable {

	@Column(name = "`key`")
	private String key;

	@Column(name = "value")
	private String value;

	public static DocumentMetadataEmbeddable create() {
		return new DocumentMetadataEmbeddable();
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public DocumentMetadataEmbeddable withKey(String key) {
		this.key = key;
		return this;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public DocumentMetadataEmbeddable withValue(String value) {
		this.value = value;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(key, value);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) { return true; }
		if (!(obj instanceof final DocumentMetadataEmbeddable other)) { return false; }
		return Objects.equals(key, other.key) && Objects.equals(value, other.value);
	}

	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		builder.append("DocumentMetadataEmbeddable [key=").append(key).append(", value=").append(value).append("]");
		return builder.toString();
	}
}
