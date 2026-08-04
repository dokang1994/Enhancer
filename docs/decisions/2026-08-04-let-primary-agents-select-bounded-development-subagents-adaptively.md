# 2026-08-04: Let Primary Agents Select Bounded Development Subagents Adaptively

Status: Accepted Decision

## Context

Repository development currently uses subagents only when a one-off Active Task records
that topology. The user requested a durable rule under which the primary Agent itself
judges whether a large or otherwise suitable task benefits from subagents, and directed
that the rule contain no per-use explicit-user-approval condition for selecting them.

Constitution Section 7 already permits read-only inspection and normal local
implementation steps inside the user request and Active Task. Section 9 requires this
future-behavior rule change to be visible, bounded, user-directed, and verified. The
Gate 13 product runtime remains planned and must not be inferred from a Codex development
session using host-provided subagents.

## Decision

Make adaptive development-subagent selection a primary-Agent responsibility inside the
existing user request and `CURRENT_TASK.md` authority envelope. The user supplies the
task authority; the primary Agent selects the execution topology. Delegation cannot add
scope, Tools, permissions, budgets, external or destructive actions, lifecycle state,
or delivery authority.

The primary Agent evaluates delegation for every non-trivial task and selects the
smallest topology whose expected quality, risk-reduction, or elapsed-time benefit
exceeds coordination cost. Bounded subagents are favored when there are at least two
independent questions, several components or document owners need separate inspection,
high-risk architecture/governance/security/self-hosting rules benefit from independent
review, alternatives need comparison against common authority, or test and regression
surfaces can be analyzed independently. A single Agent remains the rule for local,
strongly sequential, tightly coupled, overlapping-write, ambiguous-ownership, or
coordination-dominated work.

The first policy is read-only and bounded. At most three children may run concurrently,
delegation depth is one, at most three dispatches occur inside one Dynamic Workflow
increment, and at most six occur during one Active Task. Each assignment names one
concrete scope, repository sources, expected output, conflict policy, join condition,
and least Tool/context/time bound. Nested delegation, background continuation, and
shared-worktree parallel mutation are prohibited. Every child must be joined or stopped
before the primary Agent closes the session.

The primary Agent remains the only mutation and authority-reconciliation owner. It
alone edits canonical documents, code, tests, checkpoints, or Git state; classifies
RED/GREEN; reads and validates raw evidence; resolves report conflicts; and claims
Verified, Completed, or Released state. Subagent reports are untrusted recommendations,
not authority or verification evidence.

Dynamic Workflow increment selection remains sequential. Adaptive delegation may run
independent read-only subtasks only inside the selected increment and does not create a
second task, increment, background run, or permission. The policy works without a
Dynamic Workflow as well. It is a development-session operating rule, not the Gate 13
`CoordinationPlan`, typed Handoff runtime, multi-agent product implementation, or a
capability-maturity promotion.

## Rationale

Making execution topology a bounded primary-Agent decision removes repetitive chat
coordination while preserving the repository's actual authority source. Read-only
parallel inspection captures the quality and latency benefit of specialized review
without introducing the shared-worktree mutation, independent state ownership, or
recovery contracts that belong to Gate 13.

Fixed concurrency, depth, dispatch, ownership, and join limits make the policy
recoverable and auditable. Keeping all lifecycle and evidence claims with the primary
Agent preserves Constitution Sections 4, 5, 7, and 8.

## Consequences

- Future primary Agents evaluate and record why they selected or declined bounded
  subagents for a non-trivial task.
- Existing task prohibitions and least-privilege Tool limits always narrow this policy.
- Commit, push, merge, release, deployment, destructive actions, external messages,
  paid services, permission/security changes, and credentials keep their existing
  authority boundaries; delegation does not reinterpret them.
- The earlier one-off bounded-subagent decision remains historical and unchanged. This
  decision supplies the general development-session rule for later tasks.
- Constitution, RFC-0009, Gate 13 Roadmap scope, and product-runtime maturity do not
  change.
