# ADR-0023: Local database schema, outbox and sync meta

- **Status:** Accepted (2026-11-03, REP-24)

## Context
The UI reads only from Room, and every write is entity + outbox in one transaction (ARCHITECTURE §5.1). `core:database` (Room 3, ADR-0007) needs a local schema that mirrors the server (ADR-0019, ADR-0020), an outbox that coalesces edits and supports undo, and a place for sync progress and permanent rejections. The sync engine (REP-26) and the repositories (REP-27) build on it.

## Decision
- **Tables (schema v1).** 11 tables:
  - synced roots: `workouts`, `workout_templates`, `exercises`;
  - their children: `workout_exercises`, `workout_sets`, `template_exercises`, `exercise_translations`;
  - local-only: `personal_records`, `outbox`, `sync_rejections`, `sync_meta`.
- **Column types.**
  - Ids are `kotlin.uuid.Uuid`, stored as `TEXT` in canonical lowercase hyphenated form, so the schema JSON and `sqlite3` dumps stay readable.
  - Timestamps are `kotlin.time.Instant`, stored as `INTEGER` epoch milliseconds, matching the wire. Sub-millisecond precision is dropped on write.
  - Closed vocabularies (set type, muscles, equipment) are `TEXT` holding the server's lowercase snake_case values (`"warmup"`, `"full_body"`). `core:sync` writes pulled values unchanged and `core:data` maps them to the domain enums. `personal_records.type` follows the same rule (`"est_1rm"`, `"heaviest"`, `"best_set_volume"`, `"most_reps"`).
  - `exercises.secondary_muscles` is a comma-joined list (empty list = `""`); vocabulary values never contain commas.
  - The local-only enums `SyncEntityType` (`WORKOUT`, `TEMPLATE`, `EXERCISE`) and `OutboxOperation` (`UPSERT`, `DELETE`) are stored by name.
- **Root sync columns** on `workouts`, `workout_templates` and `exercises`:
  - `server_version` (nullable: the server never confirmed this row);
  - `is_deleted` (tombstone). UI-facing queries filter it out; by-id reads for history and sync are named `…IncludingDeleted`;
  - `updated_at`, local, for UI and sorting only, never for conflicts.
- **Relations.** Children have a foreign key to their parent with `ON DELETE CASCADE`. There are **no foreign keys between aggregates** (`template_id`, `exercise_id`), as on the server: a workout may reference a template or custom exercise that was created and deleted offline. `position` is the list index; the `(parent, position)` indexes are not unique, because an aggregate is always saved by replacing all of its children. `@Relation` can't order, so callers sort by `position`.
- **Writes.**
  - `saveLocalX` keeps the stored `server_version` and clears `is_deleted`. `saveSyncedX` takes both as given. Both replace all children in one transaction.
  - A tombstone (`setDeleted`) keeps its children, so undo can restore them. `deleteChildren` removes them once the server confirmed the delete. `purge` is a hard delete for the discarded active workout, which never syncs.
  - The active workout (`finished_at IS NULL`) is local-only and enters the outbox only once finished.
- **Outbox.**
  - At most one row per entity: the primary key is `(entity_type, entity_id)`. The row carries no payload; the engine builds the payload from the current entity at push time.
  - Coalescing (`coalesceOutboxEntry`, a pure function):

    | Pending \ new | UPSERT | DELETE |
    |---|---|---|
    | none | UPSERT, no hold | DELETE with `held_until` |
    | UPSERT | stays UPSERT | becomes DELETE, hold set |
    | DELETE | becomes UPSERT, hold cleared (undo) | stays DELETE, hold refreshed |

    On every coalesce `created_at` is kept (a frequently edited entity never starves behind newer ones), `updated_at` moves to now, `revision` goes up by 1 and `attempts` resets to 0. An UPSERT can't carry a hold.
  - **Revision-guarded removal.** The engine pushes a batch, then removes rows with `deleteIfUnchanged(type, id, revision)`. An edit that coalesced into the row during the push bumped `revision`, so the row survives and that edit is pushed next.
  - **A delete after a never-pushed insert is kept.** `server_version == null` doesn't prove the server never got the row: an accepted push whose response was lost leaves the same state. Dropping the DELETE would leave a live row on the server that the next pull resurrects. The server answers `version: null, deleted: true` for an id it never had (ADR-0020).
  - **`held_until` (undo).** A delete is enqueued with `held_until = now + undo window`; `releaseHold` clears it when the snackbar is dismissed, and undo enqueues an UPSERT that coalesces the DELETE away. After process death the hold simply expires. `findReadyToPush` returns only rows with no hold or an expired one, ordered by `created_at`, then type, then id. A held row still counts as pending, so a pull skips that entity.
- **`sync_rejections`.** Primary key `(entity_type, entity_id)`, plus `problem_type` (the Problem Details `type` URI; the UI maps it to localised copy) and `rejected_at`. On a rejection the engine writes this row and removes the outbox row; the local data is untouched. The "couldn't sync" mark clears only when a later push of that record is accepted, not when the user edits it.
- **`sync_meta`.** A single row with `id = 0`; reads return `SyncMetaEntity.INITIAL` (cursor `0`, everything else null) while the row is missing. Columns: `pull_cursor`, `last_successful_sync_at`, `initial_pull_completed_at`, `restore_total_changes`, `restore_total_workouts`.
  - The server's `totalEstimate` counts only the changes still left after the cursor. The restore totals keep the first page's totals, so the "N / M workouts" baseline M survives an app kill and N = M − remaining.
  - `initial_pull_completed_at` tells a finished restore apart from a cursor that is merely non-zero, and triggers the one-off PR recompute.
- **Stored personal records.** `personal_records` (primary key `(exercise_id, type)`) holds the derived records (ADR-0022). Reading a few rows per exercise keeps the Records tab and the live PR check cheap, instead of folding every set on each emission. It has no sync columns, no outbox use and no foreign keys; `replaceForExercises` replaces the given exercises' rows in one transaction.
- **Deleted exercises keep their row locally** with `is_deleted = 1`: the library hides them, and history can still show the name. A pulled tombstone for an id the device never had stores nothing.
- **Schema version.** `RepzDatabase` is version 1, and the schema JSON is committed (`mobile/core/database/schemas/`). Until the first build is distributed to users, v1 may be edited in place, since only dev devices have it installed. From then on every change bumps the version with a migration (`AutoMigration` where possible) and a migration test. **Never `fallbackToDestructiveMigration`**: it would silently drop unsynced outbox rows.
- **Boundaries.**
  - `core:database` knows no domain types. Entity ↔ domain mapping lives in `core:data` (REP-27), DTO ↔ entity mapping in `core:sync` (REP-26).
  - Koin wiring of the database moves to REP-27, the first consumer. `core:database` ships the platform file builders: `repzDatabaseBuilder(context)` on Android (`getDatabasePath("repz.db")`) and `repzDatabaseBuilder()` on iOS (`repz.db` in Application Support, not the user-visible `Documents`). Both finish with `buildRepzDatabase(…)`.
  - Excluding the database file from device backups is REP-77.
- **User decisions** (standing approval, 2026-11-02): deleted exercises keep their row; backup exclusion goes to a follow-up issue; the "couldn't sync" mark clears only on a later accepted push.

## Consequences
- REP-26 and REP-27 follow these write paths:
  - local write: `saveLocalX` + `enqueue(UPSERT)` in one write transaction, then request a sync;
  - delete: `setDeleted(true)` + `enqueue(DELETE, heldUntil)`; undo: `setDeleted(false)` + `enqueue(UPSERT)`;
  - accepted push: `setServerVersion` + `deleteIfUnchanged` and remove any rejection row; when `deleted` is true, tombstone locally and `deleteChildren`;
  - pull: skip ids from `findPendingEntityIds`, apply upserts with `saveSyncedX` and tombstones with `setDeleted` + `deleteChildren`, and save the cursor in the same transaction, one per page.
- Pushing a delete the server never saw costs one change, in exchange for never resurrecting a row.
- Repository- and screen-shaped queries (history summaries, paging, search) are added by later issues; a new query doesn't change the schema.
