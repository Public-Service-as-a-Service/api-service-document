package se.sundsvall.document.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Objects;

import static io.swagger.v3.oas.annotations.media.Schema.AccessMode.READ_ONLY;

@Schema(description = "A match location inside a file's extracted text. Offsets reference the "
	+ "Tika-extracted text stream (not the rendered document).", accessMode = READ_ONLY)
public class Match {

	@Schema(description = "Field the match occurred in.", examples = "extractedText")
	private String field;

	@Schema(description = "Start offset (inclusive, 0-based) into the extracted text.", examples = "1523")
	private Integer start;

	@Schema(description = "End offset (exclusive) into the extracted text.", examples = "1531")
	private Integer end;

	@Schema(description = "1-based page number the match falls on (PDF/PPTX only; null for formats without pages).",
		nullable = true,
		examples = "12")
	private Integer page;

	public static Match create() {
		return new Match();
	}

	public String getField() {
		return field;
	}

	public void setField(String field) {
		this.field = field;
	}

	public Match withField(String field) {
		this.field = field;
		return this;
	}

	public Integer getStart() {
		return start;
	}

	public void setStart(Integer start) {
		this.start = start;
	}

	public Match withStart(Integer start) {
		this.start = start;
		return this;
	}

	public Integer getEnd() {
		return end;
	}

	public void setEnd(Integer end) {
		this.end = end;
	}

	public Match withEnd(Integer end) {
		this.end = end;
		return this;
	}

	public Integer getPage() {
		return page;
	}

	public void setPage(Integer page) {
		this.page = page;
	}

	public Match withPage(Integer page) {
		this.page = page;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(field, start, end, page);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) { return true; }
		if (!(obj instanceof final Match other)) { return false; }
		return Objects.equals(field, other.field)
			&& Objects.equals(start, other.start)
			&& Objects.equals(end, other.end)
			&& Objects.equals(page, other.page);
	}

	@Override
	public String toString() {
		return "Match [field=" + field + ", start=" + start + ", end=" + end + ", page=" + page + "]";
	}
}
