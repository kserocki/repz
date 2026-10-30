# ADR-0004: Feature modules (api/impl) + core modules, enforced by Konsist

- **Status:** Accepted (2026-10-28)

## Context
The codebase must stay understandable as it grows, for humans and agents.

## Decision
`feature:x:api` holds only NavKeys. `feature:x:impl` holds UI, ViewModels and Koin modules. Core modules are split by responsibility. Konsist tests enforce the dependency rules in CI.

## Consequences
More modules, but clear boundaries and fast incremental builds. Rule breaks fail CI.

## Amendment (2026-10-29, REP-8)
Besides NavKeys, `feature:x:api` also exposes one `val xNavKeySerializers: SerializersModule` that registers its keys (iOS has no reflection). `app:shared` combines them. Architecture checks must allow this value.

## Amendment (2026-10-29, REP-9)
Implemented as `:konsist-tests`, a plain Kotlin/JVM module (not KMP), since it reads source **text**
rather than compiling it and so sees every source set of every module, including `iosMain`/
`iosTest`, build-logic and the root build scripts. Each rule runs twice: once against the real
project tree, once against a dedicated fixture tree under
`konsist-tests/src/test/fixtures/<ruleId>/` laid out as a fake module (e.g.
`fixtures/a3/feature/history/impl/src/commonMain/kotlin/…`) that proves the rule actually fails. No
Konsist exclusion or baseline exists for any rule; the full rule catalogue (P1, A1–A5, T1, S1, D1–D5,
V1, N1–N7, C1) is in ARCH §12.2.

Konsist 0.18.1's `KotlinFileParser` rejects `.kts` files outright, even through `scopeFromFile`, so
a rule that needs build-script content (the three architecture rules that check a module's
`build.gradle.kts`, and the comment rule, which also covers the root and build-logic scripts) reads
every `.kt`/`.kts` file as plain text instead of through a Konsist `KoScope`.
