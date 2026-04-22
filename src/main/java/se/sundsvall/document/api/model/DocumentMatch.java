package se.sundsvall.document.api.model;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Objects;

import static io.swagger.v3.oas.annotations.media.Schema.AccessMode.READ_ONLY;

@Schema(description = "Document match — a document that contains one or more files matching a fulltext search.", accessMode = READ_ONLY)
public class DocumentMatch {

	@Schema(description = "ID of the matching document.", examples = "082ba08f-03c7-409f-b8a6-940a1397ba38")
	private String id;

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
		return Objects.hash(id, files);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) { return true; }
		if (!(obj instanceof final DocumentMatch other)) { return false; }
		return Objects.equals(id, other.id) && Objects.equals(files, other.files);
	}

	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		builder.append("DocumentMatch [id=").append(id).append(", files=").append(files).append("]");
		return builder.toString();
	}
}
