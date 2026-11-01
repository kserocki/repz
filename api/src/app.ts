import type { OpenAPIHono } from "@hono/zod-openapi";
import type pino from "pino";
import { createAccessTokens } from "./modules/auth/access-tokens.ts";
import type { AuthTransactions } from "./modules/auth/auth.repository.ts";
import { createAuthRoutes } from "./modules/auth/auth.routes.ts";
import { createAuthService } from "./modules/auth/auth.service.ts";
import type { PasswordHasher } from "./modules/auth/password-hasher.ts";
import { createHealthRoutes } from "./modules/health/health.routes.ts";
import type { UsersRepository } from "./modules/users/users.repository.ts";
import { createUsersRoutes } from "./modules/users/users.routes.ts";
import { createUsersService } from "./modules/users/users.service.ts";
import type { AppEnv } from "./shared/app-env.ts";
import type { Clock } from "./shared/clock.ts";
import { errorHandler, notFoundHandler } from "./shared/errors.ts";
import { requestIdMiddleware } from "./shared/request-id.ts";
import { requestLoggerMiddleware } from "./shared/request-logger.ts";
import { createRouter } from "./shared/router.ts";

export type AppDependencies = {
  logger: pino.Logger;
  generateRequestId: () => string;
  generateId: () => string;
  clock: Clock;
  passwordHasher: PasswordHasher;
  auth: {
    jwtSecret: string;
    accessTokenTtlSeconds: number;
    refreshTokenTtlSeconds: number;
    refreshTokenReuseGraceSeconds: number;
  };
  repositories: {
    users: UsersRepository;
    authTransactions: AuthTransactions;
  };
};

/**
 * The composition root: wires cross-cutting middleware, builds every service from plain factory
 * functions (no DI framework) and mounts every module router.
 */
export function createApp(deps: AppDependencies): OpenAPIHono<AppEnv> {
  const app = createRouter();

  app.use(requestIdMiddleware(deps.generateRequestId));
  app.use(requestLoggerMiddleware(deps.logger));

  const accessTokens = createAccessTokens(
    { secret: deps.auth.jwtSecret, ttlSeconds: deps.auth.accessTokenTtlSeconds },
    deps.clock,
  );

  const authService = createAuthService({
    transactions: deps.repositories.authTransactions,
    users: deps.repositories.users,
    passwordHasher: deps.passwordHasher,
    accessTokens,
    clock: deps.clock,
    generateId: deps.generateId,
    settings: {
      refreshTokenTtlSeconds: deps.auth.refreshTokenTtlSeconds,
      refreshTokenReuseGraceSeconds: deps.auth.refreshTokenReuseGraceSeconds,
    },
    logger: deps.logger,
  });

  const usersService = createUsersService(deps.repositories.users);

  // REP-17 reads this when it starts serving /openapi.json; `/me` routes declare `security`.
  app.openAPIRegistry.registerComponent("securitySchemes", "bearerAuth", {
    type: "http",
    scheme: "bearer",
    bearerFormat: "JWT",
  });

  app.route("/", createHealthRoutes());
  app.route("/", createAuthRoutes(authService));
  app.route("/", createUsersRoutes(usersService, accessTokens.verify));

  app.notFound(notFoundHandler);
  app.onError(errorHandler);

  return app;
}
