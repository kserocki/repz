import { and, asc, eq, gt, inArray, isNull, ne, sql } from "drizzle-orm";
import postgres from "postgres";
import type { DatabaseExecutor } from "../../../db/client.ts";
import { exercises, exerciseTranslations } from "../../../db/schema/index.ts";
import { nextSyncVersion } from "../../../db/schema/sync.ts";
import type { Equipment, Muscle } from "../../../db/schema/vocabularies.ts";
import { SyncIdConflict } from "../sync.errors.ts";
import type {
  AggregateSyncRepository,
  ExerciseAggregate,
  ExerciseTranslation,
  PullScope,
  RootState,
} from "../sync.types.ts";

const UNIQUE_VIOLATION = "23505";
const EXERCISE_PKEY_CONSTRAINT = "exercises_pkey";

/**
 * A custom exercise has no children, so only a root insert race (two users, or a user and the
 * catalog seeder, picking the same id) can raise 23505, and only on this one constraint.
 */
function mapUniqueViolation(error: unknown): never {
  const cause = error instanceof Error ? (error.cause ?? error) : error;
  if (
    cause instanceof postgres.PostgresError &&
    cause.code === UNIQUE_VIOLATION &&
    cause.constraint_name === EXERCISE_PKEY_CONSTRAINT
  ) {
    throw new SyncIdConflict(cause.constraint_name);
  }
  throw error;
}

/** `{ catalog: true }` reads `owner_id IS NULL`; a user scope reads their own custom exercises. */
function scopeCondition(scope: PullScope) {
  return "catalog" in scope ? isNull(exercises.ownerId) : eq(exercises.ownerId, scope.ownerId);
}

function toRootState(row: {
  id: string;
  ownerId: string | null;
  version: number;
  deletedAt: Date | null;
}): RootState {
  return { id: row.id, ownerId: row.ownerId, version: row.version, deletedAt: row.deletedAt };
}

const ROOT_STATE_COLUMNS = {
  id: exercises.id,
  ownerId: exercises.ownerId,
  version: exercises.version,
  deletedAt: exercises.deletedAt,
};

/**
 * Drizzle queries only, shared by the `customExercise` writer (scoped to its owner) and the
 * read-only `catalogExercise` reader (scoped to `owner_id IS NULL`): both are the same table.
 */
export function createExerciseSyncRepository(
  db: DatabaseExecutor,
): AggregateSyncRepository<ExerciseAggregate> {
  return {
    async findStatesForUpdate(ids) {
      if (ids.length === 0) {
        return [];
      }
      const rows = await db
        .select(ROOT_STATE_COLUMNS)
        .from(exercises)
        .where(inArray(exercises.id, ids))
        .for("update");
      return rows.map(toRootState);
    },

    async loadAggregates(ids) {
      if (ids.length === 0) {
        return new Map();
      }
      const rows = await db.select().from(exercises).where(inArray(exercises.id, ids));
      const translationRows = await db
        .select()
        .from(exerciseTranslations)
        .where(inArray(exerciseTranslations.exerciseId, ids))
        .orderBy(asc(exerciseTranslations.exerciseId), asc(exerciseTranslations.locale));

      const translationsByExercise = new Map<string, ExerciseTranslation[]>();
      for (const row of translationRows) {
        const list = translationsByExercise.get(row.exerciseId) ?? [];
        list.push({ locale: row.locale, name: row.name, instructions: row.instructions });
        translationsByExercise.set(row.exerciseId, list);
      }

      const result = new Map<string, ExerciseAggregate>();
      for (const row of rows) {
        result.set(row.id, {
          name: row.name,
          primaryMuscle: row.primaryMuscle as Muscle,
          secondaryMuscles: row.secondaryMuscles as Muscle[],
          equipment: row.equipment as Equipment,
          license: row.license,
          author: row.author,
          sourceUrl: row.sourceUrl,
          translations: translationsByExercise.get(row.id) ?? [],
        });
      }
      return result;
    },

    async insert(ownerId, id, aggregate) {
      try {
        const [row] = await db
          .insert(exercises)
          .values({
            id,
            ownerId,
            name: aggregate.name,
            primaryMuscle: aggregate.primaryMuscle,
            secondaryMuscles: aggregate.secondaryMuscles,
            equipment: aggregate.equipment,
            license: aggregate.license,
            author: aggregate.author,
          })
          .returning(ROOT_STATE_COLUMNS);
        if (!row) {
          throw new Error("Insert into exercises returned no row");
        }
        return toRootState(row);
      } catch (error) {
        mapUniqueViolation(error);
      }
    },

    async replace(id, aggregate) {
      const [row] = await db
        .update(exercises)
        .set({
          name: aggregate.name,
          primaryMuscle: aggregate.primaryMuscle,
          secondaryMuscles: aggregate.secondaryMuscles,
          equipment: aggregate.equipment,
          version: nextSyncVersion,
          updatedAt: sql`now()`,
        })
        .where(eq(exercises.id, id))
        .returning(ROOT_STATE_COLUMNS);
      if (!row) {
        throw new Error("Replace of exercises matched no row");
      }
      return toRootState(row);
    },

    async tombstone(id) {
      const [row] = await db
        .update(exercises)
        .set({ deletedAt: sql`now()`, version: nextSyncVersion, updatedAt: sql`now()` })
        .where(eq(exercises.id, id))
        .returning(ROOT_STATE_COLUMNS);
      if (!row) {
        throw new Error("Tombstone of exercises matched no row");
      }
      return toRootState(row);
    },

    async headsAfter(scope, cursor, limit) {
      const rows = await db
        .select({
          id: exercises.id,
          version: exercises.version,
          deletedAt: exercises.deletedAt,
          updatedAt: exercises.updatedAt,
        })
        .from(exercises)
        .where(and(scopeCondition(scope), gt(exercises.version, cursor)))
        .orderBy(asc(exercises.version))
        .limit(limit);
      return rows.map((row) => ({
        id: row.id,
        version: row.version,
        deleted: row.deletedAt !== null,
        updatedAt: row.updatedAt,
      }));
    },

    async countAfter(scope, cursor) {
      const [row] = await db
        .select({ count: sql<number>`count(*)::int` })
        .from(exercises)
        .where(and(scopeCondition(scope), gt(exercises.version, cursor)));
      return row?.count ?? 0;
    },

    async findIdsOwnedByOthers(ids, userId) {
      if (ids.length === 0) {
        return [];
      }
      const rows = await db
        .select({ id: exercises.id })
        .from(exercises)
        .where(
          and(
            inArray(exercises.id, ids),
            sql`${exercises.ownerId} is not null`,
            ne(exercises.ownerId, userId),
          ),
        );
      return rows.map((row) => row.id);
    },
  };
}
