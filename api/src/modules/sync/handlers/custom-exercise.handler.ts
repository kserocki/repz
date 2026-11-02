import {
  type CustomExercisePayload,
  CustomExercisePayloadSchema,
  type ExercisePayload,
  parsePayloadWithSchema,
} from "../sync.schemas.ts";
import type { ExerciseAggregate } from "../sync.types.ts";
import { resolveWriterOutcome, type SyncHandler } from "./sync-handler.ts";

function parseCustomExercisePayload(wire: CustomExercisePayload): ExerciseAggregate {
  return {
    name: wire.name,
    primaryMuscle: wire.primaryMuscle,
    secondaryMuscles: wire.secondaryMuscles,
    equipment: wire.equipment,
    // A pushed custom exercise never carries attribution or translations; only the catalog
    // seeder sets these.
    license: null,
    author: null,
    sourceUrl: null,
    translations: [],
  };
}

/** Shared with `catalog-exercise.handler.ts`: both read the same `exercises` table. */
export function toExercisePayload(
  aggregate: ExerciseAggregate,
  extra: { isCustom: boolean; imageUrl: string | null },
): ExercisePayload {
  return {
    name: aggregate.name,
    primaryMuscle: aggregate.primaryMuscle,
    secondaryMuscles: aggregate.secondaryMuscles,
    equipment: aggregate.equipment,
    isCustom: extra.isCustom,
    imageUrl: extra.imageUrl,
    license: aggregate.license,
    author: aggregate.author,
    sourceUrl: aggregate.sourceUrl,
    translations: aggregate.translations,
  };
}

export function createCustomExerciseSyncHandler(): SyncHandler {
  return {
    name: "customExercise",
    type: "exercise",
    writer: {
      parsePayload(raw) {
        const result = parsePayloadWithSchema(CustomExercisePayloadSchema, raw);
        return result.ok
          ? { ok: true, aggregate: parseCustomExercisePayload(result.data) }
          : result;
      },
      apply(stores, userId, id, change) {
        // No cross-aggregate references on an exercise: nothing to validate.
        return resolveWriterOutcome(stores.exercises, userId, id, {
          deleted: change.deleted,
          aggregate: change.aggregate as ExerciseAggregate | null,
        });
      },
    },
    reader: {
      headsAfter: (stores, userId, cursor, limit) =>
        stores.exercises.headsAfter({ ownerId: userId }, cursor, limit),
      countAfter: (stores, userId, cursor) =>
        stores.exercises.countAfter({ ownerId: userId }, cursor),
      async loadPayloads(stores, ids) {
        const aggregates = await stores.exercises.loadAggregates(ids);
        const payloads = new Map<string, unknown>();
        aggregates.forEach((aggregate, id) => {
          payloads.set(id, toExercisePayload(aggregate, { isCustom: true, imageUrl: null }));
        });
        return payloads;
      },
    },
  };
}
