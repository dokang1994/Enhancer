# 2026-07-16: Separate Execution Acknowledgement From Verified Queue Completion And Sequence Remaining Connections

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
