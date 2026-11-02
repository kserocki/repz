import { writeFile } from "node:fs/promises";
import { fileURLToPath } from "node:url";
import { z } from "zod";
import { loadEnv } from "../env.ts";
import { createLogger } from "../shared/logger.ts";
import { buildOpenApiDocument, serializeOpenApiDocument } from "../shared/openapi.ts";
import { createTestApp } from "../shared/testing/test-app.ts";

const OPENAPI_JSON_PATH = fileURLToPath(new URL("../../openapi.json", import.meta.url));

const scriptEnvSchema = z.object({
  LOG_LEVEL: z.enum(["fatal", "error", "warn", "info", "debug", "trace", "silent"]).default("info"),
});
const env = loadEnv(process.env, scriptEnvSchema);
const logger = createLogger(env);

/**
 * `pnpm openapi:export`: writes the committed `api/openapi.json` from the same document config the
 * running API serves at `/openapi.json`. Built from `createTestApp()` (DB-free fakes): route
 * definitions, and therefore the generated document, never depend on a real database, so this runs
 * without Postgres or any environment variable beyond `LOG_LEVEL`.
 */
const { app } = createTestApp();
const document = buildOpenApiDocument(app);
await writeFile(OPENAPI_JSON_PATH, serializeOpenApiDocument(document));
logger.info({ path: OPENAPI_JSON_PATH }, "wrote openapi.json");
