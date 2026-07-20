# 2026-07-16: Bridge One Durable Queue Claim Into One Recoverable Leased AgentRun

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
