# AGENTS.md: Repz

Repz is an **offline-first workout logger** (Compose Multiplatform, Android + iOS) with a custom **TypeScript API** (Hono + Drizzle + Postgres). It is a portfolio project, so code quality, readability and polish matter more than speed.

## 0. The one rule: never guess

**If anything is unclear, ambiguous or missing (naming, behaviour, UX copy, a colour, a spring value, an API shape, which module something belongs to), stop and ask the user before writing code.**
- Don't invent tokens, screens, endpoints or animations that aren't in the docs.
- Don't silently "fix" a conflict between docs and code. Point it out and ask.
- When you make an assumption the user approved, record it (ADR or doc update) in the same change.

## 1. Read before you work

| Need | Read |
|---|---|
| Architecture, modules, sync, API | `docs/ARCHITECTURE.md` |
| Why a decision was made | `docs/adr/` |
| Colours, type, shapes, spacing, components, screens | `docs/DESIGN_SYSTEM.md` |
| Animations, springs, haptics | `docs/MOTION.md` |
| Logo, app icon, logo animation | `brand/LOGO.md` |
| Visual reference (local only, git-ignored) | `design/boards/NN-*.txt` (grep-friendly) and `design/repz-design-v0.2.html` |

## 2. Repository map

```
api/        TypeScript API (Hono, Zod/OpenAPI, Drizzle, Postgres), pnpm
mobile/     KMP Gradle root: app/{android,shared}, core/*, feature/*/{api,impl}, build-logic, iosApp
brand/      logo generators (src/) + exported logo and app-icon files. Never hand-edit exports
docs/       architecture, ADRs, design system, motion, Claude Design prompts
design/     git-ignored local design export
.claude/skills/  task playbooks (see §6)
```

## 3. Non-negotiable architecture rules (Konsist enforces them in CI)

1. `core:domain` is pure Kotlin: no Room, Ktor, Compose, Koin or `androidx.*`.
2. `feature:*:impl` never depends on another `feature:*:impl`. It navigates only via `feature:*:api` NavKeys.
3. `feature:*:impl` never touches `core:data`, `core:database` or `core:network`. It sees domain interfaces only.
4. `app:shared` is the only composition root (Koin `@KoinApplication`, navigation).
5. DTOs and entities never leave `core:data` / `core:sync`. Mapping is explicit (`toDomain()`, `toEntity()`, `toDto()`).
6. **The UI reads only from Room.** Every write is `entity + outbox` in one transaction.
7. UI uses `core:designsystem` tokens and components. No hard-coded colours, dp values outside tokens, or ad-hoc springs.

## 4. Conventions

- **Screens:** MVI-lite. `XUiState` (immutable, `ImmutableList`), `sealed interface XIntent`, `sealed interface XEffect`, `XViewModel` with `state`, `effects`, `onIntent()`, a stateful `XScreen` and a stateless `XContent` with a `@Preview` per state.
- **Use cases:** `VerbNounUseCase` with a single `operator fun invoke`.
- **Repositories:** the interface lives in domain (`WorkoutRepository`); the implementation is `OfflineFirstWorkoutRepository` in `core:data`.
- **DI:** Koin annotations on concrete classes (`@KoinViewModel`, `@Factory`, `@Single`); one `@Module @ComponentScan` class per Gradle module.
- **Errors:** domain returns typed results (`DomainResult<T, DomainError>`). No exceptions across layers.
- **Time and IDs:** inject `kotlin.time.Clock` and `IdGenerator`. Never call `Clock.System` or `Uuid.random()` directly in logic.
- **Strings and i18n:** every user-facing text lives in Compose Resources (`strings.xml`, English default, `values-pl` for Polish). Never hard-code text in Kotlin, including content descriptions, notification texts and error messages. Adding a language must only mean adding a new `values-<lang>` folder, never a code change:
  - placeholders (`%1$s`, `%1$d`) instead of concatenating strings; `plurals` for anything counted (sets, reps, workouts); one key per full sentence, never sentence fragments glued together;
  - no language lists or `if (language == "pl")` in code; the language picker reads available locales from resources;
  - numbers, units, dates and durations are formatted with locale-aware formatters, never by hand (`82.5` / `82,5`);
  - layouts survive +30 % text length (buttons wrap, never truncate).
- **Tests:** `commonTest`, kotlin.test + Turbine, hand-written fakes from `core:testing` (no mocking libraries). Name tests `given…_when…_then…`.
- **API (TS):** routes → service → repository. Zod schemas are the contract. Errors are RFC 9457 Problem Details.
- **Language:** English only in code, comments, commits and docs.
- **Naming over comments:** functions, variables and classes are named so precisely that comments are unnecessary. Extract a well-named function instead of writing a comment.
- **Comments only for non-obvious logic:** 1–3 plain lines on what the code does and how it behaves, understandable to any developer. No essays.
- **No ticket numbers in comments**, except `// TODO(REP-xx): …` pointing to an existing Linear issue.
- **Commits:** Conventional Commits with the Linear key in the message (`feat(workout): log set offline (REP-12)`).
- **Workflow:** every Linear issue is implemented with the `implement` skill (plan → approval → implementation → manual test → review → PR).

## 5. Commands

Run from `mobile/` unless noted. One-time setup: create the git-ignored `mobile/local.properties` with `sdk.dir=<path to Android SDK>`. On macOS the Xcode license must be accepted (`sudo xcodebuild -license accept`).

| Task | Command | Status |
|---|---|---|
| Full build (all targets, incl. Android lint) | `./gradlew build` | works |
| Unit tests (Android host + iOS simulator) | `./gradlew allTests` | works |
| Android debug APK / install | `./gradlew :app:android:assembleDebug` · `:app:android:installDebug` | builds; not launchable until REP-11 |
| Module list | `./gradlew projects` | works |
| Style, static analysis, architecture tests | `./gradlew check` (ktlint, Detekt, Konsist) | arrives with REP-9 |
| iOS app | open `mobile/iosApp/iosApp.xcodeproj` in Xcode | arrives with REP-11 |
| API + DB | `docker compose up -d` (from the repo root) | arrives with REP-12 |
| API dev / tests / lint | `cd api && pnpm dev` · `pnpm test` · `pnpm lint` | arrives with REP-12 |

Final command names for CI are fixed in REP-10. If a command you need is missing here, ask.

## 6. Skills (`.claude/skills/`)

| Skill | Use when |
|---|---|
| `implement` | implementing any Linear issue (REP-xx): the full agent pipeline |
| `new-feature` | adding a screen or a feature module |
| `new-endpoint` | adding or changing an API endpoint |
| `sync-entity` | making a new entity offline-first and syncable |
| `design-system` | building UI or adding a design-system component |
| `animation` | implementing any MOT-xx animation or haptic |
| `verify` | before every commit or PR |

## 7. Agent team (`.claude/agents/`)

Implementation runs through the `implement` skill with a team of agents. Every agent follows rule zero (§0) and routes questions through the orchestrator.

| Area | Implementer (Sonnet 5) | Consultant (Opus 5.5) |
|---|---|---|
| Shared core | `forge` | `anvil` |
| Design system & UI | `flex` | `muse` |
| Android | `bolt` | `volt` |
| iOS | `swift` | `falcon` |
| Backend | `iron` | `steel` |
| QA (manual tests, media) | `spotter` | `hawk` |

Leadership (Opus 5.5): `atlas` (plans, splits work into packages), `coach` (code and media review), `captain` (PO; keeps the [knowledge base](https://linear.app/repz-app/document/repz-knowledge-base-a407b9b0e724) in Linear and asks the user when unsure).

## 8. Definition of done

- `verify` skill passes (Kotlin: detekt, ktlint, Konsist, unit tests, compile-time Koin graph; TS: lint, typecheck, tests).
- New UI has previews for each state, light and dark, plus a PL string check.
- New behaviour is covered by unit tests (domain + ViewModel).
- Docs and ADRs are updated if behaviour or a decision changed.
- No TODOs without a Linear issue key.
