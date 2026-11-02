---
name: sync-entity
description: Make a Repz entity offline-first and syncable end-to-end (Room entity + outbox, repository, SyncHandler on client and server, pull/push payloads, tombstones, tests). Use when a new aggregate must survive offline edits and sync across devices.
---

# New syncable entity

## Rule zero: never guess

If anything is unclear (naming, behaviour, UX copy, a token value, which module it belongs to, an API shape) or the docs disagree with the code, **stop and ask the user** before writing code. Do not invent values, screens, endpoints or animations that are not specified. Record approved assumptions in the docs (or an ADR) in the same change.

Read `docs/ARCHITECTURE.md` §5 and ADR-0002/0003 first. **The aggregate is the unit of sync.** Confirm with the user which aggregate root this is and what its children are.

## Client (mobile)
1. **Domain:** the model plus repository interface in `core:domain`. IDs come from `IdGenerator` (client-side UUID).
2. **Room 3** (`core:database`): the root entity has `id`, `server_version: Long?`, `is_deleted`, `updated_at`. Add child entities with FK + cascade. DAO methods: `Flow` for observation, `suspend` for writes. Bump the schema version and add a migration (`suspend fun migrate(connection: SQLiteConnection)`). The schema JSON is committed.
3. **Repository** (`core:data`, `OfflineFirstXRepository`): every write runs `db.withWriteTransaction { upsert entity; outbox.upsert(type, id, op) }` and then `syncScheduler.requestSync()`. Reads are `Flow` from Room mapped `toDomain()`. A delete is a tombstone (`is_deleted = true`) + outbox DELETE.
4. **SyncHandler** (`core:sync`): implement `SyncHandler<XPayload>` (`type`, `toPayload(id)`, `apply(change)`, skip apply when a pending outbox entry exists). Register it via Koin. **Do not edit `SyncEngine`** (open/closed).
5. **DTO:** the payload DTO in `core:network` plus a fixture.

## Server (api)
6. **Server side (`api/`)** — follow ADR-0019 and ADR-0020. For a new sync type, touch every one of these:
   - Drizzle tables (root + children) with `syncColumns()` and a `(user_id, version)` index; `pnpm db:generate`.
   - `SYNC_TYPES` (the wire `type`; `totalEstimate` buckets are derived from it) and the payload schema in `sync.schemas.ts` (`.openapi("Name")`, strict keys, child-id uniqueness in `superRefine`).
   - Push `payload` `oneOf` in `sync.schemas.ts` plus `router.openAPIRegistry.register(...)` in `sync.routes.ts`.
   - `handlers/<entity>.repository.ts` (replace children on every write, tombstone deletes children, map 23505 by constraint name from `.cause` to `SyncIdConflict`) and `handlers/<entity>.handler.ts`; extend the `SyncHandler.name` union.
   - A `SyncStores` field, wired in `storesFor` (`sync.repository.ts`) and in the in-memory fake (`shared/testing/in-memory-sync-store.ts`, including global child-id keys and immutable row swaps for savepoint rollback).
   - Register the handler in `app.ts`.
   - Every version write goes through `SyncTransactions.write` after `lockUserSyncWrites`; `decideChange` decides (delete wins, idempotent retries); pull returns `version > cursor` with tombstones as `payload: null`.
   - Tests: schema tests (limits, strict keys, duplicate child ids), handler mapping, service cases for every `decideChange` row that applies, the OpenAPI test (`sync.openapi.test.ts`), and a Spotter scenario on real Postgres.
7. Export the OpenAPI contract and update the shared fixtures (`new-endpoint` skill step 6).

## Tests (must have)
- Repository: write → entity + one coalesced outbox row; ten edits → still one row.
- Handler: pull skips entities with pending outbox; tombstone wins over a later upsert; idempotent re-apply.
- Server: version increases monotonically; delete wins.

Run `verify`.
