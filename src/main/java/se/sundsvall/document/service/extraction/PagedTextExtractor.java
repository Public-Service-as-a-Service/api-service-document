package se.sundsvall.document.service.extraction;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFShape;
import org.apache.poi.xslf.usermodel.XSLFTextShape;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * Page-aware text extractor for formats with a native page concept (PDF, PPTX). For each supported
 * format, extracts text page-by-page so downstream code can resolve a char offset in
 * {@code extractedText} back to a page number — used by the search mapper to tell callers which
 * page a match falls on.
 * <p>
 * Delegates to {@link TikaTextExtractor} for every other format. If the per-page extractor
 * throws on a supported format, we fall back to Tika so the upload path still succeeds (we just
 * lose the page breakdown for that specific file).
 */
@Component
@Primary
public class PagedTextExtractor implements TextExtractor {

	private static final Logger LOGGER = LoggerFactory.getLogger(PagedTextExtractor.class);

	/**
	 * Matches the 10M-char cap used by TikaTextExtractor. We stop collecting per-page text past
	 * this threshold so a 10000-page PDF can't blow the heap; the remaining pages get excluded
	 * from search but the upload still succeeds with a truncated extract.
	 */
	private static final int MAX_CHARS = 10_000_000;

	private final TikaTextExtractor fallback;

	public PagedTextExtractor(final TikaTextExtractor fallback) {
		this.fallback = fallback;
	}

	@Override
	public ExtractedText extract(final InputStream bytes, final String contentType, final long sizeInBytes) {
		// Buffer once so both the paged path and the Tika fallback can read the same bytes — the
		// caller already committed the upload to storage, so we're not racing a deletion. Peak
		// memory is worse than TikaTextExtractor's streaming path (one full-file copy plus whatever
		// PDFBox/POI allocate), but both libraries require random access and can't stream. The
		// Spring multipart cap (60MB) bounds this per request.
		final byte[] buf;
		try {
			buf = bytes.readAllBytes();
		} catch (final IOException e) {
			return ExtractedText.failed(contentType, "Failed to buffer upload for extraction: " + e.getMessage());
		}

		if (PagedMimeTypes.PDF.equals(contentType)) {
			return extractPdf(buf, contentType, sizeInBytes);
		}
		if (PagedMimeTypes.PPTX.equals(contentType)) {
			return extractPptx(buf, contentType, sizeInBytes);
		}
		return fallback.extract(new ByteArrayInputStream(buf), contentType, sizeInBytes);
	}

	private ExtractedText extractPdf(final byte[] buf, final String contentType, final long sizeInBytes) {
		try (final var doc = PDDocument.load(buf)) {
			final var pageCount = doc.getNumberOfPages();
			final var sb = new StringBuilder();
			final var offsets = new ArrayList<Integer>(pageCount);
			final var stripper = new PDFTextStripper();
			var truncated = false;
			for (var i = 1; i <= pageCount; i++) {
				offsets.add(sb.length());
				if (sb.length() >= MAX_CHARS) {
					truncated = true;
					// Keep recording offsets so page count stays consistent; skip actual text.
					continue;
				}
				stripper.setStartPage(i);
				stripper.setEndPage(i);
				sb.append(stripper.getText(doc));
			}
			if (truncated) {
				LOGGER.warn("PDF extraction truncated at {} chars (pageCount={}, size={}B)",
					MAX_CHARS, pageCount, sizeInBytes);
			}
			LOGGER.debug("PDF extraction OK (pageCount={}, chars={}, size={}B)", pageCount, sb.length(), sizeInBytes);
			return ExtractedText.successWithPages(sb.toString(), contentType, pageCount, offsets);
		} catch (final InvalidPasswordException e) {
			return ExtractedText.failed(contentType, "Encrypted document");
		} catch (final Exception e) {
			LOGGER.warn("PDF per-page extraction failed (size={}B, errorType={}): {} — falling back to Tika",
				sizeInBytes, e.getClass().getSimpleName(), e.getMessage());
			return fallback.extract(new ByteArrayInputStream(buf), contentType, sizeInBytes);
		}
	}

	private ExtractedText extractPptx(final byte[] buf, final String contentType, final long sizeInBytes) {
		try (final var pptx = new XMLSlideShow(new ByteArrayInputStream(buf))) {
			final var slides = pptx.getSlides();
			final var pageCount = slides.size();
			final var sb = new StringBuilder();
			final var offsets = new ArrayList<Integer>(pageCount);
			var truncated = false;
			for (final var slide : slides) {
				offsets.add(sb.length());
				if (sb.length() >= MAX_CHARS) {
					truncated = true;
					continue;
				}
				for (final XSLFShape shape : slide.getShapes()) {
					if (shape instanceof final XSLFTextShape ts) {
						final var text = ts.getText();
						if (text != null && !text.isEmpty()) {
							sb.append(text).append('\n');
						}
					}
				}
			}
			if (truncated) {
				LOGGER.warn("PPTX extraction truncated at {} chars (slideCount={}, size={}B)",
					MAX_CHARS, pageCount, sizeInBytes);
			}
			LOGGER.debug("PPTX extraction OK (slideCount={}, chars={}, size={}B)", pageCount, sb.length(), sizeInBytes);
			return ExtractedText.successWithPages(sb.toString(), contentType, pageCount, offsets);
		} catch (final EncryptedDocumentException e) {
			return ExtractedText.failed(contentType, "Encrypted document");
		} catch (final Exception e) {
			LOGGER.warn("PPTX per-slide extraction failed (size={}B, errorType={}): {} — falling back to Tika",
				sizeInBytes, e.getClass().getSimpleName(), e.getMessage());
			return fallback.extract(new ByteArrayInputStream(buf), contentType, sizeInBytes);
		}
	}
}
