# Current Task

## Status

Completed

## Task

Extend `WorkPayload` with an optional caller-supplied `ExecutionInput(targetPath, expectedContentSha256)`, project it through `WorkItem`, persist it in both filesystem serializers (schema v1 revised in place), publish it through a `WorkMessagePublisher` overload, and make the AgentLoop-backed execution port's derivation seam prefer the declared input over the source-document fallback, so the durable worker executes an arbitrary governed target file end to end.

## Task ID

gate-8-workpayload-execution-input

## Justified By

- 2026-07-20: Record Caller-Supplied WorkPayload Execution Input Enabling Arbitrary-Target Worker Execution

## Context

The AgentLoop-backed execution port (Contract Verified, worker path Integrated) bounds executed work to re-reading the approved task document because the payload states no work. The accepted decision supplies the missing statement as caller-provided authority data — mirroring the CLI's `target-path`/`expected-sha256` model — and rejects snapshot-derived digests (observations are evidence, not authority). Both stores already carry an optional-string precedent (`causationId`) and the in-place schema-v1 revision precedent is established.

## Acceptance Criteria

- `WorkPayload` gains `Optional<ExecutionInput> executionInput` with nested `ExecutionInput(targetPath, expectedContentSha256)` (`targetPath` bounded non-blank, max 1024 characters; digest 64 lowercase hex) and a three-argument convenience constructor delegating to empty; every existing call site compiles unchanged.
- `WorkItem.executionInput()` projects the payload value; both `FileSystemSchedulerQueueStore` and `FileSystemAgentRuntimeStateStore` round-trip the input (present and absent) via a presence flag after `allowedTools`, schema v1 in place, pre-existing snapshots failing closed.
- `WorkMessagePublisher.publish` gains the overload carrying `Optional<WorkPayload.ExecutionInput>`; the existing signature delegates with empty.
- `AgentLoopAgentRunExecution` prefers the declared input and falls back to `(sourceDocument, sourceSha256)`; the `ApprovedTask` construction and Goal binding are unchanged; a declared arbitrary target executes through the governed pipeline to a `VERIFIED` RunRecord (matching digest) or a non-`VERIFIED` RunRecord finalized to `FAILED` (mismatch/missing), never thrown.
- `DurableAgentRunWorker` over the real port drives an arbitrary-target WorkItem to `VERIFIED_COMPLETED` end to end on real filesystem stores.
- No change to the sealed payload hierarchy shape beyond the `WorkPayload` component, and no version bump or migration machinery.

## Out Of Scope

- Write/mutation Tools, multiple execution inputs per WorkItem, payload-carried plans or Tool-call scripts.
- Worker process isolation (3b) and the concrete `MessageTransport` local IPC adapter (3c).
- Retry, cancel/pause/resume, budgets, priority/fairness, multi-agent execution, schema version bumps, migration.
- Commit, push, PR, merge, release, or deployment without a new explicit user request.

## Approval

Approved by the user's 2026-07-20 selection of "C first, A as the named follow-on" and the subsequent request to proceed with A after C completed.

## Verification

- RED: test compilation failed with exactly 30 aligned errors across the seven extended suites, naming only the absent `WorkPayload.ExecutionInput`, the four-argument `WorkPayload` constructor, and the `executionInput()` accessor; production compilation passed.
- Focused GREEN: `MessageEnvelopeTest` 6/6, `WorkItemTest` 3/3, `AgentLoopAgentRunExecutionTest` 4/4, `FileSystemAgentLoopWorkerIntegrationTest` 3/3, `FileSystemSchedulerQueueStoreIntegrationTest` 7/7, `FileSystemAgentRuntimeStateStoreIntegrationTest` 7/7, and `MessagingRuntimeIntegrationTest` 1/1, covering the optional payload contract and bounds, the `WorkItem` projection, present/absent round trips on both filesystem stores, the publisher overload carrying the input through the bus into admission, a declared arbitrary target executed to a `VERIFIED` RunRecord with the source-document binding preserved, and the worker end-to-end over a declared target.
- Full regression under `--warning-mode all`: 65 suites, 299 tests, 297 passed, 2 existing Windows symbolic-link setup skips, 0 failures, 0 errors.
- Java 17 strict lint (`javac -Xlint:all -Werror`) passed across all 158 production sources.
- Structural: `git diff --check` clean; exactly one `Status: Specified - Next` gate marker (Gate 8) in `ROADMAP.md`.

## Next

Choose the next bounded sub-increment: worker process isolation (3b) or the concrete `MessageTransport` local IPC adapter (3c).
