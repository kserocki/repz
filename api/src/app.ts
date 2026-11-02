import type { OpenAPIHono } from "@hono/zod-openapi";
import { Scalar } from "@scalar/hono-api-reference";
import type pino from "pino";
import { createAccessTokens } from "./modules/auth/access-tokens.ts";
import type { AuthTransactions } from "./modules/auth/auth.repository.ts";
import { createAuthRoutes } from "./modules/auth/auth.routes.ts";
import { createAuthService } from "./modules/auth/auth.service.ts";
import type { PasswordHasher } from "./modules/auth/password-hasher.ts";
import { createHealthRoutes } from "./modules/health/health.routes.ts";
import { createCatalogExerciseSyncHandler } from "./modules/sync/handlers/catalog-exercise.handler.ts";
import { createCustomExerciseSyncHandler } from "./modules/sync/handlers/custom-exercise.handler.ts";
import { createSyncHandlerRegistry } from "./modules/sync/handlers/sync-handler.ts";
import { createTemplateSyncHandler } from "./modules/sync/handlers/template.handler.ts";
import { createWorkoutSyncHandler } from "./modules/sync/handlers/workout.handler.ts";
import { createSyncRoutes } from "./modules/sync/sync.routes.ts";
import { createSyncService } from "./modules/sync/sync.service.ts";
import type { SyncTransactions } from "./modules/sync/sync.types.ts";
import type { UsersRepository } from "./modules/users/users.repository.ts";
import { createUsersRoutes } from "./modules/users/users.routes.ts";
import { createUsersService } from "./modules/users/users.service.ts";
import type { AppEnv } from "./shared/app-env.ts";
import type { Clock } from "./shared/clock.ts";
import { errorHandler, notFoundHandler } from "./shared/errors.ts";
import { OPENAPI_DOCUMENT_CONFIG } from "./shared/openapi.ts";
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
    syncTransactions: SyncTransactions;
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
  });

  const usersService = createUsersService(deps.repositories.users);

  // Open/closed: a new syncable entity is a new handler added here, never a service edit.
  const syncRegistry = createSyncHandlerRegistry([
    createWorkoutSyncHandler(),
    createTemplateSyncHandler(),
    createCustomExerciseSyncHandler(),
    createCatalogExerciseSyncHandler(),
  ]);
  const syncService = createSyncService({
    transactions: deps.repositories.syncTransactions,
    registry: syncRegistry,
    logger: deps.logger,
  });

  // Read by the OpenAPI document generator; `/me` and `/sync/*` routes declare `security: [{ bearerAuth: [] }]`.
  app.openAPIRegistry.registerComponent("securitySchemes", "bearerAuth", {
    type: "http",
    scheme: "bearer",
    bearerFormat: "JWT",
  });

  app.route("/", createHealthRoutes());
  app.route("/", createAuthRoutes(authService));
  app.route("/", createUsersRoutes(usersService, accessTokens.verify));
  app.route("/", createSyncRoutes(syncService, accessTokens.verify));

  // `doc31`/`Scalar` add plain `GET` handlers directly (never through `.openapi(...)`), so neither
  // route is registered on `openAPIRegistry` and neither appears in the document it serves.
  app.doc31("/openapi.json", OPENAPI_DOCUMENT_CONFIG);
  app.get(
    "/docs",
    Scalar({
      url: "/openapi.json",
      pageTitle: "Repz API",
      cdn: "https://cdn.jsdelivr.net/npm/@scalar/api-reference@1.76.4",
      telemetry: false,
    }),
  );

  app.notFound(notFoundHandler);
  app.onError(errorHandler);

  return app;
}
