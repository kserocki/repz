# ADR-0001: Monorepo with local-only API

- **Status:** Accepted (2026-10-28)

## Context
We need one repository to show in interviews, with a mobile app and a custom backend, without cloud costs.

## Decision
`mobile/` (Gradle, KMP) and `api/` (pnpm, Hono) live in one repo. The API and Postgres run locally through `docker-compose`.

## Consequences
One clone shows everything. Physical devices need the host's LAN IP (per-build config). No public demo URL.
