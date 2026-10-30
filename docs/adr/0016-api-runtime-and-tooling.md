# ADR-0016: API runtime and tooling

- **Status:** Accepted (2026-10-29, REP-12)

## Context
The API needs a runtime, a type checker, a linter, a logger and a stable error contract before any real endpoint exists. Node 26 can run TypeScript directly, and TypeScript 7 ships a native compiler with no stable JS API yet.

## Decision
- **Runtime:** Node 26 LTS runs the `.ts` sources directly through built-in type stripping. No `tsx`/`ts-node`. The sources use only erasable syntax (no `enum`, `namespace` or parameter properties), relative imports keep the `.ts` extension, and type-only imports use `import type`.
- **TypeScript 7** (`tsc`) is used only for `pnpm typecheck` (`tsc --noEmit`) and `pnpm build` (`dist/` for `pnpm start` or a later container).
- **Biome** does lint and formatting in one tool (`pnpm lint` = `biome check --error-on-warnings .`, so warnings fail the gate too).
- **Logger:** **pino** writes JSON to stdout, with a child logger per request and redaction. `pino-pretty` is used only in `pnpm dev`. Never log tokens, passwords, bodies, query strings or full headers.
- **Errors:** RFC 9457 Problem Details. `type` is `about:blank` for plain HTTP errors (404, 500, malformed JSON) and `urn:repz:problem:<kebab-name>` for application errors (first: `urn:repz:problem:validation-error`). Every problem carries a `requestId` extension, and validation errors add `errors: [{ path, message }]`. A 500 never exposes the message or stack.
- **`/health`:** a liveness check only (`200 {"status":"ok"}`). The database is pinged once at boot, and the process exits with code 1 if Postgres is unreachable.
- **Shutdown:** SIGINT/SIGTERM stops accepting connections and closes the database. A second signal, a failed close or 10 s without finishing force exit code 1 (user-approved value, 2026-10-29).
- **Local stack:** `docker-compose.yml` runs Postgres 18.6 only; the API runs on the host with `pnpm dev`. Running the API in a container is REP-59.

## Consequences
- Fast dev loop with no build step.
- Tooling that needs the TypeScript JS API can't be added until TS 7 provides one.
- The URN problem types don't depend on a public domain, which matches ADR-0001 (local-only API).
- Shared JSON fixtures (REP-17) must use these `type` values.
