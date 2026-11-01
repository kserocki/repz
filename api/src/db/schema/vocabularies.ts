import { type SQL, sql } from "drizzle-orm";
import type { PgColumn } from "drizzle-orm/pg-core";

/**
 * Closed vocabularies stored as `text` + `CHECK (col IN (...))` (ADR-0019), never `pgEnum`:
 * text + check can be extended or shrunk with an ordinary migration, a pg enum value can't be
 * removed. DB values are lowercase snake_case; the wire format is mapped at the API edge.
 */

export const SET_TYPES = ["warmup", "normal", "drop", "failure"] as const;
export type SetType = (typeof SET_TYPES)[number];

export const EQUIPMENT = [
  "barbell",
  "dumbbell",
  "kettlebell",
  "machine",
  "cable",
  "bodyweight",
  "band",
  "other",
] as const;
export type Equipment = (typeof EQUIPMENT)[number];

export const UNIT_SYSTEMS = ["kg", "lb"] as const;
export type UnitSystem = (typeof UNIT_SYSTEMS)[number];

/** Interpolating a value with `sql` produces a bound `$1`, which DDL such as `CHECK` can't use. */
function sqlLiteral(value: string): SQL {
  return sql.raw(`'${value.replaceAll("'", "''")}'`);
}

/** Builds `column in ('a', 'b', ...)` from one of the tuples above, for a table's `CHECK`. */
export function inVocabulary(column: PgColumn, values: readonly string[]): SQL {
  return sql`${column} in (${sql.join(values.map(sqlLiteral), sql.raw(", "))})`;
}
