import pino from "pino";
import { describe, expect, it } from "vitest";
import { errorHandler, notFoundHandler } from "./errors.ts";
import { requestIdMiddleware } from "./request-id.ts";
import { requestLoggerMiddleware } from "./request-logger.ts";
import { createRouter } from "./router.ts";

function createTestApp(generateId: () => string) {
  const app = createRouter();
  app.use(requestIdMiddleware(generateId));
  app.use(requestLoggerMiddleware(pino({ level: "silent" })));
  app.notFound(notFoundHandler);
  app.onError(errorHandler);
  return app;
}

describe("requestIdMiddleware", () => {
  it("given no incoming header, when requested, then generates an id and sets it on the response header and the problem body", async () => {
    const app = createTestApp(() => "generated-id");

    const res = await app.request("/nope");

    expect(res.headers.get("x-request-id")).toBe("generated-id");
    const body = (await res.json()) as { requestId: string };
    expect(body.requestId).toBe("generated-id");
  });

  it("given a valid incoming id, when requested, then echoes it back", async () => {
    const app = createTestApp(() => "should-not-be-used");

    const res = await app.request("/nope", { headers: { "X-Request-Id": "client-id-123" } });

    expect(res.headers.get("x-request-id")).toBe("client-id-123");
  });

  it("given an incoming id with illegal characters, when requested, then replaces it with a generated one", async () => {
    const app = createTestApp(() => "generated-id");

    const res = await app.request("/nope", { headers: { "X-Request-Id": "has spaces/illegal!" } });

    expect(res.headers.get("x-request-id")).toBe("generated-id");
  });

  it("given an incoming id longer than 255 characters, when requested, then replaces it with a generated one", async () => {
    const app = createTestApp(() => "generated-id");
    const tooLong = "a".repeat(256);

    const res = await app.request("/nope", { headers: { "X-Request-Id": tooLong } });

    expect(res.headers.get("x-request-id")).toBe("generated-id");
  });
});
