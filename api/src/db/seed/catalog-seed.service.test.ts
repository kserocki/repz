import { describe, expect, it } from "vitest";
import type { StoredCatalogExercise } from "./catalog-seed.plan.ts";
import {
  CatalogSeedConflictError,
  type CatalogSeedStore,
  type CatalogSeedTransactions,
  seedCatalog,
} from "./catalog-seed.service.ts";
import type { CatalogExercise } from "./snapshot.schema.ts";

function exercise(overrides: Partial<CatalogExercise> = {}): CatalogExercise {
  return {
    id: "b6e6b6a0-0000-4000-8000-000000000001",
    primaryMuscle: "chest",
    secondaryMuscles: [],
    equipment: "barbell",
    license: "CC-BY-SA 3.0",
    author: "sistab2",
    sourceUrl: "https://wger.de/exercise/73/view-base",
    translations: [{ locale: "en", name: "Bench Press", instructions: null }],
    ...overrides,
  };
}

/** Records every call in order, so protocol-order and "never writes" assertions are precise. */
function createRecordingFakeStore(
  initialCatalog: StoredCatalogExercise[] = [],
  customIds: string[] = [],
) {
  const catalog = new Map(initialCatalog.map((row) => [row.id, row]));
  const calls: string[] = [];
  const inserted: CatalogExercise[] = [];
  const updated: CatalogExercise[] = [];
  const tombstoned: string[] = [];

  const store: CatalogSeedStore = {
    async lockCatalogSyncWrites() {
      calls.push("lockCatalogSyncWrites");
    },
    async loadCatalog() {
      calls.push("loadCatalog");
      return [...catalog.values()];
    },
    async findCustomIds(ids) {
      calls.push("findCustomIds");
      return ids.filter((id) => customIds.includes(id));
    },
    async insert(row) {
      calls.push(`insert:${row.id}`);
      inserted.push(row);
      catalog.set(row.id, { ...row, deleted: false });
    },
    async update(row) {
      calls.push(`update:${row.id}`);
      updated.push(row);
      catalog.set(row.id, { ...row, deleted: false });
    },
    async tombstone(id) {
      calls.push(`tombstone:${id}`);
      tombstoned.push(id);
      const existing = catalog.get(id);
      if (existing) {
        catalog.set(id, { ...existing, deleted: true });
      }
    },
  };

  const transactions: CatalogSeedTransactions = { write: (work) => work(store) };

  return { transactions, calls, inserted, updated, tombstoned };
}

describe("seedCatalog protocol order", () => {
  it("given a seed run, when it runs, then lockCatalogSyncWrites is the first call", async () => {
    const { transactions, calls } = createRecordingFakeStore();

    await seedCatalog(transactions, [exercise()], { check: false });

    expect(calls[0]).toBe("lockCatalogSyncWrites");
  });

  it("given inserts, updates and tombstones in one run, when applied, then they happen in that order", async () => {
    const toUpdate = exercise({ id: "id-update", equipment: "dumbbell" });
    const { transactions, calls } = createRecordingFakeStore(
      [
        { ...exercise({ id: "id-update" }), deleted: false },
        { ...exercise({ id: "id-tombstone" }), deleted: false },
      ],
      [],
    );

    await seedCatalog(transactions, [exercise({ id: "id-insert" }), toUpdate], { check: false });

    const insertIndex = calls.indexOf("insert:id-insert");
    const updateIndex = calls.indexOf("update:id-update");
    const tombstoneIndex = calls.indexOf("tombstone:id-tombstone");
    expect(insertIndex).toBeGreaterThan(-1);
    expect(updateIndex).toBeGreaterThan(insertIndex);
    expect(tombstoneIndex).toBeGreaterThan(updateIndex);
  });
});

describe("seedCatalog check mode", () => {
  it("given a check run with pending changes, when it runs, then nothing is written but the counts reflect what would happen", async () => {
    const { transactions, inserted, updated, tombstoned } = createRecordingFakeStore(
      [{ ...exercise({ id: "id-tombstone" }), deleted: false }],
      [],
    );

    const summary = await seedCatalog(transactions, [exercise({ id: "id-insert" })], {
      check: true,
    });

    expect(summary).toEqual({ inserted: 1, updated: 0, tombstoned: 1, unchanged: 0 });
    expect(inserted).toEqual([]);
    expect(updated).toEqual([]);
    expect(tombstoned).toEqual([]);
  });

  it("given a check run with nothing pending, when it runs, then every count is zero", async () => {
    const { transactions } = createRecordingFakeStore([{ ...exercise(), deleted: false }], []);

    const summary = await seedCatalog(transactions, [exercise()], { check: true });

    expect(summary).toEqual({ inserted: 0, updated: 0, tombstoned: 0, unchanged: 1 });
  });
});

describe("seedCatalog conflicts", () => {
  it("given a revive conflict, when seeded, then it throws before any write", async () => {
    const { transactions, inserted, updated, tombstoned } = createRecordingFakeStore(
      [{ ...exercise(), deleted: true }],
      [],
    );

    await expect(seedCatalog(transactions, [exercise()], { check: false })).rejects.toThrow(
      CatalogSeedConflictError,
    );
    expect(inserted).toEqual([]);
    expect(updated).toEqual([]);
    expect(tombstoned).toEqual([]);
  });

  it("given an id-owned-by-user conflict, when seeded, then it throws before any write, even in check mode", async () => {
    const { transactions, inserted } = createRecordingFakeStore([], [exercise().id]);

    await expect(seedCatalog(transactions, [exercise()], { check: true })).rejects.toThrow(
      CatalogSeedConflictError,
    );
    expect(inserted).toEqual([]);
  });

  it("given a revive conflict, when the error is inspected, then it names the conflicting id", async () => {
    const { transactions } = createRecordingFakeStore([{ ...exercise(), deleted: true }], []);

    try {
      await seedCatalog(transactions, [exercise()], { check: false });
      expect.unreachable("seedCatalog should have thrown");
    } catch (error) {
      expect(error).toBeInstanceOf(CatalogSeedConflictError);
      expect((error as CatalogSeedConflictError).conflict).toEqual({
        kind: "revive",
        ids: [exercise().id],
      });
    }
  });
});

describe("seedCatalog counts", () => {
  it("given one insert, one update and one tombstone, when seeded, then the summary counts match", async () => {
    const { transactions } = createRecordingFakeStore(
      [
        { ...exercise({ id: "id-update" }), deleted: false },
        { ...exercise({ id: "id-tombstone" }), deleted: false },
        { ...exercise({ id: "id-unchanged" }), deleted: false },
      ],
      [],
    );

    const summary = await seedCatalog(
      transactions,
      [
        exercise({ id: "id-insert" }),
        exercise({ id: "id-update", equipment: "dumbbell" }),
        exercise({ id: "id-unchanged" }),
      ],
      { check: false },
    );

    expect(summary).toEqual({ inserted: 1, updated: 1, tombstoned: 1, unchanged: 1 });
  });
});
