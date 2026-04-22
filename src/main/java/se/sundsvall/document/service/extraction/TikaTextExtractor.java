package se.sundsvall.document.service.extraction;

import java.io.InputStream;
import java.util.Set;
import org.apache.tika.exception.EncryptedDocumentException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.TikaCoreProperties;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.parser.ocr.TesseractOCRConfig;
import org.apache.tika.sax.BodyContentHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class TikaTextExtractor implements TextExtractor {

	private static final Logger LOGGER = LoggerFactory.getLogger(TikaTextExtractor.class);

	/** Cap the body handler so a pathological document can't blow the heap. */
	private static final int MAX_CHARS = 10_000_000;

	/**
	 * MIME prefixes we intentionally skip. Tika can detect these but indexing extracted "text"
	 * (which would be the base64-ish byte fragments of e.g. a PNG) is pointless noise.
	 */
	private static final Set<String> UNSUPPORTED_PREFIXES = Set.of(
		"image/",
		"audio/",
		"video/",
		"application/octet-stream");

	private final AutoDetectParser parser = new AutoDetectParser();

	@Override
	public ExtractedText extract(final InputStream bytes, final String contentType, final long sizeInBytes) {
		final var metadata = new Metadata();
		if (contentType != null && !contentType.isBlank()) {
			metadata.set(Metadata.CONTENT_TYPE, contentType);
		}
		final var handler = new BodyContentHandler(MAX_CHARS);
		final var parseContext = buildParseContext();

		try {
			parser.parse(bytes, handler, metadata, parseContext);
		} catch (final EncryptedDocumentException e) {
			return ExtractedText.failed(detectedMime(metadata), "Encrypted document");
		} catch (final Exception e) {
			LOGGER.warn("Tika extraction FAILED (declaredContentType='{}', detected='{}', size={}B, errorType={}): {}",
				contentType, detectedMime(metadata), sizeInBytes, e.getClass().getSimpleName(), e.getMessage());
			return ExtractedText.failed(detectedMime(metadata), e.getMessage());
		}

		final var detected = detectedMime(metadata);
		if (isUnsupported(detected)) {
			LOGGER.debug("Tika extraction skipped — unsupported mime (declaredContentType='{}', detected='{}', size={}B)",
				contentType, detected, sizeInBytes);
			return ExtractedText.unsupported(detected);
		}
		final var text = handler.toString();
		LOGGER.debug("Tika extracted {} chars (declaredContentType='{}', detected='{}', size={}B)",
			text.length(), contentType, detected, sizeInBytes);
		return ExtractedText.success(text, detected);
	}

	private static String detectedMime(final Metadata metadata) {
		return metadata.get(TikaCoreProperties.TYPE) != null
			? metadata.get(TikaCoreProperties.TYPE)
			: metadata.get(Metadata.CONTENT_TYPE);
	}

	private static boolean isUnsupported(final String mime) {
		if (mime == null) {
			return true;
		}
		return UNSUPPORTED_PREFIXES.stream().anyMatch(mime::startsWith);
	}

	private static ParseContext buildParseContext() {
		final var ctx = new ParseContext();
		// Disable Tesseract OCR — it would otherwise try to shell out to a binary that isn't installed
		// and pollute logs with warnings. OCR of images is explicitly out of scope.
		final var ocr = new TesseractOCRConfig();
		ocr.setSkipOcr(true);
		ctx.set(TesseractOCRConfig.class, ocr);
		ctx.set(Parser.class, new AutoDetectParser());
		return ctx;
	}
}
