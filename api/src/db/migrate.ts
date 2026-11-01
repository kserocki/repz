import { drizzle } from "drizzle-orm/postgres-js";
import { migrate } from "drizzle-orm/postgres-js/migrator";
import postgres from "postgres";
import { loadEnv, migrationEnvSchema } from "../env.ts";
import { createLogger } from "../shared/logger.ts";

/**
 * Applies every committed migration to `DATABASE_URL`. A separate command from boot (ADR-0019):
 * no race between instances starting at once, and `/health` stays a pure liveness check.
 * Forward-only: migrations already on `main` are immutable, fixes are new migrations.
 * Validated against `migrationEnvSchema`, not the full `envSchema`: migrating needs no
 * `JWT_SECRET` (T6).
 */
const env = loadEnv(process.env, migrationEnvSchema);
const logger = createLogger(env);
const client = postgres(env.DATABASE_URL, {
  max: 1,
  onnotice: (notice) => logger.debug({ notice }, "postgres notice"),
});

let exitCode = 0;
try {
  await migrate(drizzle(client), { migrationsFolder: `${import.meta.dirname}/migrations` });
  logger.info("migrations applied");
} catch (error) {
  logger.fatal({ err: error, host: new URL(env.DATABASE_URL).host }, "migrations failed");
  exitCode = 1;
} finally {
  await client.end();
}

process.exit(exitCode);
