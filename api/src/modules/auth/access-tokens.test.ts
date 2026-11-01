import { SignJWT } from "jose";
import { describe, expect, it } from "vitest";
import type { Clock } from "../../shared/clock.ts";
import { createAccessTokens } from "./access-tokens.ts";
import { InvalidAccessTokenError } from "./auth.errors.ts";

const SECRET = "a".repeat(32);
const TTL_SECONDS = 900;
const ISSUED_AT = new Date("2026-01-01T00:00:00.000Z");
const USER_ID = "3b3f7a3e-5f22-4a6b-9e1b-4a6a9c7d2c10";

/** A clock that never advances, so a test controls `iat`/`exp` and the verification instant exactly. */
function clockAt(date: Date): Clock {
  return { now: () => date };
}

function base64url(input: object | string): string {
  const json = typeof input === "string" ? input : JSON.stringify(input);
  return Buffer.from(json).toString("base64url");
}

describe("createAccessTokens", () => {
  it("given a freshly issued token, when verified before it expires, then it returns the user id", async () => {
    const tokens = createAccessTokens(
      { secret: SECRET, ttlSeconds: TTL_SECONDS },
      clockAt(ISSUED_AT),
    );

    const { accessToken, expiresIn } = await tokens.issue(USER_ID);
    const result = await tokens.verify(accessToken);

    expect(expiresIn).toBe(TTL_SECONDS);
    expect(result).toEqual({ userId: USER_ID });
  });

  it("given a token verified one second before its expiry instant, when verified, then it still succeeds", async () => {
    const tokens = createAccessTokens(
      { secret: SECRET, ttlSeconds: TTL_SECONDS },
      clockAt(ISSUED_AT),
    );
    const { accessToken } = await tokens.issue(USER_ID);

    const beforeExpiry = createAccessTokens(
      { secret: SECRET, ttlSeconds: TTL_SECONDS },
      clockAt(new Date(ISSUED_AT.getTime() + (TTL_SECONDS - 1) * 1000)),
    );

    await expect(beforeExpiry.verify(accessToken)).resolves.toEqual({ userId: USER_ID });
  });

  it("given a token verified at or after its expiry instant (iat + ttl), when verified, then it throws InvalidAccessTokenError", async () => {
    const tokens = createAccessTokens(
      { secret: SECRET, ttlSeconds: TTL_SECONDS },
      clockAt(ISSUED_AT),
    );
    const { accessToken } = await tokens.issue(USER_ID);

    const atExpiry = createAccessTokens(
      { secret: SECRET, ttlSeconds: TTL_SECONDS },
      clockAt(new Date(ISSUED_AT.getTime() + TTL_SECONDS * 1000)),
    );

    await expect(atExpiry.verify(accessToken)).rejects.toBeInstanceOf(InvalidAccessTokenError);
  });

  it("given a token signed with a different secret, when verified, then it throws InvalidAccessTokenError", async () => {
    const issuer = createAccessTokens(
      { secret: "b".repeat(32), ttlSeconds: TTL_SECONDS },
      clockAt(ISSUED_AT),
    );
    const verifier = createAccessTokens(
      { secret: SECRET, ttlSeconds: TTL_SECONDS },
      clockAt(ISSUED_AT),
    );
    const { accessToken } = await issuer.issue(USER_ID);

    await expect(verifier.verify(accessToken)).rejects.toBeInstanceOf(InvalidAccessTokenError);
  });

  it("given a token with the wrong issuer, when verified, then it throws InvalidAccessTokenError", async () => {
    const key = new TextEncoder().encode(SECRET);
    const issuedAtSeconds = Math.floor(ISSUED_AT.getTime() / 1000);
    const token = await new SignJWT({})
      .setProtectedHeader({ alg: "HS256" })
      .setSubject(USER_ID)
      .setIssuedAt(issuedAtSeconds)
      .setExpirationTime(issuedAtSeconds + TTL_SECONDS)
      .setIssuer("someone-else")
      .setAudience("repz-app")
      .sign(key);

    const tokens = createAccessTokens(
      { secret: SECRET, ttlSeconds: TTL_SECONDS },
      clockAt(ISSUED_AT),
    );

    await expect(tokens.verify(token)).rejects.toBeInstanceOf(InvalidAccessTokenError);
  });

  it("given a token with the wrong audience, when verified, then it throws InvalidAccessTokenError", async () => {
    const key = new TextEncoder().encode(SECRET);
    const issuedAtSeconds = Math.floor(ISSUED_AT.getTime() / 1000);
    const token = await new SignJWT({})
      .setProtectedHeader({ alg: "HS256" })
      .setSubject(USER_ID)
      .setIssuedAt(issuedAtSeconds)
      .setExpirationTime(issuedAtSeconds + TTL_SECONDS)
      .setIssuer("repz-api")
      .setAudience("someone-else")
      .sign(key);

    const tokens = createAccessTokens(
      { secret: SECRET, ttlSeconds: TTL_SECONDS },
      clockAt(ISSUED_AT),
    );

    await expect(tokens.verify(token)).rejects.toBeInstanceOf(InvalidAccessTokenError);
  });

  it("given an alg:none token, when verified, then it throws InvalidAccessTokenError", async () => {
    const header = base64url({ alg: "none", typ: "JWT" });
    const payload = base64url({
      sub: USER_ID,
      iss: "repz-api",
      aud: "repz-app",
      iat: Math.floor(ISSUED_AT.getTime() / 1000),
      exp: Math.floor(ISSUED_AT.getTime() / 1000) + TTL_SECONDS,
    });
    const token = `${header}.${payload}.`;

    const tokens = createAccessTokens(
      { secret: SECRET, ttlSeconds: TTL_SECONDS },
      clockAt(ISSUED_AT),
    );

    await expect(tokens.verify(token)).rejects.toBeInstanceOf(InvalidAccessTokenError);
  });

  it("given an HS512-signed token, when verified with the HS256-only verifier, then it throws InvalidAccessTokenError", async () => {
    const key = new TextEncoder().encode(SECRET);
    const issuedAtSeconds = Math.floor(ISSUED_AT.getTime() / 1000);
    const token = await new SignJWT({})
      .setProtectedHeader({ alg: "HS512" })
      .setSubject(USER_ID)
      .setIssuedAt(issuedAtSeconds)
      .setExpirationTime(issuedAtSeconds + TTL_SECONDS)
      .setIssuer("repz-api")
      .setAudience("repz-app")
      .sign(key);

    const tokens = createAccessTokens(
      { secret: SECRET, ttlSeconds: TTL_SECONDS },
      clockAt(ISSUED_AT),
    );

    await expect(tokens.verify(token)).rejects.toBeInstanceOf(InvalidAccessTokenError);
  });

  it("given a correctly signed token with no exp claim, when verified, then it throws InvalidAccessTokenError", async () => {
    const key = new TextEncoder().encode(SECRET);
    const issuedAtSeconds = Math.floor(ISSUED_AT.getTime() / 1000);
    const token = await new SignJWT({})
      .setProtectedHeader({ alg: "HS256" })
      .setSubject(USER_ID)
      .setIssuedAt(issuedAtSeconds)
      .setIssuer("repz-api")
      .setAudience("repz-app")
      .sign(key);

    const tokens = createAccessTokens(
      { secret: SECRET, ttlSeconds: TTL_SECONDS },
      clockAt(ISSUED_AT),
    );

    await expect(tokens.verify(token)).rejects.toBeInstanceOf(InvalidAccessTokenError);
  });

  it("given garbage input, when verified, then it throws InvalidAccessTokenError", async () => {
    const tokens = createAccessTokens(
      { secret: SECRET, ttlSeconds: TTL_SECONDS },
      clockAt(ISSUED_AT),
    );

    await expect(tokens.verify("not-a-jwt")).rejects.toBeInstanceOf(InvalidAccessTokenError);
  });
});
