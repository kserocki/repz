import {
  parsePayloadWithSchema,
  type TemplatePayload,
  TemplatePayloadSchema,
} from "../sync.schemas.ts";
import type { FieldError, SyncStores, TemplateAggregate } from "../sync.types.ts";
import { resolveWriterOutcome, type SyncHandler } from "./sync-handler.ts";

function parseTemplatePayload(wire: TemplatePayload): TemplateAggregate {
  return {
    name: wire.name,
    notes: wire.notes,
    exercises: wire.exercises.map((exercise) => ({
      exerciseId: exercise.exerciseId,
      targetSets: exercise.targetSets,
      targetReps: exercise.targetReps,
      restSeconds: exercise.restSeconds,
    })),
  };
}

function toTemplatePayload(aggregate: TemplateAggregate): TemplatePayload {
  return {
    name: aggregate.name,
    notes: aggregate.notes,
    exercises: aggregate.exercises.map((exercise) => ({
      exerciseId: exercise.exerciseId,
      targetSets: exercise.targetSets,
      targetReps: exercise.targetReps,
      restSeconds: exercise.restSeconds,
    })),
  };
}

/** Every `exerciseId` may be unknown or the caller's own, but never another user's (ADR-0020). */
async function validateReferences(
  stores: SyncStores,
  userId: string,
  aggregate: TemplateAggregate,
): Promise<FieldError[]> {
  const errors: FieldError[] = [];
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

export function createTemplateSyncHandler(): SyncHandler {
  return {
    name: "template",
    type: "template",
    writer: {
      parsePayload(raw) {
        const result = parsePayloadWithSchema(TemplatePayloadSchema, raw);
        return result.ok ? { ok: true, aggregate: parseTemplatePayload(result.data) } : result;
      },
      apply(stores, userId, id, change) {
        return resolveWriterOutcome(
          stores.templates,
          userId,
          id,
          { deleted: change.deleted, aggregate: change.aggregate as TemplateAggregate | null },
          (aggregate) => validateReferences(stores, userId, aggregate),
        );
      },
    },
    reader: {
      headsAfter: (stores, userId, cursor, limit) =>
        stores.templates.headsAfter({ ownerId: userId }, cursor, limit),
      countAfter: (stores, userId, cursor) =>
        stores.templates.countAfter({ ownerId: userId }, cursor),
      async loadPayloads(stores, ids) {
        const aggregates = await stores.templates.loadAggregates(ids);
        const payloads = new Map<string, unknown>();
        aggregates.forEach((aggregate, id) => {
          payloads.set(id, toTemplatePayload(aggregate));
        });
        return payloads;
      },
    },
  };
}
