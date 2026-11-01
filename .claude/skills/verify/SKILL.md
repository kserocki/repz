---
name: verify
description: Run the Repz quality gates before a commit or PR (mobile: ./gradlew check = ktlint, Detekt + Compose rules, Konsist architecture tests, Android lint, Koin graph, unit tests; API: lint, typecheck, test, build; later the OpenAPI contract), what CI runs and the pre-commit checklist. Use before every commit, when asked to check, or when finishing a task.
---

# Verify

## Rule zero: never guess

If anything is unclear (naming, behaviour, UX copy, a token value, which module it belongs to, an API shape) or the docs disagree with the code, **stop and ask the user** before writing code. Do not invent values, screens, endpoints or animations that are not specified. Record approved assumptions in the docs (or an ADR) in the same change.

This also covers failures: if a gate fails for a reason you don't understand, or fixing it would require changing an architecture rule or a lint config, **ask**. Never suppress a rule, add `@Suppress`, or skip a test to make CI green without the user's approval.

## 1. Run what is affected

**Mobile** (from `mobile/`):
```bash
./gradlew check      # ktlint, Detekt + Compose rules, Konsist architecture tests, Android lint, unit tests
./gradlew build      # all targets incl. the compile-time Koin graph check
./gradlew allTests   # commonTest on the Android host + iOS simulator
./gradlew ktlintFormat   # auto-fix formatting before committing
```
No baselines and no blanket suppression: fix findings. A scoped suppression, or any new Konsist/Detekt/ktlint exclusion, needs the user's approval and a note in an ADR.

**API** (from `api/`, works today):
```bash
pnpm lint && pnpm typecheck && pnpm test && pnpm build
pnpm openapi:export && git diff --exit-code openapi.json   # from REP-17 on: contract is committed and current
```
**Contract:** shared fixtures are decoded by a `core:network` test (from REP-17 on).

**Brand assets** (from the repo root, when `brand/`, Android `res/` or `mobile/iosApp/` changed):
```bash
python3 brand/logo/src/android_vectors.py && mobile/iosApp/scripts/sync-brand-assets.sh && git status --porcelain   # must print nothing
```

**CI** (GitHub Actions, ADR-0018) runs the same commands: `./gradlew check :app:android:assembleDebug` (Linux), `./gradlew iosSimulatorArm64Test compileKotlinIosArm64` + the `xcodebuild` simulator build (macOS), the brand check and the API gates. A PR is mergeable only when `Mobile CI passed` and `API CI passed` are green. Reproduce a failure locally with the same command.

> Current commands are listed in `AGENTS.md` §5. If a command doesn't exist yet, report it as not available. Don't make one up.

## 2. Checklist
- [ ] New behaviour has unit tests (domain + ViewModel / service)
- [ ] UI: previews per state, light/dark, PL strings, 200 % font scale
- [ ] No hard-coded colours, dp, springs or user-facing strings
- [ ] Docs/ADR updated if a decision or behaviour changed
- [ ] Conventional Commit message with the Linear key (`REP-xx`)
- [ ] No secrets, `.env`, or files from `design/` staged (`git status`)

## 3. Report
Report each gate as pass/fail with the failing output, without paraphrasing. If something was skipped, say so explicitly.
