import type { Equipment, Muscle, SetType } from "../../db/schema/vocabularies.ts";
import type { SyncRejectionProblem } from "./sync.errors.ts";

/**
 * The three wire type discriminators (ADR-0020). Catalog and custom exercises share `"exercise"`;
 * the client tells them apart by `payload.isCustom`.
 */
export const SYNC_TYPES = ["workout", "template", "exercise"] as const;
export type SyncType = (typeof SYNC_TYPES)[number];

export type FieldError = { path: string; message: string };

/** Domain shape of a workout aggregate, with `Date` values (wire payloads use ISO strings). */
export type WorkoutSetAggregate = {
  id: string;
  type: SetType;
  weightGrams: number;
  reps: number;
  rpe: number | null;
  completedAt: Date | null;
};

export type WorkoutExerciseAggregate = {
  id: string;
  exerciseId: string;
  notes: string;
  sets: WorkoutSetAggregate[];
};

export type WorkoutAggregate = {
  templateId: string | null;
  name: string;
  notes: string;
  startedAt: Date;
  finishedAt: Date;
  exercises: WorkoutExerciseAggregate[];
};

export type TemplateExerciseAggregate = {
  exerciseId: string;
  targetSets: number;
  targetReps: { min: number; max: number } | null;
  restSeconds: number;
};

export type TemplateAggregate = {
  name: string;
  notes: string;
  exercises: TemplateExerciseAggregate[];
};

/** `license`/`author` are always `null` for a custom (user-owned) exercise. */
export type ExerciseAggregate = {
  name: string;
  primaryMuscle: Muscle;
  secondaryMuscles: Muscle[];
  equipment: Equipment;
  license: string | null;
  author: string | null;
};

/** One root's pull-relevant state, as returned by a pull's `headsAfter`. */
export type ChangeHead = {
  id: string;
  version: number;
  deleted: boolean;
  updatedAt: Date;
};

/** Outcome of applying one parsed change (`SyncWriter.apply`), before it is wire-shaped. */
export type PushOutcome =
  | { kind: "accepted"; version: number | null; deleted: boolean }
  | { kind: "rejected"; problem: SyncRejectionProblem; errors?: FieldError[] };

export type PullChange = {
  type: SyncType;
  id: string;
  version: number;
  deleted: boolean;
  updatedAt: Date;
  payload: unknown;
};

export type PullTotalEstimate = {
  total: number;
  workout: number;
  template: number;
  exercise: number;
};

export type PullPage = {
  changes: PullChange[];
  nextCursor: number;
  hasMore: boolean;
  totalEstimate: PullTotalEstimate;
};

/**
 * One root's state as kept in the database, independent of any caller (3.5): `ownerId: null`
 * means the global catalog, `deletedAt` is the tombstone marker. `decideChange` compares this
 * against the caller's id to tell "own" from "another user's" from "catalog".
 */
export type RootState = {
  id: string;
  ownerId: string | null;
  version: number;
  deletedAt: Date | null;
};

export type PullScope = { ownerId: string } | { catalog: true };

/**
 * The seam between the Postgres implementation (`sync.repository.ts`) and every handler
 * repository (`handlers/*.repository.ts`): one aggregate root table, read/written through
 * `SyncTransactions`.
 */
export type AggregateSyncRepository<A> = {
  findStatesForUpdate(ids: string[]): Promise<RootState[]>;
  /** Children ordered by `position`. */
  loadAggregates(ids: string[]): Promise<Map<string, A>>;
  /** `version` comes from the column's `DEFAULT nextval(...)`. */
  insert(ownerId: string, id: string, aggregate: A): Promise<RootState>;
  /** `version = nextSyncVersion`, `updatedAt = now()`, children deleted and reinserted. */
  replace(id: string, aggregate: A): Promise<RootState>;
  /** `deletedAt = now()`, version bump, children hard-deleted. */
  tombstone(id: string): Promise<RootState>;
  headsAfter(scope: PullScope, cursor: number, limit: number): Promise<ChangeHead[]>;
  countAfter(scope: PullScope, cursor: number): Promise<number>;
  /** Ids that are live or tombstoned and owned by someone other than `userId`; catalog never counts as "others". */
  findIdsOwnedByOthers(ids: string[], userId: string): Promise<string[]>;
};

export type SyncStores = {
  /** `lock_shared(3,0)` then `lock(2, hashtext(userId))`, each as its own statement. */
  locks: { lockUserSyncWrites(userId: string): Promise<void> };
  workouts: AggregateSyncRepository<WorkoutAggregate>;
  templates: AggregateSyncRepository<TemplateAggregate>;
  exercises: AggregateSyncRepository<ExerciseAggregate>;
};

export type SyncTransactions = {
  /** `READ COMMITTED`. `savepoint` is a Drizzle nested transaction, stores rebound to it. */
  write<T>(
    work: (
      stores: SyncStores & { savepoint<R>(work: (stores: SyncStores) => Promise<R>): Promise<R> },
    ) => Promise<T>,
  ): Promise<T>;
  /** `REPEATABLE READ, READ ONLY`: one consistent snapshot across every root table. */
  readSnapshot<T>(work: (stores: SyncStores) => Promise<T>): Promise<T>;
};
