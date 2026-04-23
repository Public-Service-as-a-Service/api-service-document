import type { ApiClient } from "./client.js";

export type DoctypeSpec = {
  type: string;
  displayName: string;
};

export const DOCTYPES: DoctypeSpec[] = [
  { type: "EMPLOYEE_CERTIFICATE", displayName: "Anställningsbevis" },
  { type: "ACTIONPLAN", displayName: "Handlingsplan" },
  { type: "COMMITTEEMINUTES", displayName: "Nämndsprotokoll" },
  { type: "POLICY", displayName: "Policy" },
  { type: "RULE", displayName: "Regel" },
  { type: "GUIDELINE", displayName: "Riktlinje" },
  { type: "ROUTINE", displayName: "Rutin" },
  { type: "STRATEGY", displayName: "Strategi" },
];

type ExistingType = { type: string };

/** Create the baseline doctypes that are missing in the target municipality. */
export async function upsertDoctypes(
  api: ApiClient,
  municipalityId: string,
  createdBy: string,
): Promise<{ created: string[]; skipped: string[] }> {
  const existing = await api.get<ExistingType[]>(`/${municipalityId}/admin/documenttypes`);
  const existingSet = new Set(existing.map((t) => t.type));

  const created: string[] = [];
  const skipped: string[] = [];

  for (const spec of DOCTYPES) {
    if (existingSet.has(spec.type)) {
      skipped.push(spec.type);
      continue;
    }
    await api.postJson(`/${municipalityId}/admin/documenttypes`, {
      type: spec.type,
      displayName: spec.displayName,
      createdBy,
    });
    created.push(spec.type);
  }

  return { created, skipped };
}
