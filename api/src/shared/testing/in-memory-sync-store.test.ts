import { describe, expect, it } from "vitest";
import { createInMemorySyncStore } from "./in-memory-sync-store.ts";

function workoutAggregate(name = "Push day") {
  return {
    templateId: null,
    name,
    notes: "",
    startedAt: new Date("2026-11-01T10:00:00.000Z"),
    finishedAt: new Date("2026-11-01T10:45:00.000Z"),
    exercises: [],
  };
}

describe("createInMemorySyncStore transactions.write", () => {
  it("given two writes for the same user, when the first hasn't settled, then the second's lockUserSyncWrites doesn't resolve until the first settles", async () => {
    const store = createInMemorySyncStore();

    let releaseFirst!: () => void;
    const firstCanFinish = new Promise<void>((resolve) => {
      releaseFirst = resolve;
    });

    const firstWrite = store.transactions.write(async (stores) => {
      await stores.locks.lockUserSyncWrites("user-1");
      await firstCanFinish;
    });

    let secondAcquiredLock = false;
    const secondWrite = store.transactions.write(async (stores) => {
      await stores.locks.lockUserSyncWrites("user-1");
      secondAcquiredLock = true;
    });

    await Promise.resolve();
    await Promise.resolve();
    await Promise.resolve();
    expect(secondAcquiredLock).toBe(false);

    releaseFirst();
    await firstWrite;
    await secondWrite;
    expect(secondAcquiredLock).toBe(true);
  });

  it("given two writes for different users, when the first hasn't settled, then the second's lockUserSyncWrites resolves without waiting", async () => {
    const store = createInMemorySyncStore();

    let releaseFirst!: () => void;
    const firstCanFinish = new Promise<void>((resolve) => {
      releaseFirst = resolve;
    });

    const firstWrite = store.transactions.write(async (stores) => {
      await stores.locks.lockUserSyncWrites("user-1");
      await firstCanFinish;
    });

    const secondWrite = store.transactions.write(async (stores) => {
      await stores.locks.lockUserSyncWrites("user-2");
      return "done";
    });

    await expect(secondWrite).resolves.toBe("done");

    releaseFirst();
    await firstWrite;
  });

  it("given an insert followed by a savepoint that throws, when the write settles, then only the savepoint's change is rolled back", async () => {
    const store = createInMemorySyncStore();

    await store.transactions.write(async (stores) => {
      await stores.locks.lockUserSyncWrites("user-1");
      await stores.workouts.insert("user-1", "workout-1", workoutAggregate("kept"));

      await expect(
        stores.savepoint(async (savepointStores) => {
          await savepointStores.workouts.insert(
            "user-1",
            "workout-2",
            workoutAggregate("rolled back"),
          );
          throw new Error("boom");
        }),
      ).rejects.toThrow("boom");
    });

    const rows = store.rows("workouts");
    expect(rows.map((row) => row.id)).toEqual(["workout-1"]);
  });

  it("given an unexpected error inside write, when it settles, then the whole batch is rolled back", async () => {
    const store = createInMemorySyncStore();

    await expect(
      store.transactions.write(async (stores) => {
        await stores.locks.lockUserSyncWrites("user-1");
        await stores.workouts.insert("user-1", "workout-1", workoutAggregate());
        throw new Error("unexpected");
      }),
    ).rejects.toThrow("unexpected");

    expect(store.rows("workouts")).toHaveLength(0);
  });

  it("given calls inside write, when recordedCalls is read, then the lock calls precede every repository call", async () => {
    const store = createInMemorySyncStore();

    await store.transactions.write(async (stores) => {
      await stores.locks.lockUserSyncWrites("user-1");
      await stores.workouts.insert("user-1", "workout-1", workoutAggregate());
    });

    expect(store.recordedCalls()).toEqual([
      "locks:lockSharedCatalogGate",
      "locks:lockUserWrite",
      "workouts:insert",
    ]);
  });
});

describe("createInMemorySyncStore seedCatalogExercise", () => {
  it("given a seeded catalog exercise, when pulled with a catalog scope, then readSnapshot sees it", async () => {
    const store = createInMemorySyncStore();
    store.seedCatalogExercise("exercise-1", {
      name: "Bench press",
      primaryMuscle: "chest",
      secondaryMuscles: ["triceps"],
      equipment: "barbell",
      license: "CC-BY-SA 4.0",
      author: "wger.de",
    });

    const heads = await store.transactions.readSnapshot((stores) =>
      stores.exercises.headsAfter({ catalog: true }, 0, 10),
    );

    expect(heads.map((head) => head.id)).toEqual(["exercise-1"]);
  });
});
