import { sql } from "drizzle-orm";
import type { PostgresJsDatabase } from "drizzle-orm/postgres-js";
import type { DatabaseExecutor, DatabaseSchema } from "../../db/client.ts";
import { createExerciseSyncRepository } from "./handlers/exercise.repository.ts";
import { createTemplateSyncRepository } from "./handlers/template.repository.ts";
import { createWorkoutSyncRepository } from "./handlers/workout.repository.ts";
import type { SyncStores, SyncTransactions } from "./sync.types.ts";

/** Namespace for the per-user exclusive write lock (ADR-0020). */
export const SYNC_USER_WRITE_LOCK_SPACE = 2;
/** Namespace for the catalog gate: shared by every push, exclusive for the catalog seeder. */
export const SYNC_CATALOG_GATE_LOCK_SPACE = 3;

/**
 * Exported for the catalog seeder: it must take this exclusive lock as the first statement of
 * its own write transaction, so it waits for every in-flight push and blocks new ones, and
 * catalog versions never interleave with an in-flight user write.
 */
export async function lockCatalogSyncWrites(executor: DatabaseExecutor): Promise<void> {
  await executor.execute(
    sql`select pg_advisory_xact_lock(${SYNC_CATALOG_GATE_LOCK_SPACE}::int4, 0)`,
  );
}

function createLocks(executor: DatabaseExecutor) {
  return {
    async lockUserSyncWrites(userId: string): Promise<void> {
      // Two separate statements, in this order: the shared catalog gate first, then the
      // exclusive per-user lock. Every holder takes them in the same order, so there is no
      // deadlock, and this must be the first thing any version-assigning transaction does.
      await executor.execute(
        sql`select pg_advisory_xact_lock_shared(${SYNC_CATALOG_GATE_LOCK_SPACE}::int4, 0)`,
      );
      await executor.execute(
        sql`select pg_advisory_xact_lock(${SYNC_USER_WRITE_LOCK_SPACE}::int4, hashtext(${userId}::text))`,
      );
    },
  };
}

function storesFor(executor: DatabaseExecutor): SyncStores {
  return {
    locks: createLocks(executor),
    workouts: createWorkoutSyncRepository(executor),
    templates: createTemplateSyncRepository(executor),
    exercises: createExerciseSyncRepository(executor),
  };
}

/**
 * `write` is `READ COMMITTED` (Postgres's default, named explicitly for clarity); `savepoint` is
 * a Drizzle nested transaction (a real `SAVEPOINT`/`RELEASE`/`ROLLBACK TO` with the postgres-js
 * driver), so one item's constraint failure rolls back only that item. `readSnapshot` is
 * `REPEATABLE READ, READ ONLY`, needed so a pull's several per-table queries see one consistent
 * snapshot instead of each getting its own (ADR-0020).
 */
export function createSyncTransactions(db: PostgresJsDatabase<DatabaseSchema>): SyncTransactions {
  return {
    write(work) {
      return db.transaction(
        (tx) =>
          work({
            ...storesFor(tx),
            savepoint: (savepointWork) =>
              tx.transaction((savepointTx) => savepointWork(storesFor(savepointTx))),
          }),
        { isolationLevel: "read committed" },
      );
    },

    readSnapshot(work) {
      return db.transaction((tx) => work(storesFor(tx)), {
        isolationLevel: "repeatable read",
        accessMode: "read only",
      });
    },
  };
}
