import { sql } from "drizzle-orm";
import { bigint, pgSequence, timestamp as pgTimestamp } from "drizzle-orm/pg-core";

/**
 * One global version sequence (ADR-0019 T4) instead of a table: every accepted write across
 * every syncable root gets the next value, so "last write wins" only needs to compare one
 * number. `0` stays free as the pull cursor's "never pulled" starting value.
 */
export const syncVersionSeq = pgSequence("sync_version_seq", {
  startWith: 1,
  increment: 1,
});

/**
 * `version = nextval('sync_version_seq')`, reused both as the column default (new rows) and,
 * exported, by every update of a root (REP-15's handlers, ADR-0019 T4) so the two never drift
 * apart into two different ways of asking Postgres for the next version.
 */
export const nextSyncVersion = sql`nextval('sync_version_seq')`;

/**
 * `timestamptz(3)` everywhere (ADR-0019 T2): millisecond precision matches JS `Date`, so a
 * value round-trips unchanged. Always stored in UTC.
 */
export function timestamp(name: string) {
  return pgTimestamp(name, { withTimezone: true, precision: 3 });
}

/**
 * Shared columns for every syncable aggregate root (ADR-0019 T3/T4): a version for pull/LWW,
 * a server-set `updated_at`, and a tombstone instead of a hard delete.
 */
export function syncColumns() {
  return {
    version: bigint("version", { mode: "number" }).notNull().default(nextSyncVersion),
    updatedAt: timestamp("updated_at").notNull().defaultNow(),
    deletedAt: timestamp("deleted_at"),
  };
}
