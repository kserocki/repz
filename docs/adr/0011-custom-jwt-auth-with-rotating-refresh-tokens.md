# ADR-0011: Custom JWT auth with rotating refresh tokens

- **Status:** Accepted (2026-10-28)

## Context
Auth is a strong interview topic and needs no third party locally.

## Decision
Access token 15 min (jose, HS256). Refresh token 30 days, stored hashed, rotated on every use. Reuse of an old token revokes the whole family. Passwords hashed with argon2 (`@node-rs/argon2`).

## Consequences
Tokens are stored in the platform Keychain/Keystore. Logout wipes the local DB (with an unsynced-changes warning).
