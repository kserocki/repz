import {
  type CatalogSeedConflict,
  planCatalogSeed,
  type StoredCatalogExercise,
} from "./catalog-seed.plan.ts";
import type { CatalogExercise } from "./snapshot.schema.ts";

export type CatalogSeedStore = {
  lockCatalogSyncWrites(): Promise<void>;
  /** `owner_id IS NULL`, including tombstones, with translations. */
  loadCatalog(): Promise<StoredCatalogExercise[]>;
  /** `owner_id IS NOT NULL` among `ids`. */
  findCustomIds(ids: string[]): Promise<string[]>;
  /** `version` from the column `DEFAULT`; `name` is the `en` translation; translations inserted. */
  insert(row: CatalogExercise): Promise<void>;
  /** `version = nextSyncVersion`, `updated_at = now()`; translations replaced. */
  update(row: CatalogExercise): Promise<void>;
  /** `deleted_at = now()`, version bump; translations hard-deleted. */
  tombstone(id: string): Promise<void>;
};

export type CatalogSeedTransactions = {
  write<T>(work: (store: CatalogSeedStore) => Promise<T>): Promise<T>;
};

export type CatalogSeedSummary = {
  inserted: number;
  updated: number;
  tombstoned: number;
  unchanged: number;
};

function describeConflict(conflict: CatalogSeedConflict): string {
  const ids = conflict.ids.join(", ");
  return conflict.kind === "revive"
    ? `catalog seed would revive tombstoned id(s): ${ids} (delete wins; re-add under a new id)`
    : `catalog seed id(s) already owned by a user's custom exercise: ${ids}`;
}

export class CatalogSeedConflictError extends Error {
  readonly conflict: CatalogSeedConflict;

  constructor(conflict: CatalogSeedConflict) {
    super(describeConflict(conflict));
    this.name = "CatalogSeedConflictError";
    this.conflict = conflict;
  }
}

/**
 * The seeder protocol (ADR-0021), inside one `CatalogSeedTransactions.write`:
 * 1. `lockCatalogSyncWrites` — the first call, always, so it waits for every in-flight push and
 *    blocks new ones (ADR-0020's catalog gate).
 * 2. Load the stored catalog and find which snapshot ids are owned by a user.
 * 3. `planCatalogSeed` (pure).
 * 4. A conflict throws before any write, in both modes. With `check`, a conflict-free plan is
 *    returned without writing — the caller (`seed.ts`) decides the exit code from the counts.
 * 5. Otherwise: inserts, then updates, then tombstones.
 */
export async function seedCatalog(
  transactions: CatalogSeedTransactions,
  snapshot: readonly CatalogExercise[],
  options: { check: boolean },
): Promise<CatalogSeedSummary> {
  return transactions.write(async (store) => {
    await store.lockCatalogSyncWrites();

    const stored = await store.loadCatalog();
    const customIds = await store.findCustomIds(snapshot.map((row) => row.id));

    const result = planCatalogSeed(snapshot, stored, customIds);
    if (!result.ok) {
      throw new CatalogSeedConflictError(result.conflict);
    }
    const { plan } = result;

    const summary: CatalogSeedSummary = {
      inserted: plan.inserts.length,
      updated: plan.updates.length,
      tombstoned: plan.tombstones.length,
      unchanged: plan.unchanged,
    };

    if (options.check) {
      return summary;
    }

    for (const row of plan.inserts) {
      await store.insert(row);
    }
    for (const row of plan.updates) {
      await store.update(row);
    }
    for (const id of plan.tombstones) {
      await store.tombstone(id);
    }

    return summary;
  });
}
