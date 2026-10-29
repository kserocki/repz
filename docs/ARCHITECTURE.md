# Repz — Architecture

> Status: **v0.2** (2026-10-28). Decisions are recorded in [`adr/`](adr/). Visual and motion specs: [`DESIGN_SYSTEM.md`](DESIGN_SYSTEM.md), [`MOTION.md`](MOTION.md), [`../brand/LOGO.md`](../brand/LOGO.md).

Repz is an **offline-first workout logger** for Android and iOS built with Compose Multiplatform, backed by a custom TypeScript API.
The goal of this codebase is to be **impressive and boring at the same time**: modern stack, rich motion, but code that a new developer (or an AI agent) understands in minutes.

---

## 1. Product scope (v1)

| Area | What the user can do |
|---|---|
| **Auth** | Register / log in with email + password. Session survives restarts (refresh token). Log out. |
| **Live workout** | Start an empty workout or from a template, add exercises, log sets (weight, reps, optional RPE, set type), rest timer between sets, finish → summary with PRs. The rest timer is stored as `restEndsAt`. When it ends, a **scheduled local notification** fires (Android `AlarmManager`, iOS `UNUserNotificationCenter`), so it works with the app in the background. |
| **Exercise library** | Browse & search the global catalog (served by the API, cached locally), filter by muscle group / equipment, create custom exercises. |
| **History & progress** | List of past workouts, workout details, per-exercise progress charts (est. 1RM, top set, volume), personal records. |
| **Templates** | Create / edit / delete routines (e.g. Push / Pull / Legs), start a workout from a template in one tap. |
| **Settings** | Units (kg/lb), theme (system/light/dark), dynamic color toggle (Android 12+), default rest time, haptics on/off, language (system/EN/PL; on iOS it opens system Settings), sync status, about & licenses, log out. Weekly workout goal is stored in preferences (default 4); its settings UI is still an open question. |

Everything works **without network**. Network is only needed for login/registration and for syncing.

**Out of scope for v1:** social features, password reset (no email service locally), wearables, exercise videos, web/desktop targets.

---

## 2. Repository layout (monorepo)

```
repz/
├── AGENTS.md                    # Single source of truth for AI agents (CLAUDE.md just imports it)
├── CLAUDE.md                    # → @AGENTS.md
├── docs/
│   ├── ARCHITECTURE.md          # this file
│   ├── adr/                     # Architecture Decision Records (one decision per file)
│   ├── DESIGN_SYSTEM.md         # tokens, components, screens (from design v0.2)
│   ├── MOTION.md                # springs, haptics, MOT-01…29
│   └── design/                  # prompts used with Claude Design
├── brand/                       # logo generators + exported logo/app-icon files (LOGO.md)
├── design/                      # git-ignored: local Claude Design export + unpacked boards
├── .claude/skills/              # new-feature, new-endpoint, sync-entity, animation, design-system, verify
├── .github/workflows/           # mobile.yml, api.yml
├── docker-compose.yml           # postgres + api
├── api/                         # TypeScript backend (pnpm)
└── mobile/                      # Gradle root for KMP
    ├── build-logic/             # convention plugins
    ├── gradle/libs.versions.toml
    ├── app/
    │   ├── android/             # com.android.application – thin Android entry (MainActivity, Application)
    │   └── shared/              # KMP umbrella: App(), root navigation, Koin graph, iOS framework export
    ├── iosApp/                  # Xcode project – thin SwiftUI host + BGTaskScheduler registration
    ├── core/
    │   ├── common/              # Result/AppError, DispatcherProvider, Clock & Id providers
    │   ├── domain/              # models, repository interfaces, shared use cases (pure Kotlin)
    │   ├── data/                # repository implementations (Room + outbox)
    │   ├── database/            # Room KMP: entities, DAOs, migrations
    │   ├── network/             # Ktor client, DTOs, auth plugin, API services
    │   ├── sync/                # SyncEngine, outbox processing, platform schedulers
    │   ├── datastore/           # user preferences, token storage
    │   ├── designsystem/        # theme, tokens, components, motion primitives
    │   ├── ui/                  # MVI helpers, shared UI utils, formatters
    │   ├── navigation/          # Navigator, top-level destinations, NavKey serialization
    │   └── testing/             # fakes, test dispatchers, fixtures
    └── feature/
        ├── auth/{api,impl}
        ├── workout/{api,impl}       # live session + finish summary
        ├── exercises/{api,impl}     # library + custom exercises
        ├── history/{api,impl}       # history, details, progress charts, PRs
        ├── templates/{api,impl}
        └── settings/{api,impl}
```

Identifiers: package / applicationId / bundle id **`com.repz.app`**. Minimum OS: **Android 10 (API 29)**, **iOS 18**.

Why two separate app modules? With the current Android Gradle Plugin, the Android application module can no longer be the KMP module itself, so `app/android` is a thin shell and all shared code lives in `app/shared`.

---

## 3. Layers & dependency rules

```mermaid
graph TD
    APP[app:shared / app:android] --> FIMPL[feature:*:impl]
    APP --> DATA[core:data]
    APP --> SYNC[core:sync]
    FIMPL --> FAPI[feature:*:api]
    FIMPL --> DOMAIN[core:domain]
    FIMPL --> DS[core:designsystem]
    FIMPL --> UI[core:ui]
    FIMPL --> NAV[core:navigation]
    FAPI --> NAV
    DATA --> DOMAIN
    DATA --> DB[core:database]
    DATA --> NET[core:network]
    SYNC --> DB
    SYNC --> NET
    SYNC --> DOMAIN
    DOMAIN --> COMMON[core:common]
```

**Rules (enforced by Konsist tests in CI, not by good intentions):**

1. `core:domain` is pure Kotlin. No Room, Ktor, Compose, Koin or `androidx.*` imports.
2. `feature:*:impl` **never** depends on another `feature:*:impl`. Cross-feature navigation goes only through `feature:*:api` (`NavKey`s).
3. `feature:*:impl` never depends on `core:data`, `core:database` or `core:network`. It sees only domain interfaces.
4. Only `app:shared` knows about every module. It is the **composition root**: it builds the Koin graph and the navigation.
5. DTOs (`core:network`) and entities (`core:database`) never leak outside `core:data` / `core:sync`. Mapping is explicit (`toDomain()`, `toEntity()`, `toDto()`).

### SOLID in practice (what to point at in an interview)

| Principle | Where it shows |
|---|---|
| **S**ingle responsibility | One use case = one class with `operator fun invoke`. ViewModel only maps intents → use cases → state. |
| **O**pen/closed | Sync is driven by a `SyncHandler` registry: adding a new syncable entity means **adding** a handler, not editing the engine (same on the server). |
| **L**iskov | Repositories have fakes in `core:testing` that honour the same contract and are verified by shared contract tests. |
| **I**nterface segregation | Small interfaces: `WorkoutReader` / `WorkoutWriter`, `SyncScheduler`, `TokenStore`, `Clock`, `IdGenerator`. |
| **D**ependency inversion | Domain defines interfaces; data/sync implement them; Koin wires them in the composition root. |

---

## 4. Domain model

```
User(id, email, displayName, unitSystem)

Exercise(id, name, primaryMuscle, secondaryMuscles, equipment, isCustom)

WorkoutTemplate(id, name, notes, exercises: List<TemplateExercise>)
  TemplateExercise(exerciseId, order, targetSets, targetReps: IntRange?, restSeconds)

Workout(id, templateId?, name, startedAt, finishedAt?, notes, exercises: List<WorkoutExercise>)
  WorkoutExercise(id, exerciseId, order, notes, sets: List<WorkoutSet>)
    WorkoutSet(id, order, weight: Weight, reps, rpe?, type: WARMUP|NORMAL|DROP|FAILURE, completedAt?)

PersonalRecord(exerciseId, type: EST_1RM|HEAVIEST|BEST_SET_VOLUME|MOST_REPS, value, workoutId, achievedAt)
```

Decisions:

- **IDs are generated on the client** (UUID). This is a hard requirement of offline-first, because entities must exist before the server knows about them.
- **Weight is stored in grams** (`Long`) inside a `Weight` value class. That gives exact arithmetic with no float drift. kg/lb exists only at the presentation layer.
- **Aggregates:** `Workout` (with exercises and sets), `WorkoutTemplate` and custom `Exercise` are aggregate roots. **The aggregate is the unit of sync.**
- **Personal records are derived data.** They are recomputed locally (`RecalculatePersonalRecordsUseCase`) after a workout finishes or after a sync pull, and they are never synced. Est. 1RM uses the Epley formula.
- **Time:** `kotlin.time.Clock` and `Instant` are injected, so tests are deterministic.

---

## 5. Offline-first & sync

### 5.1 Principles

1. **The UI reads only from Room** (`Flow`). The network never pushes data straight to the UI.
2. **Every write is local first:** `entity upsert + outbox entry` happen in **one Room transaction**.
3. Sync runs in the background and is **idempotent**, so running it twice is harmless.
4. **One shared `SyncEngine`** in `commonMain`. Platforms only decide *when* it runs.

### 5.2 Local schema additions

Every syncable root table has:

| Column | Meaning |
|---|---|
| `id` | client UUID |
| `server_version` | `Long?` version last seen from the server (`null` = never synced) |
| `is_deleted` | tombstone (soft delete) |
| `updated_at` | local timestamp (for UI/sorting only, **never** for conflict resolution) |

`outbox` table: `entity_type`, `entity_id`, `operation (UPSERT|DELETE)`, `created_at`, `attempts`.
At most **one pending entry per entity** (coalescing): ten edits to one workout produce one push.
`sync_meta` table: `pull_cursor`, `last_successful_sync_at`.

### 5.3 Algorithm

```mermaid
sequenceDiagram
    participant UI
    participant Repo as Repository
    participant DB as Room
    participant Engine as SyncEngine
    participant API

    UI->>Repo: finishWorkout()
    Repo->>DB: tx { upsert workout; upsert outbox }
    Repo->>Engine: scheduler.requestSync()
    Note over Engine: WorkManager / BGTask (network constraint)
    Engine->>DB: read outbox (batch)
    Engine->>API: POST /sync/push (aggregates)
    API-->>Engine: accepted [id → newVersion]
    Engine->>DB: tx { set server_version; delete outbox rows }
    Engine->>API: GET /sync/pull?cursor=N
    API-->>Engine: changes (version > N), nextCursor, hasMore
    Engine->>DB: tx { apply changes; save cursor }
    Engine->>DB: recalculate PRs for touched exercises
```

**Push first, then pull.** The client never loses its own pending changes.

### 5.4 Conflict resolution: last write wins per record (aggregate)

- The server keeps one global Postgres sequence `sync_version_seq`. Every accepted write gets the next value.
- **"Last write" means last write *to reach the server*.** We never trust client clocks, so clock skew cannot corrupt data.
- When pulling, the client **skips** incoming changes for entities that still have a pending outbox entry. The local edit is pushed next and wins, which is consistent with arrival-order LWW.
- **Delete wins:** once a tombstone exists on the server, a later upsert of that ID is ignored and the tombstone is returned on the next pull. This prevents "zombie" records.
- **Active (unfinished) workout is local-only.** It enters the outbox when it is finished. This removes most of the realistic conflicts and avoids sync traffic during a session.

### 5.5 Initial sync (new device / reinstall)

The device keeps a **full local replica** of the user's data. Personal workout data is bounded: 5 years of 5 workouts/week is about 1,300 workouts and 30k sets, which is a few MB. A full replica keeps the whole history and every PR available offline.

- After login, `pull_cursor = 0`. The engine loops `GET /sync/pull?cursor=…&limit=500` until `hasMore == false`, applying **each page in its own transaction**. If the app is killed, the next run resumes from the saved cursor.
- PRs are recalculated **once** at the end of the initial sync rather than per page.
- The UI shows a dedicated **"Restoring your history" screen** with progress (pages are counted against the server-provided `totalEstimate`).
- History screens read from Room. Lightweight summary projections are enough for v1. Paging 3 (Room `PagingSource`) is the upgrade path if lists grow.

### 5.6 Scheduling

| Trigger | Android | iOS |
|---|---|---|
| After a local write | `WorkManager` unique one-time work (`KEEP`), `NetworkType.CONNECTED`, exponential backoff | Run immediately if online; otherwise wait for the next trigger |
| App foreground | one-time work | `SyncEngine.sync()` from `scenePhase == .active` |
| Periodic | `PeriodicWorkRequest` every 1 h with network constraint | `BGAppRefreshTask` (best effort, system decides) |
| Connectivity regained | covered by the WorkManager constraint | `NWPathMonitor` → sync |

`SyncScheduler` is an interface in `core:domain`. `core:sync` has `AndroidSyncScheduler` (WorkManager) and `IosSyncScheduler` (BGTaskScheduler). Both call the same `SyncEngine`.

### 5.6 Sync status in the UI

`SyncEngine` exposes `StateFlow<SyncStatus>` (`Idle(lastSyncedAt) | Syncing | Offline(pendingCount) | Error(reason)`). The design system has an animated **sync indicator** component for it.

### 5.7 Logout

Logout **wipes the local database** (data belongs to a user). If the outbox is not empty, the user sees a confirmation: *"N changes haven't been synced yet."*

---

## 6. Mobile tech stack

> Exact versions: see §11 (filled in from verified sources and kept in `libs.versions.toml`).

| Concern | Choice |
|---|---|
| Language / UI | Kotlin 2.x, Compose Multiplatform, Material 3 (Expressive) |
| Navigation | Navigation 3 (JetBrains multiplatform artifacts) |
| DI | Koin + **Koin Compiler Plugin** (annotations, compile-time graph verification) |
| Database | **Room 3** (`androidx.room3`, KSP) + bundled SQLite driver |
| Background work | WorkManager (Android), BGTaskScheduler (iOS) |
| Networking | Ktor client + kotlinx.serialization, Ktor `Auth` plugin (bearer + refresh) |
| Preferences | DataStore Preferences (multiplatform) |
| Secure storage | Android: Keystore-backed encryption, iOS: Keychain (behind `TokenStore`) |
| Images | Coil 3 (only if exercise images are added) |
| Logging | Kermit |
| Resources / i18n | Compose Multiplatform Resources: EN (default) + PL |
| Collections in UI state | kotlinx-collections-immutable |
| Testing | kotlin.test, Turbine, coroutines-test, hand-written fakes (no mocking framework) |
| Static analysis | Detekt + Compose rules, ktlint, Konsist |

---

## 7. Presentation: MVI-lite

Each screen has exactly these pieces (generated by the `new-feature` skill):

```kotlin
// UiState: immutable, everything the screen needs to render
data class HistoryUiState(
    val isLoading: Boolean = true,
    val workouts: ImmutableList<WorkoutSummaryUi> = persistentListOf(),
)

// Intent: everything the user can do
sealed interface HistoryIntent {
    data class WorkoutClicked(val id: WorkoutId) : HistoryIntent
    data object RefreshRequested : HistoryIntent
}

// Effect: one-off events (navigation, snackbar, haptics)
sealed interface HistoryEffect {
    data class OpenWorkout(val id: WorkoutId) : HistoryEffect
}

class HistoryViewModel(
    observeHistory: ObserveWorkoutHistoryUseCase,
    private val requestSync: RequestSyncUseCase,
) : ViewModel() {
    val state: StateFlow<HistoryUiState> = /* use case flow → map → stateIn(WhileSubscribed(5s)) */
    val effects: Flow<HistoryEffect> = /* Channel(BUFFERED).receiveAsFlow() */
    fun onIntent(intent: HistoryIntent) { /* when(intent) … */ }
}
```

- **`HistoryScreen`** (stateful) gets the ViewModel from Koin, collects state with `collectAsStateWithLifecycle()`, and handles effects.
- **`HistoryContent`** (stateless) takes `state` and `onIntent`. Every visual state has a `@Preview`. Screenshot tests attach here later.
- No base-class inheritance chains. At most a tiny helper for the effect channel in `core:ui`.

---

## 8. Navigation 3

- `feature:x:api` contains only `@Serializable` route keys: `data object HistoryRoute : NavKey`, `data class WorkoutDetailsRoute(val id: String) : NavKey`.
- `feature:x:impl` contributes entries: `fun EntryProviderScope<NavKey>.historyEntries(navigator: Navigator)`.
- `core:navigation` holds the `Navigator`: **a separate back stack per top-level tab** (Train / History / Exercises / Settings), plus the auth gate.
- Back stacks are saved through an explicit polymorphic `SerializersModule` that registers every `NavKey`. Kotlin/Native has no reflection, so this is required on iOS.
- Transitions are defined once in the design system's motion spec and applied globally. Predictive back is supported on Android.

---

## 9. Dependency injection (Koin + Compiler Plugin)

- Classes declare themselves: `@KoinViewModel class HistoryViewModel(...)`, `@Factory class ObserveWorkoutHistoryUseCase(...)`, `@Single class OfflineWorkoutRepository(...) : WorkoutRepository`.
- Every Gradle module with definitions applies the plugin and exposes **one** annotated module class: `@Module @ComponentScan("com.repz.app.feature.history") class HistoryModule`.
- Platform-specific bindings (WorkManager, Keychain, Room builder) are provided through `expect`/`actual` classes in the module's platform source sets.
- `app:shared` is the only entry point: `@KoinApplication(modules = [...]) object RepzApp`, started with `startKoin<RepzApp>()`. Android calls it from `Application`, iOS from the SwiftUI `App` init.
- **The full graph is verified at compile time** (missing bindings, cycles, qualifier clashes), so a broken graph does not compile and no runtime `verify()` test is needed.
- Rule for readability: annotations only on concrete classes. No top-level annotated functions, except for third-party objects (Ktor `HttpClient`, Room database), which go in an explicit `@Module` class with `@Single fun` providers.

---

## 10. Backend (`api/`)

### 10.1 Stack

Node.js LTS · TypeScript (strict) · Hono + `@hono/zod-openapi` · Zod · Drizzle ORM + drizzle-kit migrations · Postgres (docker) · JWT · argon2 password hashing · Vitest · pnpm.
API docs: OpenAPI generated from Zod schemas, served at `/openapi.json`, with an interactive UI at `/docs`.

### 10.2 Structure

```
api/src/
├── index.ts                 # server bootstrap
├── app.ts                   # composition root: builds services, mounts routes
├── env.ts                   # Zod-validated environment
├── db/
│   ├── schema/              # one file per table group
│   ├── migrations/
│   └── seed/                # exercise catalog snapshot (wger.de, CC-BY-SA) + seeder
├── scripts/import-wger.ts   # one-off import: wger API → normalized snapshot JSON (committed)
├── modules/
│   ├── auth/      {routes, service, repository, schemas}.ts
│   ├── users/
│   └── sync/
│       ├── sync.routes.ts
│       ├── sync.service.ts
│       └── handlers/        # SyncHandler per entity: workouts, templates, exercises
└── shared/                  # error handler, auth middleware, request id, logger
```

Layers: **routes** (HTTP + validation) → **service** (business rules) → **repository** (Drizzle). Dependencies are wired through plain factory functions in `app.ts`, with no DI framework.

### 10.3 Endpoints

| Method | Path | Notes |
|---|---|---|
| POST | `/auth/register` | email, password, displayName → tokens |
| POST | `/auth/login` | → `{ accessToken (15 min), refreshToken (30 d) }` |
| POST | `/auth/refresh` | **rotation**; reusing an old refresh token revokes the whole token family |
| POST | `/auth/logout` | revokes refresh token family |
| GET / PATCH | `/me` | profile, unit system |
| POST | `/sync/push` | `{ changes: [{ type, id, deleted, payload }] }` → `{ accepted: [{ id, version }] }` |
| GET | `/sync/pull` | `?cursor=&limit=` → `{ changes, nextCursor, hasMore }` (includes global exercise catalog) |
| GET | `/health` | liveness |

**Exercise catalog:** imported from [wger.de](https://wger.de) (CC-BY-SA 4.0) by a one-off script into a **committed snapshot**. The seed is deterministic and there is no runtime dependency on wger. Attribution appears in the README and on the app's About screen. Catalog rows carry `license` and `author` fields.

**Exercise illustrations (every exercise has one):**
- The catalog is **curated to ~150 exercises** from wger (the most common movements, with EN + PL names). That keeps a consistent hand-checked illustration per exercise realistic.
- Each catalog exercise gets an **AI-generated illustration** in one art style defined in the Claude Design pass. Generation uses a fixed style prompt plus per-exercise variables, through a script `api/scripts/generate-illustrations.ts`. Outputs are reviewed by hand and committed as WebP.
- The API serves them as static files (`/static/exercises/{id}.webp`) with immutable cache headers, and `imageUrl` is part of the exercise payload.
- **Offline:** Coil 3 disk cache. After the initial sync, a low-priority job prefetches all catalog images, so the library looks complete without network.
- **Custom (user) exercises** get an illustration for their primary muscle group (about 12 generic illustrations in the same style, bundled in the app as Compose resources). Nothing is ever shown without an image.

Errors use **RFC 9457 Problem Details** (`application/problem+json`) everywhere.

### 10.4 Database

`users`, `refresh_tokens (hashed, family_id, revoked_at)`, `exercises (owner_id NULL = global)`, `workout_templates`, `template_exercises`, `workouts`, `workout_exercises`, `workout_sets`.
Every root table has `user_id`, `version bigint` (from `sync_version_seq`), `deleted_at`, `updated_at`. A push of an aggregate replaces its children in one transaction.

---

## 11. Versions

Verified 2026-10-28 against Maven Central, Google Maven, GitHub releases and npm. The source of truth is `mobile/gradle/libs.versions.toml` and `api/package.json`.

### Mobile

| Library | Artifact | Version | Notes |
|---|---|---|---|
| Kotlin | `org.jetbrains.kotlin` | 2.4.21 | |
| KSP | `com.google.devtools.ksp` | 2.3.12 | versioning decoupled from Kotlin since 2.3.0; needed for Room |
| Compose Multiplatform | `org.jetbrains.compose` | 1.12.2 | ships Material3 **1.12.0-alpha03** (M3 Expressive APIs are available in `commonMain`) |
| Navigation 3 | `org.jetbrains.androidx.navigation3:navigation3-ui` | 1.2.0 | use the JetBrains group (androidx `-ui` is Android-only) |
| M3 adaptive + Nav3 | `org.jetbrains.compose.material3.adaptive:adaptive-navigation3` | 1.3.0 | list-detail scenes (optional) |
| Lifecycle / ViewModel | `org.jetbrains.androidx.lifecycle:lifecycle-viewmodel-compose`, `-viewmodel-navigation3` | 2.11.0 | |
| Room 3 | `androidx.room3:room3-runtime` / `room3-compiler`, plugin `androidx.room3` | 3.0.3 | KSP per target (`kspAndroid`, `kspIosArm64`, `kspIosSimulatorArm64`) |
| SQLite bundled driver | `androidx.sqlite:sqlite-bundled` | 2.7.1 | |
| WorkManager | `androidx.work:work-runtime-ktx` | 2.12.0 | `androidMain` only |
| Koin | `io.insert-koin:koin-bom`, `koin-annotations` | 4.2.2 | |
| Koin Compiler Plugin | `io.insert-koin.compiler.plugin` | 1.2.1 | officially verified up to Kotlin 2.4.20; on 2.4.21 it warns but does not block (verify in spike) |
| Ktor client | `io.ktor` | 3.6.0 | |
| kotlinx.serialization | | 1.12.0 | |
| kotlinx.coroutines | | 1.11.0 | |
| kotlinx-datetime | | 0.8.0 | only for `TimeZone`/`LocalDate`. `Instant`/`Clock` come from stdlib `kotlin.time` |
| kotlinx-collections-immutable | | 0.5.2 | |
| DataStore | `androidx.datastore:datastore-preferences-core` | 1.3.0 | |
| Coil 3 | `io.coil-kt.coil3` | 3.6.3 | exercise images |
| Kermit | `co.touchlab:kermit` | 2.2.0 | |
| AGP | `com.android.application`, `com.android.kotlin.multiplatform.library` | 9.4.0 | KMP library plugin is the recommended path |
| Gradle | | 9.8.2 | |
| Turbine | `app.cash.turbine` | 1.2.1 | |
| Detekt | `dev.detekt` | 2.0.0-alpha.6 | alpha is a deliberate choice: it is the only line with K2 / Kotlin 2.4 support, and it is a dev-only tool |
| ktlint Gradle | `org.jlleitschuh.gradle.ktlint` | 14.2.0 | |
| Konsist | `com.lemonappdev:konsist` | 0.18.1 | |
| Roborazzi | `io.github.takahirom.roborazzi` | 1.77.0 | later; iOS support experimental |

### Toolchain

The machine toolchain the mobile build is verified against (checked in REP-5):

| Tool | Version | Notes |
|---|---|---|
| Xcode | 27.1 (27A9275) | kept by user decision; Kotlin 2.4.21 is tested up to Xcode 26.4 |
| macOS | 27.2 | |
| JDK | 17 | Gradle daemon toolchain |
| Android compileSdk / targetSdk | 37 | minSdk 29 |
| iOS simulator runtime | 27.1 | |
| Gradle | 9.8.2 | above Kotlin 2.4.21's tested 9.7.0 |
| AGP | 9.4.0 | above Kotlin 2.4.21's tested 9.3.1; only the `Configuration.setVisible` deprecation observed |

### API

| Item | Version | Notes |
|---|---|---|
| Node.js | 26 (Active LTS) | |
| TypeScript | 7.0.2 | native (Go) compiler |
| pnpm | 12.16.1 | |
| Hono / @hono/node-server / @hono/zod-openapi | 4.13.14 / 2.1.5 / 1.6.3 | |
| Zod | 4.6.7 | |
| Drizzle ORM / drizzle-kit | 0.45.4 / 0.31.11 | 1.0 still RC |
| postgres (postgres.js) | 3.4.9 | driver for Drizzle |
| jose | 6.2.13 | JWT sign/verify |
| @node-rs/argon2 | 2.2.2 | password hashing, no node-gyp |
| Vitest | 5.0.5 | |
| Biome | 2.6.3 | lint + format in one tool |
| Postgres (docker) | 18.6 | |

**API contract:** the OpenAPI spec is generated from Zod (`/openapi.json`) and committed to `api/openapi.json`. Kotlin DTOs are **hand-written** in `core:network`. A CI contract test validates JSON fixtures shared by both sides against the spec, so neither side can drift without the build failing. Generators (openapi-generator `kotlin`/`multiplatform`) produce noisy code that needs fixups, which is not worth it for an API we fully own.

---

## 12. Quality gates

| Gate | Tool | Where |
|---|---|---|
| Kotlin style | ktlint | pre-commit (optional) + CI |
| Kotlin static analysis | Detekt + Compose rules | CI |
| Architecture rules | Konsist (see §3 rules, naming: `*ViewModel`, `*UseCase` with single `invoke`, `*Screen` vs `*Content`) | CI |
| DI graph | Koin Compiler Plugin, full-graph check at compile time | build |
| Unit tests | `commonTest` on the Android host test (`testAndroidHostTest`) + iOS simulator (`iosSimulatorArm64Test`) | CI |
| TS | Biome/ESLint, `tsc --noEmit`, Vitest | CI |

Planned later (optional): Room DAO tests, Ktor `MockEngine` repository tests, sync conflict scenarios, backend Vitest + testcontainers, screenshot tests.

---

## 13. AI-agent setup

- `AGENTS.md` (root) is a short map: what Repz is, module rules, commands, conventions, and links to docs and skills.
- Skills in `.claude/skills/`: `new-feature`, `new-endpoint`, `sync-entity`, `animation`, `design-system`, `verify`.
- **Every skill and AGENTS.md contains the same rule: "Never guess. If anything is unclear or ambiguous (naming, behaviour, UX, API shape), stop and ask the user before writing code."**

---

## 14. Decision log

All decisions live in [`docs/adr/`](adr/) (ADR-0001 … ADR-0015). Add a new ADR for every decision that changes this document.

## 15. Risks to verify in the first scaffold spike

1. Room 3 KSP wiring with `com.android.kotlin.multiplatform.library` (AGP 9.4) on iOS targets. This is not confirmed by official docs. (REP-6)
2. Koin Compiler Plugin 1.2.1 on Kotlin 2.4.21 (officially verified up to 2.4.20; expected to warn, not fail). (REP-7)
3. Detekt 2.0 alpha + Compose rules + KMP source sets. (REP-9)
4. Nav3 polymorphic back-stack serialization on iOS. (REP-8)
5. Choice of image-generation tool and art style (decided after the Claude Design pass).
6. Xcode 27.1 / Gradle 9.8.2 / AGP 9.4.0, all above Kotlin 2.4.21's tested toolchain (Xcode 26.4, Gradle 9.7.0, AGP 9.3.1) — checked in REP-5: build, link and simulator tests pass; only the AGP deprecation was seen.

If a spike fails, we **stop and decide together**. No silent fallbacks.
