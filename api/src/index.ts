import { serve } from "@hono/node-server";
import { createApp } from "./app.ts";
import { createDatabase } from "./db/client.ts";
import { loadEnv } from "./env.ts";
import { createLogger } from "./shared/logger.ts";

const env = loadEnv(process.env);
const logger = createLogger(env);
const database = createDatabase(env.DATABASE_URL);

try {
  await database.ping();
} catch (error) {
  logger.fatal(
    { err: error, host: new URL(env.DATABASE_URL).host },
    "Database unreachable at boot",
  );
  process.exit(1);
}

const app = createApp({ logger, generateRequestId: () => crypto.randomUUID() });

const server = serve({ fetch: app.fetch, port: env.PORT }, (info) => {
  logger.info({ port: info.port }, "listening");
});

const SHUTDOWN_TIMEOUT_MS = 10_000;

let shuttingDown = false;

function shutdown(signal: NodeJS.Signals): void {
  if (shuttingDown) {
    logger.warn({ signal }, "shutdown already in progress, forcing exit");
    process.exit(1);
  }
  shuttingDown = true;
  logger.info({ signal }, "shutting down");

  const forceExitTimer = setTimeout(() => {
    logger.error({ timeoutMs: SHUTDOWN_TIMEOUT_MS }, "graceful shutdown timed out, forcing exit");
    process.exit(1);
  }, SHUTDOWN_TIMEOUT_MS);

  server.close((closeError) => {
    if (closeError) {
      logger.error({ err: closeError }, "error while closing the HTTP server");
    }

    database.close().then(
      () => {
        clearTimeout(forceExitTimer);
        process.exit(closeError ? 1 : 0);
      },
      (databaseCloseError: unknown) => {
        logger.error({ err: databaseCloseError }, "error while closing the database connection");
        clearTimeout(forceExitTimer);
        process.exit(1);
      },
    );
  });
}

process.on("SIGINT", shutdown);
process.on("SIGTERM", shutdown);
