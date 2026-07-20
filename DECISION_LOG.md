# Decision Log

## Accepted Decisions

### 2026-07-20: Enforce Document Ownership With A Structural Test And State The Store Write-Root Contract Exactly

Status: Accepted Decision

Context:

- The single-document ownership rule was accepted the same day but only stated in prose. A follow-up audit found six documents still asserting gate maturity: `README.md`, `docs/08-Multi-Agent.md`, `docs/10-Roadmap.md`, `docs/11-Architecture.md`, and `docs/rfcs/RFC-0009-Multi-Agent.md`.
- Every one of them claimed Gate 7 was `Specified - Next`, which stopped being true when Gate 8 took that marker. The same fact had been copied to five places and all five drifted together — the exact failure the ownership rule exists to prevent, reproduced while the rule was already written down.
- The repository already had the mechanism for this class of problem: `RuntimePackageBoundaryTest` makes an architectural constraint executable rather than aspirational.
- A separate audit finding claimed the CLI's `--evidence-root` and `--run-record-root` were an unconfined write surface contradicting a documented `.enhancer/` guarantee. Re-reading the sources showed the opposite: the 2026-07-15 Gate 5 decision requires those roots as explicit caller inputs, `README.md` calls `.enhancer/` "the example runtime directory", and the stores already refuse a symbolic-link root through `NOFOLLOW_LINKS` and only create freshly generated UUID-named entries. The design is deliberate; the defect was that no document stated the contract precisely enough to prevent that misreading.

Decision:

- Add `DocumentOwnershipTest` under `com.enhancer.architecture`, alongside the existing package-boundary test, asserting that no Markdown document outside `PROJECT_STATE.md` claims gate maturity and that no document outside `CURRENT_TASK.md` carries a `## Next Task` heading.
- Exempt the documents that legitimately carry maturity language: `PROJECT_STATE.md` as owner; `ROADMAP.md`, which owns the `Status: Specified - Next` grammar `RepositoryTaskPlanner` parses; and the append-only historical records `DECISION_LOG.md`, `CHANGELOG.md`, `docs/verification-log.md`, and `docs/superpowers/**`, whose entries were true when written and are never revised.
- Match the maturity claim on a gate-scoped subject rather than on the vocabulary alone, so a document may still define or discuss the maturity levels without asserting one.
- Replace the six stale claims with references to `PROJECT_STATE.md` instead of updating them, per the rule that a duplicate is deleted rather than synchronized.
- State the store write-root contract exactly in `ARCHITECTURE.md` and `README.md`: the roots are explicit caller inputs, deliberately not confined to the project root, and `.enhancer/` is an example layout rather than an enforced property; each store normalizes its root, refuses a symbolic-link root, and only creates new UUID-named entries, so it can add to a caller-named directory but cannot overwrite or delete what is already there. Read-side containment remains the separate and stricter boundary.
- Do not add project-root confinement to the stores. It would contradict the accepted Gate 5 input model and break the existing `CliArgumentsTest` case that deliberately uses sibling roots.

Rationale:

A governance rule that nothing checks decays at the speed of the fastest-moving document, and this one decayed within a day of being written. Making it executable costs one structural test and converts a review question into a build failure. The write-root finding is the mirror image: the code was right and the prose was imprecise, so the fix is to make the prose say what the code actually guarantees rather than to change working behaviour to match a misreading.

Consequences:

- Adding gate maturity to any document except `PROJECT_STATE.md` now fails the build with the offending file, line, and matched text.
- The test enforces the gate-scoped form of the claim; a non-gate-scoped maturity sentence can still pass, so review remains necessary for that narrower case.
- The exempt list is itself a maintained decision: adding a document to it must be justified, since exemption removes the guarantee for that file.
- The symbolic-link root refusal in both filesystem stores remains untested. A test would require symbolic-link creation privilege, which this Windows host denies and which already causes two existing skips, so it would give no signal here and is left as named follow-on work.
- Out of scope: confining store roots, the `GitWorkspaceCollector` output-cap and timeout tests, and the undocumented public types found in the same audit.

### 2026-07-20: Give Every Project Fact One Owning Document And Separate Verification Evidence From Current State

Status: Accepted Decision

Context:

- Six root documents were rewritten in almost every commit: `SESSION_HANDOFF.md` and `PROJECT_STATE.md` in 17 of the last 20, `CURRENT_TASK.md`, `CHANGELOG.md`, and `ARCHITECTURE.md` in 13, and `DECISION_LOG.md` in 12. Parallel feature branches therefore conflicted in all six on every increment.
- The mechanical cause was triple-writing, not file size. `Repository State` and `Current Maturity` existed in both `PROJECT_STATE.md` and `SESSION_HANDOFF.md`; per-increment verification records existed in both `PROJECT_STATE.md` and the `### Gate N` subsections of `ARCHITECTURE.md`; the next task was stated in `PROJECT_STATE.md`, `SESSION_HANDOFF.md`, and `CURRENT_TASK.md` at once.
- The duplicates had already diverged. The three next-task statements disagreed; `SESSION_HANDOFF.md` instructed the next agent to confirm a task ID that no longer existed and named a superseded branch; its "not yet committed" claims were false; and the recorded test-source count was 66 against an actual 65.
- `prompts/SESSION_CLOSE.md` required `SESSION_HANDOFF.md` to contain completed work, current state, next task, decisions, and verification commands — it instructed the duplication directly, so no amount of manual cleanup would have held.
- Verification records were 679 of the 861 lines of `PROJECT_STATE.md` (79%). They are immutable evidence written once per increment, not state, and they crowded out the hand-authored maturity judgment that is the document's actual value.
- `DECISION_LOG.md` cannot be split into per-decision files without a code change: `AcceptedDecisionProjector` requires it as one document in repository memory, `RequiredProjectDocument` hard-codes its path, and `TaskJustificationProjector` matches `## Justified By` bullets against its `###` headings by exact string.

Decision:

- Establish in `CONSTITUTION.md` Section 4 that every fact has exactly one owning document, that a document references rather than restates a fact it does not own, and that a discovered duplicate is deleted rather than synchronized.
- Bind three ownership rules explicitly, each because it has already produced a contradiction: the next task belongs to `CURRENT_TASK.md` alone; capability maturity belongs to `PROJECT_STATE.md` alone; delivery history belongs to git and `CHANGELOG.md` alone.
- Add `docs/verification-log.md` as the append-only home for per-increment verification evidence, and move the 58 historical verification and assessment sections there from `PROJECT_STATE.md`, preserving append order and content byte for byte.
- Reduce `PROJECT_STATE.md` to current state, the maturity judgment behind it, and a `Known Limitations` register; reduce `SESSION_HANDOFF.md` to working-tree facts and next-agent instructions; strip per-gate maturity trailers and ordinal increment narration from `ARCHITECTURE.md`.
- Rescue the completion-conflict root-cause analysis and the Option A/B/C queue-capacity alternatives from `SESSION_HANDOFF.md` into `ARCHITECTURE.md`, retaining the Option letters so the existing decision cross-reference stays resolvable.
- Rewrite the duplication-causing instructions in `prompts/SESSION_CLOSE.md`, `AGENTS.md`, and `.ai/workflow.md`, and state that `.ai/workflow.md` is the operational expansion of Constitution Section 6 rather than a competing sequence.
- Keep `DECISION_LOG.md` as a single document. Splitting it is a Project Brain code change, not a documentation change, and requires its own bounded task.

Rationale:

The churn was caused by an instruction to duplicate, so the fix had to be to the instruction, not to the documents. Ownership is enforceable at review time by a single question — does this document own this fact — whereas "keep the documents synchronized" has already been shown to fail silently and produce contradictory copies. Separating immutable evidence from mutable state also restores the signal in `PROJECT_STATE.md`: what remains is exactly the judgment that no test, decision entry, or run record can reproduce.

Consequences:

- `PROJECT_STATE.md` drops from 861 to 172 lines and `SESSION_HANDOFF.md` from 414 to 41; the removed content is preserved in `docs/verification-log.md` or in git.
- An increment now appends evidence once and edits current-state documents only where a fact actually changed, so parallel branches no longer conflict in six files by construction.
- `docs/verification-log.md` grows without bound by design. It is never read for current state and is not one of the required startup documents, so its size does not affect context loading.
- No production or test code changes; `RequiredProjectDocument`'s 15 paths, the `DECISION_LOG.md` heading grammar, and the `## Justified By` exact-match coupling are all preserved.
- Out of scope: splitting `DECISION_LOG.md`, generating `PROJECT_STATE.md` from its sources, any SQLite or database projection, and the residual maturity language still present in `.ai/architecture.md`.

### 2026-07-20: Record Caller-Supplied WorkPayload Execution Input Enabling Arbitrary-Target Worker Execution

Status: Accepted Decision

Context:

- The AgentLoop-backed execution port derives its target and expected digest from the approved task revision behind one private seam, bounding executed work to re-reading the task document; the same-day decision deferred the payload extension and named its open question: which producer supplies the target and digest at publish time.
- The CLI's authority model already answers that shape: the caller supplies `target-path` and `expected-sha256` as explicit governed inputs, distinct from the approved task document that binds the work.
- Deriving the pair from Workspace snapshot observations would treat observation-time digests as approval authority and couple publication to observation states; both are wrong — observations are evidence, not authority.
- Both filesystem stores already serialize an optional string with a presence flag (`causationId`), and the terminal-disposition increment established the precedent of revising schema v1 in place for the unreleased artifact with pre-existing snapshots failing closed.

Decision:

- Extend `WorkPayload` with one optional nested `ExecutionInput(targetPath, expectedContentSha256)` component — `targetPath` bounded non-blank (max 1024 characters), the digest 64 lowercase hex — plus a three-argument convenience constructor delegating to empty so every existing call site stays valid.
- Expose the `WorkItem.executionInput()` projection; append a presence flag plus the two strings after `allowedTools` in both filesystem serializers, revising schema v1 in place with no version bump.
- Give `WorkMessagePublisher.publish` an overload carrying `Optional<WorkPayload.ExecutionInput>`; the caller supplies the execution input explicitly and the publisher adds no validation beyond the payload contract.
- Make the port's derivation seam prefer the payload-declared input and fall back to `(sourceDocument, sourceSha256)` when absent; the `ApprovedTask` construction and Goal binding are unchanged.

Rationale:

Caller-supplied execution input matches the only Operational authority model the product has and keeps the envelope the single source of what the work is; the seam localizes the change to one derivation, and the in-place schema revision reuses an accepted precedent instead of inventing migration machinery for an unreleased artifact.

Consequences:

- A WorkItem can now declare an arbitrary governed target with its expected digest, and the durable worker executes it through the same contained read-file, evidence, verification, and RunRecord pipeline; absent input preserves the source-document behaviour exactly.
- Pre-existing local queue/runtime snapshots without the new field fail closed on read (accepted for the unreleased artifact).
- Out of scope: write/mutation Tools, multiple execution inputs, payload-carried plans or scripts, 3b/3c, retry, controls, and schema version bumps.

### 2026-07-20: Record AgentLoop-Backed Execution Port Running The Approved Source Document Through The Gate 1-4 Pipeline Without A Payload Schema Change

Status: Accepted Decision

Context:

- The Gate 8 in-process worker (connection 3a) executes through an injected `AgentRunExecution` port, and every existing implementation is a deterministic test stub persisting a hand-built RunRecord; no production implementation runs real Tool-driven work, so `DurableAgentRunWorker` has never driven a claim to a verifier-produced disposition.
- The real read-file pipeline needs a target relative path (the `read-file` `path` argument) and an expected content SHA-256 (`VerificationRequest.expectedContentSha256`), and the dispatched `WorkPayload` carries neither: it holds only `(taskRevision, snapshotId, allowedTools)`.
- Re-reading the full `ApprovedTask` through `ApprovedTaskReader` is not viable for the Scheduler path: it always reads the current `CURRENT_TASK.md`, hard-fails unless its status is `In Progress`, and cannot reproduce the approved revision — `sourceSha256` detects drift but cannot restore content.
- Resolving execution inputs from `snapshotId` is not viable either: no Workspace snapshot persistence exists, so the identity is not resolvable to content or metadata.
- A side store of execution inputs keyed by workItemId would split "what the work is" away from the envelope, contradicting the rule that the retained envelope is the source of run, task, snapshot, and Tool-scope provenance.
- `DurableAgentRunFinalizer` binds the resolved RunRecord to the Goal on `approvedTask.taskId()` plus `approvedTask.sourceDocument()` and reads `record.verification().status()` to derive the queue disposition, so any port must persist through the same `RunRecordStore` and satisfy that binding.

Decision:

- Add `AgentLoopAgentRunExecution` under `com.enhancer.runtime` as the first production `AgentRunExecution`: it drives the already-Integrated Gate 1-4 pipeline (`ToolExecutor`/`ReadFileTool` -> `EvidenceRecorder`/`FileSystemEvidenceStore` -> `AgentRunController` over `AgentLoop` -> `DeterministicReadFileVerifier` -> application `AgentRunFinalizer`) and returns `FinalizedAgentRun.storedRecord().reference()`.
- Execute the approved task's own governed source document: the `read-file` target is `taskRevision().sourceDocument()` and the expected content SHA-256 is `taskRevision().sourceSha256()`, so the increment needs no Gate 7 envelope, queue, or runtime serialization change. This matches the product's only Operational scenario (governed read-file plus digest verification), and a digest mismatch is real drift detection: the task document changed between approval and execution.
- Construct the `ApprovedTask` directly from the WorkItem's own fields (`taskId`, deterministic description/approvalEvidence naming the Goal and AgentRun, `allowedTools`, `sourceDocument`); no loader, no `In Progress` coupling, and the finalizer's taskId-plus-sourceDocument binding holds by construction.
- Isolate the derivation of `(targetPath, expectedContentSha256)` from the WorkItem behind one private seam inside the port, so the named follow-on `WorkPayload` execution-input extension changes only that derivation and reuses the whole pipeline assembly.
- Pin the wiring rule: the port must persist through the same `RunRecordStore` instance/root the worker's `DurableAgentRunFinalizer` resolves from, mirroring the existing same-queue-instance rule.
- Defer the `WorkPayload` execution-input extension (arbitrary target distinct from the task document) as the named next increment; it requires deciding which producer supplies the target and digest at publish time (`WorkMessagePublisher` currently derives the payload from the approved task and snapshot alone) and touches the Gate 7 envelope plus both filesystem serializers.

Rationale:

Deriving the execution inputs from data the WorkItem already carries is the smallest honest increment that gives the worker real Tool-driven execution end to end, and the loader and snapshot alternatives are demonstrably broken (In-Progress hard-fail, unresolvable snapshot identity). Extending the payload without a designed producer for the new fields would be a speculative schema change; the private derivation seam keeps that follow-on a data-source swap rather than a rewrite.

Consequences:

- `DurableAgentRunWorker` can now drive one durable claim through real Tool execution, real evidence, real independent verification, and a real persisted RunRecord to a verifier-produced `VERIFIED_COMPLETED` or `FAILED` disposition.
- The executed work is bounded to reading and digest-verifying the approved source document until the payload extension lands; arbitrary-target execution remains named follow-on work.
- Out of scope: the `WorkPayload` execution-input extension and its serializer changes, worker process isolation (3b), a concrete local IPC adapter (3c), write/mutation Tools, retry through additional AgentRuns, cancel/pause/resume, budgets, priority/fairness, multi-agent execution, and schema migration.

### 2026-07-17: Record In-Process Scheduler Worker Driving One Recoverable Claim-To-Disposition Cycle Through A Durable Cycle-Intent Checkpoint

Status: Accepted Decision

Context:

- Gate 8 held every durable piece of one work cycle as separate, tested contracts — durable queue claim plus fenced lease (`DurableAgentRunDispatcher`), fence-checked execution acknowledgement (`DurableAgentRuntime.completeExecution`), and RunRecord-backed result finalization (`DurableAgentRunFinalizer`) — but nothing drove them end to end; the finalizer had no production consumer and explicitly deferred the pre-terminal recovery window (retaining the `runRecordReference` across a crash before finalization) to the connection-3 worker/driver.
- Roadmap connection 3 bundles a process-isolated worker and a selected local IPC adapter; that is more than one bounded increment.
- By the 2026-07-16 fenced-lease decision, Goal/AgentRun identities are caller-supplied and stable, and a repeated dispatcher call with the same WorkItem/Goal/AgentRun/owner is idempotent recovery; the dispatched `WorkPayload` carries only `(taskRevision, snapshotId, allowedTools)` — no target path, expected content SHA, or concrete `ToolRequest`.

Decision:

- Split connection 3 into sub-increments: 3a in-process worker (this delivery), 3b process isolation, and 3c a concrete `MessageTransport` local IPC adapter; deliver 3a only.
- Add `DurableAgentRunWorker` under `com.enhancer.runtime`, driving one scheduling cycle per `runOneCycle(leaseDuration)` call in the authoritative order: cycle-intent (ids) -> queue claim + lease -> RunRecord persisted (ref) -> intent updated with ref -> `completeExecution` -> `finalizeAgentRun` -> queue disposition -> clear intent. No dispatcher, runtime, finalizer, queue contract, or schema change.
- Add a worker-owned durable cycle-intent checkpoint (`PendingFinalization`, `PendingFinalizationStore`, `FileSystemPendingFinalizationStore`): a single bounded, strict-UTF-8, digest-checked, atomically published record holding the worker-generated distinct canonical Goal/AgentRun UUIDs plus the optional `runRecordReference`. The intent is written before the queue claim, so re-entry supplies the same identities and the dispatcher's existing idempotent `recoverMatching` resumes the exact prefix with no second Goal and no orphaned runtime state; no dispatcher change is required.
- Persist the `runRecordReference` into the intent before `completeExecution`, closing the pre-terminal `AWAITING_VERIFICATION` window the finalizer deferred: that window always holds a recoverable reference.
- Execute through an injected `AgentRunExecution` port (`execute(dispatch) -> runRecordReference`); the real `AgentLoop`-backed port is a named follow-on because it needs a `WorkPayload` execution-input extension crossing Gate 6/7 boundaries.
- Route recovery by the runtime state as the source of truth: terminal -> `recoverFinalization`; `AWAITING_VERIFICATION` -> `finalizeAgentRun(ref)`; `EXECUTING`/`READY`/`PLANNING`, an unstarted AgentRun, or a missing runtime (`MissingAgentRuntimeStateException` tolerated) -> re-drive with the same identities, skipping re-execution when the reference is already recorded.
- Fail closed everywhere: an execution or finalizer failure propagates with the intent retained (the stable owner re-acquires its unexpired lease on resume); a cycle that claimed nothing clears its intent and leaves no durable trace.
- Require the dispatcher and finalizer handed to the worker to wrap the same queue instance, because the queue's in-memory revision advances with each persisted mutation and a second instance over the same store diverges after the claim.
- Accept that re-execution on retry orphans the earlier RunRecord (`persist` assigns a random UUID) as an at-least-once consequence with no cleanup contract.

Rationale:

The in-process worker is the smallest step that gives the finalizer and `completeExecution` their first real consumer and closes the queue -> runtime -> result loop on the durable stores before any process or IPC complexity. Caller-owned stable identity persisted before the claim turns crash recovery into the dispatcher's existing idempotent re-entry rather than new coordination, and writing the reference before acknowledgement makes every recovery row derivable from durable state alone.

Consequences:

- One `runOneCycle` call now drives claim, execution, acknowledgement, finalization, and disposition end to end over the four separate durable stores (queue, runtime, RunRecord, checkpoint) with no cross-store transaction claimed; a cycle interrupted at any seam converges under a fresh worker.
- Out of scope: 3b process isolation, 3c IPC adapter, the real `AgentLoop`-backed execution port and its `WorkPayload` extension, retry through additional AgentRuns, cancel/pause/resume, budgets, priority/fairness, multi-agent execution, schema migration, and any capability-maturity promotion beyond Contract Verified.

### 2026-07-17: Record RunRecord-Backed Result-Path Finalization Connecting Verified Outcome To Runtime Terminal State And Queue Disposition

Status: Accepted Decision

Context:

- Gate 8 had three separate durable facts with nothing connecting them: fence-checked execution acknowledgement (`EXECUTING -> AWAITING_VERIFICATION`), a durable Goal/AgentRun terminal transition (`DurableAgentRuntime.recordResult`), and a durable queue terminal disposition (`completeActiveVerified`/`failActive`).
- After a worker acknowledges execution, no component took the independently verified outcome, moved the AgentRun/Goal to its terminal state, and recorded the matching queue disposition; no recovery finished that sequence after an interruption.
- `FileSystemRunRecordStore.persist` assigns a random `run-record/<uuid>` reference, so persist is not idempotent; `ApprovedTask` carries a taskId and sourceDocument but no source SHA; the durable queue's recovery contract requeues an interrupted active WorkItem to pending, so a recovered queue exposes no active slot until the item is claimed again.

Decision:

- Add one durable, idempotent coordinator, `DurableAgentRunFinalizer` under `com.enhancer.runtime`, over the existing durable queue, `AgentRuntimeStateStore`, and `RunRecordStore`; it performs no cross-store transaction and adds no new store or schema.
- Resolve the RunRecord by reference as an input; never persist it in the finalizer (persist is non-idempotent, and the verified outcome originates outside this increment).
- Drive one recoverable order: resolve RunRecord -> runtime terminal (`recordResult`) -> queue disposition, each step guarded by observed store state.
- Derive the queue disposition from the runtime terminal status, never re-derived from the RunRecord (`COMPLETED -> completeActiveVerified`, `FAILED -> failActive`), so the two stores cannot diverge; read `verificationStatus` from the resolved RunRecord and carry it in the `ResultPayload`.
- Bind the RunRecord to the Goal on `approvedTask.taskId()` and `approvedTask.sourceDocument()` (no SHA available) before finalizing; reject a mismatch. On re-`finalizeAgentRun` after the runtime is already terminal, require the stored result's `runRecordReference` to equal the supplied reference; a mismatch is an explicit error, never a silent overwrite.
- Provide two entry points: `finalizeAgentRun(goalId, agentRunId, runRecordReference)` drives the forward path; `recoverFinalization(goalId)` is pure post-terminal recovery that applies only the queue disposition from an already-terminal runtime and needs no reference.
- Honour the queue's recovery contract when applying the disposition: if the terminal work is already in the completed/failed set, treat it as recorded (idempotent); otherwise re-claim the requeued active item before recording the terminal disposition.
- Fail closed on a missing or corrupt RunRecord: record no disposition and leave the run `AWAITING_VERIFICATION` (recoverable). Reject `finalizeAgentRun` on a run that has not acknowledged execution.
- Leave the Scheduler worker/Tool execution and RunRecord production (connection 3), retry through additional AgentRuns, and automatic failure propagation to dependents out of this contract.

Rationale:

Deriving the queue disposition from the runtime terminal status makes divergence between the two durable stores unrepresentable, and resolving (not persisting) the RunRecord keeps the coordinator idempotent under a non-idempotent store. Re-claiming the requeued active item before disposition is the same claim-then-dispose pattern the durable queue's own recovery contract already mandates, so recovery composes without a cross-store transaction.

Consequences:

- The verified outcome now drives both the AgentRun/Goal terminal state and the queue disposition in a recoverable, idempotent order; a failed or unverifiable outcome fails the run and blocks dependents rather than satisfying them.
- Post-terminal recovery is autonomous; only the pre-terminal window still relies on the connection-3 worker/driver to re-supply the same `runRecordReference`, and retaining it durably across a crash in that window is that connection's responsibility.
- Correctness rests on the idempotent, ordered, guarded steps, not on atomicity; no released format or capability maturity beyond Contract Verified changes.

### 2026-07-17: Record Durable Queue Terminal Disposition Distinguishing Verified Completion From Failure

Status: Accepted Decision

Context:

- The Scheduler queue recorded only `completedWorkItemIds`, a success-only set that both marked a work item finished and satisfied its dependents' dependencies, so it could not represent failure.
- The runtime lifecycle already distinguishes `COMPLETED` from `FAILED`, but the queue's `completeActive` operation forced every item leaving the active slot into the dependency-satisfaction set.
- The prior 2026-07-16 decision named terminal queue disposition as the next bounded contract and required verified completion and failure to stay distinct before Scheduler capacity or dependency state changes.
- The on-disk schema-v1 queue envelope is length-prefixed with no forward-compatibility marker and rejects trailing bytes; it is an unreleased, git-ignored local `.enhancer/` artifact.

Decision:

- Introduce a terminal `WorkItemDisposition` enum (`VERIFIED_COMPLETED`, `FAILED`); only `VERIFIED_COMPLETED.satisfiesDependencies()` is true.
- Split the single `completeActive` operation into `completeActiveVerified` (enters `completedWorkItemIds`, the dependency-satisfaction set) and `failActive` (enters a separate `failedWorkItemIds` set that never satisfies dependents), across the in-memory queue, the durable wrapper, and the filesystem store.
- Preserve `completedWorkItemIds` meaning exactly: verified-completed = dependency-satisfying. Extend the state partition invariant to `pending + active + verified + failed = admissionOrder`, with verified and failed disjoint.
- Keep the active slot occupied through verification (reaffirming the 2026-07-16 Option A decision); both terminal dispositions release the slot at the terminal point. Option B (a non-terminal waiting state) remains deferred.
- The queue records disposition only, never a failure reason; the inspectable cause remains in the runtime/RunRecord, linked by `workItemId`. A failed dependency leaves dependents blocked, not automatically failed.
- Revise schema-v1 in place with no version bump. Because the envelope rejects trailing bytes, any pre-existing local queue snapshot fails closed as `CorruptedSchedulerQueueStateException` on read; this is accepted for the unreleased local artifact and promises no backward compatibility.
- Leave `ResultPayload`/RunRecord result wiring, dispatcher-driven disposition recording, retry, automatic failure propagation, and Option B out of this contract.

Rationale:

Naming the terminal disposition at the type level closes the overloaded-"completion" ambiguity that caused the earlier connection conflict. A separate failed set makes dependent blocking a consequence of the existing dependency check rather than new propagation logic, and keeps the change minimal against the schema-v1 single-worker design.

Consequences:

- Failed work is durably distinct from verified completion and never satisfies dependents; the result path (next connection) has an unambiguous recoverable disposition target.
- The at-least-once requeue window, where a runtime failure precedes a persisted queue disposition, remains open until the RunRecord-backed result-wiring increment closes it with idempotent-suffix recovery.
- Pre-existing local schema-v1 queue snapshots become unreadable; no released format is affected.

### 2026-07-16: Separate Execution Acknowledgement From Verified Queue Completion And Sequence Remaining Connections

Status: Accepted Decision

Context:

- The documented Gate 8 next increment says to couple fence-checked AgentRun execution completion to durable queue acknowledgement.
- The implemented `DurableAgentRuntime.completeExecution` transition stops at `AWAITING_VERIFICATION`; it does not create a terminal AgentRun or Goal.
- The implemented Scheduler queue `completeActive` operation both releases the active slot and adds the WorkItem to `completedWorkItemIds`, which is the dependency-satisfaction set used to release dependent work.
- Treating those operations as equivalent would make execution receipt indistinguishable from verified logical completion and would conflict with the repository-wide rule that worker success and `AWAITING_VERIFICATION` are not completion.
- The remaining result, control, worker, effect, IPC, retry, and handoff connections are named across the Roadmap and Architecture, but their dependency order and owning gates are not recorded in one place.

Decision:

- Treat fence-checked `EXECUTING -> AWAITING_VERIFICATION` as durable execution acknowledgement only. It releases the lease but MUST NOT by itself call queue completion, satisfy WorkItem dependencies, or imply Verified, Completed, or successful external effects.
- Keep the current schema-v1 queue item active while its runtime is `AWAITING_VERIFICATION`. Releasing the active slot before terminal verification requires a separately accepted queue-state design with an explicit non-terminal waiting state; it must not reuse `completedWorkItemIds`.
- Make the next bounded Gate 8 contract a durable queue terminal-disposition model that distinguishes verified completion from failure. Only verified completion may enter the dependency-satisfaction set; failed disposition may release capacity only through an explicit policy and must not satisfy dependents.
- After that contract, integrate the result path in this recoverable order: persist and resolve the RunRecord; carry a matching `ResultPayload`; persist the AgentRun/Goal terminal state; then persist the matching queue terminal disposition. Re-entry after interruption must be idempotent, and the later artifact must be reconstructible from the earlier durable state without claiming a cross-store transaction.
- Sequence the remaining connections by dependency: bounded result finalization; process-isolated worker plus a selected local IPC adapter; durable control handling; effect ledger and idempotency/fencing; retry through additional immutable AgentRuns; and Gate 13 typed handoff/multi-agent execution after the single-agent runtime is Operational.
- Keep Gate ownership explicit: Gate 7 owns message and transport delivery, Gate 8 owns runtime/queue state and recovery, Gate 11 owns Tool/extension execution controls, Gate 12 owns authenticated user controls, and Gate 13 owns multi-agent handoffs and concurrency.
- Correct the Roadmap's stale statement that Agent Runtime and Scheduler require a detailed RFC before becoming active. Existing bounded Gate 8 work remains valid; detailed RFC work is required before process workers, concrete IPC production wiring, broader control/effect/retry policy, or Operational promotion.
- Change no production code or capability maturity in this documentation task.

Rationale:

Execution acknowledgement, independent verification, and Scheduler dependency satisfaction are different facts. Preserving them as separate durable transitions prevents a worker receipt from becoming an implicit completion authority. An explicit connection sequence also satisfies the contract-continuation rule by naming each integration consumer without activating later gates prematurely.

Consequences:

- The previously documented direct execution-completion-to-queue-completion increment is withdrawn before implementation.
- The current queue may remain occupied while verification is pending; improving concurrency requires an explicit waiting-state contract rather than weakening completion semantics.
- Result, worker, control, effect, retry, IPC, and handoff paths remain unimplemented and must be activated through separate bounded tasks with fresh evidence.
- `.ai/architecture.md` must mirror this boundary compactly but cannot replace the canonical Architecture and Roadmap.

### 2026-07-16: Bridge One Durable Queue Claim Into One Recoverable Leased AgentRun

Status: Accepted Decision

Context:

- Gate 8 independently persists a dependency-ready single-worker queue and one fenced Goal/AgentRun lifecycle, but no production contract connects a claimed WorkItem to runtime ownership.
- The queue store and Agent runtime store are separate atomic artifacts. Treating their combined transition as one transaction would make an unsupported cross-store atomicity claim.
- A process may stop after the queue claim, Goal creation, AgentRun creation, readiness, or lease acquisition. The integration must resume from each persisted boundary without invoking a Tool or hiding duplicate work.

Decision:

- Add one in-process durable coordinator that selects the queue's existing active WorkItem or durably claims the next ready WorkItem before creating or advancing runtime state.
- Require caller-supplied stable canonical Goal and AgentRun identities, one bounded lease owner, and one bounded lease duration. Validate caller-controlled identity and lease metadata before changing queue state.
- Create a missing Goal from the exact claimed WorkItem. If the Goal already exists, recover it and require the retained WorkItem to equal the queue's active WorkItem exactly.
- Advance only the missing prefix of `ACCEPTED -> PLANNING -> READY -> EXECUTING`: create Goal, begin the named AgentRun, mark it Ready, and acquire its fenced lease. Each underlying transition retains its existing persist-before-exposure boundary.
- Treat a repeated call with the same active WorkItem, Goal, AgentRun, and current lease owner as idempotent recovery and return the existing unexpired lease without renewal. A different AgentRun, mismatched WorkItem, different unexpired owner, Awaiting-Verification state, or terminal state fails closed.
- Claim the queue before runtime creation. If any later runtime write fails, retain the active queue claim and any durable runtime prefix; a repeated call resumes that prefix. Queue recovery may requeue the interrupted active WorkItem, after which the same admission-ordered item can be claimed again.
- Return an immutable dispatch value naming the queue, exact WorkItem, Goal, AgentRun, and current lease. Do not complete the queue item or execute a worker.
- Defer queue completion coupling, Tool/worker execution, result-message handling, retry/multiple AgentRuns, effect records/fencing, compensation, cross-store transactions, multi-process coordination, and parent-directory power-loss durability.

Rationale:

Persisted prefixes plus idempotent re-entry provide truthful crash recovery without a distributed transaction or rollback that the current stores cannot guarantee. Selecting an already-active item also permits same-process recovery after a runtime-store failure, while exact WorkItem comparison prevents an existing Goal identity from being rebound to different authority.

Consequences:

- A runtime-store failure can leave a visible active queue claim; this is intentional recoverable state rather than hidden rollback.
- The queue remains active after lease acquisition because execution and completion acknowledgement are separate future contracts.
- This coordinator is a single-process composition boundary. It adds no lock, worker authority, external effect, or Integrated/Operational gate-level claim beyond the named queue-to-lifecycle path.

### 2026-07-16: Fence One AgentRun Owner Before Worker Execution

Status: Accepted Decision

Context:

- Gate 8 now persists one Goal and one AgentRun lifecycle, but `EXECUTING` has no durable owner, expiry, or stale-writer defense.
- A queue claim is not a worker lease. Process interruption after `EXECUTING` would otherwise leave the run blocked forever or allow a replacement owner to race the original owner.
- Worker execution, heartbeats, retry, external effects, and multi-process coordination remain separate concerns. The lease contract must be independently verifiable before any worker is connected.

Decision:

- Add one optional immutable `AgentRunLease` to the schema-v1 AgentRun state and one persisted monotonically increasing last-issued fence token to the aggregate.
- Permit lease acquisition only from `READY`. Acquisition atomically advances the fence token, records a bounded non-blank owner identity, an issued time, and an exclusive expiry, and moves the AgentRun to `EXECUTING`.
- Bound lease duration from 1 millisecond through 24 hours. Time comes from an injected UTC `Clock`; caller-supplied wall-clock timestamps do not enter transition authority.
- Require matching owner and fence token for lease renewal and the transition from `EXECUTING` to `AWAITING_VERIFICATION`. Both operations fail closed at or after expiry.
- Renewal retains the same fence token, replaces the issue/expiry window, and must extend the existing expiry.
- On explicit reclaim or runtime recovery, an expired executing lease is persistently cleared and the AgentRun returns to `READY`. A later owner receives a strictly greater fence token; the expired owner can never write with its stale token.
- Preserve persist-before-exposure and exactly-one revision advancement for acquisition, renewal, execution completion, and expiry reclamation. Persistence failure leaves the prior state authoritative.
- Defer worker/Tool execution, heartbeat, retry or a second AgentRun, cancellation/pause, effect commits, lease-aware external systems, multi-process locking, clock-skew protocol, and parent-directory power-loss durability.

Rationale:

A persisted monotonically increasing fence is the smallest mechanism that distinguishes a current owner from a delayed or partitioned former owner. Returning expired execution to `READY` provides observable orphan recovery without inventing a replacement worker or retry policy. Injected time keeps expiry tests deterministic and prevents callers from choosing their own authority timestamp.

Consequences:

- Restart before expiry preserves the current owner and executing state; restart at or after expiry durably reclaims the run to `READY`.
- Lease possession grants no Tool or task authority. It only authorizes lifecycle writes already allowed by the retained WorkItem.
- In-memory and filesystem adapters remain single-process implementations; cross-process mutual exclusion and effect fencing require later work.
- Gate 8 remains `Specified - Next`; only the fenced single-owner lifecycle sub-capability may become Contract Verified.

### 2026-07-16: Persist One Goal And One AgentRun Lifecycle Before Adding Worker Ownership

Status: Accepted Decision

Context:

- Gate 8 has durable dependency-ready queue state, but it has no durable Goal or AgentRun lifecycle above a claimed WorkItem.
- The existing Gate 3 `AgentRunState` is a bootstrap Tool-loop state tied to one approved request. Reusing it as the event-driven Scheduler lifecycle would mix two different responsibilities and reopen the package boundary just made acyclic.
- Leases, fencing, retries, worker processes, budgets, effect idempotency, and schema migration are separately specified concerns. Combining them now would make restart and ownership semantics impossible to verify independently.
- Gate 7 already defines unchanged work and result envelopes. The runtime can preserve those messages as data without treating delivery or possession as authority.

Decision:

- Add one immutable schema-v1 Gate 8 runtime aggregate containing exactly one `RuntimeGoal`, its exact admitted `WorkItem`, and at most one `RuntimeAgentRun`.
- Give Goal and AgentRun distinct canonical UUID identities. The Goal retains the exact WorkItem as its authority and provenance source; runtime state cannot add or widen task, snapshot, logical-run, capability, or allowed-Tool data.
- Use deterministic Goal states `ACCEPTED`, `ACTIVE`, `COMPLETED`, and `FAILED`, and AgentRun states `PLANNING`, `READY`, `EXECUTING`, `AWAITING_VERIFICATION`, `COMPLETED`, and `FAILED`.
- Permit only the forward path `PLANNING -> READY -> EXECUTING -> AWAITING_VERIFICATION -> COMPLETED|FAILED`. This first schema supports one AgentRun and no retry or replacement.
- Require a terminal result envelope to carry `ResultPayload`, match the retained WorkItem's logical run, correlation, task, and work-message causation, and retain that exact result envelope. Only `VERIFIED` may complete; every other verification status produces explicit failure.
- Persist every successful lifecycle transition before exposing it, with a monotonic revision and immutable recovery of the exact last state. Persistence failure leaves the previous in-memory and durable revision authoritative.
- Store one bounded strict-UTF-8 integrity-checked binary state per Goal through atomic create/replace publication. Missing, corrupt, oversized, trailing, structurally invalid, or unsupported state fails closed.
- Defer retry, cancellation, pause/resume, leases, fencing, heartbeat, worker execution, effect records, RunRecord resolution, multiple AgentRuns, schema migration beyond v1, history retention, multi-process coordination, and parent-directory power-loss durability.

Rationale:

One reference-preserving Goal and one deterministic AgentRun are the smallest durable lifecycle that can sit above the existing queue without pretending that a queue claim is worker ownership. A typed result envelope closes terminal state structurally while keeping verification and RunRecord production outside this state store. Persist-before-exposure makes restart behavior observable before leases and workers are introduced.

Consequences:

- Restart restores the last persisted lifecycle state exactly and performs no automatic transition because no lease or ownership expiry exists yet.
- This increment consumes the existing WorkItem and ResultPayload contracts but does not integrate a Message Bus result path or supported runtime entry point.
- Failed or non-verified results remain terminal in schema v1; a later accepted retry design must introduce another AgentRun rather than mutating terminal history.
- Gate 8 remains `Specified - Next`; only the durable Goal/AgentRun lifecycle sub-capability may become Contract Verified.

### 2026-07-16: Make Runtime Persistence And Verification Dependencies Acyclic

Status: Accepted Decision

Context:

- Production packages currently form cycles: `loop` imports `run` through `AgentRunFinalizer`, `run` imports `loop` lifecycle types, `loop` imports verification decisions, and `verification` imports the approved-task contract from `loop`.
- The single Gradle module compiles, but the cycles obstruct later Kernel, Runtime, Verification, and Persistence module boundaries.
- Moving every Agent/runtime type or introducing new modules now would be larger than necessary and could disrupt the verified vertical slice.

Decision:

- Move `VerificationDecision`, `VerificationStatus`, and `VerificationCode` unchanged into neutral `com.enhancer.kernel` lifecycle contracts.
- Move `AgentRunFinalizer` unchanged in behavior from `com.enhancer.loop` to `com.enhancer.application`, where orchestration may depend on loop, verification, and run persistence.
- Preserve `ApprovedTask`, Agent run state, and stop reasons in `loop` for this increment. `verification` and `run` may depend on `loop`; `loop` must no longer depend on `verification` or `run`.
- Require `kernel` to depend on none of `application`, `loop`, `run`, or `verification`; require `run` not to import `verification`.
- Preserve enum constant names, RunRecord binary values, CLI behavior, verification decisions, public value invariants, and storage compatibility.
- Add a source-structure regression test that fails if the forbidden dependency directions return.
- Defer Gradle module extraction, broader Kernel naming, ApprovedTask relocation, API compatibility shims for unreleased package names, and persistence SPI separation.

Rationale:

Moving three neutral decision values and one orchestration service is the smallest extraction that turns the current strongly connected component into a directed dependency graph. It creates a credible future module seam without redesigning runtime behavior or durable formats.

Consequences:

- Source imports change, but persisted enum names and runtime semantics do not.
- The resulting direction is `application -> run/verification/loop/kernel`, `run -> loop/kernel`, `verification -> loop/kernel`, and `loop -> kernel`.
- The project remains one Gradle module; independent modules require a later task.

### 2026-07-16: Bound In-Process Tool Isolation Capacity

Status: Accepted Decision

Context:

- A Java thread that ignores interruption cannot be forcibly terminated safely in-process.
- The current ToolExecutor isolates each invocation in a fresh daemon worker so one timed-out Tool does not starve the next invocation, but every permanently stuck Tool can retain one thread indefinitely.
- Long-running Scheduler use would turn repeated malicious or broken Tools into unbounded process thread growth.
- Process isolation is the real termination boundary but is not yet available and would exceed a bounded Gate 1 hardening increment.

Decision:

- Introduce one process-wide isolation capacity shared by default across all ToolExecutor instances, bounded to 64 live isolated workers.
- Acquire one slot before creating a worker thread and release it only when that worker thread actually terminates. Timeout, interrupt, executor close, and `shutdownNow` do not pretend to release a slot while an interrupt-ignoring Tool is still running.
- Refuse a Tool invocation before thread creation when capacity is exhausted and return a typed terminal `ISOLATION_CAPACITY_EXHAUSTED` Tool failure with bounded evidence.
- Preserve invocation isolation: a timed-out worker below the global ceiling does not block an independent next invocation.
- Permit an injected smaller shared capacity only for deterministic package-level tests; production uses the single process-wide default.
- Defer process workers, OS-level termination, per-plugin quotas, Scheduler admission/backpressure integration, health recovery, and operator controls.

Rationale:

A finite fail-closed ceiling prevents unbounded daemon-thread accumulation without making a false claim that Java can kill arbitrary code. Holding the slot until actual termination ensures the accounting describes real process resource occupancy rather than timeout bookkeeping.

Consequences:

- After 64 concurrently non-terminated isolated workers, new Tool invocations fail terminally until a worker actually exits or the process restarts.
- The ceiling is containment, not recovery; a process containing permanently stuck workers still requires operator restart.
- Gate 1 Tool execution remains Integrated, and long-running Scheduler workers still require process isolation before Operational promotion.

### 2026-07-16: Harden Text And File Bounds During Production Operations

Status: Accepted Decision

Context:

- `VerificationEvidence` and several diagnostic surfaces truncate by UTF-16 index and can split a supplementary Unicode surrogate pair, producing malformed Java text that strict UTF-8 persistence rejects.
- `ReadFileTool`, `ProjectContextReader`, and `TargetFileMetadataCollector` preflight file size but then read or hash without enforcing the byte ceiling during the operation. The filesystem Evidence, RunRecord, and Scheduler queue resolvers have the same size-check/read window.
- Preflight metadata is useful for early refusal but cannot enforce a resource bound after another process changes the file.
- Tool process isolation, package modularization, and power-loss directory durability are separate architectural concerns and must not be hidden inside a text/file-bound correction.

Decision:

- Add one neutral Unicode truncation utility with prefix and suffix operations that preserve existing UTF-16 length ceilings while never returning half of a surrogate pair at the truncation boundary.
- Apply it to VerificationEvidence tails, ToolExecutor failure diagnostics, CLI bounded output and values, and bounded Workspace failure reasons.
- Add one neutral bounded-input utility that reads or hashes at most the configured bytes and probes at most one additional byte to detect overflow.
- Keep existing preflight size checks for early diagnostics, but route production file content, digest, and persisted-envelope reads through the bounded operation.
- Treat an in-operation target-file overflow as an explicit Unavailable observation; the other governed readers and stores fail with IOException or their existing typed corruption/failure conversion.
- Preserve strict UTF-8 decoding and persistence, existing configured byte ceilings, evidence identity over complete content, and current authority boundaries.
- Defer Tool process isolation/global stuck-worker policy, lifecycle package extraction, parent-directory synchronization, and power-loss fault-injection to separate accepted decisions.

Rationale:

Unicode truncation is a representation boundary and byte limits are operational resource boundaries. Centralizing both prevents repeated off-by-one and TOCTOU mistakes while keeping the change behaviorally narrow. Reading at most the limit plus one detection byte enforces memory and hashing work independently of mutable file metadata.

Consequences:

- A Unicode-safe tail may contain one fewer UTF-16 code unit than the configured maximum when the exact boundary would split a supplementary character.
- File growth after the metadata check cannot cause unbounded allocation or hashing on the corrected paths.
- This task does not solve stuck in-process Tool threads, package cycles, or power-loss durability.

### 2026-07-16: Persist Gate 8 Queue Transitions Before Exposing State

Status: Accepted Decision

Context:

- Gate 8 has a deterministic in-memory single-worker queue, but a process interruption loses pending, active, completed, dependency, and admission-order state.
- A claimed item cannot remain permanently active after restart because the current queue has no lease, heartbeat, worker process, or fence token.
- Persisting only identifiers would be insufficient because restart recovery must reproduce the exact existing `WorkItem`, unchanged Gate 7 envelope, dependency readiness, and Tool-scope provenance.
- A full AgentRun state machine, effect ledger, lease protocol, migration framework, or multi-process coordinator would exceed this increment.

Decision:

- Give each durable queue a caller-supplied canonical UUID and bind its first admitted item to one logical run identity; reject later work from another logical run.
- Define a versioned immutable queue snapshot containing the queue identity, revision, capacity, optional logical run identity, total admission order, ordered pending work, optional active work, and completed work identities.
- Persist the exact `WorkItem`, `MessageEnvelope`, `WorkPayload`, task revision, allowed-Tool scope, required capability, and dependency identities needed to reconstruct scheduling without creating new authority.
- Store one bounded integrity-checked binary snapshot per queue through strict UTF-8 encoding and atomic replacement. Creation must not overwrite an existing queue; updates must advance exactly one revision; missing, corrupt, structurally invalid, oversized, or unsupported-version state fails closed.
- Stage every enqueue, successful claim, and completion against a copy, persist the next snapshot before exposing or adopting the transition, and retain the prior in-memory and durable state when persistence fails.
- On restart, retain pending and completed state and move any previously active item back into pending position according to original admission order, persist that recovery transition, and allow later re-claim. This is at-least-once queue recovery, not exactly-once effect execution.
- Defer leases, fencing, heartbeat, worker execution, effect idempotency records, failure/retry/cancellation policy, history retention, schema migration beyond v1, and multi-process coordination.

Rationale:

Persist-before-exposure prevents a caller from observing a queue transition that cannot survive restart. Re-queuing an interrupted active item avoids hidden work loss or a permanently blocked queue while honestly accepting possible replay until later lease, fence, and effect-idempotency contracts exist. A complete bounded snapshot is simpler and safer than a partially recoverable event log at this maturity.

Consequences:

- A storage failure rejects the transition and leaves the last durable revision authoritative.
- Recovery may cause an interrupted item to be offered again; no external side-effect deduplication is claimed.
- The v1 store has one current snapshot and no automatic cleanup or historical revisions.
- The durable queue sub-capability may become Contract Verified while Gate 8 remains `Specified - Next`.
- Durable Goal/AgentRun lifecycle state and fenced worker ownership remain the next larger Gate 8 concerns.

### 2026-07-16: Start Gate 8 Scheduling With A Dependency-Ready Single-Worker Queue

Status: Accepted Decision

Context:

- Gate 8 is the sole `Specified - Next` gate and already has immutable `WorkItem` admission, but no Scheduler consumer.
- A full durable AgentRun state machine, lease protocol, recovery mechanism, or worker implementation would exceed the smallest coherent next increment.
- Dependency readiness requires a completion signal, while forward references and arbitrary graph mutation would require broader cycle and persistence policy.

Decision:

- Add an immutable queued-work value containing one existing `WorkItem` and up to 256 unique canonical dependency work identities.
- Reject self-dependency and require every dependency to have been admitted earlier or already completed. This gives the first queue deterministic dependency validation and prevents cycles by construction without claiming general graph-cycle analysis.
- Add an in-memory run-scoped queue bounded to 4096 total work-item admissions, preserving admission order and selecting the first dependency-ready item.
- Permit at most one active work item. A claim while work is active returns no item; explicit completion of the matching active identity releases dependents.
- Preserve the exact `WorkItem` and its unchanged Gate 7 envelope. Queue admission and completion create no task approval, Tool authority, or execution result.
- Defer persistence, state versioning, failure/retry/cancellation, priority/fairness, leases/fencing, checkpoints, orphan recovery, worker execution, threading, and production wiring.

Rationale:

This is the smallest consumer that makes `WorkItem` useful to Gate 8 while preserving deterministic single-agent sequencing. Requiring dependencies to exist before their dependent prevents unknown and cyclic graphs without introducing a mutable graph service. One explicit active slot establishes the Scheduler boundary without pretending that a claim is a durable lease.

Consequences:

- Work must be admitted in dependency order in this first queue.
- Completion is an in-memory Scheduler fact, not a verified AgentRun terminal state or durable record.
- The queue sub-capability may become Contract Verified, but Gate 8 remains `Specified - Next`.
- Durable queue state and restart-safe recovery are the immediate next increment.

### 2026-07-16: Assess Gate 7 Integrated Maturity Against Every Real Connection

Status: Accepted Decision

Context:

- Gate 7 is Contract Verified and now has one named integration path from a real Gate 6 approved Workspace input through the in-process queue into Gate 8 `WorkItem` admission.
- The Roadmap scope is broader than that path: it includes the four payload kinds, topic and queue delivery, idempotency/retry/cancellation/dead-letter/replay/ordering/backpressure, and a transport-neutral IPC interface.
- Architecture defines Integrated maturity as connection to real upstream and downstream collaborators. Contract tests, interface existence, and one work-message path cannot be silently generalized to branches that the path does not exercise.

Decision:

- Run a documentation-only Gate 7 maturity assessment with fresh focused, full-regression, strict-lint, and self-hosting evidence.
- Map each Roadmap scope item and exit criterion separately to Contract Verified or Integrated evidence.
- Treat a Gate 7 sub-path as Integrated only when a named test connects its real upstream and downstream production collaborators.
- Promote Gate 7 as a whole only if the evidence supports every connection required by the gate-level scope. Otherwise retain Contract Verified and record the precise missing connections.
- Do not implement a missing adapter, payload flow, reliability scenario, Scheduler behavior, or production entry point inside the assessment.

Rationale:

Maturity labels are evidence claims, not rewards for aggregate test count. Requiring scope-by-scope real connections prevents a work-only queue integration from overstating result, control, handoff, topic, reliability, or transport integration while still recognizing the value of the path that now exists.

Consequences:

- The assessment may conclude that the work-message queue path is Integrated while Gate 7 remains Contract Verified.
- A concrete IPC adapter is not automatically mandatory merely because the interface is in scope; however, the interface cannot be called Integrated until a real transport implementation consumes it.
- Gate 8 remains the sole `Specified - Next` product gate regardless of the assessment outcome.

### 2026-07-16: Add Product Journeys Evaluation And Layered Security Across Delivery Gates

Status: Accepted Decision

Context:

- Delivery gates describe dependency-ordered capabilities, but a collection of mature components does not by itself prove that a user can complete a development job safely and understand the result.
- The Roadmap names interfaces, runtime reliability, multi-agent work, security controls, and release packaging in separate gates without one cross-cutting evaluation contract.
- Universal exactly-once execution cannot be guaranteed across arbitrary external Tools and side effects; truthful reliability comes from at-least-once delivery combined with stable idempotency, fenced ownership, replay-safe effects, and recovery evidence.
- The Constitution already establishes authority, untrusted-input, verification, and amendment rules. Detailed product security, journey, UX, and evaluation guidance belongs in supporting Architecture and Roadmap documents, so no constitutional amendment is needed for this clarification.

Decision:

- Add a cross-cutting Product Journey and Evaluation Track alongside, not in place of, Delivery Gates.
- Begin with four canonical journeys: governed bug repair, bounded feature delivery, evidence-backed codebase explanation, and interrupted-run recovery. Each journey must define its user-visible outcome, approvals, evidence, recovery behavior, supported surfaces, and versioned evaluation fixtures.
- Add a fifth product priority: a repeatable evaluation and release-quality harness that records task success, incorrect changes, recovery, cost, duration, user intervention, post-verification regression, and multi-agent delta using explicit denominators and immutable result provenance.
- Require thresholds to be selected and versioned before a release evaluation run. Agent confidence, reviewer self-report, or a single passing test cannot substitute for journey evidence.
- Define Gate 8 delivery as at-least-once with stable idempotency keys, fenced leases, checkpointed recovery, versioned state migration, orphan reclamation, and replay-safe external effects. Do not claim universal exactly-once execution.
- Require all user interfaces to consume one shared Run, approval, verification, evidence, and control API. Use the CLI as the reference surface, add VS Code next for in-context work, and add Desktop later as a supervisory and cross-run view.
- Require a common change-review projection showing plan, changed files and diff, tests/evidence, risks, approvals, recovery, and commit readiness rather than exposing internal Agent topology as the primary UX.
- Treat repository instructions, Tool output, model responses, MCP content, plugins, dependencies, and terminal output as untrusted data at the Architecture layer. Assign concrete enforcement to the owning gates for secret detection, outbound data control, permission manifests, isolation, audit, provenance, disablement, and rollback.
- Require Gate 13 multi-agent promotion to demonstrate improvement over a single-agent baseline on the same versioned task set and comparable budget envelope. Require Gate 16 release evidence to meet versioned journey thresholds in addition to packaging checks.

Rationale:

This keeps capability maturity technically precise while adding a second, user-centered proof dimension. The cross-cutting track prevents isolated features from being mistaken for a usable product, makes reliability claims implementable, and gives every interface and extension the same evidence, approval, and security model.

Consequences:

- Delivery Gate numbers, dependencies, and current maturity states do not change.
- A gate may pass its technical maturity criteria while a product journey remains incomplete; release claims require both applicable gate evidence and journey-quality evidence.
- Numeric thresholds remain unspecified until the evaluation fixtures and baseline measurement task is activated.
- The Constitution remains unchanged. Any future change to its authority or safety semantics still requires the full amendment process and separate explicit user approval.

### 2026-07-16: Prepare Gate 7 Integration Through The First Runtime Admission Path

Status: Accepted Decision

Context:

- Gate 7 is Contract Verified, but Architecture requires a capability to connect real upstream and downstream collaborators in an integration test before it can be called Integrated.
- Gate 6 already produces a real repository-derived `ApprovedTask` and immutable `WorkspaceSnapshot`; Gate 8 now admits one unchanged Gate 7 work envelope as an immutable `WorkItem`.
- Wiring the bus into the supported CLI merely to exercise delivery would add behavior without a Scheduler consumer, while selecting a concrete IPC adapter would prematurely decide endpoint, serialization, authentication, and threading policy.

Decision:

- Add a small production publisher that derives one `WorkPayload` from a matching `ApprovedTask` and `WorkspaceSnapshot`, constructs one existing versioned envelope from explicit deterministic metadata, and publishes it through `InProcessMessageBus` to an explicit destination.
- Reject task-identity or source-document mismatch before publication. Allowed Tools come only from the repository-derived approved task; the publisher creates no approval or authority.
- Add a production `MessageHandler` adapter that turns one delivered work envelope into one `WorkItem` using an injected work-identity supplier, a bounded required capability, and an injected downstream sink.
- Prove the path in a named integration test using the real Context Reader, snapshot collector, bus, journal/replay behavior, admission adapter, and WorkItem contract.
- Keep Gate 7 at Contract Verified during this implementation task. A separate fresh maturity assessment decides whether the evidence supports gate-level Integrated promotion.

Rationale:

This is the narrowest real vertical connection available now: Gate 6 supplies approved provenance and authority scope, Gate 7 carries and delivers it unchanged, and Gate 8 admits it without widening authority. Explicit metadata and injected boundaries keep the integration deterministic and avoid inventing a Scheduler, production CLI behavior, or transport policy.

Consequences:

- Gate 7 gains named integration evidence with real upstream and downstream production collaborators, but no maturity claim changes automatically.
- The publisher is an in-process application boundary, not a supported entry point or a concrete transport adapter.
- The admission handler does not store, order, execute, retry, or recover work; the dependency-ready single-worker Scheduler queue remains a separate Gate 8 task.
- Any mismatch or invalid envelope fails before downstream admission; bus retry and dead-letter behavior remains unchanged.

### 2026-07-16: Start Gate 8 With Immutable WorkItem Admission Over Gate 7 Envelopes

Status: Accepted Decision

Context:

- Gate 8 owns `WorkItem`, dependency scheduling, leases, recovery, and the single worker first, while Gate 7 already owns the versioned envelope and work payload that carry task, snapshot, logical-run, provenance, and allowed-Tool data.
- The existing Gate 3 `AgentRunState` governs one pre-authorized Tool request through execution and verification. Reusing or duplicating it as the Scheduler's durable work identity would mix bootstrap execution state with the later event-driven runtime.
- A full Goal/AgentRun store, dependency queue, lease protocol, or recovery mechanism would exceed the smallest coherent first Gate 8 increment.

Decision:

- Add immutable `com.enhancer.runtime.WorkItem` as the first Gate 8 contract.
- Give each work item a canonical UUID identity distinct from the retained envelope's canonical message identity and one required-capability name bounded to 256 characters.
- Admit only an existing `MessageEnvelope` whose payload is `WorkPayload`; retain the exact envelope rather than copying or flattening its authority and provenance fields.
- Expose logical run identity, approved task revision, snapshot identity, and allowed Tools only as projections of the retained work payload and envelope.
- Create no approval, policy decision, Tool grant, queue state, lifecycle transition, lease, persistence, or worker execution behavior.
- Name the dependency-ready single-worker Scheduler queue as the immediate integration consumer in the next Gate 8 increment.

Rationale:

A Scheduler needs a stable work identity separate from a delivery attempt, but the Gate 7 envelope must remain the authoritative handoff container. Wrapping one unchanged work envelope creates that separation without reopening the message schema or inventing runtime state before queue, persistence, and recovery semantics are decided. Keeping authority fields as projections makes it impossible for the work item constructor to widen them.

Consequences:

- One logical work item may later be delivered or retried through multiple message identities without conflating work identity with transport identity; this first contract admits one initial work message only.
- Non-work result, control, and handoff envelopes cannot enter the Scheduler work-item path.
- Dependencies, states, versions, budgets, deadlines, attempts, leases, fencing, checkpoints, and persistence remain required later Gate 8 increments rather than implicit fields.
- The Gate 8 gate remains `Specified - Next`; only the `WorkItem` admission sub-capability may reach Contract Verified in this task.

### 2026-07-16: Promote Gate 7 To Contract Verified And Advance Gate 8

Status: Accepted Decision

Context:

- The Gate 7 maturity model requires core types, invariants, and focused contract tests for Contract Verified status; it does not require a production caller or real process boundary until Integrated or Operational maturity.
- The prior assessment mapped all six scope items and all four exit criteria, finding only an unbounded `WorkPayload.allowedTools` collection. The completed test-first correction now bounds that collection to 256 unique names.
- Gate 8 is the immediate integration consumer for the message, control, handoff, replay, cancellation, ordering, and backpressure contracts.

Decision:

- Re-run the complete Gate 7 bus contract suite, full regression, strict production lint, and document self-hosting checks before changing maturity state.
- If the fresh evidence passes, promote Delivery Gate 7 from `Specified - Next` to `Contract Verified` and advance the Roadmap's sole `Specified - Next` marker to Delivery Gate 8.
- Treat the provider-neutral `MessageTransport` interface as sufficient for Gate 7 Contract Verified scope. Concrete local-process or remote adapters, wire formats, authentication, persistence, production wiring, and real process hops remain later integration work.
- Make no production-code change. Update only the two actual-Roadmap self-hosting assertions whose contract is the current `Specified - Next` gate, and claim no Gate 7 Integrated, Operational, or Released maturity from this assessment.

Rationale:

Contract Verified maturity is intentionally the boundary between a tested foundation and a connected runtime. Holding Gate 7 open for a concrete adapter would confuse integration evidence with contract evidence and block its named Gate 8 consumer. Advancing the next marker after fresh verification preserves the dependency sequence without overstating what exists.

Consequences:

- Gate 8 becomes eligible for separately authorized bounded contract work; it is not implemented merely because it becomes `Specified - Next`.
- Gate 7 still lacks production publishers/consumers, a concrete adapter, durability, threading, and a supported messaging entry point, so Integrated and Operational claims remain prohibited.
- Any failed or incomplete fresh verification stops the promotion and leaves Gate 7 `Specified - Next`.

### 2026-07-16: Bound Work Payload Tool Scope Cardinality

Status: Accepted Decision

Context:

- The Gate 7 maturity assessment found that `WorkPayload` bounds every allowed-tool name to 256 characters but accepts an arbitrarily large set, so the aggregate message payload has no contract ceiling.
- The Roadmap requires payloads to be bounded or replaced by evidence references before Gate 7 can exit.
- A concrete IPC adapter would not correct this in-memory contract defect and remains outside the current foundation scope.

Decision:

- Add `WorkPayload.MAX_ALLOWED_TOOLS` with a maximum of 256 unique allowed-tool names.
- Accept scopes from 1 through 256 entries and reject larger scopes before copying them into the immutable payload.
- Keep the existing 256-character per-name bound, snapshot/task provenance, immutable set semantics, and authority model unchanged.
- Change no Message Bus delivery, transport, serialization, persistence, scheduling, or production-wiring behavior.

Rationale:

A cardinality ceiling combined with the existing per-name ceiling gives the only collection-bearing payload a finite aggregate bound of at most 65,536 tool-name characters. A limit of 256 follows the existing bus identity bound, remains far above realistic Tool scopes, and closes the recorded exit criterion without inventing a wire format or adapter.

Consequences:

- Existing normal scopes remain source- and behavior-compatible; only previously unbounded scopes above 256 entries are rejected.
- Gate 7 still requires fresh reassessment before any maturity promotion or Gate 8 activation.
- Concrete local-process or remote adapters remain deferred to later integration work.

### 2026-07-16: Separate IPC Transport Acceptance From Message-Bus Delivery

Status: Accepted Decision

Context:

- Gate 7 already has a versioned `MessageEnvelope`, typed destinations, and deterministic in-process delivery semantics, but a later local-process or remote adapter has no provider-neutral boundary through which to carry that route and envelope.
- Reusing `JournaledMessage` would falsely imply that a transport attempt was admitted to the bus journal. Returning `DeliveryOutcome` from a transport would likewise conflate acceptance by one transport hop with remote subscription delivery, which may be asynchronous or unavailable to the sender.
- Endpoint discovery, serialization, framing, authentication, threading, persistence, and concrete adapters have no verified requirement in the current increment.

Decision:

- Add immutable `TransportMessage`, containing exactly one existing `DeliveryDestination` and one existing `MessageEnvelope`; it creates no new authority and does not copy, reinterpret, or flatten the envelope.
- Add a provider-neutral functional `MessageTransport` interface with one `send(TransportMessage)` operation. A configured transport instance owns any peer or channel configuration, so provider endpoints and lifecycle do not enter the domain contract.
- Return a typed `TransportOutcome` directly from the synchronous admission call without copying a second message identity into the result. `ACCEPTED` means only that the adapter accepted responsibility for attempting the hop; it does not mean that a remote bus admitted, journaled, dispatched, or delivered the message.
- Permit explicit `BACKPRESSURED` and `UNAVAILABLE` non-acceptance outcomes with a bounded reason. A non-accepted message consumes no Message Bus delivery, idempotency, cancellation, dead-letter, or journal state; retry timing remains higher-level scheduling policy.
- Keep serialization, protocol negotiation, endpoint discovery, authentication, concrete local-process or remote adapters, buffering, threading, persistence, and production wiring out of this increment.

Rationale:

The smallest honest IPC seam transports the existing route and envelope and reports only ownership of the attempted hop. Separating transport acceptance from subscriber delivery preserves all current bus semantics, avoids promising synchronous remote outcomes, and leaves provider-specific concerns behind later adapters.

Consequences:

- Later adapters can implement one interface without changing message identities, payloads, destinations, or authority semantics.
- A caller must not translate `ACCEPTED` into `DELIVERED`; receiving-side delivery outcomes remain owned by the receiving Message Bus and must travel as explicit messages if a workflow needs them.
- The contract alone crosses no process boundary and proves no adapter, wire format, durability, authentication, or production integration.

### 2026-07-16: Verify Real-Path Boundaries With Junctions And Remove Fictional Evidence Retention

Status: Accepted Decision

Context:

- The only tests covering `toRealPath()` escape rejection attempt symbolic-link creation and are skipped on this Windows host, leaving the production boundary unverified here. Windows directory junctions can exercise the same real-path escape without symbolic-link privilege.
- `EvidenceRetentionPolicy.retentionPeriod` is validated and tested but never read by production. The store neither expires nor deletes evidence, so the API suggests a 30-day lifecycle contract that does not exist.
- Destructive retention needs explicit lifecycle, replay, audit, and cleanup authority. Inventing deletion inside this corrective task would widen behavior beyond the observed defect.

Decision:

- Add Windows-only directory-junction integration tests for both `ReadFileTool` and `ProjectContextReader`. On Windows, junction creation failure is a test failure rather than a skip; other platforms retain the existing symbolic-link coverage and skip only the Windows-specific test.
- Rename `EvidenceRetentionPolicy` to `EvidenceStoragePolicy`, remove `retentionPeriod`, and expose only the actually enforced `maxContentBytes` contract through `EvidenceStore.storagePolicy()`.
- Remove the CLI's unused 30-day constant and update all production and test callers to the truthful storage policy.
- Do not delete, expire, hide, or rewrite existing evidence. A future retention feature requires a separate accepted decision with cleanup authority and replay/audit semantics.

Rationale:

Junctions make the real-path security invariant executable on the host that previously skipped it. Removing an unused promise is safer than implementing surprise deletion: the API should name only behavior the store enforces today.

Consequences:

- Both lexical traversal and Windows real-path redirection are covered by fresh tests at the actual boundaries.
- Evidence remains durable until external/user-authorized lifecycle management exists; storage size is bounded per artifact, not by age or aggregate capacity.
- The policy type and accessor are intentionally source-incompatible inside this pre-release repository, preventing callers from continuing to rely on a fictional retention period.

### 2026-07-16: Bound Workspace RunRecord Observation To A Recent Window

Status: Accepted Decision

Context:

- `RunRecordMetadataCollector` resolves every durable record into every new Workspace snapshot. Since `WorkspaceSnapshot` is capped at 4096 observations, the 4,080th run with the current fixed sources cannot even begin.
- Resolving all prior record payloads on every run also creates cumulative quadratic payload I/O, while deletion is neither authorized nor required for observation.
- Full reference listing and point replay remain useful storage capabilities and need not be constrained by the Workspace projection window.

Decision:

- Add `RunRecordStore.recentReferences(limit)` as a bounded newest-first metadata query and retain `references()` for complete diagnostic/replay enumeration.
- Limit `RunRecordMetadataCollector` to the 256 most recent record references. Resolve only those payloads, preserving explicit unavailable observations for selected corrupted or missing entries.
- Implement filesystem recency selection with one directory scan and a bounded priority queue over no-follow file modification metadata; do not load every RunRecord payload or delete any artifact.
- Validate requested reference-window bounds from 1 through the store collection limit and keep output deterministic with the reference as the tie-breaker.

Rationale:

A fixed observation window keeps snapshot cardinality and per-run payload resolution bounded without inventing destructive retention. The complete store remains available for direct replay and future pagination/index work, while Workspace answers honestly describe a recent execution horizon.

Consequences:

- New runs no longer hit the snapshot's 4096-observation wall because of accumulated records, and record payload reads are capped at 256 per collection.
- Filesystem directory enumeration is still linear in artifact count; a durable summary index or pagination may replace it when scale justifies the additional consistency protocol.
- File modification time determines filesystem recency selection; envelope integrity and stored time are still validated when a selected reference is resolved.

### 2026-07-16: Keep Durable Run Outcome Primary Across Project-Brain Composition

Status: Accepted Decision

Context:

- The CLI currently persists a finalized RunRecord before composing Project Brain output, but any later graph exception escapes to the top-level internal-error handler. A durable `COMPLETED`/`VERIFIED` run can therefore return exit 70.
- Required documents targeted by the run appear as both `REPOSITORY_DOCUMENT` and `REPOSITORY_FILE`; graph projection currently flattens both identities into duplicate artifact nodes. Duplicate accepted-decision headings and the 256-vs-1024 identifier-bound mismatch provide additional post-persist failure triggers.
- Project Brain counts are derived diagnostics. They must not redefine the already-finalized governed execution outcome.

Decision:

- Project accepted decisions and task-justification edges and preflight the graph's non-execution inputs before creating evidence, invoking a Tool, or persisting a RunRecord. Invalid project metadata is a usage/configuration failure with no durable run.
- Collapse multiple repository observations of the same source identity into one artifact node, preferring the target-specific `REPOSITORY_FILE` observation over the general repository-document observation.
- Align graph node identifiers with the Workspace source identifier bound of 1024 characters.
- After persistence, treat Project Brain view, graph, query, and diagnostic composition as optional reporting. Catch a reporting runtime failure, emit bounded `brainStatus=UNAVAILABLE` metadata, and return the exit code derived from the durable RunRecord unchanged.

Rationale:

The transaction boundary is the durable RunRecord. Validation that can be performed from the snapshot and repository memory belongs before external work, while failures in reconstructible diagnostics after that boundary must degrade diagnostics, not rewrite execution history. Identity collapse preserves one graph artifact per repository path without discarding the more specific target observation.

Consequences:

- A malformed or oversized Project Brain input fails before Tool execution and persistence.
- A post-persist brain-reporting defect remains observable in bounded output while replay and the CLI exit code stay consistent with the durable record.
- Graph projection remains rebuildable and non-authoritative; no RunRecord schema or Tool authority changes.

### 2026-07-16: Restrict Git Observation To A Trusted Executable And Filter-Free Plumbing

Status: Accepted Decision

Context:

- `git status --porcelain` and a worktree `git diff --no-textconv` still consult a repository-configured required clean filter when resolving stat-dirty content, so an observed repository can execute a command before Tool policy applies.
- Invoking `git` by name also delegates executable discovery to process-launch rules. On Windows, an Enhancer process started in the untrusted repository can therefore resolve a repository-controlled `git.exe` before PATH.
- Focused adversarial verification established that `git ls-files --stage --deleted --others --exclude-standard` avoids the configured clean filter, while both `ls-files --modified` and `diff-files --raw --no-ext-diff --no-textconv` re-enter that pipeline. A filter-free tracked-worktree comparison has therefore not been established.

Decision:

- Resolve Git from absolute PATH directory entries, canonicalize the executable, and reject any candidate whose real path is contained by the real observed project root. If no such executable exists, publish explicit `UNAVAILABLE` observations instead of attempting discovery by name.
- Reduce command authority to one fixed, shell-free, read-only `ls-files` invocation for index, deleted, and untracked metadata. Keep `--no-optional-locks`, invocation-scoped fsmonitor disable, inherited `GIT_*` removal, project discovery ceiling, timeout, output cap, and discarded stderr.
- Publish the `GIT_DIFF` observation as explicitly `UNAVAILABLE` without starting a process until a filter-free tracked-worktree method is separately established and verified.
- Treat Git output only as untrusted bytes entering a SHA-256 digest. Retain no paths, file contents, configuration, or command output.
- Do not rewrite repository configuration or temporarily disable filters. Safety comes from command choice and trusted executable resolution, not mutation of the observed project.

Rationale:

Observation must be less privileged than the project it inspects. An absolute executable outside the real project removes current-directory/PATH-name hijacking, while index/stat plumbing avoids the conversion pipeline that executes clean filters. Returning unavailable is safer than silently widening command authority when trusted resolution cannot be established.

Consequences:

- The status digest remains sensitive to index, deleted, and untracked path metadata. Tracked unstaged content modifications are deliberately not observed, and the diff observation explains that safety limitation instead of implying availability.
- PATH itself is still host input, not cryptographic installation provenance. Absolute-entry and project-containment checks close the repository-controlled lookup vector; signature/package verification remains out of scope.
- One external command remains authorized only inside `GitWorkspaceCollector`; no other component receives command authority.

### 2026-07-16: Bound Pending Publications With Deterministic Non-Blocking Refusal

Status: Accepted Decision

Context:

- Run-to-completion ordering replaced recursive delivery with an explicit FIFO pending queue, but that queue is unbounded and a handler cascade can retain arbitrarily many envelopes before the drain catches up.
- The bus is deliberately synchronous and single-threaded. Blocking a publisher cannot create useful flow control when the publisher is the handler currently holding the drain; it would deadlock rather than relieve pressure.
- Cancellation already establishes that work refused before admission must not be journaled or consume delivery state, or a later replay could create a side effect that never originally occurred.
- The user authorized the next recorded Gate 7 increment on 2026-07-16.

Decision:

- Add an immutable `BackpressurePolicy(maxPendingPublications)` bounded from 1 through 4096; existing constructors use a finite default of 4096, and an overload accepts both retry and backpressure policies.
- Add scope-level `DeliveryStatus.BACKPRESSURED`. When the pending queue is at capacity, refuse the submission immediately without journaling it, invoking a handler, consuming an idempotency key, creating a dead letter, or changing cancellation state.
- Keep accepted work FIFO. A re-entrant publisher receives `ENQUEUED` for accepted work or `BACKPRESSURED` for refused work; the draining caller receives outcomes only for the admitted cascade, while the publisher that attempted refused work receives its refusal directly.
- Apply the same capacity to replay batch submission: accept the deterministic prefix that fits, refuse the remaining entries, drain the accepted prefix, and return the refusal outcomes without appending any replay entry or caused publication to the live journal.
- Make no thread wait, timer, retry, eviction, priority, persistence, IPC, or production-wiring policy in this increment. A caller may retry a refused envelope later under its own higher-level scheduling authority.

Rationale:

A finite admission bound closes the only memory-growth hazard introduced by run-to-completion ordering without pretending a synchronous handler can block safely. Immediate typed refusal preserves determinism and makes overload visible to the exact caller that attempted the publication. Treating refusal like cancellation before admission preserves journal replay truth and keeps a later explicit retry possible because no idempotency state was consumed.

Consequences:

- The pending queue never contains more than the configured bound, but the journal, idempotency keys, cancellation set, dead letters, and returned outcome collections remain process-local and require separate retention or persistence work.
- A handler must inspect the immediate result of a caused publication if it needs to react to overload; the top-level drain cannot fabricate an outcome for work it never admitted.
- Replay of a batch larger than the configured capacity is partial but deterministic and observable; the caller may retry the refused suffix explicitly.
- Gate 7 remains `Specified - Next` and Contract Verified only; no production caller uses the bus yet.

### 2026-07-16: Order Delivery By Running Each Publication To Completion Before Its Cascade

Status: Accepted Decision

Context:

- Topic fan-out already follows registration order, the journal already follows publication order, and replay already re-dispatches in journal order, so the ordering the Roadmap names has no content unless it addresses the one real hazard left.
- That hazard is re-entrant publication: `publish` dispatches synchronously, so a handler that publishes during its own delivery causes a nested dispatch. The child is delivered in full before the parent's fan-out finishes, and every subscriber registered after the publishing one observes the effect before its cause.
- The bus is synchronous and single-threaded, so ordering can only be established by the order in which the bus itself admits work, not by any scheduler.

Decision:

- Give the bus a pending queue and a single drain loop. A publication is appended to the queue; a top-level call drains the queue to exhaustion, and a call made while a drain is already running only enqueues.
- Add `DeliveryStatus.ENQUEUED` for a re-entrant publication accepted for later delivery, and return the whole ordered cascade from the top-level `publish` or `replay` that drained it, so no outcome is lost.
- Route `publish` and `replay` through the same submission and drain path, distinguishing them only by whether the entry is journaled, so ordering holds identically on both.
- Move admission — the cancellation check and the journal append — into the drain loop, so an entry is journaled at the moment it is admitted for delivery and the journal's order is the bus's own total delivery order.
- Generalize the scope-level status concept onto `DeliveryStatus.isScopeLevel()`, now covering `UNROUTED`, `CANCELLED`, and `ENQUEUED`, and keep `DeliveryOutcome` validating that exactly the scope-level statuses name no subscriber.
- Abandon a cascade entirely if an `Error` escapes a drain, clearing the queue rather than leaking a dead cascade into an unrelated later publication.

Rationale:

Run-to-completion is the only ordering a synchronous bus can offer that means anything: it makes delivery order equal publication order and guarantees that a cause is delivered to every subscriber before any of its effects reaches one. Draining from a queue rather than recursing also removes unbounded stack growth from a deep cascade. Journaling at admission rather than at the `publish` call keeps the invariant established by the cancellation decision — the journal records exactly what was admitted, so a fresh-bus replay reproduces exactly the side effects that occurred — and it lets a cancellation raised mid-cascade refuse work already queued behind it, which enqueue-time checking alone could not do.

Consequences:

- A handler observes `ENQUEUED` rather than a delivery result for its own publication, so a handler that needs its child's outcome must read it from the cascade the top-level caller receives.
- A publication is admitted or refused as a whole, so a cancellation raised during a fan-out cannot stop that fan-out; it stops only entries still queued behind it.
- The pending queue is unbounded, which is precisely the gap the backpressure increment will close; it joins the idempotency keys, journal, and cancellation state as process-local in-memory state.
- Re-delivery is unaffected: it targets exactly one subscription, so a publication from its handler has no fan-out to be nested inside and drains normally.
- Ordering remains registration order within a destination; competing consumers and priority ordering stay out of scope.

### 2026-07-16: Propagate Cancellation As A Terminal Correlation-Scoped Delivery Refusal

Status: Accepted Decision

Context:

- The bus delivers, retries, dead-letters, and replays deterministically, but nothing can abandon work in flight: every published envelope is delivered regardless of whether its run was cancelled, and a dead letter stays re-deliverable forever.
- The Roadmap names cancellation propagation as the next Gate 7 concern, before ordering and backpressure.
- The bus is synchronous and single-threaded, so there is no concurrently executing handler to interrupt; cancellation can only decide whether a delivery is admitted at all.
- `ControlSignal.CANCEL` already exists, but it is a consumer-facing payload semantic. Making the bus interpret `ControlPayload` would turn the bus into a consumer and break the sealed-payload decision's intent that consumers, not the transport, exhaust payload kinds.

Decision:

- Key the cancellation scope on the envelope's own `correlationId`, the identity the envelope contract already defines for grouping related messages across hops.
- Add `cancel(String correlationId)` and `isCancelled(String correlationId)` to `InProcessMessageBus`. Cancellation is idempotent and monotonic: once a correlation is cancelled it stays cancelled, and there is no resume.
- Add `DeliveryStatus.CANCELLED` and treat cancellation as admission control that runs before subscription lookup, idempotency, and dispatch: a publication into a cancelled correlation invokes no handler, consumes no idempotency key, creates no dead letter, is not journaled, and reports one scope-level `CANCELLED` outcome.
- Propagate the refusal to every delivery path: `replay` reports `CANCELLED` and skips a journal entry whose correlation is cancelled, and `redeliver` reports `CANCELLED` without invoking the handler while retaining the dead-letter record.
- Generalize the `DeliveryOutcome` invariant from "`UNROUTED` carries no subscriberId" to "a scope-level status (`UNROUTED` or `CANCELLED`) carries no subscriberId; every other status must name the subscription it targeted".
- Keep the bus free of payload interpretation: a handler that receives a `CANCEL` `ControlPayload` may call `cancel(correlationId)` itself, but the bus never reads a payload to decide delivery.

Rationale:

Correlation-scoped refusal is the only cancellation a deterministic single-threaded bus can honestly express, and it is genuine propagation rather than a per-destination flag: one `cancel` reaches every topic, queue, replay, and re-delivery that shares the correlation, and a caused child carrying the parent's correlation inherits it without the bus tracking a causation graph. Refusing admission before journaling is forced by the existing replay contract — journaling a cancelled publication would make a fresh-bus replay produce a side effect that never originally happened, breaking the determinism the journal exists to guarantee. Monotonic cancellation keeps replay reproducible, because a scope's admission decision can never differ between two passes over the same journal.

Consequences:

- Cancellation is terminal: resuming an abandoned correlation requires a new correlation identity, and `ControlSignal.PAUSE`/`RESUME` therefore remain consumer-level semantics with no bus behavior.
- A cancelled publication is invisible to the journal, so the journal records exactly the publications that were admitted for delivery and replay reproduces exactly the side effects that occurred.
- Cancelling mid-fan-out is impossible by construction: a publication is admitted or refused as a whole, so subscribers never observe a partially delivered envelope.
- Cancellation state joins the idempotency keys and journal as unbounded, process-local, in-memory state; bounds and durability wait for the persistence increment.
- Cancellation is scoped to the correlation, not the logical run; cancelling every correlation of a run is the caller's composition until a run-scoped control surface exists.

### 2026-07-16: Add Bounded Synchronous Retry And Explicit Dead-Letter Re-Delivery

Status: Accepted Decision

Context:

- The dead-letter increment made a failed delivery deterministic and terminal: the idempotency key is consumed, re-publishing reports `DUPLICATE`, and no automatic re-delivery exists, so a transiently failing handler permanently loses the delivery.
- The Roadmap names automatic retry with a bounded attempt policy and re-delivery from the dead-letter record as the next Gate 7 increment, before cancellation propagation, ordering, and backpressure.
- The bus is synchronous, single-threaded, and deterministic; timers, delays, and asynchronous scheduling remain out of Gate 7 scope.

Decision:

- Add an immutable `RetryPolicy(maxAttempts)` bounded to 1 through 10 attempts; the bus keeps its no-argument constructor at a single attempt so every existing behavior is unchanged, and accepts a policy through a new constructor.
- Retry synchronously and immediately inside dispatch: a handler `RuntimeException` is retried until the policy's attempts are exhausted, in the same deterministic order with no delay between attempts; success within the policy yields `DELIVERED` with no dead letter, and exhaustion yields one `FAILED` outcome and one dead letter.
- Record the failed attempt count on the dead letter: `DeadLetter` gains a positive `attempts` component naming how many handler invocations failed for that delivery.
- Add explicit re-delivery from the dead-letter record: `redeliver(DeadLetter)` accepts only a dead letter this bus currently records, re-invokes the subscription's handler under the same bounded policy, resolves the dead letter on success (`DELIVERED`, entry removed), and on renewed exhaustion replaces the entry in place with the accumulated attempt count and latest reason (`FAILED`).
- Keep re-delivery outside the journal and outside idempotency: the original publication is already journaled, and the consumed idempotency key stays consumed, so publish and replay still report `DUPLICATE` after a successful re-delivery.

Rationale:

Immediate bounded synchronous retry is the only retry the deterministic single-threaded contract can express without timers, and it composes with the existing failure isolation rather than reinterpreting it: the dead letter remains the terminal record of an exhausted policy, and re-delivery is an explicit, auditable operator action over that record instead of an implicit reinterpretation of consumed idempotency keys. Bounding attempts at ten keeps a pathological policy from turning one publish into unbounded synchronous work.

Consequences:

- Retry storms are impossible by construction: one publish invokes one handler at most `maxAttempts` times, and re-delivery requires an explicit call per dead letter.
- There is no backoff or delay between attempts; a handler needing time-based recovery must wait for a later scheduling increment.
- A successful re-delivery leaves the idempotency key consumed, preserving at-most-once publish semantics; the dead-letter record is the sole re-delivery authority.
- `DeadLetter` gains an `attempts` component, so the record itself proves the bounded policy was applied.

### 2026-07-15: Isolate Delivery Failures To A Terminal Dead-Letter Before Adding Retry

Status: Accepted Decision

Context:

- `InProcessMessageBus` delivered synchronously with per-subscription idempotency but had no failure semantics: a throwing handler aborted fan-out to the remaining subscribers, propagated out of `publish`, and left the failure unrecorded even though the idempotency key was already consumed.
- Retry, cancellation, ordering, and backpressure are the Roadmap's next Gate 7 concerns, but bundling them into one change would exceed the smallest-coherent-increment discipline.

Decision:

- Catch a subscriber handler's `RuntimeException` inside dispatch, record a `FAILED` `DeliveryOutcome`, capture an immutable `DeadLetter` (destination, subscriber, unmodified envelope, bounded reason), continue delivering to the remaining subscribers, and never let the exception escape `publish` or `replay`.
- Consume the idempotency key on a failed delivery so it is terminal for this increment: no automatic re-delivery, and re-publishing or replaying reports `DUPLICATE` with no further dead letter.
- Defer automatic retry, re-delivery from the dead-letter record, cancellation propagation, ordering, backpressure, and persistence to later increments.

Rationale:

Failure isolation plus a terminal dead-letter is the smallest change that makes delivery total — every subscriber gets a deterministic outcome and no failure is silently lost — while keeping the bus deterministic and free of any re-delivery policy the contract does not yet define. Consuming the idempotency key keeps the current at-most-once guarantee intact; the later retry increment will layer explicit bounded re-delivery on top of the dead-letter record rather than reinterpreting existing keys.

Consequences:

- A handler that must not lose work has to be idempotent or externally durable until the retry increment adds bounded re-delivery.
- The dead-letter record is in-memory and process-local; durability and replay-from-dead-letter wait for the persistence and transport increments.
- Only `RuntimeException` is isolated; `Error` still propagates, because it signals a condition the bus should not swallow.

### 2026-07-15: Deliver Gate 7 In-Process Messaging As A Deterministic Journal-Replayable Bus

Status: Accepted Decision

Context:

- The envelope contract named deterministic in-process topic and queue delivery with idempotency and replay as its next consumer, and the orchestration invariants require authorization and provenance to survive every hop.
- Competing queue consumers, retry, dead-letter, ordering, backpressure, threading, and IPC transport introduce non-determinism or scope the envelope contract does not yet need.

Decision:

- Add `InProcessMessageBus` under `com.enhancer.bus`: synchronous, single-threaded, deterministic delivery over `MessageEnvelope`, with a typed `DeliveryDestination` (`TOPIC` fan-out in registration order, `QUEUE` point-to-point to a single consumer that rejects a second consumer).
- Return an immutable ordered list of per-subscriber `DeliveryOutcome`s with a typed `DeliveryStatus` (`DELIVERED`, `DUPLICATE`, `UNROUTED`).
- Make delivery idempotent per `(destination, subscriber, message identity)` using a collision-free record key, and record every publication in an ordered immutable journal whose `replay` re-dispatches deterministically without appending, reproducing outcomes on a fresh bus and yielding only `DUPLICATE` with no side effect against a bus that already processed them.
- Defer retry, cancellation propagation, dead-letter, ordering beyond registration, backpressure, competing consumers, threading, journal persistence, and the IPC transport interface to later increments.

Rationale:

Synchronous single-threaded fan-out with a per-subscription idempotency key is the smallest surface that demonstrates deterministic delivery and replay without duplicate side effects, exactly the exit criterion, while carrying whole envelopes unchanged so consumers still validate authority against repository state rather than trusting the sender. Restricting a queue to one consumer keeps point-to-point delivery deterministic without a load-balancing policy the contract does not need yet.

Consequences:

- Multiple competing queue consumers require an explicit deterministic selection policy introduced through a later recorded decision.
- The in-memory idempotency and journal state is unbounded and process-local; durability, retention bounds, and cross-restart replay wait for the persistence and transport increments.
- Possessing or delivering an envelope still grants no authority; the bus never creates or widens task or Tool scope.

### 2026-07-15: Make RunRecord Observation Test Time-Independent

Status: Accepted Decision

Context:

- `FileSystemRunRecordStore.persist()` stamps `storedAt` with `Instant.now()`, but `RunRecordMetadataCollectorTest` hardcoded its observation time to `2026-07-15T10:01:00Z`.
- When the wall clock is past 10:01 UTC, an AVAILABLE record's stored time falls after the fixed observation time, the collector correctly drops `sourceUpdatedAt` as future, and the test fails; this defect is unrelated to the Gate 7 delivery increment that surfaced it.

Decision:

- Derive the test's observation time from the same run clock as `persist()` (`Instant.now().plusSeconds(60)`) instead of a hardcoded instant, keeping the correction confined to the test and separate from the delivery increment.

Rationale:

The observation contract is that `sourceUpdatedAt` is present only when the stored time is not after the observation time; a correct test must observe at or after the clock that stamped the record rather than assume a fixed relationship to wall-clock time. Injecting a deterministic clock into the store would be a larger production change than the defect warrants.

Consequences:

- The test now passes regardless of wall-clock time of day; a future deterministic-clock refactor of the store may replace this run-clock derivation.

### 2026-07-15: Start Gate 7 With Reference-Only Versioned Envelopes And Exactly Four Payload Kinds

Status: Accepted Decision

Context:

- Gate 7 owns the typed message foundation, and the adopted orchestration invariants require every handoff to preserve the approved task revision, snapshot identity, authorization, correlation, and causation.
- The Gate 6 snapshot identity is deliberately absent from the RunRecord because envelopes own cross-handoff identity.
- Carrying payload content, evidence bodies, or Tool authority inside messages would widen the sensitive-data and authority boundaries that the evidence and policy layers already govern.

Decision:

- Add the envelope contract under `com.enhancer.bus`: an immutable versioned `MessageEnvelope` with canonical-UUID message identity, bounded correlation identity, optional canonical-UUID causation identity distinct from the message identity, bounded logical run and producer identities, an occurrence time, and one typed payload.
- Seal the payload hierarchy to exactly four kinds — work, result, control, handoff — so consumers exhaust them by type instead of interpreting conventions.
- Carry authorization and provenance as data only: the work and handoff payloads carry the approved task revision, a valid snapshot identity, and (for work) a bounded non-empty allowed-tool scope copied immutably; the result payload carries the run-record reference and verification status; the control payload carries a typed cancel/pause/resume signal with a bounded reason.
- Store references and identities only; delivery, topics, queues, retry, idempotency, replay, ordering, backpressure, and IPC transport arrive in later increments over this contract.

Rationale:

A message that carries the task revision, snapshot identity, and authorization scope as bounded data lets every later consumer verify what it received against repository authority instead of trusting the sender, which is the invariant the orchestration model demands. Sealing the payload kinds makes exhaustive handling a compiler guarantee before any bus exists.

Consequences:

- Possessing an envelope grants nothing: authority still enters only through the task document and execution policy, and later delivery code must re-validate rather than trust.
- New payload kinds require extending the sealed hierarchy through a recorded decision.
- Envelope schema evolution beyond the version identifier is deliberately unspecified until persistence or IPC needs it.

### 2026-07-15: Re-Scope Editor-Dependent Observations To Gate 12 And Promote Gate 6 As The Foundation

Status: Accepted Decision

Context:

- The recorded Gate 6 maturity assessment evidenced every scope item and exit criterion except diagnostics, terminal-session, and active/selected-file observation.
- Those three observations require diagnostic providers, terminal integration, and editor state that first exist with the Gate 12 interface work; Gate 6 cannot produce honest evidence for them, and stub adapters were rejected.
- The Roadmap keeps exactly one `Specified - Next` product gate, and leaving Gate 6 open across several gates would block that flow on capabilities it does not own.
- The user approved proceeding on the assessment's Option B recommendation on 2026-07-15.

Decision:

- Move the diagnostics, terminal-session metadata, and active/selected-file observation items from Gate 6 to Gate 12 as Workspace observation integrations of the interfaces that own those capabilities; their source kinds stay typed in the Workspace contract now.
- Promote Delivery Gate 6 to Integrated as the Workspace and Project Brain foundation on the evidence recorded in the maturity assessment; the production view and graph composition remain Operational sub-capabilities.
- Advance the sole `Specified - Next` marker to Delivery Gate 7 Event Bus and IPC Foundation.
- Update the two actual-roadmap test expectations to Gate 7 in the same change, because their contract is "the current next gate", not "Gate 6".

Rationale:

A gate should exit when everything it can honestly evidence is evidenced and the remainder belongs to capabilities other gates own; keeping typed source kinds now and integrating their adapters where the sources first exist preserves both the contract's completeness and the evidence discipline. The dependency order stays truthful: Gate 7 depends on snapshots and RunRecords, which are Integrated and Operational.

Consequences:

- Gate 12 gains three explicit Workspace observation integration items and Gate 6's exit no longer waits on them.
- Gate 6 is Integrated, not Operational or Released: its Operational surface remains the governed read-only CLI scenario, and per-file Git metadata, payload capture, modifies/verified-by producers, and graph persistence remain future work in their owning gates.
- The next product work is the Gate 7 event and message envelope foundation, which carries the snapshot identity across handoffs by design.

### 2026-07-15: Grant Read-Only Git Observation Through Two Fixed Commands With Digest-Only Retention

Status: Accepted Decision

Context:

- The Gate 6 exit criteria require snapshots to explain the Git state that informed a run, and the scope names read-only Git status and diff adapters.
- Executing an external command is a new authority category never used by production code in this repository; the Constitution requires explicit user authority for such expansion.
- The user explicitly approved this authority for the Git adapter on 2026-07-15 ("3번 승인할게"), scoped to read-only observation.
- Parsing git internals directly would be fragile, and retaining status or diff content would widen the sensitive-data boundary the snapshot contract deliberately closed.

Decision:

- Add `GitWorkspaceCollector` executing exactly two fixed-argument read-only commands, `git status --porcelain` and `git diff`, with no shell interpretation, the project root as working directory, a five-second per-command timeout, and a four-MiB output cap.
- Confine repository discovery to the project root itself via `GIT_CEILING_DIRECTORIES`, so the observation describes the project's own working tree and never an enclosing repository.
- Harden the invocation against hangs and hidden writes: `--no-optional-locks` and an invocation-scoped `core.fsmonitor=false` keep the observation from touching the index or spawning daemons, stderr is discarded instead of piped so it can never deadlock, and a watchdog destroys any invocation that outlives the timeout while output is being read.
- Store only the SHA-256 digest of each raw output as `GIT_STATUS`/`GIT_DIFF` observation metadata with `git-cli` provenance; never retain output, file lists, or diff content.
- Surface every failure — launch failure, non-zero exit, timeout with process destruction, oversized output — as an explicit `UNAVAILABLE` observation with a bounded reason, so hosts without git degrade honestly.
- Prohibit any mutating, remote, or configuration invocation in this collector; any future git capability requires its own decision and authority.
- Restrict this authority to this collector; it does not extend the Tool policy, the approved-task scope, or any other component.

Rationale:

Two fixed read-only commands with digest-only retention observe the working-tree state at the minimum authority and data surface that the exit criterion permits: the digest is a content-addressed identity of the Git state, sufficient to detect change and divergence, while the actual paths and diffs stay out of snapshots, graphs, and bounded output. Explicit unavailability keeps absence of git distinguishable from a clean tree.

Consequences:

- Approval is not transitive: no other component may execute external commands on the basis of this decision.
- A digest-only observation cannot say which files changed; bounded per-file metadata would be a separate increment with its own decision.
- Runs in non-repository roots or on hosts without git carry two `UNAVAILABLE` Git observations by design.

### 2026-07-15: Observe The Run Target With A Real Pre-Run Digest And Treat Containment Violations As Errors

Status: Accepted Decision

Context:

- Snapshots observed only canonical documents and prior run records, so they could not explain which file the run was about.
- The externally supplied expected digest is a claim, not an observation, and must not masquerade as observed provenance.
- The Context Reader precedent already accepts bounded, containment-checked direct reads as normal governed infrastructure.

Decision:

- Add `TargetFileMetadataCollector`: one pre-run streamed SHA-256 of the relative target under real-path containment, emitted as a `REPOSITORY_FILE` observation with `target-file-reader` provenance; content is never retained.
- Observe a missing or over-64-MiB target as explicit `UNAVAILABLE` with a bounded reason, preserving the run's own failure semantics.
- Reject absolute, traversal, escaping, and non-regular targets as configuration errors surfaced by the CLI as usage errors before execution; violations are never observations.
- Include the observation in the CLI-collected snapshot so the target appears as an `ARTIFACT` node through the existing producer.

Rationale:

A snapshot that explains a run must observe the run's subject with evidence of its own, and a real digest is the only observation that can later expose divergence from the externally claimed expectation. Keeping containment violations as errors preserves the distinction between an unhealthy source and an illegal request.

Consequences:

- The target is read twice per run (observation and Tool execution); both reads are bounded and read-only.
- Snapshot identity now reflects the target's pre-run content revision.
- Divergence between the observed digest and the verified expectation is visible across the snapshot and the RunRecord.

### 2026-07-15: Pin The Workspace Authority Boundary With Characterization Evidence Instead Of New Mechanism

Status: Accepted Decision

Context:

- The Gate 6 exit criteria require that Workspace observations cannot override repository authority or grant Tool permission, but no test stated those claims explicitly.
- Tool scope enters execution only through the task document via `ApprovedTaskReader`, and observations carry digests and bounded metadata by construction, so the boundary should already hold.
- The Gate 0 precedent promotes claims through characterization tests that pass on first run, without manufacturing production changes.

Decision:

- Characterize the boundary in `WorkspaceAuthorityBoundaryIntegrationTest`: adversarial tool-grant text inside every observed non-task document must not widen the persisted approved task or policy scope beyond the task document's declared tools, must not appear in bounded output, and must not survive into any repository document mutation.
- Assert the converse: a task document that does not allow `read-file` is rejected as a configuration error regardless of grant text elsewhere.
- Treat a first-run failure as a defect that stops the task; add no production mechanism for this evidence.

Rationale:

The boundary's strength comes from the existing single-entry design (task document to `ApprovedTaskReader` to policy), so the honest evidence is a test that tries to break it from the observation side and fails. Adding new enforcement code for an already-enforced property would obscure where authority actually enters.

Consequences:

- The exit criterion "Workspace observations cannot override repository authority or grant Tool permission" is now pinned by regression evidence.
- Future adapters that introduce new observation kinds inherit the same test pattern for their sources.

### 2026-07-15: Link Tasks To Decisions Only Through An Explicit Justified By Section

Status: Accepted Decision

Context:

- Impact answers carry executions but never decisions, because no document grammar evidences which accepted decisions justify the active task; every earlier producer refused to infer that linkage.
- The task document reader ignores unknown sections, so an optional section can be adopted without touching `ApprovedTaskReader`, the `ApprovedTask` record, or the RunRecord encoding.
- A reference that silently fails to resolve would make stated justification indistinguishable from its absence.

Decision:

- Adopt an optional `## Justified By` section in the active task document whose bullets name accepted-decision headings exactly; absence means no justification is claimed.
- Add `TaskJustificationProjector` under `com.enhancer.brain`: it parses the section from the task source document in already-loaded memory, matches each reference against the projected accepted-decision node identities, and emits `JUSTIFIED_BY` edges from the task node.
- Reject a present-but-empty section, non-bullet content, duplicate references, and references that match no projected accepted decision, instead of skipping them.
- Give each edge the task source document as provenance with its computed SHA-256 and snapshot-relative freshness, mirroring the decision projector's rules.
- Merge the edges through a new additional-edges overload of `RunEvidenceGraphProducer` and report a bounded `impactDecisions` count on the CLI run path.
- Keep `ApprovedTaskReader`, `ApprovedTask`, and all persistence schemas unchanged.

Rationale:

Justification is a claim by the task's author, so the only honest evidence is the task document saying it explicitly; parsing exactly that section projects the claim without interpretation, and strict rejection keeps a typo from silently erasing a stated justification. Reusing the accepted-decision node identities as the reference vocabulary means a reference can only point at a decision the decision log actually accepted.

Consequences:

- Renaming a decision heading breaks references to it; reference updates are part of such a rename.
- An unresolvable reference fails graph composition after the RunRecord is persisted, surfacing as an internal error while the record stays replayable.
- Tasks without the section keep producing decision-empty impact answers, which remains the honest default.

### 2026-07-15: Promote Gate 6 Sub-Capabilities Only Against Named Fresh Integration Evidence

Status: Accepted Decision

Context:

- Six Gate 6 sub-capabilities are recorded as Contract Verified, but later increments connected each of them to real upstream and downstream components through integration tests and actual-repository runs.
- The Roadmap state model requires integration tests as the evidence class for Integrated, and the Constitution forbids promotion from past results or documentation claims alone.
- The Gate 0 precedent promoted through an audit with fresh evidence and no manufactured production change.

Decision:

- Promote a sub-capability to Integrated only when a named integration test, re-run fresh in this task, connects it to real upstream and downstream components; leave anything else Contract Verified and report the gap.
- Map evidence explicitly: `WorkspaceSnapshot`, `ProjectBrainView`, the graph projection contract, and `TaskImpactQuery` through `WorkspaceCollectionIntegrationTest` and the CLI composition suites; `AcceptedDecisionProjector` through the CLI graph-composition suite over a real decision log; `RunRecordMetadataCollector` through the real filesystem store suite and the CLI second-run prior-record observation.
- Change no production or test code for the promotion itself; an audit-exposed defect would instead stop the promotion and be reported.
- Keep Delivery Gate 6 `Specified - Next` and keep the existing Operational records for the production view and graph composition unchanged.

Rationale:

Maturity promotion is a claim about evidence, not about code, so the only honest procedure is to name the connecting evidence per capability and re-run it fresh. Requiring the evidence to predate the promotion task prevents the audit from writing tests whose purpose is to justify the promotion they measure.

Consequences:

- Promoted records cite the same suites future regressions will keep running, so integration claims stay continuously re-verified.
- Sub-capabilities promoted to Integrated may still lack Operational status individually; the production-path Operational records remain separate.
- Gate 6 gate-level promotion still requires the remaining scope: reference grammar, remaining adapters, and boundary enforcement evidence.

### 2026-07-15: Compose The Production Graph From The Same Governed Inputs The Run Already Loads

Status: Accepted Decision

Context:

- The decision projection and run-record observation are Contract Verified but have no production caller, and the CLI composes only the view.
- The run path already loads repository memory, collects the snapshot, and persists the RunRecord; the graph needs exactly those inputs plus prior run-record metadata.
- Reporting node identities or digests would leak repository structure into bounded diagnostics that existing rules keep content-free.

Decision:

- Construct the RunRecord store before snapshot collection so prior records are observed into the snapshot through `RunRecordMetadataCollector`.
- Extend the memory collector and the run-evidence producer with overloads for additional observations and additional evidence-backed nodes instead of adding parallel collection paths.
- After finalization, project accepted decisions from the same loaded memory, produce one graph keyed to the same snapshot, and answer the task impact query in process.
- Report only bounded counts (`graphNodes`, `graphEdges`, `graphDecisions`, `impactExecutions`); keep every existing output line, exit code, and replay behavior unchanged.
- Persist nothing new; graphs and impact answers remain derived, rebuildable outputs.

Rationale:

Composing from inputs the run already trusts keeps the graph an account of the governed run rather than a second collection path, and count-only reporting makes production composition observable without widening the diagnostic surface. The overloads preserve single enforcement points for snapshot and graph invariants.

Consequences:

- Snapshot identity now reflects prior run-record observations, so runs over the same tree with different run histories produce different snapshot identities by design.
- Graph production cost grows with stored record count; retention remains governed elsewhere.
- Decisions remain unlinked in impact answers until a task-to-decision reference grammar exists.

### 2026-07-15: Observe Stored Run Records As Explicit Metadata With Corruption Surfaced As Unavailable

Status: Accepted Decision

Context:

- The Workspace contract types a `RUN_RECORD` source kind and the Gate 6 scope names RunRecord metadata as a snapshot source, but no adapter observes stored records.
- The RunRecord store exposes only `persist` and `resolve`; observation needs a read-only listing.
- Resolution already validates envelope integrity, so a listed record either yields trustworthy metadata or a typed corruption failure.
- Silently skipping a corrupted record would make absence indistinguishable from health, which the snapshot's explicit state model exists to prevent.

Decision:

- Add a read-only `references()` listing to the `RunRecordStore` interface and its filesystem implementation: lexicographically ordered valid references, an empty list for a missing or empty root, and non-record files ignored.
- Add `RunRecordMetadataCollector` under `com.enhancer.workspace`, emitting one `RUN_RECORD` observation per listed reference with `run-record-store` provenance and a caller-supplied observation time.
- Emit `AVAILABLE` observations carrying the envelope SHA-256 as content digest and the stored time as source-update time when it does not postdate the observation time.
- Emit an explicit `UNAVAILABLE` observation with a bounded reason and no digest when integrity resolution fails.
- Store no payload, evidence, or Tool content, and add no write, delete, or retention capability.

Rationale:

The envelope digest is already the store's own integrity identity, so reusing it as the observation digest gives run-record observations the same content-addressed discipline as documents without decoding payloads a second time. Surfacing corruption as `UNAVAILABLE` uses the state the contract defined for exactly this case and keeps the snapshot an honest account of what the store could and could not vouch for.

Consequences:

- Observation cost grows with record count because each listed record is integrity-checked; retention and cleanup remain governed elsewhere.
- Files under the storage root that are not record artifacts are outside the store's contract and are not observed.
- The CLI does not yet include these observations; the production composition increment owns that integration.

### 2026-07-15: Project Accepted Decisions As Unlinked Nodes With Snapshot-Relative Freshness

Status: Accepted Decision

Context:

- Produced graphs contain no decision nodes because no producer parses decision evidence.
- The decision log already records accepted decisions under dated `### ` headings with an explicit `Status: Accepted Decision` line, and repository memory carries that document.
- No document grammar links the active task to the decisions that justify it, so `JUSTIFIED_BY` edges cannot be evidenced without inventing linkage.
- A decision node whose provenance cannot say whether the underlying document changed since the snapshot would silently go stale.

Decision:

- Add `AcceptedDecisionProjector` under `com.enhancer.brain`, projecting decision nodes from the `DECISION_LOG.md` document in one loaded `ProjectContext`.
- Treat a section as an accepted decision only when its `Status:` line reads exactly `Accepted Decision`; skip proposals and other statuses.
- Use the heading text as node identity, the decision log path and computed document SHA-256 as provenance, and document order for emission.
- Derive freshness against the snapshot's `DECISION_LOG.md` observation: same digest is `CURRENT`; a differing digest or an unobserved document is `STALE`, because currency cannot be proven without a matching observation.
- Emit no edges; task-to-decision linkage requires an explicit reference grammar adopted through its own decision.
- Reject duplicate accepted-decision headings instead of merging them.

Rationale:

The decision log's own status line is the only machine-readable acceptance evidence in the repository, so parsing exactly that line projects decisions without interpretation. Marking unobserved documents `STALE` keeps the projection honest: a node the snapshot cannot vouch for must not claim currency.

Consequences:

- Impact queries still return empty decision lists until a linkage grammar and its projector exist.
- Renaming a decision heading changes the node identity; stable decision identifiers would need their own convention.
- The parser depends on the documented heading and status conventions of this repository's decision log.

### 2026-07-15: Produce The First Real Graph Only From Elements The Run Evidence Actually Proves

Status: Accepted Decision

Context:

- The graph contract and impact query are Contract Verified only against contract-constructed graphs; no producer projects real repository evidence.
- The available real evidence is one Workspace snapshot (approved task revision plus observed repository documents with digests and explicit states) and one stored run record (task, verification, envelope digest, durable reference).
- A read-only Gate 5 run modifies nothing, no decision parser exists, and no test-to-code linkage evidence exists, so `MODIFIES`, `VERIFIED_BY`, `JUSTIFIED_BY`, `SUPERSEDES`, and `DEPENDS_ON` edges cannot yet be justified.
- Projecting unjustified edges would make the impact query report relationships no evidence supports, which is exactly what the graph model prohibits.

Decision:

- Add `RunEvidenceGraphProducer` under `com.enhancer.brain`, producing from one snapshot, one task-matched `ResolvedRunRecord`, and an explicit projection time.
- Project only what the evidence proves: one task node from the approved task revision, one artifact node per repository document/file observation, one execution node from the stored record, and one `RECORDED_AS` edge from task to execution.
- Map observation states to element freshness one-to-one: Available to Current, Stale to Stale, Unavailable to Source-Missing, carrying the observation digest exactly when present.
- Use the stored record's envelope SHA-256 and durable reference as the execution node's provenance.
- Key the graph to the snapshot identity and delegate all structural enforcement to `ProjectBrainGraph.project`.
- Skip non-repository observation kinds and emit no other node or edge kinds until later producers can justify them from parsed decisions, write operations, or test evidence.

Rationale:

A projection is trustworthy only if every element can be traced to evidence, so the first producer's value is precisely its refusal to invent. The one-to-one state-to-freshness mapping preserves the snapshot's explicit staleness semantics inside the graph, and reusing the stored envelope digest gives executions the same content-addressed provenance discipline as documents.

Consequences:

- Impact answers over produced graphs currently return executions and observed artifacts only; decisions, modified artifacts, and verifying tests stay empty until their producers exist.
- Later producers extend the same graph rather than replacing it; each new edge kind requires its own evidence source and decision.
- Non-repository observations (Git status, diagnostics, terminal) remain unprojected until their adapters and node semantics are decided.

### 2026-07-15: Answer The First Impact Query Over One Graph With Explicit Rebuild Status And No Transitive Closure

Status: Accepted Decision

Context:

- The graph projection contract is Contract Verified and the roadmap names the task-to-decision-to-code-to-test impact query as its first consumer.
- No producer projects real graphs yet, so the query can only be proven against contract-constructed projections.
- Transitive dependency propagation over `DEPENDS_ON` would answer a broader "what else is affected" question, but its semantics (direction, depth, cycles) deserve their own decision once real dependency projections exist.
- A query answer detached from its graph's snapshot identity could not say when it must be recomputed.

Decision:

- Add `TaskImpactQuery` and the immutable `TaskImpact` result under `com.enhancer.brain`, answering over exactly one `ProjectBrainGraph`.
- Traverse only the named chain from the queried task: `JUSTIFIED_BY` to decisions, `MODIFIES` to artifacts, `VERIFIED_BY` from those modified artifacts to verifying artifacts, and `RECORDED_AS` to executions.
- Carry the graph's source snapshot identity in the result and derive one rebuild-required status that is true exactly when the task node, any traversed edge, or any returned node requires rebuild.
- Keep result ordering derived from the graph's canonical ordering, deduplicate shared verifying artifacts, return empty collections for an edgeless task, and reject unknown or non-task identities.
- Defer transitive `DEPENDS_ON` closure, multi-graph answering, producers, persistence, caching, and indexing to separate approved tasks.

Rationale:

The named chain is the exact question the roadmap commits to answering first, and answering it over one snapshot-keyed graph keeps the result reproducible: the same graph always yields the same answer, and the snapshot identity plus rebuild status say precisely when that answer stops being trustworthy. Deriving one aggregate rebuild flag from element provenance keeps staleness visible without inventing a second freshness model.

Consequences:

- The query is only as complete as the projected graph; missing projections yield empty results rather than inferred impact.
- Impact through dependency chains is not yet visible; that closure arrives with real dependency projections and its own decision.
- Producers must later construct graphs from repository evidence before the query answers anything about the actual project.

### 2026-07-15: Constrain The First Graph Projection Contract To Typed Endpoint-Checked Metadata Keyed To One Snapshot

Status: Accepted Decision

Context:

- The Gate 6 scope requires graph projection contracts for Decision, Architecture, Dependency, Task, and Execution relationships, and its exit criteria require nodes and edges to retain source, freshness, version, and rebuild status.
- The Architecture defines Project Brain graphs as rebuildable projections that must never silently overwrite their authoritative sources.
- No projection producer, query, or persistence exists yet; the first consumer is the task-to-decision-to-code-to-test impact query in a later increment.
- An unconstrained stringly-typed graph would accept relationships that the impact query could not interpret and that no source could justify.

Decision:

- Add the projection contract under `com.enhancer.brain` with five node kinds (task, decision, component, artifact, execution) and six endpoint-checked edge kinds: `JUSTIFIED_BY` task-to-decision, `SUPERSEDES` decision-to-decision, `DEPENDS_ON` between components and artifacts, `MODIFIES` task-to-artifact, `VERIFIED_BY` artifact-to-artifact, and `RECORDED_AS` task-to-execution.
- Give every node and edge an immutable provenance of bounded source reference, optional lowercase SHA-256 source revision, and an explicit freshness state (`CURRENT`, `STALE`, `SOURCE_MISSING`) whose rebuild-required status is derived, not stored separately.
- Require a source revision for Current and Stale provenance and prohibit it for Source-Missing provenance, mirroring the Workspace observation digest rules.
- Key each `ProjectBrainGraph` to one valid source snapshot identity with an explicit projection time and a versioned projection identifier, so projections are traceable and rebuildable rather than free-floating.
- Enforce deterministic ordering, duplicate and self-loop rejection, unknown-endpoint rejection, and 4096-entry bounds in the contract itself.
- Defer projection producers, the impact query, traversal, persistence, rebuild execution, and confidence metadata to separate approved tasks.

Rationale:

Endpoint-kind checking makes the six relationships carry their meaning in the type system, so a later impact query can traverse task-to-decision-to-code-to-test chains without interpreting conventions. Deriving rebuild status from freshness avoids two fields that could contradict each other. Keying to the snapshot identity reuses the already-verified content-addressed boundary instead of inventing a second projection identity source.

Consequences:

- Producers must later justify every node and edge from repository documents, RunRecords, or snapshot observations through separate approved tasks.
- The five graph domains share one contract; a domain needing a new relationship must extend the edge taxonomy through a recorded decision rather than reusing a loosely-fitting kind.
- Graph identity intentionally does not exist yet; equality is structural, and a persisted graph identity would arrive with persistence work.

### 2026-07-15: Compose The Project Brain View On The Existing CLI Run Path Without Widening Its Surface

Status: Accepted Decision

Context:

- The repository-memory path is Integrated in a temporary-project test, but no production caller composes the `ProjectBrainView` during an actual governed run.
- The `EnhancerCli` `run` command already loads the full `ProjectContext` to derive and validate the approved task, then discards it.
- Reloading memory after the run would create a second read of the same sources and could disagree with the memory that actually informed approval.
- Persisting the snapshot identity would change the RunRecord schema, which carries its own compatibility and replay obligations.

Decision:

- On the `run` path, keep the already-loaded `ProjectContext`, collect the `WorkspaceSnapshot` through `RepositoryMemorySnapshotCollector` with a capture time taken before worker execution, and compose the `ProjectBrainView` after finalization with the persisted `RunRecord`.
- Compose for every run outcome that produces a record, since the view explains what informed the run regardless of how the run stopped.
- Report only bounded metadata in the existing output: the snapshot identity, the observation count, and a matched/diverged/notObserved freshness summary; never document content, digest lists, or evidence.
- Keep exit codes, existing output lines, persist-before-report ordering, replay behavior, and the output bound unchanged, and add no command, argument, or authority.
- Defer persisting the snapshot identity in the RunRecord to the Gate 7 envelope work that already owns cross-handoff identity.

Rationale:

Composing from the memory that produced the approved task makes the reported snapshot describe exactly the inputs the run was approved against, at no additional authority or read cost. Reporting identity and freshness counts keeps the bounded-diagnostics contract intact while making the composition externally observable, which is what an Operational claim needs.

Consequences:

- The reported freshness is trivially all-matched unless repository documents change between context load and composition, because the same in-memory context is both the snapshot source and the comparison input; real divergence reporting arrives when snapshots persist or sources are re-observed.
- Replay does not reproduce the snapshot identity because the RunRecord does not store it; that linkage is explicitly deferred.
- A composition failure after the record is persisted surfaces as an internal error while the durable record remains replayable.

### 2026-07-15: Collect The First Real Workspace Snapshot From Already-Loaded Repository Memory

Status: Accepted Decision

Context:

- The `WorkspaceSnapshot` and `ProjectBrainView` contracts are Contract Verified, but every snapshot so far was hand-written by tests, so no real source informs the aggregate.
- The two named paths toward Gate 6 integration are an explicit source adapter or a production composition path; the adapter is the smaller increment.
- The Context Reader already loads bounded, containment-checked, UTF-8-validated repository documents, and the Gate 0 lifecycle test already produces a real persisted RunRecord in a governed temporary project.
- Giving the first collector its own filesystem access would duplicate the Context Reader's containment and bounds enforcement and widen the authority surface without need.

Decision:

- Add a read-only `RepositoryMemorySnapshotCollector` under `com.enhancer.workspace` that derives a snapshot from a project root, an explicit caller-supplied capture time, an `ApprovedTask`, and an already-loaded `ProjectContext`.
- Derive the `ApprovedTaskRevision` inside the collector by digesting the approved task's source document content out of the same loaded memory, so the revision provably describes the memory that was actually read; reject a memory without that document.
- Emit one `AVAILABLE` `REPOSITORY_DOCUMENT` observation per loaded document with the document path as source identity, `context-reader` provenance, and a computed lowercase SHA-256 content digest; retain no content.
- Reuse `WorkspaceSnapshot.capture` for ordering, duplicates, bounds, and canonical identity instead of adding a second identity computation.
- Take the capture time as an explicit parameter rather than reading a clock inside the collector, keeping collection deterministic and testable.
- Prove the end-to-end path in an integration test that combines a real governed CLI run, the real Context Reader, the collector, and the view, including a divergence check after the source document changes.
- Defer Git, diagnostics, selection, terminal, and RunRecord-source adapters, and any production composition path, to separate approved tasks.

Rationale:

Deriving observations from memory the Context Reader already loaded keeps the collector free of filesystem authority and reuses the hardened containment path instead of duplicating it. Deriving the task revision from the same memory closes the gap where a caller could claim a revision digest unrelated to what was actually read. An explicit capture time avoids a hidden clock dependency, which keeps snapshots reproducible in tests and later replayable.

Consequences:

- The collector observes only what the Context Reader loads; unfetched sources are absent from the snapshot rather than marked `STALE` or `UNAVAILABLE`, and those states first appear with real per-source adapters.
- Observation time equals capture time for every document because loading time is not tracked per document in `ProjectContext`.
- The end-to-end evidence integrates the repository-memory path only; Gate 6 stays `Specified - Next` until its remaining scope exists.

### 2026-07-15: Compose The First ProjectBrainView As A Derived Read-Only Aggregate Keyed To One Snapshot

Status: Accepted Decision

Context:

- The Gate 6 `WorkspaceSnapshot` contract is Contract Verified but has no consumer, so Gate 6 cannot claim Integrated maturity.
- Architecture defines Project Brain as the reasoning-facing aggregate of repository memory, Workspace snapshots, decisions, and RunRecords that preserves source identity and freshness.
- The Context Reader already produces `ProjectContext` repository memory with document content, and the RunRecord Store already produces verified `RunRecord` run history.
- Carrying document content, Tool payloads, or evidence bodies into the aggregate would widen the sensitive-data boundary that the snapshot contract deliberately closed.

Decision:

- Add the first Project Brain aggregate as `ProjectBrainView` under a new provider-neutral `com.enhancer.brain` package that depends on `com.enhancer.workspace`, `com.enhancer.context`, and `com.enhancer.run`.
- Compose the view from exactly one `WorkspaceSnapshot`, one `ProjectContext`, and one `RunRecord`; the view derives its content and never collects sources itself.
- Key the view to the snapshot's canonical identity rather than recomputing a second identity.
- Project repository memory to document path, read order, and a computed lowercase SHA-256 of the document content; retain no document content in the aggregate.
- Derive explicit repository-memory freshness by comparing each document digest against the snapshot's `REPOSITORY_DOCUMENT` observation with the same source identity: equal digests are `SNAPSHOT_MATCHED`, differing digests are `SNAPSHOT_DIVERGED`, and an unobserved document is `NOT_OBSERVED`.
- Require the RunRecord's approved task identity and source document to equal the snapshot's `ApprovedTaskRevision`; reject a mismatched or unrelated run instead of silently aggregating it.
- Project the RunRecord to metadata-only provenance of logical run identity, record time, approved task identity, and verification status; exclude Tool requests, results, evidence bodies, and chat history.
- Expose the snapshot's Available, Stale, and Unavailable observations unchanged rather than collapsing or defaulting them.
- Add no persistence, graph projection, source adapter, or Tool authority in this increment.

Rationale:

Deriving the aggregate from existing verified inputs proves the snapshot contract is consumable without inventing a second collection path or a competing identity. Comparing loaded repository memory against the snapshot's observed digests turns divergence into an explicit, inspectable state rather than an unnoticed inconsistency, which is what the Gate 6 exit criterion about explaining a run actually requires. Requiring the run and snapshot to name the same approved task keeps provenance honest, because an aggregate that mixes unrelated work would misattribute the evidence it presents.

Consequences:

- The view is only as complete as the snapshot it is given; a snapshot without observations yields `NOT_OBSERVED` memory freshness rather than an error.
- Repository-memory digests are computed from already-loaded `ProjectContext` content, so this increment adds no new filesystem access or Tool authority.
- Freshness derivation is metadata comparison only; it does not prove that either revision is correct, authorized, or safe.
- Later graph projections and source adapters must extend this aggregate through separate approved tasks.
- Gate 6 may claim Integrated maturity only if fresh evidence shows the real snapshot, Context, and RunRecord contracts connected through this view.

### 2026-07-15: Start Gate 6 With A Metadata-Only Content-Addressed Workspace Snapshot

Status: Accepted Decision

Context:

- Gate 6 owns the immutable common input snapshot and approved task revision that later Project Brain, messaging, and worker handoffs require.
- Repository files, Git state, diagnostics, selection, terminal-session metadata, and RunRecords have different adapters and permissions that are not yet implemented.
- Capturing source content in the foundational contract would expand sensitive-data and memory boundaries before an actual consumer or adapter exists.

Decision:

- Begin Gate 6 with provider-neutral immutable metadata contracts under `com.enhancer.workspace`.
- Represent approved work with task identity, source-document identity, and a lowercase SHA-256 source revision; this is provenance and not Tool authority.
- Represent Workspace inputs as typed observations with source kind, source identity, adapter provenance, observation time, optional source-update time, explicit Available/Stale/Unavailable state, optional SHA-256 content identity, and bounded diagnostic reason.
- Store no source payload in the first snapshot contract.
- Compute a canonical SHA-256 snapshot identity in production from the normalized project root, capture time, task revision, and deterministically ordered observation metadata.
- Reject duplicate sources, inconsistent availability/digest combinations, invalid temporal relationships, excessive item counts, and unbounded strings.
- Name the next Gate 6 Project Brain aggregate as the immediate integration consumer and Gate 7 message envelopes as the next-gate identity consumer.
- Keep Gate 6 `Specified - Next` while this sub-capability advances only to Contract Verified.

Rationale:

A content-addressed metadata boundary gives every later adapter and worker one stable snapshot identity without prematurely retaining source payloads or creating command authority. Explicit unavailable and stale states prevent absence from being confused with freshness, while deterministic ordering keeps identity independent of caller collection order.

Consequences:

- Source adapters must later provide bounded metadata and digests through separate approved tasks.
- Snapshot equality and identity do not prove that source content is safe, trusted, or authorized; provenance and repository rules remain authoritative.
- A future ProjectBrainView must consume this contract before the Workspace capability can be called Integrated.

### 2026-07-15: Promote Gate 0 Only Through An Authority-Preserving Lifecycle Integration Audit

Status: Accepted Decision

Context:

- Gate 0 remains Contract Verified even though later Gates consume its Context, planning, loop, result, evidence, and governance contracts.
- The original Gate 0 limitation says the pieces do not execute one connected Agent run, while Gate 5 now provides an Operational read-only run over most of those boundaries.
- Planner proposals deliberately cannot approve themselves, so a valid integration test must not create an automatic Proposal-to-execution authority path.

Decision:

- Prepare one bounded Gate 0 promotion task that inventories each contract's real consumer and adds an end-to-end lifecycle integration test over a governed temporary repository.
- Split the test into a read-only planning phase and an execution phase separated by an explicit external test-fixture activation of `CURRENT_TASK.md`.
- Prove that planning leaves repository authority unchanged, execution before activation is rejected, and the activated read-only task reaches verified completion and restart-safe RunRecord replay through existing production boundaries.
- Reuse the Gate 5 CLI composition rather than adding a second production orchestrator.
- Allow the new characterization test to be initially GREEN if existing behavior already satisfies the accepted integration contract; create a RED cycle only for a genuine aligned behavior gap.
- Promote Gate 0 from Contract Verified to Integrated only after focused, consumer, full-regression, lint, structural, and documentation evidence passes.
- Keep Gate 6 as the sole `Specified - Next` product gate throughout this maturity-reconciliation task.

Rationale:

Integration maturity requires evidence that real collaborators are connected, not artificial new code or an authority shortcut. The explicit fixture transition models the human or governed approval boundary while allowing the full planning-to-verified-run lifecycle to be tested without mutating the actual repository.

Consequences:

- Gate 0 may be promoted without a new user interface because Operational entry is already owned by Gate 5.
- A passing characterization test can justify a documentation-only maturity promotion when it proves that existing downstream integrations satisfy every Gate 0 contract.
- Any uncovered defect remains subject to the active task, RED classification, and minimum-change rules.

### 2026-07-15: Expose The Integrated Read-Only Run Through A Minimal Local CLI

Status: Accepted Decision

Context:

- Delivery Gates 1 through 4 provide repository-derived approval, governed read-only Tool execution, complete evidence persistence, independent verification, and durable RunRecord replay, but no supported entry point connects them for a user.
- Gate 5 requires explicit project, task, target, expected digest, evidence-root, and RunRecord-root inputs plus stable exit codes and documented recovery.
- The future interface suite belongs to Gate 12; the first operational command should therefore remain deliberately small and dependency-free.

Decision:

- Add the Gradle `application` entry point `com.enhancer.cli.EnhancerCli` with two commands: `run` and `replay`.
- Make `run` require six named inputs and match the explicit task identity against the `ApprovedTask` derived from the project's active `CURRENT_TASK.md`.
- Wire only the existing `read-file` flow through `ExecutionPolicy`, `ToolExecutor`, `AgentRunController`, `DeterministicReadFileVerifier`, and `AgentRunFinalizer`.
- Use fixed documented defaults of the existing 64 MiB evidence ceiling, a five-second Tool timeout, a five-iteration loop ceiling, a three-transition stagnation threshold, and a 30-day retention declaration without automatic cleanup.
- Define stable process exit codes for completion, usage/configuration failure, verification failure, policy denial, Tool failure, stagnation, maximum iterations, and internal failure.
- Bound CLI diagnostics, print no complete target content, and report the RunRecord root and opaque reference for replay.
- Make `replay` accept only an explicit RunRecord root and opaque reference and print typed bounded metadata from the integrity-checked store.

Rationale:

This is the smallest supported control surface that proves the integrated vertical slice against a real repository without inventing a second execution path. Explicit arguments keep authority and expected results inspectable, while stable exit codes and replay make the command automatable and diagnosable.

Consequences:

- Successful Gate 5 evidence may promote the first read-only run to Operational, but it does not release a distribution or make the broader Agent Runtime Operational.
- The command does not infer task approval, expected content, storage roots, or target paths from ambient state.
- Interactive prompts, configuration discovery, shell/Git/network/LLM capabilities, and polished multi-interface behavior remain deferred.
- Gate 6 becomes the next specified capability only after temporary-project and actual-repository run/replay evidence passes.

### 2026-07-15: Harden Integrated Boundaries Before The First Operational CLI

Status: Accepted Decision

Context:

- A Gradle 9 compatibility review found that the build relies on Gradle's deprecated automatic JUnit Platform launcher injection.
- Boundary testing found that one interruption-ignoring timed-out Tool can occupy the shared worker, duration values can pass policy validation but fail execution or audit conversion, and persisted envelope metadata is outside the integrity digest.
- RunRecord UTF-8 writing can replace malformed Unicode, startup context loading does not enforce real-root containment or size bounds, and the standard test task depends on a user-profile temporary directory.
- These defects are in Integrated Gate 1 through 4 foundations that Gate 5 would expose through a supported command.

Decision:

- Complete a bounded foundation-hardening task before Gate 5 without changing capability maturity or delivery order.
- Declare the JUnit Platform launcher explicitly and configure a build-local default test temporary directory.
- Isolate Tool invocations so a timed-out invocation cannot starve later work, while retaining bounded structured results and executor shutdown ownership.
- Enforce a timeout domain that is representable in both nanoseconds for execution and positive milliseconds for policy audit records.
- Integrity-protect the complete versioned envelope metadata and payload for Evidence and RunRecords, and use strict UTF-8 encoding for persisted RunRecord strings.
- Apply the Tool boundary's real-path containment, strict decoding, and bounded-read principles to required startup documents.
- Preserve the intentional no-persistence `ReadFileTool` failure for truncated output but classify it as an execution/evidence-capability failure rather than invalid caller input.
- Keep Gate 5 as the sole `Specified - Next` capability and perform no Wrapper major-version upgrade in this task.

Rationale:

The first supported CLI must not operationalize known starvation, audit-integrity, data-preservation, path-containment, or build-compatibility defects. These changes strengthen existing contracts without adding a new product capability or broadening authority.

Consequences:

- Pre-hardening local binary Evidence and RunRecord artifacts are not promised backward compatibility; no released storage format exists yet.
- Timeout validation becomes intentionally stricter at policy construction.
- Startup context loading can reject oversized, malformed, or out-of-root required documents before planning begins.
- Gate 5 remains the next capability task after fresh hardening verification and document synchronization.

### 2026-07-14: Translate External Orchestration Patterns Into Gate-Owned Enhancer Contracts

Status: Accepted Decision

Context:

- The user requested that useful Multi-Agent orchestration lessons from Archon and meta-harness be preserved for future Enhancer implementation.
- Archon demonstrates an operational control plane with dynamic capability rosters, centralized execution profiles, dependency-aware work, heartbeats, interventions, and resumable sessions, but its provider CLI subprocesses, shared working directory, file polling, and quality-gradient completion model do not satisfy Enhancer authority or evidence rules.
- meta-harness provides a portable pattern-selection ladder, deterministic handoffs, Producer-Reviewer and supervisor guidance, normal/failure scenarios, and removable provider-specific logic, but it is a design-time meta-skill rather than a runtime with scheduling, authorization, idempotency, replay, or evidence integrity.
- The reviewed reference snapshots are Archon commit `263cf3658a7cadefa0c5fbe82cc527a00ffb4c16` under MIT and meta-harness commit `ccab9a677878f72b3316de464c99b36f56a3f2e7` under Apache-2.0.

Decision:

- Treat both repositories as pinned reference implementations, never as hidden runtime, governance, Skill-layout, prompt, or file-format dependencies.
- Select the smallest orchestration topology that satisfies the work: one worker first; then a sequential pipeline; then Producer-Reviewer; then bounded fan-out/fan-in; and only then expert routing, supervisor allocation, or a hierarchy no deeper than one subordinate coordination layer.
- Require every parallel branch to consume the same immutable `WorkspaceSnapshot` and approved task revision. Branch ownership, expected output, synthesis criteria, budget, and conflict policy are fixed before dispatch.
- Carry handoffs through versioned Message Bus envelopes with run, task, message, correlation, causation, producer, schema, authorization, input-snapshot, and artifact/evidence-reference identity. Free-form Markdown may be an inspectable projection but cannot be the authoritative queue or control signal.
- Keep one Kernel-owned coordinator responsible for terminal task and run state. Workers may propose progress, artifacts, or follow-up work; they cannot approve tasks, broaden Tool or model authority, create final completion, or verify their own output.
- Make dependency readiness, cycle rejection, leases, duplicate suppression, cancellation, retry, timeout, dead-letter, replay, pause, resume, reassignment, and recovery Scheduler or Message Bus responsibilities rather than prompt conventions.
- Represent execution profiles as provider-neutral capability, model class, reasoning budget, context budget, Tool scope, data classification, and locality requirements. Provider adapters translate an approved profile only after Kernel policy intersects it with task, Skill, and Tool authority.
- Represent pause, resume, cancel, inject-proposal, reprioritize, reassign, mediate, and scale decisions as typed, authenticated, auditable control commands. A control command cannot silently create accepted work or external-action authority.
- Treat heartbeats, quality gradients, confidence, prompt adherence, and other worker telemetry as diagnostic observations only. They may trigger inspection or a proposal, but never lifecycle promotion, verification, completion, or release.
- Preserve independent verification and durable RunRecord finalization outside the producing worker. Producer-Reviewer revision loops are bounded and remain distinct from the independent verifier required for completion.
- Keep model-specific retries, prompt heuristics, CLI flags, and provider recovery logic behind removable adapters or reference sections so deleting one provider does not rewrite the orchestration contract.

Rationale:

The useful common pattern is observable, resumable, role-aware coordination with explicit handoffs and bounded parallelism. Enhancer already has stronger authority, evidence, verification, and replay requirements than either reference. Translating the patterns into existing Workspace, Event/Message Bus, Scheduler, Model Gateway, Skill, Verification, and RunRecord boundaries preserves those strengths while avoiding provider and storage coupling.

Consequences:

- Gate 6 owns immutable shared input snapshots and provenance; Gate 7 owns typed handoffs and control/event delivery; Gate 8 owns the durable dependency graph, leases, sequential worker, Scheduler, and recovery.
- Gate 9 owns provider-neutral execution profiles and model budgets; Gate 10 owns validated workflow-pattern and Skill selection; Gate 12 owns user-facing run controls; Gate 13 owns dynamic capability rosters, bounded fan-out/fan-in, Producer-Reviewer roles, supervisor allocation, and background execution.
- Gate 15 alone may consume autonomous experiment-ledger patterns, and only after approved snapshots, fixed evaluation, budgets, independent verification, and rollback are Operational.
- Direct peer calls, prompt-only coordination, shared-worktree parallel mutation without isolation, silent ring-buffer loss, self-reported completion, optional verification, unlimited timeouts, and file polling as the core bus are rejected.
- This decision changes documentation only. It does not promote Workspace, Message Bus, Agent Runtime, Scheduler, Model Gateway, Skill Engine, Multi-Agent, background execution, or self-improvement maturity, and it does not displace Delivery Gate 5.
- No external code or templates are copied by this decision. Any later copying must preserve applicable license, attribution, and modification notices.

### 2026-07-14: Bind Run Records To The Policy Used During Execution

Status: Accepted Decision

Context:

- Gate 4 records an immutable execution-policy snapshot, but the finalizer currently accepts an `ExecutionPolicy` separately after the worker run.
- A caller could supply a different still-allowing root, timeout, or size limit and produce an internally valid record that does not describe the actual execution.
- The public `RunRecord` constructor also permits some verification and stop-reason combinations that the governed Agent run path cannot produce.

Decision:

- Preserve the exact `ExecutionPolicy` used by `AgentRunController` in `AgentRunResult`.
- Remove the replaceable policy argument from finalization and derive the persisted `PolicyDecision` from the worker-bound policy.
- Make `RunRecord` enforce the Gate 3 and Gate 4 lifecycle: worker completion is impossible, verification is performed only after successful verification-wait, and non-verification terminal or bounded stops retain failed Tool output with verification Not Performed.

Rationale:

RunRecords are audit evidence. Their policy snapshot and lifecycle must be bound to the executed run rather than trusted as a later caller assertion. Enforcing these relationships in immutable contracts prevents Gate 5 or another future entry point from constructing replayable but historically false records.

Consequences:

- Finalization cannot substitute a different policy after Tool execution.
- Invalid persisted records are rejected during both direct construction and replay decoding.
- The change strengthens existing Gate 4 semantics without adding Tool authority, CLI behavior, LLM calls, or multi-agent execution.
- Gate 5 consumes one policy-bound worker result and no longer needs to repeat the policy at finalization.

### 2026-07-14: Complete Agent Runs Only Through External Evidence Verification And Durable Records

Status: Accepted Decision

Context:

- Gate 3 intentionally stops successful Tool output at `AWAITING_VERIFICATION` and does not preserve an externally inspectable completion record.
- A worker summary, exit status, or self-reported success cannot independently prove that the expected result was produced.
- Truncated evidence must be resolved and integrity-checked before completion, while missing evidence must remain distinguishable from proven mismatch or corruption.
- Failed, stagnated, and iteration-limited runs also need durable diagnostic history even though they never enter verification.

Decision:

- Introduce typed Verification statuses: Verified, Rejected, Unverified, and Not Performed, each constrained by a structured reason code.
- Bind every `VerificationRequest` to the approved task, executed Tool request, Tool result, and an expected SHA-256 content digest supplied outside the worker.
- Implement the first `IndependentVerifier` as a deterministic read-file verifier that recomputes complete-content identity and resolves truncated evidence through the existing `EvidenceStore`.
- Preserve the executed request in terminal Agent state instead of reconstructing it from progress hashes or diagnostic prose.
- Permit only the sequential finalization boundary to create `COMPLETED`, and only after a Verified decision.
- Persist a typed RunRecord before returning completed finalization. The record includes inputs, policy snapshot and decision, Tool result and evidence, verification, iterations, and worker/final stop reasons.
- Store RunRecords as versioned binary payloads in atomically published SHA-256 envelopes and support restart-safe replay.
- Record worker failure, stagnation, and iteration exhaustion with verification Not Performed rather than fabricating a verification attempt.

Rationale:

This is the smallest provider-neutral boundary that turns worker output into independently checked, replayable execution history. Digest comparison avoids trusting prose, reuse of `EvidenceStore` keeps complete-output integrity in one place, and persist-before-return prevents an in-memory completion from being reported without durable audit evidence.

Consequences:

- Missing evidence is Unverified; corrupted or mismatched evidence is Rejected; neither can complete a run.
- A RunRecord persistence failure prevents the finalizer from returning completion.
- The initial RunRecord format is local, bounded, versioned, and integrity-checked but not encrypted, signed, remotely replicated, or automatically deleted.
- Gate 5 can consume the finalizer and RunRecord reference through a supported CLI without changing verification authority.
- LLM verification, human review adapters, parallel reviewers, Git mutation, and distributed storage remain future work.

### 2026-07-14: Adopt V1-V3 Evolution And A Provenance-Preserving Project Brain

Status: Accepted Decision

Context:

- Repository Markdown and Git provide durable human-readable memory, but they do not directly represent the relationships among decisions, architecture, dependencies, tasks, executions, tests, bugs, commits, issues, and pull requests.
- The long-term product needs Cursor-level productivity, an Agent development platform, and finally an AI Operating System without confusing those maturity levels.
- Agents, Skills, workflows, and models have distinct responsibilities that must remain separable for plugins, marketplaces, security review, and routing.
- A one-sentence user intent should reduce human orchestration, but hidden Git publication, merge, deployment, or permission escalation would violate the Constitution.

Decision:

- Define three product milestones: **V1 AI Development Experience**, **V2 AI Development Platform**, and **V3 AI Operating System**.
- V1 provides Cursor-level productivity through CLI/editor/Desktop surfaces and Workspace awareness; it does not redefine Enhancer as an IDE or Cursor clone.
- V2 provides durable workflows, Skills, Memory, Agent Runtime, MCP, model routing, plugins, marketplace foundations, and self-hosting development support.
- V3 provides the AI Kernel, Project Brain knowledge graphs, multi-agent operating model, hybrid privacy-aware model routing, scheduler, plugin ecosystem, and governed synchronization/self-improvement.
- Define AI Kernel responsibilities as Agent lifecycle, memory/context allocation, resource budgets, locks and leases, scheduling, cancellation, policy, event routing, recovery, and audit state.
- Treat Git and canonical repository documents as authoritative records. Project Brain graphs are provenance-bearing, freshness-aware, rebuildable projections over documents, code, Git, RunRecords, issues, PRs, tests, and external metadata; they do not silently replace their sources.
- Project Brain includes Decision, Architecture, Dependency, Task, and Execution graphs, with explicit links to code, tests, bugs, commits, issues, and pull requests.
- Distinguish extension types: an Agent plugin supplies a role/capability worker, a Skill supplies a validated workflow, a Tool performs an external capability, and a Workflow composes events, Skills, Agents, Tools, verification, and approval gates.
- Add a privacy-aware Model Router that selects local or remote providers from task capability, data classification, policy, cost, latency, and availability. Sensitive content defaults to an approved local route and cannot be sent remotely without policy authority.
- The one-sentence user experience compiles intent into an inspectable goal, dependency plan, authorization scope, execution graph, verification, and audit trail. External or destructive workflow stages still require explicit or pre-authorized policy approval.

Rationale:

The differentiator is the Kernel below IDEs, not another editor shell. A graph projection enables impact reasoning while preserving the repository as recoverable memory. Separating Agents, Skills, Tools, workflows, and models prevents marketplace extensions or model output from silently gaining authority.

Consequences:

- VS Code, IntelliJ, Desktop, web, and CLI can share the same Kernel and Project Brain.
- V1, V2, and V3 are product milestones, not claims about current implementation maturity.
- Workflow automation may cover issue, branch, development, test, review, commit, push, PR, and merge, but each externally visible or destructive transition must satisfy the approval policy recorded in the run.
- Marketplace packages require provenance, signatures or integrity evidence appropriate to risk, compatibility metadata, permissions, isolation, review, disable, removal, and rollback.
- Knowledge Graph storage technology remains undecided until the Project Brain delivery gate; the contract is graph semantics and provenance, not a specific graph database.
- Local Llama or other on-device models and remote Claude, GPT, Gemini, DeepSeek, or future providers remain adapters behind the same Model Gateway.

### 2026-07-14: Make Enhancer An Event-Driven Interoperable AI Operating Platform

Status: Accepted Decision

Context:

- The current foundation proves repository context, planning, governed Tool execution, evidence, and bounded Agent Loop transitions, but it does not yet provide an operating substrate for long-lived or multi-role work.
- A linear Chat -> Tool -> Stop design cannot support Planner -> Coder -> Reviewer -> Tester pipelines, resumable scheduling, external clients, or independent evolution of runtime components.
- Workspace awareness, reusable Skills, MCP interoperability, and model routing have become core platform requirements rather than optional editor features.
- Event Bus and IPC Message Bus responsibilities overlap unless their semantic and transport layers are explicitly separated.

Decision:

- The final product target is **Enhancer OS**, composed of Desktop, CLI, API, Workspace, Project Brain, Memory, MCP, Agent Runtime, Event Bus, Skill Engine, Plugin Marketplace, Model Router, Scheduler, and governed Cloud Sync.
- Use one typed messaging architecture: the Event Bus defines domain events and subscriptions; the Message Bus provides envelopes, queues, delivery, replay, and backpressure; IPC is a transport adapter for the same envelopes across process boundaries.
- Runtime Agents MUST communicate through the bus once the runtime boundary exists. Direct Planner-to-Coder or Coder-to-Reviewer calls are not the target architecture.
- Build the Agent Runtime as a persisted state machine around Goal -> Planner -> Executor -> Memory -> Reflection -> Retry -> Done, with bounded budgets and explicit stop reasons.
- Add a first-class Workspace layer for project files, active and selected context, Git state, diagnostics, terminal-session metadata, and later editor state. Project Brain combines governed repository memory with Workspace observations; it does not replace either source.
- Treat Skills as validated, progressively loaded, composable workflows that can form chains such as Spring -> Java -> Database -> Test while preserving least-privilege Tool scope.
- Promote MCP to a core interoperability layer with both server and client boundaries so Claude Code, Cursor, VS Code, and other model clients can share governed Tools, resources, and memory.
- Keep the immediate Gate 4 verification and RunRecord dependency order. Introduce Workspace, messaging, runtime, MCP/model gateway, Skill, plugin, interface, multi-agent, sync, and self-improvement capabilities only through their delivery gates.

Rationale:

An AI operating system needs durable state, shared context, asynchronous coordination, reusable behavior, and interoperable capability exposure. Separating semantic events from transport avoids duplicate buses while allowing the first implementation to remain in-process and later gain IPC or durable queues without rewriting Agent contracts.

Consequences:

- The current sequential Agent controller remains a verified bootstrap slice and will later become a runtime worker behind bus contracts.
- Event envelopes require identity, causation, correlation, schema version, provenance, authorization context, and idempotency semantics before asynchronous execution.
- Workspace is a governed snapshot boundary, not unrestricted editor or terminal access.
- MCP, plugins, Skills, and models cannot bypass Tool policy, evidence, independent verification, RunRecord, or user approval.
- Multi-agent execution follows a stable single-agent runtime and durable messaging; it is not introduced as direct Agent-to-Agent calls.
- Cloud Sync remains opt-in and must define encryption, conflict resolution, ownership, and secret exclusion before implementation.
- The owner's qualitative assessment that roughly 20-25% of the intended foundation is established is recorded as planning context, not verified capability maturity or release progress.

### 2026-07-14: Harden Agent Runs With Repository Approval And Semantic Progress

Status: Accepted Decision

Context:

- Gate 3 integrates real Tool results, but `approvedTask` is only a non-blank string and the integration test does not derive it from repository context.
- Progress currently includes an opaque evidence reference, so identical content stored under a new UUID can appear to be progress and evade stagnation detection.
- Tool failures expose only success or failure; a real retry policy cannot distinguish timeout, cancellation, denial, invalid requests, or temporary failures without parsing prose.
- The public `AgentRunState` record constructor accepts caller-supplied progress keys and structurally valid states outside the governed transition path.

Decision:

- Introduce `ApprovedTask` and `ApprovedTaskReader`. The reader consumes `CURRENT_TASK.md` from `ProjectContext` and requires explicit `Task ID`, `Status: In Progress`, `Task`, `Approval`, and `Allowed Tools` sections.
- Bind every initial `ToolRequest` to the approved task's Tool-name scope before execution policy is applied. Repository approval evidence is explicit provenance, not a cryptographic signature or permission escalation.
- Add structured `ToolFailureCode` to every failed `ToolResult`; successful results carry no failure code. The executor assigns codes at its policy and execution boundaries.
- Provide a standard failure classifier that retries only explicitly temporary failures and timeouts; all other codes are terminal by default.
- Add an optional SHA-256 content digest to `VerificationEvidence.capture` and use it for semantic progress. Opaque storage references and human-readable summaries do not define progress identity.
- Replace the public Agent run record constructor with a final immutable class whose constructor is private. Only the initial approval factory and package-owned controller transitions can construct states.

Rationale:

These changes turn the RED scenarios into production invariants instead of test conventions. Approval, failure semantics, and progress equality become structured and deterministic, while state transitions remain controlled by the orchestration boundary.

Consequences:

- A repository task without an active status, explicit approval evidence, or Tool scope cannot start an Agent run.
- Execution policy remains an additional deny-over-allow boundary; repository approval cannot broaden it.
- Identical evidence content remains identical progress even when persisted at different references.
- Retry behavior no longer depends on diagnostic message text.
- Gate 4 can consume structured approval, failure, and evidence identities in its verifier and RunRecord.
- Signature-backed approval, argument-level authorization, identity federation, mutation Tools, independent verification, and RunRecord persistence remain deferred.

### 2026-07-14: Stop Tool Success At The Independent Verification Boundary

Status: Accepted Decision

Context:

- Delivery Gate 3 must connect a real `ToolResult` to the bounded Agent Loop without implementing the Gate 4 independent verifier.
- Treating Tool success as task completion would let worker output bypass the accepted rule that completion requires successful independent verification.
- Retry behavior must be deterministic without parsing diagnostic prose or allowing a Tool to grant itself execution authority.

Decision:

- Add immutable `AgentRunState` with an externally approved task, a caller-supplied pending `ToolRequest`, the last `ToolResult`, loop status, and deterministic progress key.
- Add `AWAITING_VERIFICATION` as an explicit terminal loop state and stop reason; a successful Tool result reaches this boundary but cannot reach `COMPLETED` in Gate 3.
- Add `AgentRunController` as the orchestration owner. It consumes an existing `ToolExecutor`, immutable `ExecutionPolicy`, and external `ToolFailureClassifier`; it does not implement Tools, create requests, expand policy, or approve tasks.
- Keep terminal failures as `FAILED`; retain the same pending request only for failures classified `RETRYABLE`.
- Derive progress keys from canonical Tool request/result content so identical retry results activate the existing maximum-iteration and stagnation rules.
- Reuse the existing bounded loop engine for both the small `AgentLoopState` contract and the richer Agent run state without weakening the 20/3 defaults or precedence rules.

Rationale:

The verification-wait boundary preserves separation of duties while still proving the real Context -> approved task -> Tool -> evidence -> loop transition. External retry classification avoids unreliable message parsing and prevents a Tool from deciding its own authority or finality.

Consequences:

- Gate 3 can stop successfully executed work without claiming it is verified or completed.
- Gate 4 becomes the only component allowed to turn an independently accepted result into `COMPLETED`.
- Repeated identical retryable failures remain bounded and observable as `STAGNATED`.
- A denied Tool remains uninvoked even if its implementation would attempt mutation; the controller cannot alter the caller's allow/deny policy.
- LLM decisions, Tool mutation, Git or network authorization, independent verification, RunRecord persistence, CLI wiring, and multi-agent routing remain deferred.

### 2026-07-14: Persist Complete Evidence As Atomic Integrity-Checked Envelopes

Status: Accepted Decision

Context:

- `VerificationEvidence` requires a complete-output reference when its 4096-character tail is truncated, but no current component makes that reference real.
- Gate 2 must provide durable resolution and corruption detection without broadening Tool authority or fabricating references.
- Separate payload and metadata files would make atomic publication across both files difficult in the first filesystem implementation.
- Automatic retention cleanup would delete data and is not required to prove the Gate 2 exit criteria.

Decision:

- Add an `EvidenceStore` boundary with a filesystem implementation that generates UUID run and evidence identities.
- Store metadata and UTF-8 payload together in one versioned binary envelope.
- Publish evidence by atomic move from a temporary file in the final run directory; do not fall back to a non-atomic move.
- Record creation time, UTF-8 byte length, and SHA-256 digest and validate all of them during resolution.
- Use opaque `evidence/<run-id>/<evidence-id>` references and reject malformed, missing, oversized, or corrupted artifacts.
- Add an explicit maximum-content and retention-duration policy, but perform no automatic cleanup in Gate 2.
- Connect a persistence-enabled `ReadFileTool` through `EvidenceRecorder`; the request correlation identity is a run identity previously created by the store.

Rationale:

A single atomic envelope is the smallest durable format that keeps reference metadata and content consistent. Digest, length, strict decoding, bounded reads, and reference containment make evidence failures observable before later verification consumes them.

Consequences:

- Truncated Tool output can carry a real reference that resolves after the Tool call.
- Short output remains in memory and does not create unnecessary evidence artifacts.
- Gate 2 detects accidental or unauthorized artifact modification but does not provide encryption, signatures, or external tamper-proof storage.
- Evidence deletion, compaction, migration, distributed storage, Agent Loop integration, independent verification, and RunRecord remain deferred.

### 2026-07-14: Restore Self-Hosting Context And Roadmap Compatibility Before Gate 2

Status: Accepted Decision

Context:

- The Planner still recognizes only the retired `## Phase ...` and `Status: Ready` grammar.
- The canonical Roadmap now uses `## Delivery Gate ...` and `Status: Specified - Next`, so the Planner cannot propose the actual next Enhancer task.
- The executable Context Reader loads only eight root documents even though repository startup governance requires `.ai/` to be read first.
- Existing tests use the retired Roadmap grammar and synthetic root-only context, so they do not detect either self-hosting regression.

Decision:

- Restore the self-hosting planning path before beginning Delivery Gate 2.
- Make the seven governed `.ai/` Markdown files explicit required context inputs before the eight canonical root documents.
- Replace the retired phase parser with the accepted Delivery Gate maturity grammar and select the first gate marked `Specified - Next`.
- Map the selected gate's required-capability or scope bullets into proposal scope and its exit criteria into proposal acceptance criteria.
- Add a regression test that reads the actual Enhancer `ROADMAP.md`, plus context-order tests that prove `.ai/` is loaded first.

Rationale:

Repository-backed memory and deterministic next-task proposal are core self-hosting promises. Later Tool and evidence work must not proceed while the front of that flow is known to reject the repository's own source of truth.

Consequences:

- Delivery Gate 2 remains next but is temporarily blocked by this foundation recovery task.
- The Planner intentionally follows the current canonical Delivery Gate grammar rather than retaining undocumented compatibility with the retired Phase/Ready format.
- Adding or removing governed `.ai/` bootstrap documents requires synchronizing the explicit executable context list.
- Dynamic Markdown interpretation, proposal ranking, LLM planning, and automatic task acceptance remain out of scope.

### 2026-07-14: Implement Gate 1 As A Bounded Read-Only Tool Boundary

Status: Accepted Decision

Context:

- Delivery Gate 0 verifies Tool result and evidence invariants but executes no Tool.
- Gate 1 must produce one real `ToolResult` without introducing evidence persistence, shell mutation, Git writes, network access, or LLM behavior.
- A path-prefix check alone is insufficient because traversal and symbolic links can escape an approved project root.
- Truncated evidence cannot truthfully reference complete output until Gate 2 provides an EvidenceStore.

Decision:

- Introduce immutable `ToolRequest`, `ExecutionPolicy`, and a minimal `Tool` interface under `com.enhancer.tool`.
- Use an in-process `ToolExecutor` registry with unique names and structured failure conversion.
- Make deny override allow and enforce cancellation before and after invocation plus a positive execution timeout.
- Implement `ReadFileTool` as the only production Tool in Gate 1.
- Require relative paths, real-path containment, regular files, strict UTF-8, and a policy size ceiling no greater than the existing 4096-character evidence boundary.
- Keep deterministic fake Tools in tests as the immediate consumer of the generic executor contract.

Rationale:

This is the smallest real external-boundary slice that exercises policy, execution, result, and evidence together. Strict path and output limits prevent the read-only first Tool from creating hidden authority or unverifiable truncated evidence.

Consequences:

- Allowed temporary project files can produce real successful `ToolResult` values.
- Denial, malformed input, traversal, missing files, invalid UTF-8, cancellation, timeout, and Tool exceptions remain observable failure results.
- The first size ceiling is intentionally conservative and may be raised only after Gate 2 persists full evidence.
- Agent Loop integration, evidence persistence, independent verification, CLI, shell, Git, network, and LLM behavior remain deferred.

### 2026-07-14: Promote Foundation Contracts Through Executable Vertical Slices

Status: Accepted Decision

Context:

- The repository has 21 production Java files and approximately 479 production lines centered on contracts and deterministic control rules.
- Context, planning, loop termination, and Tool evidence invariants are tested, but there is no application entry point, concrete Tool execution, Agent-Loop/Tool integration, evidence persistence, LLM boundary, or end-to-end runtime.
- The existing roadmap uses `Implemented` for narrow slices, which can be mistaken for operational product completeness.
- Continuing to add isolated contracts would increase skeleton breadth without proving that Enhancer can execute a useful governed workflow.

Decision:

- Use capability maturity states: Specified, Contract Verified, Integrated, Operational, and Released.
- Replace ambiguous standalone `Implemented` roadmap labels with the most precise verified maturity.
- Promote capabilities through executable vertical slices with explicit integration and operational exit gates.
- Supersede the sequential independent verifier as a standalone next task. First implement the bounded Tool execution boundary and evidence persistence, then integrate the Agent Loop and sequential verifier in the same E2E delivery track.
- Require every new foundation contract to name its current or immediately following integration consumer.

Rationale:

Control-plane safety contracts were necessary before Tool or LLM execution, but their value is realized only when they participate in an observable, recoverable run. Maturity gates prevent focused unit tests from being reported as product readiness and make the path from foundation to usable system explicit.

Consequences:

- The next product task is the first Tool Execution Boundary slice, not an isolated verifier record.
- The first operational milestone must run Context → Tool → Evidence → Verification → Stop → Run Record through a supported entry point.
- Independent verification remains mandatory but moves after real ToolResult production and evidence persistence.
- Skill, LLM, MCP, plugin, multi-agent, and self-improvement work remains gated behind the executable single-agent path.
- Roadmap, architecture guides, state, and handoff documents must use the new maturity vocabulary.

### 2026-07-14: Recover Existing Git Metadata Instead Of Initializing New History

Status: Accepted Decision

Context:

- The active C:\Enhancer directory contains the project files but no .git metadata.
- PowerShell history shows that the GitHub repository was cloned into the nested C:\Enhancer\Enhancer path.
- Windows Recycle Bin metadata identifies the deleted nested clone and its Enhancer origin, main branch, and commit history.
- Running git init in the active directory would create unrelated history and lose the existing repository relationship.

Decision:

- Prefer the existing Recycle Bin metadata, but if it is unavailable before copying, create a temporary no-checkout clone from the verified Enhancer origin and copy only its .git metadata into C:\Enhancer.
- Preserve the available recovery source until validation completes and verify that non-.git working files are unchanged.
- Do not perform checkout, reset, clean, commit, push, or other worktree reconciliation as part of recovery.

Consequences:

- Existing Git history and origin can be restored without overwriting current work; a fresh clone may be used when the deleted metadata is no longer available.
- The restored status may show substantial local differences that must be reviewed separately.
- Recovery success does not authorize committing or pushing those differences.

### 2026-07-14: Restructure The Constitution As A Versioned Kernel

Status: Accepted Decision

Decision:

Enhancer will replace the repetitive Constitution 1.0.0 structure with Constitution 1.1.0 as a concise normative Kernel. The revised document defines normative language, document responsibilities, lifecycle states, authorization and safety boundaries, fresh verification evidence, self-hosting safeguards, failure recovery, and an explicit amendment process.

The 300-page Codex guidebook target applies to the complete repository documentation system, not to `CONSTITUTION.md` alone. Detailed procedures and component contracts remain in `AGENTS.md`, `.ai/`, `docs/`, RFCs, prompts, and Skills.

Rationale:

The previous Constitution repeated repository-memory and working-process rules while omitting the governance needed for safe self-hosting. A smaller but stronger Kernel reduces startup context, makes mandatory rules easier to locate, and prevents detailed implementation guidance from becoming constitutional debt.

Consequences:

- Constitution version increases from 1.0.0 to 1.1.0.
- `MUST`, `SHOULD`, and `MAY` receive explicit meanings.
- Proposal, Accepted Decision, Active Task, Implemented, Verified, Completed, and Released become distinct lifecycle states.
- Destructive and externally visible actions require explicit authority; secrets and external content receive safety rules.
- Constitution amendments require explicit user approval, Decision Log rationale, semantic versioning, and mirror review.
- Technology choices and detailed procedures remain changeable through Architecture, RFCs, and task documents rather than being frozen in the Kernel.

### 2026-07-14: Colocate Examples With Specifications And Tests

Status: Accepted Decision

Decision:

Enhancer will not maintain a standalone `examples/` directory. Conceptual examples belong in the `docs/` or RFC document that owns the contract, while executable examples belong in focused tests.

Rationale:

The standalone Agent Loop and Tool examples were already behind the implemented contracts. Colocation reduces duplicate descriptions, prevents conceptual samples from drifting away from code, and keeps the repository structure smaller.

Consequences:

- Remove `examples/agent-loop.md`, `examples/tool-example.md`, `examples/skill-example.md`, and the empty-directory marker.
- Do not treat examples as a separate source of truth.
- New conceptual examples must be updated with their owning specification.
- Observable executable behavior remains demonstrated and verified through tests.

### 2026-07-14: Adopt External Agent Harness Patterns Selectively

Status: Accepted Decision

Decision:

Enhancer will treat [MoAI-ADK](https://github.com/modu-ai/moai-adk) and similar agent harnesses as reference implementations rather than runtime dependencies. The first adopted pattern is an explicit terminal outcome for the deterministic Assisted Development Loop that composes repository context reading and task planning. Other useful patterns will be introduced only in the roadmap slice that owns them.

Rationale:

MoAI-ADK contains useful operational patterns, including explicit stop reasons, stagnation detection, bounded verification evidence, progressive Skill loading, artifact provenance, and approval-protected self-improvement. Importing its framework, provider-specific schemas, or Git workflow would duplicate Enhancer components and weaken the current document-driven approval model. Selective, provider-neutral adoption preserves the useful semantics without coupling the products.

Consequences:

- The current slice adds no MoAI package, command, generated configuration, or runtime dependency.
- The first Assisted Development Loop is a single read-and-plan pass with explicit outcomes and no repository mutation.
- Repeated-loop termination and stagnation are implemented in a separate Agent Loop slice.
- Verification evidence belongs to the Tool System; progressive loading belongs to the Skill System; provenance belongs to Plugin and template management.
- Token budgets follow LLM integration, while self-improvement requires snapshot, approval, verification, and rollback contracts before implementation.
- Claude-specific configuration, automatic commits or pushes, and parallel multi-agent orchestration are not adopted.

Adoption sequence:

1. Implement bounded repeated-loop termination and consecutive-state stagnation detection in the Agent Loop.
2. Define a bounded verification-evidence contract with Tool results.
3. Add a sequential independent verifier after the single-agent loop is stable.
4. Add progressive Skill loading while preserving the rule that Proposed catalog entries are not loadable.
5. Add artifact provenance when Plugin or template installation exists.
6. Add provider-neutral token and context budgets only after an LLM invocation boundary exists.
7. Implement self-improvement only after snapshot, human approval, independent verification, and rollback contracts exist.

The sequence does not conflict with `.ai/` rules: each item remains a small `CURRENT_TASK.md` scope, uses test-first verification for observable behavior, preserves proposal/decision/implemented-state separation, and cannot override the Constitution. The independent verifier begins as a sequential component, not multi-agent routing.

### 2026-07-14: Use Bounded Deterministic Agent Loop Termination

Status: Accepted Decision

Decision:

The first repeated Agent Loop uses immutable state transitions and explicit `COMPLETED`, `FAILED`, `MAX_ITERATIONS`, and `STAGNATED` stop reasons. The default ceiling is 20 executed steps. Stagnation means the progress key remains unchanged for 3 consecutive executed steps; both limits are constructor-configurable for focused tests and later runtime configuration.

Rationale:

Explicit bounded exits prevent silent infinite work before Tool or LLM execution is introduced. A caller-provided deterministic step keeps the loop independently testable and avoids premature Agent, Tool, prompt, or provider abstractions.

Consequences:

- Terminal status wins over stagnation after a step.
- Maximum iteration wins when its ceiling and the stagnation threshold coincide.
- Iteration count reports executed steps, including the terminal step.
- Maximum-iteration and stagnation results retain the latest running state for diagnosis.
- Tool execution, verification evidence, independent verification, LLM calls, and multi-agent routing remain out of scope.

### 2026-07-14: Bound Tool Verification Evidence

Status: Accepted Decision

Decision:

Every future Tool result will include structured verification evidence. The first contract limits summaries to 512 characters and retained output tails to 4096 characters. Output exceeding the tail limit must be marked truncated and include a non-blank reference to the complete output. The contract records original output length without implementing persistence.

Tool result status is explicit. An optional exit code supports process-like tools while allowing file or API tools to omit it. When present, success requires exit code zero and failure requires a non-zero code.

Rationale:

Unbounded command output would consume future Agent Context and obscure the most useful recent diagnostics. Keeping a bounded tail plus a reference preserves inspectability without introducing an LLM-specific token model, filesystem policy, or concrete Tool implementation.

Consequences:

- `VerificationEvidence` is mandatory on every `ToolResult`.
- Evidence summaries and output tails are bounded before they can enter Agent Context.
- Truncated output cannot be represented without a reference to the complete evidence.
- The contract does not claim that referenced evidence has been persisted or independently verified.
- Evidence storage, real Tool execution, Agent Loop integration, and the sequential independent verifier remain separate tasks.

### 2026-07-14: Use A Repository Gradle Wrapper

Status: Accepted Decision

Decision:

Enhancer will store a Gradle Wrapper in the repository and use Java 17 as the build runtime. A global Gradle installation is not required for normal project builds.

Rationale:

The Wrapper makes local development and future CI reproducible while matching the existing Java 17 Gradle build. It also removes reliance on user-specific cached Gradle paths.

Consequences:

- Developers run `gradlew.bat` on Windows or `./gradlew` on Unix-like systems.
- Wrapper scripts, properties, and the wrapper JAR are version-controlled.
- Java 17 remains an external prerequisite and is not committed to the repository.

### 2026-07-14: Adopt Verified Skill And Evidence Operating Rules

Status: Accepted Decision

Decision:

Enhancer will adopt repository-defined Skill authoring rules, memory distillation, test-first behavior for observable feature and bug-fix changes, and fresh verification evidence before completion claims. The initial Skill catalog remains explicitly proposed until corresponding `SKILL.md` files exist. Task cycles do not force commits; commits remain controlled by repository policy and user instruction.

Rationale:

These rules strengthen repeatability and verification while preserving Document Driven Development, least privilege, proposal-state separation, and the existing human approval boundary for Git operations.

Consequences:

- RFC-0002, RFC-0005, RFC-0007, RFC-0008, and RFC-0009 describe the accepted direction.
- `.ai/skill_rules.md` defines operational authoring constraints for future Skills.
- Proposed catalog entries cannot be treated as installed or available Skills.
- `allowed-tools` uses a small documented permission vocabulary rather than undeclared tool names.
- Actual Skill workflows, loading, and runtime enforcement remain future tasks.

### 2026-07-12: Start Planner With Deterministic Repository Proposals

Status: Accepted Decision

Decision:

The first Planner consumes `ProjectContext`, blocks proposals while `CURRENT_TASK.md` is active, and otherwise proposes the first ready roadmap phase. Its output has explicit `PROPOSAL` state and structured reason, scope, acceptance criteria, out-of-scope items, and risks.

Rationale:

This reaches the first self-hosting planning behavior without introducing an LLM, hidden chat context, document mutation, or premature ranking logic.

Consequences:

- Planner behavior is deterministic and unit-testable.
- A proposal cannot be confused with an accepted decision.
- Natural-language planning, proposal ranking, persistence, and execution remain future work.

### 2026-07-12: Implement Context Reader As A Single Java Module

Status: Accepted Decision

Decision:

The first Repository Context Reader is implemented in a single Gradle Java 17 project under `com.enhancer.context`. The required document order is represented by an enum, and the returned context uses immutable records.

Rationale:

This matches the existing architecture guide and provides a stable structured input for future planning without premature modules, Spring wiring, or domain abstractions.

Consequences:

- Required startup documents have one canonical code-level order.
- Missing documents fail with a checked exception that identifies the path.
- Future context sources can build on `ProjectContext` without changing this task into a full Context Builder.

### 2026-07-10: Manage Major Designs As RFCs

Status: Accepted Decision

Decision:

Enhancer will manage major design areas as RFC-style Markdown documents under `docs/rfcs/`, starting with RFC-0001 through RFC-0012.

Rationale:

The project is large enough that design topics need stable identifiers, reviewable history, and clear references. RFC-style documents make long-term architecture easier to maintain across multiple AI agents and sessions.

Consequences:

- Major architecture changes should add or update an RFC.
- Accepted direction should still be summarized in `DECISION_LOG.md`.
- RFC statuses distinguish Draft, Accepted, Implemented, and Superseded.
- The initial RFC track covers Constitution, AI Behavior, Prompt Contract, Context Builder, Planner, Tool, Skill, Memory, Multi-Agent, AI Operating System, Plugin SDK, and Self Improvement.

### 2026-07-10: Adopt Six-Month AI Development OS Roadmap

Status: Accepted Decision

Decision:

Enhancer will use a six-month open-source roadmap that evolves from Constitution, Architecture, Context, Agent Loop, and Tool toward Planner, Skill, Memory, Prompt Engine, MCP, Plugin, Git, Terminal, VSCode Extension, CLI, Dashboard, Multi-Agent, Scheduler, Background Agent, Self Improvement, Plugin SDK, and Open Source Release.

Rationale:

The target is larger than a 30-day prototype. A six-month roadmap gives the project realistic phases while keeping the 30-day self-hosting milestone as the first checkpoint.

Consequences:

- The 30-day goal remains the first self-hosting milestone.
- Long-term architecture is tracked separately from immediate implementation tasks.
- Work remains Sprint-based and document-driven.

### 2026-07-10: Read `.ai/` Before Every AI Work Session

Status: Accepted Decision

Decision:

Every AI agent should read the `.ai/` folder before starting work. The folder contains AI-only operational documents: `constitution.md`, `workflow.md`, `coding_rules.md`, `architecture.md`, `prompt_rules.md`, and `memory.md`.

Rationale:

The root documents are canonical, but `.ai/` gives agents a compact operational entry point. This allows the user to say "항상 .ai 폴더를 읽고 시작해" and have a consistent startup rule across Codex, Claude, GPT, and future Enhancer agents.

Consequences:

- `prompts/SESSION_START.md` includes `.ai/` in the required reading order.
- `AGENTS.md` requires agents to read `.ai/` before work.
- `.ai/` must mirror operational rules without replacing root canonical documents.

### 2026-07-10: Treat Docs As A Multi-Agent Prompt Book

Status: Accepted Decision

Decision:

Each major `docs/` chapter will end with a `Prompt Book` section containing separate prompts for Codex, Claude, and GPT.

Rationale:

Enhancer is developed by multiple AI agents with different strengths. A shared chapter can guide all agents, but each agent needs role-specific instructions to reduce ambiguity.

Consequences:

- Codex prompts focus on implementation and verification.
- Claude prompts focus on architecture and risk review.
- GPT prompts focus on explanation, task framing, and session continuity.
- New chapter documents should include all three prompt types.

### 2026-07-10: Use Explicit Session Resume Protocol

Status: Accepted Decision

Decision:

New ChatGPT sessions must be resumed by providing the core repository documents, because ChatGPT cannot automatically read the user's local Enhancer repository across sessions.

Rationale:

The project depends on repository-backed memory. Without an explicit resume protocol, a new session may rely on incomplete chat memory and drift away from the source of truth.

Consequences:

- `prompts/CHATGPT_SESSION_RESUME.md` defines the required upload/paste workflow.
- `SESSION_HANDOFF.md` must remain complete enough to recover short-term state.
- Documents override chat history when conflicts occur.
- The human owner controls final approval and push.

### 2026-07-10: Operate Enhancer As A Real Open Source Project

Status: Accepted Decision

Decision:

Enhancer will be operated as a real open source project, not as a one-off chat artifact or documentation-only repository. The project will include documentation, code, ADRs, tests, inline specification examples, and shared prompts for Codex, Claude, and GPT.

Rationale:

The expected scope is too large for a single chat session. A Git-managed, chapter-based, reviewed workflow allows the project to grow over months without losing architectural consistency.

Consequences:

- Work proceeds by Sprint and small tasks.
- Documentation and code evolve together.
- ADR review is required for meaningful design changes.
- AI roles are explicit: Codex implements; ChatGPT supports architecture, backend design, agent research, documentation, and review.
- Git repository documents remain the source of truth.

### 2026-07-10: Use Codex-Ready Chapter Documents

Status: Accepted Decision

Decision:

Enhancer will maintain feature documents under `docs/` as Codex-ready prompts. Each document should describe the goal, architecture, task boundary, tests, and out-of-scope items for a major platform capability.

Rationale:

The project is too large to drive from chat history. Chapter-based Markdown specifications allow Codex, Claude, GPT, and future Enhancer agents to implement one slice at a time from repository state.

Consequences:

- `docs/` is part of the operating system for development, not passive documentation.
- New major capabilities should receive a prompt-style specification before implementation.
- Implementation should proceed sprint by sprint rather than attempting a full Cursor-like platform at once.

### 2026-07-10: Use Document Driven Development

Status: Accepted Decision

Decision:

Enhancer will follow Document Driven Development. New work must move through Constitution, Architecture, ADR / Decision Log, Task, Implementation, Test, and Documentation Update before it is considered complete.

Rationale:

Enhancer depends on repository documents as durable memory. If code changes happen before architecture, decisions, and tasks are clarified, future AI sessions will lose the reason behind the implementation.

Consequences:

- Agents must not jump directly from idea to code.
- Important architectural changes must be recorded before or during implementation.
- `CURRENT_TASK.md` remains the scope boundary for implementation.
- Documentation update is part of Definition of Done.

### 2026-07-10: Build Enhancer As A Self-Hosting AI Development OS

Status: Accepted Decision

Decision:

Enhancer is not a Cursor clone. Enhancer is a self-hosting AI Development Operating System that should eventually read its own repository context, understand project state, propose the next task, and assist its own development.

Rationale:

The project goal is not to copy Cursor's interface or behavior. The goal is to build a durable framework where AI agents can resume work from repository state and eventually help operate the project themselves.

Consequences:

- The first product slice should prioritize context reading and task planning over UI polish.
- Repository documents are product inputs, not only project management artifacts.
- The 30-day milestone is for Enhancer to propose next tasks from repository context.

### 2026-07-10: Use Repository Documents As Durable Memory

Status: Accepted Decision

Decision:

Enhancer will use repository documents as the durable memory for future ChatGPT and Codex sessions.

Rationale:

Conversation memory is unreliable across sessions. Repository files can be read, reviewed, committed, and treated as the single source of truth.

Consequences:

- Agents must read the required documents at session start.
- `SESSION_HANDOFF.md` must be updated at session close.
- Proposals must not be treated as accepted decisions until recorded here.

## Proposals

- Define the product scope for Enhancer.
- Choose the initial implementation stack.
