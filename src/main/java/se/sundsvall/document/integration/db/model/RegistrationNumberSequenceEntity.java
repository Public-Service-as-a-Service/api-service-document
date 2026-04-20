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
import se.sundsvall.document.integration.db.model.listener.RegistrationNumberSequenceEntityListener;

import static org.hibernate.annotations.TimeZoneStorageType.NORMALIZE;

@Entity
@Table(
	name = "registration_number_sequence",
	uniqueConstraints = {
		@UniqueConstraint(name = "uq_municipality_id", columnNames = "municipality_id"),
	},
	indexes = {
		@Index(name = "ix_municipality_id", columnList = "municipality_id")
	})
@EntityListeners(RegistrationNumberSequenceEntityListener.class)
public class RegistrationNumberSequenceEntity {

	@Id
	@UuidGenerator
	@Column(name = "id")
	private String id;

	@Column(name = "municipality_id", length = 4)
	private String municipalityId;

	@Column(name = "sequence_number")
	private int sequenceNumber;

	@Column(name = "created")
	@TimeZoneStorage(NORMALIZE)
	private OffsetDateTime created;

	@Column(name = "modified")
	@TimeZoneStorage(NORMALIZE)
	private OffsetDateTime modified;

	public static RegistrationNumberSequenceEntity create() {
		return new RegistrationNumberSequenceEntity();
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public RegistrationNumberSequenceEntity withId(String id) {
		this.id = id;
		return this;
	}

	public String getMunicipalityId() {
		return municipalityId;
	}

	public void setMunicipalityId(String municipalityId) {
		this.municipalityId = municipalityId;
	}

	public RegistrationNumberSequenceEntity withMunicipalityId(String municipalityId) {
		this.municipalityId = municipalityId;
		return this;
	}

	public int getSequenceNumber() {
		return sequenceNumber;
	}

	public void setSequenceNumber(int sequenceNumber) {
		this.sequenceNumber = sequenceNumber;
	}

	public RegistrationNumberSequenceEntity withSequenceNumber(int sequenceNumber) {
		this.sequenceNumber = sequenceNumber;
		return this;
	}

	public OffsetDateTime getCreated() {
		return created;
	}

	public void setCreated(OffsetDateTime created) {
		this.created = created;
	}

	public RegistrationNumberSequenceEntity withCreated(OffsetDateTime created) {
		this.created = created;
		return this;
	}

	public OffsetDateTime getModified() {
		return modified;
	}

	public void setModified(OffsetDateTime modified) {
		this.modified = modified;
	}

	public RegistrationNumberSequenceEntity withModified(OffsetDateTime modified) {
		this.modified = modified;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(created, id, modified, municipalityId, sequenceNumber);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) { return true; }
		if (!(obj instanceof final RegistrationNumberSequenceEntity other)) { return false; }
		return Objects.equals(created, other.created) && Objects.equals(id, other.id) && Objects.equals(modified, other.modified) && Objects.equals(municipalityId, other.municipalityId) && (sequenceNumber == other.sequenceNumber);
	}

	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		builder.append("RegistrationNumberSequenceEntity [id=").append(id).append(", municipalityId=").append(municipalityId).append(", sequenceNumber=").append(sequenceNumber).append(", created=").append(created).append(", modified=").append(modified)
			.append("]");
		return builder.toString();
	}
}
