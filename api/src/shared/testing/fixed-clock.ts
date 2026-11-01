import type { Clock } from "../clock.ts";

export type FixedClock = Clock & {
  set(date: Date): void;
  advance(milliseconds: number): void;
};

/** A settable, advanceable clock for deterministic tests (expiry, rotation, grace windows). */
export function createFixedClock(initial: Date): FixedClock {
  let current = initial;

  return {
    now: () => current,
    set(date) {
      current = date;
    },
    advance(milliseconds) {
      current = new Date(current.getTime() + milliseconds);
    },
  };
}
