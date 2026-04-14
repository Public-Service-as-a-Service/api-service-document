package se.sundsvall.document.integration.db.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.util.Objects;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(name = "document_data")
public class DocumentDataEntity implements Serializable {

	private static final long serialVersionUID = -7783051635903859326L;

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

	@Column(name = "storage_backend", nullable = false, length = 16)
	@ColumnDefault("'jdbc'")
	private String storageBackend = "jdbc";

	@Column(name = "storage_locator")
	private String storageLocator;

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

	public String getStorageBackend() {
		return storageBackend;
	}

	public void setStorageBackend(String storageBackend) {
		this.storageBackend = storageBackend;
	}

	public DocumentDataEntity withStorageBackend(String storageBackend) {
		this.storageBackend = storageBackend;
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
			Objects.equals(storageBackend, that.storageBackend) && Objects.equals(storageLocator, that.storageLocator);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, mimeType, fileName, fileSizeInBytes, storageBackend, storageLocator);
	}

	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		builder.append("DocumentDataEntity [id=").append(id).append(", mimeType=").append(mimeType).append(", fileName=").append(fileName).append(", fileSizeInBytes=").append(fileSizeInBytes)
			.append(", storageBackend=").append(storageBackend).append(", storageLocator=").append(storageLocator).append("]");
		return builder.toString();
	}
}
