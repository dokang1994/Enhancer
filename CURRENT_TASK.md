# Current Task

## Status

Completed

## Task

Connect one durable Scheduler queue claim to one recoverable durable Goal/AgentRun planning, readiness, and fenced lease-acquisition path without worker execution.

## Task ID

gate-8-durable-queue-runtime-dispatch

## Justified By

- 2026-07-16: Bridge One Durable Queue Claim Into One Recoverable Leased AgentRun

## Context

Gate 8 has separate Contract Verified durable queue and fenced AgentRun lifecycle contracts. The next bounded integration must retain the exact claimed WorkItem, persist each existing boundary, and recover from any intermediate prefix without pretending that the two stores form one atomic transaction.

## Acceptance Criteria

- Add one durable queue-to-runtime coordinator and one immutable dispatch result.
- Validate caller-supplied Goal ID, AgentRun ID, lease owner, and lease duration before queue mutation.
- Use an existing active WorkItem or persistently claim the next ready WorkItem; return empty when no work is active or ready.
- Create a missing Goal from the exact claimed WorkItem or recover an existing matching Goal.
- Advance only the missing lifecycle prefix through named AgentRun planning, readiness, and fenced lease acquisition.
- Repeated calls with the same WorkItem, Goal, AgentRun, and current owner return the existing unexpired lease without renewal or extra revision.
- Reject mismatched retained WorkItem, different AgentRun identity, different unexpired owner, Awaiting-Verification state, and terminal state.
- At lease expiry, recover the runtime to `READY` and permit a new owner to acquire a strictly greater fence.
- If queue claim persistence fails, create no runtime state.
- If any runtime persistence step fails, retain the active queue claim and durable runtime prefix so a later call resumes safely.
- Preserve exact WorkItem authority/provenance and add no Tool permission, worker execution, external effect, queue completion, or retry.
- Verify with focused in-memory and filesystem integration tests, full regression, Java 17 strict lint, actual-document self-hosting, structural/reference, and whitespace checks.

## Out Of Scope

- Tool or worker process execution
- Queue completion or acknowledgement coupling
- Result messages, verification, or RunRecord resolution
- Retry or more than one AgentRun per Goal
- Cancellation, pause/resume, reassignment, priority, fairness, or budgets
- External-effect idempotency, compensation, effect records, or effect fencing
- Cross-store transactions, rollback, multi-process locking, or distributed clock-skew handling
- Schema migration, history cleanup, or parent-directory fsync
- CLI/API/Message Bus production wiring
- Commit, push, PR, merge, release, or deployment

## Approval

Approved by the user's 2026-07-16 request to continue from the documented durable queue-to-lifecycle integration task.

## Verification

- RED: production compilation passed and test compilation failed with 13 aligned errors naming only the missing dispatcher and dispatch-result contracts.
- Focused GREEN: 31 tests across 7 dispatcher, durable queue/runtime, filesystem-store, and package-boundary suites passed with no skips, failures, or errors.
- Full regression: 57 suites and 251 tests; 249 passed, 2 existing Windows symbolic-link setup skips, 0 failures, and 0 errors under `--warning-mode all`.
- Java 17 strict lint passed across all 149 production sources with `-Xlint:all -Werror`.
- Queue claim failure created no runtime, while failures at each of four runtime persistence boundaries retained a recoverable active claim and durable prefix.
- Cross-instance filesystem recovery requeued and reclaimed the same WorkItem and returned the exact existing Unicode-bearing unexpired lease.
- Mismatch, different AgentRun/owner, post-execution, caller-metadata, expiry/new-fence, and public result-identity invariants passed.
- Post-document Context Reader, Planner, Assisted Loop, package-boundary, dispatcher, lifecycle, queue, and filesystem integration verification passed 36 of 37 tests with 1 existing Windows symbolic-link setup skip and no failure or error.
- Structural checks resolved the active task's accepted decision, retained exactly one Gate 8 `Status: Specified - Next` marker, and passed tracked/untracked whitespace validation.

## Next

Couple matching fence-checked AgentRun execution completion to durable queue acknowledgement with recoverable ordering, without adding Tool execution, result handling, or external effects.
