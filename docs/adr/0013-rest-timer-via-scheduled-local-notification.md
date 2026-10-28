# ADR-0013: Rest timer via scheduled local notification

- **Status:** Accepted (2026-10-28)

## Context
The rest timer must work with the app in the background. The design suggested an Android foreground service.

## Decision
Store `restEndsAt` in the DB. The UI computes the remaining time from the wall clock. Schedule a local notification (`AlarmManager` / `UNUserNotificationCenter`). No foreground service, no Live Activity in v1.

## Consequences
Less native code and nothing to keep alive. No live countdown in the notification shade in v1.
