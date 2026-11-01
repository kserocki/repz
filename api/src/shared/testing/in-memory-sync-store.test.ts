import { describe, expect, it } from "vitest";
import { SyncIdConflict } from "../../modules/sync/sync.errors.ts";
import type { WorkoutAggregate, WorkoutExerciseAggregate } from "../../modules/sync/sync.types.ts";
import { createInMemorySyncStore } from "./in-memory-sync-store.ts";

function workoutAggregate(
  name = "Push day",
  exercises: WorkoutExerciseAggregate[] = [],
): WorkoutAggregate {
  return {
    templateId: null,
    name,
    notes: "",
    startedAt: new Date("2026-11-01T10:00:00.000Z"),
    finishedAt: new Date("2026-11-01T10:45:00.000Z"),
    exercises,
  };
}

function workoutExercise(id: string, setIds: string[] = []): WorkoutExerciseAggregate {
  return {
    id,
    exerciseId: "exercise-ref",
    notes: "",
    sets: setIds.map((setId) => ({
      id: setId,
      type: "normal",
      weightGrams: 0,
      reps: 0,
      rpe: null,
      completedAt: null,
    })),
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

  it("given a replace inside a savepoint that throws, when the write settles, then the previous version and aggregate are intact", async () => {
    const store = createInMemorySyncStore();

    await store.transactions.write(async (stores) => {
      await stores.locks.lockUserSyncWrites("user-1");
      await stores.workouts.insert("user-1", "workout-1", workoutAggregate("original"));
    });
    const beforeReplace = store.rows("workouts").find((row) => row.id === "workout-1");
    expect(beforeReplace?.aggregate?.name).toBe("original");
    const versionBeforeReplace = beforeReplace?.version;

    await store.transactions.write(async (stores) => {
      await stores.locks.lockUserSyncWrites("user-1");

      await expect(
        stores.savepoint(async (savepointStores) => {
          await savepointStores.workouts.replace("workout-1", workoutAggregate("replaced"));
          throw new Error("boom");
        }),
      ).rejects.toThrow("boom");
    });

    const afterRollback = store.rows("workouts").find((row) => row.id === "workout-1");
    expect(afterRollback?.aggregate?.name).toBe("original");
    expect(afterRollback?.version).toBe(versionBeforeReplace);
  });

  it("given a tombstone inside a savepoint that throws, when the write settles, then the row is still live with its aggregate intact", async () => {
    const store = createInMemorySyncStore();

    await store.transactions.write(async (stores) => {
      await stores.locks.lockUserSyncWrites("user-1");
      await stores.workouts.insert("user-1", "workout-1", workoutAggregate("kept"));
    });

    await store.transactions.write(async (stores) => {
      await stores.locks.lockUserSyncWrites("user-1");

      await expect(
        stores.savepoint(async (savepointStores) => {
          await savepointStores.workouts.tombstone("workout-1");
          throw new Error("boom");
        }),
      ).rejects.toThrow("boom");
    });

    const row = store.rows("workouts").find((candidate) => candidate.id === "workout-1");
    expect(row?.deletedAt).toBeNull();
    expect(row?.aggregate?.name).toBe("kept");
  });

  it("given a child (exercise) id already used under a different workout, when a new workout is inserted, then it throws SyncIdConflict for workout_exercises_pkey", async () => {
    const store = createInMemorySyncStore();
    const sharedExerciseId = "shared-exercise-id";

    await store.transactions.write(async (stores) => {
      await stores.locks.lockUserSyncWrites("user-1");
      await stores.workouts.insert(
        "user-1",
        "workout-1",
        workoutAggregate("first", [workoutExercise(sharedExerciseId)]),
      );
    });

    await expect(
      store.transactions.write(async (stores) => {
        await stores.locks.lockUserSyncWrites("user-2");
        await stores.workouts.insert(
          "user-2",
          "workout-2",
          workoutAggregate("second", [workoutExercise(sharedExerciseId)]),
        );
      }),
    ).rejects.toThrow(SyncIdConflict);
  });

  it("given a child (set) id already used under a different workout, when a new workout is inserted, then it throws SyncIdConflict for workout_sets_pkey", async () => {
    const store = createInMemorySyncStore();
    const sharedSetId = "shared-set-id";

    await store.transactions.write(async (stores) => {
      await stores.locks.lockUserSyncWrites("user-1");
      await stores.workouts.insert(
        "user-1",
        "workout-1",
        workoutAggregate("first", [workoutExercise("exercise-1", [sharedSetId])]),
      );
    });

    await expect(
      store.transactions.write(async (stores) => {
        await stores.locks.lockUserSyncWrites("user-2");
        await stores.workouts.insert(
          "user-2",
          "workout-2",
          workoutAggregate("second", [workoutExercise("exercise-2", [sharedSetId])]),
        );
      }),
    ).rejects.toThrow(SyncIdConflict);
  });

  it("given a workout replaced with the same exercise and set ids it already owns, when replaced, then it does not throw", async () => {
    const store = createInMemorySyncStore();
    const exerciseId = "own-exercise-id";
    const setId = "own-set-id";

    await store.transactions.write(async (stores) => {
      await stores.locks.lockUserSyncWrites("user-1");
      await stores.workouts.insert(
        "user-1",
        "workout-1",
        workoutAggregate("first", [workoutExercise(exerciseId, [setId])]),
      );
    });

    await store.transactions.write(async (stores) => {
      await stores.locks.lockUserSyncWrites("user-1");
      await stores.workouts.replace(
        "workout-1",
        workoutAggregate("renamed", [workoutExercise(exerciseId, [setId])]),
      );
    });

    const row = store.rows("workouts").find((candidate) => candidate.id === "workout-1");
    expect(row?.aggregate?.name).toBe("renamed");
  });

  it("given a tombstoned workout's exercise id, when reused by a new workout, then it is no longer in use (children were hard-deleted)", async () => {
    const store = createInMemorySyncStore();
    const exerciseId = "freed-exercise-id";

    await store.transactions.write(async (stores) => {
      await stores.locks.lockUserSyncWrites("user-1");
      await stores.workouts.insert(
        "user-1",
        "workout-1",
        workoutAggregate("to be deleted", [workoutExercise(exerciseId)]),
      );
    });
    await store.transactions.write(async (stores) => {
      await stores.locks.lockUserSyncWrites("user-1");
      await stores.workouts.tombstone("workout-1");
    });

    await expect(
      store.transactions.write(async (stores) => {
        await stores.locks.lockUserSyncWrites("user-2");
        await stores.workouts.insert(
          "user-2",
          "workout-2",
          workoutAggregate("reuses the freed id", [workoutExercise(exerciseId)]),
        );
      }),
    ).resolves.toBeUndefined();
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
