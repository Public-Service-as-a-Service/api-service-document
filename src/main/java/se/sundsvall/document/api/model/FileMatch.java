package se.sundsvall.document.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static io.swagger.v3.oas.annotations.media.Schema.AccessMode.READ_ONLY;

@Schema(description = "File match — identifies a file that matched a fulltext search.", accessMode = READ_ONLY)
public class FileMatch {

	@Schema(description = "ID of the matching file.", examples = "082ba08f-03c7-409f-b8a6-940a1397ba38")
	private String id;

	@Schema(description = "File name.", examples = "my-file.pdf")
	private String fileName;

	@Schema(description = "Highlighted fragments grouped by matched field (e.g. extractedText, title, description, fileName). Matches are wrapped in <em>…</em>. Only fields with matches appear.")
	private Map<String, List<String>> highlights;

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

	public Map<String, List<String>> getHighlights() {
		return highlights;
	}

	public void setHighlights(Map<String, List<String>> highlights) {
		this.highlights = highlights;
	}

	public FileMatch withHighlights(Map<String, List<String>> highlights) {
		this.highlights = highlights;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, fileName, highlights);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) { return true; }
		if (!(obj instanceof final FileMatch other)) { return false; }
		return Objects.equals(id, other.id) && Objects.equals(fileName, other.fileName) && Objects.equals(highlights, other.highlights);
	}

	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		builder.append("FileMatch [id=").append(id).append(", fileName=").append(fileName).append(", highlights=").append(highlights).append("]");
		return builder.toString();
	}
}
