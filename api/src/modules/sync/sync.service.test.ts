import pino from "pino";
import { describe, expect, it } from "vitest";
import { createInMemorySyncStore } from "../../shared/testing/in-memory-sync-store.ts";
import { createCatalogExerciseSyncHandler } from "./handlers/catalog-exercise.handler.ts";
import { createCustomExerciseSyncHandler } from "./handlers/custom-exercise.handler.ts";
import {
  createSyncHandlerRegistry,
  type SyncHandler,
  type SyncWriter,
} from "./handlers/sync-handler.ts";
import { createTemplateSyncHandler } from "./handlers/template.handler.ts";
import { createWorkoutSyncHandler } from "./handlers/workout.handler.ts";
import { SyncIdConflict } from "./sync.errors.ts";
import { createSyncService, type PushEnvelope } from "./sync.service.ts";
import type { SyncTransactions } from "./sync.types.ts";

const USER_A = "b6e6b6a0-0000-4a22-8a33-00000000000a";
const USER_B = "b6e6b6a0-0000-4a22-8a33-00000000000b";

const silentLogger = pino({ level: "silent" });

function harness() {
  const store = createInMemorySyncStore();
  const registry = createSyncHandlerRegistry([
    createWorkoutSyncHandler(),
    createTemplateSyncHandler(),
    createCustomExerciseSyncHandler(),
    createCatalogExerciseSyncHandler(),
  ]);
  const service = createSyncService({
    transactions: store.transactions,
    registry,
    logger: silentLogger,
  });
  return { store, service };
}

function workoutPayload(overrides: Record<string, unknown> = {}) {
  return {
    templateId: null,
    name: "Push day",
    notes: "",
    startedAt: "2026-11-01T10:00:00.000Z",
    finishedAt: "2026-11-01T10:45:00.000Z",
    exercises: [],
    ...overrides,
  };
}

function templatePayload(overrides: Record<string, unknown> = {}) {
  return { name: "Push day", notes: "", exercises: [], ...overrides };
}

function customExercisePayload(overrides: Record<string, unknown> = {}) {
  return {
    name: "Zottman curl",
    primaryMuscle: "biceps",
    secondaryMuscles: ["forearms"],
    equipment: "dumbbell",
    ...overrides,
  };
}

function upsert(type: PushEnvelope["type"], id: string, payload: unknown): PushEnvelope {
  return { type, id, deleted: false, payload };
}

function remove(type: PushEnvelope["type"], id: string): PushEnvelope {
  return { type, id, deleted: true };
}

describe("sync.service push: protocol order", () => {
  it("given a push that writes, when it runs, then the two lock calls are the first two store calls", async () => {
    const { store, service } = harness();

    await service.push(USER_A, [upsert("workout", "w1", workoutPayload())]);

    const calls = store.recordedCalls();
    expect(calls.slice(0, 2)).toEqual(["locks:lockSharedCatalogGate", "locks:lockUserWrite"]);
  });

  it("given a push where every item fails payload validation, when it runs, then no lock is taken and nothing is recorded", async () => {
    const { store, service } = harness();

    const result = await service.push(USER_A, [upsert("workout", "w1", { not: "a workout" })]);

    expect(result.accepted).toHaveLength(0);
    expect(result.rejected).toHaveLength(1);
    expect(result.rejected[0]?.problem.type).toBe("urn:repz:problem:validation-error");
    expect(store.recordedCalls()).toEqual([]);
  });
});

describe("sync.service push: decision table outcomes", () => {
  it("given an id never seen before and a delete, when pushed, then it is accepted with a null version", async () => {
    const { service } = harness();

    const result = await service.push(USER_A, [remove("workout", "unknown-id")]);

    expect(result.accepted).toEqual([
      { type: "workout", id: "unknown-id", version: null, deleted: true },
    ]);
  });

  it("given a workout that references a catalog exercise, when pushed as a custom exercise push targeting that id, then it is rejected read-only", async () => {
    const { store, service } = harness();
    store.seedCatalogExercise("catalog-1", {
      name: "Bench press",
      primaryMuscle: "chest",
      secondaryMuscles: [],
      equipment: "barbell",
      license: "CC-BY-SA 4.0",
      author: "wger.de",
    });

    const result = await service.push(USER_A, [
      upsert("exercise", "catalog-1", customExercisePayload()),
    ]);

    expect(result.rejected).toEqual([
      {
        type: "exercise",
        id: "catalog-1",
        problem: { type: "urn:repz:problem:read-only", title: "Read Only" },
      },
    ]);
  });

  it("given an insert then a delete then a re-upsert (delete wins), when pushed, then the re-upsert is accepted with the tombstone's version", async () => {
    const { service } = harness();

    const insertResult = await service.push(USER_A, [upsert("workout", "w1", workoutPayload())]);
    const insertedVersion = insertResult.accepted[0]?.version ?? Number.NaN;

    const deleteResult = await service.push(USER_A, [remove("workout", "w1")]);
    const tombstoneVersion = deleteResult.accepted[0]?.version;
    expect(tombstoneVersion).toBeGreaterThan(insertedVersion);
    expect(deleteResult.accepted[0]).toMatchObject({ deleted: true });

    const reUpsertResult = await service.push(USER_A, [
      upsert("workout", "w1", workoutPayload({ name: "changed" })),
    ]);

    expect(reUpsertResult.accepted).toEqual([
      { type: "workout", id: "w1", version: tombstoneVersion, deleted: true },
    ]);
  });

  it("given a delete of an id that is already tombstoned, when pushed again, then it is still accepted with the tombstone's version", async () => {
    const { service } = harness();
    await service.push(USER_A, [upsert("workout", "w1", workoutPayload())]);
    const deleteResult = await service.push(USER_A, [remove("workout", "w1")]);
    const tombstoneVersion = deleteResult.accepted[0]?.version;

    const secondDelete = await service.push(USER_A, [remove("workout", "w1")]);

    expect(secondDelete.accepted).toEqual([
      { type: "workout", id: "w1", version: tombstoneVersion, deleted: true },
    ]);
  });

  it("given a tombstoned workout, when its children are inspected in storage, then they were purged", async () => {
    const { store, service } = harness();
    await service.push(USER_A, [
      upsert(
        "workout",
        "w1",
        workoutPayload({
          exercises: [
            {
              id: "b6e6b6a0-2222-4a22-8a33-000000000002",
              exerciseId: "b6e6b6a0-3333-4a22-8a33-000000000001",
              notes: "",
              sets: [],
            },
          ],
        }),
      ),
    ]);
    await service.push(USER_A, [remove("workout", "w1")]);

    const row = store.rows("workouts").find((candidate) => candidate.id === "w1");
    expect(row?.aggregate).toBeUndefined();
  });

  it("given an identical retry of the same payload, when pushed again, then the version does not bump", async () => {
    const { service } = harness();
    const payload = workoutPayload();

    const first = await service.push(USER_A, [upsert("workout", "w1", payload)]);
    const retry = await service.push(USER_A, [upsert("workout", "w1", payload)]);

    expect(retry.accepted).toEqual(first.accepted);
  });

  it("given a changed payload, when pushed again (LWW), then the version bumps and deleted stays false", async () => {
    const { service } = harness();

    const first = await service.push(USER_A, [
      upsert("workout", "w1", workoutPayload({ name: "Push day" })),
    ]);
    const second = await service.push(USER_A, [
      upsert("workout", "w1", workoutPayload({ name: "Pull day" })),
    ]);

    expect(first.accepted[0]).toBeDefined();
    const firstVersion = first.accepted[0]?.version ?? Number.NaN;
    expect(second.accepted[0]?.version).toBeGreaterThan(firstVersion);
    expect(second.accepted[0]?.deleted).toBe(false);
  });

  it("given an id already owned by another user, when pushed, then it is rejected id-conflict", async () => {
    const { service } = harness();
    await service.push(USER_B, [upsert("workout", "shared-id", workoutPayload())]);

    const result = await service.push(USER_A, [upsert("workout", "shared-id", workoutPayload())]);

    expect(result.rejected).toEqual([
      {
        type: "workout",
        id: "shared-id",
        problem: { type: "urn:repz:problem:id-conflict", title: "Id Conflict" },
      },
    ]);
  });

  const FOREIGN_TEMPLATE_ID = "b6e6b6a0-4444-4a22-8a33-000000000099";
  const UNKNOWN_TEMPLATE_ID = "b6e6b6a0-4444-4a22-8a33-000000000098";
  const OWN_TEMPLATE_ID = "b6e6b6a0-4444-4a22-8a33-000000000097";
  const CATALOG_EXERCISE_ID = "b6e6b6a0-3333-4a22-8a33-000000000001";
  const WORKOUT_EXERCISE_ID = "b6e6b6a0-2222-4a22-8a33-000000000002";

  it("given a template reference to another user's template, when pushed, then it is rejected invalid-reference and sibling items in the batch are still accepted", async () => {
    const { service } = harness();
    await service.push(USER_B, [upsert("template", FOREIGN_TEMPLATE_ID, templatePayload())]);

    const result = await service.push(USER_A, [
      upsert("workout", "w1", workoutPayload({ templateId: FOREIGN_TEMPLATE_ID })),
      upsert("workout", "w2", workoutPayload()),
    ]);

    expect(result.rejected).toEqual([
      {
        type: "workout",
        id: "w1",
        problem: {
          type: "urn:repz:problem:invalid-reference",
          title: "Invalid Reference",
          errors: [{ path: "payload.templateId", message: "templateId belongs to another user" }],
        },
      },
    ]);
    expect(result.accepted.map((item) => item.id)).toEqual(["w2"]);
  });

  it("given a dangling, catalog or own-tombstoned template/exercise reference, when pushed, then it is accepted", async () => {
    const { store, service } = harness();
    store.seedCatalogExercise(CATALOG_EXERCISE_ID, {
      name: "Bench press",
      primaryMuscle: "chest",
      secondaryMuscles: [],
      equipment: "barbell",
      license: "CC-BY-SA 4.0",
      author: "wger.de",
    });
    await service.push(USER_A, [upsert("template", OWN_TEMPLATE_ID, templatePayload())]);
    await service.push(USER_A, [remove("template", OWN_TEMPLATE_ID)]);

    const result = await service.push(USER_A, [
      upsert("workout", "w-dangling", workoutPayload({ templateId: UNKNOWN_TEMPLATE_ID })),
      upsert(
        "workout",
        "w-catalog",
        workoutPayload({
          exercises: [
            { id: WORKOUT_EXERCISE_ID, exerciseId: CATALOG_EXERCISE_ID, notes: "", sets: [] },
          ],
        }),
      ),
      upsert("workout", "w-own-tombstoned", workoutPayload({ templateId: OWN_TEMPLATE_ID })),
    ]);

    expect(result.rejected).toHaveLength(0);
    expect(result.accepted).toHaveLength(3);
  });

  it("given pushes for the same user across several calls, when their versions are compared, then they strictly increase", async () => {
    const { service } = harness();
    const versions: number[] = [];

    for (const id of ["w1", "w2", "w3"]) {
      const result = await service.push(USER_A, [upsert("workout", id, workoutPayload())]);
      expect(result.accepted[0]).toBeDefined();
      versions.push(result.accepted[0]?.version ?? Number.NaN);
    }

    expect(versions).toEqual([...versions].sort((a, b) => a - b));
    expect(new Set(versions).size).toBe(versions.length);
  });
});

describe("sync.service push: savepoint isolation", () => {
  function fakeWriterHandler(
    name: SyncHandler["name"],
    type: SyncHandler["type"],
    apply: SyncWriter["apply"],
  ): SyncHandler {
    return {
      name,
      type,
      writer: { parsePayload: () => ({ ok: true, aggregate: {} }), apply },
      reader: {
        headsAfter: async () => [],
        countAfter: async () => 0,
        loadPayloads: async () => new Map(),
      },
    };
  }

  it("given one item throws SyncIdConflict, when the push settles, then only that item is rejected id-conflict and the others are accepted", async () => {
    const store = createInMemorySyncStore();
    const registry = createSyncHandlerRegistry([
      fakeWriterHandler("workout", "workout", async () => ({
        kind: "accepted",
        version: 1,
        deleted: false,
      })),
      fakeWriterHandler("template", "template", async (_stores, _userId, id) => {
        if (id === "t1") {
          throw new SyncIdConflict("test_pkey");
        }
        return { kind: "accepted", version: 1, deleted: false };
      }),
    ]);
    const service = createSyncService({
      transactions: store.transactions,
      registry,
      logger: silentLogger,
    });

    const result = await service.push(USER_A, [
      upsert("workout", "w1", {}),
      upsert("template", "t1", {}),
      upsert("workout", "w2", {}),
    ]);

    expect(result.accepted.map((item) => item.id)).toEqual(["w1", "w2"]);
    expect(result.rejected).toEqual([
      {
        type: "template",
        id: "t1",
        problem: { type: "urn:repz:problem:id-conflict", title: "Id Conflict" },
      },
    ]);
  });

  it("given an item that owns another user's root, when applied in a batch, then the item is rejected and the real store still records the accepted siblings", async () => {
    const { service } = harness();
    await service.push(USER_B, [upsert("workout", "shared", workoutPayload())]);

    const result = await service.push(USER_A, [
      upsert("workout", "w1", workoutPayload()),
      upsert("workout", "shared", workoutPayload()),
      upsert("workout", "w2", workoutPayload()),
    ]);

    expect(result.accepted.map((item) => item.id)).toEqual(["w1", "w2"]);
    expect(result.rejected.map((item) => item.id)).toEqual(["shared"]);
  });
});

describe("sync.service pull", () => {
  it("given changes across every reader, when pulled, then they are merged by version in one page", async () => {
    const { store, service } = harness();
    store.seedCatalogExercise("catalog-1", {
      name: "Bench press",
      primaryMuscle: "chest",
      secondaryMuscles: [],
      equipment: "barbell",
      license: "CC-BY-SA 4.0",
      author: "wger.de",
    });
    await service.push(USER_A, [upsert("workout", "w1", workoutPayload())]);
    await service.push(USER_A, [upsert("template", "t1", templatePayload())]);
    await service.push(USER_A, [upsert("exercise", "e1", customExercisePayload())]);

    const page = await service.pull(USER_A, 0, 500);

    expect(page.changes.map((change) => change.id)).toEqual(["catalog-1", "w1", "t1", "e1"]);
    expect(page.changes.map((change) => change.version)).toEqual(
      [...page.changes.map((change) => change.version)].sort((a, b) => a - b),
    );
    expect(page.hasMore).toBe(false);
  });

  it("given more changes than the limit, when pulled, then hasMore is true and nextCursor follows the page, and the next page completes it", async () => {
    const { service } = harness();
    await service.push(USER_A, [upsert("workout", "w1", workoutPayload())]);
    await service.push(USER_A, [upsert("workout", "w2", workoutPayload())]);
    await service.push(USER_A, [upsert("workout", "w3", workoutPayload())]);

    const firstPage = await service.pull(USER_A, 0, 2);
    expect(firstPage.changes).toHaveLength(2);
    expect(firstPage.hasMore).toBe(true);

    const secondPage = await service.pull(USER_A, firstPage.nextCursor, 2);
    expect(secondPage.changes).toHaveLength(1);
    expect(secondPage.hasMore).toBe(false);
  });

  it("given a cursor beyond every change, when pulled, then the page is empty and nextCursor is unchanged", async () => {
    const { service } = harness();
    await service.push(USER_A, [upsert("workout", "w1", workoutPayload())]);

    const page = await service.pull(USER_A, 1_000_000, 500);

    expect(page.changes).toEqual([]);
    expect(page.hasMore).toBe(false);
    expect(page.nextCursor).toBe(1_000_000);
  });

  it("given changes owned by another user, when pulled, then they are excluded (catalog stays included)", async () => {
    const { store, service } = harness();
    store.seedCatalogExercise("catalog-1", {
      name: "Bench press",
      primaryMuscle: "chest",
      secondaryMuscles: [],
      equipment: "barbell",
      license: "CC-BY-SA 4.0",
      author: "wger.de",
    });
    await service.push(USER_B, [upsert("workout", "b-workout", workoutPayload())]);

    const page = await service.pull(USER_A, 0, 500);

    expect(page.changes.map((change) => change.id)).toEqual(["catalog-1"]);
  });

  it("given a tombstoned workout, when pulled, then it carries deleted: true and a null payload", async () => {
    const { service } = harness();
    await service.push(USER_A, [upsert("workout", "w1", workoutPayload())]);
    await service.push(USER_A, [remove("workout", "w1")]);

    const page = await service.pull(USER_A, 0, 500);

    expect(page.changes).toEqual([
      expect.objectContaining({ id: "w1", deleted: true, payload: null }),
    ]);
  });

  it("given changes of every type, when pulled, then totalEstimate buckets them correctly", async () => {
    const { service } = harness();
    await service.push(USER_A, [upsert("workout", "w1", workoutPayload())]);
    await service.push(USER_A, [upsert("workout", "w2", workoutPayload())]);
    await service.push(USER_A, [upsert("template", "t1", templatePayload())]);
    await service.push(USER_A, [upsert("exercise", "e1", customExercisePayload())]);

    const page = await service.pull(USER_A, 0, 500);

    expect(page.totalEstimate).toEqual({ total: 4, workout: 2, template: 1, exercise: 1 });
  });

  it("given a pull, when it runs, then it opens exactly one readSnapshot", async () => {
    const store = createInMemorySyncStore();
    let readSnapshotCalls = 0;
    const countingTransactions: SyncTransactions = {
      write: (work) => store.transactions.write(work),
      readSnapshot: (work) => {
        readSnapshotCalls += 1;
        return store.transactions.readSnapshot(work);
      },
    };
    const registry = createSyncHandlerRegistry([
      createWorkoutSyncHandler(),
      createTemplateSyncHandler(),
      createCustomExerciseSyncHandler(),
      createCatalogExerciseSyncHandler(),
    ]);
    const service = createSyncService({
      transactions: countingTransactions,
      registry,
      logger: silentLogger,
    });

    await service.pull(USER_A, 0, 500);

    expect(readSnapshotCalls).toBe(1);
  });
});
