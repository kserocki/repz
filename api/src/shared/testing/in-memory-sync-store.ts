import { SyncIdConflict } from "../../modules/sync/sync.errors.ts";
import type {
  AggregateSyncRepository,
  ExerciseAggregate,
  PullScope,
  RootState,
  SyncStores,
  SyncTransactions,
  TemplateAggregate,
  WorkoutAggregate,
} from "../../modules/sync/sync.types.ts";

type FakeRoot<A> = {
  id: string;
  ownerId: string | null;
  version: number;
  deletedAt: Date | null;
  updatedAt: Date;
  /** `undefined` once tombstoned: a tombstone hard-deletes children and is never read again. */
  aggregate: A | undefined;
};

type SyncTableName = "workouts" | "templates" | "exercises";

function matchesScope(row: { ownerId: string | null }, scope: PullScope): boolean {
  return "catalog" in scope ? row.ownerId === null : row.ownerId === scope.ownerId;
}

function toRootState<A>(row: FakeRoot<A>): RootState {
  return { id: row.id, ownerId: row.ownerId, version: row.version, deletedAt: row.deletedAt };
}

/** DB-free `AggregateSyncRepository<A>` fake backed by one `Map` per aggregate table. */
function createFakeAggregateRepository<A>(
  table: Map<string, FakeRoot<A>>,
  nextVersion: () => number,
  pkeyConstraintName: string,
): AggregateSyncRepository<A> {
  return {
    async findStatesForUpdate(ids) {
      return ids.flatMap((id) => {
        const row = table.get(id);
        return row ? [toRootState(row)] : [];
      });
    },

    async loadAggregates(ids) {
      const result = new Map<string, A>();
      for (const id of ids) {
        const row = table.get(id);
        if (row?.aggregate !== undefined) {
          result.set(id, row.aggregate);
        }
      }
      return result;
    },

    async insert(ownerId, id, aggregate) {
      if (table.has(id)) {
        throw new SyncIdConflict(pkeyConstraintName);
      }
      const row: FakeRoot<A> = {
        id,
        ownerId,
        version: nextVersion(),
        deletedAt: null,
        updatedAt: new Date(),
        aggregate,
      };
      table.set(id, row);
      return toRootState(row);
    },

    async replace(id, aggregate) {
      const row = table.get(id);
      if (!row) {
        throw new Error(`Fake replace: no row for id "${id}"`);
      }
      row.aggregate = aggregate;
      row.version = nextVersion();
      row.updatedAt = new Date();
      return toRootState(row);
    },

    async tombstone(id) {
      const row = table.get(id);
      if (!row) {
        throw new Error(`Fake tombstone: no row for id "${id}"`);
      }
      row.deletedAt = new Date();
      row.version = nextVersion();
      row.updatedAt = new Date();
      row.aggregate = undefined;
      return toRootState(row);
    },

    async headsAfter(scope, cursor, limit) {
      return [...table.values()]
        .filter((row) => matchesScope(row, scope) && row.version > cursor)
        .sort((a, b) => a.version - b.version)
        .slice(0, limit)
        .map((row) => ({
          id: row.id,
          version: row.version,
          deleted: row.deletedAt !== null,
          updatedAt: row.updatedAt,
        }));
    },

    async countAfter(scope, cursor) {
      return [...table.values()].filter((row) => matchesScope(row, scope) && row.version > cursor)
        .length;
    },

    async findIdsOwnedByOthers(ids, userId) {
      return ids.filter((id) => {
        const row = table.get(id);
        return row !== undefined && row.ownerId !== null && row.ownerId !== userId;
      });
    },
  };
}

/** Logs `"<table>:<method>"` for every repository call, in call order, for protocol-order tests. */
function withRecording<A>(
  name: SyncTableName,
  repo: AggregateSyncRepository<A>,
  log: string[],
): AggregateSyncRepository<A> {
  return {
    findStatesForUpdate: (ids) => {
      log.push(`${name}:findStatesForUpdate`);
      return repo.findStatesForUpdate(ids);
    },
    loadAggregates: (ids) => {
      log.push(`${name}:loadAggregates`);
      return repo.loadAggregates(ids);
    },
    insert: (ownerId, id, aggregate) => {
      log.push(`${name}:insert`);
      return repo.insert(ownerId, id, aggregate);
    },
    replace: (id, aggregate) => {
      log.push(`${name}:replace`);
      return repo.replace(id, aggregate);
    },
    tombstone: (id) => {
      log.push(`${name}:tombstone`);
      return repo.tombstone(id);
    },
    headsAfter: (scope, cursor, limit) => {
      log.push(`${name}:headsAfter`);
      return repo.headsAfter(scope, cursor, limit);
    },
    countAfter: (scope, cursor) => {
      log.push(`${name}:countAfter`);
      return repo.countAfter(scope, cursor);
    },
    findIdsOwnedByOthers: (ids, userId) => {
      log.push(`${name}:findIdsOwnedByOthers`);
      return repo.findIdsOwnedByOthers(ids, userId);
    },
  };
}

type AnyFakeRoot =
  | FakeRoot<WorkoutAggregate>
  | FakeRoot<TemplateAggregate>
  | FakeRoot<ExerciseAggregate>;

export type InMemorySyncStore = {
  transactions: SyncTransactions;
  /** Every table call made since the store was created, in call order (protocol-order tests). */
  recordedCalls(): string[];
  /** Test-only direct seeding of a catalog row (`ownerId: null`), bypassing the write lock. */
  seedCatalogExercise(id: string, aggregate: ExerciseAggregate): RootState;
  /** Raw rows of one table, for assertions beyond what a push/pull response exposes. */
  rows(table: SyncTableName): AnyFakeRoot[];
};

/**
 * DB-free `SyncTransactions` fake. `write`'s `lockUserSyncWrites` models Postgres's advisory-lock
 * serialization as a per-user async mutex held until `write` settles (same shape as
 * `in-memory-auth-store`'s family lock) and records the two lock calls before anything else, so a
 * protocol-order test can assert they are the first calls. `savepoint` snapshots every table and
 * restores it on throw; `write` does the same for the whole batch on an unexpected error.
 *
 * This fake proves the protocol, not the database race — Spotter proves the races on real
 * Postgres.
 */
export function createInMemorySyncStore(): InMemorySyncStore {
  let versionCounter = 0;
  const nextVersion = () => ++versionCounter;

  const workoutsTable = new Map<string, FakeRoot<WorkoutAggregate>>();
  const templatesTable = new Map<string, FakeRoot<TemplateAggregate>>();
  const exercisesTable = new Map<string, FakeRoot<ExerciseAggregate>>();

  const userLockTails = new Map<string, Promise<void>>();
  const recordedCalls: string[] = [];

  function baseStores(): Omit<SyncStores, "locks"> {
    return {
      workouts: withRecording(
        "workouts",
        createFakeAggregateRepository(workoutsTable, nextVersion, "workouts_pkey"),
        recordedCalls,
      ),
      templates: withRecording(
        "templates",
        createFakeAggregateRepository(templatesTable, nextVersion, "workout_templates_pkey"),
        recordedCalls,
      ),
      exercises: withRecording(
        "exercises",
        createFakeAggregateRepository(exercisesTable, nextVersion, "exercises_pkey"),
        recordedCalls,
      ),
    };
  }

  function snapshotTables() {
    return {
      workouts: new Map(workoutsTable),
      templates: new Map(templatesTable),
      exercises: new Map(exercisesTable),
    };
  }

  function restoreTables(snapshot: ReturnType<typeof snapshotTables>): void {
    workoutsTable.clear();
    for (const [id, row] of snapshot.workouts) {
      workoutsTable.set(id, row);
    }
    templatesTable.clear();
    for (const [id, row] of snapshot.templates) {
      templatesTable.set(id, row);
    }
    exercisesTable.clear();
    for (const [id, row] of snapshot.exercises) {
      exercisesTable.set(id, row);
    }
  }

  const transactions: SyncTransactions = {
    async write(work) {
      const releases: Array<() => void> = [];
      const locks = {
        async lockUserSyncWrites(userId: string): Promise<void> {
          recordedCalls.push("locks:lockSharedCatalogGate");
          recordedCalls.push("locks:lockUserWrite");
          const previous = userLockTails.get(userId) ?? Promise.resolve();
          let release!: () => void;
          const held = new Promise<void>((resolve) => {
            release = resolve;
          });
          userLockTails.set(
            userId,
            previous.then(() => held),
          );
          await previous;
          releases.push(release);
        },
      };

      const writeSnapshot = snapshotTables();
      try {
        return await work({
          ...baseStores(),
          locks,
          async savepoint(savepointWork) {
            const savepointSnapshot = snapshotTables();
            try {
              return await savepointWork(baseStores() as SyncStores);
            } catch (error) {
              restoreTables(savepointSnapshot);
              throw error;
            }
          },
        });
      } catch (error) {
        restoreTables(writeSnapshot);
        throw error;
      } finally {
        releases.forEach((release) => {
          release();
        });
      }
    },

    async readSnapshot(work) {
      return work(baseStores() as SyncStores);
    },
  };

  return {
    transactions,
    recordedCalls: () => [...recordedCalls],
    seedCatalogExercise(id, aggregate) {
      const row: FakeRoot<ExerciseAggregate> = {
        id,
        ownerId: null,
        version: nextVersion(),
        deletedAt: null,
        updatedAt: new Date(),
        aggregate,
      };
      exercisesTable.set(id, row);
      return toRootState(row);
    },
    rows(table) {
      const source =
        table === "workouts"
          ? workoutsTable
          : table === "templates"
            ? templatesTable
            : exercisesTable;
      return [...source.values()];
    },
  };
}
