import { describe, expect, it } from "vitest";
import { createInMemoryAuthStore } from "./in-memory-auth-store.ts";

const FAR_FUTURE = new Date("2099-01-01T00:00:00.000Z");

describe("createInMemoryAuthStore transactions", () => {
  it("given two runs on the same family, when the first run hasn't settled, then the second run's lockFamilyOfToken doesn't resolve until the first run settles", async () => {
    const store = createInMemoryAuthStore();
    await store.refreshTokens.insert({
      userId: "user-1",
      familyId: "family-1",
      tokenHash: "hash-1",
      expiresAt: FAR_FUTURE,
    });

    let releaseFirstRun!: () => void;
    const firstRunCanFinish = new Promise<void>((resolve) => {
      releaseFirstRun = resolve;
    });

    const firstRun = store.transactions.run(async ({ refreshTokens }) => {
      await refreshTokens.lockFamilyOfToken("hash-1");
      await firstRunCanFinish;
    });

    let secondRunAcquiredLock = false;
    const secondRun = store.transactions.run(async ({ refreshTokens }) => {
      await refreshTokens.lockFamilyOfToken("hash-1");
      secondRunAcquiredLock = true;
    });

    // Let every pending microtask settle without releasing the first run.
    await Promise.resolve();
    await Promise.resolve();
    await Promise.resolve();
    expect(secondRunAcquiredLock).toBe(false);

    releaseFirstRun();
    await firstRun;
    await secondRun;
    expect(secondRunAcquiredLock).toBe(true);
  });

  it("given two runs on different families, when the first run hasn't settled, then the second run's lockFamilyOfToken resolves without waiting", async () => {
    const store = createInMemoryAuthStore();
    await store.refreshTokens.insert({
      userId: "user-1",
      familyId: "family-1",
      tokenHash: "hash-1",
      expiresAt: FAR_FUTURE,
    });
    await store.refreshTokens.insert({
      userId: "user-2",
      familyId: "family-2",
      tokenHash: "hash-2",
      expiresAt: FAR_FUTURE,
    });

    let releaseFirstRun!: () => void;
    const firstRunCanFinish = new Promise<void>((resolve) => {
      releaseFirstRun = resolve;
    });

    const firstRun = store.transactions.run(async ({ refreshTokens }) => {
      await refreshTokens.lockFamilyOfToken("hash-1");
      await firstRunCanFinish;
    });

    const secondRun = store.transactions.run(({ refreshTokens }) =>
      refreshTokens.lockFamilyOfToken("hash-2"),
    );

    await expect(secondRun).resolves.toBe("family-2");

    releaseFirstRun();
    await firstRun;
  });
});
