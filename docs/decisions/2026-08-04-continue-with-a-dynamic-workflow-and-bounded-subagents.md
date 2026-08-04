# User request on 2026-08-04 to continue with a dynamic workflow and bounded subagents

Status: Accepted Decision

## Context

The first concrete filesystem runtime-event reference publisher is completed and
verified locally. Its recorded next boundary is either supported publisher composition
or a bounded consumer. The user explicitly requested that project work continue through
a Dynamic Workflow configured with subagents.

Repository dynamic workflows remain sequential at the increment level and do not grant
multi-agent authority by themselves. Subagent output is also input rather than
repository authority or independent verification.

## Decision

Authorize one bounded multi-agent analysis increment inside the next Active Task. The
primary Agent may run at most three concurrent read-only subagents with distinct scopes:
the supported Control receiver composition, Scheduler composition alternatives, and
test/governance risks. Subagents may inspect repository files and report recommendations
but may not edit files, mutate Git/checkpoints, run external effects, or claim
verification. They must finish and return their findings before the primary Agent
selects the next sequential increment.

The primary Agent alone reconciles the reports against the Constitution, accepted
decisions, Architecture, current task, working-tree diff, and fresh tests. It owns every
canonical edit, test-first RED/GREEN classification, checkpoint transition, and
completion claim. Later workflow increments remain sequential even though bounded
read-only discovery is parallel within the first increment.

This authority grants no background work after the increment, nested delegation,
subagent mutation, commit, push, merge, release, deployment, destructive action,
credential change, paid service, external message, or broader multi-agent runtime
implementation.

## Consequences

- The selected supported composition can use parallel bounded discovery without
  weakening the single Active Task or evidence authority.
- Any subagent conflict or scope-expanding recommendation stops at primary review; it
  cannot silently become an Accepted Decision or implementation requirement.
- Further multi-agent mutation or background orchestration requires a separate accepted
  authority boundary.
