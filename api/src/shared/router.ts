import { OpenAPIHono } from "@hono/zod-openapi";
import type { AppEnv } from "./app-env.ts";
import { validationErrorHook } from "./errors.ts";

/**
 * Every module router is built from this factory, so Zod validation failures are always Problem
 * Details. `E` defaults to the base `AppEnv`; a module that needs more context variables (e.g.
 * `AuthenticatedAppEnv`'s `userId`) passes its own env, since every such env extends `AppEnv`.
 */
export function createRouter<E extends AppEnv = AppEnv>(): OpenAPIHono<E> {
  return new OpenAPIHono<E>({ defaultHook: validationErrorHook<E> });
}
