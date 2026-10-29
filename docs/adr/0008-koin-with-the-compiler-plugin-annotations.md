# ADR-0008: Koin with the compiler plugin (annotations)

- **Status:** Accepted (2026-10-28). Amended 2026-10-29 (REP-7): decision changed to explicit provider functions after the REP-7 spike found cross-module `@ComponentScan` broken on Kotlin/Native.

## Context
We want DI errors at compile time instead of at runtime.

## Decision
Every Gradle module with definitions exposes **one** `@Module` class whose bindings are explicit `@Single`/`@Factory`/`@KoinViewModel` provider functions declared inside it — not self-annotated concrete classes discovered via `@ComponentScan`. Using `@KoinViewModel` as a cross-module provider on Kotlin/Native is unverified until REP-28 confirms it. `app:shared` is the exception: its own module and its `@KoinApplication` entry point always compile together, so it may keep a local `@ComponentScan("com.repz.app.shared")`. One `@KoinApplication` entry in `app:shared` (`RepzApp`, started via `startRepzKoin()`), with its `modules` list built explicitly (no `@Configuration` auto-discovery).

`core:domain`'s and `core:common`'s own shared definitions stay out of those modules (both remain Koin-free): `app:shared` provides them instead, through a `DomainModule` (shared use cases) and a `CommonModule` (`Clock`, `IdGenerator`, dispatchers) — both land with REP-23, not here. Features provide only their own feature-local use cases. Each definition has exactly one owner, since Koin resolves a duplicate binding last-wins at runtime and the compiler plugin isn't verified to flag that at compile time (a Konsist rule is a REP-9 candidate).

## Consequences
A broken graph doesn't compile, verified by `app:shared`'s per-platform main and test compile tasks, which cache normally. This also auto-enables `strictSafety`, which stops `app:shared`'s common-metadata compile task from ever reporting up-to-date, though that task itself runs no graph check. The plugin is young: Kotlin 2.4.21 is not on its verified list and warns on every Kotlin compile task, with source, in a module that applies `repz.koin` (35 tasks today) — confirmed in REP-7, never blocks.

REP-7's spike found a real bug: cross-module `@ComponentScan` hints are dropped by the Kotlin/Native KLIB serializer on Kotlin 2.4.20+ (koin-compiler-plugin#106 / KT-79565) — a binding scanned in one Gradle module is invisible to the plugin when a different module compiles against it. Reproduced with a binding that resolved correctly, both at compile time and at runtime, on the JVM/Android target, while the identical binding failed to compile (`[Koin][KOIN-D002] Missing definition`) on both `iosArm64` and `iosSimulatorArm64`. Fixed upstream by PR #129, unreleased as of plugin 1.2.1. Until 1.2.2 ships, every module pays the cost of writing one explicit provider function per binding instead of relying on scanning — more boilerplate, but it is what keeps compile-time safety across module boundaries today. Re-check cross-module `@ComponentScan` in REP-58.
