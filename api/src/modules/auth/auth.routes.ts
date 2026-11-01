import { createRoute, type OpenAPIHono } from "@hono/zod-openapi";
import type { AppEnv } from "../../shared/app-env.ts";
import { ProblemDetailsSchema } from "../../shared/problem-details.ts";
import { createRouter } from "../../shared/router.ts";
import { toUserResponse } from "../users/users.schemas.ts";
import {
  AuthSessionSchema,
  AuthTokensSchema,
  LoginRequestSchema,
  LogoutRequestSchema,
  RefreshRequestSchema,
  RegisterRequestSchema,
} from "./auth.schemas.ts";
import type { AuthService } from "./auth.service.ts";

const PROBLEM_DETAILS_CONTENT = {
  "application/problem+json": { schema: ProblemDetailsSchema },
};

const registerRoute = createRoute({
  method: "post",
  path: "/auth/register",
  request: { body: { content: { "application/json": { schema: RegisterRequestSchema } } } },
  responses: {
    201: {
      description: "The account was created.",
      content: { "application/json": { schema: AuthSessionSchema } },
    },
    400: { description: "The request body failed validation.", content: PROBLEM_DETAILS_CONTENT },
    409: { description: "The email is already registered.", content: PROBLEM_DETAILS_CONTENT },
  },
});

const loginRoute = createRoute({
  method: "post",
  path: "/auth/login",
  request: { body: { content: { "application/json": { schema: LoginRequestSchema } } } },
  responses: {
    200: {
      description: "Signed in.",
      content: { "application/json": { schema: AuthSessionSchema } },
    },
    400: { description: "The request body failed validation.", content: PROBLEM_DETAILS_CONTENT },
    401: { description: "The email or password is incorrect.", content: PROBLEM_DETAILS_CONTENT },
  },
});

const refreshRoute = createRoute({
  method: "post",
  path: "/auth/refresh",
  request: { body: { content: { "application/json": { schema: RefreshRequestSchema } } } },
  responses: {
    200: {
      description: "A new access/refresh token pair.",
      content: { "application/json": { schema: AuthTokensSchema } },
    },
    400: { description: "The request body failed validation.", content: PROBLEM_DETAILS_CONTENT },
    401: {
      description: "The refresh token is unknown, expired, revoked or reused.",
      content: PROBLEM_DETAILS_CONTENT,
    },
  },
});

const logoutRoute = createRoute({
  method: "post",
  path: "/auth/logout",
  request: { body: { content: { "application/json": { schema: LogoutRequestSchema } } } },
  responses: {
    204: { description: "The token's family was revoked, or there was nothing to revoke." },
    400: { description: "The request body failed validation.", content: PROBLEM_DETAILS_CONTENT },
  },
});

/**
 * `/auth/*`: register, login, refresh and logout. Every response here carries
 * `Cache-Control: no-store` (RFC 6749 §5.1), including error responses, so the header is set by
 * a `try/finally` middleware rather than per handler.
 */
export function createAuthRoutes(authService: AuthService): OpenAPIHono<AppEnv> {
  const router = createRouter();

  router.use("/auth/*", async (c, next) => {
    try {
      await next();
    } finally {
      c.header("Cache-Control", "no-store");
    }
  });

  router.openapi(registerRoute, async (c) => {
    const session = await authService.register(c.req.valid("json"), c.var.logger);
    return c.json({ ...session, user: toUserResponse(session.user) }, 201);
  });

  router.openapi(loginRoute, async (c) => {
    const session = await authService.login(c.req.valid("json"), c.var.logger);
    return c.json({ ...session, user: toUserResponse(session.user) }, 200);
  });

  router.openapi(refreshRoute, async (c) => {
    const { refreshToken } = c.req.valid("json");
    const tokens = await authService.refresh(refreshToken, c.var.logger);
    return c.json(tokens, 200);
  });

  router.openapi(logoutRoute, async (c) => {
    const { refreshToken } = c.req.valid("json");
    await authService.logout(refreshToken);
    return c.body(null, 204);
  });

  return router;
}
