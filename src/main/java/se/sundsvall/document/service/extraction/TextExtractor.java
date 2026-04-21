package se.sundsvall.document.service.extraction;

import java.io.InputStream;

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

	record ExtractedText(String text, String detectedMimeType, ExtractionStatus status, String failureReason) {
		public static ExtractedText success(String text, String detectedMimeType) {
			return new ExtractedText(text, detectedMimeType, ExtractionStatus.SUCCESS, null);
		}

		public static ExtractedText unsupported(String detectedMimeType) {
			return new ExtractedText(null, detectedMimeType, ExtractionStatus.UNSUPPORTED, null);
		}

		public static ExtractedText failed(String detectedMimeType, String reason) {
			return new ExtractedText(null, detectedMimeType, ExtractionStatus.FAILED, reason);
		}
	}
}
