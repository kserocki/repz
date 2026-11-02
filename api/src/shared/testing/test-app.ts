import type { OpenAPIHono } from "@hono/zod-openapi";
import pino from "pino";
import { type AppDependencies, createApp } from "../../app.ts";
import type { AppEnv } from "../app-env.ts";
import { createFakePasswordHasher } from "./fake-password-hasher.ts";
import { createFixedClock, type FixedClock } from "./fixed-clock.ts";
import { createInMemoryAuthStore, type InMemoryAuthStore } from "./in-memory-auth-store.ts";
import { createInMemorySyncStore, type InMemorySyncStore } from "./in-memory-sync-store.ts";

const TEST_JWT_SECRET = "a".repeat(32);

export type TestAppOverrides = {
  logger?: AppDependencies["logger"];
  generateRequestId?: AppDependencies["generateRequestId"];
  generateId?: AppDependencies["generateId"];
  passwordHasher?: AppDependencies["passwordHasher"];
  clock?: FixedClock;
  store?: InMemoryAuthStore;
  syncStore?: InMemorySyncStore;
  auth?: Partial<AppDependencies["auth"]>;
};

export type TestApp = {
  app: OpenAPIHono<AppEnv>;
  clock: FixedClock;
  store: InMemoryAuthStore;
  syncStore: InMemorySyncStore;
};

/**
 * `createApp` wired entirely with DB-free fakes: an in-memory auth store, an in-memory sync
 * store, a fast deterministic password hasher and a settable clock. Returns the app alongside the
 * clock and both stores so a test can advance time (e.g. access/refresh token expiry) or inspect
 * what was stored, without reaching into `createApp`'s internals.
 */
export function createTestApp(overrides: TestAppOverrides = {}): TestApp {
  const generateId = overrides.generateId ?? (() => crypto.randomUUID());
  const clock = overrides.clock ?? createFixedClock(new Date("2026-01-01T00:00:00.000Z"));
  const store = overrides.store ?? createInMemoryAuthStore(generateId);
  const syncStore = overrides.syncStore ?? createInMemorySyncStore(clock);

  const app = createApp({
    logger: overrides.logger ?? pino({ level: "silent" }),
    generateRequestId: overrides.generateRequestId ?? (() => "test-request-id"),
    generateId,
    clock,
    passwordHasher: overrides.passwordHasher ?? createFakePasswordHasher(),
    auth: {
      jwtSecret: TEST_JWT_SECRET,
      accessTokenTtlSeconds: 900,
      refreshTokenTtlSeconds: 2_592_000,
      refreshTokenReuseGraceSeconds: 30,
      ...overrides.auth,
    },
    repositories: {
      users: store.users,
      authTransactions: store.transactions,
      syncTransactions: syncStore.transactions,
    },
  });

  return { app, clock, store, syncStore };
}
