import type { MiddlewareHandler } from "hono";
import { requestId } from "hono/request-id";
import type { AppEnv } from "./app-env.ts";

/**
 * Wraps `hono/request-id`: reuses a valid incoming `X-Request-Id` header
 * (Hono's default length limit and charset), otherwise generates one.
 */
export function requestIdMiddleware(generateId: () => string): MiddlewareHandler<AppEnv> {
  return requestId({ generator: () => generateId() });
}
