package se.sundsvall.document.integration.db.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.Objects;

@Embeddable
public class ConfidentialityEmbeddable {

	@Column(name = "confidential", nullable = false)
	private boolean confidential;

	@Column(name = "legal_citation")
	private String legalCitation;

	public static ConfidentialityEmbeddable create() {
		return new ConfidentialityEmbeddable();
	}

	public boolean isConfidential() {
		return confidential;
	}

	public void setConfidential(boolean confidential) {
		this.confidential = confidential;
	}

	public ConfidentialityEmbeddable withConfidential(boolean confidential) {
		this.confidential = confidential;
		return this;
	}

	public String getLegalCitation() {
		return legalCitation;
	}

	public void setLegalCitation(String legalCitation) {
		this.legalCitation = legalCitation;
	}

	public ConfidentialityEmbeddable withLegalCitation(String legalCitation) {
		this.legalCitation = legalCitation;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(confidential, legalCitation);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) { return true; }
		if (!(obj instanceof final ConfidentialityEmbeddable other)) { return false; }
		return (confidential == other.confidential) && Objects.equals(legalCitation, other.legalCitation);
	}

	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		builder.append("ConfidentialityEmbeddable [confidential=").append(confidential).append(", legalCitation=").append(legalCitation).append("]");
		return builder.toString();
	}
}
