import pino from "pino";
import { describe, expect, it } from "vitest";
import { requestIdMiddleware } from "./request-id.ts";
import { requestLoggerMiddleware } from "./request-logger.ts";
import { createRouter } from "./router.ts";
import { MemoryDestination } from "./testing/memory-destination.ts";

function createTestApp(destination: MemoryDestination) {
  const logger = pino({ level: "info" }, destination);
  const app = createRouter();
  app.use(requestIdMiddleware(() => "req-1"));
  app.use(requestLoggerMiddleware(logger));
  app.get("/secure", (c) => c.text("ok"));
  app.get("/boom", () => {
    throw new Error("unexpected");
  });
  app.onError((_error, c) => c.text("internal error", 500));
  return app;
}

describe("requestLoggerMiddleware", () => {
  it("given a request, when it completes, then logs exactly one line with method, path, status, durationMs and requestId", async () => {
    const destination = new MemoryDestination();
    const app = createTestApp(destination);

    const res = await app.request("/secure?token=abc");

    expect(res.status).toBe(200);
    expect(destination.entries).toHaveLength(1);
    const entry = destination.entries[0];
    expect(entry?.method).toBe("GET");
    expect(entry?.path).toBe("/secure");
    expect(entry?.status).toBe(200);
    expect(entry?.requestId).toBe("req-1");
    expect(typeof entry?.durationMs).toBe("number");
    expect(destination.lines.join("\n")).not.toContain("abc");
  });

  it("given a request with an Authorization header, when logged, then the header value never appears in the output", async () => {
    const destination = new MemoryDestination();
    const app = createTestApp(destination);

    await app.request("/secure", { headers: { Authorization: "Bearer super-secret-token" } });

    const output = destination.lines.join("\n");
    expect(output).not.toContain("super-secret-token");
  });

  it("given a handler that throws, when the error is handled, then still logs one line with status 500", async () => {
    const destination = new MemoryDestination();
    const app = createTestApp(destination);

    const res = await app.request("/boom");

    expect(res.status).toBe(500);
    expect(destination.entries).toHaveLength(1);
    expect(destination.entries[0]?.status).toBe(500);
    expect(destination.entries[0]?.path).toBe("/boom");
  });
});
