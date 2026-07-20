# Changelog

## 2026-07-20 - Add WorkPayload Execution Input For Arbitrary-Target Execution

- Extended `WorkPayload` (`com.enhancer.bus`) with one optional caller-supplied `ExecutionInput(targetPath, expectedContentSha256)` component (`targetPath` bounded non-blank to 1024 characters, digest 64 lowercase hex) plus a three-argument convenience constructor delegating to empty, so every existing call site and the sealed payload hierarchy stay valid.
- Added the `WorkItem.executionInput()` projection and round-tripped the optional input through both filesystem serializers (`FileSystemSchedulerQueueStore`, `FileSystemAgentRuntimeStateStore`) via a presence flag after `allowedTools`, revising schema v1 in place with no version bump; pre-existing snapshots without the field fail closed on read.
- Added a `WorkMessagePublisher.publish` overload carrying `Optional<WorkPayload.ExecutionInput>` as explicit caller authority data (the existing signature delegates with empty), mirroring the CLI's `target-path`/`expected-sha256` model; snapshot-derived targets were rejected because observations are evidence, not approval authority.
- Made `AgentLoopAgentRunExecution`'s derivation seam prefer the payload-declared input and fall back to the approved source document, leaving the `ApprovedTask` construction and Goal binding unchanged, so a WorkItem now executes an arbitrary governed target through the same contained read-file, evidence, independent digest verification, and RunRecord pipeline.
- Proved the change test-first (30 aligned test-compile errors naming only the absent `ExecutionInput`, four-argument constructor, and accessor) and passed the extended payload, projection, both store round-trip, publisher-overload, port arbitrary-target, and worker end-to-end suites, then the full 65-suite/299-test regression (297 passed, 2 existing Windows symbolic-link skips, 0 failures, 0 errors) under `--warning-mode all` with Java 17 strict lint across 158 production sources.
- Left write/mutation Tools, multiple execution inputs, payload-carried plans/scripts, worker process isolation (3b), the local IPC adapter (3c), retry, and controls as future connections; no version bump or migration machinery was added.

## 2026-07-20 - Add AgentLoop-Backed Execution Port

- Added `AgentLoopAgentRunExecution` under `com.enhancer.runtime`, the first production implementation of the worker's `AgentRunExecution` port: one `execute(dispatch)` call assembles the Integrated Gate 1-4 pipeline (governed `read-file` `ToolExecutor` with `EvidenceRecorder`-persisted evidence, bounded `AgentRunController`/`AgentLoop` with the CLI reference bounds, `DeterministicReadFileVerifier`, and the application `AgentRunFinalizer`) and returns the persisted `run-record/<uuid>` reference.
- Executed the approved task's own source document as the governed target — `taskRevision().sourceDocument()` as the `read-file` path and `taskRevision().sourceSha256()` as the expected content SHA-256 — so the increment needs no Gate 7 envelope, queue, or runtime serialization change; a digest mismatch is real drift detection carried in the persisted RunRecord (non-`VERIFIED`, finalized to `FAILED`), never thrown.
- Constructed the `ApprovedTask` directly from the WorkItem's fields (no `ApprovedTaskReader`, no `In Progress` coupling), so the runtime finalizer's taskId-plus-sourceDocument binding holds by construction; pinned the wiring rule that the port persists through the same `RunRecordStore` the worker's `DurableAgentRunFinalizer` resolves from.
- Isolated the `(targetPath, expectedContentSha256)` derivation behind one private seam so the named follow-on `WorkPayload` execution-input extension (arbitrary targets plus its publish-time producer design) replaces only that derivation; recorded the rejected alternatives (revision-irreproducible `ApprovedTaskReader`, unresolvable `snapshotId`, envelope-splitting side store) in the accepted decision.
- Proved the port test-first (2 aligned missing-type compile errors, then 3/3 focused contract cases) and wired the real port into `DurableAgentRunWorker` over shared real filesystem stores: `FileSystemAgentLoopWorkerIntegrationTest` passed 2 of 2, driving a verified claim and its dependent to `VERIFIED_COMPLETED` with really persisted RunRecords and a digest-mismatch claim to `FAILED` with the dependent blocked.
- Passed the 16-suite/78-test runtime package suite and the full 65-suite/293-test regression (291 passed, 2 existing Windows symbolic-link skips, 0 failures, 0 errors) under `--warning-mode all` with Java 17 strict lint across 158 production sources.
- Left the `WorkPayload` execution-input extension, worker process isolation (3b), the concrete `MessageTransport` local IPC adapter (3c), write Tools, retry, and controls as future connections; no worker, dispatcher, runtime, finalizer, queue, or schema contract changed.

## 2026-07-20 - Add Gate 8 In-Process Scheduler Worker

- Added `DurableAgentRunWorker` under `com.enhancer.runtime` (connection sub-increment 3a): one `runOneCycle(leaseDuration)` call drives claim + fenced lease, injected execution to a durable RunRecord, fence-checked `completeExecution`, `finalizeAgentRun`, and the queue disposition in one recoverable, idempotent order, returning the cycle's `WorkItemDisposition` or empty when nothing was claimable.
- Added the worker-owned durable cycle-intent checkpoint: `PendingFinalization` (distinct canonical Goal/AgentRun UUIDs plus optional `runRecordReference`), `PendingFinalizationStore`, and the bounded, strict-UTF-8, digest-checked, atomically published, fail-closed `FileSystemPendingFinalizationStore` single-record adapter with `CorruptedPendingFinalizationException`.
- Wrote the intent before the queue claim so a restarted worker re-supplies the same identities and the dispatcher's idempotent recovery resumes the exact prefix (no second Goal, no orphaned runtime state, no dispatcher change), and persisted the reference before acknowledgement, closing the finalizer's deferred pre-terminal recovery window.
- Added `AgentRunExecution`, the injected execution port returning the RunRecord reference; the real `AgentLoop`-backed port is a named follow-on requiring a `WorkPayload` execution-input extension.
- Routed recovery by runtime state as the source of truth: terminal -> `recoverFinalization`; `AWAITING_VERIFICATION` -> `finalizeAgentRun(ref)`; earlier, unstarted, or missing runtime state (`MissingAgentRuntimeStateException` tolerated) -> re-drive with the same identities, skipping re-execution when the reference exists; execution/finalizer failures fail closed with the intent retained, and an empty-queue cycle leaves no durable trace.
- Proved each behaviour test-first (19 aligned checkpoint compile errors, 5 aligned worker compile errors, then 6 behavioural recovery failures against a deliberate resume stub) and passed 8/8 checkpoint, 9/9 worker, 2/2 filesystem end-to-end integration, and the 14-suite/73-test runtime package suites.
- Passed the full 63-suite/288-test regression (286 passed, 2 existing Windows symbolic-link skips, 0 failures, 0 errors) under `--warning-mode all` with Java 17 strict lint across 157 production sources.
- Left worker process isolation (3b), the concrete `MessageTransport` local IPC adapter (3c), the real execution port, retry, controls, and effect records as future connections; no dispatcher/runtime/finalizer/queue contract or schema changed.

## 2026-07-17 - Add RunRecord-Backed Result-Path Finalization

- Added `DurableAgentRunFinalizer` under `com.enhancer.runtime`, one durable idempotent coordinator over the durable queue, `AgentRuntimeStateStore`, and `RunRecordStore` with no new store or schema change.
- Drove the recoverable order resolve RunRecord -> runtime terminal (`recordResult`) -> queue disposition, deriving the disposition from the runtime terminal status (`COMPLETED -> completeActiveVerified`, `FAILED -> failActive`) so the two stores cannot diverge.
- Resolved (never persisted) the RunRecord by reference, bound it to the Goal on `taskId` plus `sourceDocument`, and carried the RunRecord's `verificationStatus` in a deterministic `ResultPayload` envelope keyed to the AgentRun identity.
- Added two entry points: `finalizeAgentRun(goalId, agentRunId, runRecordReference)` for the forward path and `recoverFinalization(goalId)` for autonomous post-terminal recovery that applies only the queue disposition and needs no reference.
- Honoured the durable queue's recovery contract by re-claiming a requeued active WorkItem before recording its terminal disposition; a disposition already in the completed/failed set is a no-op.
- Failed closed on a missing/corrupt RunRecord (run stays `AWAITING_VERIFICATION`, recoverable), rejected a RunRecord bound to a different task, rejected re-finalize with a different reference, and rejected finalize before execution acknowledgement.
- Proved each behaviour test-first (missing `DurableAgentRunFinalizer`, then missing `recoverFinalization`) and passed the full 60-suite/269-test regression (267 passed, 2 existing Windows symbolic-link skips, 0 failures, 0 errors) with Java 17 strict lint across 151 production sources.
- Left the Scheduler worker/Tool execution and RunRecord production (connection 3), retry through additional AgentRuns, and automatic failure propagation to dependents as future connections.

## 2026-07-17 - Add Durable Queue Terminal Disposition

- Added the terminal `WorkItemDisposition` enum (`VERIFIED_COMPLETED`, `FAILED`) where only verified completion satisfies dependencies.
- Added a separate `failedWorkItemIds` set to schema-v1 `SchedulerQueueState` and extended the partition invariant to `pending + active + verified + failed = admissionOrder` with verified and failed disjoint.
- Split the queue's single `completeActive` into `completeActiveVerified` and `failActive` across the in-memory queue, the durable persist-before-exposure wrapper, and the filesystem store; failed work never enters the dependency-satisfaction set, so its dependents stay blocked.
- Persisted the failed disposition in the schema-v1 on-disk format (revised in place, no version bump) with exact restart recovery; a persisted terminal disposition is never re-run and only interrupted active work is requeued.
- Recorded that the queue stores disposition only, not a failure reason, and that pre-existing local queue snapshots fail closed on read because the unreleased schema-v1 envelope rejects trailing bytes.
- Proved each contract test-first (missing enum, constructor arity, methods, dropped serialization) and passed the full 59-suite/261-test regression (259 passed, 2 existing Windows symbolic-link skips, 0 failures, 0 errors) with Java 17 strict lint across 150 production sources.
- Left `ResultPayload`/RunRecord result wiring, dispatcher-driven disposition recording, retry, automatic failure propagation, and a non-terminal waiting state as future connections.

## 2026-07-16 - Align Gate 8 Connection And Completion Boundaries

- Cross-checked the seven `.ai/` bootstrap documents, canonical governance and architecture documents, and the implemented Gate 8 queue/runtime state contracts.
- Corrected the conflicting next-step wording: fence-checked execution completion persists `AWAITING_VERIFICATION`, while queue completion satisfies dependencies, so the two operations cannot be coupled directly.
- Made durable terminal queue disposition the next bounded contract, with verified completion and failed disposition kept distinct before Scheduler capacity or dependency state changes.
- Added an ordered, gate-owned connection backlog for RunRecord-backed results, process-isolated workers and local IPC, controls, effects, retry, and later multi-agent handoffs.
- Corrected stale RFC and blanket-unimplemented wording without changing production code, capability maturity, Constitution text, Agent rules, or external authority.
- Passed 24 focused actual-document tests (23 passed, 1 existing Windows symbolic-link skip), the full 57-suite/251-test regression (249 passed, 2 existing skips), and structural/reference/whitespace checks with no failure or error.
- Added a next-session design brief describing the semantic collision, the recommended conservative implementation, the higher-throughput waiting-state alternative, the rejected unsafe shortcut, and the unresolved schema/failure/identity decisions.

## 2026-07-16 - Integrate Durable Queue Claims With Fenced AgentRuns

- Added `DurableAgentRunDispatcher` and immutable `AgentRunDispatch`, connecting one active or newly claimed exact WorkItem to Goal creation/recovery, named AgentRun planning/readiness, and fenced lease acquisition.
- Kept queue and runtime artifacts as separate durable boundaries: queue claim persists first, runtime prefixes persist independently, and partial runtime failures retain a recoverable active claim instead of claiming unsupported cross-store atomicity or rollback.
- Added idempotent same-owner re-entry, exact WorkItem matching before recovery mutation, strict AgentRun/owner/post-execution mismatch refusal, and expiry recovery that permits a new owner only with a greater fence.
- Verified all four runtime persistence interruption points, queue-claim failure with no runtime creation, exact filesystem restart recovery, Unicode lease preservation, and authority/provenance retention without Tool execution or queue completion.
- Proved the missing contract through 13 aligned test-compilation errors, then passed 31 focused runtime/store/boundary tests and the complete 57-suite/251-test regression (249 passed, 2 existing Windows symbolic-link skips).
- Passed Java 17 strict lint across 149 production sources; retained terminal queue disposition, workers, results, retry, effects, cross-store transactions, multi-process coordination, and power-loss directory durability as future work.

## 2026-07-16 - Fence Durable Gate 8 AgentRun Ownership

- Added immutable bounded `AgentRunLease` ownership to the durable schema-v1 AgentRun lifecycle with injected time, exclusive expiry, and a persisted monotonic last-issued fence token.
- Restricted acquisition to `READY`; renewal and execution completion require the matching current owner and fence and fail closed at or after expiry.
- Added explicit and recovery-time orphan reclamation that durably returns expired `EXECUTING` work to `READY`, retains fence history, and gives the next owner a strictly greater token.
- Extended strict-UTF-8 integrity-checked filesystem state encoding to recover exact lease timestamps, owner, fence, and aggregate fence history; store updates permit the fence to stay current or advance exactly one.
- Preserved persist-before-exposure for acquire, renew, complete, and reclaim, with regression coverage proving each injected storage failure leaves the previous revision and lease authoritative.
- Passed 68 focused runtime/bus/boundary tests, the complete 55-suite/243-test regression (241 passed, 2 existing Windows symbolic-link skips), and Java 17 strict lint across 147 production sources.
- Added no worker or Tool execution, external-effect fencing, retry, multi-process locking, distributed clock-skew protocol, schema migration, or parent-directory power-loss durability.

## 2026-07-16 - Add Durable Gate 8 Goal And AgentRun Lifecycle

- Added immutable schema-v1 `RuntimeGoal`, `RuntimeAgentRun`, and `AgentRuntimeState` over one exact existing WorkItem without widening task, snapshot, capability, logical-run, or Tool-scope provenance.
- Enforced forward-only Goal and AgentRun lifecycles, distinct canonical identities, matching work/result message provenance, and Verified-only completion with every other verification state recorded as explicit failure.
- Added `DurableAgentRuntime`, persisting every successful transition before exposure and leaving the previous durable and in-memory revision unchanged on storage failure.
- Added `FileSystemAgentRuntimeStateStore` with a 4 MiB ceiling, strict UTF-8, complete-envelope SHA-256 integrity, exact WorkItem/result-envelope recovery, atomic create/replace publication, revision checks, and fail-closed missing/corrupt/oversized/trailing/unsupported-state handling.
- Proved the missing contract through 64 aligned test-compilation errors, then passed 63 focused runtime/bus/boundary tests and the complete 55-suite/238-test regression (236 passed, 2 existing Windows symbolic-link skips).
- Passed Java 17 strict lint across 146 production sources; retained retry, leases/fencing, worker execution, effect records, RunRecord resolution, schema migration, history cleanup, multi-process coordination, and parent-directory power-loss durability as future work.

## 2026-07-16 - Remove Runtime Package Dependency Cycles

- Moved VerificationDecision, VerificationStatus, and VerificationCode unchanged from verification implementation code to neutral `com.enhancer.kernel`.
- Moved AgentRunFinalizer from `com.enhancer.loop` to `com.enhancer.application`, leaving finalization behavior and persist-before-completion semantics unchanged.
- Added `VerifiedAgentRunTransition` as the explicit application-facing port while retaining the actual AgentRunState completion method as package-private.
- Enforced an acyclic dependency direction with `RuntimePackageBoundaryTest`; loop no longer imports run or verification, run no longer imports verification, and kernel imports none of the runtime/application packages.
- Preserved RunRecord schema, stored enum names, CLI behavior, verification decisions, and replay compatibility.
- Passed 27 focused tests, the complete 53-suite/228-test regression (226 passed, 2 existing symbolic-link skips), and Java 17 strict lint across 135 production sources.

## 2026-07-16 - Bound In-Process Tool Worker Accumulation

- Added one process-wide 64-slot live Tool isolation capacity shared by default across ToolExecutor instances.
- Held each slot until the actual worker thread terminates, so timeout, interrupt, close, and shutdown do not undercount interrupt-ignoring code.
- Added typed terminal `ISOLATION_CAPACITY_EXHAUSTED` refusal before worker/thread creation when the process ceiling is full.
- Preserved independent next invocation below the ceiling and proved deterministic saturation/recovery with an injected one-slot shared capacity.
- Passed 41 affected tests with 1 existing symbolic-link skip, the full 52-suite/227-test regression (225 passed, 2 existing skips), and Java 17 strict lint across 134 production sources.
- Kept process isolation and OS-level termination as required future boundaries; this change is containment, not forced recovery.

## 2026-07-16 - Harden Unicode And Mutable-File Resource Bounds

- Added shared Unicode-safe prefix/suffix bounding that preserves existing UTF-16 ceilings without splitting supplementary surrogate pairs.
- Applied it to VerificationEvidence tails, ToolExecutor diagnostics, CLI bounded output/values, and bounded Workspace failure reasons.
- Added bounded file read/hash operations that enforce the byte ceiling during consumption, allocate no more than the accepted read limit, and inspect at most one extra byte for overflow.
- Applied in-operation bounds to ReadFileTool, ProjectContextReader, target-file hashing, and Evidence, RunRecord, and Scheduler queue artifact resolution while preserving strict UTF-8 and typed failure behavior.
- Reproduced the Unicode defect with a 4097-code-unit valid string and proved strict UTF-8-safe output; focused tests passed 18/18, affected integration tests passed 54 with 2 existing symbolic-link skips, the full 52-suite/226-test regression passed 224 with the same 2 skips, and strict lint passed across 133 production sources.
- Recorded that stuck in-process Tool threads, the loop/run/verification package cycle, and parent-directory power-loss durability require separate work.

## 2026-07-16 - Add Durable Gate 8 Queue State And Restart Recovery

- Added immutable schema-v1 `SchedulerQueueState` over one canonical queue identity, one logical run, admission order, pending/active/completed state, WorkItems, dependencies, and unchanged Gate 7 envelope provenance.
- Added `FileSystemSchedulerQueueStore` with a 64 MiB state ceiling, strict UTF-8, complete-envelope SHA-256 integrity, atomic create/replace publication, revision checks, and fail-closed missing/corrupt/oversized/trailing/unsupported-state handling.
- Added `DurableSingleWorkerSchedulerQueue`, staging every enqueue, successful claim, and completion on a copy and persisting the next revision before exposing it.
- Added restart recovery that persists interrupted active work back into admission-ordered pending state, making the queue honestly at-least-once without claiming effect deduplication, leases, fencing, or workers.
- Passed 14 focused runtime/integration tests, the 50-suite/219-test full regression (217 passed, 2 existing symbolic-link skips), and Java 17 strict lint across 130 production sources.
- Clarified that atomic publication prevents partial process-visible state but does not claim power-loss durability of parent-directory metadata.

## 2026-07-16 - Add The Gate 8 Single-Worker Scheduler Queue

- Added immutable `QueuedWork` over one existing `WorkItem` with at most 256 unique canonical earlier-admitted dependency identities.
- Added a deterministic run-scoped `SingleWorkerSchedulerQueue` bounded to 4096 admissions, with duplicate rejection, FIFO dependency readiness, one active slot, and matching explicit completion.
- Preserved the exact WorkItem and Gate 7 envelope without adding task approval, Tool authority, verification, worker execution, persistence, leases, retry, cancellation, priority, or recovery.
- Proved the missing contracts through a 25-error focused RED, then passed all 45 focused messaging/runtime tests, the complete regression, and Java 17 strict lint.
- Kept Gate 8 at `Specified - Next`; only WorkItem admission and the in-memory queue sub-capabilities are Contract Verified, with durable queue state and restart-safe recovery next.

## 2026-07-16 - Assess Gate 7 Integrated Maturity

- Reassessed all six Gate 7 scope items and four exit criteria against the named Gate 6-to-Gate 7-to-Gate 8 integration path and fresh contract evidence.
- Classified the real work-message queue path as Integrated: approved Workspace input crosses one unchanged `WorkPayload` envelope through queue delivery, journaling, replay, and duplicate suppression into `WorkItem` admission.
- Retained Gate 7 at Contract Verified because result/control/handoff and non-empty-causation flows, topic and reliability branches, and `MessageTransport` still lack named real production connections.
- Passed all 42 focused messaging/runtime tests, the 47-suite/208-test full regression (206 passed, 2 existing symbolic-link skips), and Java 17 strict lint across 122 production sources.
- Changed no production or test behavior, capability maturity, next-gate marker, Constitution rule, external authority, or release state.

## 2026-07-16 - Add Product Journey Evaluation And Security Tracks

- Added a cross-cutting Product Journey and Evaluation Track with four initial end-to-end journeys and a fifth priority for a repeatable release-quality harness.
- Defined explicit-denominator measures for task success, incorrect changes, recovery, cost/time, intervention, held-out regression, and multi-agent delta, with versioned fixtures and thresholds fixed before evaluation.
- Replaced any implied universal exactly-once Scheduler goal with at-least-once delivery plus stable idempotency, fenced leases, checkpoints, state migration, orphan reclamation, and replay-safe or compensatable effects.
- Ordered interface delivery around one shared Run/approval/verification/evidence/control API: CLI reference surface, VS Code second, and Desktop later as supervision; made one change-centered review a Gate 12 exit criterion.
- Added a layered default-security baseline for untrusted repository/Tool/model/MCP/plugin/dependency content and assigned concrete enforcement to the owning delivery gates without amending the Constitution.
- Strengthened multi-agent and release criteria so claims require measured journey improvement and predeclared quality thresholds rather than Agent self-report or successful demonstrations alone.

## 2026-07-16 - Prepare The Gate 7 Runtime Integration Path

- Added `WorkMessagePublisher` to derive and publish one existing bounded work envelope from a matching repository-approved task and real Gate 6 Workspace snapshot without creating approval or Tool authority.
- Added `WorkItemAdmissionHandler` to retain the delivered envelope unchanged in one Gate 8 `WorkItem` through injected boundaries, without adding storage, scheduling, execution, or concrete IPC behavior.
- Added a named integration test over the real Context Reader, Workspace collector, in-process queue, journal, replay, and WorkItem admission path; it proves unchanged provenance/authorization projections and duplicate-free replay.
- Passed 42 focused tests, the 47-suite/208-test full regression (206 passed, 2 existing symbolic-link skips), and Java 17 strict lint across 122 production sources.
- Kept Gate 7 at Contract Verified; the integration evidence is input to a separate gate-level maturity assessment rather than an automatic promotion.

## 2026-07-16 - Add Gate 8 WorkItem Admission

- Added immutable `WorkItem` admission over one unchanged Gate 7 `WorkPayload` envelope with a distinct canonical work identity and a 256-character required-capability ceiling.
- Exposed approved task revision, Workspace snapshot identity, logical run identity, and allowed Tools only as projections of the retained envelope, without creating authority, runtime state, scheduling, persistence, leases, or execution.
- Proved the missing contract through an 11-error focused RED, then passed 41 focused tests, the 46-suite/207-test full regression (205 passed, 2 existing symbolic-link skips), and Java 17 strict lint across 120 production sources.
- Kept Gate 8 at `Specified - Next`; only the admission sub-capability is Contract Verified and the dependency-ready single-worker Scheduler queue is next.

## 2026-07-16 - Promote Gate 7 And Advance Gate 8

- Reassessed all six Gate 7 scope items and all four exit criteria against fresh contract evidence after closing the payload-cardinality blocker.
- Promoted Delivery Gate 7 to Contract Verified without claiming production messaging integration, a concrete adapter, or a real process boundary.
- Advanced Delivery Gate 8 Agent Runtime and Scheduler to the sole `Specified - Next` marker and synchronized the two actual-Roadmap self-hosting expectations.
- Verified 39 focused bus tests, the 45-suite/205-test full regression (203 passed, 2 existing symbolic-link skips), and Java 17 strict lint across 119 production sources.
- Verified the completed document state through 16 actual-document self-hosting tests: 15 passed and 1 existing Windows symbolic-link setup case skipped, with both proposal paths selecting Gate 8.

## 2026-07-16 - Bound Gate 7 Work Payload Scope

- Added an explicit maximum of 256 unique `WorkPayload.allowedTools` names while retaining the existing 256-character per-name ceiling and immutable copying.
- Proved the gap behaviorally before the fix: a payload with 257 valid names was accepted during RED; the corrected contract accepts exactly 256 and rejects 257.
- Closed the prior maturity assessment's bounded-payload blocker without selecting a concrete IPC adapter or changing Gate 7 lifecycle status.
- Verified all 39 bus tests, the 45-suite/205-test full regression (203 passed, 2 existing symbolic-link skips), and Java 17 strict lint across 119 production sources.

## 2026-07-16 - Assess Gate 7 Messaging Maturity

- Mapped every Gate 7 scope item and exit criterion to fresh contract evidence or an explicit blocker without changing production or test code.
- Confirmed that transport adapters and production wiring are integration work rather than requirements for Contract Verified maturity.
- Found one gate-level blocker: `WorkPayload.allowedTools` has bounded entries but unbounded collection cardinality, so the bounded-payload exit criterion is not yet satisfied.
- Recommended one test-first payload-bound correction before Gate 7 promotion; rejected both premature promotion and a premature concrete IPC adapter.

## 2026-07-16 - Define The Transport-Neutral IPC Boundary

- Added immutable `TransportMessage` over the existing destination and envelope plus provider-neutral `MessageTransport`.
- Added typed `TransportOutcome`/`TransportStatus` so hop acceptance, backpressure, and unavailability remain distinct from Message Bus subscriber delivery.
- Kept adapters, endpoints, serialization, authentication, threading, persistence, scheduling, and production wiring out of the contract.
- Verified test-first with 33 expected RED missing-symbol errors, 38 focused bus tests, the 204-test full regression, and Java 17 strict lint across 119 production sources.

## 2026-07-16 - Verify Windows Real-Path Boundaries And Correct Evidence Policy

- Added Windows junction regressions that execute successfully on this host and prove both `ReadFileTool` and `ProjectContextReader` reject real paths escaping the project root.
- Replaced the unused `EvidenceRetentionPolicy`/30-day field with `EvidenceStoragePolicy`, which exposes only the enforced per-artifact content bound.
- Added no evidence deletion or expiry and passed focused plus Tool/Context/Verification/Loop regressions; the two privilege-dependent symbolic-link tests remain skipped while junction coverage passes.
- Completed the combined delivery cross-regression at 44 suites/200 tests (198 passed, 2 existing symlink skips), with both junction cases executed and Java 17 strict lint passing across 115 production sources.

## 2026-07-16 - Bound Workspace RunRecord Observation

- Added deterministic newest-first `recentReferences(limit)` while retaining complete reference listing and point replay.
- Limited Workspace collection to 256 recent records, capping full payload resolution and preventing accumulated history from exhausting the 4096-observation snapshot bound.
- Verified newest selection, invalid limits, old-record exclusion, new-record inclusion, no deletion, and RunRecord/Workspace/CLI package regressions.

## 2026-07-16 - Preserve Durable CLI Outcomes Across Brain Reporting

- Preflighted decision, justification, artifact, and graph bounds before evidence creation and Tool execution.
- Collapsed required-document target duplicates into one target-preferred artifact and aligned graph/source identifier bounds at 1024 characters.
- Made post-persist Project Brain reporting degradable with explicit bounded status while preserving the RunRecord-derived exit code.
- Verified required-document targets, pre-persist malformed-decision rejection, and injected post-persist reporting failure across focused CLI and Brain regressions.

## 2026-07-16 - Eliminate Git Observer Command Execution Vectors

- Replaced unqualified Git lookup with canonical absolute PATH resolution that rejects project-contained executables.
- Reduced Git observation to filter-free index/untracked/deleted metadata and made tracked worktree diff explicitly unavailable after adversarial tests proved status, modified-file, and raw-diff paths can execute required clean filters.
- Verified the focused 8-test collector suite and the complete Workspace package with no failures.

## 2026-07-16 - Bound Gate 7 Pending Publications

- Added immutable `BackpressurePolicy` with a finite 1-4096 pending-publication capacity and preserved the existing bus constructors through a finite default.
- Added scope-level `BACKPRESSURED` refusal without blocking or journal, handler, idempotency, dead-letter, or cancellation side effects; refused work remains explicitly retryable.
- Applied deterministic prefix admission to replay while keeping replay cascades non-journaling.
- Verified test-first with 34 focused bus tests, the 189-test full regression, and Java 17 strict lint over all 115 production sources.

## 2026-07-16 - Synchronize PR #3 And Current Repository State

- Verified local `main` and `origin/main` already match PR #3 merge commit `52987f2`; no checkout or pull was necessary.
- Replaced stale current-state claims that retry, cancellation, and ordering were uncommitted or that `e74be87` was the published tip.
- Aligned canonical and compact guidance with Gate 6 Integrated, Gate 7 Specified - Next, current delivery and Git boundaries, and backpressure next.

## 2026-07-16 - Harden Git Workspace Observation Authority

- Removed inherited `GIT_*` overrides from the two authorized Git child processes while preserving unrelated process environment.
- Disabled external diff and text-conversion helpers explicitly without adding another command or widening authority.
- Added focused security regression coverage and passed the 186-test full suite plus Java 17 strict lint.

## 2026-07-16 - Correct Gate 7 Replay Cascades

- Reproduced and fixed replay-caused publications appending to the live journal.
- Routed every publication through drain-owned admission so cancelled re-entrant work remains visible in the ordered cascade without delivery or journaling.
- Added focused regression coverage and passed the 183-test full suite plus Java 17 strict lint.

## 2026-07-16

- Added Contract Verified Gate 7 run-to-completion delivery ordering on `InProcessMessageBus`: a pending queue and a single drain loop replace nested dispatch, so a publication made from inside a handler is queued and reports the new scope-level `ENQUEUED` status while the draining top-level `publish` or `replay` returns the whole ordered cascade; delivery order now equals publication order and no subscriber observes an effect before its cause. Admission and journaling moved into the drain loop, so the journal's order is the bus's own delivery order and a correlation cancelled mid-cascade refuses entries queued behind it while an in-flight fan-out stays atomic; an `Error` abandons the cascade entirely.
- Proved the ordering defect was real before fixing it: with only the two missing symbols added so the suite could run, the focused tests observed `[first, child, second]` where `[first, second, child]` was required, confirming a cascaded child really was delivered inside its parent's fan-out.
- Added `DeliveryStatus.isScopeLevel()` covering `UNROUTED`, `CANCELLED`, and `ENQUEUED`, and reduced the `DeliveryOutcome` invariant to it.
- Verified delivery ordering test-first with 8 expected RED errors naming only the absent `ENQUEUED` constant and `isScopeLevel` accessor, then a behavioural RED pass, then 25 focused `InProcessMessageBusTest` tests, the full 181-test regression with only the 2 existing Windows symbolic-link setup skips, and Java 17 `-Xlint:all -Werror` over all 114 production sources.
- Added Contract Verified Gate 7 cancellation propagation on `InProcessMessageBus`: `cancel(correlationId)` is idempotent and monotonic with no resume, and a cancelled correlation is refused admission before subscription lookup, idempotency, and dispatch on every path — publish, replay, and dead-letter re-delivery — reporting a scope-level `CANCELLED` outcome that names no subscription, invoking no handler, consuming no idempotency key, creating no dead letter, and appending nothing to the journal so replay stays deterministic; cancellation dominates both `UNROUTED` and `DUPLICATE`, and the bus reads no payload to decide delivery, keeping `ControlSignal.CANCEL` a consumer semantic a handler may act on by calling `cancel` itself.
- Generalized the `DeliveryOutcome` invariant to "a scope-level status (`UNROUTED` or `CANCELLED`) carries no subscriberId; every other status must name the subscription it targeted".
- Verified cancellation propagation test-first with 19 expected RED errors naming only the absent `CANCELLED` constant, `cancel`, and `isCancelled`, then 20 focused `InProcessMessageBusTest` tests, the full 176-test regression with only the 2 existing Windows symbolic-link setup skips, and Java 17 `-Xlint:all -Werror` over all 114 production sources.
- Added Contract Verified Gate 7 bounded synchronous retry and explicit dead-letter re-delivery on `InProcessMessageBus`: an immutable `RetryPolicy` (1-10 attempts; the no-argument bus constructor keeps a single attempt) retries a failing handler immediately and with no delay until the policy is exhausted, `DeadLetter` now records the failed attempt count, and `redeliver` accepts only a currently recorded dead letter, resolves it on success, and on renewed exhaustion replaces it in place with the accumulated attempt count and latest reason — never appending to the journal or releasing the consumed idempotency key, so publish and replay still report `DUPLICATE`.
- Verified the retry and re-delivery contract test-first with 16 expected RED errors naming only the absent `RetryPolicy`, policy constructor, `redeliver` operation, and `DeadLetter` attempt count, then 15 focused `InProcessMessageBusTest` tests, the full 171-test regression with only the 2 existing Windows symbolic-link setup skips, and Java 17 `-Xlint:all -Werror` over all 114 production sources.

## 2026-07-15

- Added Contract Verified Gate 7 delivery-failure isolation and dead-letter capture on `InProcessMessageBus`: a subscriber handler that throws a `RuntimeException` yields a typed `FAILED` outcome and an ordered immutable `DeadLetter` (destination, subscriber, unmodified envelope, bounded reason) while fan-out continues to the remaining subscribers; a failed delivery consumes the idempotency key and is terminal, so re-publishing or replaying it reports `DUPLICATE` and adds no further dead letter, deferring automatic retry and re-delivery to a later increment.
- Verified the failure handling test-first with 8 expected RED errors naming only the absent `FAILED` constant, `DeadLetter` type, and `deadLetters()` accessor, then 10 focused `InProcessMessageBusTest` tests, the full 166-test regression with only the 2 existing Windows symbolic-link setup skips, and Java 17 `-Xlint:all -Werror` over all 113 production sources.
- Added the Contract Verified Gate 7 in-process delivery surface `InProcessMessageBus` under `com.enhancer.bus`: deterministic synchronous topic fan-out in registration order and single-consumer queue delivery over `MessageEnvelope`, typed `DeliveryOutcome`/`DeliveryStatus` (`DELIVERED`/`DUPLICATE`/`UNROUTED`) results, per-`(destination, subscriber, message identity)` idempotency, and an ordered immutable journal supporting deterministic replay without duplicate side effects; authorization and provenance survive every hop and no retry, dead-letter, ordering, backpressure, threading, persistence, or transport was introduced.
- Verified the delivery surface test-first with 54 expected RED missing-symbol errors naming only the five absent delivery types, then 7 focused `InProcessMessageBusTest` tests, the full 163-test regression with only the 2 existing Windows symbolic-link setup skips, and Java 17 `-Xlint:all -Werror` over all 112 production sources.
- Fixed a pre-existing wall-clock-dependent defect in `RunRecordMetadataCollectorTest` (unrelated to the delivery increment): the test hardcoded the observation time to `2026-07-15T10:01:00Z` while `persist()` stamps `storedAt` with `Instant.now()`, so an AVAILABLE record's stored time fell after the fixed observation time and its `sourceUpdatedAt` was dropped as future, failing whenever the wall clock passed 10:01 UTC; the observation time is now derived from the run clock, making the test time-independent.
- Published the Gate 6 assessment and promotion and the Gate 7 envelope contract to `origin/main` in delivery commit `3423201`.
- Added the Contract Verified Gate 7 `MessageEnvelope` contract: versioned reference-only envelopes with canonical message/causation identities, bounded correlation/run/producer identities, and a sealed four-kind payload hierarchy carrying task revisions, snapshot identities, authorization scopes, run-record references, verification status, and control signals as data.
- Verified the envelope contract test-first with 38 expected RED missing-symbol errors, then 4 focused tests and the full 156-test regression with only the 2 existing Windows symbolic-link setup skips.
- Promoted Delivery Gate 6 to Integrated through the user-approved re-scope decision: diagnostics, terminal-session, and active/selected-file observation moved to Gate 12, which owns those capabilities, and Delivery Gate 7 Event Bus and IPC Foundation became the sole `Specified - Next` product gate.
- Updated the two actual-roadmap test contracts to Gate 7 and verified the full 152-test regression with the marker at Gate 7 and no production change.
- Recorded the Gate 6 maturity assessment against fresh 152-test evidence: all evidenced scope items and exit criteria named, diagnostics/terminal/selection blockers traced to Gate 8-12 capabilities, and the re-scope-and-promote recommendation recorded pending explicit user approval.
- Published the authority-boundary evidence, target-file observation, and Git adapter increments to `origin/main` in delivery commit `21e6230`.
- Added the `GitWorkspaceCollector` under explicitly granted read-only external command authority: two fixed git invocations (status/diff) with discovery confined to the project root, watchdog-enforced timeout, discarded stderr, `--no-optional-locks` and fsmonitor disabled, digest-only retention, and every failure surfaced as explicit `UNAVAILABLE`.
- Caught and fixed a real semantic defect during GREEN: without a discovery ceiling, temporary directories observed the enclosing repository; `GIT_CEILING_DIRECTORIES` now confines observation to the project's own working tree.
- Verified the Git adapter through 62 focused tests, the full 152-test regression, and an actual-repository run observing 23 sources including 2 `AVAILABLE` Git observations.
- Added the `TargetFileMetadataCollector`: the governed run's target file is observed pre-run as a `REPOSITORY_FILE` snapshot observation with a real streamed containment-checked SHA-256, missing/oversized targets surface as explicit `UNAVAILABLE`, and containment violations fail early as usage errors; verified test-first through 59 focused tests, the full 149-test regression, and an actual-repository run observing README.md.
- Pinned the Gate 6 authority boundary with `WorkspaceAuthorityBoundaryIntegrationTest`, passing on first run: adversarial tool-grant text in observed documents cannot widen the persisted task or policy scope, appear in bounded output, or mutate any repository document, and a task without `read-file` stays rejected; full 146-test regression passed with no production change.
- Published the justification-reference increment to `origin/main` in delivery commit `0e2be2c`.
- Adopted the optional `Justified By` task-document section and the `TaskJustificationProjector`: explicit references to accepted-decision headings become `JUSTIFIED_BY` edges with task-document provenance, strict rejection of malformed or unresolved references, and a bounded `impactDecisions` count on the production run output.
- Resolved the first real justification on the actual repository: this increment's own task document reference surfaced as `impactDecisions=1` with 46 decision nodes and 18 observations.
- Verified the reference grammar test-first with 6 expected RED missing-symbol errors, then 54 focused tests and the full 144-test regression with only the 2 existing Windows symbolic-link setup skips.
- Promoted all six Contract Verified Gate 6 sub-capabilities to Integrated through a documentation-only audit: `WorkspaceSnapshot`, `ProjectBrainView`, the graph projection contract, `TaskImpactQuery`, `AcceptedDecisionProjector`, and `RunRecordMetadataCollector`, each mapped to named pre-existing integration evidence re-run fresh (59 focused tests, full 140-test regression, no failures).
- Kept Delivery Gate 6 `Specified - Next`; gate-level promotion still requires the reference grammar, remaining producers and adapters, and full exit-criteria evidence.
- Published the run-evidence producer, decision projection, run-record observation, and production graph composition increments to `origin/main` in delivery commit `396665b`.
- Composed the Project Brain graph on the production CLI `run` path: prior run records observed into the snapshot, accepted-decision nodes merged into the run-evidence graph, and the task impact query answered in process, reporting bounded `graphNodes`, `graphEdges`, `graphDecisions`, and `impactExecutions` counts.
- Promoted the production graph composition to Operational with an actual-repository run: 17 observations including 2 prior run records, 61 graph nodes, and 44 decision nodes matching the decision log's 44 accepted decisions exactly.
- Verified the composition test-first (expected missing-output RED, 50 focused tests GREEN) and passed the full 140-test regression with only the 2 existing Windows symbolic-link setup skips.
- Added the Contract Verified `RunRecordMetadataCollector` and a read-only lexicographically ordered `references()` listing on the RunRecord store: one `RUN_RECORD` observation per stored record with the envelope SHA-256 and stored time, and explicit `UNAVAILABLE` observations with bounded reasons for corrupted or missing records.
- Verified the observation path test-first with 8 expected RED missing-symbol errors, then 33 focused tests and the full 139-test regression with only the 2 existing Windows symbolic-link setup skips.
- Added the Contract Verified `AcceptedDecisionProjector`: accepted decisions parsed from the decision log's own `Status: Accepted Decision` lines into unlinked `DECISION` nodes with snapshot-relative freshness (matched digest Current, diverged or unobserved Stale) and no invented edges.
- Verified the projector test-first with 6 expected RED missing-symbol errors, then 20 focused Project Brain tests and the full 134-test regression with only the 2 existing Windows symbolic-link setup skips.
- Added the first Project Brain graph producer: `RunEvidenceGraphProducer` projects one task node, observed repository artifacts with one-to-one state-to-freshness mapping, one execution node with the stored envelope SHA-256, and one `RECORDED_AS` edge from one snapshot and one task-matched run record.
- Integrated the run-evidence production path end to end: a real governed CLI run and really-collected snapshot flow through the producer into a `TaskImpactQuery` answer naming the real stored execution.
- Refused unjustified projection by decision: no decision, modifies, verified-by, justified-by, supersedes, or depends-on elements are emitted until their evidence sources exist.
- Verified the producer test-first with 6 expected RED missing-symbol errors, then 18 focused tests and the full 130-test regression with only the 2 existing Windows symbolic-link setup skips.
- Published the five Gate 6 project brain foundation increments to `origin/main` in delivery commit `d3b6197`.
- Added the Contract Verified Gate 6 task impact query: `TaskImpactQuery` answers the first rebuildable task-to-decision-to-code-to-test chain over one projected graph with snapshot-traceable immutable results.
- Derived one rebuild-required status from every traversed node and edge so unrelated staleness does not taint the answer, deduplicated shared verifying artifacts, and rejected unknown or non-task identities.
- Verified the query test-first with 9 expected RED missing-symbol errors, then 13 focused tests and the full 127-test regression with only the 2 existing Windows symbolic-link setup skips.
- Deferred transitive `DEPENDS_ON` closure, graph producers, and persistence by recorded decision; the next producer increment gives the query real project evidence.
- Added the Contract Verified Gate 6 graph projection contract: five typed node kinds, six endpoint-checked edge kinds over the Decision, Architecture, Dependency, Task, and Execution relationship domains, and immutable element provenance with source, optional SHA-256 revision, explicit freshness, and derived rebuild status.
- Keyed each `ProjectBrainGraph` projection to one Workspace snapshot identity with an explicit projection time and version, deterministic ordering, and duplicate/self-loop/unknown-endpoint/bound rejection.
- Verified the graph contract test-first with 100 expected RED missing-symbol errors, then 9 focused tests and the full 123-test regression with only the 2 existing Windows symbolic-link setup skips.
- Named the task-to-decision-to-code-to-test impact query as the contract's consumer and kept Gate 6 `Specified - Next` with no producer, query, or persistence.
- Composed the `ProjectBrainView` on the production CLI `run` path from the already-loaded repository memory, the collected snapshot, and the persisted RunRecord, for every outcome that produces a record.
- Reported bounded `workspaceSnapshotId`, `workspaceObservations`, and `memoryFreshness` metadata in the run output without changing commands, arguments, exit codes, the RunRecord schema, or replay.
- Promoted the production repository-memory composition to Operational with an actual-repository run (`README.md`, exit code 0, snapshot identity, 15 matched documents) and its unchanged replay.
- Verified the composition test-first (expected missing-output RED, 29 focused tests GREEN) and passed the full 119-test regression with only the 2 existing Windows symbolic-link setup skips.
- Added the first Workspace source adapter: the read-only `RepositoryMemorySnapshotCollector` deriving a real `WorkspaceSnapshot` from Context Reader repository memory with computed digests, `context-reader` provenance, and an `ApprovedTaskRevision` digested from the same memory.
- Integrated the Gate 6 repository-memory path end to end: a real governed CLI run, its persisted RunRecord, really-loaded repository memory, the collector, and the composed `ProjectBrainView` with all documents `SNAPSHOT_MATCHED` and exact `SNAPSHOT_DIVERGED` detection after the active task document changed.
- Verified the collector test-first with 6 expected RED missing-symbol errors, then 20 focused tests and the full 117-test regression with only the 2 existing Windows symbolic-link setup skips.
- Kept Gate 6 `Specified - Next`: no production caller composes the view during an actual run, and Git, diagnostics, selection, and terminal adapters plus graph projections remain unimplemented.
- Added the read-only Gate 6 `ProjectBrainView` aggregate under `com.enhancer.brain`, giving the Contract Verified `WorkspaceSnapshot` its first consumer.
- Composed the view from one real Workspace snapshot, one real repository-memory `ProjectContext`, and one real `RunRecord`, keyed to the existing canonical snapshot identity.
- Projected repository memory to path, read order, and computed SHA-256 with explicit `SNAPSHOT_MATCHED`, `SNAPSHOT_DIVERGED`, and `NOT_OBSERVED` freshness, retaining no document content.
- Projected RunRecords to logical run identity, record time, approved task identity, and verification status, excluding Tool payloads, evidence bodies, and chat history.
- Rejected runs whose approved task identity or source document does not match the snapshot revision, so the aggregate cannot misattribute provenance.
- Verified the aggregate test-first with 19 expected RED missing-symbol errors, then 15 focused tests and the full 113-test regression with only the 2 existing Windows symbolic-link setup skips.
- Recorded `ProjectBrainView` as Contract Verified and kept Gate 6 `Specified - Next`, because no adapter collects a live snapshot and no production path composes the view.
- Published the Contract Verified Gate 6 WorkspaceSnapshot contract and synchronized project memory to `origin/main` in delivery commit `c5a16b9`.
- Added the first Delivery Gate 6 contract: immutable metadata-only `WorkspaceSnapshot`, approved-task revision provenance, typed source observations, explicit freshness/availability, deterministic ordering, bounded metadata, and canonical SHA-256 identity.
- Verified the Workspace contract test-first with 10 focused tests, then passed the 108-test full regression and Java 17 warning-as-error production lint without promoting Gate 6 beyond `Specified - Next`.
- Recorded the Workspace snapshot sub-capability as Contract Verified and selected a minimal read-only `ProjectBrainView` as its next integration consumer.
- Published the Operational Gate 5 CLI, Integrated Gate 0 lifecycle evidence, and RED workflow clarification to `origin/main` in commit `ed901f3`.
- Promoted Delivery Gate 0 Foundation Safety Contracts from Contract Verified to Integrated through a new authority-preserving lifecycle characterization test.
- Proved Proposal non-mutation, pre-activation rejection, external-only task activation, verified Gate 5 execution, persist-before-completion RunRecord storage, and replay after target deletion.
- The Gate 0 lifecycle test passed on its first run, so no production correction or second orchestrator was added.
- Verified 43 focused tests and the full 98-test regression suite while preserving Gate 6 as the sole `Specified - Next` product gate.
- Prepared the authority-preserving Gate 0 integration-promotion task, accepted decision, lifecycle test contract, and verification plan without changing Gate 0 maturity.
- Kept Gate 6 as the sole `Specified - Next` product gate and prohibited automatic Proposal approval or a second production orchestrator in the Gate 0 audit.
- Made Delivery Gate 5 Operational with the supported local `EnhancerCli` `run` and `replay` commands.
- Added explicit governed inputs, stable outcome exit codes, bounded diagnostics, verified-only completion, and persist-before-report RunRecord behavior.
- Added test-first CLI parsing, exit-code, temporary-project, mismatch, Tool-failure persistence, and restart-safe replay coverage.
- Verified 7 focused CLI tests and the full 97-test regression suite, with only 2 existing Windows symbolic-link setup skips.
- Completed and replayed an actual-repository `README.md` run with a Verified decision and exit code 0.
- Promoted Delivery Gate 6 Workspace and Project Brain Foundation to the sole `Specified - Next` gate.
- Clarified the RED-to-GREEN workflow: expected missing implementation proceeds when the test contract matches the active task, accepted decisions, Architecture, and repository settings; unrelated or authority-expanding failures are separated.
- Published the Gradle 9 and integrated execution-foundation hardening to `origin/main` in commit `b504ba4`.
- Removed the Gradle 9 automatic-test-framework dependency deprecation by declaring the JUnit Platform Launcher explicitly.
- Made Gradle tests use a workspace-local default temporary directory with an explicit `testTmpDir` override.
- Isolated Tool invocation workers so an interrupt-ignoring timed-out Tool cannot starve later work, and bounded timeout values to consistent execution/audit representations.
- Extended Evidence and RunRecord integrity digests across envelope version, timestamp, declared length, and payload/content metadata.
- Rejected malformed RunRecord Unicode instead of replacing it and added strict, bounded, real-root-contained startup context loading.
- Corrected oversized no-persistence read failure classification and removed all production Java serialization lint warnings.
- Added regression coverage for starvation, timeout edge values, metadata tampering, Unicode loss, startup-document size/encoding/containment, and evidence-capability errors.
- Fast-forwarded the verified Gate 4 and Agent orchestration delivery commit `f731afc` into `main` and published it to `origin/main`.
- Translated selected Archon `263cf365` and meta-harness `ccab9a6` orchestration patterns into provider-neutral Enhancer documentation without adding either repository as a dependency.
- Defined the escalation path from one worker through sequential, Producer-Reviewer, bounded fan-out/fan-in, routing or supervision, and shallow hierarchy only when justified.
- Added immutable common snapshots, typed handoffs, single terminal-state ownership, dependency and lease controls, idempotency, replay, bounded budgets, diagnostic-only telemetry, and verified-only completion invariants.
- Assigned adopted contracts to Delivery Gates 6 through 15 and expanded the Planned Gate 13 scope without changing capability maturity or displacing Gate 5 First Operational CLI.
- Added explicit rejected patterns covering prompt/file-based authority, direct peer control, shared-worktree parallel mutation, optional verification, self-reported completion, unlimited execution, and silent evidence loss.
- Verified both pinned GitHub reference links, canonical Roadmap structure, Planner behavior, and the full 82-test regression suite with the existing Windows symbolic-link setup skip.

## 2026-07-14

- Bound the exact Tool execution policy to the governed Agent run result and removed replaceable policy input from finalization.
- Restricted `AgentRunResult` construction and hardened RunRecord lifecycle invariants against impossible or historically false audit records.
- Integrated Delivery Gate 4 sequential independent verification and durable RunRecord replay.
- Added typed verification status/reason contracts and deterministic complete-content read verification over inline or referenced evidence.
- Preserved executed requests across terminal Agent state and allowed only verified finalization to create `COMPLETED`.
- Added typed policy snapshots and RunRecords with external expected digests, iterations, evidence, decisions, and worker/final stop reasons.
- Added atomic versioned SHA-256 RunRecord envelopes, strict UTF-8 replay, and missing/corruption detection.
- Added persist-before-return completion gating and durable records for failed, stagnated, and maximum-iteration runs.
- Promoted Delivery Gate 5 First Operational CLI to `Specified - Next`.
- Published the Gate 1-3 governed Agent execution foundation, self-hosting recovery, long-term vision, and documentation alignment to `origin/main` in commit `3fcda4c`.
- Corrected Gate 3 evidence wording to distinguish the governed temporary-repository integration test from separate actual-Enhancer Context and Planner regressions.
- Separated current build dependencies from planned Spring Boot, local-model, CLI, and editor integrations.
- Clarified product milestones versus dependency-ordered Delivery Gates and distinguished self-hosting development from local or hybrid model execution.
- Synchronized the project-overview startup order with the required `.ai/`-first bootstrap sequence and replaced stale foundation checklist states.
- Added V1 development-experience, V2 Agent-platform, and V3 AI-OS product milestones.
- Defined AI Kernel responsibilities and provenance-preserving Project Brain Decision, Architecture, Dependency, Task, and Execution graphs.
- Distinguished Agent plugins, Skills, Tools, and workflows and added marketplace and workflow approval boundaries.
- Added privacy-aware local/remote Model Router direction with sensitive-code-local defaults.
- Accepted the event-driven Enhancer OS target with Workspace, Project Brain, messaging/IPC, Agent Runtime, MCP, Skills, plugins, model routing, scheduling, interfaces, and governed Cloud Sync.
- Reordered future delivery gates so verification and the first CLI precede Workspace, Event Bus, runtime, MCP/model, Skill, plugin, interface, multi-agent, sync, and self-improvement tracks.
- Defined Event Bus semantics, Message Bus delivery, IPC transport separation, and queue-only Agent collaboration as the target architecture.
- Hardened Gate 3 with repository-derived `ApprovedTask` identity, explicit approval evidence, and Tool-name scope.
- Added structured Tool failure codes and a standard retry policy for timeouts and explicit temporary failures.
- Added evidence content digests and semantic progress detection independent of opaque references and prose summaries.
- Restricted `AgentRunState` construction to governed factories and controller-owned transitions.
- Integrated Delivery Gate 3 with approved Agent run state, Tool-result-driven transitions, and shared bounded termination behavior.
- Added `AWAITING_VERIFICATION` so Tool success cannot claim completion before the sequential independent verifier.
- Added external retry/terminal classification and canonical progress fingerprints for deterministic stagnation detection.
- Added governed temporary-repository Context-to-ReadFileTool-to-evidence-to-loop coverage plus terminal-failure and denied-mutation safeguards.
- Integrated Delivery Gate 2 with UUID evidence identities, atomic versioned filesystem envelopes, restart-safe resolution, and SHA-256/length/UTF-8 integrity checks.
- Added explicit maximum-content and retention-duration policy without automatic cleanup or destructive deletion.
- Connected large read-only Tool output to bounded `VerificationEvidence` and a resolvable complete-output reference through `EvidenceRecorder`.
- Added focused persistence, uniqueness, missing, corruption, invalid-encoding, and real large-file integration tests.
- Restored self-hosting planning compatibility by replacing the retired Phase/Ready parser with the canonical Delivery Gate/Specified - Next grammar.
- Added actual Enhancer Roadmap regression coverage and mapped gate scope and exit criteria into structured task proposals.
- Expanded executable startup context from eight root documents to seven `.ai/` documents followed by eight canonical root documents.
- Integrated Delivery Gate 1 with immutable Tool requests, execution policy, cancellation, timeout, a unique Tool registry, and bounded structured failure conversion.
- Added a real UTF-8 read-only file Tool with normalized and real-path containment, size limits, strict decoding, and no filesystem mutation.
- Added test-first request, policy, executor, and temporary-file integration coverage and promoted Gate 2 evidence persistence to the next task.
- Replaced ambiguous implementation roadmap labels with capability maturity and 12 dependency-ordered delivery gates.
- Redirected the next product task from an isolated verifier contract to a real read-only Tool Execution Boundary and E2E promotion track.
- Added explicit integration consumers, evidence, and exit criteria for post-foundation contracts.
- Published the delivery-gate roadmap realignment to draft PR #2.
- Published the governed Agent Loop foundation on `agent/governed-agent-loop-foundations` and opened draft PR #2.
- Restored Git metadata for the active C:\Enhancer worktree from a validated no-checkout clone without changing working files.
- Reconstructed the Git index from HEAD, verified repository identity and object integrity, and confirmed all 1,479 non-.git files remained byte-identical.
- Replaced Constitution 1.0.0 with a deduplicated, versioned 1.1.0 normative Kernel.
- Added explicit lifecycle states, scoped authorization, fresh-evidence rules, self-hosting safeguards, recovery requirements, and protected semantic-versioned amendments.
- Delegated detailed technology and component guidance to Architecture and RFCs and synchronized Agent, `.ai`, RFC-0001, and session-prompt rules.
- Verified all 15 required Constitution sections, confirmed obsolete implementation details are absent, and reran all 25 product tests successfully.
- Removed the standalone `examples/` directory and consolidated conceptual examples into specifications and executable examples into tests.
- Updated Constitution, README, architecture, roadmap, decision, state, and handoff documents for the smaller repository structure.
- Verified the unchanged product code with all 25 tests passing after removal.
- Implemented bounded Tool result verification evidence without real Tool execution.
- Added 512-character summaries, 4096-character diagnostic tails, truncation metadata, and complete-output references.
- Added explicit Tool success/failure status with optional exit-code consistency rules.
- Added 8 Tool contract tests and verified all 25 repository tests with no failures, errors, or skips.
- Confirmed the selected external agent-harness patterns are compatible with `.ai/` under staged, provider-neutral adoption.
- Added the ordered pattern adoption plan, including a sequential independent verifier, to the roadmap and decision log.
- Implemented bounded repeated Agent Loop termination with completed, failed, maximum-iteration, and stagnated reasons.
- Added immutable loop state/result contracts, configurable 20/3 defaults, deterministic termination precedence, and invariants.
- Added 9 Agent Loop tests and verified all 17 repository tests with no failures, errors, or skips.
- Accepted selective, provider-neutral adoption of high-value MoAI-ADK patterns without adding it as a dependency.
- Implemented a deterministic, read-only Assisted Development Loop that composes context reading and task planning once.
- Added explicit proposal-available and active-task-preserved outcomes with result invariants.
- Added 3 loop tests and verified all 8 repository tests with no failures, errors, or skips.
- Staged repeated-loop termination, verification evidence, Skill loading, artifact provenance, token budgets, and self-improvement for their owning roadmap slices.
- Added a Gradle 8.4 Wrapper and project-local Microsoft OpenJDK 17 setup workflow.
- Added PowerShell setup and Gradle launcher scripts for reproducible Windows builds.
- Verified compilation and all 5 tests through the Wrapper.
- Accepted repository Skill authoring and least-privilege permission rules.
- Added memory distillation, test-first scope, and fresh verification evidence requirements.
- Added a Proposed-only Skill catalog without activating unimplemented Skills.
- Synchronized `.ai`, session prompts, README, architecture chapters, and related RFCs.
- Clarified that verification cycles do not require automatic commits.

## 2026-07-12

- Implemented the deterministic Repository Task Planner.
- Added structured task proposals with explicit proposal state, scope, acceptance criteria, exclusions, and risks.
- Added Planner tests for ready roadmap selection, active-task protection, and incomplete roadmap risk reporting.
- Added the Java 17 Gradle project structure for the first product slice.
- Added Gradle build output exclusions.
- Implemented the Repository Context Reader with ordered UTF-8 document loading and clear missing-document errors.
- Added JUnit 5 tests for successful context reads and missing required documents.
- Added `.editorconfig` to declare UTF-8 repository text encoding.
- Added `.gitattributes` to keep repository text normalization stable.
- Replaced the `.ai/workflow.md` Korean startup sentence with ASCII English to avoid console encoding display issues.

## 2026-07-10

- Added repository-backed project memory documents.
- Added Codex session prompt templates.
- Established source-of-truth and session handoff rules.
- Added self-hosting AI Development Operating System vision.
- Added 30-day milestone for repository-context-based task proposal.
- Created required repository folders: `docs/`, `examples/`, `.ai/`, and `src/`.
- Added AI-only operating documents under `.ai/`.
- Defined the first self-hosting implementation task: Repository Context Reader.
- Added Document Driven Development workflow as the project operating process.
- Added Codex-ready feature specification documents under `docs/`.
- Added shared coding, architecture, and review prompts.
- Added Agent Loop, Tool, and Skill concept examples.
- Created initial local Git commit for project memory bootstrap.
- Added open source operating model and long-running Sprint-based project direction.
- Added explicit ChatGPT session resume protocol and prompt.
- Added Prompt Book sections for Codex, Claude, and GPT to chapter documents.
- Renamed local branch to `main` and configured GitHub remote `origin`.
- Pushed `main` to `origin/main`.
- Documented the startup rule: always read `.ai/` before starting work.
- Recorded the `.ai/` startup rule as an accepted decision.
- Added RFC-style design track under `docs/rfcs/`.
- Added six-month AI Development OS roadmap.
