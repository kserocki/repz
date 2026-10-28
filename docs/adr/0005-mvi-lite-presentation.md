# ADR-0005: MVI-lite presentation

- **Status:** Accepted (2026-10-28)

## Context
Screens need predictable state handling that is easy to test and preview.

## Decision
Each screen has an immutable `UiState`, a sealed `Intent`, a sealed `Effect`, a `ViewModel` with `state: StateFlow`, `effects: Flow` and `onIntent()`, plus a stateful `Screen` and a stateless `Content` with previews.

## Consequences
A bit of boilerplate per screen (scaffolded by the `new-feature` skill). No inheritance chains.
