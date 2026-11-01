/**
 * Per-item rejection reasons that never cross an HTTP boundary by themselves: `sync.service.ts`
 * wraps one of these in the `rejected[].problem` wire shape (`sync.schemas.ts`'s `ItemProblem`).
 * `validation-error` (a bad payload) is produced directly by a handler's `parsePayload` and
 * carries no entry here, since it is not a decision table outcome.
 */
export type SyncRejectionProblem = "read-only" | "id-conflict" | "invalid-reference";

/**
 * Thrown by `AggregateSyncRepository.insert`/`.replace` when the underlying Postgres unique
 * violation (23505) lands on one of the sync aggregate primary keys (`workouts_pkey`,
 * `workout_exercises_pkey`, `workout_sets_pkey`, `workout_templates_pkey`, `exercises_pkey`).
 * Never crosses an HTTP boundary: `sync.service.ts` catches it inside the per-change savepoint
 * and turns it into an `id-conflict` rejection for that one item. Any other database error
 * propagates and fails the whole batch.
 */
export class SyncIdConflict extends Error {
  readonly constraintName: string;

  constructor(constraintName: string) {
    super(`Sync id conflict on constraint "${constraintName}"`);
    this.constraintName = constraintName;
  }
}
