# ADR-0010: Hand-written DTOs + OpenAPI contract tests

- **Status:** Accepted (2026-10-28)

## Context
The API is small and fully ours. Generated Kotlin clients are noisy.

## Decision
The API publishes OpenAPI generated from Zod (`api/openapi.json`, committed). Kotlin DTOs are hand-written. Shared JSON fixtures are validated against the spec in CI on both sides.

## Consequences
Clean code, but discipline is needed: every API change updates the spec, the fixtures and the DTOs (the `new-endpoint` skill).
