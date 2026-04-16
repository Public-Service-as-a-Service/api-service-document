package se.sundsvall.document.integration.db.model;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import org.hibernate.annotations.TimeZoneStorage;
import org.hibernate.annotations.UuidGenerator;
import se.sundsvall.document.api.model.DocumentStatus;
import se.sundsvall.document.integration.db.model.listener.DocumentEntityListener;

import static jakarta.persistence.CascadeType.ALL;
import static jakarta.persistence.FetchType.EAGER;
import static org.hibernate.annotations.TimeZoneStorageType.NORMALIZE;

@Entity
@Table(name = "document", uniqueConstraints = {
	@UniqueConstraint(name = "uq_revision_and_registration_number", columnNames = {
		"revision", "registration_number"
	})
}, indexes = {
	@Index(name = "ix_registration_number", columnList = "registration_number"),
	@Index(name = "ix_created_by", columnList = "created_by"),
	@Index(name = "ix_municipality_id", columnList = "municipality_id"),
	@Index(name = "ix_confidential", columnList = "confidential"),
	@Index(name = "ix_status", columnList = "status"),
})
@EntityListeners(DocumentEntityListener.class)
public class DocumentEntity {

	@Id
	@UuidGenerator
	@Column(name = "id")
	private String id;

	@Column(name = "revision", nullable = false)
	private int revision;

	@Column(name = "municipality_id")
	private String municipalityId;

	@Column(name = "registration_number", nullable = false, updatable = false)
	private String registrationNumber;

	@ManyToOne(cascade = ALL, optional = false, fetch = EAGER)
	@JoinColumn(name = "document_type_id", referencedColumnName = "id", foreignKey = @ForeignKey(name = "fk_document_document_type"), nullable = false)
	private DocumentTypeEntity type;

	@Column(name = "description", nullable = false, columnDefinition = "varchar(8192)")
	private String description;

	@Embedded
	private ConfidentialityEmbeddable confidentiality;

	@Column(name = "archive", nullable = false)
	private boolean archive;

	@Column(name = "created_by")
	private String createdBy;

	@Column(name = "updated_by")
	private String updatedBy;

	@Column(name = "created")
	@TimeZoneStorage(NORMALIZE)
	private OffsetDateTime created;

	@Column(name = "valid_from", columnDefinition = "date")
	private LocalDate validFrom;

	@Column(name = "valid_to", columnDefinition = "date")
	private LocalDate validTo;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 20)
	private DocumentStatus status;

	@OneToMany(cascade = ALL, orphanRemoval = true)
	@JoinColumn(name = "document_id", referencedColumnName = "id", nullable = false, foreignKey = @ForeignKey(name = "fk_document_data_document"))
	private List<DocumentDataEntity> documentData;

	@ElementCollection(fetch = EAGER)
	@CollectionTable(name = "document_metadata", indexes = {
		@Index(name = "ix_key", columnList = "key")
	}, joinColumns = @JoinColumn(name = "document_id", referencedColumnName = "id", foreignKey = @ForeignKey(name = "fk_document_metadata_document")))
	private List<DocumentMetadataEmbeddable> metadata;

	public static DocumentEntity create() {
		return new DocumentEntity();
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public DocumentEntity withId(String id) {
		this.id = id;
		return this;
	}

	public int getRevision() {
		return revision;
	}

	public void setRevision(int revision) {
		this.revision = revision;
	}

	public DocumentEntity withRevision(int revision) {
		this.revision = revision;
		return this;
	}

	public String getMunicipalityId() {
		return municipalityId;
	}

	public void setMunicipalityId(String municipalityId) {
		this.municipalityId = municipalityId;
	}

	public DocumentEntity withMunicipalityId(String municipalityId) {
		this.municipalityId = municipalityId;
		return this;
	}

	public String getRegistrationNumber() {
		return registrationNumber;
	}

	public void setRegistrationNumber(String registrationNumber) {
		this.registrationNumber = registrationNumber;
	}

	public DocumentEntity withRegistrationNumber(String registrationNumber) {
		this.registrationNumber = registrationNumber;
		return this;
	}

	public DocumentTypeEntity getType() {
		return type;
	}

	public void setType(DocumentTypeEntity type) {
		this.type = type;
	}

	public DocumentEntity withType(DocumentTypeEntity type) {
		this.type = type;
		return this;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public DocumentEntity withDescription(String description) {
		this.description = description;
		return this;
	}

	public ConfidentialityEmbeddable getConfidentiality() {
		return confidentiality;
	}

	public void setConfidentiality(ConfidentialityEmbeddable confidentiality) {
		this.confidentiality = confidentiality;
	}

	public DocumentEntity withConfidentiality(ConfidentialityEmbeddable confidentiality) {
		this.confidentiality = confidentiality;
		return this;
	}

	public boolean isArchive() {
		return archive;
	}

	public void setArchive(final boolean archive) {
		this.archive = archive;
	}

	public DocumentEntity withArchive(final boolean archive) {
		this.archive = archive;
		return this;
	}

	public String getCreatedBy() {
		return createdBy;
	}

	public void setCreatedBy(String createdBy) {
		this.createdBy = createdBy;
	}

	public DocumentEntity withCreatedBy(String createdBy) {
		this.createdBy = createdBy;
		return this;
	}

	public String getUpdatedBy() {
		return updatedBy;
	}

	public void setUpdatedBy(String updatedBy) {
		this.updatedBy = updatedBy;
	}

	public DocumentEntity withUpdatedBy(String updatedBy) {
		this.updatedBy = updatedBy;
		return this;
	}

	public OffsetDateTime getCreated() {
		return created;
	}

	public void setCreated(OffsetDateTime created) {
		this.created = created;
	}

	public DocumentEntity withCreated(OffsetDateTime created) {
		this.created = created;
		return this;
	}

	public LocalDate getValidFrom() {
		return validFrom;
	}

	public void setValidFrom(LocalDate validFrom) {
		this.validFrom = validFrom;
	}

	public DocumentEntity withValidFrom(LocalDate validFrom) {
		this.validFrom = validFrom;
		return this;
	}

	public LocalDate getValidTo() {
		return validTo;
	}

	public void setValidTo(LocalDate validTo) {
		this.validTo = validTo;
	}

	public DocumentEntity withValidTo(LocalDate validTo) {
		this.validTo = validTo;
		return this;
	}

	public DocumentStatus getStatus() {
		return status;
	}

	public void setStatus(DocumentStatus status) {
		this.status = status;
	}

	public DocumentEntity withStatus(DocumentStatus status) {
		this.status = status;
		return this;
	}

	public List<DocumentDataEntity> getDocumentData() {
		return documentData;
	}

	public void setDocumentData(List<DocumentDataEntity> documentData) {
		this.documentData = documentData;
	}

	public DocumentEntity withDocumentData(List<DocumentDataEntity> documentData) {
		this.documentData = documentData;
		return this;
	}

	public List<DocumentMetadataEmbeddable> getMetadata() {
		return metadata;
	}

	public void setMetadata(List<DocumentMetadataEmbeddable> metadata) {
		this.metadata = metadata;
	}

	public DocumentEntity withMetadata(List<DocumentMetadataEmbeddable> metadata) {
		this.metadata = metadata;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(archive, confidentiality, created, createdBy, description, documentData, id, metadata, municipalityId, registrationNumber, revision, status, type, updatedBy, validFrom, validTo);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof final DocumentEntity other)) {
			return false;
		}
		return archive == other.archive && Objects.equals(confidentiality, other.confidentiality) && Objects.equals(created, other.created) && Objects.equals(createdBy, other.createdBy) && Objects.equals(description, other.description) && Objects
			.equals(documentData, other.documentData) && Objects.equals(id, other.id) && Objects.equals(metadata, other.metadata) && Objects.equals(municipalityId, other.municipalityId) && Objects.equals(registrationNumber, other.registrationNumber)
			&& revision == other.revision && status == other.status && Objects.equals(type, other.type) && Objects.equals(updatedBy, other.updatedBy) && Objects.equals(validFrom, other.validFrom) && Objects.equals(validTo, other.validTo);
	}

	@Override
	public String toString() {
		final var builder = new StringBuilder();
		builder.append("DocumentEntity [id=").append(id).append(", revision=").append(revision).append(", municipalityId=").append(municipalityId).append(", registrationNumber=").append(registrationNumber).append(", type=").append(type).append(
			", description=").append(description).append(", confidentiality=").append(confidentiality).append(", archive=").append(archive).append(", createdBy=").append(createdBy).append(", updatedBy=").append(updatedBy).append(", created=").append(
				created).append(", validFrom=").append(validFrom).append(", validTo=").append(validTo).append(", status=").append(status).append(", documentData=").append(documentData).append(", metadata=").append(metadata).append("]");
		return builder.toString();
	}
}
