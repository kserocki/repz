import { toExercisePayload } from "./custom-exercise.handler.ts";
import type { SyncHandler } from "./sync-handler.ts";

/**
 * Read-only: the catalog is seeded by a separate importer, never pushed to. Shares the
 * `exercises` table with `customExercise`, scoped to `owner_id IS NULL`.
 */
export function createCatalogExerciseSyncHandler(): SyncHandler {
  return {
    name: "catalogExercise",
    type: "exercise",
    reader: {
      headsAfter: (stores, _userId, cursor, limit) =>
        stores.exercises.headsAfter({ catalog: true }, cursor, limit),
      countAfter: (stores, _userId, cursor) =>
        stores.exercises.countAfter({ catalog: true }, cursor),
      async loadPayloads(stores, ids) {
        const aggregates = await stores.exercises.loadAggregates(ids);
        const payloads = new Map<string, unknown>();
        aggregates.forEach((aggregate, id) => {
          payloads.set(
            id,
            toExercisePayload(aggregate, {
              isCustom: false,
              imageUrl: `/static/exercises/${id}.webp`,
            }),
          );
        });
        return payloads;
      },
    },
  };
}
