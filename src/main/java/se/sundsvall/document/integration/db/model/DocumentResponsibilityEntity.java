package se.sundsvall.document.integration.db.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.Objects;
import org.hibernate.annotations.TimeZoneStorage;
import org.hibernate.annotations.UuidGenerator;
import se.sundsvall.document.integration.db.model.listener.DocumentResponsibilityEntityListener;

import static org.hibernate.annotations.TimeZoneStorageType.NORMALIZE;

@Entity
@Table(name = "document_responsibility", uniqueConstraints = {
	@UniqueConstraint(name = "uq_document_responsibility", columnNames = {
		"municipality_id", "registration_number", "person_id"
	})
}, indexes = {
	@Index(name = "ix_document_responsibility_lookup", columnList = "municipality_id, person_id"),
	@Index(name = "ix_document_responsibility_document", columnList = "municipality_id, registration_number")
})
@EntityListeners(DocumentResponsibilityEntityListener.class)
public class DocumentResponsibilityEntity implements Serializable {

	private static final long serialVersionUID = -7854389191427042994L;

	@Id
	@UuidGenerator
	@Column(name = "id")
	private String id;

	@Column(name = "municipality_id", nullable = false, length = 4)
	private String municipalityId;

	@Column(name = "registration_number", nullable = false)
	private String registrationNumber;

	@Column(name = "person_id", nullable = false, length = 36)
	private String personId;

	@Column(name = "created_by")
	private String createdBy;

	@Column(name = "created")
	@TimeZoneStorage(NORMALIZE)
	private OffsetDateTime created;

	@Column(name = "updated_by")
	private String updatedBy;

	@Column(name = "updated")
	@TimeZoneStorage(NORMALIZE)
	private OffsetDateTime updated;

	public static DocumentResponsibilityEntity create() {
		return new DocumentResponsibilityEntity();
	}

	public String getId() {
		return id;
	}

	public void setId(final String id) {
		this.id = id;
	}

	public DocumentResponsibilityEntity withId(final String id) {
		this.id = id;
		return this;
	}

	public String getMunicipalityId() {
		return municipalityId;
	}

	public void setMunicipalityId(final String municipalityId) {
		this.municipalityId = municipalityId;
	}

	public DocumentResponsibilityEntity withMunicipalityId(final String municipalityId) {
		this.municipalityId = municipalityId;
		return this;
	}

	public String getRegistrationNumber() {
		return registrationNumber;
	}

	public void setRegistrationNumber(final String registrationNumber) {
		this.registrationNumber = registrationNumber;
	}

	public DocumentResponsibilityEntity withRegistrationNumber(final String registrationNumber) {
		this.registrationNumber = registrationNumber;
		return this;
	}

	public String getPersonId() {
		return personId;
	}

	public void setPersonId(final String personId) {
		this.personId = personId;
	}

	public DocumentResponsibilityEntity withPersonId(final String personId) {
		this.personId = personId;
		return this;
	}

	public String getCreatedBy() {
		return createdBy;
	}

	public void setCreatedBy(final String createdBy) {
		this.createdBy = createdBy;
	}

	public DocumentResponsibilityEntity withCreatedBy(final String createdBy) {
		this.createdBy = createdBy;
		return this;
	}

	public OffsetDateTime getCreated() {
		return created;
	}

	public void setCreated(final OffsetDateTime created) {
		this.created = created;
	}

	public DocumentResponsibilityEntity withCreated(final OffsetDateTime created) {
		this.created = created;
		return this;
	}

	public String getUpdatedBy() {
		return updatedBy;
	}

	public void setUpdatedBy(final String updatedBy) {
		this.updatedBy = updatedBy;
	}

	public DocumentResponsibilityEntity withUpdatedBy(final String updatedBy) {
		this.updatedBy = updatedBy;
		return this;
	}

	public OffsetDateTime getUpdated() {
		return updated;
	}

	public void setUpdated(final OffsetDateTime updated) {
		this.updated = updated;
	}

	public DocumentResponsibilityEntity withUpdated(final OffsetDateTime updated) {
		this.updated = updated;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(created, createdBy, id, municipalityId, personId, registrationNumber, updated, updatedBy);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof final DocumentResponsibilityEntity other)) {
			return false;
		}
		return Objects.equals(created, other.created) && Objects.equals(createdBy, other.createdBy) && Objects.equals(id, other.id) && Objects.equals(municipalityId, other.municipalityId)
			&& Objects.equals(personId, other.personId) && Objects.equals(registrationNumber, other.registrationNumber) && Objects.equals(updated, other.updated) && Objects.equals(updatedBy, other.updatedBy);
	}

	@Override
	public String toString() {
		final var builder = new StringBuilder();
		builder.append("DocumentResponsibilityEntity [id=").append(id).append(", municipalityId=").append(municipalityId).append(", registrationNumber=").append(registrationNumber)
			.append(", personId=").append(personId).append(", createdBy=").append(createdBy).append(", created=").append(created).append(", updatedBy=").append(updatedBy).append(", updated=").append(updated).append("]");
		return builder.toString();
	}
}
