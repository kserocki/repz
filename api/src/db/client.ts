import { drizzle, type PostgresJsDatabase } from "drizzle-orm/postgres-js";
import postgres from "postgres";
import * as schema from "./schema/index.ts";

export type DatabaseSchema = typeof schema;

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
