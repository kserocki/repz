import pino from "pino";
import { describe, expect, it } from "vitest";
import { requestIdMiddleware } from "./request-id.ts";
import { requestLoggerMiddleware } from "./request-logger.ts";
import { createRouter } from "./router.ts";

class MemoryDestination implements pino.DestinationStream {
  readonly lines: string[] = [];

  write(msg: string): void {
    this.lines.push(msg);
  }
}

function createTestApp(destination: MemoryDestination) {
  const logger = pino({ level: "info" }, destination);
  const app = createRouter();
  app.use(requestIdMiddleware(() => "req-1"));
  app.use(requestLoggerMiddleware(logger));
  app.get("/secure", (c) => c.text("ok"));
  return app;
}

describe("requestLoggerMiddleware", () => {
  it("given a request, when it completes, then logs exactly one line with method, path, status, durationMs and requestId", async () => {
    const destination = new MemoryDestination();
    const app = createTestApp(destination);

    const res = await app.request("/secure?token=abc");

    expect(res.status).toBe(200);
    expect(destination.lines).toHaveLength(1);
    const entry = JSON.parse(destination.lines[0] as string);
    expect(entry.method).toBe("GET");
    expect(entry.path).toBe("/secure");
    expect(entry.status).toBe(200);
    expect(entry.requestId).toBe("req-1");
    expect(typeof entry.durationMs).toBe("number");
  });

  it("given a request with an Authorization header, when logged, then the header value never appears in the output", async () => {
    const destination = new MemoryDestination();
    const app = createTestApp(destination);

    await app.request("/secure", { headers: { Authorization: "Bearer super-secret-token" } });

    const output = destination.lines.join("\n");
    expect(output).not.toContain("super-secret-token");
  });
});
