package se.sundsvall.document.api.model;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import se.sundsvall.document.service.extraction.ExtractionStatus;

import static io.swagger.v3.oas.annotations.media.Schema.AccessMode.READ_ONLY;

@Schema(description = "File match — identifies a file that matched a fulltext search.", accessMode = READ_ONLY)
public class FileMatch {

	@Schema(description = "ID of the matching file.", examples = "082ba08f-03c7-409f-b8a6-940a1397ba38")
	private String id;

	@Schema(description = "File name.", examples = "my-file.pdf")
	private String fileName;

	@Schema(description = "Highlighted fragments grouped by matched field (e.g. extractedText, title, description, fileName). Matches are wrapped in <em>…</em>. Only fields with matches appear.")
	private Map<String, List<String>> highlights;

	@Schema(description = "Total page count for the file. Populated for PDF and PPTX; null for formats without pages "
		+ "or files not yet reprocessed by the page-extraction backfill.", nullable = true, examples = "42")
	private Integer pageCount;

	@ArraySchema(schema = @Schema(description = "Exact char-offset match locations inside the file's extracted text. "
		+ "Only emitted for the extractedText field. Empty list when the query matched no text content "
		+ "(e.g. title-only match) or when extraction failed.", implementation = Match.class))
	private List<Match> matches;

	@Schema(description = "Status of text extraction for this file — determines whether matches/highlights on "
		+ "extractedText are available.")
	private ExtractionStatus extractionStatus;

	@Schema(description = "Relevance score assigned by the search engine. Higher is more relevant.", examples = "7.42")
	private Float score;

	@Schema(description = "Confidentiality flag for the parent document revision.", examples = "false")
	private Boolean confidential;

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

	public Integer getPageCount() {
		return pageCount;
	}

	public void setPageCount(Integer pageCount) {
		this.pageCount = pageCount;
	}

	public FileMatch withPageCount(Integer pageCount) {
		this.pageCount = pageCount;
		return this;
	}

	public List<Match> getMatches() {
		return matches;
	}

	public void setMatches(List<Match> matches) {
		this.matches = matches;
	}

	public FileMatch withMatches(List<Match> matches) {
		this.matches = matches;
		return this;
	}

	public ExtractionStatus getExtractionStatus() {
		return extractionStatus;
	}

	public void setExtractionStatus(ExtractionStatus extractionStatus) {
		this.extractionStatus = extractionStatus;
	}

	public FileMatch withExtractionStatus(ExtractionStatus extractionStatus) {
		this.extractionStatus = extractionStatus;
		return this;
	}

	public Float getScore() {
		return score;
	}

	public void setScore(Float score) {
		this.score = score;
	}

	public FileMatch withScore(Float score) {
		this.score = score;
		return this;
	}

	public Boolean getConfidential() {
		return confidential;
	}

	public void setConfidential(Boolean confidential) {
		this.confidential = confidential;
	}

	public FileMatch withConfidential(Boolean confidential) {
		this.confidential = confidential;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, fileName, highlights, pageCount, matches, extractionStatus, score, confidential);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) { return true; }
		if (!(obj instanceof final FileMatch other)) { return false; }
		return Objects.equals(id, other.id)
			&& Objects.equals(fileName, other.fileName)
			&& Objects.equals(highlights, other.highlights)
			&& Objects.equals(pageCount, other.pageCount)
			&& Objects.equals(matches, other.matches)
			&& Objects.equals(extractionStatus, other.extractionStatus)
			&& Objects.equals(score, other.score)
			&& Objects.equals(confidential, other.confidential);
	}

	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		builder.append("FileMatch [id=").append(id)
			.append(", fileName=").append(fileName)
			.append(", highlights=").append(highlights)
			.append(", pageCount=").append(pageCount)
			.append(", matches=").append(matches)
			.append(", extractionStatus=").append(extractionStatus)
			.append(", score=").append(score)
			.append(", confidential=").append(confidential)
			.append("]");
		return builder.toString();
	}
}
