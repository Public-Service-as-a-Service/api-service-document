package se.sundsvall.document.integration.db.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.OffsetDateTime;
import java.util.Objects;
import org.hibernate.annotations.TimeZoneStorage;
import org.hibernate.annotations.UuidGenerator;
import se.sundsvall.document.integration.db.model.listener.DocumentTypeEntityListener;

import static org.hibernate.annotations.TimeZoneStorageType.NORMALIZE;

@Entity
@Table(name = "document_type", uniqueConstraints = {
	@UniqueConstraint(name = "uq_municipality_id_and_type", columnNames = {
		"municipality_id", "`type`"
	})
}, indexes = {
	@Index(name = "ix_municipality_id_type", columnList = "municipality_id, `type`"),
	@Index(name = "ix_municipality_id", columnList = "municipality_id")
})
@EntityListeners(DocumentTypeEntityListener.class)
public class DocumentTypeEntity {

	@Id
	@UuidGenerator
	@Column(name = "id")
	private String id;

	@Column(name = "municipality_id", length = 4)
	private String municipalityId;

	@Column(name = "`type`", nullable = false)
	private String type;

	@Column(name = "display_name", nullable = false)
	private String displayName;

	@Column(name = "created")
	@TimeZoneStorage(NORMALIZE)
	private OffsetDateTime created;

	@Column(name = "created_by")
	private String createdBy;

	@Column(name = "last_updated")
	@TimeZoneStorage(NORMALIZE)
	private OffsetDateTime lastUpdated;

	@Column(name = "last_updated_by")
	private String lastUpdatedBy;

	public static DocumentTypeEntity create() {
		return new DocumentTypeEntity();
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public DocumentTypeEntity withId(String id) {
		this.id = id;
		return this;
	}

	public String getMunicipalityId() {
		return municipalityId;
	}

	public void setMunicipalityId(String municipalityId) {
		this.municipalityId = municipalityId;
	}

	public DocumentTypeEntity withMunicipalityId(String municipalityId) {
		this.municipalityId = municipalityId;
		return this;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public DocumentTypeEntity withType(String type) {
		this.type = type;
		return this;
	}

	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	public DocumentTypeEntity withDisplayName(String displayName) {
		this.displayName = displayName;
		return this;
	}

	public OffsetDateTime getCreated() {
		return created;
	}

	public void setCreated(OffsetDateTime created) {
		this.created = created;
	}

	public DocumentTypeEntity withCreated(OffsetDateTime created) {
		this.created = created;
		return this;
	}

	public String getCreatedBy() {
		return createdBy;
	}

	public void setCreatedBy(String createdBy) {
		this.createdBy = createdBy;
	}

	public DocumentTypeEntity withCreatedBy(String createdBy) {
		this.createdBy = createdBy;
		return this;
	}

	public OffsetDateTime getLastUpdated() {
		return lastUpdated;
	}

	public void setLastUpdated(OffsetDateTime lastUpdated) {
		this.lastUpdated = lastUpdated;
	}

	public DocumentTypeEntity withLastUpdated(OffsetDateTime lastUpdated) {
		this.lastUpdated = lastUpdated;
		return this;
	}

	public String getLastUpdatedBy() {
		return lastUpdatedBy;
	}

	public void setLastUpdatedBy(String lastUpdatedBy) {
		this.lastUpdatedBy = lastUpdatedBy;
	}

	public DocumentTypeEntity withLastUpdatedBy(String lastUpdatedBy) {
		this.lastUpdatedBy = lastUpdatedBy;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(created, createdBy, displayName, id, lastUpdated, lastUpdatedBy, municipalityId, type);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof final DocumentTypeEntity other)) {
			return false;
		}
		return Objects.equals(created, other.created) && Objects.equals(createdBy, other.createdBy) && Objects.equals(displayName, other.displayName) && Objects.equals(id, other.id) && Objects.equals(lastUpdated, other.lastUpdated) && Objects.equals(
			lastUpdatedBy, other.lastUpdatedBy) && Objects.equals(municipalityId, other.municipalityId) && Objects.equals(type, other.type);
	}

	@Override
	public String toString() {
		final var builder = new StringBuilder();
		builder.append("DocumentTypeEntity [id=").append(id).append(", municipalityId=").append(municipalityId).append(", type=").append(type).append(", displayName=").append(displayName).append(", created=").append(created).append(", createdBy=")
			.append(createdBy).append(", lastUpdated=").append(lastUpdated).append(", lastUpdatedBy=").append(lastUpdatedBy).append("]");
		return builder.toString();
	}
}
