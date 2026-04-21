package se.sundsvall.document.api.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Objects;
import se.sundsvall.dept44.models.api.paging.PagingMetaData;

import static io.swagger.v3.oas.annotations.media.Schema.AccessMode.READ_ONLY;

@Schema(description = "Paged document match response — documents that matched a fulltext search, stripped to the matching files.", accessMode = READ_ONLY)
public class PagedDocumentMatchResponse {

	@ArraySchema(schema = @Schema(implementation = DocumentMatch.class))
	private List<DocumentMatch> documents;

	@JsonProperty("_meta")
	@Schema(implementation = PagingMetaData.class)
	private PagingMetaData metadata;

	public static PagedDocumentMatchResponse create() {
		return new PagedDocumentMatchResponse();
	}

	public List<DocumentMatch> getDocuments() {
		return documents;
	}

	public void setDocuments(List<DocumentMatch> documents) {
		this.documents = documents;
	}

	public PagedDocumentMatchResponse withDocuments(List<DocumentMatch> documents) {
		this.documents = documents;
		return this;
	}

	public PagingMetaData getMetadata() {
		return metadata;
	}

	public void setMetadata(PagingMetaData metadata) {
		this.metadata = metadata;
	}

	public PagedDocumentMatchResponse withMetaData(PagingMetaData metadata) {
		this.metadata = metadata;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(documents, metadata);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) { return true; }
		if (!(obj instanceof final PagedDocumentMatchResponse other)) { return false; }
		return Objects.equals(documents, other.documents) && Objects.equals(metadata, other.metadata);
	}

	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		builder.append("PagedDocumentMatchResponse [documents=").append(documents).append(", metadata=").append(metadata).append("]");
		return builder.toString();
	}
}
