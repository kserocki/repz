import { describe, expect, it } from "vitest";
import { aggregatesEqual, decideChange } from "./sync.decisions.ts";
import type { RootState } from "./sync.types.ts";

const CALLER_ID = "b6e6b6a0-0000-4a22-8a33-000000000001";
const OTHER_USER_ID = "b6e6b6a0-0000-4a22-8a33-000000000002";

function liveOwn(version = 5): RootState {
  return { id: "root-1", ownerId: CALLER_ID, version, deletedAt: null };
}

function tombstonedOwn(version = 5): RootState {
  return {
    id: "root-1",
    ownerId: CALLER_ID,
    version,
    deletedAt: new Date("2026-10-01T00:00:00.000Z"),
  };
}

function catalog(version = 5): RootState {
  return { id: "root-1", ownerId: null, version, deletedAt: null };
}

function foreign(version = 5): RootState {
  return { id: "root-1", ownerId: OTHER_USER_ID, version, deletedAt: null };
}

describe("decideChange", () => {
  it("row 1: given no existing row and an upsert, when decided, then it inserts", () => {
    const decision = decideChange(undefined, { deleted: false }, CALLER_ID, false);
    expect(decision).toEqual({ action: "insert" });
  });

  it("row 2: given no existing row and a delete, when decided, then it accepts with a null version and no write", () => {
    const decision = decideChange(undefined, { deleted: true }, CALLER_ID, false);
    expect(decision).toEqual({ action: "none", version: null, deleted: true });
  });

  it("row 3: given a catalog row and any change, when decided, then it rejects read-only", () => {
    expect(decideChange(catalog(), { deleted: false }, CALLER_ID, false)).toEqual({
      action: "reject",
      problem: "read-only",
    });
    expect(decideChange(catalog(), { deleted: true }, CALLER_ID, false)).toEqual({
      action: "reject",
      problem: "read-only",
    });
  });

  it("row 4: given a row owned by another user and any change, when decided, then it rejects id-conflict", () => {
    expect(decideChange(foreign(), { deleted: false }, CALLER_ID, false)).toEqual({
      action: "reject",
      problem: "id-conflict",
    });
    expect(decideChange(foreign(), { deleted: true }, CALLER_ID, false)).toEqual({
      action: "reject",
      problem: "id-conflict",
    });
  });

  it("row 5: given an own tombstoned row and any change, when decided, then it accepts the tombstone's version with no write", () => {
    expect(decideChange(tombstonedOwn(9), { deleted: false }, CALLER_ID, false)).toEqual({
      action: "none",
      version: 9,
      deleted: true,
    });
    expect(decideChange(tombstonedOwn(9), { deleted: true }, CALLER_ID, false)).toEqual({
      action: "none",
      version: 9,
      deleted: true,
    });
  });

  it("row 6: given an own live row and a delete, when decided, then it tombstones", () => {
    const decision = decideChange(liveOwn(), { deleted: false }, CALLER_ID, false);
    expect(decision).not.toEqual({ action: "tombstone" }); // sanity: deleted:false never tombstones
    expect(decideChange(liveOwn(), { deleted: true }, CALLER_ID, false)).toEqual({
      action: "tombstone",
    });
  });

  it("row 7: given an own live row, an upsert and an equal payload, when decided, then it accepts the current version with no write", () => {
    const decision = decideChange(liveOwn(5), { deleted: false }, CALLER_ID, true);
    expect(decision).toEqual({ action: "none", version: 5, deleted: false });
  });

  it("row 8: given an own live row, an upsert and a different payload, when decided, then it replaces", () => {
    const decision = decideChange(liveOwn(5), { deleted: false }, CALLER_ID, false);
    expect(decision).toEqual({ action: "replace" });
  });
});

describe("aggregatesEqual", () => {
  it("given identical primitives, when compared, then it is equal", () => {
    expect(aggregatesEqual({ name: "Push day", reps: 8 }, { name: "Push day", reps: 8 })).toBe(
      true,
    );
  });

  it("given dates with the same instant, when compared, then it is equal regardless of object identity", () => {
    expect(
      aggregatesEqual(new Date("2026-11-01T10:00:00.000Z"), new Date("2026-11-01T10:00:00.000Z")),
    ).toBe(true);
  });

  it("given dates with different instants, when compared, then it is not equal", () => {
    expect(
      aggregatesEqual(new Date("2026-11-01T10:00:00.000Z"), new Date("2026-11-01T10:00:00.001Z")),
    ).toBe(false);
  });

  it("given rpe as a number on both sides, when compared, then it is equal", () => {
    expect(aggregatesEqual({ rpe: 8 }, { rpe: 8 })).toBe(true);
    expect(aggregatesEqual({ rpe: 8 }, { rpe: 8.5 })).toBe(false);
  });

  it("given children in the same position order, when compared, then it is equal", () => {
    expect(
      aggregatesEqual({ sets: [{ id: "a" }, { id: "b" }] }, { sets: [{ id: "a" }, { id: "b" }] }),
    ).toBe(true);
  });

  it("given children in a different order, when compared, then it is not equal", () => {
    expect(
      aggregatesEqual({ sets: [{ id: "a" }, { id: "b" }] }, { sets: [{ id: "b" }, { id: "a" }] }),
    ).toBe(false);
  });

  it("given secondaryMuscles in a different order, when compared, then it is not equal", () => {
    expect(
      aggregatesEqual(
        { secondaryMuscles: ["chest", "back"] },
        { secondaryMuscles: ["back", "chest"] },
      ),
    ).toBe(false);
  });

  it("given an empty notes string default on both sides, when compared, then it is equal", () => {
    expect(aggregatesEqual({ notes: "" }, { notes: "" })).toBe(true);
  });

  it("given object keys in a different declaration order, when compared, then it is still equal", () => {
    expect(aggregatesEqual({ a: 1, b: 2 }, { b: 2, a: 1 })).toBe(true);
  });

  it("given a missing key on one side, when compared, then it is not equal", () => {
    expect(aggregatesEqual({ a: 1, b: 2 }, { a: 1 })).toBe(false);
  });

  it("given a null on one side and an object on the other, when compared, then it is not equal", () => {
    expect(aggregatesEqual(null, { a: 1 })).toBe(false);
  });
});
