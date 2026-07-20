# Gate 8 WorkPayload Execution Input — Plan

Design: `docs/superpowers/specs/2026-07-20-gate-8-workpayload-execution-input-design.md`

Extend the work envelope with a caller-supplied execution input and consume it in the
AgentLoop-backed port so the durable worker executes an arbitrary governed target.

## Task 1 — Payload and projection contract (Contract Verified)

RED: extend `MessageEnvelopeTest` (optional `ExecutionInput`, bounds, 4-arg
constructor) and `WorkItemTest` (`executionInput()` projection); failures name only
the absent symbol/arity. GREEN: add nested `WorkPayload.ExecutionInput`, the
optional component, the three-argument convenience constructor, and
`WorkItem.executionInput()`.

## Task 2 — Durable serialization (Contract Verified)

RED then GREEN: extend `FileSystemSchedulerQueueStoreIntegrationTest` and
`FileSystemAgentRuntimeStateStoreIntegrationTest` with present/absent round trips;
append a presence flag plus two strings after `allowedTools` in both serializers,
schema v1 in place.

## Task 3 — Publisher overload (Integrated) + port seam (Integrated)

RED then GREEN: extend `MessagingRuntimeIntegrationTest` (publisher overload
carrying the input through the bus into admission), `AgentLoopAgentRunExecutionTest`
(declared arbitrary target to a `VERIFIED` record with the source-document binding
preserved), and `FileSystemAgentLoopWorkerIntegrationTest` (worker end-to-end over
a declared target). Add the `WorkMessagePublisher` overload and make the port's
derivation seam prefer the declared input.

## Verification

Aligned RED, focused GREEN per suite, runtime package suite, full `clean test
--warning-mode all`, Java 17 strict lint, `git diff --check`, single `Specified -
Next` marker.

## Documents to synchronize on completion

`CURRENT_TASK.md`, `DECISION_LOG.md`, `CHANGELOG.md`, `ARCHITECTURE.md`,
`.ai/architecture.md`, `PROJECT_STATE.md`, `SESSION_HANDOFF.md`.

## Out of scope

Write Tools, multiple inputs, payload plans/scripts, 3b/3c, retry, controls, schema
version bumps, and any commit/push/PR/release without a new explicit request.
