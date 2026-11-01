import { createRoute, type OpenAPIHono, type z } from "@hono/zod-openapi";
import type { Context } from "hono";
import { bodyLimit } from "hono/body-limit";
import type { AuthenticatedAppEnv } from "../../shared/app-env.ts";
import { type AccessTokenVerifier, requireAuth } from "../../shared/auth.ts";
import { ProblemDetailsSchema, problemResponse } from "../../shared/problem-details.ts";
import { createRouter } from "../../shared/router.ts";
import {
  CustomExercisePayloadSchema,
  ExercisePayloadSchema,
  PUSH_BODY_MAX_BYTES,
  PullQuerySchema,
  PullResponseSchema,
  PushRequestSchema,
  PushResponseSchema,
  TemplatePayloadSchema,
  WorkoutPayloadSchema,
} from "./sync.schemas.ts";
import type { SyncService } from "./sync.service.ts";

const PROBLEM_DETAILS_CONTENT = {
  "application/problem+json": { schema: ProblemDetailsSchema },
};

const UNAUTHORIZED_RESPONSE = {
  description: "The access token is missing, invalid or expired.",
  content: PROBLEM_DETAILS_CONTENT,
};

function contentTooLarge(c: Context): Response {
  return problemResponse(c, {
    type: "about:blank",
    title: "Content Too Large",
    status: 413,
    instance: new URL(c.req.url).pathname,
  });
}

const pushRoute = createRoute({
  method: "post",
  path: "/sync/push",
  security: [{ bearerAuth: [] }],
  middleware: [bodyLimit({ maxSize: PUSH_BODY_MAX_BYTES, onError: contentTooLarge })] as const,
  request: {
    body: { content: { "application/json": { schema: PushRequestSchema } }, required: true },
  },
  responses: {
    200: {
      description: "Every change was decided; see each item's accepted/rejected outcome.",
      content: { "application/json": { schema: PushResponseSchema } },
    },
    400: {
      description: "The request body failed envelope validation.",
      content: PROBLEM_DETAILS_CONTENT,
    },
    401: UNAUTHORIZED_RESPONSE,
    413: {
      description: "The request body is over the size limit.",
      content: PROBLEM_DETAILS_CONTENT,
    },
  },
});

const pullRoute = createRoute({
  method: "get",
  path: "/sync/pull",
  security: [{ bearerAuth: [] }],
  request: { query: PullQuerySchema },
  responses: {
    200: {
      description: "Every change visible to the user with `version > cursor`, up to `limit`.",
      content: { "application/json": { schema: PullResponseSchema } },
    },
    400: {
      description: "The query parameters failed validation.",
      content: PROBLEM_DETAILS_CONTENT,
    },
    401: UNAUTHORIZED_RESPONSE,
  },
});

/**
 * `/sync/push` and `/sync/pull`, behind `requireAuth` and always `Cache-Control: no-store`
 * (3.1). Both middlewares are mounted scoped to `/sync/*` (never unscoped, which would turn them
 * into global middleware once this router is mounted at the app root).
 */
export function createSyncRoutes(
  syncService: SyncService,
  verifyAccessToken: AccessTokenVerifier,
): OpenAPIHono<AuthenticatedAppEnv> {
  const router = createRouter<AuthenticatedAppEnv>();

  // Explicitly registered so the push request's `oneOf` override (sync.schemas.ts) resolves to a
  // real component even though no route ever uses these schemas as a literal request validator.
  router.openAPIRegistry.register("WorkoutPayload", WorkoutPayloadSchema);
  router.openAPIRegistry.register("TemplatePayload", TemplatePayloadSchema);
  router.openAPIRegistry.register("CustomExercisePayload", CustomExercisePayloadSchema);
  router.openAPIRegistry.register("ExercisePayload", ExercisePayloadSchema);

  router.use("/sync/*", async (c, next) => {
    try {
      await next();
    } finally {
      c.header("Cache-Control", "no-store");
    }
  });
  router.use("/sync/*", requireAuth(verifyAccessToken));

  router.openapi(pushRoute, async (c) => {
    const { changes } = c.req.valid("json");
    const result = await syncService.push(c.var.userId, changes);
    return c.json(result, 200);
  });

  router.openapi(pullRoute, async (c) => {
    const { cursor, limit } = c.req.valid("query");
    const page = await syncService.pull(c.var.userId, cursor, limit);
    // The payloads come from `toWorkoutPayload`/`toTemplatePayload`/`toExercisePayload`, which
    // already produce the wire shape `SyncChangeSchema` describes; only `updatedAt` is mapped here.
    const body: z.infer<typeof PullResponseSchema> = {
      changes: page.changes.map((change) => ({
        ...change,
        updatedAt: change.updatedAt.toISOString(),
      })) as z.infer<typeof PullResponseSchema>["changes"],
      nextCursor: page.nextCursor,
      hasMore: page.hasMore,
      totalEstimate: page.totalEstimate,
    };
    return c.json(body, 200);
  });

  return router;
}
