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
- **Rotation and reuse:** every write to a token family (rotation, grace replay, reuse revoke, logout) first takes a transaction-scoped advisory lock on that family (`pg_advisory_xact_lock(1, hashtext(family_id))`, as its own statement, under READ COMMITTED). The writes are therefore serialised, and a revoke can never miss a successor that a concurrent rotation inserted. Rotation then runs under a row lock in the same transaction. Reuse of a revoked token revokes every live token of its family, and the revoke is committed before the 401. A token that was swept with its family counts as reuse every time it is presented again, so each attempt is logged.
- **Strict reuse detection is relaxed by a grace window** (user decision; `REFRESH_TOKEN_REUSE_GRACE_SECONDS`, default 30, `0` = strict). A token rotated less than 30 s ago whose direct successor is still live gets a new pair in the same family instead of a family revoke. This stops a retried refresh after a lost response, or two concurrent refreshes, from signing the user out. Trade-off: a stolen token replayed within those 30 s isn't detected. A token revoked by logout or reuse, or one whose successor has already been rotated, never gets the grace. `refresh_tokens.replaced_by` records the successor.
- **Logout and passwords:** logout revokes the presented token's family and always answers 204. Passwords use argon2id with m = 19 MiB, t = 2, p = 1 (OWASP). Passwords are 8–128 characters with no composition rules (NIST 800-63B); emails are trimmed and stored lowercased (user decisions).
- **Failures** are Problem Details: `email-taken` 409, `invalid-credentials` 401, `invalid-refresh-token` 401 (reuse included, so reuse shows only in the log), `invalid-access-token` 401 with `WWW-Authenticate`. Registering returns 409 for a taken email, which accepts that anyone can check whether an email has an account (user decision).
- **Not yet:** rate limiting on `/auth/login` and `/auth/register`, and cleanup of expired or revoked tokens (follow-up issues).

## Client (REP-25, 2026-11-03)

- **Two Ktor clients, one engine config:** `createRepzHttpClients` (`core:network/.../client/RepzHttpClients.kt`) builds `public` (no `Auth` plugin: `/health`, `/auth/*`, including the refresh call itself) and `authenticated` (`public` plus a bearer provider: `/me`, `/sync/*`). A login's own 401 can never trigger a refresh, and the refresh call can't recurse into refreshing itself.
- **Reactive refresh, single flight:** the bearer provider's `refreshTokens` callback runs through `TokenRefresher`, which wraps the server call in its own `Mutex` plus an "already refreshed" check (the caller's `oldTokens` against the current in-memory pair). N concurrent 401s produce exactly one `/auth/refresh` call.
- **Refresh outcome:** a `401` or `400` on the refresh call means the refresh token itself was rejected (expired, reused, or logged out elsewhere) — `SessionManager.expire()` clears storage and moves `status` to `SignedOut(Expired)`, and the original caller sees `NetworkError.SessionExpired` (`ApiCall`'s `hasJustExpired()` check). Offline, a timeout or a 5xx instead throw `RefreshUnavailableException`, which surfaces as that same transport `NetworkError` while leaving the tokens untouched — the grace window.
- **`nonCancellableRefresh = true`** on the bearer provider keeps a refresh running even if the request that triggered it is cancelled. `SessionManager.replace()` is a compare-and-set against the `AuthTokens` captured before the call started, so a concurrent `end()` (logout) discards the rotated pair instead of reviving a signed-out session.
- Ktor 3.6 with a single registered `Auth` provider never requires a `WWW-Authenticate` challenge to trigger a refresh (pinned both ways by a test).
- **Storage:** Android's `KeystoreSecureStringStorage` keeps one AES-256-GCM key in the Android Keystore (alias `repz.session.v1`); only the IV and ciphertext ever leave the Keystore boundary, into a private `SharedPreferences` file (`repz_secure_session`) — no EncryptedSharedPreferences — excluded from both backup (`backup_rules.xml`) and device transfer (`data_extraction_rules.xml`). iOS's `KeychainSecureStringStorage` keeps one `kSecClassGenericPassword` item, `kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly`. The Keychain survives an app deletion, so the first Keychain access of a process checks an `NSUserDefaults` install marker (`repz.session.installMarker`) and wipes the item on a fresh install: a reinstall always starts signed out on both platforms (Q1).
- Both platforms persist the session as **one atomic JSON entry** (`PersistentTokenStore`, `version = 1`); a corrupt or old-version entry is deleted and treated as no session.
- **Accepted edge case:** a process killed between the server committing a token rotation and the local `save()` landing, restarted more than the reuse grace window (30 s, above) later, ends up signed out — the rotated refresh token was never persisted, so the next refresh presents the already-superseded one.
- **Keychain storage test gap (Q6):** the write/read device test for `KeychainSecureStringStorage` is not automated. A bare Kotlin/Native `iosSimulatorArm64Test` process (`simctl spawn`, no app bundle, no entitlements) has no Keychain access at all — every `SecItem*` call fails with `errSecNotAvailable` (OSStatus -25291) regardless of how the code is written, which would make a "nothing stored" assertion pass vacuously. The manual check moves to REP-30. `KeystoreSecureStringStorageTest` (Android, `androidDeviceTest`, a real emulator process) is unaffected and does run.
- **The app shell's gate**, which reads `SessionRepository`'s state to decide whether the signed-out flow or the signed-in shell appears, is documented separately in ADR-0025.
