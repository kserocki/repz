import type pino from "pino";

/**
 * Hono generic `Env` shared by every router: request-scoped variables set by
 * `requestIdMiddleware` and `requestLoggerMiddleware` before routes run.
 */
export type AppEnv = {
  Variables: {
    requestId: string;
    logger: pino.Logger;
  };
};

/** `AppEnv` plus the user id `requireAuth` sets once a bearer token verifies. */
export type AuthenticatedAppEnv = AppEnv & { Variables: { userId: string } };
