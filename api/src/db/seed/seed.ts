import { readFile } from "node:fs/promises";
import { drizzle } from "drizzle-orm/postgres-js";
import postgres from "postgres";
import { loadEnv, migrationEnvSchema } from "../../env.ts";
import { createLogger } from "../../shared/logger.ts";
import * as schema from "../schema/index.ts";
import { createCatalogSeedTransactions } from "./catalog-seed.repository.ts";
import { CatalogSeedConflictError, seedCatalog } from "./catalog-seed.service.ts";
import { SnapshotFileSchema, toCatalogExercise } from "./snapshot.schema.ts";

/**
 * `pnpm db:seed` (`-- --check` for a dry run): upserts the committed `exercises.json` into the
 * catalog, idempotently (§3.8). Validated against `migrationEnvSchema`, not the full `envSchema`:
 * seeding needs no `JWT_SECRET`, same as `migrate.ts`.
 */
const check = process.argv.includes("--check");

const env = loadEnv(process.env, migrationEnvSchema);
const logger = createLogger(env);
const client = postgres(env.DATABASE_URL, {
  max: 1,
  onnotice: (notice) => logger.debug({ notice }, "postgres notice"),
});
const db = drizzle(client, { schema });

let exitCode = 0;
try {
  const raw: unknown = JSON.parse(await readFile(`${import.meta.dirname}/exercises.json`, "utf8"));
  const snapshot = SnapshotFileSchema.parse(raw).exercises.map(toCatalogExercise);

  const transactions = createCatalogSeedTransactions(db);
  const summary = await seedCatalog(transactions, snapshot, { check });
  const pending = summary.inserted + summary.updated + summary.tombstoned;

  if (check) {
    logger.info({ ...summary }, "catalog seed check");
    exitCode = pending > 0 ? 1 : 0;
  } else {
    logger.info({ ...summary }, "catalog seeded");
  }
} catch (error) {
  if (error instanceof CatalogSeedConflictError) {
    logger.fatal({ conflict: error.conflict }, "catalog seed conflict; nothing written");
  } else {
    logger.fatal({ err: error, host: new URL(env.DATABASE_URL).host }, "catalog seed failed");
  }
  exitCode = 1;
} finally {
  await client.end();
}

process.exit(exitCode);
