import { createRoute, type OpenAPIHono } from "@hono/zod-openapi";
import type { AppEnv } from "../../shared/app-env.ts";
import { createRouter } from "../../shared/router.ts";
import { HealthResponseSchema } from "./health.schemas.ts";

const healthRoute = createRoute({
  method: "get",
  path: "/health",
  responses: {
    200: {
      description: "The process is alive.",
      content: { "application/json": { schema: HealthResponseSchema } },
    },
  },
});

/** Liveness only: no DB call (ADR-0016). The DB is checked once at boot instead. */
export function createHealthRoutes(): OpenAPIHono<AppEnv> {
  const router = createRouter();
  router.openapi(healthRoute, (c) => c.json({ status: "ok" as const }));
  return router;
}
