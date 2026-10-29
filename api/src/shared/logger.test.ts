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
});
