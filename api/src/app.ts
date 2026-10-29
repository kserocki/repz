import { OpenAPIHono } from "@hono/zod-openapi";
import type pino from "pino";
import { createHealthRoutes } from "./modules/health/health.routes.ts";
import type { AppEnv } from "./shared/app-env.ts";
import { errorHandler, notFoundHandler } from "./shared/errors.ts";
import { requestIdMiddleware } from "./shared/request-id.ts";
import { requestLoggerMiddleware } from "./shared/request-logger.ts";

export type AppDependencies = {
  logger: pino.Logger;
  generateRequestId: () => string;
};

/**
 * The composition root: wires cross-cutting middleware and mounts every
 * module router. Later issues extend `AppDependencies` (e.g. a database) and
 * add their router here; no DI framework.
 */
export function createApp(deps: AppDependencies): OpenAPIHono<AppEnv> {
  const app = new OpenAPIHono<AppEnv>();

  app.use(requestIdMiddleware(deps.generateRequestId));
  app.use(requestLoggerMiddleware(deps.logger));

  app.route("/", createHealthRoutes());

  app.notFound(notFoundHandler);
  app.onError(errorHandler);

  return app;
}
