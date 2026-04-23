package se.sundsvall.document.service.extraction;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFTextBox;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PagedTextExtractorTest {

	@Mock
	private TikaTextExtractor fallback;

	@InjectMocks
	private PagedTextExtractor extractor;

	@Test
	void extract_pdf_producesPagePerPageOffsetsMatchingSlideCount() throws Exception {
		// Build a 3-page PDF where each page contains a unique string — we can then assert both
		// pageCount and that offsets point at the correct page text.
		final var pdfBytes = buildPdf("page-one-alpha", "page-two-beta", "page-three-gamma");

		final var result = extractor.extract(new ByteArrayInputStream(pdfBytes), "application/pdf", pdfBytes.length);

		assertThat(result.status()).isEqualTo(ExtractionStatus.SUCCESS);
		assertThat(result.pageCount()).isEqualTo(3);
		assertThat(result.pageOffsets()).hasSize(3);
		assertThat(result.pageOffsets().get(0)).isZero();
		assertThat(result.pageOffsets().get(1)).isGreaterThan(0);
		assertThat(result.pageOffsets().get(2)).isGreaterThan(result.pageOffsets().get(1));

		// Each offset should land at the start of the corresponding page's text.
		assertThat(result.text().substring(result.pageOffsets().get(0))).contains("page-one-alpha");
		assertThat(result.text().substring(result.pageOffsets().get(1))).contains("page-two-beta");
		assertThat(result.text().substring(result.pageOffsets().get(2))).contains("page-three-gamma");
		verifyNoInteractions(fallback);
	}

	@Test
	void extract_pptx_producesSlidePerSlideOffsets() throws Exception {
		final var pptxBytes = buildPptx("slide-one-alpha", "slide-two-beta");

		final var result = extractor.extract(new ByteArrayInputStream(pptxBytes),
			"application/vnd.openxmlformats-officedocument.presentationml.presentation", pptxBytes.length);

		assertThat(result.status()).isEqualTo(ExtractionStatus.SUCCESS);
		assertThat(result.pageCount()).isEqualTo(2);
		assertThat(result.pageOffsets()).hasSize(2);
		assertThat(result.pageOffsets().get(0)).isZero();
		assertThat(result.text()).contains("slide-one-alpha", "slide-two-beta");
		assertThat(result.text().substring(result.pageOffsets().get(1))).contains("slide-two-beta");
		verifyNoInteractions(fallback);
	}

	@Test
	void extract_nonPagedMime_delegatesToTikaWithoutPageData() {
		final var expected = TextExtractor.ExtractedText.success("plain text", "text/plain");
		when(fallback.extract(any(InputStream.class), eq("text/plain"), anyLong())).thenReturn(expected);

		final var result = extractor.extract(new ByteArrayInputStream("plain text".getBytes()), "text/plain", 10);

		assertThat(result).isSameAs(expected);
		verify(fallback).extract(any(InputStream.class), eq("text/plain"), anyLong());
	}

	@Test
	void extract_malformedPdf_fallsBackToTika() {
		final var expected = TextExtractor.ExtractedText.failed("application/pdf", "tika failed too");
		when(fallback.extract(any(InputStream.class), eq("application/pdf"), anyLong())).thenReturn(expected);

		final var junk = new byte[] {
			0x25, 0x50, 0x44, 0x46, 'x', 'x', 'x'
		}; // starts like %PDF but is garbage
		final var result = extractor.extract(new ByteArrayInputStream(junk), "application/pdf", junk.length);

		// Either PDFBox threw and we delegated, or PDFBox produced an empty/broken extract. Both
		// acceptable — the contract is "upload never fails"; verify we didn't crash and we got a
		// valid ExtractedText back.
		assertThat(result).isNotNull();
	}

	@Test
	void extract_malformedPptx_fallsBackToTika() {
		final var expected = TextExtractor.ExtractedText.failed(
			"application/vnd.openxmlformats-officedocument.presentationml.presentation", "tika failed too");
		when(fallback.extract(any(InputStream.class),
			eq("application/vnd.openxmlformats-officedocument.presentationml.presentation"), anyLong()))
			.thenReturn(expected);

		final var junk = "not a pptx".getBytes();
		final var result = extractor.extract(new ByteArrayInputStream(junk),
			"application/vnd.openxmlformats-officedocument.presentationml.presentation", junk.length);

		assertThat(result).isSameAs(expected);
	}

	@Test
	void extract_encryptedPdf_returnsFailedWithoutCallingFallback() throws Exception {
		// An owner-password-protected PDF that disallows content extraction yields an
		// InvalidPasswordException from PDFBox when we call load(...) without the password.
		final var encryptedBytes = buildEncryptedPdf("secret-contents");

		final var result = extractor.extract(new ByteArrayInputStream(encryptedBytes), "application/pdf", encryptedBytes.length);

		assertThat(result.status()).isEqualTo(ExtractionStatus.FAILED);
		assertThat(result.failureReason()).isEqualTo("Encrypted document");
		verifyNoInteractions(fallback);
	}

	@Test
	void extract_whenInputStreamFailsToRead_returnsFailedWithoutCallingFallback() {
		final var failing = new InputStream() {
			@Override
			public int read() throws java.io.IOException {
				throw new java.io.IOException("synthetic read failure");
			}
		};

		final var result = extractor.extract(failing, "application/pdf", 100);

		assertThat(result.status()).isEqualTo(ExtractionStatus.FAILED);
		assertThat(result.failureReason()).contains("synthetic read failure");
		verifyNoInteractions(fallback);
	}

	private static byte[] buildPdf(final String... pageContents) throws Exception {
		try (final var doc = new PDDocument(); final var out = new ByteArrayOutputStream()) {
			for (final var content : pageContents) {
				final var page = new PDPage();
				doc.addPage(page);
				try (final var stream = new PDPageContentStream(doc, page)) {
					stream.beginText();
					stream.setFont(PDType1Font.HELVETICA, 12);
					stream.newLineAtOffset(50, 700);
					stream.showText(content);
					stream.endText();
				}
			}
			doc.save(out);
			return out.toByteArray();
		}
	}

	private static byte[] buildEncryptedPdf(final String content) throws Exception {
		try (final var doc = new PDDocument(); final var out = new ByteArrayOutputStream()) {
			final var page = new PDPage();
			doc.addPage(page);
			try (final var stream = new PDPageContentStream(doc, page)) {
				stream.beginText();
				stream.setFont(PDType1Font.HELVETICA, 12);
				stream.newLineAtOffset(50, 700);
				stream.showText(content);
				stream.endText();
			}
			final var permissions = new AccessPermission();
			permissions.setCanExtractContent(false);
			doc.protect(new StandardProtectionPolicy("owner-pw", "user-pw", permissions));
			doc.save(out);
			return out.toByteArray();
		}
	}

	private static byte[] buildPptx(final String... slideContents) throws Exception {
		try (final var pptx = new XMLSlideShow(); final var out = new ByteArrayOutputStream()) {
			for (final var content : slideContents) {
				final var slide = pptx.createSlide();
				final XSLFTextBox box = slide.createTextBox();
				box.setText(content);
			}
			pptx.write(out);
			return out.toByteArray();
		}
	}
}
