package se.sundsvall.document.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Objects;

import static io.swagger.v3.oas.annotations.media.Schema.AccessMode.READ_ONLY;

@Schema(description = "Per-revision usage statistics for a document.", accessMode = READ_ONLY)
public class RevisionStatistics {

	@Schema(description = "Document revision number.", examples = "2")
	private int revision;

	@Schema(description = "Total download accesses across all files in this revision.", examples = "30")
	private long downloads;

	@Schema(description = "Total view accesses across all files in this revision.", examples = "50")
	private long views;

	@Schema(description = "Per-file breakdown.")
	private List<FileStatistics> perFile;

	public static RevisionStatistics create() {
		return new RevisionStatistics();
	}

	public int getRevision() {
		return revision;
	}

	public void setRevision(final int revision) {
		this.revision = revision;
	}

	public RevisionStatistics withRevision(final int revision) {
		this.revision = revision;
		return this;
	}

	public long getDownloads() {
		return downloads;
	}

	public void setDownloads(final long downloads) {
		this.downloads = downloads;
	}

	public RevisionStatistics withDownloads(final long downloads) {
		this.downloads = downloads;
		return this;
	}

	public long getViews() {
		return views;
	}

	public void setViews(final long views) {
		this.views = views;
	}

	public RevisionStatistics withViews(final long views) {
		this.views = views;
		return this;
	}

	public List<FileStatistics> getPerFile() {
		return perFile;
	}

	public void setPerFile(final List<FileStatistics> perFile) {
		this.perFile = perFile;
	}

	public RevisionStatistics withPerFile(final List<FileStatistics> perFile) {
		this.perFile = perFile;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(downloads, perFile, revision, views);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof final RevisionStatistics other)) {
			return false;
		}
		return downloads == other.downloads
			&& Objects.equals(perFile, other.perFile)
			&& revision == other.revision
			&& views == other.views;
	}

	@Override
	public String toString() {
		return "RevisionStatistics [revision=" + revision + ", downloads=" + downloads + ", views=" + views + ", perFile=" + perFile + "]";
	}
}
