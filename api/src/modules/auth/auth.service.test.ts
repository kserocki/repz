import pino from "pino";
import { describe, expect, it } from "vitest";
import { createFakePasswordHasher } from "../../shared/testing/fake-password-hasher.ts";
import { createFixedClock } from "../../shared/testing/fixed-clock.ts";
import { createInMemoryAuthStore } from "../../shared/testing/in-memory-auth-store.ts";
import { createAccessTokens } from "./access-tokens.ts";
import {
  EmailTakenError,
  InvalidCredentialsError,
  InvalidRefreshTokenError,
  RefreshTokenReusedError,
} from "./auth.errors.ts";
import { createAuthService } from "./auth.service.ts";
import { hashRefreshToken } from "./refresh-token.ts";

const JWT_SECRET = "a".repeat(32);
const ACCESS_TOKEN_TTL_SECONDS = 900;
const REFRESH_TOKEN_TTL_SECONDS = 2_592_000;
const GRACE_SECONDS = 30;
const NOW = new Date("2026-01-01T00:00:00.000Z");

function setup(graceSeconds = GRACE_SECONDS) {
  let nextId = 0;
  const generateId = () => `id-${nextId++}`;
  const clock = createFixedClock(NOW);
  const store = createInMemoryAuthStore(generateId);
  const accessTokens = createAccessTokens(
    { secret: JWT_SECRET, ttlSeconds: ACCESS_TOKEN_TTL_SECONDS },
    clock,
  );
  const service = createAuthService({
    transactions: store.transactions,
    users: store.users,
    passwordHasher: createFakePasswordHasher(),
    accessTokens,
    clock,
    generateId,
    settings: {
      refreshTokenTtlSeconds: REFRESH_TOKEN_TTL_SECONDS,
      refreshTokenReuseGraceSeconds: graceSeconds,
    },
    logger: pino({ level: "silent" }),
  });
  return { service, store, clock, generateId };
}

async function tokenRowFor(store: ReturnType<typeof createInMemoryAuthStore>, rawToken: string) {
  return store.refreshTokens.findByHashForUpdate(hashRefreshToken(rawToken));
}

describe("register", () => {
  it("given a new email, when registered, then it creates the user and one refresh token in a new family", async () => {
    const { service, store } = setup();

    const session = await service.register({
      email: "Alex@Example.com",
      password: "password123",
      displayName: "  Alex  ",
    });

    expect(session.user.email).toBe("alex@example.com");
    expect(session.user.displayName).toBe("Alex");
    expect(session.accessToken).toEqual(expect.any(String));
    expect(session.expiresIn).toBe(ACCESS_TOKEN_TTL_SECONDS);

    const row = await tokenRowFor(store, session.refreshToken);
    expect(row).toBeDefined();
    expect(row?.userId).toBe(session.user.id);
    expect(row?.familyId).toBeDefined();
    expect(row?.revokedAt).toBeNull();
  });

  it("given a registered password, when stored, then the stored hash is not the raw password", async () => {
    const { service, store } = setup();

    const session = await service.register({
      email: "alex@example.com",
      password: "password123",
      displayName: "Alex",
    });

    const stored = await store.users.findById(session.user.id);
    expect(stored?.passwordHash).not.toBe("password123");
  });

  it("given an email already registered in a different case, when registered again, then it throws EmailTakenError and creates no token", async () => {
    const { service, store } = setup();
    await service.register({
      email: "alex@example.com",
      password: "password123",
      displayName: "Alex",
    });
    const tokenCountBefore = store.listRefreshTokens().length;

    await expect(
      service.register({
        email: "ALEX@example.com",
        password: "password456",
        displayName: "Alex Two",
      }),
    ).rejects.toBeInstanceOf(EmailTakenError);

    expect(store.listRefreshTokens()).toHaveLength(tokenCountBefore);
  });
});

describe("login", () => {
  async function registerUser(service: ReturnType<typeof setup>["service"]) {
    return service.register({
      email: "alex@example.com",
      password: "password123",
      displayName: "Alex",
    });
  }

  it("given correct credentials, when logging in twice, then each login starts a new family", async () => {
    const { service } = setup();
    await registerUser(service);

    const first = await service.login({ email: "alex@example.com", password: "password123" });
    const second = await service.login({ email: "alex@example.com", password: "password123" });

    expect(first.refreshToken).not.toBe(second.refreshToken);
  });

  it("given a wrong password, when logging in, then it throws InvalidCredentialsError", async () => {
    const { service } = setup();
    await registerUser(service);

    await expect(
      service.login({ email: "alex@example.com", password: "wrong-password" }),
    ).rejects.toBeInstanceOf(InvalidCredentialsError);
  });

  it("given an unknown email, when logging in, then it throws the same InvalidCredentialsError and still calls the hasher's verify", async () => {
    let verifyCalls = 0;
    const store = createInMemoryAuthStore();
    const clock = createFixedClock(NOW);
    const accessTokens = createAccessTokens(
      { secret: JWT_SECRET, ttlSeconds: ACCESS_TOKEN_TTL_SECONDS },
      clock,
    );
    const countingHasher = {
      hash: async (password: string) => `hashed:${password}`,
      verify: async (_hash: string, _password: string) => {
        verifyCalls += 1;
        return false;
      },
    };
    const serviceWithCountingHasher = createAuthService({
      transactions: store.transactions,
      users: store.users,
      passwordHasher: countingHasher,
      accessTokens,
      clock,
      generateId: () => crypto.randomUUID(),
      settings: {
        refreshTokenTtlSeconds: REFRESH_TOKEN_TTL_SECONDS,
        refreshTokenReuseGraceSeconds: GRACE_SECONDS,
      },
      logger: pino({ level: "silent" }),
    });

    await expect(
      serviceWithCountingHasher.login({ email: "unknown@example.com", password: "whatever" }),
    ).rejects.toBeInstanceOf(InvalidCredentialsError);

    expect(verifyCalls).toBe(1);
  });
});

describe("refresh", () => {
  async function registerUser(service: ReturnType<typeof setup>["service"]) {
    return service.register({
      email: "alex@example.com",
      password: "password123",
      displayName: "Alex",
    });
  }

  it("given a live token, when refreshed, then the presented row is revoked with replacedBy set and the successor shares the family with a sliding expiry", async () => {
    const { service, store, clock } = setup();
    const session = await registerUser(service);
    const presentedBefore = await tokenRowFor(store, session.refreshToken);

    const tokens = await service.refresh(session.refreshToken);

    const presentedAfter = await tokenRowFor(store, session.refreshToken);
    const successor = await tokenRowFor(store, tokens.refreshToken);
    expect(presentedAfter?.revokedAt).toEqual(clock.now());
    expect(presentedAfter?.replacedBy).toBe(successor?.id);
    expect(successor?.familyId).toBe(presentedBefore?.familyId);
    expect(successor?.revokedAt).toBeNull();
    expect(successor?.expiresAt).toEqual(
      new Date(clock.now().getTime() + REFRESH_TOKEN_TTL_SECONDS * 1000),
    );
  });

  it("given a replay within the grace window, when refreshed again with the old token, then it gets a new sibling and the first successor stays live", async () => {
    const { service, store, clock } = setup();
    const session = await registerUser(service);
    const firstRotation = await service.refresh(session.refreshToken);

    clock.advance(10_000);
    const replay = await service.refresh(session.refreshToken);

    expect(replay.refreshToken).not.toBe(firstRotation.refreshToken);

    const presented = await tokenRowFor(store, session.refreshToken);
    const firstSuccessor = await tokenRowFor(store, firstRotation.refreshToken);
    const sibling = await tokenRowFor(store, replay.refreshToken);

    expect(presented?.replacedBy).toBe(firstSuccessor?.id);
    expect(firstSuccessor?.revokedAt).toBeNull();
    expect(sibling?.revokedAt).toBeNull();
    expect(sibling?.familyId).toBe(presented?.familyId);

    // The first successor still rotates normally afterwards (the family is still live).
    const rotatedAgain = await service.refresh(firstRotation.refreshToken);
    expect(rotatedAgain.refreshToken).toEqual(expect.any(String));
  });

  it("given two replays within the window followed by a replay after it, when refreshed, then every sibling is revoked (reuse)", async () => {
    const { service, store, clock } = setup();
    const session = await registerUser(service);
    const firstRotation = await service.refresh(session.refreshToken);

    clock.advance(5_000);
    const replayOne = await service.refresh(session.refreshToken);
    clock.advance(5_000);
    const replayTwo = await service.refresh(session.refreshToken);

    clock.advance(30_000);
    await expect(service.refresh(session.refreshToken)).rejects.toBeInstanceOf(
      RefreshTokenReusedError,
    );

    const firstSuccessor = await tokenRowFor(store, firstRotation.refreshToken);
    const siblingOne = await tokenRowFor(store, replayOne.refreshToken);
    const siblingTwo = await tokenRowFor(store, replayTwo.refreshToken);
    expect(firstSuccessor?.revokedAt).not.toBeNull();
    expect(siblingOne?.revokedAt).not.toBeNull();
    expect(siblingTwo?.revokedAt).not.toBeNull();
  });

  it("given a retry after the grace window has passed, when refreshed, then it throws RefreshTokenReusedError and revokes the whole family (even after the call returns)", async () => {
    const { service, store, clock } = setup();
    const session = await registerUser(service);
    const rotated = await service.refresh(session.refreshToken);

    clock.advance(31_000);
    await expect(service.refresh(session.refreshToken)).rejects.toBeInstanceOf(
      RefreshTokenReusedError,
    );

    const presented = await tokenRowFor(store, session.refreshToken);
    const successor = await tokenRowFor(store, rotated.refreshToken);
    expect(presented?.revokedAt).not.toBeNull();
    expect(successor?.revokedAt).not.toBeNull();
  });

  it("given a family revoked by reuse, when its live successor at sweep time is presented, then it is reuse again and the family stays revoked", async () => {
    const { service, store, clock } = setup();
    const session = await registerUser(service);
    const rotated = await service.refresh(session.refreshToken);

    clock.advance(31_000);
    await expect(service.refresh(session.refreshToken)).rejects.toBeInstanceOf(
      RefreshTokenReusedError,
    );
    const sweepTimestamp = clock.now();

    await expect(service.refresh(rotated.refreshToken)).rejects.toBeInstanceOf(
      RefreshTokenReusedError,
    );

    const successor = await tokenRowFor(store, rotated.refreshToken);
    expect(successor?.revokedAt).toEqual(sweepTimestamp);
  });

  it("given the successor was already rotated, when the oldest token is replayed inside the grace window, then it is reuse and the whole family (including the newest token) is revoked", async () => {
    const { service, store, clock } = setup();
    const session = await registerUser(service);
    const rotatedOnce = await service.refresh(session.refreshToken);
    const rotatedTwice = await service.refresh(rotatedOnce.refreshToken);

    clock.advance(5_000);
    await expect(service.refresh(session.refreshToken)).rejects.toBeInstanceOf(
      RefreshTokenReusedError,
    );

    const newest = await tokenRowFor(store, rotatedTwice.refreshToken);
    expect(newest?.revokedAt).not.toBeNull();
  });

  it("given a token rotated and then logged out, when the old token is replayed inside the grace window, then it is 401 and no sibling is created", async () => {
    const { service, store } = setup();
    const session = await registerUser(service);
    const rotated = await service.refresh(session.refreshToken);

    await service.logout(rotated.refreshToken);
    await expect(service.refresh(session.refreshToken)).rejects.toBeInstanceOf(
      RefreshTokenReusedError,
    );

    const presented = await tokenRowFor(store, session.refreshToken);
    const successor = await tokenRowFor(store, rotated.refreshToken);
    expect(presented?.replacedBy).toBe(successor?.id);
    expect(successor?.revokedAt).not.toBeNull();
  });

  it("given an expired token, when refreshed, then it throws InvalidRefreshTokenError and the family is untouched", async () => {
    const { service, store, clock } = setup();
    const session = await registerUser(service);
    clock.advance((REFRESH_TOKEN_TTL_SECONDS + 1) * 1000);

    await expect(service.refresh(session.refreshToken)).rejects.toBeInstanceOf(
      InvalidRefreshTokenError,
    );

    const presented = await tokenRowFor(store, session.refreshToken);
    expect(presented?.revokedAt).toBeNull();
  });

  it("given an unknown token, when refreshed, then it throws InvalidRefreshTokenError", async () => {
    const { service } = setup();

    await expect(service.refresh("unknown-token")).rejects.toBeInstanceOf(InvalidRefreshTokenError);
  });
});

describe("logout", () => {
  async function registerUser(service: ReturnType<typeof setup>["service"]) {
    return service.register({
      email: "alex@example.com",
      password: "password123",
      displayName: "Alex",
    });
  }

  it("given a live token, when logged out, then the whole family is revoked", async () => {
    const { service, store } = setup();
    const session = await registerUser(service);

    await service.logout(session.refreshToken);

    const presented = await tokenRowFor(store, session.refreshToken);
    expect(presented?.revokedAt).not.toBeNull();
    await expect(service.refresh(session.refreshToken)).rejects.toBeInstanceOf(
      RefreshTokenReusedError,
    );
  });

  it("given an unknown, expired or already revoked token, when logged out, then it is a no-op", async () => {
    const { service } = setup();
    const session = await registerUser(service);
    await service.logout(session.refreshToken);

    await expect(service.logout("unknown-token")).resolves.toBeUndefined();
    await expect(service.logout(session.refreshToken)).resolves.toBeUndefined();
  });

  it("given a second, unrelated login family, when one family is logged out, then the other is unaffected", async () => {
    const { service, store } = setup();
    const registerSession = await registerUser(service);
    const loginSession = await service.login({
      email: "alex@example.com",
      password: "password123",
    });

    await service.logout(registerSession.refreshToken);

    const loginRow = await tokenRowFor(store, loginSession.refreshToken);
    expect(loginRow?.revokedAt).toBeNull();
  });
});
