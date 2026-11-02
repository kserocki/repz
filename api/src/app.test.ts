import { describe, expect, it } from "vitest";
import { createTestApp } from "./shared/testing/test-app.ts";

const REGISTER_BODY = {
  email: "alex@example.com",
  password: "correct horse battery staple",
  displayName: "Alex",
};

/**
 * Regression tests for a routing bug: `router.use(middleware)` with no path, registered inside
 * a module's own `OpenAPIHono` instance, becomes a global `/*` middleware once that router is
 * mounted at the app root with `app.route("/", ...)` — it is no longer scoped to that module's
 * own routes. Every `.use()` call in `auth.routes.ts`/`users.routes.ts` must name its path.
 */
describe("route mounting", () => {
  it("given an unknown path, when requested, then it is a plain 404 with no bearer or no-store headers from other routers", async () => {
    const { app } = createTestApp();

    const res = await app.request("/does-not-exist");

    expect(res.status).toBe(404);
    expect(res.headers.get("WWW-Authenticate")).toBeNull();
    expect(res.headers.get("Cache-Control")).not.toBe("no-store");
    const body = await res.json();
    expect(body).toMatchObject({ type: "about:blank", status: 404 });
  });

  it("given an unknown path under /auth, when requested, then it is still a plain 404, not an auth failure", async () => {
    const { app } = createTestApp();

    const res = await app.request("/auth/nope");

    expect(res.status).toBe(404);
    expect(res.headers.get("WWW-Authenticate")).toBeNull();
  });

  it("given GET /health, when requested, then it carries neither WWW-Authenticate nor Cache-Control: no-store", async () => {
    const { app } = createTestApp();

    const res = await app.request("/health");

    expect(res.status).toBe(200);
    expect(res.headers.get("WWW-Authenticate")).toBeNull();
    expect(res.headers.get("Cache-Control")).not.toBe("no-store");
  });

  it("given GET /me with no token, when requested, then it is still 401, scoped correctly to /me", async () => {
    const { app } = createTestApp();

    const res = await app.request("/me");

    expect(res.status).toBe(401);
    expect(res.headers.get("WWW-Authenticate")).toBe("Bearer");
  });

  it("given GET /me with a valid token, when requested, then the response does not carry the auth router's Cache-Control: no-store", async () => {
    const { app } = createTestApp();
    const registerRes = await app.request("/auth/register", {
      method: "POST",
      headers: { "content-type": "application/json" },
      body: JSON.stringify(REGISTER_BODY),
    });
    const { accessToken } = (await registerRes.json()) as { accessToken: string };

    const res = await app.request("/me", { headers: { Authorization: `Bearer ${accessToken}` } });

    expect(res.status).toBe(200);
    expect(res.headers.get("Cache-Control")).not.toBe("no-store");
  });

  it("given POST /auth/register, when requested, then it still carries Cache-Control: no-store", async () => {
    const { app } = createTestApp();

    const res = await app.request("/auth/register", {
      method: "POST",
      headers: { "content-type": "application/json" },
      body: JSON.stringify(REGISTER_BODY),
    });

    expect(res.headers.get("Cache-Control")).toBe("no-store");
  });

  it("given an unknown path under /sync with no token, when requested, then it is still 401, scoped to /sync/*", async () => {
    const { app } = createTestApp();

    const res = await app.request("/sync/nope");

    expect(res.status).toBe(401);
    expect(res.headers.get("WWW-Authenticate")).toBe("Bearer");
    expect(res.headers.get("Cache-Control")).toBe("no-store");
  });

  it("given GET /health, when requested, then it is still unaffected by the /sync/* middleware", async () => {
    const { app } = createTestApp();

    const res = await app.request("/health");

    expect(res.status).toBe(200);
    expect(res.headers.get("Cache-Control")).not.toBe("no-store");
  });
});
