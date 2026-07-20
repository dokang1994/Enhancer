# 2026-07-20: Record AgentLoop-Backed Execution Port Running The Approved Source Document Through The Gate 1-4 Pipeline Without A Payload Schema Change

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
