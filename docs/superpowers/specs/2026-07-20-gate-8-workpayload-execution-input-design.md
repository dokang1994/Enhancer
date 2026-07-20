# Gate 8 WorkPayload Execution Input — Design

- Date: 2026-07-20
- Gate: Delivery Gate 8 (consuming a Gate 7 payload extension)
- Connection: the named follow-on of the AgentLoop-backed execution port — replace
  the port's source-document fallback with a payload-declared execution input so a
  WorkItem can execute an arbitrary governed target file.
- Maturity target: Contract Verified payload/serialization contracts; Integrated
  worker path over an arbitrary target.

## Problem

`AgentLoopAgentRunExecution` derives `(targetPath, expectedContentSha256)` from
`taskRevision().sourceDocument()`/`sourceSha256()` behind one private seam, so the
executed work is bounded to re-reading the approved task document. The payload
carries no statement of what work to perform. The accepted 2026-07-20 decision
deferred exactly this extension and named its open question: which producer
supplies the target and digest at publish time.

## Producer decision

The publisher's caller supplies the execution input explicitly, mirroring the CLI
authority model where the user provides `target-path` and `expected-sha256` as
governed inputs. `WorkMessagePublisher` gains an overload carrying an
`Optional<WorkPayload.ExecutionInput>`; deriving the pair from snapshot
observations is rejected (observation-time digests are workspace evidence, not
approval authority, and coupling publication to observation states is fragile).

## Contract changes

1. `WorkPayload` (Gate 7, `com.enhancer.bus`) gains one optional component:

   ```java
   public record WorkPayload(
           ApprovedTaskRevision taskRevision,
           String snapshotId,
           Set<String> allowedTools,
           Optional<ExecutionInput> executionInput) implements MessagePayload
   ```

   with nested `public record ExecutionInput(String targetPath,
   String expectedContentSha256)` — `targetPath` bounded non-blank (max 1024
   characters, the existing reference bound), `expectedContentSha256` validated as
   64 lowercase hex. A three-argument convenience constructor delegates with
   `Optional.empty()`, so every existing call site and serialized form remains
   valid source-compatibly.

2. `WorkItem` (`com.enhancer.runtime`) exposes the projection
   `Optional<WorkPayload.ExecutionInput> executionInput()`.

3. Both filesystem serializers (`FileSystemSchedulerQueueStore`,
   `FileSystemAgentRuntimeStateStore`) append a presence flag plus the two strings
   after `allowedTools`, mirroring the existing `writeOptionalString` pattern.
   Schema v1 is revised in place with no version bump (established precedent from
   the terminal-disposition increment); a pre-existing local snapshot without the
   flag fails closed on read because the envelope rejects truncated state —
   accepted for the unreleased artifact.

4. `WorkMessagePublisher.publish` gains the overload with
   `Optional<WorkPayload.ExecutionInput>` after `approvedTask`; the existing
   signature delegates with empty. The publisher performs no new validation beyond
   the payload's own contract: the input is caller-supplied authority data.

5. `AgentLoopAgentRunExecution.deriveExecutionInput` prefers the payload-declared
   input and falls back to `(sourceDocument, sourceSha256)` when absent. The
   `ApprovedTask` construction is unchanged — `sourceDocument` stays the task
   binding, exactly as the CLI separates `CURRENT_TASK.md` from `target-path`.

## Behaviour

- A WorkItem whose payload declares `ExecutionInput(target, digest)` executes that
  target through the same governed pipeline: contained relative path, real
  read-file, evidence, independent digest verification, persisted RunRecord.
- Absent input preserves the source-document behaviour byte-for-byte.
- All existing fail-closed properties carry over: mismatch/missing target are
  carried in the RunRecord as non-`VERIFIED` and finalized to `FAILED`.

## Verification plan

- RED: extend `MessageEnvelopeTest` (payload contract), the two store integration
  tests (round-trip with and without input), `WorkItemTest` (projection),
  `MessagingRuntimeIntegrationTest` (publisher overload),
  `AgentLoopAgentRunExecutionTest` (arbitrary-target execution), and
  `FileSystemAgentLoopWorkerIntegrationTest` (worker end-to-end over an arbitrary
  target); the failure set must name only the absent `ExecutionInput`,
  constructor arity, and accessors.
- GREEN: minimum implementation; focused suites, runtime package suite, full
  `clean test --warning-mode all`, Java 17 strict lint, `git diff --check`, single
  `Specified - Next` marker.

## Out of scope

- Write/mutation Tools, multiple execution inputs per WorkItem, tool-call scripts
  or plans in the payload, worker process isolation (3b), the local IPC adapter
  (3c), retry, controls, and schema version bumps or migration.
