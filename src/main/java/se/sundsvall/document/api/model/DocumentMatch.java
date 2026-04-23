package se.sundsvall.document.api.model;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Objects;

import static io.swagger.v3.oas.annotations.media.Schema.AccessMode.READ_ONLY;

@Schema(description = "Document match — a document that contains one or more files matching a fulltext search.", accessMode = READ_ONLY)
public class DocumentMatch {

	@Schema(description = "ID of the matching document (revision-specific, copy-on-write creates a new ID per revision).", examples = "082ba08f-03c7-409f-b8a6-940a1397ba38")
	private String id;

	@Schema(description = "Registration number of the matching document.", examples = "2023-2281-1337")
	private String registrationNumber;

	@Schema(description = "Revision that the matched files belong to.", examples = "3")
	private Integer revision;

	@ArraySchema(schema = @Schema(implementation = FileMatch.class))
	private List<FileMatch> files;

	public static DocumentMatch create() {
		return new DocumentMatch();
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public DocumentMatch withId(String id) {
		this.id = id;
		return this;
	}

	public String getRegistrationNumber() {
		return registrationNumber;
	}

	public void setRegistrationNumber(String registrationNumber) {
		this.registrationNumber = registrationNumber;
	}

	public DocumentMatch withRegistrationNumber(String registrationNumber) {
		this.registrationNumber = registrationNumber;
		return this;
	}

	public Integer getRevision() {
		return revision;
	}

	public void setRevision(Integer revision) {
		this.revision = revision;
	}

	public DocumentMatch withRevision(Integer revision) {
		this.revision = revision;
		return this;
	}

	public List<FileMatch> getFiles() {
		return files;
	}

	public void setFiles(List<FileMatch> files) {
		this.files = files;
	}

	public DocumentMatch withFiles(List<FileMatch> files) {
		this.files = files;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, registrationNumber, revision, files);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) { return true; }
		if (!(obj instanceof final DocumentMatch other)) { return false; }
		return Objects.equals(id, other.id)
			&& Objects.equals(registrationNumber, other.registrationNumber)
			&& Objects.equals(revision, other.revision)
			&& Objects.equals(files, other.files);
	}

	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		builder.append("DocumentMatch [id=").append(id)
			.append(", registrationNumber=").append(registrationNumber)
			.append(", revision=").append(revision)
			.append(", files=").append(files).append("]");
		return builder.toString();
	}
}
