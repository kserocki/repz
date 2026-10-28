# ADR-0002: Offline-first sync: outbox + per-aggregate last-write-wins

- **Status:** Accepted (2026-10-28)

## Context
Gyms often have no signal. Writes must never wait for the network, and conflicts must be simple to explain.

## Decision
Room is the single source of truth. Every write is `entity + outbox` in one transaction. Push first, then pull by cursor. LWW per aggregate by **server arrival order** (global sequence). Delete wins (sticky tombstones). The active workout is local-only until finished.

## Consequences
Deterministic, clock-skew-proof, easy to test. Concurrent edits of the same aggregate on two devices: the later push wins, which is documented and accepted.
