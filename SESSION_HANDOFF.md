# Session Handoff

- Gate 8 in-process Scheduler worker (connection sub-increment 3a) is now Contract Verified: `DurableAgentRunWorker` (`com.enhancer.runtime`) drives one scheduling cycle end to end — claim + fenced lease through the existing dispatcher, execution through an injected `AgentRunExecution` port that persists a RunRecord and returns its reference, fence-checked `completeExecution`, then `finalizeAgentRun` recording the runtime terminal state and matching queue disposition — and recovers a cycle interrupted at any seam via a worker-owned durable single-record cycle-intent checkpoint (`PendingFinalization` in `FileSystemPendingFinalizationStore`).
- The cycle-intent is written before the queue claim with worker-generated distinct canonical Goal/AgentRun UUIDs, so a restarted worker re-supplies the same identities and the dispatcher's idempotent `recoverMatching` resumes the exact prefix (no second Goal, no orphaned runtime state, no dispatcher change); the `runRecordReference` persists into the intent before `completeExecution`, closing the pre-terminal recovery window the finalizer deferred. Recovery routes by runtime state: terminal -> `recoverFinalization`; `AWAITING_VERIFICATION` -> `finalizeAgentRun(ref)`; `EXECUTING`/`READY`/`PLANNING`, an unstarted AgentRun, or a missing runtime (`MissingAgentRuntimeStateException` tolerated) -> re-drive with the same identities, skipping re-execution when the reference exists. Failures fail closed with the intent retained; an empty-queue cycle clears its intent and leaves no durable trace; re-execution on retry orphans the earlier RunRecord (accepted at-least-once consequence).
- The wiring rule is pinned: the dispatcher and finalizer handed to the worker must wrap the same `DurableSingleWorkerSchedulerQueue` instance, because the queue's in-memory revision advances with each persisted mutation and a stale second instance fails the store's exactly-one revision advance. Out of scope and next: worker process isolation (3b), a concrete `MessageTransport` local IPC adapter (3c), and the real `AgentLoop`-backed execution port with its `WorkPayload` execution-input extension.
- Fresh worker evidence: Task-classified REDs (19 checkpoint compile errors, 5 worker compile errors, 6 behavioural recovery failures against the deliberate resume stub), focused GREENs of 8/8 checkpoint, 9/9 worker, and 2/2 filesystem end-to-end integration tests, and a 14-suite/73-test runtime package pass; full regression and strict lint recorded below. The earlier finalizer and queue-disposition increments were merged to `origin/main` through PR #4 and PR #5, and the worker design/plan documents through PR #6 (`eb8ad38`).
- Gate 8 RunRecord-backed result-path finalization is Contract Verified: `DurableAgentRunFinalizer` (`com.enhancer.runtime`) connects a resolved RunRecord to the runtime terminal state and the matching queue disposition in one recoverable, idempotent order (resolve RunRecord -> runtime `recordResult` -> queue disposition), deriving the disposition from the runtime terminal status (`COMPLETED -> completeActiveVerified`, `FAILED -> failActive`) so the two durable stores cannot diverge. It resolves but never persists the RunRecord, binds it to the Goal on `taskId` plus `sourceDocument`, and carries the RunRecord's `verificationStatus` in a deterministic `ResultPayload` keyed to the AgentRun identity.
- Two entry points: `finalizeAgentRun(goalId, agentRunId, runRecordReference)` drives the forward path; `recoverFinalization(goalId)` is autonomous post-terminal recovery that applies only the queue disposition and needs no reference. Because the durable queue's recovery contract requeues an interrupted active WorkItem to pending, the finalizer re-claims that requeued item before recording the disposition — the same claim-then-dispose pattern the queue's own recovery already mandates; a disposition already in the completed/failed set is a no-op.
- Guards fail closed: a missing/corrupt RunRecord records no disposition and leaves the run `AWAITING_VERIFICATION` (recoverable); a RunRecord bound to a different task, a re-finalize with a different reference, and a finalize before execution acknowledgement are all rejected. Queue and runtime remain separate durable boundaries with no cross-store transaction; only the pre-terminal window depends on the connection-3 worker/driver to re-supply the same reference.
- Fresh evidence: each behaviour proven test-first (missing `DurableAgentRunFinalizer`, then missing `recoverFinalization`), 8/8 focused `DurableAgentRunFinalizerTest` cases, full 60-suite/269-test regression (267 passed, 2 existing Windows symbolic-link skips, 0 failures, 0 errors), and Java 17 strict lint across 151 production sources. That work is merged to `origin/main` through PR #5.
- Gate 8 durable queue terminal disposition is now Contract Verified: a terminal `WorkItemDisposition` (`VERIFIED_COMPLETED`/`FAILED`) splits the queue's single completion into `completeActiveVerified` and `failActive` across the in-memory queue, durable wrapper, and filesystem store. Only verified completion enters `completedWorkItemIds`; failed work enters a disjoint `failedWorkItemIds` set and never satisfies dependents, so its dependents stay blocked with the cause held in the runtime/RunRecord.
- The schema-v1 partition is `pending + active + verified + failed`, revised in place with no version bump; the failed disposition persists with exact restart recovery (a persisted terminal disposition is never re-run), and because the on-disk envelope rejects trailing bytes any pre-existing local `.enhancer/` snapshot fails closed on read (accepted for the unreleased artifact).
- Fresh evidence: each contract proven test-first (missing enum, constructor arity, methods, dropped serialization), full 59-suite/261-test regression (259 passed, 2 existing Windows symbolic-link skips, 0 failures, 0 errors), Java 17 strict lint across 150 production sources. That work is merged to `origin/main` through PR #4.
- The Gate 8 connection backlog is now aligned with the implemented queue/runtime contracts and `.ai/` rules: fence-checked execution completion reaches `AWAITING_VERIFICATION` only and cannot directly complete the queue or satisfy dependencies.
- The next bounded Gate 8 contract is durable queue terminal disposition, distinguishing verified completion from failure before releasing Scheduler capacity or changing the dependency-satisfaction set.
- Remaining connections are ordered and gate-owned: RunRecord-backed result finalization; process-isolated worker/local IPC; durable controls; effect ledger/fencing; retry through additional AgentRuns; and Gate 13 typed handoff/multi-agent execution.
- The stale Roadmap RFC wording is corrected: existing bounded Gate 8 work remains valid, while detailed RFC work is still required before process workers, concrete IPC production wiring, broader Scheduler policy, or Operational promotion.
- This documentation alignment changes no production/test code, capability maturity, Constitution text, Agent operating rules, external authority, or release state.
- Fresh verification passed 24 focused actual-document tests (23 passed, 1 existing Windows symbolic-link skip) and the full 57-suite/251-test regression (249 passed, 2 existing skips), with no failure or error; structural/reference and whitespace checks also passed.

## Next-Session Design Brief: Why The Completion Conflict Happened

The conflict was caused by one word, `completion`, naming three different lifecycle facts that were implemented in separate increments:

1. Worker execution completion: the fenced owner has stopped executing and the runtime moves to `AWAITING_VERIFICATION`.
2. Verified runtime completion: an independently supported `ResultPayload` completes or fails the AgentRun and Goal.
3. Scheduler queue completion: `completeActive` adds the WorkItem to `completedWorkItemIds`, releases the active slot, and allows dependent work to become ready.

Each individual contract was internally consistent. The conflict appeared when the previous next-task sentence proposed connecting fact 1 directly to fact 3 without re-checking fact 2. That wording was written after the dispatch and lease increments, while queue completion still carried its earlier dependency-satisfaction meaning. The compact `.ai/architecture.md` described current contracts but did not contain an ordered connection backlog, so it did not expose the missing middle transition. A separate stale Roadmap sentence also still said Agent Runtime and Scheduler could not become active before detailed RFC work, even though bounded Gate 8 work was already Contract Verified or Integrated.

The unsafe interpretation is now rejected:

- `EXECUTING -> AWAITING_VERIFICATION` must not call `completeActive`.
- A worker acknowledgement must not add a WorkItem to the dependency-satisfaction set.
- Releasing capacity must not be represented as successful completion merely because the current queue has only pending, active, and completed states.

## Next-Session Design Choices

### Option A: Keep The Queue Item Active Through Verification

Current recommendation for the next bounded implementation.

Advantages:

- smallest change consistent with the current schema-v1 queue and single-worker design;
- preserves Verified-only completion without a new intermediate queue state;
- keeps crash recovery and cross-store ordering easier to prove;
- prevents another WorkItem from starting while the current result is unresolved, which is conservative and deterministic.

Cost:

- verification latency occupies the single Scheduler slot and reduces throughput;
- a slow or unavailable verifier blocks unrelated ready work in that queue.

### Option B: Add A Non-Terminal Awaiting-Verification Queue State

Potential later throughput improvement.

Advantages:

- releases the execution slot while verification proceeds;
- permits another independent WorkItem to execute without falsely satisfying dependencies.

Required additional design:

- a durable queue-state/schema change and recovery rules for the waiting set;
- separate execution and verification capacity limits/backpressure;
- ordering and fairness between pending execution and pending verification;
- cancellation, timeout, restart, and orphan behavior for both stages;
- a rule that waiting work remains outside the dependency-satisfaction set.

This is more scalable but is not the smallest safe next increment.

### Option C: Mark The Queue Completed At Execution Acknowledgement

Rejected.

It is simple and releases capacity, but it lets dependent work start before independent verification and makes a worker receipt equivalent to completion authority. That conflicts with the Constitution-backed verification model, existing Gate 3/4 behavior, Runtime AgentRun states, and Gate 8 Verified-only terminal contract.

## Recommended Implementation Order To Discuss

1. Add terminal queue dispositions such as verified-completed and failed, replacing the assumption that every released active item belongs to one success-only completed set.
2. Keep the active WorkItem occupied through verification for the first contract.
3. For the result path, persist and resolve the RunRecord first, produce the matching `ResultPayload`, persist AgentRun/Goal terminal state second, and persist the matching queue disposition last.
4. Make restart recovery idempotently finish the missing suffix of that order without claiming a cross-store transaction.
5. Decide failure dependency behavior explicitly. The safest initial rule is that failed work never satisfies a dependency and dependents remain blocked with an inspectable reason; automatic dependent failure or failure-tolerant dependencies require later policy.
6. Reconsider Option B only after the terminal-disposition and result paths are Contract Verified and verification throughput is a demonstrated bottleneck.

Questions for the next session:

- Should the first terminal-disposition contract keep the active slot through verification as recommended, or is concurrent execution during verification already a required user scenario?
- Should a failed dependency leave dependents blocked for user action, or durably propagate a failure disposition to them?
- Should the queue format advance to schema v2, or may the unreleased schema-v1 format be revised with an explicit local-artifact compatibility decision?
- What exact durable identity links one RunRecord, ResultPayload, AgentRun terminal transition, and queue disposition during recovery?
- Gate 8 now has an Integrated durable queue-to-AgentRun dispatch path: one active or ready exact WorkItem reaches Goal creation/recovery, named AgentRun planning/readiness, and fenced lease acquisition through persisted-prefix re-entry.
- Queue and runtime remain separate durable artifacts. Claim failure creates no runtime; later runtime failure leaves an intentional recoverable active claim, and repeated same-owner calls resume without renewing an existing lease.
- Fresh dispatch evidence includes 31/31 focused tests, a 57-suite/251-test full regression (249 passed, 2 existing Windows symbolic-link skips), and strict lint across 149 production sources.
- Post-document self-hosting and boundary verification passed 36 of 37 tests with 1 existing Windows symbolic-link setup skip; the task decision, sole Gate 8 next marker, and whitespace checks are consistent.
- The real filesystem integration requeues and reclaims the same WorkItem after restart and returns the exact existing Unicode-bearing lease; no worker, queue completion, result message, external effect, or cross-store transaction is added.
- Gate 8 fenced AgentRun ownership is now Contract Verified locally: acquisition from `READY` issues one bounded owner and persisted monotonic fence, renew/complete require the current unexpired lease, and recovery at expiry durably returns the run to `READY`.
- Lease acquisition, renewal, completion, and reclaim all persist before exposure; injected storage failures retain the prior revision and lease, and restart preserves an unexpired owner or reclaims exactly at expiry.
- Fresh lease evidence includes 68/68 focused runtime/bus/boundary tests, a 55-suite/243-test full regression (241 passed, 2 existing Windows symbolic-link skips), and strict lint across 147 production sources.
- Post-document self-hosting and boundary verification passed 31 of 32 tests with 1 existing Windows symbolic-link setup skip; the Gate 8 marker, task-to-decision reference, and whitespace checks are consistent.
- This adds no worker/Tool execution, retry, external-effect fencing, multi-process lock, distributed clock-skew protocol, schema migration, or parent-directory power-loss claim.
- Gate 8 now has a Contract Verified durable Goal/AgentRun lifecycle: one exact WorkItem-backed Goal, one forward-only schema-v1 AgentRun, matching typed terminal results, Verified-only completion, and explicit non-verified failure.
- Every lifecycle transition is persisted before exposure; the bounded strict-UTF-8 integrity-checked filesystem store restores exact WorkItem and result envelopes and fails closed on corrupt or unsupported state.
- Fresh evidence includes 63/63 focused runtime/bus/boundary tests, a 55-suite/238-test full regression (236 passed, 2 existing Windows symbolic-link skips), and strict lint across 146 production sources.
- The lifecycle core adds no retry, worker execution, external-effect record, RunRecord resolution, multi-process coordination, schema migration, or parent-directory power-loss claim; ownership and fencing are supplied only by the separate lease contract above.
- The loop/run/verification cycle is removed locally. Neutral verification values are in `kernel`, AgentRunFinalizer is in `application`, and a structural test enforces the acyclic dependency direction.
- Fresh package-boundary evidence includes 27/27 focused tests, a 53-suite/228-test full regression (226 passed, 2 existing skips), and strict lint across 135 production sources.
- RunRecord schema, serialized enum names, verification behavior, CLI behavior, and replay compatibility are unchanged.
- ToolExecutor now has finite process-wide containment: 64 live isolated workers, real-thread-lifetime slot accounting, and typed terminal refusal before new thread creation when full.
- Fresh Tool-capacity evidence includes 41 affected passes plus 1 existing symbolic-link skip, a 52-suite/227-test full regression (225 passed, 2 skips), and strict lint across 134 production sources.
- This does not kill stuck code. Permanently stuck workers consume capacity until process restart; process isolation remains required before long-running Scheduler Tool workers.
- Unicode truncation and mutable-file TOCTOU bounds are corrected locally: supplementary characters are never split at bounded text edges, and governed reads/hashes enforce limits during stream consumption with at most one overflow byte.
- Fresh safety evidence includes 18/18 focused tests, 54 affected integration passes plus 2 existing symbolic-link skips, a 52-suite/226-test full regression (224 passed, 2 skips), and strict lint across 133 production sources.
- The review findings now stand as follows: Unicode truncation and file-growth bounds are fixed; stuck Tool accumulation is finitely contained but not terminable; the package cycle is removed; atomic stores still do not fsync parent directories and make no power-loss claim.
- Gate 8 durable queue state and restart recovery are now Contract Verified locally: schema-v1 snapshots preserve the exact WorkItem/envelope graph, every transition persists before exposure, and interrupted active work is durably requeued in admission order under at-least-once semantics.
- The filesystem queue store is strict-UTF-8, integrity checked, atomically published, revision checked, and bounded to 64 MiB; it fails closed on missing, corrupt, oversized, trailing, structurally invalid, or unsupported state.
- Fresh durable-queue evidence includes 14/14 focused tests, a 50-suite/219-test full regression (217 passed, 2 existing symbolic-link skips), and Java 17 strict lint across 130 production sources.
- Atomic publication does not include parent-directory fsync and therefore makes no power-loss durability claim. Leases, fencing, workers, external-effect idempotency, and migration beyond schema v1 remain absent.
- Gate 8 now has a Contract Verified dependency-ready single-worker queue: up to 256 earlier-admitted dependencies per work item, 4096 admissions per run-scoped queue, FIFO readiness, one active slot, and explicit matching completion.
- The base queue is in-memory and adds no lease, retry/cancellation, worker execution, or authority; the separate durable wrapper now supplies schema-v1 persistence and restart recovery.
- The fresh Gate 7 Integrated maturity assessment retained Gate 7 at Contract Verified. Only the real work-message queue/journal/replay/idempotency path is Integrated; the other payload, topic, reliability, causation, and transport branches lack named production connections.
- Fresh assessment evidence includes 42/42 focused tests, the 47-suite/208-test full regression (206 passed, 2 existing symbolic-link skips), and strict Java 17 lint across 122 production sources.
- Commit `1151fc5` delivers the Gate 7 assessment, Gate 8 queue and durable queue-state increments, Unicode/file-bound correction, bounded Tool-isolation capacity, and runtime package-cycle extraction.
- The cross-cutting Product Journey and Evaluation Track is now specified with four canonical journeys and a fifth priority for a versioned evaluation/release-quality harness; it changes no Delivery Gate maturity.
- Scheduler reliability now truthfully specifies at-least-once delivery plus idempotency, fenced leases, checkpoints, state migration, orphan reclamation, and replay-safe/compensatable effects rather than universal exactly-once execution.
- Interfaces now share one Run/approval/verification/evidence/control API with CLI first, VS Code second, and Desktop later; Gate 12 owns one change-centered review projection.
- The layered default-security model treats repository, Tool/terminal, model, MCP, plugin, dependency, and generated content as untrusted data and assigns concrete enforcement to the owning gates without amending the Constitution.
- Gate 7 now has one named Integrated work-message path: a real repository-approved task and Gate 6 Workspace snapshot flow through `WorkMessagePublisher`, the real in-process queue/journal/replay path, and `WorkItemAdmissionHandler` into the unchanged Gate 8 `WorkItem`.
- Gate 7 remains Contract Verified after the separate full Integrated maturity assessment. Gate 8 remains the sole `Specified - Next` gate, and its immutable `WorkItem` admission sub-capability is Contract Verified.
- A concrete IPC adapter was correctly excluded from the promotion prerequisite: endpoint, serialization, authentication, threading, persistence, and production wiring remain deferred integration work.
- Fresh combined evidence includes 42 focused tests, a 47-suite/208-test full regression (206 passed, 2 existing symbolic-link skips), and Java 17 strict lint across 122 production sources.
- The Gate 7 IPC contract, WorkItem admission, runtime-path integration, product-quality tracks, current Gate 7 maturity assessment, and Gate 8 queue increments are delivered through `1151fc5`.

## Updated At

2026-07-20

## Completed Work

- Implemented and Contract Verified the Gate 8 in-process Scheduler worker (connection 3a): `DurableAgentRunWorker.runOneCycle` drives claim + lease, injected execution to a durable RunRecord, fence-checked acknowledgement, finalization, and queue disposition in one recoverable, idempotent order over a worker-owned durable cycle-intent checkpoint written before the claim; recovery routes by runtime state and tolerates a missing runtime and an unstarted AgentRun; execution/finalizer failures fail closed with the intent retained; the reference persists before acknowledgement, closing the finalizer's deferred pre-terminal window; process isolation (3b), the local IPC adapter (3c), and the real `AgentLoop`-backed execution port remain the next connections.
- Implemented and Contract Verified Gate 8 RunRecord-backed result-path finalization: `DurableAgentRunFinalizer` connects a resolved RunRecord to the runtime terminal state and the matching queue disposition in one recoverable, idempotent order, deriving the disposition from the runtime terminal status so the stores cannot diverge; it resolves (never persists) the RunRecord, binds it to the Goal on `taskId` plus `sourceDocument`, fails closed on a missing/corrupt record, re-claims the queue's requeued active item before disposing, and exposes a forward `finalizeAgentRun` plus autonomous `recoverFinalization`; the process-isolated worker and RunRecord production remain the next connection.
- Implemented and Contract Verified Gate 8 durable queue terminal disposition: a terminal `WorkItemDisposition`, a disjoint `failedWorkItemIds` set with an extended partition invariant, the `completeActiveVerified`/`failActive` split across in-memory/durable/filesystem layers, in-place schema-v1 serialization with restart recovery, and dependents of failed work left blocked; result/RunRecord wiring and dispatcher disposition recording remain the next connection.
- Integrated one durable queue active/ready claim with exact Goal/AgentRun persisted-prefix recovery and fenced lease acquisition without worker execution or queue acknowledgement.
- Implemented and verified the Gate 8 durable one-Goal/one-AgentRun lifecycle with exact WorkItem provenance, typed terminal result matching, persist-before-exposure state, and restart-safe filesystem recovery.
- Implemented and verified fenced single-owner AgentRun lease acquisition, renewal, completion, expiry reclamation, monotonic persisted fence history, and restart behavior without adding worker execution.
- Removed the runtime package cycle through neutral Kernel verification values, application-layer finalization, and an executable dependency-direction regression.
- Added a process-wide bounded Tool isolation capacity with actual-thread-lifetime leases and fail-closed typed saturation.
- Corrected Unicode-unsafe truncation and size-check/read TOCTOU boundaries across Evidence, Tool diagnostics, CLI output, Context, Workspace, and all current filesystem stores.
- Implemented and verified Gate 8 durable queue state: immutable schema-v1 snapshots, atomic integrity-checked filesystem persistence, persist-before-exposure transitions, exact envelope recovery, monotonic revisions, and admission-ordered active-work requeue after interruption.
- Implemented the first Gate 8 Scheduler queue contract test-first, retaining exact WorkItems while enforcing bounded dependency-first admission and deterministic single-active-work sequencing.
- Completed the Gate 7 Integrated maturity assessment without production or test changes: the work-message queue path is Integrated, but gate-level promotion is unsupported by the unconnected payload, topic, reliability, causation, and transport branches.
- Specified the cross-cutting product journeys, evaluation metrics/harness, truthful Scheduler delivery model, shared interface order, change-centered UX, and layered security ownership; strengthened Gate 13 and Gate 16 measured-quality exit criteria without code or maturity changes.
- Added and verified the authority-preserving Gate 7 integration-preparation path: real Context Reader and Workspace inputs flow through one bounded work publisher, the actual in-process queue/journal/replay behavior, and one injected WorkItem admission handler without a concrete IPC adapter or Scheduler behavior.
- Implemented and Contract Verified immutable Gate 8 `WorkItem` admission over one unchanged Gate 7 work envelope, with separate identity, bounded capability metadata, projection-only accessors, and no authority or scheduler behavior.
- Promoted Gate 7 to Contract Verified after mapping every scope item and exit criterion to fresh evidence, and advanced the sole `Specified - Next` marker to Gate 8 without production code or concrete-adapter work.
- Updated only the actual-Roadmap Planner and Assisted Loop expectations from Gate 7 to Gate 8 after the marker move produced the expected focused RED.
- Completed the test-first Gate 7 work-payload correction: public `WorkPayload.MAX_ALLOWED_TOOLS = 256` accepts the exact boundary and rejects 257 while preserving immutable scope copying and all existing envelope semantics.
- Completed the Gate 7 maturity assessment without production or test changes; its original finding was five fully verified scope items and three fully verified exit criteria, with the payload cardinality gap recorded for the separate correction that has now closed it. Integrated and Operational claims remain unsupported.
- Implemented the seventh Gate 7 increment: Contract Verified finite non-blocking pending-queue backpressure through immutable `BackpressurePolicy` (1-4096), scope-level `BACKPRESSURED` refusal with no delivery-state side effects, FIFO admission, explicit retryability, and deterministic bounded replay.
- Corrected the Gate 7 replay cascade: handler publications caused by replay inherit non-journaling mode, and every publication reaches drain-owned cancellation admission.
- Eliminated Git observer command-execution vectors by using one canonical absolute, project-external Git executable for filter-free index metadata and explicitly disabling tracked-worktree diff observation.
- Synchronized canonical and compact current-state guidance with PR #3 merge commit `52987f2`, Gate 6 Integrated, Gate 7 Specified - Next, and backpressure as the next separately activated increment.

- Implemented the sixth Delivery Gate 7 increment: Contract Verified run-to-completion delivery ordering on `InProcessMessageBus` — a pending queue and a single drain loop replace nested dispatch, so a publication made from inside a handler is queued and reports the scope-level `ENQUEUED` status while the draining top-level `publish` or `replay` returns the whole ordered cascade; delivery order equals publication order, no subscriber observes an effect before its cause, admission and journaling happen in the drain loop so the journal's order is the bus's delivery order, a correlation cancelled mid-cascade refuses entries queued behind it while an in-flight fan-out stays atomic, and an `Error` abandons the cascade entirely. The ordering defect was proven real behaviourally before the fix.
- Implemented the fifth Delivery Gate 7 increment: Contract Verified correlation-scoped cancellation propagation on `InProcessMessageBus` — `cancel(correlationId)` is idempotent and monotonic with no resume, and a cancelled correlation is refused admission before subscription lookup, idempotency, and dispatch on every path (publish, replay, and dead-letter re-delivery), reporting a scope-level `CANCELLED` outcome that names no subscription, invoking no handler, consuming no idempotency key, creating no dead letter, and appending nothing to the journal so fresh-bus replay stays deterministic; cancellation dominates both `UNROUTED` and `DUPLICATE`, and the bus reads no payload to decide delivery, keeping `ControlSignal.CANCEL` a consumer semantic.
- Implemented the fourth Delivery Gate 7 increment: Contract Verified bounded synchronous retry and explicit dead-letter re-delivery on `InProcessMessageBus` — an immutable `RetryPolicy` (1-10 attempts; the default bus keeps a single attempt) retries a failing handler immediately with no delay before dead-lettering it with its failed attempt count (`DeadLetter.attempts`), and `redeliver` accepts only a currently recorded dead letter, resolves it on success, and on renewed exhaustion replaces it in place with the accumulated attempt count and latest reason, never appending to the journal or releasing the consumed idempotency key.
- Implemented the third Delivery Gate 7 increment: Contract Verified delivery-failure isolation and dead-letter capture on `InProcessMessageBus` — a subscriber handler that throws yields a `FAILED` outcome and an ordered immutable `DeadLetter` while fan-out continues, and a failed delivery consumes the idempotency key and is terminal (no automatic retry), reporting `DUPLICATE` with no further dead letter on re-publish or replay.
- Implemented the second Delivery Gate 7 increment: the Contract Verified `InProcessMessageBus` under `com.enhancer.bus` — synchronous single-threaded deterministic topic fan-out and single-consumer queue delivery over `MessageEnvelope`, typed `DeliveryOutcome`/`DeliveryStatus` results, per-`(destination, subscriber, message identity)` idempotency, and an ordered immutable journal supporting deterministic replay without duplicate side effects; envelopes are carried unmutated so authorization and provenance survive every hop.
- Fixed a pre-existing wall-clock-dependent defect (unrelated to the delivery increment) in `RunRecordMetadataCollectorTest`: it hardcoded the observation time to `10:01 UTC` while `persist()` stamps `storedAt` with `Instant.now()`, so it failed whenever the wall clock passed that time; the observation time is now derived from the run clock. Recorded as a separate accepted decision.
- Implemented the first Delivery Gate 7 increment: the Contract Verified `MessageEnvelope` under `com.enhancer.bus` — versioned reference-only envelopes with canonical identities and the sealed four-kind payload hierarchy carrying authorization and snapshot references as data.
- Executed the user-approved Gate 6 re-scope and promotion: editor-dependent observations moved to Gate 12, Gate 6 promoted to Integrated, the `Specified - Next` marker advanced to Gate 7, and the two actual-roadmap test contracts updated in the same change.
- Completed the Gate 6 maturity assessment: every scope item and exit criterion mapped to fresh evidence or its later-gate blocker, with the re-scope-and-promote recommendation (Option B) recorded pending explicit user approval; documentation-only, gate status unchanged.
- Implemented the fourteenth Delivery Gate 6 increment: `GitWorkspaceCollector` under explicitly granted read-only external command authority — two fixed git invocations, digest-only retention, discovery confined to the project root, watchdog timeout, and explicit `UNAVAILABLE` on every failure; a real discovery-ceiling defect and a piped-stderr hang were caught and fixed during GREEN.
- Implemented the thirteenth Delivery Gate 6 increment: `TargetFileMetadataCollector` observing the run target pre-run with a streamed containment-checked digest, missing targets explicit `UNAVAILABLE`, and containment violations rejected as usage errors before execution.
- Implemented the twelfth Delivery Gate 6 increment: `WorkspaceAuthorityBoundaryIntegrationTest`, pinning on first run that adversarial tool-grant text in observed documents cannot widen task or policy scope, appear in bounded output, or mutate any repository document.
- Implemented the eleventh Delivery Gate 6 increment: the `Justified By` task-document reference grammar and `TaskJustificationProjector`, projecting explicit task-to-decision references into `JUSTIFIED_BY` edges with strict rejection of malformed or unresolved references, merged into the production graph with an `impactDecisions` count; the first real reference resolved on the actual repository.
- Completed the Gate 6 sub-capability integration promotion audit: all six Contract Verified sub-capabilities promoted to Integrated against named, fresh-run, pre-existing integration evidence, with no production or test code change and Gate 6 preserved as `Specified - Next`.
- Implemented the tenth Delivery Gate 6 increment: production graph composition on the CLI `run` path — prior run records observed into the snapshot, accepted-decision nodes merged into the run-evidence graph, the task impact query answered in process, and bounded `graphNodes`/`graphEdges`/`graphDecisions`/`impactExecutions` output.
- Implemented the ninth Delivery Gate 6 increment: `RunRecordMetadataCollector` plus the store's read-only ordered `references()` listing, with corrupted or missing records surfaced as explicit `UNAVAILABLE` observations.
- Implemented the eighth Delivery Gate 6 increment: `AcceptedDecisionProjector` parsing accepted decisions from the decision log's own status lines into unlinked `DECISION` nodes with snapshot-relative freshness.
- Implemented the seventh Delivery Gate 6 increment: `RunEvidenceGraphProducer`, the first graph producer, projecting evidence-only task/artifact/execution nodes and one `RECORDED_AS` edge from one snapshot and one task-matched stored run record, with one-to-one observation-state-to-freshness mapping.
- Integrated the run-evidence production path: the end-to-end test flows a real governed run and really-collected snapshot through the producer into an impact-query answer naming the real stored execution.
- Implemented the sixth Delivery Gate 6 increment: `TaskImpactQuery` and the immutable `TaskImpact` result answering the task-to-decision-to-code-to-test chain over one projected graph, with snapshot-traceable identity and rebuild status derived from every traversed element.
- Implemented the fifth Delivery Gate 6 increment: the metadata-only graph projection contract (`GraphNode`, `GraphEdge`, `GraphProvenance`, `GraphElementFreshness`, `ProjectBrainGraph`) with five node kinds, six endpoint-checked edge kinds over the five roadmap relationship domains, and snapshot-keyed versioned projections.
- Enforced provenance invariants (Current/Stale require a SHA-256 revision, Source-Missing prohibits one, rebuild status is derived) plus deterministic ordering, duplicate/self-loop/unknown-endpoint rejection, and 4096-element bounds; named the impact query as the consumer.
- Implemented the fourth Delivery Gate 6 increment: production composition of the `ProjectBrainView` on the CLI `run` path from already-loaded memory, the collected snapshot, and the persisted RunRecord, with bounded `workspaceSnapshotId`, `workspaceObservations`, and `memoryFreshness` output.
- Promoted the production repository-memory composition to Operational with an actual-repository run and unchanged replay; the snapshot identity is intentionally not stored in the RunRecord (deferred to Gate 7 envelopes).
- Implemented the third Delivery Gate 6 increment: the read-only `RepositoryMemorySnapshotCollector`, the first Workspace source adapter, deriving a real snapshot from already-loaded Context Reader memory without reading files or retaining content.
- Integrated the repository-memory path end to end through `WorkspaceCollectionIntegrationTest`: real governed CLI run, real persisted RunRecord, real Context Reader memory, collector, and composed `ProjectBrainView`, including exact `SNAPSHOT_DIVERGED` detection after the active task document changed.
- Implemented the second Delivery Gate 6 increment: the read-only `ProjectBrainView` aggregate under `com.enhancer.brain` with `RepositoryMemoryEntry`, `RunProvenance`, and `MemoryFreshness`.
- Gave the Contract Verified `WorkspaceSnapshot` its first consumer by composing one real snapshot, one real `ProjectContext`, and one real `RunRecord` behind one immutable view.
- Derived repository-memory freshness from digest comparison against snapshot observations and rejected runs that do not match the snapshot's approved task revision.
- Kept document content, Tool payloads, evidence bodies, adapters, live collection, persistence, and Tool authority outside the increment.
- Recorded `ProjectBrainView` as Contract Verified while leaving Gate 6 as the sole `Specified - Next` product gate.
- Implemented the first Delivery Gate 6 contract under `com.enhancer.workspace`.
- Added immutable approved-task revision provenance and typed metadata observations for repository documents/files, active/selected files, Git status/diff, diagnostics, terminal sessions, and RunRecords.
- Added explicit Available, Stale, and Unavailable invariants with bounded metadata, digest validation, and temporal consistency.
- Added immutable `WorkspaceSnapshot` capture with absolute normalized root, canonical ordering, duplicate and 4096-entry limits, and a versioned SHA-256 identity sensitive to every metadata field.
- Kept payload capture, adapters, persistence, Tool authority, approval creation, and Project Brain integration outside the contract.
- Recorded the snapshot sub-capability as Contract Verified while leaving Gate 6 as the sole `Specified - Next` product gate.
- Implemented Delivery Gate 5 First Operational CLI at `com.enhancer.cli.EnhancerCli`.
- Registered the Gradle `application` entry point and exposed only non-interactive `run` and `replay` commands.
- Required explicit project root, active task ID, relative target path, expected lowercase SHA-256, evidence root, and RunRecord root for execution.
- Reused the existing Context Reader, repository-derived approval, `read-file` Tool boundary, Agent Loop, independent verifier, finalizer, Evidence Store, and RunRecord Store.
- Added stable exit codes for completed, usage/configuration, verification failure, policy denial, Tool failure, stagnation, maximum iterations, and internal failure.
- Bounded stdout and stderr to 4096 characters, sanitized line breaks, omitted complete evidence, and emitted no stack traces.
- Added restart-safe typed RunRecord replay without Tool re-execution or chat history.
- Added test-first coverage for parsing, exit-code mapping, bounded diagnostics, temporary-project completion, mismatch, task mismatch, Tool failure persistence, and replay.
- Documented invocation, exit codes, recovery, and replay in `README.md` and synchronized canonical/compact architecture, Roadmap, Project State, Current Task, Changelog, and this handoff.
- Promoted Delivery Gate 5 to Operational and Delivery Gate 6 Workspace and Project Brain Foundation to the sole `Specified - Next` gate.
- Added a RED classification gate to the AI workflow, Agent rules, and implementation prompt: aligned missing implementation proceeds directly to the minimum GREEN change, while unrelated, flaky, conflicting, scope-expanding, or newly privileged failures are reported separately.
- Promoted Gate 0 to Integrated through `FoundationLifecycleIntegrationTest` without a production correction or second orchestration path.

## Repository State

- Root: `C:/Enhancer`.
- Branch: `main`, tracking `origin/main`.
- Published Gate 6 delivery commit: `c5a16b9` (`feat: add Gate 6 workspace snapshot contract`).
- Gate 5, Gate 0 integration promotion, and the RED workflow clarification are committed and published on `origin/main`.
- The Gate 6 WorkspaceSnapshot implementation and synchronized documents are committed and published on `origin/main`.
- `gate-6-workspace-snapshot-contract` is Completed; its record is preserved in commit `c5a16b9`, `CHANGELOG.md`, and `PROJECT_STATE.md`.
- `gate-6-project-brain-view-integration` is Completed; its record is preserved in `CHANGELOG.md` and `PROJECT_STATE.md`.
- `gate-6-repository-memory-snapshot-collection` is Completed; its record is preserved in `CHANGELOG.md` and `PROJECT_STATE.md`.
- `gate-6-run-evidence-graph-producer`, `gate-6-accepted-decision-projection`, and `gate-6-run-record-metadata-observation` are Completed; their records are preserved in `CHANGELOG.md` and `PROJECT_STATE.md`.
- `gate-6-production-graph-composition` is Completed; its record is preserved in `CHANGELOG.md` and `PROJECT_STATE.md`.
- `gate-6-task-justification-references` (published through `0e2be2c`), `gate-6-authority-boundary-evidence`, and `gate-6-target-file-observation` are Completed; their records are preserved in `CHANGELOG.md` and `PROJECT_STATE.md`.
- `gate-6-git-workspace-adapter` is Completed and published through `21e6230`; `gate-6-maturity-assessment` is Completed with its record preserved in `PROJECT_STATE.md`.
- `gate-6-rescope-and-promotion` is Completed; its record is preserved in `CHANGELOG.md` and `PROJECT_STATE.md`.
- PR #3 published bounded retry/re-delivery, cancellation propagation, and delivery ordering through `52987f2`; replay/Git corrections followed through `2585a10`, and backpressure plus the four reliability/security corrections are published through `b3be720`.
- The Gate 7 transport-neutral IPC contract, maturity assessment, bounded work-payload correction, Contract Verified promotion, and Gate 8 next-marker synchronization are published through `16c7f5d`.
- The Gate 8 `WorkItem` admission contract, named Gate 7 integration path, and product-quality specifications are published through `c40e31e`.
- The completed documentation-only Gate 7 Integrated maturity assessment, implemented/verified Gate 8 dependency-ready single-worker queue, and subsequent runtime hardening are delivered through `1151fc5`.
- The completed Gate 8 durable Goal/AgentRun lifecycle and fenced lease work are delivered through `ed1c41c`.
- The Integrated durable queue-to-AgentRun dispatch path is delivered through `4ada41c`.
- The Gate 7 in-process delivery surface and its delivery-failure and dead-letter handling are committed and published on `origin/main` through delivery commit `b278c53`; the unrelated wall-clock test correction is published through `2a69182`.
- Local build note: this host had no JDK, so Java 17 was provisioned by junctioning `C:/Users/dokan/.jdks/corretto-17.0.14` into the Git-ignored `.tools/jdk17-runtime`; `scripts/gradle.ps1` then works normally.
- The maturity assessment, the re-scope-and-promotion, and the Gate 7 envelope contract are committed and published on `origin/main` through delivery commit `3423201`.
- The authority-boundary, target-file, and Git-adapter increments are committed and published on `origin/main` through delivery commit `21e6230`.
- The external read-only command authority for the Git adapter was explicitly granted by the user on 2026-07-15 ("3번 승인할게") and is scoped to `GitWorkspaceCollector` by accepted decision.
- The actual-repository evidence runs persisted records under the Git-ignored `.enhancer/run-records` directory, most recently `run-record/4f0d3da1-e8a8-412f-a513-79338d47b2b7`.
- The first five Gate 6 increments (view, collector, production composition, graph contract, impact query) are published on `origin/main` through delivery commit `d3b6197`.
- The four later increments (run-evidence producer, decision projection, run-record observation with store listing, production graph composition) are committed and published on `origin/main` through delivery commit `396665b`.
- The actual-repository evidence runs persisted `run-record/ca604c7c-23e8-4b1c-8aa2-38fb6bfed5cf` and `run-record/69977403-1cfb-45ba-ba0f-9239ad26a8c1` under the Git-ignored `.enhancer/run-records` directory.
- The actual-repository evidence run persisted `run-record/ca604c7c-23e8-4b1c-8aa2-38fb6bfed5cf` under the Git-ignored `.enhancer/run-records` directory.

## Fresh Verification

- Worker checkpoint RED: test compilation failed with exactly 19 errors naming only the absent `PendingFinalization`, `FileSystemPendingFinalizationStore`, and `CorruptedPendingFinalizationException`; focused GREEN passed 8 of 8 `FileSystemPendingFinalizationStoreIntegrationTest` cases.
- Worker happy-path RED: test compilation failed with exactly 5 errors naming only the absent `AgentRunExecution` and `DurableAgentRunWorker`; focused GREEN passed the verified-cycle, failed-cycle, and empty-queue tests (3 of 3).
- Worker recovery RED: the six added recovery tests failed behaviourally with the deliberate `UnsupportedOperationException` resume stub while the three happy-path tests stayed green; GREEN passed all 9 `DurableAgentRunWorkerTest` cases after the recovery routing replaced the stub.
- Worker integration: `FileSystemAgentRunWorkerIntegrationTest` passed 2 of 2 on first run (fresh-worker resume driving the dependent end to end; failed outcome blocking the dependent); the runtime package suite passed 14 suites and 73 tests with no skips, failures, or errors.
- Worker full result: 63 suites, 288 tests, 286 passed, 2 existing Windows symbolic-link setup skips, 0 failures, and 0 errors under `--warning-mode all`; Java 17 strict lint (`javac -Xlint:all -Werror`) passed across all 157 production sources.
- Worker structural: exactly one `Status: Specified - Next` gate marker (Gate 8) in `ROADMAP.md`; `git diff --check` reported no whitespace errors.
- Goal/AgentRun lifecycle RED: production compilation passed and test compilation failed with exactly 64 aligned missing-contract errors.
- Goal/AgentRun focused GREEN: 63 of 63 runtime, bus, and package-boundary tests passed across 10 suites.
- Goal/AgentRun full result: 55 suites, 238 tests, 236 passed, 2 existing Windows symbolic-link setup skips, 0 failures, and 0 errors.
- Goal/AgentRun strict lint passed across all 146 production sources; post-document Context Reader, Planner, Assisted Loop, package-boundary, lifecycle, and filesystem-store verification passed 26 of 27 tests with 1 existing Windows symbolic-link setup skip and no failure or error.
- Package-boundary RED: one structural test failed with exactly six forbidden import directions.
- Package-boundary focused GREEN: 27 of 27 structural/verifier/finalizer/RunRecord/CLI tests passed.
- Package-boundary full result: 53 suites, 228 tests, 226 passed, 2 existing symbolic-link skips, 0 failures, and 0 errors; strict lint passed across 135 production sources.
- Package-boundary post-document self-hosting passed 16 of 17 tests with 1 existing Windows symbolic-link setup skip and no failure or error.
- Tool-capacity RED: production compilation passed and test compilation failed with exactly 3 aligned errors for the absent capacity and failure contracts.
- Tool-capacity affected GREEN: 41 tests passed with 1 existing symbolic-link skip; full regression passed 225 of 227 with the 2 existing skips.
- Tool-capacity strict lint passed across 134 production sources.
- Unicode/file-bound RED: production compilation passed and test compilation failed with exactly 10 errors naming only the absent helper contracts.
- Unicode/file-bound focused GREEN: 18 of 18 tests passed across 4 suites; the affected integration group passed 54 with 2 existing Windows symbolic-link setup skips.
- Unicode/file-bound full result: 52 suites, 226 tests, 224 passed, 2 existing symbolic-link setup skips, 0 failures, and 0 errors; strict lint passed across 133 production sources.
- Gate 8 durable queue RED: production compilation passed and test compilation failed with exactly 48 aligned errors, 47 for absent durable contracts and 1 for the absent logical-run accessor.
- Gate 8 durable queue focused GREEN: 14 of 14 tests passed across 5 runtime/integration suites with no skips, failures, or errors.
- Gate 8 durable queue full result: 50 suites, 219 tests, 217 passed, 2 existing Windows symbolic-link setup skips, 0 failures, and 0 errors; Java 17 strict lint passed across 130 production sources.
- Gate 8 queue RED: production compilation passed and test compilation failed with exactly 25 errors naming only the absent `QueuedWork` and `SingleWorkerSchedulerQueue`.
- Gate 8 queue focused GREEN: 45 of 45 messaging/runtime tests passed across 6 suites with no skips, failures, or errors.
- Gate 8 queue full result: 48 suites, 211 tests, 209 passed, 2 existing Windows symbolic-link setup skips, 0 failures, and 0 errors; Java 17 strict lint passed across 124 production sources.
- Gate 8 queue post-document self-hosting passed 15 of 16 actual-document Context Reader, Planner, and Assisted Loop tests with 1 existing Windows symbolic-link setup skip and no failure or error.
- Gate 7 Integrated assessment focused result: 42 of 42 messaging/runtime tests passed across 5 suites with no skips, failures, or errors.
- Gate 7 Integrated assessment full result: 47 suites, 208 tests, 206 passed, 2 existing Windows symbolic-link setup skips, 0 failures, and 0 errors; Java 17 strict production lint passed across 122 sources.
- Gate 7 assessment post-document self-hosting passed 15 of 16 Context Reader, Planner, and Assisted Loop tests with 1 existing Windows symbolic-link setup skip and no failure or error; Gate 8 remained the sole next marker.
- Product-track documentation: 17 sequential Delivery Gates, one Gate 8 `Specified - Next` marker, four canonical journeys, one resolved accepted decision, seven explicit-denominator metric definitions, consistent Scheduler/interface/security language, and no Constitution diff.
- Product-track actual-document self-hosting: 15 of 16 Context Reader, Planner, and Assisted Loop tests passed with 1 existing Windows symbolic-link setup skip and no failure or error.
- Runtime-path focused verification: 42 of 42 tests passed across `MessagingRuntimeIntegrationTest`, `WorkItemTest`, and all Gate 7 bus suites with no skips, failures, or errors.
- Runtime-path full result: 47 suites, 208 tests, 206 passed, 2 existing Windows symbolic-link setup skips, 0 failures, and 0 errors; Java 17 production compilation of all 122 sources passed with `-Xlint:all -Werror`.
- The resumed worktree already contained both production adapters, so no missing-type RED is claimed for the integration-preparation increment.
- Post-document self-hosting passed 15 of 16 actual-document Context Reader, Planner, and Assisted Loop tests with 1 existing Windows symbolic-link setup skip and no failure or error.
- WorkItem RED: production compilation passed and test compilation failed with exactly 11 errors naming only the absent `WorkItem` contract.
- WorkItem focused GREEN: 41 of 41 tests passed across `WorkItemTest` and all Gate 7 bus suites with no skips, failures, or errors.
- Current full result: 46 suites, 207 tests, 205 passed, 2 existing Windows symbolic-link setup skips, 0 failures, and 0 errors; Java 17 production compilation of all 120 sources passed with `-Xlint:all -Werror`.
- Post-document self-hosting verification passed 15 of 16 Context Reader, Planner, and Assisted Loop tests with 1 existing Windows symbolic-link setup skip and both proposal paths retaining Gate 8.
- Promotion assessment focused evidence: all 39 Gate 7 bus tests passed with no skips, failures, or errors.
- Promotion marker RED: the first Planner/Assisted Loop run after moving the marker ran 8 tests and failed exactly the actual-Roadmap Planner assertion because it still expected Gate 7; the expectation-only correction restored all 8 tests.
- Work-payload boundary RED: the new test failed behaviorally only because 257 valid unique names were accepted; the exact 256-name boundary and all existing production compilation remained valid.
- Work-payload focused GREEN: all 39 bus tests passed with no skips, failures, or errors (`InProcessMessageBusTest` 30, `MessageEnvelopeTest` 5, `MessageTransportTest` 4).
- IPC contract RED: focused compilation failed with 33 expected errors naming only the absent `TransportMessage`, `MessageTransport`, `TransportOutcome`, and `TransportStatus`; production compilation passed.
- IPC contract focused GREEN: 38 tests across `MessageTransportTest`, `MessageEnvelopeTest`, and `InProcessMessageBusTest` passed with no skips, failures, or errors.
- Backpressure RED: focused compilation failed with exactly 10 expected errors naming only the absent `BackpressurePolicy`, `BACKPRESSURED`, and combined constructor; production compilation passed.
- Backpressure focused GREEN: 30 `InProcessMessageBusTest` tests and 4 `MessageEnvelopeTest` tests passed with no skips, failures, or errors.
- Ordering RED, first pass: 8 expected errors naming only the absent `DeliveryStatus.ENQUEUED` constant and `isScopeLevel()` accessor, with production compilation passing and no non-bus error.
- Ordering RED, second pass: after adding only those two symbols so the suite could run, three focused tests failed behaviourally against the real defect — two observed `[first, child, second]` where `[first, second, child]` was required, proving a cascaded child was delivered inside its parent's fan-out, and one observed `DELIVERED` where `ENQUEUED` was required; classified as aligned missing implementation.
- Ordering focused GREEN: `InProcessMessageBusTest` passed 25 tests (20 prior plus 5 new) and `MessageEnvelopeTest` 4 with no skips, failures, or errors, confirmed against fresh XML.
- Current full command: `.\scripts\gradle.ps1 --no-daemon clean test --warning-mode all`.
- Current full result: 45 suites, 205 tests, 203 passed, 2 Windows symbolic-link setup skips, 0 failures, and 0 errors; Gradle emitted no deprecation warning; Java 17 production compilation of all 119 sources passed with `-Xlint:all -Werror`.
- Post-promotion actual-document verification passed 15 of 16 Context Reader, Planner, and Assisted Loop tests with 1 existing Windows symbolic-link setup skip and selected Gate 8 on both proposal paths.
- Post-document self-hosting verification passed 15 of 16 Context Reader, Planner, and Assisted Loop tests with 1 existing Windows symbolic-link setup skip and no failure or error.
- Earlier cancellation RED: 19 expected errors naming only the absent `CANCELLED` constant and the `cancel`/`isCancelled` operations; focused GREEN passed 20 `InProcessMessageBusTest` tests and the full 176-test regression.
- Earlier retry/re-delivery RED: 16 expected errors naming only the absent `RetryPolicy` type, policy constructor, `redeliver` operation, `DeadLetter.attempts()` accessor, and five-component `DeadLetter` constructor; focused GREEN passed 15 `InProcessMessageBusTest` tests and the full 171-test regression.
- Earlier failure/dead-letter RED: 8 expected errors naming only the absent `DeliveryStatus.FAILED` constant, `DeadLetter` type, and `deadLetters()` accessor with no non-bus error; focused GREEN passed 10 `InProcessMessageBusTest` tests.
- Earlier session full result: 44 suites, 166 tests, 164 passed, 2 Windows symbolic-link setup skips, 0 failures, and 0 errors over 113 production sources.
- Structural: exactly one `Specified - Next` gate status marker at Gate 7; `git diff --check` passed for tracked and newly added files.
- Earlier delivery RED: 54 expected missing-symbol errors naming only the five absent delivery types; focused GREEN passed 11 bus tests.
- Earlier envelope RED: 38 expected missing-symbol errors (after replacing a Java 17 preview switch pattern in the test); focused GREEN passed 4 bus tests.
- Rescope verification: focused planner and Assisted Loop suites passed 8 tests against the actual roadmap proposing Gate 7; the marker moved with exactly one `Specified - Next` remaining.
- Git adapter RED: 6 expected missing-symbol errors; GREEN caught and fixed the discovery-ceiling defect; focused GREEN passed 21 suites and 62 tests.
- Target-file RED: 4 expected missing-symbol errors; focused GREEN passed 20 suites and 59 tests.
- Boundary characterization passed on first run (2 tests) with no production change.
- Earlier session full result: 42 suites, 152 tests, 150 passed, 2 Windows symbolic-link setup skips, 0 failures, and 0 errors.
- Actual repository `run`: task `gate-6-git-workspace-adapter`, exit code 0, `COMPLETED`, `VERIFIED`, `workspaceObservations=23` (15 documents, 5 prior run records, 1 target file, 2 `AVAILABLE` Git observations), `graphNodes=67`, `graphDecisions=49`.
- Earlier: the justification run resolved its own `Justified By` reference (`impactDecisions=1`, `graphDecisions=46`).
- Promotion audit: fresh focused verification across workspace, brain, run, CLI, and integration suites passed 19 suites and 59 tests with no skips, failures, or errors; each named connecting suite passed individually.
- Promotion audit full regression: 38 suites, 140 tests, 138 passed, 2 Windows symbolic-link setup skips, 0 failures, 0 errors; the audit diff was documentation-only.
- Composition RED: both focused CLI graph-composition tests failed with the expected `output does not contain graphDecisions=` assertion while the runs completed.
- Composition focused GREEN: CLI, workspace, brain, and integration suites passed 17 suites and 50 tests with no skips, failures, or errors.
- Actual repository `run`: `README.md`, task `gate-6-production-graph-composition`, exit code 0, `COMPLETED`, `VERIFIED`, snapshot `d5bd10cb...a44632`, 17 observations (15 documents plus 2 prior run records), `graphNodes=61`, `graphEdges=1`, `graphDecisions=44` matching the decision log exactly, `impactExecutions=1`.
- Observation RED: 8 expected missing-symbol errors naming only the absent `RunRecordMetadataCollector` and `references()`; focused GREEN passed 8 suites and 33 tests.
- Projection RED: 6 expected missing-symbol errors naming only the absent `AcceptedDecisionProjector`; focused GREEN passed 5 suites and 20 tests.
- Producer RED: the first focused compile failed with 6 expected missing-symbol errors naming only the absent `RunEvidenceGraphProducer`.
- Producer focused GREEN: Project Brain and integration suites passed 6 suites and 18 tests with no skips, failures, or errors.
- End-to-end: the extended `WorkspaceCollectionIntegrationTest` flowed a real governed run and really-collected snapshot through the producer into an impact-query answer naming the real stored execution reference.
- Impact-query RED: the first focused compile failed with 9 expected missing-symbol errors naming only the absent `TaskImpactQuery` and `TaskImpact`.
- Impact-query focused GREEN: 3 suites and 13 tests with no skips, failures, or errors.
- Graph RED: the first focused compile failed with 100 expected missing-symbol errors naming only the seven intentionally absent graph types.
- Graph focused GREEN: 2 suites and 9 tests with no skips, failures, or errors.
- Composition RED: both focused CLI composition tests failed with the expected `output does not contain workspaceSnapshotId=` assertion while the runs completed and were recorded.
- Composition focused GREEN: CLI, Workspace, Project Brain, and integration suites passed 11 suites and 29 tests with no skips, failures, or errors.
- Actual repository `run`: `README.md`, task `gate-6-production-brain-composition`, exit code 0, `COMPLETED`, `VERIFIED`, `workspaceSnapshotId=b729514d272701e8f46d32b282f24570a75147470a6b82c0bd21bb0e97e9f39f`, `workspaceObservations=15`, `memoryFreshness=matched=15,diverged=0,notObserved=0`.
- Actual repository `replay` of `run-record/ca604c7c-23e8-4b1c-8aa2-38fb6bfed5cf` returned exit code 0 with unchanged output.
- Collector RED: the focused compile failed with 6 expected missing-symbol errors, all naming only the absent `RepositoryMemorySnapshotCollector`.
- Collector focused GREEN: 7 suites and 20 tests with no skips, failures, or errors.
- Earlier session full result: 38 suites, 140 tests, 138 passed, 2 Windows symbolic-link setup skips, 0 failures, and 0 errors.
- Current Java 17 production lint passed with `-Xlint:all -Werror`; Gradle emitted no deprecation warning.
- Current structural verification: exactly one `Specified - Next` gate status marker at Gate 6 and `git diff --check` passed.
- End-to-end: `WorkspaceCollectionIntegrationTest` connected a real governed CLI run, real RunRecord, real Context Reader memory, the collector, and the composed view; all 15 documents were `SNAPSHOT_MATCHED` and exactly `CURRENT_TASK.md` reported `SNAPSHOT_DIVERGED` after its edit.
- Not run: no production caller composes the view during an actual repository run; the end-to-end evidence is a governed temporary-project integration test.
- Project Brain RED: the first focused compile failed with 19 expected missing-symbol errors naming only `ProjectBrainView`, `RepositoryMemoryEntry`, `RunProvenance`, and `MemoryFreshness`; no error came from existing contracts.
- Project Brain focused GREEN: 4 suites, 15 tests, no skips, failures, or errors.
- Workspace RED: the first focused compile failed with 79 expected missing-symbol errors before production contracts existed.
- Workspace focused GREEN: 3 suites, 10 tests, all passed with no skips, failures, or errors.
- Post-document self-hosting verification passed 14 of 15 Context Reader, Planner, and Assisted Loop tests with 1 existing Windows symbolic-link setup skip and no failure or error.
- Structural verification retained exactly one `Specified - Next` marker at Gate 6; `git diff --check` passed.
- RED: the first focused CLI compile failed with 45 expected missing-symbol errors.
- Focused GREEN command: `.\scripts\gradle.ps1 --no-daemon cleanTest test --tests 'com.enhancer.cli.*'`.
- Focused GREEN result: 3 suites, 7 tests, 0 failures, 0 errors, 0 skips.
- Full command: `.\scripts\gradle.ps1 --no-daemon clean test --warning-mode all`.
- Full result: 24 suites, 97 tests, 95 passed, 2 Windows symbolic-link setup skips, 0 failures, and 0 errors.
- Gradle emitted no deprecation warning.
- Java 17 production lint passed with `-Xlint:all -Werror` and no warning or error.
- Actual repository `run`: `README.md`, task `gate-5-first-operational-cli`, exit code 0, `COMPLETED`, `VERIFIED`, one worker iteration.
- Persisted record: `run-record/0ff7b4ea-5d4a-4698-b9f7-642169262737` under the Git-ignored `.enhancer/run-records` runtime directory, which Gradle `clean` does not remove.
- Actual repository `replay`: the record survived Gradle `clean` and returned exit code 0 with matching task, allowed policy, worker `AWAITING_VERIFICATION`, final `COMPLETED`, and `VERIFIED` metadata.
- Separate focused verification passed for Gate 0 (35 tests, 1 skip), Gate 1 (15 tests, 1 skip), Gate 2 (10 tests), Gate 3 (9 tests), Gate 4 (21 tests), and Gate 5 (7 tests), with no failures or errors.
- Gate 0 promotion preparation verification ran 4 context/planning/task suites and 18 tests: 17 passed, 1 Windows symbolic-link setup test skipped, and no failures or errors occurred; Roadmap retained exactly one Gate 6 next marker.
- Gate 0 lifecycle characterization passed on its first run; combined focused verification passed 43 tests across 10 suites with 1 Windows symbolic-link setup skip.
- Final full regression passed 98 tests across 25 suites: 96 passed, 2 Windows symbolic-link setup skips, 0 failures, and 0 errors.
- Java 17 production lint passed with `-Xlint:all -Werror`; no production correction was required for Gate 0 promotion.

## Current Maturity

- Product Journey and Evaluation Track: Accepted specification only; no journey or evaluation harness is claimed Contract Verified, Integrated, Operational, or Released.
- Gate 0: Integrated.
- Gates 1 through 4: Integrated.
- Gate 5: Operational for one governed read-only local CLI scenario.
- Gate 6: Integrated by the user-approved re-scope-and-promotion decision; the production view and graph composition remain Operational sub-capabilities.
- Gate 7: Contract Verified after fresh assessment. The work-message queue path is Integrated; result/control/handoff, non-empty causation, topic and remaining reliability branches, and `MessageTransport` remain contract-only. Durable messaging, a concrete adapter, and a supported production entry point do not exist.
- Gate 8: Specified - Next; immutable `WorkItem` admission, the dependency-ready single-worker queue, durable schema-v1 queue state/restart recovery, the durable one-Goal/one-AgentRun lifecycle, fenced single-owner lease/expiry recovery, durable queue terminal disposition, RunRecord-backed result-path finalization, and the in-process Scheduler worker (connection 3a) are Contract Verified; durable queue-to-lifecycle dispatch is Integrated. `DurableAgentRunWorker` now drives one recoverable claim-to-disposition cycle over a durable cycle-intent checkpoint, but worker process isolation (3b), a concrete local IPC adapter (3c), the real `AgentLoop`-backed execution port with its `WorkPayload` extension, retry, effect records/fencing, and broader production wiring do not yet exist.
- Gate 6 repository-memory path (real governed run -> real memory -> collector -> composed view with divergence detection): Integrated.
- Gate 6 production composition: Operational for the governed read-only CLI scenario; every recorded `run` reports bounded snapshot identity, observation count, and memory freshness.
- Gate 6 `WorkspaceSnapshot`, `ProjectBrainView`, graph projection contract, `TaskImpactQuery`, `AcceptedDecisionProjector`, and `RunRecordMetadataCollector`: Integrated through the fresh promotion audit against named pre-existing integration evidence.
- Gate 6 `TaskJustificationProjector` and the `Justified By` reference grammar: Integrated; the first real reference resolved on the actual repository.
- Gate 6 `TargetFileMetadataCollector` and `GitWorkspaceCollector`: Integrated on the production CLI path; Git observation runs under explicitly granted, decision-scoped read-only external command authority with inherited Git overrides removed and external diff/text-conversion helpers disabled.
- Gate 6 authority-boundary exit criterion: pinned by `WorkspaceAuthorityBoundaryIntegrationTest`.
- Gate 6 repository-memory and run-evidence production paths: Integrated.
- Gate 6 production view and graph composition: Operational for the governed read-only CLI scenario; impact answers carry executions and explicitly justified decisions, and modifies/verified-by producers do not exist.
- Gate 0 integration proves planning, explicit external activation, verified execution, persistence, and replay without changing Proposal authority.
- Remaining editor/diagnostic Workspace adapters, LLM invocation, production Event/Message Bus wiring, concrete IPC adapters, Scheduler, broader Agent Runtime, MCP, Skills, plugins, multi-agent execution, background execution, and release packaging remain unimplemented.

## Next Task

Choose and implement the next bounded Gate 8 sub-increment after the in-process worker (3a): worker process isolation (3b), the concrete `MessageTransport` local IPC adapter (3c), or the real `AgentLoop`-backed `AgentRunExecution` port with its `WorkPayload` execution-input extension (crossing Gate 6/7 boundaries, so it needs its own bounded task and design). Transport acceptance never means bus delivery or work completion, and long-running Tool execution stays behind process isolation.

## Remaining Risks

- The CLI trusts an externally supplied expected digest; its origin is explicit and auditable but not signed.
- Evidence and RunRecord envelopes detect corruption but are not encrypted, signed, remotely replicated, or automatically cleaned up.
- The CLI uses the existing 64 MiB per-artifact/in-memory ceiling, five-second Tool timeout, five-iteration loop ceiling, and three-transition stagnation threshold. Evidence has no time-based retention or automatic cleanup contract.
- Two privilege-dependent symbolic-link containment tests are skipped on this Windows host; two Windows junction tests now execute and pass against the same production real-path guards.
- Gradle remains at Wrapper 8.4. The known Gradle 9 test-runtime deprecation is removed, but an actual major Wrapper upgrade requires a separate compatibility task.
- Gate 5 is a bootstrap CLI, not the future multi-interface control surface planned for Gate 12.

## Instructions For Next Agent

1. Read `.ai/` and every canonical startup document in repository order.
2. Confirm Gate 7 is `Contract Verified`, Gate 8 is the sole `Specified - Next` gate, and `CURRENT_TASK.md` records the in-process Scheduler worker (`gate-8-in-process-scheduler-worker`) as Completed.
3. Inspect `git status --short` and the current branch log; the worker implementation lives on `feature/gate-8-worker-implementation`, delivered per the user's 2026-07-20 commit/push/merge authorization.
4. If the host has no JDK, provision Java 17 through `.tools/jdk17-runtime` (this host already has `corretto-17.0.14` there) or run `scripts/setup-dev.ps1`; `scripts/gradle.ps1` then works normally.
5. The only external command authority is the decision-scoped read-only Git adapter; any new external command capability requires its own explicit user approval.
6. Pick the next bounded Gate 8 sub-increment — worker process isolation (3b), the concrete local IPC adapter (3c), or the `AgentLoop`-backed execution port with its `WorkPayload` extension — and design it before implementation; preserve the distinction among execution acknowledgement, independently verified terminal state, and dependency satisfaction.
7. Do not commit or push future changes without a new explicit user request.
