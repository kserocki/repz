import { drizzle } from "drizzle-orm/postgres-js";
import { migrate } from "drizzle-orm/postgres-js/migrator";
import postgres from "postgres";
import { loadEnv } from "../env.ts";
import { createLogger } from "../shared/logger.ts";

/**
 * Applies every committed migration to `DATABASE_URL`. A separate command from boot (ADR-0019
 * T12): no race between instances starting at once, and `/health` stays a pure liveness check.
 * Forward-only: migrations already on `main` are immutable, fixes are new migrations.
 */
const env = loadEnv(process.env);
const logger = createLogger(env);
const client = postgres(env.DATABASE_URL, { max: 1 });

try {
  await migrate(drizzle(client), { migrationsFolder: `${import.meta.dirname}/migrations` });
  logger.info("migrations applied");
  await client.end();
  process.exit(0);
} catch (error) {
  logger.fatal({ err: error, host: new URL(env.DATABASE_URL).host }, "migrations failed");
  await client.end();
  process.exit(1);
}
