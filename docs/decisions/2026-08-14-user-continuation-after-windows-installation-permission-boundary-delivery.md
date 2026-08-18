# User continuation request on 2026-08-14 after Windows installation permission boundary delivery

Status: Accepted Decision

## Context

The Windows installation permission adapter and fake-gateway evidence boundary were
delivered to `main`. The completed task then required separate authority before any
production/default/native gateway or real installation enforcement. The user requested
that the project continue.

A read-only continuation review found a narrower correctness defect inside the existing
accepted adapter contract: replacement publication validates a new target file identity
but does not retain it for the following durability and published-security checks.

## Decision

Authorize the smallest repository-local test-first hardening increment that binds the
successful atomic-publication target identity to subsequent durability and
published-security evidence. Exact replay may reuse only the same retained identity;
same-volume identity drift must fail at its actual boundary.

The public gateway/evidence contracts remain unchanged. This decision authorizes no
production/default/native gateway, native dependency, host or filesystem security-state
inspection, permission or file mutation, installer composition, deployment, cleanup,
external Git delivery, or other external effect.

## Rationale

Closing a concrete fail-closed identity gap in the already accepted adapter is smaller
and safer than adding another installation layer or selecting native technology before
its privilege, fixture, integration, and recovery prerequisites exist.

## Consequences

- Focused RED/GREEN work may change only the adapter's internal retained identity state
  and its fake-gateway tests, followed by owning-document synchronization.
- Fake-gateway verification remains contract evidence only and cannot promote real
  Windows installation or permission enforcement maturity.
- The next broader repository-local prerequisite remains a pure installation
  transaction/recovery contract; production native enforcement still requires a later
  explicit task.
