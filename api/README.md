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
pnpm dev                      # http://localhost:3000, pretty-printed logs
```

`GET /health` should now return `200 {"status":"ok"}`.

## Scripts

| Script | Command | Notes |
|---|---|---|
| `pnpm dev` | `node --watch --env-file-if-exists=.env src/index.ts \| pino-pretty` | restarts on file change |
| `pnpm start` | `node --env-file-if-exists=.env dist/index.js` | runs the build output |
| `pnpm build` | `tsc -p tsconfig.build.json` | emits `dist/` |
| `pnpm typecheck` | `tsc --noEmit` | TypeScript 7, no emit |
| `pnpm test` | `vitest run` | Vitest 5 |
| `pnpm lint` | `biome check .` | lint + format check |
| `pnpm lint:fix` | `biome check --write .` | auto-fixes what it can |

## Environment variables

See `.env.example`. All variables are validated at boot by `src/env.ts` (Zod); the process exits with a message naming every invalid or missing variable (never its value) if validation fails.

| Variable | Default | Notes |
|---|---|---|
| `NODE_ENV` | `development` | `development` \| `test` \| `production` |
| `PORT` | `3000` | 1–65535 |
| `DATABASE_URL` | — (required) | `postgres://user:password@host:port/db` |
| `LOG_LEVEL` | `info` | pino level |

## Database

`docker-compose.yml` (repository root) starts Postgres 18.6 only; there is no API container yet (tracked as a follow-up issue). The connection is checked once at boot (`select 1`); if Postgres is unreachable, the process logs a fatal error (host only, never the full connection string) and exits.

There is no schema yet — `db/schema/`, migrations and seed data land in REP-13.

## Errors

Every error response is an RFC 9457 Problem Detail (`application/problem+json`), with a `requestId` extension that matches the `X-Request-Id` response header and the structured log line for that request. See `docs/ARCHITECTURE.md` §10 and ADR-0016 for the `type` URI scheme.
