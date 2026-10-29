import { drizzle, type PostgresJsDatabase } from "drizzle-orm/postgres-js";
import postgres from "postgres";

export type Database = {
  db: PostgresJsDatabase;
  ping(): Promise<void>;
  close(): Promise<void>;
};

/**
 * Postgres.js + Drizzle client, without a schema (REP-13 adds `db/schema/`).
 * `ping()` is used once at boot to fail fast if the database is unreachable.
 */
export function createDatabase(databaseUrl: string): Database {
  const client = postgres(databaseUrl);
  const db = drizzle(client);

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
