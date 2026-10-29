---
name: new-feature
description: Scaffold a new Repz screen or feature module (feature:x:api + impl) following MVI-lite, Nav3, Koin annotations and the design system. Use when adding a screen, a feature module, a ViewModel or a navigation destination in mobile/.
---

# New feature / screen

## Rule zero: never guess

If anything is unclear (naming, behaviour, UX copy, a token value, which module it belongs to, an API shape) or the docs disagree with the code, **stop and ask the user** before writing code. Do not invent values, screens, endpoints or animations that are not specified. Record approved assumptions in the docs (or an ADR) in the same change.

## Before you start, confirm with the user
1. Screen ID from `docs/DESIGN_SYSTEM.md` §10 (e.g. D18 Workout details) and which states it has (loading / empty / error / offline / content).
2. Which module it belongs to (an existing `feature:x` or a new one).
3. The intents (user actions) and effects (navigation, snackbar, haptics). Write them down and get a yes.

## Steps
1. **Module** (only for a new feature): create `mobile/feature/<name>/api` and `.../impl` with the convention plugins `repz.kmp.feature.api` / `repz.kmp.feature.impl` (see `mobile/build-logic`). Register them in `settings.gradle.kts`.
2. **api:** only `@Serializable` NavKeys, e.g. `data object HistoryRoute : NavKey`, `data class WorkoutDetailsRoute(val workoutId: String) : NavKey`. Expose `val xNavKeySerializers = navKeySerializers { subclass(…) }` in the api module (required on iOS, no reflection); `app:shared` combines all of them.
3. **Domain first:** use cases go in `core:domain` if shared, or in the feature's `impl/domain` package if feature-only. Use `VerbNounUseCase` with a single `operator fun invoke` and no Koin annotation on the class. Shared use cases are provided by `DomainModule` in `app:shared`, and feature-only ones by the feature's `@Module` (step 6). Each use case has exactly one provider.
4. **Presentation** in `impl/.../presentation/<screen>/`:
   - `XUiState`: data class, defaults = loading state, `ImmutableList` for lists, UI models (`XUi`) mapped from domain (no domain objects with logic in the UI).
   - `XIntent`: sealed interface. `XEffect`: sealed interface.
   - `XViewModel`: plain class, provided from the module (see step 6); `state = …stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), XUiState())`; effects via `Channel(Channel.BUFFERED).receiveAsFlow()`; a single `fun onIntent(intent: XIntent)` with an exhaustive `when`.
   - `XScreen(onNavigate…)`: gets the VM with `koinViewModel()`, `collectAsStateWithLifecycle()`, collects effects in `LaunchedEffect`.
   - `XContent(state, onIntent, modifier)`: stateless, only `core:designsystem` components and tokens. Add `@Preview` per state, light + dark.
5. **Navigation:** `fun EntryProviderScope<NavKey>.xEntries(navigator: Navigator)` in impl; wire it in `app:shared`. Cross-feature navigation goes only through api NavKeys.
6. **DI:** one `@Module` class per module with explicit provider functions (feature-local example: `@Factory fun observeHistory(repo: WorkoutRepository): ObserveWorkoutHistoryUseCase = ObserveWorkoutHistoryUseCase(repo)`), no `@ComponentScan` (ADR-0008). Include it in the `@KoinApplication` in `app:shared`. The compiler plugin verifies the graph on Android and iOS. The exact ViewModel provider shape (`@KoinViewModel fun …`) is confirmed in REP-28. Until then, ask.
7. **Strings:** add EN + PL to Compose Resources. Check that the PL layout does not truncate.
8. **Motion:** use only the MOT-xx IDs listed for this screen (see the `animation` skill).
9. **Tests** (`commonTest`): a ViewModel test per intent (Turbine, fakes from `core:testing`, `StandardTestDispatcher`) and a use-case test per rule.
10. Run the `verify` skill.

## Don'ts
- No `MaterialTheme.colorScheme.primary` for brand semantics (use `RepzTheme.colors.*`), no raw dp outside `RepzSpacing`, no inline `spring(...)`.
- No dependency from `impl` on `core:data`, `core:database`, `core:network` or another `impl`.
- No business logic in Composables or in `XContent`.
