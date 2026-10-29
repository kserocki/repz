# ADR-0009: Detekt 2.0 alpha

- **Status:** Accepted (2026-10-28)

## Context
Detekt 1.23 lags behind the current Kotlin and K2.

## Decision
Use Detekt 2.0 alpha (`dev.detekt`) with Compose rules. It is a dev-only tool.

## Consequences
Rule names and config may change between alphas, so pin the version.

## Amendment (2026-10-29, REP-9)
Wired `io.nlopez.compose.rules:detekt` 0.6.8 (built for exactly this alpha) alongside Detekt. The
gate is the plain, non-type-resolved `detekt` task with `source` set explicitly to every
`src/<sourceSet>/kotlin` directory of the module, not the alpha's own per-compilation
(`detektMainAndroid`, …) or per-source-set (`detektCommonMainSourceSet`, …) tasks — those are not
part of `check` by default and are left unused, so coverage does not depend on how the alpha
discovers source sets under the AGP 9 KMP library plugin or AGP 9 built-in Kotlin. Config
(`mobile/config/detekt/detekt.yml`) is `buildUponDefaultConfig: true` with four small overrides plus
the full Compose rule set at its documented defaults; no baseline. Detekt runs its own bundled
Kotlin 2.4.10 in an isolated classpath, so it does not conflict with the project's Kotlin 2.4.21.

`FunctionNaming.ignoreAnnotated: ['Composable']` and `UnusedPrivateFunction.ignoreAnnotated:
['Preview']` are both configured, even though neither key appears in the alpha.6 generated default
config: `ignoreAnnotated` is a generic key accepted on every rule (`dev.detekt.api.Config.Companion`,
`ConfigValidationKt`, `AnnotationSuppressorKt`), not a per-rule property, so it never shows up there.
Verified against the alpha.6 jars: a `@Composable fun SampleComposable()` and a private
`@Composable @Preview fun` are not flagged by `FunctionNaming`, while a plain (non-Composable)
PascalCase function still is. Custom multipreview annotations must be added to `FunctionNaming`'s
`ignoreAnnotated` list too. Accepted gap (user, 2026-10-29): `UnusedPrivateFunction` implements
`RequiresAnalysisApi`, so it needs type resolution and is inactive in the plain `detekt` gate today
regardless of its `ignoreAnnotated` config — a private `@Preview` function is simply never checked
for being unused; revisit when Detekt 2.0 is stable.
