package se.sundsvall.document.service.extraction;

import java.util.List;

/**
 * MIME types that have a native page concept and for which {@link PagedTextExtractor} produces
 * per-page offset data. Centralised so the extractor and the reindex scheduler can't drift apart —
 * adding a new paged format (e.g. ODT) only touches this file plus the extractor's dispatch.
 */
public final class PagedMimeTypes {

	public static final String PDF = "application/pdf";
	public static final String PPTX = "application/vnd.openxmlformats-officedocument.presentationml.presentation";

	/** All paged MIME types, ordered as the extractor's dispatch. */
	public static final List<String> ALL = List.of(PDF, PPTX);

	private PagedMimeTypes() {}
}
