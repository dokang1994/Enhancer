# Gate 8 AgentLoop-Backed Execution Port — Plan

Design: `docs/superpowers/specs/2026-07-20-gate-8-agentloop-execution-port-design.md`

Deliver a real `AgentRunExecution` implementation that runs the Gate 1–4 pipeline
and lets `DurableAgentRunWorker` drive one claim to a verifier-produced disposition.

## Task 1 — `AgentLoopAgentRunExecution` (Contract Verified)

RED: add `AgentLoopAgentRunExecutionTest` naming the absent
`com.enhancer.runtime.AgentLoopAgentRunExecution`; it must fail to compile only on
the absent type. GREEN: implement the port per the design so the three focused
cases (verified, digest-mismatch, missing-target) pass, each resolving the returned
reference through the shared `RunRecordStore`.

Cases:

1. Verified — real source document, `sourceSha256` = its digest; the returned
   reference resolves to a `VERIFIED` RunRecord bound to `taskId` + `sourceDocument`.
2. Digest mismatch — `sourceSha256` differs from the file; the reference resolves to
   a `REJECTED` RunRecord (non-verified); `execute` still returns a reference.
3. Missing target — absent `sourceDocument`; the reference resolves to a
   non-`VERIFIED` RunRecord.

## Task 2 — Durable worker end-to-end over the real port (Integrated)

RED then GREEN: add `FileSystemAgentLoopWorkerIntegrationTest` wiring the real
`AgentLoopAgentRunExecution` into `DurableAgentRunWorker`, sharing one
`FileSystemRunRecordStore` with the `DurableAgentRunFinalizer` and one real
`projectRoot`.

Cases:

1. A verified claim drives the worker to `VERIFIED_COMPLETED`, completes the
   dependent on the next cycle, and clears the checkpoint.
2. A digest-mismatch claim drives the worker to `FAILED`, leaving the dependent
   blocked; the next cycle claims nothing.

## Verification

- Task-classified RED for each new test (missing type; then behavioural cases).
- Focused GREEN for both new suites.
- Full runtime package suite, then full `clean test` regression under
  `--warning-mode all`, and Java 17 strict lint (`-Xlint:all -Werror`).
- `git diff --check`; exactly one `Status: Specified - Next` gate marker.

## Documents to synchronize on completion

`CURRENT_TASK.md`, `DECISION_LOG.md`, `CHANGELOG.md`, `ARCHITECTURE.md`,
`.ai/architecture.md`, `PROJECT_STATE.md`, `SESSION_HANDOFF.md`.

## Out of scope

Per the design: the `WorkPayload` arbitrary-target extension, worker process
isolation (3b), the local IPC adapter (3c), write Tools, retry, controls, and any
commit/push/PR/release without a new explicit request.
