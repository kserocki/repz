import { describe, expect, it } from "vitest";
import { loadEnv } from "./env.ts";

const VALID = {
  DATABASE_URL: "postgres://repz:repz@localhost:5432/repz",
};

describe("loadEnv", () => {
  it("given a valid env, when loaded, then parses it and fills in the defaults", () => {
    const env = loadEnv(VALID);

    expect(env).toEqual({
      NODE_ENV: "development",
      PORT: 3000,
      LOG_LEVEL: "info",
      DATABASE_URL: VALID.DATABASE_URL,
    });
  });

  it("given a missing DATABASE_URL, when loaded, then throws naming the variable", () => {
    expect(() => loadEnv({})).toThrowError(/DATABASE_URL/);
  });

  it("given a non-numeric PORT, when loaded, then throws", () => {
    expect(() => loadEnv({ ...VALID, PORT: "abc" })).toThrowError(/PORT/);
  });

  it("given a PORT out of range, when loaded, then throws", () => {
    expect(() => loadEnv({ ...VALID, PORT: "70000" })).toThrowError(/PORT/);
  });

  it("given a non-postgres DATABASE_URL, when loaded, then throws", () => {
    expect(() => loadEnv({ DATABASE_URL: "http://not-postgres.example.com/db" })).toThrowError(
      /DATABASE_URL/,
    );
  });

  it("given invalid values, when the error is thrown, then it never contains a supplied value", () => {
    const secretUrl = "postgres://repz:super-secret-password@localhost:99999/repz";

    let caught: Error | undefined;
    try {
      loadEnv({ DATABASE_URL: secretUrl, PORT: "abc" });
    } catch (error) {
      caught = error as Error;
    }

    expect(caught).toBeDefined();
    const message = (caught as Error).message;
    expect(message).not.toContain("super-secret-password");
    expect(message).not.toContain("abc");
    expect(message).not.toContain(secretUrl);
  });
});
