# ADR-0003: Full local replica, paged initial pull, locally derived PRs

- **Status:** Accepted (2026-10-28)

## Context
A new device must have the whole history and every PR offline.

## Decision
After login the engine pulls all pages (`limit=500`, one transaction per page, resumable cursor). PRs are recalculated locally once at the end, and are never synced.

## Consequences
A few MB for heavy users. The restore screen shows progress (MOT-18). No server-side PR logic.
