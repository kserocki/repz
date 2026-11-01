import { ApplicationError } from "../../shared/application-error.ts";

/** `POST /auth/register` with an email already in use (`users_email_lower_key`). */
export class EmailTakenError extends ApplicationError {
  readonly type = "urn:repz:problem:email-taken";
  readonly title = "Email Taken";
  readonly status = 409;

  constructor() {
    super("Email is already registered");
  }
}

/**
 * `POST /auth/login` with an unknown email or a wrong password. Both cases throw this one
 * error so the response can never reveal which was wrong; the log line matches that intent by
 * never naming the attempted email either.
 */
export class InvalidCredentialsError extends ApplicationError {
  readonly type = "urn:repz:problem:invalid-credentials";
  readonly title = "Invalid Credentials";
  readonly status = 401;
  override readonly log = { level: "info" as const, message: "login failed", fields: {} };

  constructor() {
    super("Invalid email or password");
  }
}

/** `POST /auth/refresh` with an unknown or expired token. */
export class InvalidRefreshTokenError extends ApplicationError {
  readonly type = "urn:repz:problem:invalid-refresh-token";
  readonly title = "Invalid Refresh Token";
  readonly status = 401;

  constructor() {
    super("Invalid refresh token");
  }
}

/**
 * A refresh token was presented outside the reuse grace window after its family had already
 * rotated (ADR-0011). Same wire shape as `InvalidRefreshTokenError` (reuse is visible only in
 * the log, never in the response); the whole family is revoked, and committed, before this is
 * thrown.
 */
export class RefreshTokenReusedError extends ApplicationError {
  readonly type = "urn:repz:problem:invalid-refresh-token";
  readonly title = "Invalid Refresh Token";
  readonly status = 401;
  override readonly log: { level: "warn"; message: string; fields: Record<string, unknown> };

  constructor(userId: string, familyId: string) {
    super("Refresh token reuse detected");
    this.log = {
      level: "warn",
      message: "refresh token reuse detected",
      fields: { userId, familyId },
    };
  }
}

/**
 * A bearer token is missing, malformed, badly signed, has the wrong `iss`/`aud`/`alg`, is
 * expired, or names a user that no longer exists. `reason` picks the `WWW-Authenticate`
 * challenge (RFC 6750 §3.1): a request that presented no bearer credentials at all gets the
 * bare challenge, one that presented an invalid token gets `error="invalid_token"`.
 */
export class InvalidAccessTokenError extends ApplicationError {
  readonly type = "urn:repz:problem:invalid-access-token";
  readonly title = "Invalid Access Token";
  readonly status = 401;
  override readonly headers: Record<string, string>;

  constructor(reason: "missing" | "invalid") {
    super("Invalid access token");
    this.headers = {
      "WWW-Authenticate": reason === "missing" ? "Bearer" : 'Bearer error="invalid_token"',
    };
  }
}
