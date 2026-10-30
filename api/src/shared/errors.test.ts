import { createRoute, z } from "@hono/zod-openapi";
import { HTTPException } from "hono/http-exception";
import pino from "pino";
import { describe, expect, it } from "vitest";
import { errorHandler, notFoundHandler } from "./errors.ts";
import type { ProblemDetails } from "./problem-details.ts";
import { requestIdMiddleware } from "./request-id.ts";
import { requestLoggerMiddleware } from "./request-logger.ts";
import { createRouter } from "./router.ts";
import { MemoryDestination } from "./testing/memory-destination.ts";

const EmptyResponseSchema = z.object({});

/** A throwaway app wiring the shared pieces plus test-only routes; never part of `createApp`. */
function createTestApp(destination: pino.DestinationStream = { write: () => {} }) {
  const app = createRouter();
  app.use(requestIdMiddleware(() => "test-request-id"));
  app.use(requestLoggerMiddleware(pino({ level: "info" }, destination)));

  app.openapi(
    createRoute({
      method: "get",
      path: "/boom",
      responses: {
        200: {
          description: "never",
          content: { "application/json": { schema: EmptyResponseSchema } },
        },
      },
    }),
    () => {
      throw new Error("secret detail");
    },
  );

  app.openapi(
    createRoute({
      method: "get",
      path: "/forbidden",
      responses: {
        200: {
          description: "never",
          content: { "application/json": { schema: EmptyResponseSchema } },
        },
      },
    }),
    () => {
      throw new HTTPException(403, { message: "Not allowed" });
    },
  );

  app.openapi(
    createRoute({
      method: "get",
      path: "/items",
      request: { query: z.object({ limit: z.coerce.number().int().positive() }) },
      responses: {
        200: {
          description: "ok",
          content: { "application/json": { schema: z.object({ limit: z.number() }) } },
        },
      },
    }),
    (c) => {
      const { limit } = c.req.valid("query");
      return c.json({ limit });
    },
  );

  app.openapi(
    createRoute({
      method: "post",
      path: "/echo",
      request: {
        body: { content: { "application/json": { schema: z.object({ name: z.string() }) } } },
      },
      responses: {
        200: {
          description: "ok",
          content: { "application/json": { schema: z.object({ name: z.string() }) } },
        },
      },
    }),
    (c) => {
      const body = c.req.valid("json");
      return c.json(body);
    },
  );

  app.notFound(notFoundHandler);
  app.onError(errorHandler);

  return app;
}

describe("notFoundHandler", () => {
  it("given an unknown path, when requested, then responds with a 404 problem", async () => {
    const app = createTestApp();

    const res = await app.request("/nope");

    expect(res.status).toBe(404);
    expect(res.headers.get("content-type")).toBe("application/problem+json");
    const body = await res.json();
    expect(body).toMatchObject({
      type: "about:blank",
      title: "Not Found",
      status: 404,
      instance: "/nope",
      requestId: "test-request-id",
    });
  });
});

describe("errorHandler", () => {
  it("given a handler that throws an unexpected Error, when requested, then responds 500 without the error message and logs it once with the request id", async () => {
    const destination = new MemoryDestination();
    const app = createTestApp(destination);

    const res = await app.request("/boom");

    expect(res.status).toBe(500);
    expect(res.headers.get("content-type")).toBe("application/problem+json");
    const text = await res.text();
    expect(text).not.toContain("secret detail");
    const body = JSON.parse(text);
    expect(body).toMatchObject({
      type: "about:blank",
      title: "Internal Server Error",
      status: 500,
    });
    expect(body.requestId).toBe("test-request-id");

    const errorEntries = destination.entries.filter((entry) => entry.level === 50);
    expect(errorEntries).toHaveLength(1);
    expect(errorEntries[0]).toMatchObject({
      requestId: "test-request-id",
      msg: "Unhandled error",
    });
    expect((errorEntries[0]?.err as { message?: string } | undefined)?.message).toBe(
      "secret detail",
    );
  });

  it("given a handler that throws an HTTPException, when requested, then responds with its status", async () => {
    const app = createTestApp();

    const res = await app.request("/forbidden");

    expect(res.status).toBe(403);
    const body = await res.json();
    expect(body).toMatchObject({
      type: "about:blank",
      title: "Forbidden",
      status: 403,
      detail: "Not allowed",
    });
  });

  it("given malformed JSON in a request body, when requested, then responds 400 problem details", async () => {
    const app = createTestApp();

    const res = await app.request("/echo", {
      method: "POST",
      headers: { "content-type": "application/json" },
      body: "{not-json",
    });

    expect(res.status).toBe(400);
    expect(res.headers.get("content-type")).toBe("application/problem+json");
    const body = (await res.json()) as ProblemDetails;
    expect(body).toMatchObject({ type: "about:blank", status: 400 });
    expect(body.detail).toMatch(/Malformed JSON/i);
  });
});

describe("validationErrorHook", () => {
  it("given an invalid query param, when requested, then responds 400 with a validation-error problem", async () => {
    const app = createTestApp();

    const res = await app.request("/items?limit=abc");

    expect(res.status).toBe(400);
    expect(res.headers.get("content-type")).toBe("application/problem+json");
    const body = (await res.json()) as ProblemDetails;
    expect(body.type).toBe("urn:repz:problem:validation-error");
    expect(body.status).toBe(400);
    expect(body.instance).toBe("/items");
    expect(body.errors?.[0]?.path).toBe("query.limit");
    expect(body.requestId).toBe("test-request-id");
  });
});
