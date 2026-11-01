import type { MiddlewareHandler } from "hono";
import { InvalidAccessTokenError } from "../modules/auth/auth.errors.ts";
import type { AuthenticatedAppEnv } from "./app-env.ts";

export type AccessTokenVerifier = (token: string) => Promise<{ userId: string }>;

const BEARER_SCHEME = /^bearer /i;

/**
 * Reads `Authorization: Bearer <token>` (scheme case-insensitive) and verifies it with the
 * injected verifier, setting `c.var.userId` on success. A missing header or a header using a
 * different scheme (RFC 6750 §3.1: no bearer credentials were presented) throws
 * `InvalidAccessTokenError("missing")`; `verify` is responsible for every other failure.
 * REP-15 mounts this on `/sync/*`.
 */
export function requireAuth(verify: AccessTokenVerifier): MiddlewareHandler<AuthenticatedAppEnv> {
  return async (c, next) => {
    const header = c.req.header("Authorization");
    if (!header || !BEARER_SCHEME.test(header)) {
      throw new InvalidAccessTokenError("missing");
    }

    const token = header.replace(BEARER_SCHEME, "");
    const { userId } = await verify(token);
    c.set("userId", userId);
    await next();
  };
}
