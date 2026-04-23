package se.sundsvall.document.service.extraction;

import java.io.InputStream;
import java.util.List;

public interface TextExtractor {

	/**
	 * Extract textual content from `bytes`. Implementations MUST NOT throw for parse failures —
	 * they return an {@link ExtractedText} with the appropriate {@link ExtractionStatus} instead,
	 * so a bad file never breaks the upload path.
	 *
	 * @param bytes       the file bytes; implementation is free to fully consume and close
	 * @param contentType caller-supplied MIME type (hint only; Tika auto-detects regardless)
	 * @param sizeInBytes length of the stream
	 */
	ExtractedText extract(InputStream bytes, String contentType, long sizeInBytes);

	/**
	 * Outcome of an extraction attempt.
	 * <p>
	 * {@code pageCount} and {@code pageOffsets} are populated only for formats with a native page
	 * concept (PDF, PPTX) and only on success. For all other formats, and for non-success statuses,
	 * they are {@code null}. When populated, {@code pageOffsets.size() == pageCount} and each entry
	 * is the 0-based char offset into {@code text} where that page starts.
	 */
	record ExtractedText(String text, String detectedMimeType, ExtractionStatus status, String failureReason,
		Integer pageCount, List<Integer> pageOffsets) {

		public static ExtractedText success(String text, String detectedMimeType) {
			return new ExtractedText(text, detectedMimeType, ExtractionStatus.SUCCESS, null, null, null);
		}

		public static ExtractedText successWithPages(String text, String detectedMimeType, int pageCount, List<Integer> pageOffsets) {
			if (pageOffsets == null) {
				throw new IllegalArgumentException("pageOffsets must not be null for successWithPages — use success(...) for non-paged formats");
			}
			return new ExtractedText(text, detectedMimeType, ExtractionStatus.SUCCESS, null, pageCount, List.copyOf(pageOffsets));
		}

		public static ExtractedText unsupported(String detectedMimeType) {
			return new ExtractedText(null, detectedMimeType, ExtractionStatus.UNSUPPORTED, null, null, null);
		}

		public static ExtractedText failed(String detectedMimeType, String reason) {
			return new ExtractedText(null, detectedMimeType, ExtractionStatus.FAILED, reason, null, null);
		}
	}
}
