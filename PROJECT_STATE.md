# Project State

## Updated At

2026-07-20

## Repository State

- Repository root: `C:/Enhancer`.
- Current branch: `main` tracking `origin/main`.
- Build system: Gradle 8.4 Wrapper with Java 17.
- Production source: 158 Java files.
- Test source: 65 Java files.

Delivery history is `git log`, and per-increment delivery is described in
`CHANGELOG.md`. This section states only what is true of the working tree now;
it does not restate which commit published which increment.

## Capability Maturity

### Contract Verified

- Delivery Gate 8 WorkPayload execution input under `com.enhancer.bus`/`com.enhancer.runtime`: `WorkPayload` carries an optional caller-supplied `ExecutionInput(targetPath, expectedContentSha256)` (bounded target path, 64-hex digest) with a three-argument convenience constructor, projected by `WorkItem.executionInput()`, persisted after `allowedTools` in both filesystem serializers via a presence flag (schema v1 revised in place, pre-existing snapshots fail closed), and published through a `WorkMessagePublisher` overload as explicit caller authority data (not snapshot-derived). `AgentLoopAgentRunExecution`'s derivation seam prefers the declared input and falls back to the approved source document, so a WorkItem executes an arbitrary governed target while the `ApprovedTask`/Goal binding stays the source document. No version bump, migration, write Tool, multiple inputs, or payload-carried plan is added.
- Delivery Gate 8 AgentLoop-backed execution port under `com.enhancer.runtime`: `AgentLoopAgentRunExecution` is the first production `AgentRunExecution` — one `execute(dispatch)` call assembles the Gate 1-4 pipeline (governed `read-file` `ToolExecutor` with `EvidenceRecorder`-persisted evidence, bounded `AgentRunController`/`AgentLoop` at the CLI reference bounds, `DeterministicReadFileVerifier`, application `AgentRunFinalizer`) against the approved task's own source document (`taskRevision().sourceDocument()` as target, `taskRevision().sourceSha256()` as expected content SHA-256; no payload or serialization schema change) and returns the persisted `run-record/<uuid>` reference. The `ApprovedTask` is built directly from the WorkItem so the runtime finalizer's taskId-plus-sourceDocument binding holds by construction; the port must persist through the same `RunRecordStore` the worker's finalizer resolves from; a digest mismatch or Tool failure is carried in the persisted RunRecord (non-`VERIFIED`, finalized to `FAILED`), never thrown; the `(targetPath, expectedContentSha256)` derivation sits behind one private seam that the named `WorkPayload` execution-input extension will replace. It adds no write Tool, retry, control, process isolation, IPC, or contract change to the worker/dispatcher/runtime/finalizer/queue.
- Delivery Gate 8 in-process Scheduler worker (connection sub-increment 3a) under `com.enhancer.runtime`: `DurableAgentRunWorker.runOneCycle(leaseDuration)` drives one scheduling cycle end to end over the existing `DurableAgentRunDispatcher`, `DurableAgentRuntime`, and `DurableAgentRunFinalizer` in the authoritative recoverable order cycle-intent (ids) -> queue claim + lease -> RunRecord persisted (ref) -> intent updated with ref -> `completeExecution` -> `finalizeAgentRun` -> queue disposition -> clear intent, returning the cycle's `WorkItemDisposition` or empty when nothing was claimable. A worker-owned single-record durable cycle-intent checkpoint (`PendingFinalization`/`PendingFinalizationStore`/`FileSystemPendingFinalizationStore`: bounded, strict-UTF-8, digest-checked, atomically published, fail-closed) is written before the claim so a restarted worker re-supplies the same distinct canonical Goal/AgentRun identities and dispatcher recovery is idempotent (no second Goal, no orphaned runtime state, no dispatcher change); the reference persists before acknowledgement, closing the finalizer's deferred pre-terminal window. Recovery routes by runtime state as the source of truth (terminal -> `recoverFinalization`; `AWAITING_VERIFICATION` -> `finalizeAgentRun(ref)`; earlier, unstarted, or missing runtime state -> re-drive with the same identities, skipping re-execution when the reference exists). Execution is an injected `AgentRunExecution` port returning the RunRecord reference; failures fail closed with the intent retained, an empty-queue cycle leaves no durable trace, and re-execution on retry orphans the earlier RunRecord as an accepted at-least-once consequence. It adds no process isolation, IPC adapter, real `AgentLoop`-backed port, retry, controls, schema change, or contract change to the dispatcher/runtime/finalizer/queue.
- Delivery Gate 8 RunRecord-backed result-path finalization under `com.enhancer.runtime`: `DurableAgentRunFinalizer` composes the durable queue, `AgentRuntimeStateStore`, and `RunRecordStore` and drives one recoverable, idempotent order (resolve RunRecord by reference -> runtime terminal `recordResult` -> queue disposition), deriving the disposition from the runtime terminal status (`COMPLETED -> completeActiveVerified`, `FAILED -> failActive`) so the two stores cannot diverge; it resolves but never persists the RunRecord, binds it to the Goal on `taskId` plus `sourceDocument`, fails closed on a missing/corrupt record while leaving the run `AWAITING_VERIFICATION`, rejects a different-task record, a different-reference re-finalize, and finalize before execution acknowledgement, and re-claims the queue's requeued active item before recording the disposition; `finalizeAgentRun` drives the forward path and `recoverFinalization` applies only the autonomous post-terminal queue disposition with no reference. It adds no new store, schema change, cross-store transaction, worker, Tool execution, retry, or failure propagation.
- Delivery Gate 8 durable queue terminal disposition under `com.enhancer.runtime`: a terminal `WorkItemDisposition` (`VERIFIED_COMPLETED`, `FAILED`) where only verified completion satisfies dependencies, a disjoint `failedWorkItemIds` set extending the partition invariant to `pending + active + verified + failed`, the `completeActiveVerified`/`failActive` split across the in-memory queue, durable wrapper, and filesystem store, and in-place schema-v1 persistence with exact restart recovery; failed work never satisfies dependents and the queue stores disposition only, not a failure reason.
- Delivery Gate 8 durable Goal/AgentRun lifecycle and fenced ownership under `com.enhancer.runtime`: one exact WorkItem-backed Goal, one schema-v1 AgentRun, distinct canonical identities, forward-only Planning/Ready/leased-Executing/Awaiting-Verification/terminal transitions, matching typed result provenance, Verified-only completion, explicit non-verified failure, bounded single-owner leases, injected expiry time, persisted monotonic fences, stale-owner rejection, durable expiry reclamation, monotonic persist-before-exposure revisions, bounded strict-UTF-8 integrity-checked atomic filesystem state, and exact restart recovery; no retry, worker, external-effect fencing, RunRecord resolution, migration, history, multi-process locking, distributed clock-skew protocol, or power-loss directory-sync claim.
- Delivery Gate 8 durable queue state and restart recovery under `com.enhancer.runtime`: canonical queue identity and one-logical-run binding, immutable schema-v1 snapshots, a bounded strict-UTF-8 integrity-checked atomic filesystem store, exact WorkItem/envelope persistence, monotonic revisions, persist-before-exposure transitions, and admission-ordered active-work requeue on restart; no lease, fence, worker, effect deduplication, migration beyond v1, history retention, or power-loss directory-sync claim.
- Delivery Gate 8 dependency-ready single-worker queue under `com.enhancer.runtime`: immutable queued work with at most 256 unique canonical earlier-admitted dependencies, a run-scoped 4096-admission ceiling, duplicate rejection, deterministic FIFO readiness, one active slot, and matching explicit completion; no persistence, lease, recovery, worker execution, or authority.
- Delivery Gate 8 immutable `WorkItem` admission under `com.enhancer.runtime`: one canonical work identity distinct from the retained Gate 7 message identity, one bounded required capability, and approved task, Workspace snapshot, logical run, and immutable Tool scope exposed only as projections of one unchanged `WorkPayload` envelope; no runtime state, queue, persistence, lease, execution, or authority.
- Delivery Gate 7 versioned reference-only `MessageEnvelope` under `com.enhancer.bus`: canonical-UUID message identity, bounded correlation/run/producer identities, optional canonical-UUID causation distinct from the message identity, and one typed payload.
- Sealed four-kind payload hierarchy (work, result, control, handoff) carrying task revisions, snapshot identities, immutable allowed-tool scopes of 1 through 256 unique names, run-record references, verification status, and typed control signals as bounded data; no content, delivery semantics, or Tool authority.
- Delivery Gate 7 in-process delivery surface `InProcessMessageBus` under `com.enhancer.bus`: synchronous single-threaded topic fan-out in registration order and single-consumer queue delivery over `MessageEnvelope`, typed `DeliveryOutcome`/`DeliveryStatus` results, per-`(destination, subscriber, message identity)` idempotency, and an ordered immutable journal supporting deterministic replay without duplicate side effects; envelopes are carried unmutated so authorization and provenance survive every hop.
- Delivery Gate 7 delivery-failure isolation and dead-letter capture: a subscriber handler that throws yields a `FAILED` outcome and an ordered immutable `DeadLetter` (destination, subscriber, unmodified envelope, bounded reason, failed attempt count) while fan-out continues; a failed delivery consumes the idempotency key, reporting `DUPLICATE` with no further dead letter on re-publish or replay.
- Delivery Gate 7 bounded synchronous retry and explicit dead-letter re-delivery: an immutable `RetryPolicy` (1-10 attempts; the default bus keeps a single attempt) retries a failing handler immediately with no delay before dead-lettering it, and `redeliver` accepts only a currently recorded dead letter, resolves it on success, and on renewed exhaustion replaces it in place with the accumulated attempt count and latest reason, never appending to the journal or releasing the consumed idempotency key.
- Delivery Gate 7 cancellation propagation: `cancel(correlationId)` is idempotent and monotonic with no resume, and a cancelled correlation is refused admission before subscription lookup, idempotency, and dispatch on every path — publish, replay, and re-delivery — reporting a scope-level `CANCELLED` outcome that names no subscription, invoking no handler, consuming no idempotency key, creating no dead letter, and appending nothing to the journal; the bus reads no payload to decide delivery, so `ControlSignal.CANCEL` stays a consumer semantic.
- Delivery Gate 7 run-to-completion delivery ordering: a pending queue and a single drain loop replace nested dispatch, so a publication made from inside a handler is queued and reports the scope-level `ENQUEUED` status while the draining top-level `publish` or `replay` returns the whole ordered cascade; delivery order equals publication order, no subscriber observes an effect before its cause, every publication reaches drain-owned admission, replay-caused cascades inherit non-journaling mode, a correlation cancelled mid-cascade refuses entries queued behind it while an in-flight fan-out stays atomic, and an `Error` abandons the cascade entirely.
- Delivery Gate 7 deterministic pending-queue backpressure: immutable `BackpressurePolicy` bounds waiting publications from 1 through 4096 with a finite default; capacity exhaustion reports scope-level `BACKPRESSURED` without blocking, journaling, handler invocation, idempotency consumption, dead-letter creation, or cancellation mutation; accepted work remains FIFO and replay delivers the prefix that fits while reporting the refused suffix without growing the live journal.
- Delivery Gate 7 transport-neutral IPC boundary: immutable `TransportMessage` carries one existing destination and envelope unchanged through provider-neutral `MessageTransport`; `TransportOutcome` distinguishes hop-level `ACCEPTED`, `BACKPRESSURED`, and `UNAVAILABLE` from Message Bus delivery and bounds refusal reasons without consuming bus state.
- Delivery Gate 7 is Contract Verified after fresh reassessment: `WorkPayload.allowedTools` bounds both each name and collection cardinality, and all six scope items plus all four exit criteria remain supported by focused contract evidence.
- Backoff or delayed retry, pause/resume, run-scoped or causation-graph cancellation, priority ordering, competing queue consumers, threading, journal persistence, concrete IPC adapters, and production wiring remain outside these verified contracts.

### Integrated

- Gate 8 worker-over-real-execution path under `com.enhancer.runtime`: `DurableAgentRunWorker` wired with the real `AgentLoopAgentRunExecution` over one shared `FileSystemRunRecordStore` and real filesystem queue/runtime/checkpoint/evidence stores drives a verified claim and its dependent to `VERIFIED_COMPLETED` with really persisted, resolvable RunRecords and a cleared checkpoint, and a digest-mismatch claim to `FAILED` with the dependent blocked and the next cycle claiming nothing (`FileSystemAgentLoopWorkerIntegrationTest`). No production caller wires the worker into the CLI yet; executed work is bounded to reading and digest-verifying the approved source document until the `WorkPayload` execution-input extension lands.
- Gate 8 durable queue-to-AgentRun dispatch under `com.enhancer.runtime`: one existing active WorkItem or newly persisted ready claim flows through `DurableAgentRunDispatcher` into the exact matching durable Goal, named AgentRun planning/readiness prefix, and current fenced lease; partial runtime persistence is recoverable by idempotent re-entry, both filesystem stores recover the same WorkItem and lease, mismatches fail closed before runtime mutation where applicable, and no worker, queue completion, result, effect, or cross-store transaction is claimed.
- Runtime package boundary: neutral verification lifecycle values in `com.enhancer.kernel`, application-layer AgentRun finalization, package-private AgentRunState completion behind an explicit transition port, and an enforced acyclic application/run/verification/loop/kernel dependency direction with unchanged RunRecord schema and behavior.
- Gate 1 in-process Tool isolation capacity: default ToolExecutor instances share a process-wide ceiling of 64 actual live workers, hold capacity until real thread termination, and fail closed with typed terminal evidence before creating another thread when saturated; this bounds accumulation but does not terminate stuck code or replace future process isolation.
- Runtime text and mutable-file resource boundaries: valid supplementary Unicode survives every bounded Evidence/Tool/CLI/Workspace truncation point, while governed file and persisted-artifact reads enforce configured byte ceilings during consumption rather than trusting mutable preflight size metadata.
- Gate 7-to-Gate 8 work-message queue path: one repository-derived approved task and real Gate 6 Workspace snapshot flow through `WorkMessagePublisher`, the real in-process queue, journal, and replay behavior into `WorkItemAdmissionHandler` and the unchanged Gate 8 `WorkItem`; this named path is Integrated, while Gate 7 as a whole remains Contract Verified because the other payload, destination, reliability, causation, and transport branches lack named real production connections.
- Delivery Gate 6 metadata-only immutable Workspace snapshot contract under `com.enhancer.workspace`: approved task source revision provenance, typed source metadata, explicit Available/Stale/Unavailable states, deterministic ordering, bounds, temporal validation, and versioned canonical SHA-256 identity; connected to the real Context Reader upstream and the view, producer, and query downstream through `WorkspaceCollectionIntegrationTest` and the production CLI suites.
- Delivery Gate 6 read-only `ProjectBrainView` aggregate: composed from one real snapshot, one real `ProjectContext`, and the real persisted `RunRecord` of a real governed run, with derived memory freshness and approved-task mismatch rejection.
- Delivery Gate 6 graph projection contract: five typed node kinds, six endpoint-checked edge kinds over the Decision, Architecture, Dependency, Task, and Execution relationship domains, snapshot-keyed versioned projections, and element provenance with derived rebuild status; populated exclusively through the real producer chain in integration and production-CLI tests.
- Delivery Gate 6 `TaskImpactQuery`: answers the task-to-decision-to-code-to-test chain over really-produced graphs, naming the real stored execution, with deduplication, modified-artifact-restricted `VERIFIED_BY` traversal, and rebuild status derived from every traversed element; transitive `DEPENDS_ON` closure remains deferred by decision.
- Delivery Gate 6 `AcceptedDecisionProjector`: accepted decisions parsed from a real decision log through the real Context Reader and merged into the production graph output.
- Delivery Gate 6 `RunRecordMetadataCollector` and store `references()` listing: observations produced against the real filesystem RunRecord store, with a really-persisted prior record observed on the production CLI path and corruption surfaced as explicit `UNAVAILABLE`.
- Delivery Gate 6 `TaskJustificationProjector` and the optional `Justified By` task-document section: explicit references to accepted-decision headings projected into `JUSTIFIED_BY` edges with task-document provenance and snapshot-relative freshness, with strict rejection of empty, non-bullet, duplicate, or unresolved references; the first real reference resolved on the actual repository through the production CLI path.
- Gate 6 boundaries that remain outside these integrations: source payloads, Git/diagnostics/selection/terminal adapters, graph persistence, confidence metadata, and modifies/verified-by/supersedes/depends-on projection, each requiring its own evidence source.

- Delivery Gate 6 repository-memory Workspace path: `RepositoryMemorySnapshotCollector` derives a real snapshot from really-loaded Context Reader memory, and the composed `ProjectBrainView` explains a real governed run including explicit divergence detection.
- The collector reads no files, retains no content, derives the `ApprovedTaskRevision` from the same loaded memory, and reuses `WorkspaceSnapshot.capture` for identity and bounds.
- Delivery Gate 0 authority-preserving foundation lifecycle integration.
- Repository Context Reader with seven `.ai/` documents followed by eight canonical root documents.
- Deterministic Task Planner using Delivery Gate/Specified - Next grammar and explicit proposal state.
- Single-pass Assisted Development Loop.
- Repeated Agent Loop completion, failure, iteration, and stagnation exits.
- Bounded `ToolResult` and `VerificationEvidence` invariants.
- Delivery Gate 1 bounded read-only Tool Execution Boundary.
- Immutable `ToolRequest` with correlation identity and arguments.
- Immutable `ExecutionPolicy` with deny-over-allow policy, project root, size, timeout, and cancellation boundaries.
- Unique in-process `ToolExecutor` registry with bounded structured failure conversion.
- `ReadFileTool` request-to-policy-to-executor-to-real-file-to-result flow.
- Relative-path, traversal, real-path containment, regular-file, size, and strict UTF-8 checks.
- Delivery Gate 2 atomic complete-evidence persistence and restart-safe resolution.
- UUID run/evidence identities, opaque references, creation time, UTF-8 byte length, and SHA-256 metadata.
- Missing, malformed, oversized, length-mismatched, digest-mismatched, and invalid-UTF-8 evidence rejection.
- Large `ReadFileTool` output connected through `EvidenceRecorder` to a resolvable full-output reference.
- Delivery Gate 3 Tool-result-driven Agent Loop integration.
- `AgentRunState` with approved task, pending request, last result, explicit status, and deterministic progress key.
- `AgentRunController` orchestration over an existing executor, immutable policy, and external failure classifier.
- Successful Tool execution stops at `AWAITING_VERIFICATION`; retryable and terminal failures remain distinct.
- Existing maximum-iteration and stagnation exits operate over real Tool results.
- Repository-derived `ApprovedTask` identity, approval evidence, and Tool-name scope.
- Structured Tool failure codes and a standard retry policy without prose parsing.
- SHA-256 evidence content identity and semantic progress independent of storage references.
- Private Agent run construction with public ready-state creation only.
- Delivery Gate 4 sequential independent verification and durable RunRecord replay.
- Typed Verified, Rejected, Unverified, and Not Performed decisions with structured reason codes.
- Deterministic read verifier over inline or integrity-checked referenced complete evidence.
- Executed Tool request retention across worker terminal states.
- Verified-only completion through `AgentRunFinalizer` outside the worker controller.
- Immutable policy snapshot and decision recorded with task, request, Tool result, expected digest, verification, iterations, and stop reasons.
- Atomic versioned RunRecord envelopes with SHA-256 integrity and restart-safe replay.
- Controller-bound execution policy retained in the non-publicly constructible `AgentRunResult`.
- RunRecord lifecycle validation that rejects policy-history substitution and impossible worker, verification, result, and stop-reason combinations.
- Gradle 9-compatible explicit JUnit Platform Launcher runtime and workspace-local default test temporary storage.
- Invocation-isolated Tool workers that prevent interruption-ignoring timeout starvation.
- Millisecond-positive and nanosecond-representable execution-policy timeouts.
- Complete-envelope Evidence and RunRecord integrity digests covering version, timestamp, declared length, and content/payload.
- Strict RunRecord UTF-8 encoding and bounded, real-root-contained, strict UTF-8 startup-context loading.

### Operational

- Delivery Gate 5 first supported local CLI over the integrated read-only vertical slice.
- `EnhancerCli run` requires explicit governed project, active task identity, target, expected digest, evidence root, and RunRecord root inputs.
- `EnhancerCli replay` resolves integrity-checked records without Tool re-execution or chat history.
- Stable process exit codes, 4096-character diagnostics, verified-only completion, and persist-before-report behavior.

### Operational Governance

- Constitution 1.1 Kernel and Document Driven Development.
- Explicit lifecycle, authorization, fresh-evidence, self-hosting, recovery, and amendment rules.
- Git-backed project memory and session handoff.
- RED failures are classified against active task authority, accepted decisions, Architecture, and repository settings before aligned missing implementation proceeds to the minimum GREEN change.

## Accepted Product Direction

- Enhancer OS is an event-driven AI development platform, not a Chat -> Tool -> Stop wrapper.
- The target platform includes Desktop, CLI, API, Workspace, Project Brain, Memory, MCP Server/Client, Agent Runtime, Event/Message Bus with IPC adapters, Skill Engine, Plugin Marketplace, Model Router, Scheduler, and governed Cloud Sync.
- Event Bus defines domain semantics, Message Bus defines delivery, and IPC is a transport adapter for the same versioned envelope.
- Runtime Agents will communicate through queues rather than direct Agent-to-Agent calls after the messaging boundary exists.
- Agent orchestration escalates only as needed from one worker to sequential work, Producer-Reviewer, bounded fan-out/fan-in, expert routing or supervisor allocation, and at most one subordinate coordination layer.
- One Kernel coordinator owns terminal run state; every worker shares an immutable input snapshot and approved task revision through typed versioned handoffs with bounded authority, budgets, evidence, and recovery state.
- Archon `263cf365` and meta-harness `ccab9a6` are pinned design references, not runtime, prompt, Skill, storage, provider, or governance dependencies.
- Workspace will expose governed file, Git, diagnostic, terminal-metadata, and selection snapshots; Project Brain will combine them with repository memory and RunRecords while preserving provenance.
- The owner's rough 20-25% foundation estimate is qualitative planning context, not verified maturity or completion evidence.
- Product milestones are V1 AI Development Experience, V2 AI Development Platform, and V3 AI Operating System.
- Product milestones describe user-visible outcomes, while Delivery Gates define dependency-ordered implementation and promotion; their numbering is not a claim that every V1 surface precedes all V2 foundations.
- The AI Kernel target owns Agent/workflow lifecycle, context and memory resources, locks and leases, scheduling, cancellation, recovery, policy, verification gates, and audit state.
- Project Brain will expose rebuildable Decision, Architecture, Dependency, Task, and Execution graph projections while Git and canonical documents remain authoritative.
- Agent plugins, Skills, Tools, and workflows are distinct extension types with separate authority and provenance.
- The Model Router target selects approved local or remote providers using capability, data classification, policy, cost, latency, context, and availability; sensitive code defaults local.
- Self-hosting development means applying Enhancer's governed workflow to its own repository; local or hybrid model execution is a separate provider-routing capability.

## Not Yet Integrated Or Operational

- Prompt and LLM invocation.
- Remaining Workspace adapters, Project Brain graph persistence, Event/Message Bus production wiring, concrete IPC adapters, broader Agent Runtime and Scheduler production paths, and Model Gateway.
- Project Brain graph storage and impact reasoning, Dependency Analyzer, Workflow Engine, Agent Marketplace, and privacy-aware hybrid model routing.
- Skill loading runtime, plugins, MCP, multi-agent, background execution, Cloud Sync, and governed self-improvement.
- CI/CD and released distribution.

## Current Delivery Position

- Delivery Gate 0: Integrated.
- Delivery Gate 1: Integrated.
- Delivery Gate 2: Integrated.
- Delivery Gate 3: Integrated.
- Delivery Gate 4: Integrated.
- Delivery Gate 5: Operational.
- Delivery Gate 6: Integrated by the 2026-07-15 re-scope-and-promotion decision; diagnostics, terminal-session, and active/selected-file observation moved to Gate 12.
- Delivery Gate 7: Contract Verified after a fresh Integrated maturity assessment. The work-message queue/journal/replay/idempotency path is Integrated, but result/control/handoff and non-empty-causation flows, topic and failure/retry/dead-letter/cancellation/cascade-ordering/backpressure branches, and `MessageTransport` remain contract-only. No concrete adapter, durable bus, or supported messaging entry point exists.
- Delivery Gate 8: Specified - Next; `WorkItem` admission, the dependency-ready single-worker queue, durable schema-v1 queue state/restart recovery, the durable one-Goal/one-AgentRun lifecycle, fenced single-owner lease/expiry recovery, durable queue terminal disposition, RunRecord-backed result-path finalization, the in-process Scheduler worker (connection 3a), the AgentLoop-backed execution port, and the `WorkPayload` execution-input extension are Contract Verified; the durable queue-to-lifecycle dispatch path and the worker-over-real-execution path are Integrated. `DurableAgentRunWorker` now drives claim, real Gate 1-4 Tool execution with independent digest verification, acknowledgement, finalization, and disposition in one recoverable cycle over a durable cycle-intent checkpoint, and a WorkItem can declare an arbitrary governed target through the caller-supplied payload execution input; worker process isolation (3b), a concrete local IPC adapter (3c), effect records/fencing, retries, and broader production wiring (no CLI/Agent-Loop caller wires the worker yet) do not yet exist.
- Gate 6 `WorkspaceSnapshot`, `ProjectBrainView`, graph projection contract, `TaskImpactQuery`, `AcceptedDecisionProjector`, and `RunRecordMetadataCollector` sub-capabilities: Integrated through the fresh promotion audit `gate-6-sub-capability-integration-promotion`, each connected to real upstream and downstream components by named integration evidence.
- Gate 6 `TaskJustificationProjector` and the `Justified By` reference grammar: Integrated; the first real reference resolved on the actual repository through the production composition.
- Gate 6 authority boundary: the exit criterion "Workspace observations cannot override repository authority or grant Tool permission" is pinned by `WorkspaceAuthorityBoundaryIntegrationTest`.
- Gate 6 `TargetFileMetadataCollector`: Integrated on the production CLI path; the run target is observed pre-run with a real containment-checked digest.
- Gate 6 `GitWorkspaceCollector`: Integrated on the production CLI path; one canonical absolute project-external Git executable may collect filter-free index/untracked/deleted metadata, while tracked-worktree diff is explicitly unavailable because verified comparison paths can execute clean filters.
- Gate 6 repository-memory path (real governed run -> real Context Reader memory -> collector -> composed view with divergence detection): Integrated through `WorkspaceCollectionIntegrationTest`.
- Gate 6 run-evidence graph production path (real governed run -> real snapshot -> producer -> impact-query answer naming the real stored execution): Integrated through the extended `WorkspaceCollectionIntegrationTest`.
- Gate 6 production view composition: Operational for the governed read-only CLI scenario; each run observes at most 256 recent records and reports bounded snapshot metadata when post-persist reporting is available.
- Gate 6 production graph composition: Operational for the governed read-only CLI scenario; graph inputs are preflighted before work, duplicate document/target artifacts collapse, and post-persist view/graph/query failure cannot change the durable RunRecord-derived exit code.
- Enhancer has one Operational read-only scenario; the broader Agent Runtime remains planned.
- Gate 0 integration audit is verified without a production correction or second orchestrator and does not displace Gate 6.


## Known Limitations

Durable caveats on the state claimed above. These are properties of the current
system, not open tasks; each is retired only by a bounded increment of its own.

- The CLI trusts an externally supplied expected digest; its origin is explicit and auditable but not signed.
- Evidence and RunRecord envelopes detect corruption but are not encrypted, signed, remotely replicated, or automatically cleaned up.
- The CLI uses the existing 64 MiB per-artifact/in-memory ceiling, five-second Tool timeout, five-iteration loop ceiling, and three-transition stagnation threshold. Evidence has no time-based retention or automatic cleanup contract.
- Atomic stores do not fsync parent directories and therefore make no power-loss durability claim.
- Permanently stuck Tool workers consume isolation capacity until process restart; the runtime contains them finitely but cannot terminate them.
- Two privilege-dependent symbolic-link containment tests are skipped on this Windows host; two Windows junction tests execute and pass against the same production real-path guards.
- Gradle remains at Wrapper 8.4. The known Gradle 9 test-runtime deprecation is removed, but an actual major Wrapper upgrade requires a separate compatibility task.
- Gate 5 is a bootstrap CLI, not the future multi-interface control surface planned for Gate 12.

## Verification Evidence

Per-increment RED/GREEN results, regression counts, lint status, and promotion
outcomes are recorded in `docs/verification-log.md` in append order. This document
states the current position; that log holds the evidence behind it.
