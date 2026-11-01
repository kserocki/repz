import { describe, expect, it } from "vitest";
import { createTestApp } from "../../shared/testing/test-app.ts";

const REGISTER_BODY = {
  email: "alex@example.com",
  password: "correct horse battery staple",
  displayName: "Alex",
};

async function readJson(res: Response): Promise<Record<string, unknown>> {
  return (await res.json()) as Record<string, unknown>;
}

async function registerAndGetAccessToken(app: ReturnType<typeof createTestApp>["app"]) {
  const res = await app.request("/auth/register", {
    method: "POST",
    headers: { "content-type": "application/json" },
    body: JSON.stringify(REGISTER_BODY),
  });
  const body = await readJson(res);
  return body.accessToken as string;
}

describe("GET /me", () => {
  it("given a valid access token, when requested, then it responds 200 with the user", async () => {
    const { app } = createTestApp();
    const accessToken = await registerAndGetAccessToken(app);

    const res = await app.request("/me", { headers: { Authorization: `Bearer ${accessToken}` } });

    expect(res.status).toBe(200);
    const body = await readJson(res);
    expect(body).toMatchObject({
      email: "alex@example.com",
      displayName: "Alex",
      unitSystem: "kg",
    });
    expect(body.passwordHash).toBeUndefined();
  });

  it("given no Authorization header, when requested, then it responds 401 with WWW-Authenticate: Bearer", async () => {
    const { app } = createTestApp();

    const res = await app.request("/me");

    expect(res.status).toBe(401);
    expect(res.headers.get("WWW-Authenticate")).toBe("Bearer");
    const body = await readJson(res);
    expect(body.type).toBe("urn:repz:problem:invalid-access-token");
  });

  it("given a non-bearer scheme, when requested, then it responds 401 with WWW-Authenticate: Bearer", async () => {
    const { app } = createTestApp();

    const res = await app.request("/me", { headers: { Authorization: "Basic dXNlcjpwYXNz" } });

    expect(res.status).toBe(401);
    expect(res.headers.get("WWW-Authenticate")).toBe("Bearer");
  });

  it('given a malformed bearer token, when requested, then it responds 401 with WWW-Authenticate: Bearer error="invalid_token"', async () => {
    const { app } = createTestApp();

    const res = await app.request("/me", { headers: { Authorization: "Bearer not-a-jwt" } });

    expect(res.status).toBe(401);
    expect(res.headers.get("WWW-Authenticate")).toBe('Bearer error="invalid_token"');
  });

  it("given an expired access token, when requested, then it responds 401 invalid-access-token", async () => {
    const { app, clock } = createTestApp({ auth: { accessTokenTtlSeconds: 5 } });
    const accessToken = await registerAndGetAccessToken(app);

    clock.advance(6_000);
    const res = await app.request("/me", { headers: { Authorization: `Bearer ${accessToken}` } });

    expect(res.status).toBe(401);
    const body = await readJson(res);
    expect(body.type).toBe("urn:repz:problem:invalid-access-token");
  });

  it("given a valid token whose user no longer exists in the store, when requested, then it responds 401 invalid-access-token", async () => {
    // The in-memory fake has no delete; a fresh app (same fixed test JWT secret, empty store)
    // verifies the signature fine but can't resolve `sub` to a user, exactly like a deleted one.
    const { app: issuerApp } = createTestApp();
    const accessToken = await registerAndGetAccessToken(issuerApp);
    const { app: emptyStoreApp } = createTestApp();

    const res = await emptyStoreApp.request("/me", {
      headers: { Authorization: `Bearer ${accessToken}` },
    });

    expect(res.status).toBe(401);
    const problem = await readJson(res);
    expect(problem.type).toBe("urn:repz:problem:invalid-access-token");
  });
});

describe("PATCH /me", () => {
  it("given a new unitSystem, when patched, then it responds 200 with the change applied", async () => {
    const { app } = createTestApp();
    const accessToken = await registerAndGetAccessToken(app);

    const res = await app.request("/me", {
      method: "PATCH",
      headers: { Authorization: `Bearer ${accessToken}`, "content-type": "application/json" },
      body: JSON.stringify({ unitSystem: "lb" }),
    });

    expect(res.status).toBe(200);
    const body = await readJson(res);
    expect(body.unitSystem).toBe("lb");
  });

  it("given a displayName with surrounding whitespace, when patched, then it responds 200 with it trimmed", async () => {
    const { app } = createTestApp();
    const accessToken = await registerAndGetAccessToken(app);

    const res = await app.request("/me", {
      method: "PATCH",
      headers: { Authorization: `Bearer ${accessToken}`, "content-type": "application/json" },
      body: JSON.stringify({ displayName: "  Al  " }),
    });

    expect(res.status).toBe(200);
    const body = await readJson(res);
    expect(body.displayName).toBe("Al");
  });

  it("given both fields, when patched, then it responds 200 with both applied", async () => {
    const { app } = createTestApp();
    const accessToken = await registerAndGetAccessToken(app);

    const res = await app.request("/me", {
      method: "PATCH",
      headers: { Authorization: `Bearer ${accessToken}`, "content-type": "application/json" },
      body: JSON.stringify({ displayName: "Robin", unitSystem: "lb" }),
    });

    expect(res.status).toBe(200);
    const body = await readJson(res);
    expect(body).toMatchObject({ displayName: "Robin", unitSystem: "lb" });
  });

  it.each([
    ["an empty body", {}],
    ["an unknown key", { email: "new@example.com" }],
    ["an invalid unit system", { unitSystem: "stone" }],
  ])("given %s, when patched, then it responds 400 validation-error", async (_label, body) => {
    const { app } = createTestApp();
    const accessToken = await registerAndGetAccessToken(app);

    const res = await app.request("/me", {
      method: "PATCH",
      headers: { Authorization: `Bearer ${accessToken}`, "content-type": "application/json" },
      body: JSON.stringify(body),
    });

    expect(res.status).toBe(400);
    const problem = await readJson(res);
    expect(problem.type).toBe("urn:repz:problem:validation-error");
  });

  it("given no Authorization header, when patched, then it responds 401", async () => {
    const { app } = createTestApp();

    const res = await app.request("/me", {
      method: "PATCH",
      headers: { "content-type": "application/json" },
      body: JSON.stringify({ unitSystem: "lb" }),
    });

    expect(res.status).toBe(401);
  });
});
