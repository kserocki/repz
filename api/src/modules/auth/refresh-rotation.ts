import type { RefreshTokenRecord } from "./auth.repository.ts";

export type RefreshDecision = "invalid" | "expired" | "rotate" | "replay-within-grace" | "reuse";

/**
 * Pure decision function for `POST /auth/refresh` (no I/O): given the presented token's row
 * and, if it was already rotated, its direct successor's row, decides what the transaction does
 * next. Evaluated in rule order: an unknown or expired token is rejected outright; a live token
 * rotates; a revoked token with no successor (logout, or an earlier reuse) is reuse; a revoked
 * token whose direct successor is still live, within the grace window, is a retried/concurrent
 * refresh rather than reuse; anything else is reuse (see ADR-0011 for the reuse/grace rule).
 */
export function decideRefresh(input: {
  presented: RefreshTokenRecord | undefined;
  successor: RefreshTokenRecord | undefined;
  now: Date;
  graceSeconds: number;
}): RefreshDecision {
  const { presented, successor, now, graceSeconds } = input;

  if (!presented) {
    return "invalid";
  }
  if (presented.expiresAt.getTime() <= now.getTime()) {
    return "expired";
  }
  if (presented.revokedAt === null) {
    return "rotate";
  }
  if (presented.replacedBy === null) {
    return "reuse";
  }

  const millisecondsSinceRotation = now.getTime() - presented.revokedAt.getTime();
  const withinGraceWindow = millisecondsSinceRotation < graceSeconds * 1000;
  const successorIsLive =
    successor !== undefined &&
    successor.revokedAt === null &&
    successor.expiresAt.getTime() > now.getTime();

  return withinGraceWindow && successorIsLive ? "replay-within-grace" : "reuse";
}
