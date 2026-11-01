import { sql } from "drizzle-orm";
import { bigint, pgSequence, timestamp as pgTimestamp } from "drizzle-orm/pg-core";

/**
 * One global version sequence (ADR-0019) instead of a table: every accepted write across
 * every syncable root gets the next value, so "last write wins" only needs to compare one
 * number. `0` stays free as the pull cursor's "never pulled" starting value.
 */
export const syncVersionSeq = pgSequence("sync_version_seq", {
  startWith: 1,
  increment: 1,
});

/** Single source for the next version: the column default and every root update use it. */
export const nextSyncVersion = sql`nextval('sync_version_seq')`;

/**
 * `timestamptz(3)` everywhere (ADR-0019): millisecond precision matches JS `Date`, so a
 * value round-trips unchanged. Always stored in UTC.
 */
export function timestamp(name: string) {
  return pgTimestamp(name, { withTimezone: true, precision: 3 });
}

/**
 * Shared columns for every syncable aggregate root (ADR-0019): a version for pull/LWW,
 * a server-set `updated_at`, and a tombstone instead of a hard delete.
 */
export function syncColumns() {
  return {
    version: bigint("version", { mode: "number" }).notNull().default(nextSyncVersion),
    updatedAt: timestamp("updated_at").notNull().defaultNow(),
    deletedAt: timestamp("deleted_at"),
  };
}
