---
name: implement
description: Repz team workflow for implementing Linear issues (REP-xx) with the agent team in .claude/agents. Atlas plans (user approves), implementers Forge/Flex/Bolt/Swift/Iron build their work packages in parallel, consultants Anvil/Muse/Volt/Falcon/Steel/Hawk help, Spotter tests manually with screenshots/recordings, Coach reviews (loops back on changes), a PR is opened, and Captain (PO) records project knowledge. Use whenever the user asks to implement, build or work on one or more REP issues (e.g. "/implement REP-12", "zrób REP-12 i REP-14").
---

# Implement Linear issues with the Repz agent team

## Rule zero: never guess
Applies to everyone, including you (the orchestrator). Unclear means ask. Questions are routed as described below. Only **Captain** talks to the user about product behaviour, and only through you. Never invent answers on behalf of an agent or the user.

## The team (`.claude/agents/*.md`, spawn with `subagent_type: <name>`)

| Area | Implementer (Sonnet 5) | Consultant (Opus 5.5) | Owns |
|---|---|---|---|
| Shared core | `forge` | `anvil` | `mobile/core/*`, ViewModels, use cases, `feature/*/api`, `app/shared` |
| Design system & UI | `flex` | `muse` | `core/designsystem`, `*Screen`/`*Content`, resources, MOT-xx |
| Android | `bolt` | `volt` | `androidMain`, `app/android` |
| iOS | `swift` | `falcon` | `iosMain`, `iosApp/` |
| Backend | `iron` | `steel` | `api/`, `contract/`, `docker-compose.yml` |
| QA | `spotter` | `hawk` | manual tests, media, `test-report.md` |

Leadership (Opus 5.5): **`atlas`** (architect and planner), **`coach`** (reviewer), **`captain`** (PO and keeper of the knowledge base in Linear).

You are the **orchestrator**: you spawn agents, route questions, keep the work folder, talk to the user, and touch git, GitHub and Linear status.

## Question routing

Any agent may end its turn with:
```
QUESTION
to: <Anvil|Muse|Volt|Falcon|Steel|Hawk|Atlas|Captain|User>
context: …
question: …
```
1. Append it to `.claude/work/REP-xx/questions.md` (time, from, to, question).
2. Route it:
   - consultant / Atlas / Captain: spawn that agent with the question plus pointers (plan, files). Reuse an agent already running for this issue via SendMessage.
   - `to: User` (only Captain or Atlas send these): ask the user, ideally with AskUserQuestion and options.
3. Resume the asker (SendMessage) with the answer, and log the answer.
4. If Captain wasn't involved but the answer is product knowledge, send it to Captain to record.

Default routing for implementers: technical → their consultant; plan intent → Atlas; product behaviour → Captain → (if unknown) User.

## Work folder and worktree per issue

- Worktree: `git worktree add ../repz-REP-xx -b <type>/REP-xx-<slug> origin/main`. Every agent for this issue works **only** in that path (tell them the absolute path).
- Work folder (git-ignored, in the main checkout): `.claude/work/REP-xx/` with `issue.md`, `plan.md`, `questions.md`, `impl-log.md`, `test-report.md`, `media/`, `review-N.md`.

## Parallelism
- **Across issues:** up to **3 issues** at once, each in its own worktree. Plans still need approval one by one. Spotter runs for one issue at a time (shared emulator/simulator), so queue QA.
- **Within an issue:** run all work packages of the same `parallel group` concurrently (multiple Agent calls in one message). Respect `depends_on`. Owners never edit each other's paths, so there are no conflicts.
- Don't start an issue that Linear marks as **blocked by** an unfinished one.

## Pipeline per issue

### 0 · Setup
1. Fetch the issue, its comments and relations from Linear and write `issue.md`. Stop if it's blocked.
2. Create the worktree and branch. Set Linear status to **In Progress**.

### 1 · Plan (Atlas) → user approval
1. Spawn `atlas` with the issue, the worktree path and the work-folder path. Atlas writes `plan.md` (scope, open questions, design, **work packages with owner / paths / depends_on / parallel group**, manual test plan with required recordings, acceptance criteria).
2. Resolve open questions (routing above).
3. Show the user a short plan summary (packages and owners, what runs in parallel, the test plan) and the path. Post the plan as a comment on the Linear issue.
4. **Wait for explicit approval.** If the user requests changes, Atlas revises. No implementation before an "ok".

### 2 · Implementation (Forge / Flex / Bolt / Swift / Iron)
1. Spawn the owner of each package of the current parallel group, concurrently. Give each: worktree path, `plan.md`, its package ids, and the latest `review-N.md` in fix rounds.
2. Route questions as they come in. When a group is done, start the next group.
3. When all packages are done: run the `verify` gates once more on the whole branch (or ask the relevant implementer to). Every gate must be green before QA.

### 3 · Manual test (Spotter)
Spawn `spotter` with the worktree path and the work folder. It follows the plan's manual test plan, saves media to `media/` (**recordings for every animation in scope**) and writes `test-report.md`. Failing scenarios don't go back to implementers directly: Coach decides in stage 4.

### 4 · Review (Coach)
Spawn `coach` (fresh) with the worktree path and the work folder. It writes `review-N.md`: `APPROVED` or `CHANGES_REQUESTED`, with changes per **owner**.
- `CHANGES_REQUESTED`: back to stage 2 only for the owners listed. Then stage 3 (re-test what changed, re-record touched animations), then stage 4 with N+1.
- **Max 3 rounds.** If round 3 still requests changes, stop and bring the open points to the user.

### 5 · Pull request
1. `git push -u origin <branch>` from the worktree.
2. Upload every file in `media/` to the Linear issue as attachments (Linear attachment upload tools) and collect the URLs.
3. `gh pr create --base main` with title `<type>(<scope>): <summary> (REP-xx)` and this body:
   ```markdown
   ## What & why
   <2–4 sentences, user-visible result and reason>

   ## Highlights
   - <key change>   ·   built by <owners>

   ## Screenshots & recordings
   | Screen / state | Android | iOS |
   |---|---|---|
   | <state> | [screenshot](<linear url>) | [recording](<linear url>) |
   > All media are attached to REP-xx in Linear.

   ## How it was tested
   - Unit tests: <what>
   - Manual (Spotter): <platforms, scenarios, pass/fail counts>

   ## Review
   Approved by Coach after <N> round(s). <notable fixes>

   ## Notes
   <follow-ups with REP keys, limitations, ADRs touched>

   Closes REP-xx
   ```
4. Set the Linear status to **In Review** and comment the PR link on the issue.
5. **Knowledge capture:** spawn `captain` to update the knowledge base and comment a summary on the issue. If Captain lists outdated repo docs, tell the user (and propose a follow-up issue).
6. **The user merges** (squash). Never merge, close the PR or push to `main` yourself. After the user merges: set the issue to **Done** and remove the worktree (`git worktree remove`).

## Code style (enforced by every agent)
English only · names precise enough to make comments unnecessary · comments only for non-obvious logic (1–3 plain lines) · **never ticket numbers in comments**, except `TODO(REP-xx)` pointing to an existing issue · `AGENTS.md` §3–4.

## Report to the user (per issue)
PR link · Linear link · owners involved · review rounds · Spotter pass/fail summary · questions asked and answered · follow-ups.
