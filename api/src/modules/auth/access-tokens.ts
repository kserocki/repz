import { jwtVerify, SignJWT } from "jose";
import type { AccessTokenVerifier } from "../../shared/auth.ts";
import type { Clock } from "../../shared/clock.ts";
import { InvalidAccessTokenError } from "./auth.errors.ts";

const ISSUER = "repz-api";
const AUDIENCE = "repz-app";

export type AccessTokenSettings = { secret: string; ttlSeconds: number };

export type AccessTokens = {
  issue(userId: string): Promise<{ accessToken: string; expiresIn: number }>;
  verify: AccessTokenVerifier;
};

/**
 * HS256 JWT access tokens (jose): `sub`/`iat`/`exp`/`iss`/`aud` only, no `jti` (nothing else in
 * the claims can change without a sign-out). Verification pins the algorithm, issuer and
 * audience so neither `alg: none` nor a token meant for another audience is accepted, and uses
 * the injected clock with no leeway, since the same server issues and verifies every token.
 */
export function createAccessTokens(settings: AccessTokenSettings, clock: Clock): AccessTokens {
  const key = new TextEncoder().encode(settings.secret);

  return {
    async issue(userId) {
      const issuedAtSeconds = Math.floor(clock.now().getTime() / 1000);
      const accessToken = await new SignJWT({})
        .setProtectedHeader({ alg: "HS256" })
        .setSubject(userId)
        .setIssuedAt(issuedAtSeconds)
        .setExpirationTime(issuedAtSeconds + settings.ttlSeconds)
        .setIssuer(ISSUER)
        .setAudience(AUDIENCE)
        .sign(key);

      return { accessToken, expiresIn: settings.ttlSeconds };
    },

    async verify(token) {
      let subject: string | undefined;
      try {
        const { payload } = await jwtVerify(token, key, {
          algorithms: ["HS256"],
          issuer: ISSUER,
          audience: AUDIENCE,
          currentDate: clock.now(),
        });
        subject = payload.sub;
      } catch {
        throw new InvalidAccessTokenError("invalid");
      }

      if (!subject) {
        throw new InvalidAccessTokenError("invalid");
      }
      return { userId: subject };
    },
  };
}
