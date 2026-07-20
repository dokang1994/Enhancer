# Session Handoff

Continuation context between work sessions. This file holds only what is true right
now and would otherwise be lost with the session.

It does not restate state, evidence, maturity, or delivery history. Current verified
state is in `PROJECT_STATE.md`, the evidence behind it in `docs/verification-log.md`,
the active task in `CURRENT_TASK.md`, and delivery history in `CHANGELOG.md` and
`git log`. Where this file disagrees with any of them, they win.

## Updated At

2026-07-20

## Working Tree

- The Gate 8 `WorkPayload` execution-input extension and the AgentLoop-backed
  execution port beneath it are merged to `origin/main` through PR #8
  (`ca745c7`).
- The document single-owner restructure is on `refactor/docs-deduplication`,
  recorded in the accepted decision of the same date.
- A governed CLI run was executed against the restructured documents to verify the
  Project Brain composition; its evidence and RunRecord are in the Git-ignored
  `.enhancer/` tree and no document records them.

## Instructions For Next Agent

1. Read `.ai/` and every canonical startup document in the order `AGENTS.md` states.
2. Take the active task from `CURRENT_TASK.md` and current maturity from
   `PROJECT_STATE.md`. Do not infer either from this file.
3. Inspect `git status --short` and the current branch before assuming anything
   about the working tree.
4. If the host has no JDK, provision Java 17 into `.tools/jdk17-runtime` or run
   `scripts/setup-dev.ps1`; `scripts/gradle.ps1` then works normally.
5. The only external command authority is the decision-scoped read-only Git
   adapter. Any new external command capability requires explicit user approval.
6. Preserve the distinction among execution acknowledgement, independently verified
   terminal state, and dependency satisfaction. `ARCHITECTURE.md` states it under
   Completion Semantics.
7. Do not commit or push without an explicit user request.
