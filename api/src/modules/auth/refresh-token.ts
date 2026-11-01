import { createHash, randomBytes } from "node:crypto";

/**
 * 256 bits of entropy, opaque to the client. Returned once by register/login/refresh and never
 * stored or logged in this form.
 */
export function generateRefreshToken(): string {
  return randomBytes(32).toString("base64url");
}

/**
 * The value stored in `refresh_tokens.token_hash`. A fast hash is appropriate here (unlike a
 * password hash): the input already has 256 bits of entropy, so there is nothing to slow down
 * an attacker's guessing (T7).
 */
export function hashRefreshToken(token: string): string {
  return createHash("sha256").update(token).digest("hex");
}
