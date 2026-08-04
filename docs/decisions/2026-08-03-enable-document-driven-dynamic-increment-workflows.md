# 2026-08-03: Enable Document-Driven Dynamic Increment Workflows

Status: Accepted Decision

## Context

Enhancer currently records one bounded Active Task in `CURRENT_TASK.md` and one
machine-written development-session checkpoint. This preserves authority and recovery,
but a user request that contains several related increments must currently be split and
reactivated through repeated chat turns even when every increment, dependency, stop
condition, and approval boundary can be declared up front.

The user asked to change the document structure so Enhancer development can proceed as
a dynamic workflow across several increments. The Constitution still requires one
single Active Task, small reviewable work, explicit authority, fresh verification, and
separate lifecycle states. A document structure may organize several increments inside
that one approved task, but it cannot create parallel authority, automatic approval, or
a second task owner.

## Decision

Add an optional `## Dynamic Workflow` section to `CURRENT_TASK.md` for an approved task
containing two through sixteen related bounded increments. The task's existing `Task`,
`Task ID`, `Approval`, `Acceptance Criteria`, and `Out Of Scope` sections remain the
authority envelope. The workflow section is execution structure only and does not widen
that envelope.

The section records one workflow identity, `Sequential` mode, a bounded increment count,
a deterministic selection rule, explicit stop conditions, and ordered increment entries.
Each increment has a stable identity, `Pending`, `In Progress`, `Completed`, or `Blocked`
state, dependency identities, bounded scope, exit criteria, verification expectation,
and next action. At most one increment may be `In Progress`. The first dependency-ready
pending increment is selected only after the previous increment's required evidence is
read. A failed, blocked, stagnant, budget-exhausted, authority-expanding, or unsafe-
recovery result stops the workflow rather than selecting another increment.

The operating rules, compact AI workflow, implementation/session prompts, Architecture,
and human README must describe the same connection. A repository structural test will
pin the actual `CURRENT_TASK.md` example and the required instruction surfaces. No new
canonical root document is introduced: `CURRENT_TASK.md` remains the sole active-task
and next-task owner, while `docs/verification-log.md` remains the evidence owner.

## Rationale

Nesting a bounded increment graph inside the existing Active Task provides dynamic
selection and interruption recovery without amending the Constitution's document
ownership or lifecycle model. It also avoids a second mutable work-queue document that
could conflict with `CURRENT_TASK.md`.

Sequential selection matches the implemented single-session checkpoint and single-agent
baseline. Later Workflow Engine or multi-agent runtime work may consume the same ideas,
but cannot be claimed from a documentation contract.

## Consequences

- A single-increment task may omit `## Dynamic Workflow`; a task with two or more
  pre-authorized increments uses the bounded section.
- Increment state and cursor updates do not replace task approval, checkpoint position,
  verification evidence, capability maturity, or delivery history.
- Adding an unplanned increment, widening scope, or changing external-action authority
  requires a new accepted decision or explicit user approval before work continues.
- Commit, push, merge, release, deployment, destructive action, background execution,
  parallel agents, and automatic self-improvement remain separately authorized or
  unimplemented.
