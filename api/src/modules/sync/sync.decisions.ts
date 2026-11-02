import type { SyncRejectionProblem } from "./sync.errors.ts";
import type { RootState } from "./sync.types.ts";

export type SyncDecision =
  | { action: "insert" }
  | { action: "replace" }
  | { action: "tombstone" }
  | { action: "none"; version: number | null; deleted: boolean }
  | { action: "reject"; problem: Exclude<SyncRejectionProblem, "invalid-reference"> };

/**
 * Pure decision function for one push item (ADR-0020's decision table), evaluated after payload
 * parsing and with `existing` read `FOR UPDATE` inside the locked transaction. `isEqual` is the
 * caller-supplied result of `aggregatesEqual` against the stored aggregate; it is only meaningful
 * (and should only be computed, since it requires loading the stored aggregate) when `existing`
 * is live, owned by `callerId`, and `change.deleted` is `false` — every other branch ignores it.
 *
 * Table rows, in evaluation order:
 * 1. no existing row, upsert            → insert
 * 2. no existing row, delete            → none (nothing was ever stored)
 * 3. catalog row (`ownerId === null`)   → reject read-only
 * 4. owned by another user              → reject id-conflict (never 403: one bad item must not fail the batch)
 * 5. own, already tombstoned            → none (delete wins, tombstone's version)
 * 6. own, live, delete                  → tombstone
 * 7. own, live, upsert, payload equal   → none (idempotent retry, no version bump)
 * 8. own, live, upsert, payload differs → replace (last write wins)
 */
export function decideChange(
  existing: RootState | undefined,
  change: { deleted: boolean },
  callerId: string,
  isEqual: boolean,
): SyncDecision {
  if (!existing) {
    return change.deleted ? { action: "none", version: null, deleted: true } : { action: "insert" };
  }
  if (existing.ownerId === null) {
    return { action: "reject", problem: "read-only" };
  }
  if (existing.ownerId !== callerId) {
    return { action: "reject", problem: "id-conflict" };
  }
  if (existing.deletedAt !== null) {
    return { action: "none", version: existing.version, deleted: true };
  }
  if (change.deleted) {
    return { action: "tombstone" };
  }
  if (isEqual) {
    return { action: "none", version: existing.version, deleted: false };
  }
  return { action: "replace" };
}

function isPlainObject(value: unknown): value is Record<string, unknown> {
  return (
    typeof value === "object" && value !== null && !Array.isArray(value) && !(value instanceof Date)
  );
}

/**
 * Structural equality of two canonical aggregates (dates compared by epoch millisecond, arrays
 * compared element-by-element in their given order — children in position order, secondary
 * muscles in their stored/incoming order — objects compared key-by-key regardless of key order).
 * Used only to decide rows 7/8 of the decision table.
 */
export function aggregatesEqual(a: unknown, b: unknown): boolean {
  if (a === b) {
    return true;
  }
  if (a instanceof Date || b instanceof Date) {
    return a instanceof Date && b instanceof Date && a.getTime() === b.getTime();
  }
  if (Array.isArray(a) || Array.isArray(b)) {
    return (
      Array.isArray(a) &&
      Array.isArray(b) &&
      a.length === b.length &&
      a.every((item, index) => aggregatesEqual(item, b[index]))
    );
  }
  if (isPlainObject(a) || isPlainObject(b)) {
    if (!isPlainObject(a) || !isPlainObject(b)) {
      return false;
    }
    const aKeys = Object.keys(a).sort();
    const bKeys = Object.keys(b).sort();
    return (
      aKeys.length === bKeys.length &&
      aKeys.every((key, index) => key === bKeys[index] && aggregatesEqual(a[key], b[key]))
    );
  }
  return false;
}
