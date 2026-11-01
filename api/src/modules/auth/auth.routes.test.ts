import { describe, expect, it } from "vitest";
import { createTestApp } from "../../shared/testing/test-app.ts";

const VALID_REGISTER_BODY = {
  email: "Alex@Example.com",
  password: "correct horse battery staple",
  displayName: "  Alex  ",
};

async function postJson(app: ReturnType<typeof createTestApp>["app"], path: string, body: unknown) {
  return app.request(path, {
    method: "POST",
    headers: { "content-type": "application/json" },
    body: JSON.stringify(body),
  });
}

async function readJson(res: Response): Promise<Record<string, unknown>> {
  return (await res.json()) as Record<string, unknown>;
}

describe("POST /auth/register", () => {
  it("given a new account, when registered, then it responds 201 with the session and Cache-Control: no-store", async () => {
    const { app } = createTestApp();

    const res = await postJson(app, "/auth/register", VALID_REGISTER_BODY);

    expect(res.status).toBe(201);
    expect(res.headers.get("cache-control")).toBe("no-store");
    const body = await readJson(res);
    expect(body).toMatchObject({
      accessToken: expect.any(String),
      refreshToken: expect.any(String),
      expiresIn: 900,
      user: {
        id: expect.any(String),
        email: "alex@example.com",
        displayName: "Alex",
        unitSystem: "kg",
      },
    });
    expect(JSON.stringify(body)).not.toContain("passwordHash");
  });

  it("given an email already registered in a different case, when registered again, then it responds 409 email-taken", async () => {
    const { app } = createTestApp();
    await postJson(app, "/auth/register", VALID_REGISTER_BODY);

    const res = await postJson(app, "/auth/register", {
      ...VALID_REGISTER_BODY,
      email: "ALEX@example.com",
    });

    expect(res.status).toBe(409);
    expect(res.headers.get("cache-control")).toBe("no-store");
    const body = await readJson(res);
    expect(body).toMatchObject({ type: "urn:repz:problem:email-taken", status: 409 });
  });

  it.each([
    ["a missing email", { ...VALID_REGISTER_BODY, email: undefined }],
    ["a malformed email", { ...VALID_REGISTER_BODY, email: "not-an-email" }],
    ["a 7-character password", { ...VALID_REGISTER_BODY, password: "1234567" }],
    ["a 129-character password", { ...VALID_REGISTER_BODY, password: "a".repeat(129) }],
    ["a blank display name", { ...VALID_REGISTER_BODY, displayName: "   " }],
    ["a 51-character display name", { ...VALID_REGISTER_BODY, displayName: "a".repeat(51) }],
  ])("given %s, when registered, then it responds 400 validation-error", async (_label, body) => {
    const { app } = createTestApp();

    const res = await postJson(app, "/auth/register", body);

    expect(res.status).toBe(400);
    const problem = await readJson(res);
    expect(problem.type).toBe("urn:repz:problem:validation-error");
  });
});

describe("POST /auth/login", () => {
  async function register(app: ReturnType<typeof createTestApp>["app"]) {
    return postJson(app, "/auth/register", VALID_REGISTER_BODY);
  }

  it("given correct credentials, when logging in, then it responds 200 with a session", async () => {
    const { app } = createTestApp();
    await register(app);

    const res = await postJson(app, "/auth/login", {
      email: "alex@example.com",
      password: VALID_REGISTER_BODY.password,
    });

    expect(res.status).toBe(200);
    expect(res.headers.get("cache-control")).toBe("no-store");
    const body = await readJson(res);
    expect((body.user as { email: string }).email).toBe("alex@example.com");
  });

  it("given a wrong password and an unknown email, when logging in, then both respond with the identical invalid-credentials body (except requestId)", async () => {
    const { app } = createTestApp();
    await register(app);

    const wrongPassword = await postJson(app, "/auth/login", {
      email: "alex@example.com",
      password: "not the password",
    });
    const unknownEmail = await postJson(app, "/auth/login", {
      email: "unknown@example.com",
      password: "whatever12",
    });

    expect(wrongPassword.status).toBe(401);
    expect(unknownEmail.status).toBe(401);
    const wrongPasswordBody = await readJson(wrongPassword);
    const unknownEmailBody = await readJson(unknownEmail);
    expect(wrongPasswordBody).toMatchObject({
      type: "urn:repz:problem:invalid-credentials",
      status: 401,
    });
    const { requestId: _requestId1, ...wrongPasswordRest } = wrongPasswordBody;
    const { requestId: _requestId2, ...unknownEmailRest } = unknownEmailBody;
    expect(wrongPasswordRest).toEqual(unknownEmailRest);
  });
});

describe("POST /auth/refresh", () => {
  async function registerAndGetTokens(app: ReturnType<typeof createTestApp>["app"]) {
    const res = await postJson(app, "/auth/register", VALID_REGISTER_BODY);
    return res.json() as Promise<{ refreshToken: string }>;
  }

  it("given a live refresh token, when refreshed, then it responds 200 with a new pair", async () => {
    const { app } = createTestApp();
    const session = await registerAndGetTokens(app);

    const res = await postJson(app, "/auth/refresh", { refreshToken: session.refreshToken });

    expect(res.status).toBe(200);
    expect(res.headers.get("cache-control")).toBe("no-store");
    const body = await readJson(res);
    expect(body.refreshToken).not.toBe(session.refreshToken);
    expect(body).toMatchObject({ accessToken: expect.any(String), expiresIn: 900 });
  });

  it("given a replay within the grace window, when refreshed twice with the original token, then both respond 200 with different pairs", async () => {
    const { app } = createTestApp();
    const session = await registerAndGetTokens(app);

    const first = await postJson(app, "/auth/refresh", { refreshToken: session.refreshToken });
    const second = await postJson(app, "/auth/refresh", { refreshToken: session.refreshToken });

    expect(first.status).toBe(200);
    expect(second.status).toBe(200);
    const firstBody = await readJson(first);
    const secondBody = await readJson(second);
    expect(firstBody.refreshToken).not.toBe(secondBody.refreshToken);
  });

  it("given an unknown refresh token, when refreshed, then it responds 401 invalid-refresh-token", async () => {
    const { app } = createTestApp();

    const res = await postJson(app, "/auth/refresh", { refreshToken: "unknown-token" });

    expect(res.status).toBe(401);
    expect(res.headers.get("cache-control")).toBe("no-store");
    const body = await readJson(res);
    expect(body.type).toBe("urn:repz:problem:invalid-refresh-token");
  });
});

describe("POST /auth/logout", () => {
  it("given a live refresh token, when logged out, then it responds 204 with Cache-Control: no-store", async () => {
    const { app } = createTestApp();
    const registerRes = await postJson(app, "/auth/register", VALID_REGISTER_BODY);
    const session = (await registerRes.json()) as { refreshToken: string };

    const res = await postJson(app, "/auth/logout", { refreshToken: session.refreshToken });

    expect(res.status).toBe(204);
    expect(res.headers.get("cache-control")).toBe("no-store");
  });

  it("given an unknown refresh token, when logged out, then it still responds 204", async () => {
    const { app } = createTestApp();

    const res = await postJson(app, "/auth/logout", { refreshToken: "unknown-token" });

    expect(res.status).toBe(204);
  });
});
