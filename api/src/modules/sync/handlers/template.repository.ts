import { and, asc, eq, gt, inArray, ne, sql } from "drizzle-orm";
import postgres from "postgres";
import type { DatabaseExecutor } from "../../../db/client.ts";
import { templateExercises, workoutTemplates } from "../../../db/schema/index.ts";
import { nextSyncVersion } from "../../../db/schema/sync.ts";
import { SyncIdConflict } from "../sync.errors.ts";
import type {
  AggregateSyncRepository,
  PullScope,
  RootState,
  TemplateAggregate,
  TemplateExerciseAggregate,
} from "../sync.types.ts";

const UNIQUE_VIOLATION = "23505";
const TEMPLATE_PKEY_CONSTRAINT = "workout_templates_pkey";

/**
 * `template_exercises`'s primary key is `(template_id, position)`, which can never collide
 * across roots, so only a root insert race (two users picking the same template id offline) can
 * raise 23505 here, and only on this one constraint.
 */
function mapUniqueViolation(error: unknown): never {
  const cause = error instanceof Error ? (error.cause ?? error) : error;
  if (
    cause instanceof postgres.PostgresError &&
    cause.code === UNIQUE_VIOLATION &&
    cause.constraint_name === TEMPLATE_PKEY_CONSTRAINT
  ) {
    throw new SyncIdConflict(cause.constraint_name);
  }
  throw error;
}

function ownerIdOf(scope: PullScope): string {
  if ("catalog" in scope) {
    throw new Error("Templates have no catalog scope");
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
  id: workoutTemplates.id,
  userId: workoutTemplates.userId,
  version: workoutTemplates.version,
  deletedAt: workoutTemplates.deletedAt,
};

/** Drizzle queries only; `sync.decisions.ts` owns every outcome rule. */
export function createTemplateSyncRepository(
  db: DatabaseExecutor,
): AggregateSyncRepository<TemplateAggregate> {
  async function insertChildren(
    templateId: string,
    exercises: TemplateExerciseAggregate[],
  ): Promise<void> {
    if (exercises.length === 0) {
      return;
    }
    await db.insert(templateExercises).values(
      exercises.map((exercise, position) => ({
        templateId,
        position,
        exerciseId: exercise.exerciseId,
        targetSets: exercise.targetSets,
        targetRepsMin: exercise.targetReps?.min ?? null,
        targetRepsMax: exercise.targetReps?.max ?? null,
        restSeconds: exercise.restSeconds,
      })),
    );
  }

  async function deleteChildren(templateId: string): Promise<void> {
    await db.delete(templateExercises).where(eq(templateExercises.templateId, templateId));
  }

  return {
    async findStatesForUpdate(ids) {
      if (ids.length === 0) {
        return [];
      }
      const rows = await db
        .select(ROOT_STATE_COLUMNS)
        .from(workoutTemplates)
        .where(inArray(workoutTemplates.id, ids))
        .for("update");
      return rows.map(toRootState);
    },

    async loadAggregates(ids) {
      if (ids.length === 0) {
        return new Map();
      }
      const templateRows = await db
        .select()
        .from(workoutTemplates)
        .where(inArray(workoutTemplates.id, ids));
      const exerciseRows = await db
        .select()
        .from(templateExercises)
        .where(inArray(templateExercises.templateId, ids))
        .orderBy(asc(templateExercises.templateId), asc(templateExercises.position));

      const exercisesByTemplate = new Map<string, TemplateExerciseAggregate[]>();
      for (const row of exerciseRows) {
        const list = exercisesByTemplate.get(row.templateId) ?? [];
        list.push({
          exerciseId: row.exerciseId,
          targetSets: row.targetSets,
          targetReps:
            row.targetRepsMin !== null && row.targetRepsMax !== null
              ? { min: row.targetRepsMin, max: row.targetRepsMax }
              : null,
          restSeconds: row.restSeconds,
        });
        exercisesByTemplate.set(row.templateId, list);
      }

      const result = new Map<string, TemplateAggregate>();
      for (const row of templateRows) {
        result.set(row.id, {
          name: row.name,
          notes: row.notes,
          exercises: exercisesByTemplate.get(row.id) ?? [],
        });
      }
      return result;
    },

    async insert(ownerId, id, aggregate) {
      try {
        const [row] = await db
          .insert(workoutTemplates)
          .values({ id, userId: ownerId, name: aggregate.name, notes: aggregate.notes })
          .returning(ROOT_STATE_COLUMNS);
        if (!row) {
          throw new Error("Insert into workout_templates returned no row");
        }
        await insertChildren(id, aggregate.exercises);
        return toRootState(row);
      } catch (error) {
        mapUniqueViolation(error);
      }
    },

    async replace(id, aggregate) {
      const [row] = await db
        .update(workoutTemplates)
        .set({
          name: aggregate.name,
          notes: aggregate.notes,
          version: nextSyncVersion,
          updatedAt: sql`now()`,
        })
        .where(eq(workoutTemplates.id, id))
        .returning(ROOT_STATE_COLUMNS);
      if (!row) {
        throw new Error("Replace of workout_templates matched no row");
      }
      await deleteChildren(id);
      await insertChildren(id, aggregate.exercises);
      return toRootState(row);
    },

    async tombstone(id) {
      const [row] = await db
        .update(workoutTemplates)
        .set({ deletedAt: sql`now()`, version: nextSyncVersion, updatedAt: sql`now()` })
        .where(eq(workoutTemplates.id, id))
        .returning(ROOT_STATE_COLUMNS);
      if (!row) {
        throw new Error("Tombstone of workout_templates matched no row");
      }
      await deleteChildren(id);
      return toRootState(row);
    },

    async headsAfter(scope, cursor, limit) {
      const ownerId = ownerIdOf(scope);
      const rows = await db
        .select({
          id: workoutTemplates.id,
          version: workoutTemplates.version,
          deletedAt: workoutTemplates.deletedAt,
          updatedAt: workoutTemplates.updatedAt,
        })
        .from(workoutTemplates)
        .where(and(eq(workoutTemplates.userId, ownerId), gt(workoutTemplates.version, cursor)))
        .orderBy(asc(workoutTemplates.version))
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
        .from(workoutTemplates)
        .where(and(eq(workoutTemplates.userId, ownerId), gt(workoutTemplates.version, cursor)));
      return row?.count ?? 0;
    },

    async findIdsOwnedByOthers(ids, userId) {
      if (ids.length === 0) {
        return [];
      }
      const rows = await db
        .select({ id: workoutTemplates.id })
        .from(workoutTemplates)
        .where(and(inArray(workoutTemplates.id, ids), ne(workoutTemplates.userId, userId)));
      return rows.map((row) => row.id);
    },
  };
}
