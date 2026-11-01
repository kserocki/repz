import { describe, expect, it } from "vitest";
import { loadEnv, migrationEnvSchema } from "./env.ts";

const JWT_SECRET = "a".repeat(32);

const VALID = {
  DATABASE_URL: "postgres://repz:repz@localhost:5432/repz",
  JWT_SECRET,
};

describe("loadEnv", () => {
  it("given a valid env, when loaded, then parses it and fills in the defaults", () => {
    const env = loadEnv(VALID);

    expect(env).toEqual({
      NODE_ENV: "development",
      PORT: 3000,
      LOG_LEVEL: "info",
      DATABASE_URL: VALID.DATABASE_URL,
      JWT_SECRET,
      ACCESS_TOKEN_TTL_SECONDS: 900,
      REFRESH_TOKEN_TTL_SECONDS: 2_592_000,
      REFRESH_TOKEN_REUSE_GRACE_SECONDS: 30,
    });
  });

  it("given a missing DATABASE_URL, when loaded, then throws naming the variable", () => {
    expect(() => loadEnv({ JWT_SECRET })).toThrowError(/DATABASE_URL/);
  });

  it("given a non-numeric PORT, when loaded, then throws", () => {
    expect(() => loadEnv({ ...VALID, PORT: "abc" })).toThrowError(/PORT/);
  });

  it("given a PORT out of range, when loaded, then throws", () => {
    expect(() => loadEnv({ ...VALID, PORT: "70000" })).toThrowError(/PORT/);
  });

  it("given a non-postgres DATABASE_URL, when loaded, then throws", () => {
    expect(() =>
      loadEnv({ JWT_SECRET, DATABASE_URL: "http://not-postgres.example.com/db" }),
    ).toThrowError(/DATABASE_URL/);
  });

  it("given a missing JWT_SECRET, when loaded, then throws naming the variable", () => {
    expect(() => loadEnv({ DATABASE_URL: VALID.DATABASE_URL })).toThrowError(/JWT_SECRET/);
  });

  it("given a JWT_SECRET shorter than 32 characters, when loaded, then throws", () => {
    expect(() => loadEnv({ ...VALID, JWT_SECRET: "short" })).toThrowError(/JWT_SECRET/);
  });

  it.each([
    ["ACCESS_TOKEN_TTL_SECONDS", 900] as const,
    ["REFRESH_TOKEN_TTL_SECONDS", 2_592_000] as const,
    ["REFRESH_TOKEN_REUSE_GRACE_SECONDS", 30] as const,
  ])("given %s is not set, when loaded, then it defaults to %d", (key, fallback) => {
    const env = loadEnv(VALID);
    expect(env[key]).toBe(fallback);
  });

  it.each([
    ["ACCESS_TOKEN_TTL_SECONDS", "0"],
    ["ACCESS_TOKEN_TTL_SECONDS", "-1"],
    ["REFRESH_TOKEN_TTL_SECONDS", "0"],
    ["REFRESH_TOKEN_REUSE_GRACE_SECONDS", "-1"],
  ])("given %s=%s, when loaded, then throws", (key, value) => {
    expect(() => loadEnv({ ...VALID, [key]: value })).toThrowError(new RegExp(key));
  });

  it("given REFRESH_TOKEN_REUSE_GRACE_SECONDS=0, when loaded, then it is accepted (strict mode)", () => {
    const env = loadEnv({ ...VALID, REFRESH_TOKEN_REUSE_GRACE_SECONDS: "0" });
    expect(env.REFRESH_TOKEN_REUSE_GRACE_SECONDS).toBe(0);
  });

  it("given invalid values, when the error is thrown, then it never contains a supplied value", () => {
    const secretUrl = "postgres://repz:super-secret-password@localhost:99999/repz";

    let caught: Error | undefined;
    try {
      loadEnv({ JWT_SECRET, DATABASE_URL: secretUrl, PORT: "abc" });
    } catch (error) {
      caught = error as Error;
    }

    expect(caught).toBeDefined();
    const message = (caught as Error).message;
    expect(message).not.toContain("super-secret-password");
    expect(message).not.toContain("abc");
    expect(message).not.toContain(secretUrl);
  });

  it("given an invalid JWT_SECRET, when the error is thrown, then it never contains the secret", () => {
    expect(() => loadEnv({ ...VALID, JWT_SECRET: "short-secret-value" })).not.toThrowError(
      /short-secret-value/,
    );
  });

  it("given migrationEnvSchema and no JWT_SECRET, when loaded, then it succeeds", () => {
    const env = loadEnv({ DATABASE_URL: VALID.DATABASE_URL }, migrationEnvSchema);

    expect(env).toEqual({
      NODE_ENV: "development",
      LOG_LEVEL: "info",
      DATABASE_URL: VALID.DATABASE_URL,
    });
  });

  it("given migrationEnvSchema and a missing DATABASE_URL, when loaded, then throws", () => {
    expect(() => loadEnv({}, migrationEnvSchema)).toThrowError(/DATABASE_URL/);
  });
});
