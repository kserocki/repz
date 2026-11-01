# ADR-0011: Custom JWT auth with rotating refresh tokens

- **Status:** Accepted (2026-10-28)

## Context
Auth is a strong interview topic and needs no third party locally.

## Decision
Access token 15 min (jose, HS256). Refresh token 30 days, stored hashed, rotated on every use. Reuse of an old token revokes the whole family. Passwords hashed with argon2 (`@node-rs/argon2`).

## Consequences
Tokens are stored in the platform Keychain/Keystore. Logout wipes the local DB (with an unsynced-changes warning).

## Details (REP-14, 2026-11-01)
- **Access token:** HS256 JWT (jose) with `sub`, `iat`, `exp`, `iss: repz-api` and `aud: repz-app`, verified with the algorithm pinned. The secret is `JWT_SECRET` (at least 32 characters). Lifetimes come from env and default to 15 min and 30 d.
- **Refresh token:** 256-bit random value, base64url, stored as SHA-256 hex. Every login or register starts a new family. The lifetime slides: every new token is valid for 30 days from its issue (user decision).
- **Rotation and reuse:** rotation runs in one transaction under a row lock. Reuse of a revoked token revokes every live token of its family, and the revoke is committed before the 401. A token that was swept with its family counts as reuse every time it is presented again, so each attempt is logged.
- **Strict reuse detection is relaxed by a grace window** (user decision; `REFRESH_TOKEN_REUSE_GRACE_SECONDS`, default 30, `0` = strict). A token rotated less than 30 s ago whose direct successor is still live gets a new pair in the same family instead of a family revoke. This stops a retried refresh after a lost response, or two concurrent refreshes, from signing the user out. Trade-off: a stolen token replayed within those 30 s isn't detected. A token revoked by logout or reuse, or one whose successor has already been rotated, never gets the grace. `refresh_tokens.replaced_by` records the successor.
- **Logout and passwords:** logout revokes the presented token's family and always answers 204. Passwords use argon2id with m = 19 MiB, t = 2, p = 1 (OWASP). Passwords are 8–128 characters with no composition rules (NIST 800-63B); emails are trimmed and stored lowercased (user decisions).
- **Failures** are Problem Details: `email-taken` 409, `invalid-credentials` 401, `invalid-refresh-token` 401 (reuse included, so reuse shows only in the log), `invalid-access-token` 401 with `WWW-Authenticate`. Registering returns 409 for a taken email, which accepts that anyone can check whether an email has an account (user decision).
- **Not yet:** rate limiting on `/auth/login` and `/auth/register`, and cleanup of expired or revoked tokens (follow-up issues).
