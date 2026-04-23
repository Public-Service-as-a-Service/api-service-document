import { parseArgs } from "node:util";
import { randomUUID } from "node:crypto";

export type Config = {
  count: number;
  municipalityId: string;
  baseUrl: string;
  publishRate: number;
  filesMin: number;
  filesMax: number;
  pdfPagesMin: number;
  pdfPagesMax: number;
  withValidityRate: number;
  createdBy: string;
  seed: number;
};

const USAGE = `seed-testdata — populate api-service-document with test data

Usage:
  node dist/index.js --count N [options]

Options:
  --count N                    Number of documents to create (required).
  --municipality ID            Municipality ID (default: 2281).
  --base-url URL               Base URL of the API (default: http://localhost:8080).
  --publish-rate 0.0-1.0       Fraction of docs to publish (default: 0.8).
  --files-per-doc N|MIN-MAX    Files per document, 1 or range (default: 1).
  --pdf-pages N|MIN-MAX        Pages per generated PDF, 1 or range (default: 1-5).
  --with-validity-rate 0.0-1.0 Fraction of docs given validFrom/validTo
                               (default: 0.3). Of those: ~70% ACTIVE, ~30% SCHEDULED.
  --created-by UUID            UUID used as createdBy (default: random).
  --seed N                     Faker RNG seed for reproducibility (default: time).
  --help                       Print this help and exit.
`;

export function parseConfig(argv: string[]): Config {
  const { values } = parseArgs({
    args: argv,
    options: {
      count: { type: "string" },
      municipality: { type: "string" },
      "base-url": { type: "string" },
      "publish-rate": { type: "string" },
      "files-per-doc": { type: "string" },
      "pdf-pages": { type: "string" },
      "with-validity-rate": { type: "string" },
      "created-by": { type: "string" },
      seed: { type: "string" },
      help: { type: "boolean", short: "h" },
    },
    allowPositionals: false,
    strict: true,
  });

  if (values.help) {
    process.stdout.write(USAGE);
    process.exit(0);
  }

  if (values.count === undefined) {
    fail("--count is required. Use --help for usage.");
  }

  const count = parseIntStrict("--count", values.count);
  if (count <= 0) fail("--count must be a positive integer.");

  const municipalityId = values.municipality ?? "2281";
  const baseUrl = (values["base-url"] ?? "http://localhost:8080").replace(/\/+$/, "");

  const publishRate = values["publish-rate"] !== undefined ? Number(values["publish-rate"]) : 0.8;
  if (!Number.isFinite(publishRate) || publishRate < 0 || publishRate > 1) {
    fail("--publish-rate must be between 0.0 and 1.0.");
  }

  const { min: filesMin, max: filesMax } = parseIntRange("--files-per-doc", values["files-per-doc"] ?? "1");
  const { min: pdfPagesMin, max: pdfPagesMax } = parseIntRange("--pdf-pages", values["pdf-pages"] ?? "1-5");

  const withValidityRate = values["with-validity-rate"] !== undefined ? Number(values["with-validity-rate"]) : 0.3;
  if (!Number.isFinite(withValidityRate) || withValidityRate < 0 || withValidityRate > 1) {
    fail("--with-validity-rate must be between 0.0 and 1.0.");
  }

  const createdBy = values["created-by"] ?? randomUUID();

  const seed = values.seed !== undefined ? parseIntStrict("--seed", values.seed) : Date.now();

  return {
    count,
    municipalityId,
    baseUrl,
    publishRate,
    filesMin,
    filesMax,
    pdfPagesMin,
    pdfPagesMax,
    withValidityRate,
    createdBy,
    seed,
  };
}

function parseIntRange(flag: string, raw: string): { min: number; max: number } {
  const range = raw.match(/^(\d+)-(\d+)$/);
  if (range) {
    const min = Number(range[1]);
    const max = Number(range[2]);
    if (min <= 0 || max < min) fail(`${flag} range must be MIN-MAX with 1 ≤ MIN ≤ MAX. Got '${raw}'.`);
    return { min, max };
  }
  const single = Number(raw);
  if (!Number.isInteger(single) || single <= 0) fail(`${flag} must be a positive integer or MIN-MAX range. Got '${raw}'.`);
  return { min: single, max: single };
}

function parseIntStrict(flag: string, raw: string): number {
  const value = Number(raw);
  if (!Number.isInteger(value)) fail(`${flag} must be an integer. Got '${raw}'.`);
  return value;
}

function fail(message: string): never {
  process.stderr.write(`${message}\n`);
  process.exit(2);
}
