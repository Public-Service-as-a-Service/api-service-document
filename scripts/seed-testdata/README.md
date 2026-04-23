# seed-testdata

CLI that seeds a running `api-service-document` instance with realistic test
data. Intended for local development and manual QA — not used by any
automated test suite.

## What it does

1. **Document types** — creates a fixed set of types (idempotent; existing
   types are left alone).
2. **Documents** — per iteration:
   1. Per doc, rolls once against `--with-validity-rate` to decide whether
      this doc gets a `validFrom` / `validTo` window. Of those that do, ~70%
      get a currently-valid window (→ `ACTIVE` on publish) and ~30% get a
      future `validFrom` (→ `SCHEDULED` on publish). `validTo` is always
      future-dated so publish never gets rejected.
   2. `POST /{muni}/documents` with a generated PDF and faker-based metadata
      (creates revision 1 in status `DRAFT`).
   3. If `--files-per-doc` > 1, `PUT /{muni}/documents/{regnr}/files` adds the
      remaining PDFs (each call bumps the revision).
   4. With probability `--publish-rate`, `POST /{muni}/documents/{regnr}/publish`
      transitions the document to `ACTIVE` or `SCHEDULED` (see above).

Each generated PDF has a unique title, a unique faker-derived body, and a page
count sampled from `--pdf-pages`, so the corpus is meaningfully varied (not
just same-bytes-different-filename) and covers both small (~2 KB) and larger
(~10 KB+) documents.

`EXPIRED` and `REVOKED` documents cannot be produced through this script —
`EXPIRED` only comes from the wall-clock-driven `DocumentStatusScheduler`, and
`REVOKED` requires a separate `POST .../revoke` call (deliberately omitted;
seed output stays in the "just published" states).

## Prerequisites

- Node 20+
- A running `api-service-document` (default `http://localhost:8080`).

## Install

```bash
cd scripts/seed-testdata
npm install
npm run build
```

## Run

```bash
# Minimal: 10 documents in municipality 2281, mostly published.
node dist/index.js --count 10

# Fuller example — larger corpus, multi-file revisions, bigger PDFs,
# more docs with validity windows:
node dist/index.js \
  --count 200 \
  --municipality 2281 \
  --base-url http://localhost:8080 \
  --publish-rate 0.8 \
  --files-per-doc 1-3 \
  --pdf-pages 1-10 \
  --with-validity-rate 0.5

# Reproducible run (useful when you want identical data each time):
node dist/index.js --count 50 --seed 42 --created-by 6c3e4f5a-7b8d-4e9c-a1f2-d3e4b5c6a7f8
```

### Flags

| Flag | Default | Notes |
|------|---------|-------|
| `--count N` | **required** | Number of documents to create in this run. |
| `--municipality 2281` | `2281` | Target municipality. Must be a valid Swedish municipality code. |
| `--base-url URL` | `http://localhost:8080` | Base URL of the REST API, no trailing slash. |
| `--publish-rate 0.0–1.0` | `0.8` | Probability each created doc gets published. `1.0` = all, `0.0` = all stay `DRAFT`. |
| `--files-per-doc N` or `MIN-MAX` | `1` | Files per document. Range samples uniformly per doc. Each extra file bumps the revision. |
| `--pdf-pages N` or `MIN-MAX` | `1-5` | Pages per generated PDF. Larger values produce bigger files (~1 KB per page). |
| `--with-validity-rate 0.0–1.0` | `0.3` | Probability a doc gets `validFrom`/`validTo`. Of those, ~70% become `ACTIVE` on publish (past `validFrom`, far-future `validTo`) and ~30% become `SCHEDULED` (future `validFrom`). `EXPIRED` can't be produced through the API — the service rejects publishing a past `validTo`. |
| `--created-by UUID` | randomly generated | `createdBy` value used on all create calls. Pin a value to simulate "one user's documents". |
| `--seed N` | current time | Seed the faker RNG for reproducible output. |
| `--help` | — | Print usage and exit. |

### Idempotency

- Doctypes: GET-then-filter before creating; running the script many times
  doesn't produce duplicate type rows.
- Documents: each run adds fresh documents. The registration-number sequence
  just keeps bumping — that's the service's design.

## Layout

```
src/
├── index.ts      # CLI entry (parseArgs + usage + dispatch)
├── config.ts     # flag parsing + defaults + validation
├── client.ts     # fetch-based HTTP client, multipart-aware
├── pdf.ts        # pdfkit + faker — generates one text-only PDF
├── doctypes.ts   # idempotent doctype upsert
└── seed.ts       # main loop: create → add files → publish
```
