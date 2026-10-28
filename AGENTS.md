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
- **Strings:** Compose Resources, EN + PL. No hard-coded user-facing text. Numbers are formatted per locale (`82.5` / `82,5`).
- **Tests:** `commonTest`, kotlin.test + Turbine, hand-written fakes from `core:testing` (no mocking libraries). Name tests `given…_when…_then…`.
- **API (TS):** routes → service → repository. Zod schemas are the contract. Errors are RFC 9457 Problem Details.
- **Comments:** explain *why*, not *what*. Keep KDoc on public domain APIs only.
- **Commits:** Conventional Commits, referencing the Linear issue (`feat(workout): log set offline REP-12`).

## 5. Commands

> Filled in during the scaffold (REP "Scaffold" milestone). Until then, ask the user.

| Task | Command |
|---|---|
| Mobile: build + unit tests + lint + architecture | `cd mobile && ./gradlew check` |
| Android app | `cd mobile && ./gradlew :app:android:installDebug` |
| iOS app | open `mobile/iosApp/iosApp.xcodeproj` in Xcode |
| API + DB | `docker compose up -d` (from the repo root) |
| API dev / tests / lint | `cd api && pnpm dev` · `pnpm test` · `pnpm lint` |

## 6. Skills (`.claude/skills/`)

| Skill | Use when |
|---|---|
| `new-feature` | adding a screen or a feature module |
| `new-endpoint` | adding or changing an API endpoint |
| `sync-entity` | making a new entity offline-first and syncable |
| `design-system` | building UI or adding a design-system component |
| `animation` | implementing any MOT-xx animation or haptic |
| `verify` | before every commit or PR |

## 7. Definition of done

- `verify` skill passes (Kotlin: detekt, ktlint, Konsist, unit tests, compile-time Koin graph; TS: lint, typecheck, tests).
- New UI has previews for each state, light and dark, plus a PL string check.
- New behaviour is covered by unit tests (domain + ViewModel).
- Docs and ADRs are updated if behaviour or a decision changed.
- No TODOs without a Linear issue key.
