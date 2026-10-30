import pino from "pino";
import { describe, expect, it } from "vitest";
import { createApp } from "../../app.ts";

function createTestApp() {
  return createApp({
    logger: pino({ level: "silent" }),
    generateRequestId: () => "test-request-id",
  });
}

describe("GET /health", () => {
  it("given the app is running, when requested, then responds 200 with status ok", async () => {
    const app = createTestApp();

    const res = await app.request("/health");

    expect(res.status).toBe(200);
    expect(res.headers.get("content-type")).toMatch(/^application\/json/);
    expect(await res.json()).toEqual({ status: "ok" });
  });
});
