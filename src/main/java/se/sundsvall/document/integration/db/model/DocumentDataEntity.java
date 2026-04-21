package se.sundsvall.document.integration.db.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.Objects;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.UuidGenerator;
import se.sundsvall.document.service.extraction.ExtractionStatus;

@Entity
@Table(name = "document_data")
public class DocumentDataEntity {

	@Id
	@UuidGenerator
	@Column(name = "id")
	private String id;

	@Column(name = "mime_type")
	private String mimeType;

	@Column(name = "file_name")
	private String fileName;

	@Column(name = "file_size_in_bytes")
	@ColumnDefault("0")
	private long fileSizeInBytes;

	@Column(name = "storage_locator", nullable = false)
	private String storageLocator;

	@Column(name = "content_hash", length = 64)
	private String contentHash;

	@Column(name = "extracted_text", columnDefinition = "LONGTEXT")
	private String extractedText;

	@Enumerated(EnumType.STRING)
	@Column(name = "extraction_status", length = 32, nullable = false)
	private ExtractionStatus extractionStatus;

	public static DocumentDataEntity create() {
		return new DocumentDataEntity();
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public DocumentDataEntity withId(String id) {
		this.id = id;
		return this;
	}

	public String getMimeType() {
		return mimeType;
	}

	public void setMimeType(String mimeType) {
		this.mimeType = mimeType;
	}

	public DocumentDataEntity withMimeType(String mimeType) {
		this.mimeType = mimeType;
		return this;
	}

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public DocumentDataEntity withFileName(String fileName) {
		this.fileName = fileName;
		return this;
	}

	public long getFileSizeInBytes() {
		return fileSizeInBytes;
	}

	public void setFileSizeInBytes(long fileSizeInBytes) {
		this.fileSizeInBytes = fileSizeInBytes;
	}

	public DocumentDataEntity withFileSizeInBytes(long fileSizeInBytes) {
		this.fileSizeInBytes = fileSizeInBytes;
		return this;
	}

	public String getStorageLocator() {
		return storageLocator;
	}

	public void setStorageLocator(String storageLocator) {
		this.storageLocator = storageLocator;
	}

	public DocumentDataEntity withStorageLocator(String storageLocator) {
		this.storageLocator = storageLocator;
		return this;
	}

	public String getContentHash() {
		return contentHash;
	}

	public void setContentHash(String contentHash) {
		this.contentHash = contentHash;
	}

	public DocumentDataEntity withContentHash(String contentHash) {
		this.contentHash = contentHash;
		return this;
	}

	public String getExtractedText() {
		return extractedText;
	}

	public void setExtractedText(String extractedText) {
		this.extractedText = extractedText;
	}

	public DocumentDataEntity withExtractedText(String extractedText) {
		this.extractedText = extractedText;
		return this;
	}

	public ExtractionStatus getExtractionStatus() {
		return extractionStatus;
	}

	public void setExtractionStatus(ExtractionStatus extractionStatus) {
		this.extractionStatus = extractionStatus;
	}

	public DocumentDataEntity withExtractionStatus(ExtractionStatus extractionStatus) {
		this.extractionStatus = extractionStatus;
		return this;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if ((o == null) || (getClass() != o.getClass())) {
			return false;
		}
		final DocumentDataEntity that = (DocumentDataEntity) o;
		return (fileSizeInBytes == that.fileSizeInBytes) && Objects.equals(id, that.id) && Objects.equals(mimeType, that.mimeType) && Objects.equals(fileName, that.fileName) &&
			Objects.equals(storageLocator, that.storageLocator) && Objects.equals(contentHash, that.contentHash) && Objects.equals(extractedText, that.extractedText) && extractionStatus == that.extractionStatus;
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, mimeType, fileName, fileSizeInBytes, storageLocator, contentHash, extractedText, extractionStatus);
	}

	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		builder.append("DocumentDataEntity [id=").append(id).append(", mimeType=").append(mimeType).append(", fileName=").append(fileName).append(", fileSizeInBytes=").append(fileSizeInBytes)
			.append(", storageLocator=").append(storageLocator).append(", contentHash=").append(contentHash).append(", extractedText=").append(extractedText).append(", extractionStatus=").append(extractionStatus).append("]");
		return builder.toString();
	}
}
