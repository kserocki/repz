# ADR-0022: Domain value types and personal-record rules

- **Status:** Accepted (2026-11-02, REP-23)

## Context
`core:domain` is the pure-Kotlin heart of the app (ARCHITECTURE §3). Its types must mirror the server rules (ADR-0019, ADR-0020, ADR-0021) and keep money-like precision for weights. Personal records are derived locally and never synced (§4), so the rules for computing them have to be written down exactly.

## Decision
- **Ids.** `@JvmInline` value classes wrap `kotlin.uuid.Uuid`, one per entity (`WorkoutId`, `ExerciseId`, …). New ids are **UUID v7**: time-ordered, and the server accepts any version. They come from the `IdGenerator` interface in `core:common`, whose default is `UuidV7IdGenerator`. Time comes from an injected `kotlin.time.Clock`, and dispatchers from `AppDispatchers(default, io)`. All three are provided by `CommonModule` in `app:shared`. Konsist rule **I1** allows `Clock.System` and `Uuid.random/generateV4/generateV7` only there and in the id generator, tests included.
- **`Weight`** holds grams in a `Long` and is never negative. kg/lb input is rounded half-up to the nearest gram, and every lb value with two decimals round-trips. Volume is also a `Weight`. The 1,000,000 g maximum is checked by `ValidationLimits`, not by the type, and the UI phrases the limit in the user's unit (user decision via standing approval; board 05's "0–999 kg" is superseded).
- **Display.** Set weights show up to 2 decimals (half-up, trailing zeros trimmed). Est. 1RM and PR deltas show 1 fixed decimal. Separators follow the locale.
- **`Rpe`** has a private constructor. `Rpe.of` returns a `DomainResult` and accepts 6.0–10.0 in 0.5 steps; `Rpe.ALL` lists the 9 values.
- **Vocabularies.** `Muscle` (12), `Equipment` (8), `SetType` (4) and `UnitSystem` mirror the server values, in the server's order.
- **Errors.** `DomainResult<T, E : DomainError>` and `DomainError` live in `core:domain`. An issue adds error types only when it needs them; REP-23 adds only the `ValidationError`s. `ValidationLimits` holds the ADR-0020 limits and their validators.
- **Invariants.** Value types (`Weight`, `Rpe`, ids) enforce their own invariants. Aggregates don't check anything in their constructors, so drafts and stored data stay representable; use cases validate on save.
- **No `order` fields.** List order is the order, as on the wire (ADR-0020).
- **Exercise origin.** `Exercise.origin` is either `Custom` or `Catalog(imageUrl?, ExerciseAttribution(license, author, sourceUrl))`. `isCustom` is derived from it, so a custom exercise with a licence can't be represented. Localised text comes from `translations` (ADR-0021): the exact language tag first, then the same primary language, then `en`, then `name`.
- **Personal records** (user decision via standing approval) are computed by the pure function `calculatePersonalRecords`:
  - **Counted sets:** completed sets in finished workouts, any set type except `WARMUP`, with reps ≥ 1.
  - **EST_1RM:** Epley, `weight × (1 + reps / 30)`, computed in integer grams and rounded half-up. With reps = 1 it is the weight itself, and there is no rep cap.
  - **HEAVIEST:** the heaviest weight in any counted set, at any rep count.
  - **BEST_SET_VOLUME:** weight × reps of one set.
  - **MOST_REPS:** the most reps in one counted set, at any weight.
  - **Winning:** a record needs a value above 0, and only a strictly greater value beats it. On a tie the earliest set wins.
  - **Stored data:** a record keeps its set's weight, reps, `workoutId` and `setId`, because the UI shows "82.5 kg × 8".
- **Deferred.** Repository interfaces, `RecalculatePersonalRecordsUseCase` and `DomainModule` move to REP-27, which designs the repositories together with their first implementation. Fakes move to REP-29.

## Consequences
- Weight arithmetic has no float drift, and the client agrees with the server's limits.
- A 0 kg set can only set a MOST_REPS record.
- Epley overestimates at high reps (20 kg × 50 → 53.3 kg). This is accepted for now, and changing the rule later only needs a recompute, because records are derived.
