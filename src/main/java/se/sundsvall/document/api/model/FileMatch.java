package se.sundsvall.document.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Objects;

import static io.swagger.v3.oas.annotations.media.Schema.AccessMode.READ_ONLY;

@Schema(description = "File match — identifies a file that matched a fulltext search.", accessMode = READ_ONLY)
public class FileMatch {

	@Schema(description = "ID of the matching file.", examples = "082ba08f-03c7-409f-b8a6-940a1397ba38")
	private String id;

	@Schema(description = "File name.", examples = "my-file.pdf")
	private String fileName;

	public static FileMatch create() {
		return new FileMatch();
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public FileMatch withId(String id) {
		this.id = id;
		return this;
	}

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public FileMatch withFileName(String fileName) {
		this.fileName = fileName;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, fileName);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) { return true; }
		if (!(obj instanceof final FileMatch other)) { return false; }
		return Objects.equals(id, other.id) && Objects.equals(fileName, other.fileName);
	}

	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		builder.append("FileMatch [id=").append(id).append(", fileName=").append(fileName).append("]");
		return builder.toString();
	}
}
