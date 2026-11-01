import { and, asc, eq, gt, inArray, ne, sql } from "drizzle-orm";
import postgres from "postgres";
import type { DatabaseExecutor } from "../../../db/client.ts";
import { workoutExercises, workoutSets, workouts } from "../../../db/schema/index.ts";
import { nextSyncVersion } from "../../../db/schema/sync.ts";
import { SyncIdConflict } from "../sync.errors.ts";
import type {
  AggregateSyncRepository,
  PullScope,
  RootState,
  WorkoutAggregate,
  WorkoutExerciseAggregate,
  WorkoutSetAggregate,
} from "../sync.types.ts";

const UNIQUE_VIOLATION = "23505";
const WORKOUT_PKEY_CONSTRAINTS = new Set([
  "workouts_pkey",
  "workout_exercises_pkey",
  "workout_sets_pkey",
]);

/**
 * `insert`/`replace` write client-given child ids, which are global primary keys: a collision
 * (this root's own id race, or a child id already used under a different root) raises 23505 on
 * one of these three constraints. Anything else propagates and fails the whole push item's
 * savepoint with a 500, per ADR-0020.
 */
function mapUniqueViolation(error: unknown): never {
  const cause = error instanceof Error ? (error.cause ?? error) : error;
  if (
    cause instanceof postgres.PostgresError &&
    cause.code === UNIQUE_VIOLATION &&
    cause.constraint_name !== undefined &&
    WORKOUT_PKEY_CONSTRAINTS.has(cause.constraint_name)
  ) {
    throw new SyncIdConflict(cause.constraint_name);
  }
  throw error;
}

function ownerIdOf(scope: PullScope): string {
  if ("catalog" in scope) {
    throw new Error("Workouts have no catalog scope");
  }
  return scope.ownerId;
}

function toRootState(row: {
  id: string;
  userId: string;
  version: number;
  deletedAt: Date | null;
}): RootState {
  return { id: row.id, ownerId: row.userId, version: row.version, deletedAt: row.deletedAt };
}

const ROOT_STATE_COLUMNS = {
  id: workouts.id,
  userId: workouts.userId,
  version: workouts.version,
  deletedAt: workouts.deletedAt,
};

/** Drizzle queries only; `sync.decisions.ts` owns every outcome rule. */
export function createWorkoutSyncRepository(
  db: DatabaseExecutor,
): AggregateSyncRepository<WorkoutAggregate> {
  async function insertChildren(
    workoutId: string,
    exercises: WorkoutExerciseAggregate[],
  ): Promise<void> {
    if (exercises.length === 0) {
      return;
    }
    await db.insert(workoutExercises).values(
      exercises.map((exercise, position) => ({
        id: exercise.id,
        workoutId,
        exerciseId: exercise.exerciseId,
        position,
        notes: exercise.notes,
      })),
    );
    const sets = exercises.flatMap((exercise) =>
      exercise.sets.map((set, position) => ({
        id: set.id,
        workoutExerciseId: exercise.id,
        position,
        type: set.type,
        weightGrams: set.weightGrams,
        reps: set.reps,
        rpe: set.rpe,
        completedAt: set.completedAt,
      })),
    );
    if (sets.length > 0) {
      await db.insert(workoutSets).values(sets);
    }
  }

  /** `workout_sets` cascades from `workout_exercises`, so deleting the exercises is enough. */
  async function deleteChildren(workoutId: string): Promise<void> {
    await db.delete(workoutExercises).where(eq(workoutExercises.workoutId, workoutId));
  }

  return {
    async findStatesForUpdate(ids) {
      if (ids.length === 0) {
        return [];
      }
      const rows = await db
        .select(ROOT_STATE_COLUMNS)
        .from(workouts)
        .where(inArray(workouts.id, ids))
        .for("update");
      return rows.map(toRootState);
    },

    async loadAggregates(ids) {
      if (ids.length === 0) {
        return new Map();
      }
      const workoutRows = await db.select().from(workouts).where(inArray(workouts.id, ids));
      const exerciseRows = await db
        .select()
        .from(workoutExercises)
        .where(inArray(workoutExercises.workoutId, ids))
        .orderBy(asc(workoutExercises.workoutId), asc(workoutExercises.position));
      const exerciseIds = exerciseRows.map((row) => row.id);
      const setRows = exerciseIds.length
        ? await db
            .select()
            .from(workoutSets)
            .where(inArray(workoutSets.workoutExerciseId, exerciseIds))
            .orderBy(asc(workoutSets.workoutExerciseId), asc(workoutSets.position))
        : [];

      const setsByExercise = new Map<string, WorkoutSetAggregate[]>();
      for (const row of setRows) {
        const list = setsByExercise.get(row.workoutExerciseId) ?? [];
        list.push({
          id: row.id,
          type: row.type as WorkoutSetAggregate["type"],
          weightGrams: row.weightGrams,
          reps: row.reps,
          rpe: row.rpe,
          completedAt: row.completedAt,
        });
        setsByExercise.set(row.workoutExerciseId, list);
      }

      const exercisesByWorkout = new Map<string, WorkoutExerciseAggregate[]>();
      for (const row of exerciseRows) {
        const list = exercisesByWorkout.get(row.workoutId) ?? [];
        list.push({
          id: row.id,
          exerciseId: row.exerciseId,
          notes: row.notes,
          sets: setsByExercise.get(row.id) ?? [],
        });
        exercisesByWorkout.set(row.workoutId, list);
      }

      const result = new Map<string, WorkoutAggregate>();
      for (const row of workoutRows) {
        result.set(row.id, {
          templateId: row.templateId,
          name: row.name,
          notes: row.notes,
          startedAt: row.startedAt,
          finishedAt: row.finishedAt,
          exercises: exercisesByWorkout.get(row.id) ?? [],
        });
      }
      return result;
    },

    async insert(ownerId, id, aggregate) {
      try {
        const [row] = await db
          .insert(workouts)
          .values({
            id,
            userId: ownerId,
            templateId: aggregate.templateId,
            name: aggregate.name,
            notes: aggregate.notes,
            startedAt: aggregate.startedAt,
            finishedAt: aggregate.finishedAt,
          })
          .returning(ROOT_STATE_COLUMNS);
        if (!row) {
          throw new Error("Insert into workouts returned no row");
        }
        await insertChildren(id, aggregate.exercises);
        return toRootState(row);
      } catch (error) {
        mapUniqueViolation(error);
      }
    },

    async replace(id, aggregate) {
      try {
        const [row] = await db
          .update(workouts)
          .set({
            templateId: aggregate.templateId,
            name: aggregate.name,
            notes: aggregate.notes,
            startedAt: aggregate.startedAt,
            finishedAt: aggregate.finishedAt,
            version: nextSyncVersion,
            updatedAt: sql`now()`,
          })
          .where(eq(workouts.id, id))
          .returning(ROOT_STATE_COLUMNS);
        if (!row) {
          throw new Error("Replace of workouts matched no row");
        }
        await deleteChildren(id);
        await insertChildren(id, aggregate.exercises);
        return toRootState(row);
      } catch (error) {
        mapUniqueViolation(error);
      }
    },

    async tombstone(id) {
      const [row] = await db
        .update(workouts)
        .set({ deletedAt: sql`now()`, version: nextSyncVersion, updatedAt: sql`now()` })
        .where(eq(workouts.id, id))
        .returning(ROOT_STATE_COLUMNS);
      if (!row) {
        throw new Error("Tombstone of workouts matched no row");
      }
      await deleteChildren(id);
      return toRootState(row);
    },

    async headsAfter(scope, cursor, limit) {
      const ownerId = ownerIdOf(scope);
      const rows = await db
        .select({
          id: workouts.id,
          version: workouts.version,
          deletedAt: workouts.deletedAt,
          updatedAt: workouts.updatedAt,
        })
        .from(workouts)
        .where(and(eq(workouts.userId, ownerId), gt(workouts.version, cursor)))
        .orderBy(asc(workouts.version))
        .limit(limit);
      return rows.map((row) => ({
        id: row.id,
        version: row.version,
        deleted: row.deletedAt !== null,
        updatedAt: row.updatedAt,
      }));
    },

    async countAfter(scope, cursor) {
      const ownerId = ownerIdOf(scope);
      const [row] = await db
        .select({ count: sql<number>`count(*)::int` })
        .from(workouts)
        .where(and(eq(workouts.userId, ownerId), gt(workouts.version, cursor)));
      return row?.count ?? 0;
    },

    async findIdsOwnedByOthers(ids, userId) {
      if (ids.length === 0) {
        return [];
      }
      const rows = await db
        .select({ id: workouts.id })
        .from(workouts)
        .where(and(inArray(workouts.id, ids), ne(workouts.userId, userId)));
      return rows.map((row) => row.id);
    },
  };
}
