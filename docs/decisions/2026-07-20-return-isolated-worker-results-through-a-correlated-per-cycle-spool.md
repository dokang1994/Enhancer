# 2026-07-20: Return Isolated Worker Results Through A Correlated Per-Cycle Spool With RunRecord As Authority

Status: Accepted Decision

Context:

- Connection 3 now has both halves as unwired capabilities: `FileSpoolMessageTransport` carries a route and envelope to a peer, and `IsolatedWorkerLauncher` runs a bounded child. Neither is connected to `AgentRunExecution`.
- `AgentRunExecution.execute` must return a `run-record/<uuid>` string. The child's only channel back is an exit code, which cannot carry a reference, so the connection is blocked on a design decision rather than on capability.
- The 2026-07-20 spool decision already deferred exactly this: the spool is one-directional by construction, and results returning the other way need their own spool or their own decision.
- Two alternatives were rejected. Having the child write a RunRecord that the parent then locates by scanning the shared store is ambiguous, because no AgentRun or attempt identity appears in the store's lookup contract, so a parent cannot distinguish this cycle's record from an orphan left by an earlier at-least-once retry. Inferring the outcome from the exit code alone cannot produce a reference at all.
- `ResultPayload(taskId, runRecordReference, verificationStatus)` and its codec already exist, so the return direction needs no new payload kind or wire format.
- `DurableAgentRunFinalizer` already resolves the RunRecord and derives the runtime terminal state and queue disposition from the record's own verification status. A result message that claimed authority over the outcome would contradict that and would let a child decide its own verdict.
- The current spool primitives are not yet an execution-request channel. `IsolatedWorkerMain` decodes one message and exits without executing; `FileSpoolMessageTransport.read` does not delete or claim a file; and the child selects the first file in the directory. A single shared spool reused across cycles would therefore re-read earlier work and earlier results.

Decision:

- Return results through a reverse spool carrying an existing `ResultPayload` envelope. Add no payload kind and no wire format.
- Give each cycle its own invocation root containing separate `work/` and `result/` spools, keyed by the Goal and AgentRun identities the worker already generates before its queue claim. Exactly one valid message is expected in each direction; zero, several, corrupt, or identity-mismatched contents fail closed.
- Reconstruct the child's execution from the `WorkPayload` the work spool already carries plus the project, evidence, and RunRecord roots supplied by the parent as launcher arguments. Store locations are parent configuration, never payload data, because a payload crossing a process boundary is untrusted input and must not be able to redirect where artifacts are written.
- Treat the result envelope as a claim, never as authority. Before returning a reference the parent validates that the envelope's task, correlation, logical-run, and causation identities match the dispatched work, that the payload is exactly a `ResultPayload`, that the reference resolves in the same shared `RunRecordStore`, and that the claimed verification status equals the resolved record's own status. A mismatch fails closed.
- Keep `DurableAgentRunFinalizer` as the final authority. The new adapter returns only the reference; the finalizer continues to resolve the RunRecord and derive the runtime terminal state and queue disposition from it.
- Fix the durable ordering as: parent persists the work message, child executes the Gate 1-4 pipeline, child persists the RunRecord, child publishes the matching `ResultPayload`, parent validates the envelope and resolves the RunRecord, `AgentRunExecution` returns the reference, the worker's existing cycle-intent checkpoint records it, and acknowledgement and finalization continue unchanged.
- On re-entry, check the result spool for a valid result before launching a new child. A cycle interrupted after the child published its result recovers without re-executing.
- State the remaining window explicitly rather than claiming to close it. A child that persists a RunRecord and dies before publishing its result leaves an orphaned RunRecord and will be re-executed, which is the same at-least-once consequence the in-process worker already accepts and records.

Rationale:

The reverse spool reuses the adapter, the codec, and a payload that already exists for this purpose, so the return direction costs no new format. Per-cycle roots are what make the primitives usable at all: the adapter deliberately neither claims nor deletes, so isolation has to come from the namespace rather than from the read. Validating the envelope against the resolved RunRecord keeps the authority where every other part of this runtime already puts it — a message states what a child believes, and the durable record states what actually happened — which is also why store locations come from the parent rather than from the payload.

Consequences:

- Connection 3 becomes wirable: the adapter, the process lifecycle, and the return path together satisfy the `AgentRunExecution` contract.
- Each cycle leaves a directory tree under its invocation root. Nothing deletes it in this increment, so spool retention and cleanup become named follow-on work.
- A child can no longer influence where artifacts are written, because roots are launcher arguments rather than payload fields.
- A dishonest or buggy child cannot promote its own run: a claimed status that disagrees with the resolved record fails closed.
- The orphaned-RunRecord window stays open and is documented rather than silently accepted.
- Out of scope: retry policy, cancellation of a running child, concurrent cycles sharing one invocation root, spool cleanup, and carrying anything other than a result reference back across the boundary.
