# Repz API

The Repz backend: Hono + `@hono/zod-openapi`, Drizzle ORM, Postgres, pino. See `docs/ARCHITECTURE.md` §10 for the full design and ADR-0016 for the runtime/tooling decisions behind this scaffold.

## Prerequisites

- Node.js 26 (Active LTS). `node --version` must report `v26.x`.
- pnpm (the pinned version in `package.json#packageManager` is installed automatically by pnpm's version manager).
- Docker (OrbStack or Docker Desktop) running, for Postgres.

## Local setup

```bash
# from the repository root
docker compose up -d          # starts Postgres only; the API runs on the host

cd api
pnpm install --frozen-lockfile
cp .env.example .env          # edit if you changed the compose credentials or port
pnpm db:migrate                # applies every committed migration
pnpm dev                      # http://localhost:3000, pretty-printed logs
```

`GET /health` should now return `200 {"status":"ok"}`.

## Scripts

| Script           | Command                                                              | Notes                   |
| ---------------- | -------------------------------------------------------------------- | ----------------------- |
| `pnpm dev`       | `node --watch --env-file-if-exists=.env src/index.ts \| pino-pretty` | restarts on file change |
| `pnpm start`     | `node --env-file-if-exists=.env dist/index.js`                       | runs the build output   |
| `pnpm build`     | `tsc -p tsconfig.build.json`                                         | emits `dist/`           |
| `pnpm typecheck` | `tsc --noEmit`                                                       | TypeScript 7, no emit   |
| `pnpm test`      | `vitest run`                                                         | Vitest 5                |
| `pnpm lint`      | `biome check --error-on-warnings .`                                  | lint + format check     |
| `pnpm lint:fix`  | `biome check --write .`                                              | auto-fixes what it can  |
| `pnpm db:generate` | `drizzle-kit generate`                                             | writes a migration from a schema change; no-op if nothing changed |
| `pnpm db:check`  | `drizzle-kit check`                                                  | validates migration metadata; no database needed |
| `pnpm db:migrate` | `node --env-file-if-exists=.env src/db/migrate.ts`                  | applies every committed migration; a re-run is a no-op |

## Environment variables

See `.env.example`. All variables are validated at boot by `src/env.ts` (Zod); the process exits with a message naming every invalid or missing variable (never its value) if validation fails.

| Variable       | Default       | Notes                                   |
| -------------- | ------------- | --------------------------------------- |
| `NODE_ENV`     | `development` | `development` \| `test` \| `production` |
| `PORT`         | `3000`        | 1–65535                                 |
| `DATABASE_URL` | — (required)  | `postgres://user:password@host:port/db` |
| `LOG_LEVEL`    | `info`        | pino level                              |

## Database

`docker-compose.yml` (repository root) starts Postgres 18.6 only; there is no API container yet (tracked as REP-59). The connection is checked once at boot (`select 1`); if Postgres is unreachable, the process logs a fatal error (host only, never the full connection string) and calls `process.exit(1)`. Under `pnpm dev`, `node --watch` then waits for a file change before it tries again, instead of the terminal returning; use `pnpm build && pnpm start` to see the real exit code.

The schema lives in `src/db/schema/` (Drizzle): one file per table group, plus the shared `sync_version_seq` sequence and vocabularies. Migrations are generated into `src/db/migrations/`, committed, and never hand-edited:

- `pnpm db:generate` reads the schema and writes a new migration (SQL + a `meta/` snapshot) only when something changed.
- `pnpm db:migrate` applies every committed migration, as a separate step from boot (so two instances starting at once never race, and `/health` stays a pure liveness check). Re-running it is a no-op — Drizzle tracks applied migrations in `drizzle.__drizzle_migrations`.
- **Migrations are forward-only.** Once one is on `main` it is immutable; a fix is a new migration, never an edit to an old one.
- `pnpm db:check` validates migration metadata without a database. CI also re-runs `pnpm db:generate` after `pnpm build` and fails the "workspace is clean" check if that produces anything new — a drift check that proves every schema change has a committed migration.
- Local reset: `docker compose down -v && docker compose up -d && pnpm db:migrate`.

`pnpm-workspace.yaml` explicitly denies esbuild's postinstall script (a transitive dependency of drizzle-kit): pnpm's build-script allowlist would otherwise block `pnpm install --frozen-lockfile`, and esbuild works correctly without running it (it only re-checks the platform binary pnpm already installed).

## Errors

Every error response is an RFC 9457 Problem Detail (`application/problem+json`), with a `requestId` extension that matches the `X-Request-Id` response header and the structured log line for that request. See `docs/ARCHITECTURE.md` §10 and ADR-0016 for the `type` URI scheme.
