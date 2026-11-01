export type Clock = { now(): Date };

/** The real clock, injected everywhere in production. Tests use a fixed/advanceable fake instead. */
export const systemClock: Clock = { now: () => new Date() };
