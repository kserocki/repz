import { DrizzleQueryError } from "drizzle-orm";
import { describe, expect, it } from "vitest";
import { createLogger } from "./logger.ts";
import { MemoryDestination } from "./testing/memory-destination.ts";

describe("createLogger", () => {
  it("given a value at a redacted path, when logged, then censors it instead of printing it", () => {
    const destination = new MemoryDestination();
    const logger = createLogger({ LOG_LEVEL: "info" }, destination);

    logger.info({ req: { headers: { authorization: "Bearer super-secret-token" } } }, "request");

    const output = destination.lines.join("\n");
    expect(output).not.toContain("super-secret-token");
    expect(destination.entries[0]?.req).toMatchObject({
      headers: { authorization: "[REDACTED]" },
    });
  });

  it("given a password field anywhere in the log object, when logged, then censors it", () => {
    const destination = new MemoryDestination();
    const logger = createLogger({ LOG_LEVEL: "info" }, destination);

    logger.info({ user: { password: "hunter2" } }, "user created");

    const output = destination.lines.join("\n");
    expect(output).not.toContain("hunter2");
  });

  it("given a configured level, when a lower-level message is logged, then it is suppressed", () => {
    const destination = new MemoryDestination();
    const logger = createLogger({ LOG_LEVEL: "warn" }, destination);

    logger.info({}, "should not appear");

    expect(destination.lines).toHaveLength(0);
  });

  it("given a DrizzleQueryError with bound params, when logged as err, then no param value appears, but the query text, name, code and constraint do", () => {
    const destination = new MemoryDestination();
    const logger = createLogger({ LOG_LEVEL: "info" }, destination);

    const secretNote = "do-not-leak-this-workout-note";
    const cause = Object.assign(
      new Error('duplicate key value violates unique constraint "workouts_pkey"'),
      {
        code: "23505",
        constraint_name: "workouts_pkey",
      },
    );
    const error = new DrizzleQueryError(
      'insert into "workouts" ("id", "name", "notes") values ($1, $2, $3)',
      ["b6e6b6a0-0000-0000-0000-000000000001", "Push day", secretNote],
      cause,
    );

    logger.error({ err: error }, "unexpected database error");

    const output = destination.lines.join("\n");
    expect(output).not.toContain(secretNote);
    expect(output).not.toContain("params");

    const entry = destination.entries[0];
    expect(entry?.err).toMatchObject({
      name: "DrizzleQueryError",
      message: 'Failed query: insert into "workouts" ("id", "name", "notes") values ($1, $2, $3)',
      code: "23505",
      constraintName: "workouts_pkey",
    });
  });
});
