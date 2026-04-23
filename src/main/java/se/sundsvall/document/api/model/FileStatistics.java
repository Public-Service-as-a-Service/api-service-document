package se.sundsvall.document.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Objects;

import static io.swagger.v3.oas.annotations.media.Schema.AccessMode.READ_ONLY;

@Schema(description = "Per-file usage statistics within a document revision.", accessMode = READ_ONLY)
public class FileStatistics {

	@Schema(description = "Document data ID.", examples = "082ba08f-03c7-409f-b8a6-940a1397ba38")
	private String documentDataId;

	@Schema(description = "File name. May be null if the file no longer exists in any revision of the document.", examples = "report.pdf")
	private String fileName;

	@Schema(description = "Number of download accesses (Content-Disposition: attachment).", examples = "20")
	private long downloads;

	@Schema(description = "Number of view accesses (Content-Disposition: inline).", examples = "35")
	private long views;

	public static FileStatistics create() {
		return new FileStatistics();
	}

	public String getDocumentDataId() {
		return documentDataId;
	}

	public void setDocumentDataId(final String documentDataId) {
		this.documentDataId = documentDataId;
	}

	public FileStatistics withDocumentDataId(final String documentDataId) {
		this.documentDataId = documentDataId;
		return this;
	}

	public String getFileName() {
		return fileName;
	}

	public void setFileName(final String fileName) {
		this.fileName = fileName;
	}

	public FileStatistics withFileName(final String fileName) {
		this.fileName = fileName;
		return this;
	}

	public long getDownloads() {
		return downloads;
	}

	public void setDownloads(final long downloads) {
		this.downloads = downloads;
	}

	public FileStatistics withDownloads(final long downloads) {
		this.downloads = downloads;
		return this;
	}

	public long getViews() {
		return views;
	}

	public void setViews(final long views) {
		this.views = views;
	}

	public FileStatistics withViews(final long views) {
		this.views = views;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(documentDataId, downloads, fileName, views);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof final FileStatistics other)) {
			return false;
		}
		return Objects.equals(documentDataId, other.documentDataId)
			&& downloads == other.downloads
			&& Objects.equals(fileName, other.fileName)
			&& views == other.views;
	}

	@Override
	public String toString() {
		return "FileStatistics [documentDataId=" + documentDataId + ", fileName=" + fileName + ", downloads=" + downloads + ", views=" + views + "]";
	}
}
