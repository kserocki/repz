---
name: verify
description: Run Repz quality gates before a commit or PR (Kotlin detekt, ktlint, Konsist architecture tests, unit tests, compile-time Koin graph; TS lint, typecheck, tests, OpenAPI contract) and the pre-commit checklist. Use before every commit, when asked to check, or when finishing a task.
---

# Verify

## Rule zero: never guess

If anything is unclear (naming, behaviour, UX copy, a token value, which module it belongs to, an API shape) or the docs disagree with the code, **stop and ask the user** before writing code. Do not invent values, screens, endpoints or animations that are not specified. Record approved assumptions in the docs (or an ADR) in the same change.

This also covers failures: if a gate fails for a reason you don't understand, or fixing it would require changing an architecture rule or a lint config, **ask**. Never suppress a rule, add `@Suppress`, or skip a test to make CI green without the user's approval.

## 1. Run what is affected

**Mobile** (from `mobile/`):
```bash
./gradlew ktlintCheck detekt          # style + static analysis (Compose rules included)
./gradlew :konsist-tests:test         # architecture rules (see AGENTS.md §3)
./gradlew allTests                    # commonTest on JVM + iOS simulator
./gradlew :app:android:assembleDebug  # also compiles the Koin graph (compiler plugin)
```
**API** (from `api/`):
```bash
pnpm lint && pnpm typecheck && pnpm test
pnpm openapi:export && git diff --exit-code openapi.json   # contract is committed and current
```
**Contract:** `./gradlew :core:network:allTests` decodes the shared fixtures.

> Exact task names are fixed during the scaffold. If a command doesn't exist yet, say so and ask. Don't invent an alternative.

## 2. Checklist
- [ ] New behaviour has unit tests (domain + ViewModel / service)
- [ ] UI: previews per state, light/dark, PL strings, 200 % font scale
- [ ] No hard-coded colours, dp, springs or user-facing strings
- [ ] Docs/ADR updated if a decision or behaviour changed
- [ ] Conventional Commit message with the Linear key (`REP-xx`)
- [ ] No secrets, `.env`, or files from `design/` staged (`git status`)

## 3. Report
Report each gate as pass/fail with the failing output, without paraphrasing. If something was skipped, say so explicitly.
