import { describe, expect, it } from "vitest";
import type { RefreshTokenRecord } from "./auth.repository.ts";
import { decideRefresh } from "./refresh-rotation.ts";

const NOW = new Date("2026-01-01T00:00:00.000Z");
const FAR_FUTURE = new Date(NOW.getTime() + 30 * 24 * 60 * 60 * 1000);
const DEFAULT_GRACE_SECONDS = 30;

function token(overrides: Partial<RefreshTokenRecord> = {}): RefreshTokenRecord {
  return {
    id: "11111111-1111-4111-8111-111111111111",
    userId: "22222222-2222-4222-8222-222222222222",
    familyId: "33333333-3333-4333-8333-333333333333",
    expiresAt: FAR_FUTURE,
    revokedAt: null,
    replacedBy: null,
    ...overrides,
  };
}

describe("decideRefresh", () => {
  it("given no row for the hash, when decided, then it is invalid", () => {
    const decision = decideRefresh({
      presented: undefined,
      successor: undefined,
      now: NOW,
      graceSeconds: DEFAULT_GRACE_SECONDS,
    });

    expect(decision).toBe("invalid");
  });

  it("given an expired token, when decided, then it is expired", () => {
    const presented = token({ expiresAt: new Date(NOW.getTime() - 1_000) });

    const decision = decideRefresh({
      presented,
      successor: undefined,
      now: NOW,
      graceSeconds: DEFAULT_GRACE_SECONDS,
    });

    expect(decision).toBe("expired");
  });

  it("given an expired token that was also rotated with a live successor inside the grace window, when decided, then it is still expired", () => {
    const presented = token({
      expiresAt: new Date(NOW.getTime() - 1_000),
      revokedAt: new Date(NOW.getTime() - 500),
      replacedBy: "successor",
    });
    const successor = token({ id: "successor" });

    const decision = decideRefresh({
      presented,
      successor,
      now: NOW,
      graceSeconds: DEFAULT_GRACE_SECONDS,
    });

    expect(decision).toBe("expired");
  });

  it("given a live, never-rotated token, when decided, then it is rotate", () => {
    const presented = token();

    const decision = decideRefresh({
      presented,
      successor: undefined,
      now: NOW,
      graceSeconds: DEFAULT_GRACE_SECONDS,
    });

    expect(decision).toBe("rotate");
  });

  it("given a revoked token with no successor recorded (logout or an earlier reuse), when decided, then it is reuse", () => {
    const presented = token({ revokedAt: new Date(NOW.getTime() - 1_000), replacedBy: null });

    const decision = decideRefresh({
      presented,
      successor: undefined,
      now: NOW,
      graceSeconds: DEFAULT_GRACE_SECONDS,
    });

    expect(decision).toBe("reuse");
  });

  it("given a token rotated 29.999s ago with a live successor, when decided, then it is replay-within-grace", () => {
    const presented = token({
      revokedAt: new Date(NOW.getTime() - 29_999),
      replacedBy: "successor",
    });
    const successor = token({ id: "successor" });

    const decision = decideRefresh({
      presented,
      successor,
      now: NOW,
      graceSeconds: DEFAULT_GRACE_SECONDS,
    });

    expect(decision).toBe("replay-within-grace");
  });

  it("given a token rotated exactly 30s ago with a live successor, when decided, then it is reuse (the boundary is strict)", () => {
    const presented = token({
      revokedAt: new Date(NOW.getTime() - 30_000),
      replacedBy: "successor",
    });
    const successor = token({ id: "successor" });

    const decision = decideRefresh({
      presented,
      successor,
      now: NOW,
      graceSeconds: DEFAULT_GRACE_SECONDS,
    });

    expect(decision).toBe("reuse");
  });

  it("given a token rotated 31s ago with a live successor, when decided, then it is reuse", () => {
    const presented = token({
      revokedAt: new Date(NOW.getTime() - 31_000),
      replacedBy: "successor",
    });
    const successor = token({ id: "successor" });

    const decision = decideRefresh({
      presented,
      successor,
      now: NOW,
      graceSeconds: DEFAULT_GRACE_SECONDS,
    });

    expect(decision).toBe("reuse");
  });

  it("given the successor was already rotated, even inside the grace window, when decided, then it is reuse", () => {
    const presented = token({
      revokedAt: new Date(NOW.getTime() - 5_000),
      replacedBy: "successor",
    });
    const successor = token({ id: "successor", revokedAt: new Date(NOW.getTime() - 1_000) });

    const decision = decideRefresh({
      presented,
      successor,
      now: NOW,
      graceSeconds: DEFAULT_GRACE_SECONDS,
    });

    expect(decision).toBe("reuse");
  });

  it("given the successor was revoked by logout, even inside the grace window, when decided, then it is reuse", () => {
    const presented = token({
      revokedAt: new Date(NOW.getTime() - 5_000),
      replacedBy: "successor",
    });
    const successor = token({
      id: "successor",
      revokedAt: new Date(NOW.getTime() - 1_000),
      replacedBy: null,
    });

    const decision = decideRefresh({
      presented,
      successor,
      now: NOW,
      graceSeconds: DEFAULT_GRACE_SECONDS,
    });

    expect(decision).toBe("reuse");
  });

  it("given the successor has expired, when decided, then it is reuse", () => {
    const presented = token({
      revokedAt: new Date(NOW.getTime() - 5_000),
      replacedBy: "successor",
    });
    const successor = token({ id: "successor", expiresAt: new Date(NOW.getTime() - 1_000) });

    const decision = decideRefresh({
      presented,
      successor,
      now: NOW,
      graceSeconds: DEFAULT_GRACE_SECONDS,
    });

    expect(decision).toBe("reuse");
  });

  it("given the successor row is missing (its pointer was nulled by cleanup), when decided, then it is reuse", () => {
    const presented = token({
      revokedAt: new Date(NOW.getTime() - 5_000),
      replacedBy: "successor",
    });

    const decision = decideRefresh({
      presented,
      successor: undefined,
      now: NOW,
      graceSeconds: DEFAULT_GRACE_SECONDS,
    });

    expect(decision).toBe("reuse");
  });

  it("given graceSeconds = 0 (strict mode) with a just-rotated token and a live successor, when decided, then it is reuse", () => {
    const presented = token({ revokedAt: NOW, replacedBy: "successor" });
    const successor = token({ id: "successor" });

    const decision = decideRefresh({ presented, successor, now: NOW, graceSeconds: 0 });

    expect(decision).toBe("reuse");
  });
});
