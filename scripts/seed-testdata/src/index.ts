#!/usr/bin/env node
import { parseConfig } from "./config.js";
import { createClient } from "./client.js";
import { runSeed } from "./seed.js";

async function main(): Promise<void> {
  const config = parseConfig(process.argv.slice(2));
  const api = createClient(config.baseUrl);

  console.log(`seed-testdata → ${config.baseUrl} (muni ${config.municipalityId})`);
  console.log(
    `  count=${config.count}, publish-rate=${config.publishRate}, files-per-doc=${config.filesMin}-${config.filesMax}, ` +
      `pdf-pages=${config.pdfPagesMin}-${config.pdfPagesMax}, with-validity-rate=${config.withValidityRate}`,
  );

  const started = Date.now();
  await runSeed(api, config);
  console.log(`Elapsed: ${((Date.now() - started) / 1000).toFixed(1)}s`);
}

main().catch((err) => {
  console.error(err instanceof Error ? err.message : String(err));
  process.exit(1);
});
