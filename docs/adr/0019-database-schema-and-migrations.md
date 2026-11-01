# ADR-0019: Database schema and migration conventions

- **Status:** Accepted (2026-11-01, REP-13)

## Context
REP-14 (auth), REP-15 (sync push/pull), REP-16 (exercise catalog) and REP-17 (contract) all build on one Postgres schema. The sync model (ADR-0002, ADR-0003) needs a monotonic change version and tombstones. Offline clients create their own rows, and the API must stay cheap to evolve without ever breaking a database that is already migrated.

## Decision
- **Schema in Drizzle** (`api/src/db/schema/`), one file per table group. SQL names are snake_case and constraint names are explicit (`<table>_<column(s)>_<kind>`), so errors and REP-15's error mapping can rely on them.
- **IDs:** `uuid`. Rows the client creates (workouts, templates, custom exercises and their children) have no default and store the client's id as-is, in any UUID version. Rows the server creates (`users`, `refresh_tokens`) default to Postgres 18 `uuidv7()`. Catalog exercise ids come from the REP-16 snapshot.
- **Timestamps:** `timestamptz(3)` everywhere, which round-trips a JS `Date` exactly. The server sets `updated_at` with `now()`, never from the client clock.
- **Sync columns on every root** (`exercises`, `workout_templates`, `workouts`): `version bigint NOT NULL DEFAULT nextval('sync_version_seq')`, `updated_at`, `deleted_at` (tombstone). `sync_version_seq` is one global sequence starting at 1, so `0` stays free as the "never pulled" cursor. Every update of a root sets `version = nextSyncVersion` explicitly; there is no trigger. Each root has a `(user_id | owner_id, version)` index for pull.
- **Fixed value lists** are `text` + `CHECK (… IN (…))` built from `as const` tuples in `schema/vocabularies.ts`, never `pgEnum`, because a value can then be added or removed with an ordinary migration. The API's Zod schemas should reuse the same tuples when they arrive.
- **Child keys:** `workout_exercises` and `workout_sets` keep their own `id uuid` plus `UNIQUE (parent_id, position)`; `template_exercises` has no id, so its primary key is `(template_id, position)`. The column is `position` because `order` is a reserved word.
- **Derived fields are not stored:** an exercise is custom when `owner_id IS NOT NULL`, and its image URL is `/static/exercises/{id}.webp`.
- **Foreign keys:** children cascade from their root, and roots cascade from `users`. There are **no foreign keys between aggregates** (`workouts.template_id`, `*.exercise_id`): a workout may reference a template or custom exercise that was created and deleted offline and never reached the server. REP-15's handlers validate those references.
- **Value rules in the database** (user decision, 2026-11-01): `weight_grams bigint >= 0` (grams, no floats); `reps >= 0`; `rpe numeric(3,1)` nullable and `BETWEEN 1 AND 10` (the 6–10 in 0.5 steps rule lives in Zod); a stored workout is finished (`finished_at NOT NULL`, `>= started_at`); template targets are consistent (`target_sets >= 1`, reps min/max both null or `1 <= min <= max`, `rest_seconds >= 0`); `position >= 0`. Text lengths and "not blank" are validated by Zod at the API edge only. A new account's `unit_system` defaults to `kg`. Email uniqueness is case-insensitive (`lower(email)`).
- **Migrations:** `pnpm db:generate` (drizzle-kit) writes SQL + snapshot to `src/db/migrations/`, committed and never hand-edited. Migrations are **forward-only**: a migration on `main` is immutable, and fixes are new migrations. `pnpm db:migrate` applies them as a separate step, never at boot, so boot stays a liveness check (ADR-0016) and instances never race. Local reset is `docker compose down -v`.
- **CI** (`api.yml`): `pnpm db:check`, then `pnpm db:generate`, and the clean-tree step fails if the schema changed without a committed migration. A `postgres:18.6-alpine` service container applies the migrations twice: the SQL runs on the real database version, and a re-run does nothing (user decision). Database-backed Vitest tests stay in REP-53.
- **esbuild install scripts are denied** (`api/pnpm-workspace.yaml`: `allowBuilds: { esbuild: false }`, user decision 2026-11-01). drizzle-kit pulls in esbuild, whose postinstall only re-checks the platform binary that pnpm already installs. pnpm 12's `strictDepBuilds` stays on, so any other new build script still fails `pnpm install --frozen-lockfile`.

## Consequences
- A sequence hands out versions before commit, so a pull could move its cursor past a write that hasn't committed yet. REP-15 must close this gap (for example with an advisory lock, or by capping pull at a safe watermark).
- Localised exercise names and instructions, and the muscle vocabulary check, arrive with REP-16 in its own migration.
- Removing a vocabulary value needs a data migration first, but it never needs an enum rewrite.
- After the drizzle-kit 1.0 upgrade, re-check whether the esbuild entry is still needed.
