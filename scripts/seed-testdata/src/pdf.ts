import PDFDocument from "pdfkit";
import type { Faker } from "@faker-js/faker";

export type GeneratedFile = {
  filename: string;
  bytes: Uint8Array;
};

/**
 * Generate a small text-only PDF with faker-derived unique content. Each PDF
 * has a distinct title (used as the filename) and a distinct body, so the
 * resulting corpus exercises search/extraction with real variety rather than
 * identical bytes.
 */
export async function generatePdf(faker: Faker, index: number, numPages: number): Promise<GeneratedFile> {
  const title = `${faker.commerce.productName()} – ${faker.string.alphanumeric({ length: 6, casing: "upper" })}`;

  const doc = new PDFDocument({ size: "A4", margin: 60 });
  const chunks: Uint8Array[] = [];

  const done = new Promise<Uint8Array>((resolve, reject) => {
    doc.on("data", (chunk: Buffer) => chunks.push(new Uint8Array(chunk.buffer, chunk.byteOffset, chunk.byteLength)));
    doc.on("end", () => resolve(concatChunks(chunks)));
    doc.on("error", reject);
  });

  // Page 1 — header block
  doc.fontSize(18).text(title);
  doc.moveDown();
  doc.fontSize(11);
  doc.text(`${faker.lorem.sentence()} Reference: ${faker.string.nanoid(10)}.`);
  doc.moveDown();
  writeBodyForPage(doc, faker);

  // Additional pages
  for (let page = 2; page <= numPages; page++) {
    doc.addPage();
    doc.fontSize(14).text(`${faker.lorem.words({ min: 3, max: 6 })}`, { underline: false });
    doc.moveDown();
    doc.fontSize(11);
    writeBodyForPage(doc, faker);
  }

  // Closing block on the last page
  doc.moveDown();
  doc
    .fontSize(11)
    .text(`Undertecknat av ${faker.person.fullName()}, ${faker.location.city()}, ${faker.date.past().toISOString().slice(0, 10)}.`);
  doc.moveDown();
  doc
    .fontSize(9)
    .fillColor("gray")
    .text(`Generated document #${index} — ${numPages} page(s) — synthetic test data.`);

  doc.end();
  const bytes = await done;

  return {
    filename: sanitizeFilename(`${title}.pdf`),
    bytes,
  };
}

/** Fill the current page with a few paragraphs, staying conservatively within
 * one page so our explicit addPage() calls line up with actual page counts. */
function writeBodyForPage(doc: PDFKit.PDFDocument, faker: Faker): void {
  const paragraphs = faker.number.int({ min: 2, max: 4 });
  for (let i = 0; i < paragraphs; i++) {
    doc.text(faker.lorem.paragraph({ min: 3, max: 5 }));
    doc.moveDown();
  }
}

function sanitizeFilename(name: string): string {
  return name.replace(/[/\\?%*:|"<>]/g, "-").slice(0, 120);
}

function concatChunks(chunks: Uint8Array[]): Uint8Array {
  const total = chunks.reduce((sum, c) => sum + c.byteLength, 0);
  const out = new Uint8Array(total);
  let offset = 0;
  for (const c of chunks) {
    out.set(c, offset);
    offset += c.byteLength;
  }
  return out;
}
