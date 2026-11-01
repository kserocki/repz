# ADR-0020: Sync protocol: write serialisation, push outcomes, pull snapshot

- **Status:** Accepted (2026-11-01, REP-15)

## Context
ADR-0002 decided on an outbox with per-aggregate last write wins (LWW) in server arrival order. ADR-0003 decided on a full local replica with paged pulls. ADR-0019 left one problem open: `sync_version_seq` hands out versions before commit, so a pull could move its cursor past a version that commits later and miss that row forever. The API also needs exact rules for retries, deletes, foreign ids and dangling references.

## Decision
- **Write serialisation (closes the ADR-0019 gap).** Every push starts with two transaction-scoped advisory locks, each as its own statement and in this order:
  1. a shared catalog gate, `pg_advisory_xact_lock_shared(3, 0)`;
  2. a per-user lock, `pg_advisory_xact_lock(2, hashtext(user_id))`.

  The catalog writer (the REP-16 seeder) takes the gate exclusively through `lockCatalogSyncWrites`. A user's writes are therefore serialised, and catalog writes never interleave with an in-flight push. As a result, every committed row in a user's pull stream has a lower version than any version that can still commit into it. Different users never block each other.
- **Pull reads a single snapshot.** Each pull runs in one `REPEATABLE READ, READ ONLY` transaction and takes no lock. Under READ COMMITTED, a push touching several tables could be half-visible to a pull.
- **Rejected alternatives:**
  - a global lock, which serialises every user;
  - a `pg_snapshot_xmin` watermark, which needs a migration and a composite cursor, and lets one long transaction stall every pull;
  - a reservations table.
- **Push outcomes.** The response is `{ accepted: [{ type, id, version, deleted }], rejected: [{ type, id, problem }] }`. Every change appears exactly once, in request order.
  - **Whole-request errors.** A malformed envelope is a 400 for the whole request: more than 100 changes, a duplicate `(type, id)`, or a `deleted`/`payload` mismatch. A body over 1 MiB gets 413.
  - **Per-change rejections.** These reject only their own change:
    - an invalid payload (`validation-error`);
    - a catalog exercise (`read-only`);
    - an id owned by another user (`id-conflict`);
    - a reference to another user's template or exercise (`invalid-reference`).
  - **Savepoints.** Each change runs in its own savepoint inside one locked transaction, so a failing change never poisons the rest of the outbox.
- **Conflict rules** are a pure decision function:
  - Insert when the id is new.
  - Delete of an id the server never had: nothing is stored, and the response is `version: null, deleted: true`.
  - **Delete wins:** an upsert of a tombstoned id writes nothing and answers `deleted: true`, and **the client must then delete the record locally**. The flag exists because a pull skips entities that have a pending outbox entry. Without it, a tombstone skipped that way would come back as a zombie record.
  - A retried push whose payload is identical to what the server stores does not bump the version.
  - Any other upsert replaces the root and all its children (LWW, no base-version check).
- **Aggregates and references** (user decisions, 2026-11-01):
  - Child order is the array order. There is no `position` on the wire; the server stores `position = index`.
  - Tombstoning a workout or template permanently deletes its children.
  - References to unknown templates or exercises are allowed: they were created and deleted offline, or arrive in a later batch. References to another user's are rejected.
  - Deleting a custom exercise leaves workouts and templates that use it untouched.
  - Limits:
    - names 1–100 characters, notes up to 2000;
    - up to 50 exercises per workout, 50 sets per exercise and 50 exercises per template;
    - `targetSets` 1–20, reps 0–999, weight 0–1,000,000 g, rest 0–3600 s;
    - custom-exercise muscles from the 12 `MUSCLES` groups.
- **Pull.** Workouts, templates, custom exercises and catalog exercises are merged in version order. Versions are globally unique, so the cursor is the last version. `limit` is 1–500, default 500, and one extra row is fetched to set `hasMore`. Tombstones are sent with `payload: null`. `totalEstimate` is `{ total, workout, template, exercise }`, counted from the `(scope, version)` indexes on every page (user decision), so the restore screen can show "N / M workouts".

## Consequences
- The advisory-lock namespaces are listed in ARCHITECTURE §10.4: `1` refresh-token family, `2` per-user sync writes, `3` catalog gate.
- Every version-assigning write must go through the sync repositories, inside `SyncTransactions.write`, after the locks are taken. A protocol-order test enforces this. Races are proven on real Postgres, because in-memory fakes can't reproduce them.
- Client obligations (REP-26):
  - push at most 100 changes per request;
  - apply `accepted[].deleted`;
  - treat `version: null` as "the server holds nothing";
  - treat a rejection as permanent: keep the local data, mark it "couldn't sync" and drop the outbox entry;
  - keep a delete in the outbox until its undo snackbar is dismissed.
