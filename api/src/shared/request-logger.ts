import type { MiddlewareHandler } from "hono";
import type pino from "pino";
import type { AppEnv } from "./app-env.ts";

/**
 * Sets a per-request child logger (`c.var.logger`) and logs exactly one
 * structured line per request: method, path (without the query string),
 * status and duration. Must run after `requestIdMiddleware`.
 */
export function requestLoggerMiddleware(logger: pino.Logger): MiddlewareHandler<AppEnv> {
  return async (c, next) => {
    const requestLogger = logger.child({ requestId: c.var.requestId });
    c.set("logger", requestLogger);

    const path = new URL(c.req.url).pathname;
    const method = c.req.method;
    const startedAt = performance.now();

    await next();

    const durationMs = performance.now() - startedAt;
    requestLogger.info(
      { method, path, status: c.res.status, durationMs: Math.round(durationMs * 100) / 100 },
      "request completed",
    );
  };
}
