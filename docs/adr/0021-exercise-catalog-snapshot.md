# ADR-0021: Exercise catalog snapshot, localisation and attribution

- **Status:** Accepted (2026-11-02, REP-16)

## Context
ADR-0012 decided on a curated catalog of about 150 exercises from wger.de, stored as a committed snapshot. The live wger data turned out to be uneven:
- Polish exists for 4 of 904 exercises.
- Names and muscles are sometimes wrong.
- wger has no forearms, lower back or full-body muscle, and no "machine" equipment.
- Licences are mixed: CC BY-SA 4.0, CC BY-SA 3.0 and CC0.

The app has to work offline and switch language without re-syncing. Adding a language later must not need code changes.

## Decision
- **Curation file plus generated snapshot.** `api/src/db/seed/curation.json` is edited by hand. Each entry holds our own uuid, the wger id, and optional overrides: names per locale, instructions, muscles, equipment and a review note. `pnpm import:wger` (`api/src/scripts/`) is run by hand, never in CI. It turns wger data plus the curation into `exercises.json` and `ATTRIBUTION.md`. The output is deterministic: sorted, with no timestamps, so a re-import diff is upstream drift and is reviewed like code. The snapshot, not wger, is the source of truth.
- **Our own ids.** Catalog ids are uuids minted in the curation file and never changed. They are not derived from wger, because illustrations (`/static/exercises/{id}.webp`) and users' workouts reference them forever.
- **Mapping onto Repz vocabularies** (user decision, 2026-11-02):
  - **Muscles:** serratus anterior maps to chest, trapezius to back, brachialis to biceps. The other wger muscles map 1:1, with obliques to abs and soleus to calves.
  - **Equipment:** the SZ-bar counts as barbell. A pull-up bar or no equipment counts as bodyweight. A bench or mat is ignored next to real equipment.
  - **Precedence:** when an entry lists several pieces of equipment, the first match in barbell > dumbbell > kettlebell > cable > band > bodyweight wins.
  - **Manual values:** forearms, full_body and machine are only ever set by hand. Every override carries a reason.
  - **Database checks:** `primary_muscle` and `secondary_muscles` are checked against `MUSCLES`.
- **Localisation is generic.**
  - Names and instructions live in `exercise_translations(exercise_id, locale, name, instructions)`. `exercises.name` keeps the English name.
  - The pull `ExercisePayload` carries `translations: [{ locale, name, instructions }]`, sorted by locale; catalog rows always include `en`, and custom exercises send `[]`. It also carries `sourceUrl`, the wger page used for attribution.
  - The client picks the device locale, then `en`, then `name`.
  - **No code anywhere names a specific second language.** A locale that some entries have and others don't fails the import (the completeness rule).
  - **The catalog currently ships English only** (user decision, 2026-11-02). Adding a language is data-only: add `names.<locale>` (and optionally `instructions.<locale>`) to every curation entry, re-import, then re-seed.
- **Instructions are plain text**: paragraphs plus `1.` / `•` lines, converted from wger HTML. Poor or empty English instructions may be rewritten and flagged as modified.
- **Licence and attribution.**
  - Each row keeps the more restrictive of the exercise's and the translation's licence, plus the wger authors.
  - The compilation is CC BY-SA 4.0. 3.0 material may be adapted under 4.0, and CC0 is free.
  - `ATTRIBUTION.md` lists every row and states that Repz modified the data.
  - The app's exercise About tab and its About & licenses screen show each row's licence, author and wger link.
- **Seeder** (`pnpm db:seed`, with `--check` for a dry run):
  - It takes `lockCatalogSyncWrites` (ADR-0020) as its first statement.
  - It inserts new rows, and bumps the version only when content changes.
  - It tombstones catalog rows that are no longer in the snapshot.
  - It refuses to write anything if a snapshot id is already tombstoned (delete wins, no revive; re-adding an exercise gets a new id) or belongs to a user's custom exercise.
  - CI seeds twice and then runs `--check`.

## Consequences
- The catalog has 144 exercises, every primary group and every equipment value is covered, and the content is English only.
- Translating the catalog is a content task (curation entries), not a code task. The app's UI strings are still localised separately via Compose Resources.
- Workouts that reference a removed catalog exercise keep a dangling reference. ADR-0019 allows this.
- A future wger re-import can change rows. The diff is reviewed before it is committed.
