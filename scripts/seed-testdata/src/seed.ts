import { fakerSV as faker } from "@faker-js/faker";
import type { ApiClient } from "./client.js";
import type { Config } from "./config.js";
import { DOCTYPES, upsertDoctypes } from "./doctypes.js";
import { generatePdf, type GeneratedFile } from "./pdf.js";

export async function runSeed(api: ApiClient, config: Config): Promise<void> {
  faker.seed(config.seed);

  console.log(`Using createdBy=${config.createdBy}, seed=${config.seed}`);

  const { created, skipped } = await upsertDoctypes(api, config.municipalityId, config.createdBy);
  console.log(`Doctypes: created=${created.length} [${created.join(", ")}], skipped=${skipped.length}`);

  let published = 0;
  let draft = 0;
  let withValidity = 0;
  let totalBytes = 0;

  for (let i = 0; i < config.count; i++) {
    const filesForThisDoc = randomInt(faker, config.filesMin, config.filesMax);
    const validity = rollValidity(faker, config);
    if (validity) withValidity++;

    const bytesForThisDoc = await createDocument(api, config, i, filesForThisDoc, validity);
    totalBytes += bytesForThisDoc.bytes;
    const registrationNumber = bytesForThisDoc.registrationNumber;

    if (filesForThisDoc > 1) {
      totalBytes += await addExtraFiles(api, config, registrationNumber, filesForThisDoc - 1, i);
    }

    if (faker.number.float({ min: 0, max: 1 }) < config.publishRate) {
      await api.postJson(
        `/${config.municipalityId}/documents/${encodeURIComponent(registrationNumber)}/publish?updatedBy=${encodeURIComponent(config.createdBy)}`,
        undefined,
      );
      published++;
    } else {
      draft++;
    }

    if ((i + 1) % 10 === 0 || i + 1 === config.count) {
      console.log(`  progress: ${i + 1}/${config.count} (published=${published}, draft=${draft}, withValidity=${withValidity})`);
    }
  }

  console.log(
    `Done. ${config.count} documents — ${published} published, ${draft} DRAFT, ${withValidity} with validity dates. ` +
      `Total uploaded: ${(totalBytes / 1024).toFixed(1)} KiB.`,
  );
}

type Validity = { validFrom: string; validTo: string } | undefined;

function rollValidity(rng: typeof faker, config: Config): Validity {
  if (rng.number.float({ min: 0, max: 1 }) >= config.withValidityRate) return undefined;

  const today = new Date();
  today.setHours(0, 0, 0, 0);

  // 70% "currently valid" → becomes ACTIVE on publish.
  // 30% "future-dated"   → becomes SCHEDULED on publish.
  const futureScheduled = rng.number.float({ min: 0, max: 1 }) < 0.3;

  if (futureScheduled) {
    const validFrom = addDays(today, rng.number.int({ min: 30, max: 180 }));
    const validTo = addDays(validFrom, rng.number.int({ min: 365, max: 3 * 365 }));
    return { validFrom: toIsoDate(validFrom), validTo: toIsoDate(validTo) };
  } else {
    const validFrom = addDays(today, -rng.number.int({ min: 30, max: 2 * 365 }));
    const validTo = addDays(today, rng.number.int({ min: 180, max: 5 * 365 }));
    return { validFrom: toIsoDate(validFrom), validTo: toIsoDate(validTo) };
  }
}

function addDays(base: Date, days: number): Date {
  const out = new Date(base);
  out.setDate(out.getDate() + days);
  return out;
}

function toIsoDate(date: Date): string {
  const y = date.getFullYear();
  const m = String(date.getMonth() + 1).padStart(2, "0");
  const d = String(date.getDate()).padStart(2, "0");
  return `${y}-${m}-${d}`;
}

async function createDocument(
  api: ApiClient,
  config: Config,
  index: number,
  filesForThisDoc: number,
  validity: Validity,
): Promise<{ registrationNumber: string; bytes: number }> {
  const doctype = faker.helpers.arrayElement(DOCTYPES);
  const pages = randomInt(faker, config.pdfPagesMin, config.pdfPagesMax);
  const firstPdf = await generatePdf(faker, index, pages);

  const documentJson: Record<string, unknown> = {
    createdBy: config.createdBy,
    title: firstPdf.filename.replace(/\.pdf$/, ""),
    description: faker.lorem.sentences(2),
    type: doctype.type,
    archive: false,
    confidentiality: {
      confidential: faker.number.float({ min: 0, max: 1 }) < 0.1,
      legalCitation: null,
    },
    metadataList: [],
    responsibilities: [],
  };
  if (validity) {
    documentJson["validFrom"] = validity.validFrom;
    documentJson["validTo"] = validity.validTo;
  }

  const res = await api.postMultipartVoid(`/${config.municipalityId}/documents`, [
    {
      name: "document",
      filename: "document.json",
      contentType: "application/json",
      body: JSON.stringify(documentJson),
    },
    pdfPart(firstPdf),
  ]);

  const regnr = extractRegistrationNumber(res.location);
  if (!regnr) {
    throw new Error(`Could not parse registrationNumber from Location header: '${res.location}'`);
  }
  return { registrationNumber: regnr, bytes: firstPdf.bytes.byteLength };
}

async function addExtraFiles(
  api: ApiClient,
  config: Config,
  registrationNumber: string,
  extraCount: number,
  docIndex: number,
): Promise<number> {
  let bytes = 0;
  for (let i = 0; i < extraCount; i++) {
    const pages = randomInt(faker, config.pdfPagesMin, config.pdfPagesMax);
    const pdf = await generatePdf(faker, docIndex * 1000 + i + 1, pages);
    bytes += pdf.bytes.byteLength;
    const documentDataJson = {
      createdBy: config.createdBy,
    };
    await api.putMultipartVoid(
      `/${config.municipalityId}/documents/${encodeURIComponent(registrationNumber)}/files`,
      [
        {
          name: "document",
          filename: "document-data.json",
          contentType: "application/json",
          body: JSON.stringify(documentDataJson),
        },
        pdfPart(pdf),
      ],
    );
  }
  return bytes;
}

function pdfPart(file: GeneratedFile) {
  return {
    name: "documentFiles",
    filename: file.filename,
    contentType: "application/pdf",
    body: file.bytes,
  };
}

function extractRegistrationNumber(location: string | undefined): string | undefined {
  if (!location) return undefined;
  // Location: /{muni}/documents/{regnr}
  const match = location.match(/\/documents\/([^/?#]+)/);
  return match?.[1];
}

function randomInt(rng: typeof faker, min: number, max: number): number {
  return rng.number.int({ min, max });
}
