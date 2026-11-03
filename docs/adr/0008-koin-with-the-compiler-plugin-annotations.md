# ADR-0008: Koin with the compiler plugin (annotations)

- **Status:** Accepted (2026-10-28). Amended 2026-10-29 (REP-7): decision changed to explicit provider functions after the REP-7 spike found cross-module `@ComponentScan` broken on Kotlin/Native. Amended 2026-11-03 (REP-25): added the S1 pattern for a binding that needs a platform-only constructor argument (`Context`). Amended 2026-11-03 (REP-28): `@KoinViewModel` verified as a cross-module provider function on Android and iOS (the S1 spike), with the `api`-dependency rule that makes it work.

## Context
We want DI errors at compile time instead of at runtime.

## Decision
Every Gradle module with definitions exposes **one** `@Module` class whose bindings are explicit `@Single`/`@Factory`/`@KoinViewModel` provider functions declared inside it — not self-annotated concrete classes discovered via `@ComponentScan`. `@KoinViewModel` works as a cross-module provider on Kotlin/Native (verified by REP-28's S1 spike, see its amendment below), as long as the module declaring the `ViewModel` subclass exposes the lifecycle `ViewModel` dependency as `api`. `app:shared` is the exception: its own module and its `@KoinApplication` entry point always compile together, so it may keep a local `@ComponentScan("com.repz.app.shared")`. One `@KoinApplication` entry in `app:shared` (`RepzApp`, started via `startRepzKoin()`), with its `modules` list built explicitly (no `@Configuration` auto-discovery).

`core:domain`'s and `core:common`'s own shared definitions stay out of those modules (both remain Koin-free): `app:shared` provides them instead, through a `DomainModule` (shared use cases) and a `CommonModule` (`Clock`, `IdGenerator`, dispatchers) — `CommonModule` landed with REP-23, `DomainModule` lands with REP-27. Features provide only their own feature-local use cases. Each definition has exactly one owner, since Koin resolves a duplicate binding last-wins at runtime and the compiler plugin isn't verified to flag that at compile time (Konsist rule D5 enforces one owner).

## Consequences
A broken graph doesn't compile, verified by `app:shared`'s per-platform main and test compile tasks, which cache normally. This also auto-enables `strictSafety`, which stops `app:shared`'s common-metadata compile task from ever reporting up-to-date, though that task itself runs no graph check. The plugin is young: Kotlin 2.4.21 is not on its verified list and warns on every Kotlin compile task, with source, in a module that applies `repz.koin` (35 tasks today) — confirmed in REP-7, never blocks.

REP-7's spike found a real bug: cross-module `@ComponentScan` hints are dropped by the Kotlin/Native KLIB serializer on Kotlin 2.4.20+ (koin-compiler-plugin#106 / KT-79565) — a binding scanned in one Gradle module is invisible to the plugin when a different module compiles against it. Reproduced with a binding that resolved correctly, both at compile time and at runtime, on the JVM/Android target, while the identical binding failed to compile (`[Koin][KOIN-D002] Missing definition`) on both `iosArm64` and `iosSimulatorArm64`. Fixed upstream by PR #129, unreleased as of plugin 1.2.1. Until 1.2.2 ships, every module pays the cost of writing one explicit provider function per binding instead of relying on scanning — more boilerplate, but it is what keeps compile-time safety across module boundaries today. Re-check cross-module `@ComponentScan` in REP-58.

## Amendment (REP-25, 2026-11-03): the S1 pattern for a Context-dependent binding

`core:network`'s `SecureStringStorage` is the first binding that needs a platform type (Android's `Context`) as a constructor argument. `app:shared`'s `commonMain` declares `@Module expect class PlatformModule` with no members; each platform's `actual class PlatformModule` adds its own `@Single fun secureStringStorage(...)`. `Context` is only ever a provider-function **parameter**, never an `actual class` constructor parameter — the Koin compiler plugin does not support generating code for a constructor parameter of an `expect`/`actual` class (koin-compiler-plugin#37). `RepzApp`'s `@KoinApplication(modules = [...])` list includes `PlatformModule::class` alongside `NetworkModule::class`.

Two same-named `actual class PlatformModule` definitions (one per platform source set) would otherwise look like two owners of the same binding to Konsist rule D5 (§12.2); D5 has a documented exception for this exact shape: a binding provided once per platform inside an `expect`/`actual` `@Module` counts as one owner when every `actual` shares the same fully qualified class name and sits in its own source set.

A compile-time graph test needs the same `Context` dependency the real app gets from `androidContext(...)`: `app:shared`'s `commonTest` declares `internal expect val platformTestDeclaration: KoinAppDeclaration`; its Android `actual` binds `single<Context> { Application() }` (a plain host `Application()`, not an Android instrumentation context); its iOS `actual` is a no-op, since `KeychainSecureStringStorage` takes no `Context`.

REP-27 (the Room database builder, which also needs `Context`) reuses this pattern rather than inventing a second one.

## Amendment (REP-28, 2026-11-03): `@KoinViewModel` verified as a cross-module provider

S1 (a 30-minute spike, throwaway worktree) added `@KoinViewModel fun koinProbeViewModel(dependency: KoinProbeDependency): KoinProbeViewModel` to `core:testing`'s one `@Module`, and resolved `KoinProbeViewModel` from `app:shared`'s `commonTest` — a different Gradle module. First attempt (the lifecycle `ViewModel` dependency as `implementation` in `core:testing`) failed `testAndroidHostTest` with:

```
e: ... KoinMultiModuleGraphTest.kt:37:42 Cannot access 'androidx.lifecycle.ViewModel' which is a supertype of
'com.repz.app.core.testing.koin.KoinProbeViewModel'. Check your module classpath for missing or conflicting dependencies.
> Kotlin compiler: MISSING_DEPENDENCY_SUPERCLASS
```

Changing that one dependency to `api` fixed it: **BUILD SUCCESSFUL** on `testAndroidHostTest`, `iosSimulatorArm64Test` and `compileKotlinIosArm64`, all green, no other errors.

**The rule:** a module that declares a class extending `ViewModel` and provides it as a `@KoinViewModel fun`, when that class is resolved by name from a *different* Gradle module (`koinViewModel<X>()`/`get<X>()`/`inject<X>()` outside the declaring module), must depend on the lifecycle `ViewModel` artifact (`androidx-lifecycle-viewmodel-compose`) as **`api`**, not `implementation` — `implementation` doesn't expose the `ViewModel` supertype on the consumer's compile classpath. `koin-core-viewmodel` itself stays `implementation` (it is never referenced by type from outside the declaring module). An ordinary feature `XViewModel`, declared and consumed only inside its own `feature:x:impl`, is unaffected: `KmpFeatureImplConventionPlugin`'s existing `implementation` dependencies already compile that case and don't need to change.

The provider shape is now final (ARCHITECTURE §9): `@KoinViewModel fun xViewModel(...)` inside a module's one `@Module`, resolved with `koinViewModel<X>()` in `XScreen` (Compose) or `get<X>()`/`inject<X>()` in a graph/unit test.

Also landed with REP-28: the first app-lifetime `CoroutineScope` binding (`CommonModule.applicationScope`, `app:shared`); `core:data`'s own `DataModule` (its first binding, `SessionRepository`); and `KmpFeatureImplConventionPlugin`'s `commonMain` now carries the viewmodel (`koin-core-viewmodel`, `koin-compose-viewmodel`), lifecycle (`androidx-lifecycle-viewmodel-compose`, `androidx-lifecycle-runtime-compose`) and immutable-collections (`kotlinx-collections-immutable`) dependencies every `feature:x:impl` needs, resolving the `TODO(REP-28)` that REP-7 left in that plugin.

**Benign warning, not a build failure:** `koin-core-viewmodel:4.2.2` transitively pulls lifecycle `2.9.6` against the project's pinned `2.11.0`, which prints `KLIB loader: the same 'unique_name=...' found in more than one library` during an `iosMain`/`iosTest` metadata compile of any module applying `repz.kmp.feature.impl`. The real per-platform compile (e.g. `compileKotlinIosSimulatorArm64`) is unaffected and warning-free; no action was taken.
