# Gate 8 AgentLoop-Backed Execution Port — Design

- Date: 2026-07-20
- Gate: Delivery Gate 8 (Agent Runtime and Scheduler)
- Connection: ROADMAP backlog #3, the real `AgentLoop`-backed execution port that
  replaces the injected deterministic `AgentRunExecution` stub used by
  `DurableAgentRunWorker` (sub-increment 3a).
- Maturity target: Integrated (the durable worker drives one claim to a real,
  verifier-produced terminal disposition through the actual Gate 1–4 pipeline).

## Problem

`DurableAgentRunWorker.runOneCycle` (3a, Contract Verified) closes the durable
queue -> runtime -> result loop, but its execution is an injected
`AgentRunExecution` port: every test supplies a deterministic stub that persists a
hand-built `RunRecord` and returns its reference. No production implementation runs
real Tool-driven work. This increment supplies that implementation so the durable
Scheduler worker actually executes the approved task through the already-Integrated
Gate 1 Tool boundary, Gate 2 evidence, Gate 3 Agent Loop, and Gate 4 independent
verification, and persists a real RunRecord the finalizer can resolve.

## Contract already required of any execution port

`DurableAgentRunFinalizer.finalizeAgentRun` resolves the returned reference and
reads a real verification status to derive the queue disposition
(`src/main/java/com/enhancer/runtime/DurableAgentRunFinalizer.java:56-62`,
`:100-108`). Therefore the port implementation MUST:

1. Persist the RunRecord through the **same** `RunRecordStore` (same
   `FileSystemRunRecordStore` root) the worker's finalizer is constructed with,
   and return `StoredRunRecord.reference()` verbatim (`run-record/<uuid>`).
2. Build its `ApprovedTask` so the finalizer binding holds:
   `approvedTask.taskId()` equals `dispatch.workItem().taskRevision().taskId()`
   and `approvedTask.sourceDocument()` equals
   `dispatch.workItem().taskRevision().sourceDocument()`.
3. Produce a real, verifier-derived `VerificationStatus` on the record, so the
   finalizer maps `VERIFIED -> VERIFIED_COMPLETED` and any other status ->
   `FAILED`.

## Scope decision: read-and-verify the approved task's own governed source document

The real read-file pipeline needs a **target relative path** (the `read-file`
`path` argument) and an **expected content SHA-256** (the `VerificationRequest`).
The current `WorkPayload`/`WorkItem` already carries both, addressed at the
approved task document itself:

- target path = `taskRevision().sourceDocument()` (e.g. `CURRENT_TASK.md`);
- expected SHA-256 = `taskRevision().sourceSha256()`, validated as
  `[0-9a-f]{64}` by `WorkspaceContractSupport.sha256`, which exactly satisfies
  `VerificationRequest.expectedContentSha256`.

So this increment executes the approved task's own source document as the governed
`read-file` target and verifies it against the approved revision digest. This is a
complete, honest end-to-end AgentLoop-backed execution that needs **no** Gate 7
`WorkPayload` schema change and **no** durable-queue/runtime serialization change.

Extending `WorkPayload` with an execution-input for an **arbitrary** target file
(distinct from the task document) remains a named, deferred follow-on; it touches
the Gate 7 envelope plus both filesystem serializers, and it additionally requires
a publish-time producer design (`WorkMessagePublisher` currently derives the
payload from the approved task and snapshot alone, so nothing yet supplies a
target/digest pair). The binding requirement above is why the natural minimal
target is the source document: it makes `approvedTask.sourceDocument()` equal the
revision's `sourceDocument` without new data.

Rejected alternatives (2026-07-20 review): re-reading the full `ApprovedTask`
through `ApprovedTaskReader` (current-document-only, hard-fails unless the status
is `In Progress`, cannot reproduce the approved revision); resolving execution
inputs from `snapshotId` (no snapshot persistence exists, the identity is not
resolvable); a side store of execution inputs keyed by workItemId (splits the
authority over "what the work is" away from the retained envelope).

**Derivation seam:** the port derives `(targetPath, expectedContentSha256)` from
the WorkItem in one private method. The follow-on payload extension replaces only
that derivation; the pipeline assembly, verification, and persistence are reused
unchanged.

## New type

`com.enhancer.runtime.AgentLoopAgentRunExecution implements AgentRunExecution`
(final class). Placement in `runtime` is allowed: `RuntimePackageBoundaryTest`
constrains only `loop`, `run`, and `kernel` import directions and never restricts
`runtime`; `application`, `loop`, `tool`, and `verification` do not import
`runtime`, so no cycle is introduced.

Constructor dependencies (all injected; no static state):

- `Path projectRoot` — the root the `read-file` Tool resolves the target against
  and contains it within.
- `EvidenceStore evidenceStore` — Gate 2 evidence run/capture/resolve.
- `RunRecordStore runRecordStore` — MUST be the same store the worker's finalizer
  uses (pinned wiring rule, mirroring the existing "same queue instance" rule).
- `Clock clock` — passed to `AgentRunFinalizer` for deterministic `recordedAt`.

`execute(AgentRunDispatch dispatch)`:

1. `WorkItem wi = dispatch.workItem(); ApprovedTaskRevision rev = wi.taskRevision();`
2. `ApprovedTask approvedTask = new ApprovedTask(rev.taskId(),` a deterministic
   non-blank description referencing the goal, a deterministic non-blank
   `approvalEvidence` referencing the AgentRun, `wi.allowedTools()`,
   `rev.sourceDocument())`. `wi.allowedTools()` must contain `read-file`.
3. `String logicalRunId = evidenceStore.createRun();`
4. `ToolRequest request = new ToolRequest(ReadFileTool.NAME, logicalRunId,
   Map.of(ReadFileTool.PATH_ARGUMENT, rev.sourceDocument()));`
5. `ExecutionPolicy policy = new ExecutionPolicy(projectRoot,
   Set.of(ReadFileTool.NAME), Set.of(),
   EvidenceStoragePolicy.MAX_SUPPORTED_CONTENT_BYTES, TOOL_TIMEOUT,
   CancellationToken.none());`
6. Run the controller inside a try-with-resources `ToolExecutor`:
   `AgentRunResult workerRun = new AgentRunController(executor, policy,
   ToolFailureClassifier.standard()).run(AgentRunState.ready(approvedTask,
   request), new AgentLoop(MAX_ITERATIONS, STAGNATION_THRESHOLD));`
7. Build the optional `VerificationRequest` only when
   `workerRun.stopReason() == AWAITING_VERIFICATION`, with
   `expectedContentSha256 = rev.sourceSha256()`.
8. `FinalizedAgentRun finalized = new AgentRunFinalizer(new
   DeterministicReadFileVerifier(evidenceStore), runRecordStore,
   clock).finalizeRun(workerRun, verificationRequest);`
9. `return finalized.storedRecord().reference();`

Loop bounds reuse the CLI reference values (`MAX_ITERATIONS = 5`,
`STAGNATION_THRESHOLD = 3`, `TOOL_TIMEOUT = 5s`).

## Behaviour

- **Verified path:** the target file exists, is contained, is within size, is
  strict UTF-8, and its SHA-256 equals `sourceSha256`. The verifier returns
  `VERIFIED`; the record's `finalStopReason` becomes `COMPLETED`; the finalizer
  maps it to `VERIFIED_COMPLETED`.
- **Digest mismatch:** the file resolves but its SHA-256 differs from
  `sourceSha256`. The verifier returns `REJECTED(CONTENT_MISMATCH)`; the record is
  non-verified; the finalizer maps it to `FAILED`. `execute` still returns a
  resolvable reference (the failure is carried in the RunRecord, not thrown).
- **Tool failure** (missing target, containment violation, oversize, non-UTF-8):
  the worker run stops `FAILED`/`AWAITING_VERIFICATION` without a `VERIFIED`
  status; the record is persisted and the reference returned; the finalizer maps
  it to `FAILED`. No target is written; the port performs a read-only Tool only.
- The port persists exactly one RunRecord per call and returns its reference; it
  creates no queue, runtime, lease, or checkpoint state and holds no external
  authority beyond the injected read-only Tool boundary.

## Fail-closed / boundary notes

- If `rev.taskId()` does not satisfy the stricter `ApprovedTask` identifier
  pattern, `ApprovedTask` construction throws; `execute` propagates it as a
  fail-closed error with no RunRecord persisted. Real task ids (`gate-8-...`)
  satisfy the pattern.
- Queue, runtime, evidence, and RunRecord remain separate durable boundaries; this
  port adds no cross-store transaction. Retry, cancellation, budgets, and multi-
  agent execution stay out of scope.

## Verification plan

Test-first, focused, mirroring the existing runtime suite style:

- `AgentLoopAgentRunExecutionTest` (new): over a real temp `projectRoot`,
  - verified: a real source document whose SHA-256 is used as `sourceSha256`
    yields a reference that resolves in the shared store to a `VERIFIED` RunRecord
    bound to `taskId` + `sourceDocument`;
  - mismatch: a `sourceSha256` that does not match the file content yields a
    non-`VERIFIED` (`REJECTED`) RunRecord and still returns a resolvable reference;
  - missing target: an absent `sourceDocument` yields a non-`VERIFIED` RunRecord.
- `FileSystemAgentLoopWorkerIntegrationTest` (new): wire the **real** port into
  `DurableAgentRunWorker` sharing one `FileSystemRunRecordStore` with the
  finalizer, and assert the durable worker drives a claim to
  `VERIFIED_COMPLETED` end to end (and a digest-mismatch WorkItem to `FAILED`,
  leaving the dependent blocked) — the whole real vertical slice.

## Out of scope

- The `WorkPayload` execution-input extension for an arbitrary target file distinct
  from the task document, and the durable-queue/runtime serialization changes it
  requires.
- Process isolation of the worker (3b) and a concrete local IPC adapter (3c).
- Write/mutation Tools, retry through additional AgentRuns, cancel/pause/resume,
  budgets, priority/fairness, multi-agent execution, schema migration.
- Commit, push, PR, merge, release, or deployment without a new explicit request.
