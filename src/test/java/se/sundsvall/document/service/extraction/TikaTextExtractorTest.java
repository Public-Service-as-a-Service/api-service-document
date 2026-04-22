package se.sundsvall.document.service.extraction;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TikaTextExtractorTest {

	private final TikaTextExtractor extractor = new TikaTextExtractor();

	@Test
	void extract_plainTextFile_returnsSuccessWithContent() throws Exception {
		final var bytes = Files.readAllBytes(Path.of("src/test/resources/files/readme.txt"));
		try (InputStream in = new ByteArrayInputStream(bytes)) {
			final var result = extractor.extract(in, "text/plain", bytes.length);

			assertThat(result.status()).isEqualTo(ExtractionStatus.SUCCESS);
			assertThat(result.text()).isNotBlank();
			assertThat(result.failureReason()).isNull();
		}
	}

	@Test
	void extract_pngImage_returnsUnsupported() throws Exception {
		try (InputStream in = new FileInputStream("src/test/resources/files/image.png")) {
			final var result = extractor.extract(in, "image/png", Files.size(Path.of("src/test/resources/files/image.png")));

			assertThat(result.status()).isEqualTo(ExtractionStatus.UNSUPPORTED);
			assertThat(result.text()).isNull();
			assertThat(result.detectedMimeType()).startsWith("image/");
		}
	}

	@Test
	void extract_garbageBytesWithUnknownType_detectedAsOctetStream_returnsUnsupported() throws Exception {
		// A few bytes with no recognizable magic number. Tika falls back to
		// application/octet-stream, which is on the UNSUPPORTED_PREFIXES list.
		final var garbage = new byte[] {
			0x01, 0x02, 0x03, 0x04, 0x05
		};
		try (InputStream in = new ByteArrayInputStream(garbage)) {
			final var result = extractor.extract(in, null, garbage.length);

			assertThat(result.status()).isEqualTo(ExtractionStatus.UNSUPPORTED);
			assertThat(result.detectedMimeType()).isEqualTo("application/octet-stream");
		}
	}

	@Test
	void extract_whenStreamThrowsIOException_returnsFailed() {
		final var failing = new InputStream() {
			@Override
			public int read() throws java.io.IOException {
				throw new java.io.IOException("synthetic stream failure");
			}
		};
		final var result = extractor.extract(failing, "text/plain", 100);

		assertThat(result.status()).isEqualTo(ExtractionStatus.FAILED);
		assertThat(result.failureReason()).contains("synthetic stream failure");
		assertThat(result.text()).isNull();
	}

	@Test
	void extractedText_factoryMethods_populateAllFields() {
		final var success = TextExtractor.ExtractedText.success("hello", "text/plain");
		assertThat(success.status()).isEqualTo(ExtractionStatus.SUCCESS);
		assertThat(success.text()).isEqualTo("hello");
		assertThat(success.detectedMimeType()).isEqualTo("text/plain");
		assertThat(success.failureReason()).isNull();

		final var unsupported = TextExtractor.ExtractedText.unsupported("image/png");
		assertThat(unsupported.status()).isEqualTo(ExtractionStatus.UNSUPPORTED);
		assertThat(unsupported.text()).isNull();
		assertThat(unsupported.detectedMimeType()).isEqualTo("image/png");
		assertThat(unsupported.failureReason()).isNull();

		final var failed = TextExtractor.ExtractedText.failed("application/pdf", "encrypted");
		assertThat(failed.status()).isEqualTo(ExtractionStatus.FAILED);
		assertThat(failed.text()).isNull();
		assertThat(failed.detectedMimeType()).isEqualTo("application/pdf");
		assertThat(failed.failureReason()).isEqualTo("encrypted");
	}
}
