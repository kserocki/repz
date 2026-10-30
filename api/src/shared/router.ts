import { OpenAPIHono } from "@hono/zod-openapi";
import type { AppEnv } from "./app-env.ts";
import { validationErrorHook } from "./errors.ts";

/** Every module router is built from this factory, so Zod validation failures are always Problem Details. */
export function createRouter(): OpenAPIHono<AppEnv> {
  return new OpenAPIHono<AppEnv>({ defaultHook: validationErrorHook });
}
