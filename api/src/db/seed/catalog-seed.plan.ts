import type { CatalogExercise, SnapshotTranslation } from "./snapshot.schema.ts";

export type StoredCatalogExercise = CatalogExercise & { deleted: boolean };

export type CatalogSeedPlan = {
  inserts: CatalogExercise[];
  updates: CatalogExercise[];
  tombstones: string[];
  unchanged: number;
};

export type CatalogSeedConflict =
  | { kind: "revive"; ids: string[] } // the snapshot contains a tombstoned catalog id (ADR-0020: delete wins, no revive)
  | { kind: "id-owned-by-user"; ids: string[] }; // the snapshot id already exists as someone's custom exercise

export type CatalogSeedPlanResult =
  | { ok: true; plan: CatalogSeedPlan }
  | { ok: false; conflict: CatalogSeedConflict };

function sortedByLocale(translations: readonly SnapshotTranslation[]): SnapshotTranslation[] {
  return [...translations].sort((a, b) => a.locale.localeCompare(b.locale));
}

function translationsEqual(
  a: readonly SnapshotTranslation[],
  b: readonly SnapshotTranslation[],
): boolean {
  if (a.length !== b.length) {
    return false;
  }
  const sortedA = sortedByLocale(a);
  const sortedB = sortedByLocale(b);
  return sortedA.every((translation, index) => {
    const other = sortedB[index];
    return (
      other !== undefined &&
      translation.locale === other.locale &&
      translation.name === other.name &&
      translation.instructions === other.instructions
    );
  });
}

function arraysEqual<T>(a: readonly T[], b: readonly T[]): boolean {
  return a.length === b.length && a.every((value, index) => value === b[index]);
}

/**
 * Covers every field the seeder writes: primary/secondary muscles, equipment, licence, author,
 * `sourceUrl` and translations (order-independent). `name` is derived from the `en` translation,
 * so comparing translations already covers it.
 */
function contentEqual(stored: CatalogExercise, incoming: CatalogExercise): boolean {
  return (
    stored.primaryMuscle === incoming.primaryMuscle &&
    arraysEqual(stored.secondaryMuscles, incoming.secondaryMuscles) &&
    stored.equipment === incoming.equipment &&
    stored.license === incoming.license &&
    stored.author === incoming.author &&
    stored.sourceUrl === incoming.sourceUrl &&
    translationsEqual(stored.translations, incoming.translations)
  );
}

/**
 * Pure decision table (§3.8): `(snapshot, stored, customIds)` → a plan to apply, or the first
 * conflict found (an id-owned-by-user conflict is checked before a revive conflict; the two
 * never overlap, since a custom row and a catalog row never share scope). No side effects — the
 * caller applies the plan inside the locked transaction.
 */
export function planCatalogSeed(
  snapshot: readonly CatalogExercise[],
  stored: readonly StoredCatalogExercise[],
  customIds: readonly string[],
): CatalogSeedPlanResult {
  const storedById = new Map(stored.map((row) => [row.id, row]));
  const snapshotIds = new Set(snapshot.map((row) => row.id));
  const customIdSet = new Set(customIds);

  const ownedByUserIds: string[] = [];
  const reviveIds: string[] = [];
  const inserts: CatalogExercise[] = [];
  const updates: CatalogExercise[] = [];
  let unchanged = 0;

  for (const row of snapshot) {
    if (customIdSet.has(row.id)) {
      ownedByUserIds.push(row.id);
      continue;
    }
    const existing = storedById.get(row.id);
    if (!existing) {
      inserts.push(row);
    } else if (existing.deleted) {
      reviveIds.push(row.id);
    } else if (contentEqual(existing, row)) {
      unchanged += 1;
    } else {
      updates.push(row);
    }
  }

  if (ownedByUserIds.length > 0) {
    return { ok: false, conflict: { kind: "id-owned-by-user", ids: ownedByUserIds } };
  }
  if (reviveIds.length > 0) {
    return { ok: false, conflict: { kind: "revive", ids: reviveIds } };
  }

  const tombstones = stored
    .filter((row) => !row.deleted && !snapshotIds.has(row.id))
    .map((row) => row.id);

  return { ok: true, plan: { inserts, updates, tombstones, unchanged } };
}
