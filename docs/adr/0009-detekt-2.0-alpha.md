# ADR-0009: Detekt 2.0 alpha

- **Status:** Accepted (2026-10-28)

## Context
Detekt 1.23 lags behind the current Kotlin and K2.

## Decision
Use Detekt 2.0 alpha (`dev.detekt`) with Compose rules. It is a dev-only tool.

## Consequences
Rule names and config may change between alphas, so pin the version.
