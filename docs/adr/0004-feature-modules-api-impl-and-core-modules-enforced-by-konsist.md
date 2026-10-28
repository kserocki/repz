# ADR-0004: Feature modules (api/impl) + core modules, enforced by Konsist

- **Status:** Accepted (2026-10-28)

## Context
The codebase must stay understandable as it grows, for humans and agents.

## Decision
`feature:x:api` holds only NavKeys. `feature:x:impl` holds UI, ViewModels and Koin modules. Core modules are split by responsibility. Konsist tests enforce the dependency rules in CI.

## Consequences
More modules, but clear boundaries and fast incremental builds. Rule breaks fail CI.
