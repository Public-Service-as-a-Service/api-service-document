package se.sundsvall.document.integration.elasticsearch;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import se.sundsvall.document.api.model.DocumentStatus;
import se.sundsvall.document.service.extraction.ExtractionStatus;

/**
 * One Elasticsearch document per {@code DocumentDataEntity} (i.e. one file, one ES doc).
 * <p>
 * The field set covers everything the retired {@code SearchSpecification.withSearchQuery} used
 * to match against the DB (description, metadata, fileName, mimeType, createdBy, registrationNumber)
 * plus the new {@code extractedText} from Tika. {@code ?query=} runs a single {@code multi_match}
 * against this index — no DB-side fallback — so any new free-text field must be added both here
 * and in the indexer (see {@code DocumentIndexingService}) and in the search query fields list
 * (see {@code DocumentService.search}).
 */
@Document(indexName = "documents")
public class DocumentIndexEntity {

	@Id
	@Field(type = FieldType.Keyword)
	private String id;

	@Field(type = FieldType.Keyword)
	private String documentId;

	@Field(type = FieldType.Keyword)
	private String registrationNumber;

	@Field(type = FieldType.Integer)
	private int revision;

	@Field(type = FieldType.Keyword)
	private String municipalityId;

	@Field(type = FieldType.Keyword)
	private String documentType;

	@Field(type = FieldType.Keyword)
	private DocumentStatus status;

	@Field(type = FieldType.Boolean)
	private boolean confidential;

	@Field(type = FieldType.Date, format = DateFormat.date)
	private LocalDate validFrom;

	@Field(type = FieldType.Date, format = DateFormat.date)
	private LocalDate validTo;

	@Field(type = FieldType.Text)
	private String fileName;

	@Field(type = FieldType.Keyword)
	private String mimeType;

	@Field(type = FieldType.Text)
	private String description;

	@Field(type = FieldType.Text)
	private String createdBy;

	// Metadata is flattened into two parallel text fields so a match_phrase across all fields
	// catches occurrences inside structured data. Key-filtered metadata queries remain on the
	// DB-side searchByParameters endpoint.
	@Field(type = FieldType.Text)
	private List<String> metadataKeys;

	@Field(type = FieldType.Text)
	private List<String> metadataValues;

	@Field(type = FieldType.Text, analyzer = "standard")
	private String extractedText;

	@Field(type = FieldType.Keyword)
	private ExtractionStatus extractionStatus;

	public String getId() {
		return id;
	}

	public DocumentIndexEntity setId(String id) {
		this.id = id;
		return this;
	}

	public String getDocumentId() {
		return documentId;
	}

	public DocumentIndexEntity setDocumentId(String documentId) {
		this.documentId = documentId;
		return this;
	}

	public String getRegistrationNumber() {
		return registrationNumber;
	}

	public DocumentIndexEntity setRegistrationNumber(String registrationNumber) {
		this.registrationNumber = registrationNumber;
		return this;
	}

	public int getRevision() {
		return revision;
	}

	public DocumentIndexEntity setRevision(int revision) {
		this.revision = revision;
		return this;
	}

	public String getMunicipalityId() {
		return municipalityId;
	}

	public DocumentIndexEntity setMunicipalityId(String municipalityId) {
		this.municipalityId = municipalityId;
		return this;
	}

	public String getDocumentType() {
		return documentType;
	}

	public DocumentIndexEntity setDocumentType(String documentType) {
		this.documentType = documentType;
		return this;
	}

	public DocumentStatus getStatus() {
		return status;
	}

	public DocumentIndexEntity setStatus(DocumentStatus status) {
		this.status = status;
		return this;
	}

	public boolean isConfidential() {
		return confidential;
	}

	public DocumentIndexEntity setConfidential(boolean confidential) {
		this.confidential = confidential;
		return this;
	}

	public LocalDate getValidFrom() {
		return validFrom;
	}

	public DocumentIndexEntity setValidFrom(LocalDate validFrom) {
		this.validFrom = validFrom;
		return this;
	}

	public LocalDate getValidTo() {
		return validTo;
	}

	public DocumentIndexEntity setValidTo(LocalDate validTo) {
		this.validTo = validTo;
		return this;
	}

	public String getFileName() {
		return fileName;
	}

	public DocumentIndexEntity setFileName(String fileName) {
		this.fileName = fileName;
		return this;
	}

	public String getMimeType() {
		return mimeType;
	}

	public DocumentIndexEntity setMimeType(String mimeType) {
		this.mimeType = mimeType;
		return this;
	}

	public String getDescription() {
		return description;
	}

	public DocumentIndexEntity setDescription(String description) {
		this.description = description;
		return this;
	}

	public String getCreatedBy() {
		return createdBy;
	}

	public DocumentIndexEntity setCreatedBy(String createdBy) {
		this.createdBy = createdBy;
		return this;
	}

	public List<String> getMetadataKeys() {
		return metadataKeys;
	}

	public DocumentIndexEntity setMetadataKeys(List<String> metadataKeys) {
		this.metadataKeys = metadataKeys;
		return this;
	}

	public List<String> getMetadataValues() {
		return metadataValues;
	}

	public DocumentIndexEntity setMetadataValues(List<String> metadataValues) {
		this.metadataValues = metadataValues;
		return this;
	}

	public String getExtractedText() {
		return extractedText;
	}

	public DocumentIndexEntity setExtractedText(String extractedText) {
		this.extractedText = extractedText;
		return this;
	}

	public ExtractionStatus getExtractionStatus() {
		return extractionStatus;
	}

	public DocumentIndexEntity setExtractionStatus(ExtractionStatus extractionStatus) {
		this.extractionStatus = extractionStatus;
		return this;
	}
}
