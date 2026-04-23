package se.sundsvall.document.integration.db.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.Objects;
import org.hibernate.annotations.TimeZoneStorage;
import org.hibernate.annotations.UuidGenerator;
import se.sundsvall.document.api.model.DocumentAccessType;
import se.sundsvall.document.integration.db.model.listener.DocumentAccessLogEntityListener;

import static org.hibernate.annotations.TimeZoneStorageType.NORMALIZE;

@Entity
@Table(name = "document_access_log", indexes = {
	@Index(name = "ix_document_access_log_doc", columnList = "municipality_id, registration_number"),
	@Index(name = "ix_document_access_log_revision", columnList = "municipality_id, registration_number, revision"),
	@Index(name = "ix_document_access_log_accessed_at", columnList = "accessed_at")
})
@EntityListeners(DocumentAccessLogEntityListener.class)
public class DocumentAccessLogEntity implements Serializable {

	private static final long serialVersionUID = 1L;

	@Id
	@UuidGenerator
	@Column(name = "id", length = 36)
	private String id;

	@Column(name = "municipality_id", nullable = false, updatable = false, length = 4)
	private String municipalityId;

	@Column(name = "document_id", nullable = false, updatable = false, length = 36)
	private String documentId;

	@Column(name = "registration_number", nullable = false, updatable = false)
	private String registrationNumber;

	@Column(name = "revision", nullable = false, updatable = false)
	private int revision;

	@Column(name = "document_data_id", nullable = false, updatable = false, length = 36)
	private String documentDataId;

	@Enumerated(EnumType.STRING)
	@Column(name = "access_type", nullable = false, updatable = false, length = 20)
	private DocumentAccessType accessType;

	@Column(name = "accessed_by", updatable = false)
	private String accessedBy;

	@Column(name = "accessed_at", nullable = false, updatable = false)
	@TimeZoneStorage(NORMALIZE)
	private OffsetDateTime accessedAt;

	public static DocumentAccessLogEntity create() {
		return new DocumentAccessLogEntity();
	}

	public String getId() {
		return id;
	}

	public void setId(final String id) {
		this.id = id;
	}

	public DocumentAccessLogEntity withId(final String id) {
		this.id = id;
		return this;
	}

	public String getMunicipalityId() {
		return municipalityId;
	}

	public void setMunicipalityId(final String municipalityId) {
		this.municipalityId = municipalityId;
	}

	public DocumentAccessLogEntity withMunicipalityId(final String municipalityId) {
		this.municipalityId = municipalityId;
		return this;
	}

	public String getDocumentId() {
		return documentId;
	}

	public void setDocumentId(final String documentId) {
		this.documentId = documentId;
	}

	public DocumentAccessLogEntity withDocumentId(final String documentId) {
		this.documentId = documentId;
		return this;
	}

	public String getRegistrationNumber() {
		return registrationNumber;
	}

	public void setRegistrationNumber(final String registrationNumber) {
		this.registrationNumber = registrationNumber;
	}

	public DocumentAccessLogEntity withRegistrationNumber(final String registrationNumber) {
		this.registrationNumber = registrationNumber;
		return this;
	}

	public int getRevision() {
		return revision;
	}

	public void setRevision(final int revision) {
		this.revision = revision;
	}

	public DocumentAccessLogEntity withRevision(final int revision) {
		this.revision = revision;
		return this;
	}

	public String getDocumentDataId() {
		return documentDataId;
	}

	public void setDocumentDataId(final String documentDataId) {
		this.documentDataId = documentDataId;
	}

	public DocumentAccessLogEntity withDocumentDataId(final String documentDataId) {
		this.documentDataId = documentDataId;
		return this;
	}

	public DocumentAccessType getAccessType() {
		return accessType;
	}

	public void setAccessType(final DocumentAccessType accessType) {
		this.accessType = accessType;
	}

	public DocumentAccessLogEntity withAccessType(final DocumentAccessType accessType) {
		this.accessType = accessType;
		return this;
	}

	public String getAccessedBy() {
		return accessedBy;
	}

	public void setAccessedBy(final String accessedBy) {
		this.accessedBy = accessedBy;
	}

	public DocumentAccessLogEntity withAccessedBy(final String accessedBy) {
		this.accessedBy = accessedBy;
		return this;
	}

	public OffsetDateTime getAccessedAt() {
		return accessedAt;
	}

	public void setAccessedAt(final OffsetDateTime accessedAt) {
		this.accessedAt = accessedAt;
	}

	public DocumentAccessLogEntity withAccessedAt(final OffsetDateTime accessedAt) {
		this.accessedAt = accessedAt;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(accessType, accessedAt, accessedBy, documentDataId, documentId, id, municipalityId, registrationNumber, revision);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof final DocumentAccessLogEntity other)) {
			return false;
		}
		return accessType == other.accessType
			&& Objects.equals(accessedAt, other.accessedAt)
			&& Objects.equals(accessedBy, other.accessedBy)
			&& Objects.equals(documentDataId, other.documentDataId)
			&& Objects.equals(documentId, other.documentId)
			&& Objects.equals(id, other.id)
			&& Objects.equals(municipalityId, other.municipalityId)
			&& Objects.equals(registrationNumber, other.registrationNumber)
			&& revision == other.revision;
	}

	@Override
	public String toString() {
		final var builder = new StringBuilder();
		builder.append("DocumentAccessLogEntity [id=").append(id)
			.append(", municipalityId=").append(municipalityId)
			.append(", documentId=").append(documentId)
			.append(", registrationNumber=").append(registrationNumber)
			.append(", revision=").append(revision)
			.append(", documentDataId=").append(documentDataId)
			.append(", accessType=").append(accessType)
			.append(", accessedBy=").append(accessedBy)
			.append(", accessedAt=").append(accessedAt)
			.append("]");
		return builder.toString();
	}
}
