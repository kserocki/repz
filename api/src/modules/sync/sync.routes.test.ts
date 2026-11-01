import { describe, expect, it } from "vitest";
import { createTestApp } from "../../shared/testing/test-app.ts";
import {
  PULL_MAX_LIMIT,
  PUSH_BODY_MAX_BYTES,
  PullResponseSchema,
  PushResponseSchema,
} from "./sync.schemas.ts";

const REGISTER_BODY = {
  email: "alex@example.com",
  password: "correct horse battery staple",
  displayName: "Alex",
};

const VALID_WORKOUT_CHANGE = {
  type: "workout",
  id: "b6e6b6a0-2222-4a22-8a33-000000000001",
  deleted: false,
  payload: {
    templateId: null,
    name: "Push day",
    notes: "",
    startedAt: "2026-11-01T10:00:00.000Z",
    finishedAt: "2026-11-01T10:45:00.000Z",
    exercises: [],
  },
};

async function readJson(res: Response): Promise<Record<string, unknown>> {
  return (await res.json()) as Record<string, unknown>;
}

async function registerAndGetAccessToken(
  app: ReturnType<typeof createTestApp>["app"],
): Promise<string> {
  const res = await app.request("/auth/register", {
    method: "POST",
    headers: { "content-type": "application/json" },
    body: JSON.stringify(REGISTER_BODY),
  });
  const body = await readJson(res);
  return body.accessToken as string;
}

async function push(
  app: ReturnType<typeof createTestApp>["app"],
  token: string,
  body: unknown,
): Promise<Response> {
  return app.request("/sync/push", {
    method: "POST",
    headers: { Authorization: `Bearer ${token}`, "content-type": "application/json" },
    body: JSON.stringify(body),
  });
}

async function pull(
  app: ReturnType<typeof createTestApp>["app"],
  token: string,
  query = "",
): Promise<Response> {
  return app.request(`/sync/pull${query}`, { headers: { Authorization: `Bearer ${token}` } });
}

describe("POST /sync/push", () => {
  it("given no Authorization header, when pushed, then it responds 401 with Cache-Control: no-store", async () => {
    const { app } = createTestApp();

    const res = await app.request("/sync/push", {
      method: "POST",
      headers: { "content-type": "application/json" },
      body: JSON.stringify({ changes: [VALID_WORKOUT_CHANGE] }),
    });

    expect(res.status).toBe(401);
    expect(res.headers.get("cache-control")).toBe("no-store");
  });

  it("given an invalid bearer token, when pushed, then it responds 401", async () => {
    const { app } = createTestApp();

    const res = await push(app, "not-a-jwt", { changes: [VALID_WORKOUT_CHANGE] });

    expect(res.status).toBe(401);
  });

  it("given a valid change, when pushed, then it responds 200 with Cache-Control: no-store and a response matching PushResponseSchema", async () => {
    const { app } = createTestApp();
    const token = await registerAndGetAccessToken(app);

    const res = await push(app, token, { changes: [VALID_WORKOUT_CHANGE] });

    expect(res.status).toBe(200);
    expect(res.headers.get("cache-control")).toBe("no-store");
    const body = await readJson(res);
    expect(() => PushResponseSchema.parse(body)).not.toThrow();
    expect(body).toMatchObject({
      accepted: [{ type: "workout", id: VALID_WORKOUT_CHANGE.id, deleted: false }],
      rejected: [],
    });
  });

  it.each([
    ["zero changes", { changes: [] }],
    ["a duplicate (type, id) pair", { changes: [VALID_WORKOUT_CHANGE, VALID_WORKOUT_CHANGE] }],
    [
      "deleted: true with a payload",
      { changes: [{ type: "workout", id: VALID_WORKOUT_CHANGE.id, deleted: true, payload: {} }] },
    ],
    [
      "an unknown type",
      { changes: [{ type: "unknown", id: VALID_WORKOUT_CHANGE.id, deleted: true }] },
    ],
  ])("given %s, when pushed, then it responds 400 validation-error", async (_label, body) => {
    const { app } = createTestApp();
    const token = await registerAndGetAccessToken(app);

    const res = await push(app, token, body);

    expect(res.status).toBe(400);
    const problem = await readJson(res);
    expect(problem.type).toBe("urn:repz:problem:validation-error");
  });

  it("given a payload that fails per-item validation, when pushed, then the envelope still responds 200 with that item rejected", async () => {
    const { app } = createTestApp();
    const token = await registerAndGetAccessToken(app);

    const res = await push(app, token, {
      changes: [
        { ...VALID_WORKOUT_CHANGE, payload: { ...VALID_WORKOUT_CHANGE.payload, name: "" } },
      ],
    });

    expect(res.status).toBe(200);
    const body = await readJson(res);
    expect(body).toMatchObject({
      accepted: [],
      rejected: [
        {
          type: "workout",
          id: VALID_WORKOUT_CHANGE.id,
          problem: { type: "urn:repz:problem:validation-error" },
        },
      ],
    });
  });

  it("given a body over the size limit, when pushed, then it responds 413 Content Too Large", async () => {
    const { app } = createTestApp();
    const token = await registerAndGetAccessToken(app);
    const oversizedBody = JSON.stringify({
      changes: [
        {
          ...VALID_WORKOUT_CHANGE,
          payload: { ...VALID_WORKOUT_CHANGE.payload, notes: "a".repeat(PUSH_BODY_MAX_BYTES + 1) },
        },
      ],
    });

    const res = await app.request("/sync/push", {
      method: "POST",
      headers: { Authorization: `Bearer ${token}`, "content-type": "application/json" },
      body: oversizedBody,
    });

    expect(res.status).toBe(413);
    const body = await readJson(res);
    expect(body).toMatchObject({ type: "about:blank", title: "Content Too Large", status: 413 });
  });
});

describe("GET /sync/pull", () => {
  it("given no Authorization header, when pulled, then it responds 401 with Cache-Control: no-store", async () => {
    const { app } = createTestApp();

    const res = await app.request("/sync/pull");

    expect(res.status).toBe(401);
    expect(res.headers.get("cache-control")).toBe("no-store");
  });

  it("given a valid token and no query, when pulled, then it responds 200 with a response matching PullResponseSchema", async () => {
    const { app } = createTestApp();
    const token = await registerAndGetAccessToken(app);

    const res = await pull(app, token);

    expect(res.status).toBe(200);
    expect(res.headers.get("cache-control")).toBe("no-store");
    const body = await readJson(res);
    expect(() => PullResponseSchema.parse(body)).not.toThrow();
    expect(body).toMatchObject({ changes: [], hasMore: false, nextCursor: 0 });
  });

  it("given a push followed by a pull, when pulled, then the pushed change comes back", async () => {
    const { app } = createTestApp();
    const token = await registerAndGetAccessToken(app);
    await push(app, token, { changes: [VALID_WORKOUT_CHANGE] });

    const res = await pull(app, token);

    const body = await readJson(res);
    expect(body.changes).toEqual([
      expect.objectContaining({ type: "workout", id: VALID_WORKOUT_CHANGE.id, deleted: false }),
    ]);
  });

  it.each([
    ["limit=0", "?limit=0"],
    [`limit=${PULL_MAX_LIMIT + 1}`, `?limit=${PULL_MAX_LIMIT + 1}`],
    ["cursor=-1", "?cursor=-1"],
    ["cursor=abc", "?cursor=abc"],
  ])("given %s, when pulled, then it responds 400 validation-error", async (_label, query) => {
    const { app } = createTestApp();
    const token = await registerAndGetAccessToken(app);

    const res = await pull(app, token, query);

    expect(res.status).toBe(400);
    const problem = await readJson(res);
    expect(problem.type).toBe("urn:repz:problem:validation-error");
  });
});
