import type { ExtractTablesWithRelations } from "drizzle-orm";
import type { PgDatabase } from "drizzle-orm/pg-core";
import {
  drizzle,
  type PostgresJsDatabase,
  type PostgresJsQueryResultHKT,
} from "drizzle-orm/postgres-js";
import postgres from "postgres";
import * as schema from "./schema/index.ts";

export type DatabaseSchema = typeof schema;

/**
 * Accepted by every repository: either the top-level Drizzle client or the `tx` a
 * `db.transaction(...)` callback receives, so a repository works unchanged inside a
 * transaction (`AuthTransactions`, REP-14) and outside one.
 */
export type DatabaseExecutor = PgDatabase<
  PostgresJsQueryResultHKT,
  DatabaseSchema,
  ExtractTablesWithRelations<DatabaseSchema>
>;

export type Database = {
  db: PostgresJsDatabase<DatabaseSchema>;
  ping(): Promise<void>;
  close(): Promise<void>;
};

/**
 * Postgres.js + Drizzle client, typed with the full schema.
 * `ping()` is used once at boot to fail fast if the database is unreachable.
 */
export function createDatabase(databaseUrl: string): Database {
  const client = postgres(databaseUrl);
  const db = drizzle(client, { schema });

  return {
    db,
    async ping() {
      await client`select 1`;
    },
    async close() {
      await client.end();
    },
  };
}
