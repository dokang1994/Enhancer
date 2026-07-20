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

- Everything through Gate 8 connection sub-increment 3b is merged to `origin/main`.
  Both halves of connection 3 exist as unwired capabilities: the file spool adapter
  with its codec (3c) and the isolated worker launcher with its child entry point
  (3b). No production path constructs either.
- The next increment is the one that connects them, and it is blocked on a design
  decision rather than on capability: `AgentRunExecution` must return a
  `run-record/<uuid>` string, and the child's only channel back is an exit code.
  A reverse result spool carrying `ResultPayload` is the accepted direction; the
  decision recording it is not yet written.
- Governed CLI runs were executed during earlier increments to verify the Project
  Brain composition. Their evidence and RunRecords live in the Git-ignored
  `.enhancer/` tree and no document records them.

## Instructions For Next Agent

1. Read `.ai/` and every canonical startup document in the order `AGENTS.md` states.
2. Take the active task from `CURRENT_TASK.md` and current maturity from
   `PROJECT_STATE.md`. Do not infer either from this file.
3. Inspect `git status --short` and the current branch before assuming anything
   about the working tree.
4. If the host has no JDK, provision Java 17 into `.tools/jdk17-runtime` or run
   `scripts/setup-dev.ps1`; `scripts/gradle.ps1` then works normally. That JDK lives
   inside the project root by design, so a check that an executable sits outside the
   project does not apply to the JVM.
5. External command authority exists in exactly two places, each scoped by its own
   accepted decision: `GitWorkspaceCollector`, which runs one fixed read-only Git
   command through a configured external executable, and `IsolatedWorkerLauncher`,
   which can only re-run the JVM this process is already running. Any further
   external command capability requires explicit user approval.
6. Preserve the distinction among execution acknowledgement, independently verified
   terminal state, and dependency satisfaction. `ARCHITECTURE.md` states it under
   Completion Semantics.
7. The RunRecord, not a message, is the authority on a run's outcome. A result
   envelope crossing a process boundary is a claim to be validated against the
   resolved RunRecord, never a substitute for it.
8. Do not commit or push without an explicit user request.
