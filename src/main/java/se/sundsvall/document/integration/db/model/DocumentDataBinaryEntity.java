package se.sundsvall.document.integration.db.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import java.sql.Blob;
import java.util.Objects;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(name = "document_data_binary")
public class DocumentDataBinaryEntity {

	@Id
	@UuidGenerator
	@Column(name = "id")
	private String id;

	@Lob
	@Column(name = "binary_file", columnDefinition = "longblob")
	private Blob binaryFile;

	public static DocumentDataBinaryEntity create() {
		return new DocumentDataBinaryEntity();
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public DocumentDataBinaryEntity withId(String id) {
		this.id = id;
		return this;
	}

	public Blob getBinaryFile() {
		return binaryFile;
	}

	public void setBinaryFile(Blob binaryFile) {
		this.binaryFile = binaryFile;
	}

	public DocumentDataBinaryEntity withBinaryFile(Blob binaryFile) {
		this.binaryFile = binaryFile;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(binaryFile, id);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) { return true; }
		if (!(obj instanceof final DocumentDataBinaryEntity other)) { return false; }
		return Objects.equals(binaryFile, other.binaryFile) && Objects.equals(id, other.id);
	}

	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		builder.append("DocumentDataBinaryEntity [id=").append(id).append(", binaryFile=").append(binaryFile).append("]");
		return builder.toString();
	}
}
