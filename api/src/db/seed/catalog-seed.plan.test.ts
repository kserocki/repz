import { describe, expect, it } from "vitest";
import { planCatalogSeed, type StoredCatalogExercise } from "./catalog-seed.plan.ts";
import type { CatalogExercise } from "./snapshot.schema.ts";

function exercise(overrides: Partial<CatalogExercise> = {}): CatalogExercise {
  return {
    id: "b6e6b6a0-0000-4000-8000-000000000001",
    primaryMuscle: "chest",
    secondaryMuscles: ["triceps", "shoulders"],
    equipment: "barbell",
    license: "CC-BY-SA 3.0",
    author: "sistab2",
    sourceUrl: "https://wger.de/exercise/73/view-base",
    translations: [
      { locale: "en", name: "Bench Press", instructions: "Lay down on a bench." },
      { locale: "pl", name: "Wyciskanie sztangi leżąc", instructions: null },
    ],
    ...overrides,
  };
}

function stored(overrides: Partial<StoredCatalogExercise> = {}): StoredCatalogExercise {
  return { ...exercise(), deleted: false, ...overrides };
}

describe("planCatalogSeed decision table", () => {
  it("given an id with no stored row, when planned, then it is an insert", () => {
    const result = planCatalogSeed([exercise()], [], []);

    expect(result).toEqual({
      ok: true,
      plan: { inserts: [exercise()], updates: [], tombstones: [], unchanged: 0 },
    });
  });

  it("given a live stored row with identical content, when planned, then it is unchanged (no write, no version)", () => {
    const result = planCatalogSeed([exercise()], [stored()], []);

    expect(result).toEqual({
      ok: true,
      plan: { inserts: [], updates: [], tombstones: [], unchanged: 1 },
    });
  });

  it("given a live stored row whose content differs, when planned, then it is an update", () => {
    const changed = exercise({ equipment: "dumbbell" });
    const result = planCatalogSeed([changed], [stored()], []);

    expect(result).toEqual({
      ok: true,
      plan: { inserts: [], updates: [changed], tombstones: [], unchanged: 0 },
    });
  });

  it("given a live stored row absent from the snapshot, when planned, then it is a tombstone", () => {
    const result = planCatalogSeed([], [stored()], []);

    expect(result).toEqual({
      ok: true,
      plan: { inserts: [], updates: [], tombstones: [exercise().id], unchanged: 0 },
    });
  });

  it("given a tombstoned stored row absent from the snapshot, when planned, then it is unchanged", () => {
    const result = planCatalogSeed([], [stored({ deleted: true })], []);

    expect(result).toEqual({
      ok: true,
      plan: { inserts: [], updates: [], tombstones: [], unchanged: 0 },
    });
  });

  it("given a tombstoned stored row present in the snapshot, when planned, then it is a revive conflict", () => {
    const result = planCatalogSeed([exercise()], [stored({ deleted: true })], []);

    expect(result).toEqual({ ok: false, conflict: { kind: "revive", ids: [exercise().id] } });
  });

  it("given an id owned by a user's custom exercise, when planned, then it is an id-owned-by-user conflict", () => {
    const result = planCatalogSeed([exercise()], [], [exercise().id]);

    expect(result).toEqual({
      ok: false,
      conflict: { kind: "id-owned-by-user", ids: [exercise().id] },
    });
  });
});

describe("planCatalogSeed content equality", () => {
  it("given translations in a different order, when planned, then it is still unchanged", () => {
    const reordered = exercise({
      translations: [
        { locale: "pl", name: "Wyciskanie sztangi leżąc", instructions: null },
        { locale: "en", name: "Bench Press", instructions: "Lay down on a bench." },
      ],
    });

    const result = planCatalogSeed([reordered], [stored()], []);

    expect(result).toEqual({
      ok: true,
      plan: { inserts: [], updates: [], tombstones: [], unchanged: 1 },
    });
  });

  it("given instructions null vs. text for the same locale, when planned, then it is an update", () => {
    const withText = exercise({
      translations: [
        { locale: "en", name: "Bench Press", instructions: "Lay down on a bench." },
        { locale: "pl", name: "Wyciskanie sztangi leżąc", instructions: "Połóż się na ławce." },
      ],
    });

    const result = planCatalogSeed([withText], [stored()], []);

    expect(result.ok).toBe(true);
    if (result.ok) {
      expect(result.plan.updates).toEqual([withText]);
    }
  });

  it("given secondaryMuscles in a different order, when planned, then it is an update (order is meaningful)", () => {
    const reordered = exercise({ secondaryMuscles: ["shoulders", "triceps"] });

    const result = planCatalogSeed([reordered], [stored()], []);

    expect(result.ok).toBe(true);
    if (result.ok) {
      expect(result.plan.updates).toEqual([reordered]);
    }
  });

  it("given a different number of translations, when planned, then it is an update", () => {
    const withExtra = exercise({
      translations: [
        ...exercise().translations,
        { locale: "fr", name: "Développé couché", instructions: null },
      ],
    });

    const result = planCatalogSeed([withExtra], [stored()], []);

    expect(result.ok).toBe(true);
    if (result.ok) {
      expect(result.plan.updates).toEqual([withExtra]);
    }
  });
});

describe("planCatalogSeed with a mixed snapshot", () => {
  it("given inserts, updates, tombstones and unchanged rows together, when planned, then each is bucketed correctly", () => {
    const unchangedRow = exercise({ id: "id-unchanged" });
    const updatedRow = exercise({ id: "id-updated", equipment: "dumbbell" });
    const insertedRow = exercise({ id: "id-inserted" });
    const toTombstone = stored({ id: "id-tombstone" });

    const result = planCatalogSeed(
      [unchangedRow, updatedRow, insertedRow],
      [stored({ id: "id-unchanged" }), stored({ id: "id-updated" }), toTombstone],
      [],
    );

    expect(result).toEqual({
      ok: true,
      plan: {
        inserts: [insertedRow],
        updates: [updatedRow],
        tombstones: ["id-tombstone"],
        unchanged: 1,
      },
    });
  });
});
