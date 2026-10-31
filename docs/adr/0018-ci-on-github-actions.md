# ADR-0018: CI on GitHub Actions

- **Status:** Accepted (2026-10-30, REP-10)

## Context
Every gate from REP-9 and REP-12 ran only on a developer machine. The repo is public on GitHub, so standard GitHub-hosted runners (Linux and macOS) are free. iOS simulator tests and the Xcode build need macOS. The project uses Xcode 27.1, and only the `xcode-27` preview image ships it as the default.

## Decision
- **Two workflows**, `.github/workflows/mobile.yml` and `api.yml`, mirroring the local commands in `AGENTS.md` §5:
  - `android` (`ubuntu-24.04`): `./gradlew check :app:android:assembleDebug --continue`. iOS test tasks are disabled on a Linux host (Kotlin prints a warning), but the iOS klibs still compile there;
  - `ios` (`xcode-27`, Xcode 27.1 selected explicitly): `./gradlew iosSimulatorArm64Test compileKotlinIosArm64 --continue`, then the `xcodebuild` simulator build with `CODE_SIGNING_ALLOWED=NO`;
  - `brand-assets` (`ubuntu-24.04`): re-runs `brand/logo/src/android_vectors.py` and `mobile/iosApp/scripts/sync-brand-assets.sh`, then fails if `git status --porcelain` is not empty (closes the ADR-0017 follow-up);
  - `api` (`ubuntu-24.04`, Node from `api/.node-version`, pnpm from `packageManager`): `pnpm install --frozen-lockfile`, `lint`, `typecheck`, `test`, `build`, then a clean-tree check.
- **Path filtering inside the workflow, not with `on.paths`:** a `changes` job (`dorny/paths-filter`) decides which work jobs run. Each workflow ends in one aggregate job, **`Mobile CI passed`** and **`API CI passed`**, which treats skipped jobs as success. A skipped workflow would leave a required check pending forever; an aggregate job never does. Pushes to `main` run everything. On a PR the filter compares the whole PR diff with `main`, so a docs-only commit on a PR that already touches a workflow still runs every job.
- **Hardening:** top-level `permissions: contents: read` (only `changes` adds `pull-requests: read`), no secrets, every action pinned to a full commit SHA with a `# vX.Y.Z` comment, a timeout on every job, failure reports uploaded as artifacts. `concurrency` uses one group per PR, so a newer push cancels the older run; every other run (pushes to `main`, manual runs) gets its own group (`github.run_id`) and is never cancelled or dropped from the queue.
- **Gradle caching:** `gradle/actions/setup-gradle` with the **enhanced** cache provider (user decision, 2026-10-30; Gradle Technologies Terms of Use accepted, free for public repos). The `basic` provider was rejected because its key ignores `mobile/gradle/libs.versions.toml`. `~/.konan` (the Kotlin/Native toolchain) is cached with `actions/cache` in both the `android` and `ios` jobs, because the Linux job also compiles the iOS klibs. The configuration cache stays on, so CI proves compatibility, but it is not cached between runs (that would need an encryption-key secret).
- **REP-61 does not affect CI:** every job is a fresh checkout that runs each Gradle task set once, so the "second build of a fresh checkout" miss never happens there.
- **Protection of `main`:** a repository ruleset requires a pull request and both aggregate checks; the Admin role can bypass it (user decision, 2026-10-30).

## Consequences
- A red aggregate check blocks merging; reproduce it locally with the same command from `AGENTS.md` §5.
- The `ios` job (~12 min cold) is the slowest and depends on a preview image that may queue. When `xcode-27` goes GA or Xcode changes, update the label, the `xcode-select` path and ARCHITECTURE §11 together.
- Known gaps, proposed as follow-ups: Dependabot for action SHAs, SARIF upload for Detekt/lint, an iOS device link/archive job, `actionlint` in CI (today it runs locally: `docker run --rm -v "$PWD:/repo" -w /repo rhysd/actionlint:1.7.12`, configured by `.github/actionlint.yaml`, which declares the preview `xcode-27` label), and the OpenAPI diff (REP-17).
