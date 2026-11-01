import { createRoute, type OpenAPIHono } from "@hono/zod-openapi";
import type { AuthenticatedAppEnv } from "../../shared/app-env.ts";
import type { AccessTokenVerifier } from "../../shared/auth.ts";
import { requireAuth } from "../../shared/auth.ts";
import { ProblemDetailsSchema } from "../../shared/problem-details.ts";
import { createRouter } from "../../shared/router.ts";
import { toUserResponse, UpdateProfileRequestSchema, UserSchema } from "./users.schemas.ts";
import type { UsersService } from "./users.service.ts";

const PROBLEM_DETAILS_CONTENT = {
  "application/problem+json": { schema: ProblemDetailsSchema },
};

const UNAUTHORIZED_RESPONSE = {
  description: "The access token is missing, invalid or expired.",
  content: PROBLEM_DETAILS_CONTENT,
};

const getMeRoute = createRoute({
  method: "get",
  path: "/me",
  security: [{ bearerAuth: [] }],
  responses: {
    200: {
      description: "The signed-in user's profile.",
      content: { "application/json": { schema: UserSchema } },
    },
    401: UNAUTHORIZED_RESPONSE,
  },
});

const patchMeRoute = createRoute({
  method: "patch",
  path: "/me",
  security: [{ bearerAuth: [] }],
  request: { body: { content: { "application/json": { schema: UpdateProfileRequestSchema } } } },
  responses: {
    200: {
      description: "The updated profile.",
      content: { "application/json": { schema: UserSchema } },
    },
    400: { description: "The request body failed validation.", content: PROBLEM_DETAILS_CONTENT },
    401: UNAUTHORIZED_RESPONSE,
  },
});

/** `GET`/`PATCH /me`, behind `requireAuth`. REP-15 mounts `requireAuth` on `/sync/*` the same way. */
export function createUsersRoutes(
  usersService: UsersService,
  verifyAccessToken: AccessTokenVerifier,
): OpenAPIHono<AuthenticatedAppEnv> {
  const router = createRouter<AuthenticatedAppEnv>();

  router.use(requireAuth(verifyAccessToken));

  router.openapi(getMeRoute, async (c) => {
    const user = await usersService.getProfile(c.var.userId);
    return c.json(toUserResponse(user), 200);
  });

  router.openapi(patchMeRoute, async (c) => {
    const user = await usersService.updateProfile(c.var.userId, c.req.valid("json"));
    return c.json(toUserResponse(user), 200);
  });

  return router;
}
