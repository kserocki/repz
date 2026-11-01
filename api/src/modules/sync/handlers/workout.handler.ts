import {
  parsePayloadWithSchema,
  type WorkoutPayload,
  WorkoutPayloadSchema,
} from "../sync.schemas.ts";
import type { FieldError, SyncStores, WorkoutAggregate } from "../sync.types.ts";
import { resolveWriterOutcome, type SyncHandler } from "./sync-handler.ts";

function parseWorkoutPayload(wire: WorkoutPayload): WorkoutAggregate {
  return {
    templateId: wire.templateId,
    name: wire.name,
    notes: wire.notes,
    startedAt: new Date(wire.startedAt),
    finishedAt: new Date(wire.finishedAt),
    exercises: wire.exercises.map((exercise) => ({
      id: exercise.id,
      exerciseId: exercise.exerciseId,
      notes: exercise.notes,
      sets: exercise.sets.map((set) => ({
        id: set.id,
        type: set.type,
        weightGrams: set.weightGrams,
        reps: set.reps,
        rpe: set.rpe,
        completedAt: set.completedAt === null ? null : new Date(set.completedAt),
      })),
    })),
  };
}

function toWorkoutPayload(aggregate: WorkoutAggregate): WorkoutPayload {
  return {
    templateId: aggregate.templateId,
    name: aggregate.name,
    notes: aggregate.notes,
    startedAt: aggregate.startedAt.toISOString(),
    finishedAt: aggregate.finishedAt.toISOString(),
    exercises: aggregate.exercises.map((exercise) => ({
      id: exercise.id,
      exerciseId: exercise.exerciseId,
      notes: exercise.notes,
      sets: exercise.sets.map((set) => ({
        id: set.id,
        type: set.type,
        weightGrams: set.weightGrams,
        reps: set.reps,
        rpe: set.rpe,
        completedAt: set.completedAt === null ? null : set.completedAt.toISOString(),
      })),
    })),
  };
}

/**
 * `templateId` and every `exerciseId` may be unknown (dangling, created and deleted offline) or
 * the caller's own, live or tombstoned, but never another user's (ADR-0020 §3.3).
 */
async function validateReferences(
  stores: SyncStores,
  userId: string,
  aggregate: WorkoutAggregate,
): Promise<FieldError[]> {
  const errors: FieldError[] = [];

  if (aggregate.templateId !== null) {
    const foreign = await stores.templates.findIdsOwnedByOthers([aggregate.templateId], userId);
    if (foreign.includes(aggregate.templateId)) {
      errors.push({ path: "payload.templateId", message: "templateId belongs to another user" });
    }
  }

  const exerciseIds = [...new Set(aggregate.exercises.map((exercise) => exercise.exerciseId))];
  if (exerciseIds.length > 0) {
    const foreignExerciseIds = new Set(
      await stores.exercises.findIdsOwnedByOthers(exerciseIds, userId),
    );
    aggregate.exercises.forEach((exercise, index) => {
      if (foreignExerciseIds.has(exercise.exerciseId)) {
        errors.push({
          path: `payload.exercises.${index}.exerciseId`,
          message: "exerciseId belongs to another user",
        });
      }
    });
  }

  return errors;
}

export function createWorkoutSyncHandler(): SyncHandler {
  return {
    name: "workout",
    type: "workout",
    writer: {
      parsePayload(raw) {
        const result = parsePayloadWithSchema(WorkoutPayloadSchema, raw);
        return result.ok ? { ok: true, aggregate: parseWorkoutPayload(result.data) } : result;
      },
      apply(stores, userId, id, change) {
        return resolveWriterOutcome(
          stores.workouts,
          userId,
          id,
          { deleted: change.deleted, aggregate: change.aggregate as WorkoutAggregate | null },
          (aggregate) => validateReferences(stores, userId, aggregate),
        );
      },
    },
    reader: {
      headsAfter: (stores, userId, cursor, limit) =>
        stores.workouts.headsAfter({ ownerId: userId }, cursor, limit),
      countAfter: (stores, userId, cursor) =>
        stores.workouts.countAfter({ ownerId: userId }, cursor),
      async loadPayloads(stores, ids) {
        const aggregates = await stores.workouts.loadAggregates(ids);
        const payloads = new Map<string, unknown>();
        aggregates.forEach((aggregate, aggregateId) => {
          payloads.set(aggregateId, toWorkoutPayload(aggregate));
        });
        return payloads;
      },
    },
  };
}
