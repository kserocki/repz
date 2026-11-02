# Repz API

The Repz backend: Hono + `@hono/zod-openapi`, Drizzle ORM, Postgres, pino. See `docs/ARCHITECTURE.md` §10 for the full design and ADR-0016 for the runtime/tooling decisions behind this scaffold.

## Prerequisites

- Node.js 26 (Active LTS). `node --version` must report `v26.x`.
- pnpm (the pinned version in `package.json#packageManager` is installed automatically by pnpm's version manager).
- Docker (OrbStack or Docker Desktop) running, for Postgres.

## Local setup

```bash
# from the repository root
docker compose up -d          # starts Postgres only, bound to 127.0.0.1; the API runs on the host

cd api
pnpm install --frozen-lockfile
cp .env.example .env          # fill in JWT_SECRET (see "Environment variables" below)
pnpm db:migrate                # applies every committed migration
pnpm dev                      # http://localhost:3000, pretty-printed logs
```

`GET /health` should now return `200 {"status":"ok"}`.

Without a `.env` file, pass every variable inline instead (useful for a one-off run or a quick manual test with non-default settings):

```bash
JWT_SECRET=$(openssl rand -base64 48) DATABASE_URL=postgres://repz:repz@localhost:5432/repz node src/index.ts
```

## Scripts

| Script             | Command                                                              | Notes                                                             |
| ------------------ | -------------------------------------------------------------------- | ----------------------------------------------------------------- |
| `pnpm dev`         | `node --watch --env-file-if-exists=.env src/index.ts \| pino-pretty` | restarts on file change                                           |
| `pnpm start`       | `node --env-file-if-exists=.env dist/index.js`                       | runs the build output                                             |
| `pnpm build`       | `tsc -p tsconfig.build.json`                                         | emits `dist/`                                                     |
| `pnpm typecheck`   | `tsc --noEmit`                                                       | TypeScript 7, no emit                                             |
| `pnpm test`        | `vitest run`                                                         | Vitest 5                                                          |
| `pnpm lint`        | `biome check --error-on-warnings .`                                  | lint + format check                                               |
| `pnpm lint:fix`    | `biome check --write .`                                              | auto-fixes what it can                                            |
| `pnpm db:generate` | `drizzle-kit generate`                                               | writes a migration from a schema change; no-op if nothing changed |
| `pnpm db:check`    | `drizzle-kit check`                                                  | validates migration metadata; no database needed                  |
| `pnpm db:migrate`  | `node --env-file-if-exists=.env src/db/migrate.ts`                   | applies every committed migration; a re-run is a no-op            |
| `pnpm db:seed`     | `node --env-file-if-exists=.env src/db/seed/seed.ts`                 | seeds the exercise catalog; idempotent; `-- --check` = dry run    |
| `pnpm import:wger` | `node --env-file-if-exists=.env src/scripts/import-wger.ts`          | regenerates the catalog snapshot from wger (network, run by hand) |

## Environment variables

See `.env.example`. All variables are validated at boot by `src/env.ts` (Zod); the process exits with a message naming every invalid or missing variable (never its value) if validation fails. `pnpm db:migrate` validates only `NODE_ENV`/`DATABASE_URL`/`LOG_LEVEL` (`migrationEnvSchema`), so it works without `JWT_SECRET` (CI's migration step has none).

| Variable                            | Default          | Notes                                                                                                                                                                                         |
| ----------------------------------- | ---------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `NODE_ENV`                          | `development`    | `development` \| `test` \| `production`                                                                                                                                                       |
| `PORT`                              | `3000`           | 1–65535                                                                                                                                                                                       |
| `DATABASE_URL`                      | — (required)     | `postgres://user:password@host:port/db`                                                                                                                                                       |
| `LOG_LEVEL`                         | `info`           | pino level                                                                                                                                                                                    |
| `JWT_SECRET`                        | — (required)     | >= 32 characters (256+ bits), used directly as the HS256 key. Generate one with `openssl rand -base64 48`. Required in every `NODE_ENV`, including `development`.                             |
| `ACCESS_TOKEN_TTL_SECONDS`          | `900` (15 min)   | Access token (JWT) lifetime.                                                                                                                                                                  |
| `REFRESH_TOKEN_TTL_SECONDS`         | `2592000` (30 d) | Refresh token lifetime, sliding: every new token (login, register, rotation, grace replay) is valid this long from its own issue time.                                                        |
| `REFRESH_TOKEN_REUSE_GRACE_SECONDS` | `30`             | Window in which a rotated refresh token whose successor is still live is treated as a retried/concurrent refresh instead of reuse. `0` disables the grace window (strict ADR-0011 behaviour). |

## Auth

See `docs/ARCHITECTURE.md` §10.3 and ADR-0011 (with its REP-14 amendment) for the full design. Routes:

| Method | Path             | Auth   | Notes                                                                                                       |
| ------ | ---------------- | ------ | ----------------------------------------------------------------------------------------------------------- |
| POST   | `/auth/register` | public | 201, returns `AuthSession` (`user`, `accessToken`, `refreshToken`, `expiresIn`)                             |
| POST   | `/auth/login`    | public | 200, same shape as register; wrong password and unknown email give the identical `invalid-credentials` body |
| POST   | `/auth/refresh`  | public | 200, rotates the refresh token (see below) and returns `AuthTokens`                                         |
| POST   | `/auth/logout`   | public | 204 always, even for an unknown/expired/revoked token (idempotent)                                          |
| GET    | `/me`            | bearer | 200, the current user                                                                                       |
| PATCH  | `/me`            | bearer | 200, `displayName` and/or `unitSystem`, at least one required                                               |

- **Access tokens** are HS256 JWTs (`jose`): `sub`, `iat`, `exp`, `iss: repz-api`, `aud: repz-app`, verified with the algorithm pinned. Protected routes read `Authorization: Bearer <token>`; a missing/invalid/expired token answers 401 `invalid-access-token` with `WWW-Authenticate`.
- **Refresh tokens** are 256-bit random values, stored only as a SHA-256 hash. Every register/login starts a new "family" (one per signed-in device session); refreshing **rotates** the token and records the successor. Every write to a family (rotate, replay, revoke, logout) first takes a per-family advisory lock, so a revoke never misses a concurrent successor (ADR-0011), and the lifetime is **sliding** (each new token is valid `REFRESH_TOKEN_TTL_SECONDS` from its own issue time).
- **Reuse detection, relaxed by a grace window:** presenting an already-rotated token revokes every live token in its family (a stolen-token signal). A token rotated less than `REFRESH_TOKEN_REUSE_GRACE_SECONDS` ago whose direct successor is still live is treated as a retried or concurrent refresh instead: it gets a new sibling pair in the same family, and the original successor stays live. This absorbs a client retrying after a lost response, or two parallel requests from the app (e.g. the UI and a background sync worker) refreshing at once, without signing the user out. `REFRESH_TOKEN_REUSE_GRACE_SECONDS=0` disables it (strict detection).
- Passwords are hashed with argon2id (`@node-rs/argon2`, OWASP's first recommended configuration: 19 MiB, 2 passes, 1 thread).

## Sync

`POST /sync/push` and `GET /sync/pull` (bearer token, `Cache-Control: no-store`) implement the offline-first protocol from ADR-0002, ADR-0003 and ADR-0020:

- **Push** takes 1–100 changes (`{ type, id, deleted, payload }`, at most 1 MiB) and answers `{ accepted, rejected }`. A bad change rejects only itself (`validation-error`, `read-only`, `id-conflict`, `invalid-reference`), never the batch. Delete wins: an upsert of a tombstoned id comes back as `deleted: true`. A retry with an identical payload keeps the same version.
- **Pull** returns changes with `version > cursor`, ordered by version (`limit` 1–500, default 500). It includes the global exercise catalog, sends tombstones with `payload: null` and reports `totalEstimate` per type.
- **Ordering guarantee:** every push takes a per-user advisory lock (plus a shared catalog gate) before it writes, and every pull reads one `REPEATABLE READ` snapshot. A cursor therefore never skips a version that commits later. Code that assigns versions must go through `SyncTransactions.write` after `lockUserSyncWrites`.

## Exercise catalog

The catalog (ADR-0012, ADR-0021) is 144 exercises adapted from [wger.de](https://wger.de), English only for now.

- **Add or change an exercise:** edit `src/db/seed/curation.json`. Each entry has our own uuid (mint a new one, never reuse an id) and the wger id. Override the name, instructions, muscles or equipment, and give a `reviewNote` explaining why. Then run `pnpm import:wger`, review the diff of `exercises.json` and `ATTRIBUTION.md`, and run `pnpm db:seed`.
- **Add a language:** add `names.<locale>` (and optionally `instructions.<locale>`) to **every** curation entry, then re-import and re-seed. No code changes, no API change. The import fails if any entry lacks the new locale.
- **Remove an exercise:** delete its entry. The seeder tombstones the row, and the id is never reused.
- **Seeder rules:** it is idempotent, so versions only change when content changes. It takes the catalog sync lock first. It refuses to revive a tombstoned id or to touch an id that belongs to a user's custom exercise — in either case nothing is written.

## Database

`docker-compose.yml` (repository root) starts Postgres 18.6 only, bound to `127.0.0.1:5432` (not reachable from other machines on the network); there is no API container yet (tracked as REP-59). The connection is checked once at boot (`select 1`); if Postgres is unreachable, the process logs a fatal error (host only, never the full connection string) and calls `process.exit(1)`. Under `pnpm dev`, `node --watch` then waits for a file change before it tries again, instead of the terminal returning; use `pnpm build && pnpm start` to see the real exit code.

The schema lives in `src/db/schema/` (Drizzle): one file per table group, plus the shared `sync_version_seq` sequence and vocabularies. Migrations are generated into `src/db/migrations/`, committed, and never hand-edited:

- `pnpm db:generate` reads the schema and writes a new migration (SQL + a `meta/` snapshot) only when something changed.
- `pnpm db:migrate` applies every committed migration, as a separate step from boot (so two instances starting at once never race, and `/health` stays a pure liveness check). Re-running it is a no-op — Drizzle tracks applied migrations in `drizzle.__drizzle_migrations`.
- **Migrations are forward-only.** Once one is on `main` it is immutable; a fix is a new migration, never an edit to an old one.
- `pnpm db:check` validates migration metadata without a database. CI also re-runs `pnpm db:generate` after `pnpm build` and fails the "workspace is clean" check if that produces anything new — a drift check that proves every schema change has a committed migration.
- Local reset: `docker compose down -v && docker compose up -d && pnpm db:migrate`.

`pnpm-workspace.yaml` explicitly denies esbuild's postinstall script (a transitive dependency of drizzle-kit): pnpm's build-script allowlist would otherwise block `pnpm install --frozen-lockfile`, and esbuild works correctly without running it (it only re-checks the platform binary pnpm already installed).

## Errors

Every error response is an RFC 9457 Problem Detail (`application/problem+json`), with a `requestId` extension that matches the `X-Request-Id` response header and the structured log line for that request. See `docs/ARCHITECTURE.md` §10 and ADR-0016 for the `type` URI scheme.
