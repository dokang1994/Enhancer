# User continuation request on 2026-08-14 after pure installation transaction recovery contracts

Status: Accepted Decision

## Context

The pure schema-v1 transaction cursor and revisioned store port are now Contract
Verified with a test-only in-memory fake. The completed task records the next local
prerequisite as a pure coordinator over fake source verification, phase effects,
activation, and that store. The user requested that the project continue.

Read-only review found that the current store return value does not say whether this
call freshly persisted a pending state or exact-replayed another caller's state. A
coordinator that invokes a port from either result could duplicate an effect.

## Decision

Authorize the smallest repository-local test-first increment adding an outcome-bearing
store write receipt and a pure one-phase-at-a-time coordinator. Only `CREATED` or
`ADVANCED` may grant the current call permission to invoke the newly persisted pending
phase. `EXACT_REPLAY` and pre-existing pending state require reconciliation without a
port call.

The first two existing phases route only to a source/preflight verifier over supplied
bindings, activation routes only to its distinct port, and the other eight phases route
only to a phase-effect port. Every implementation remains a test-local in-memory fake.
This decision authorizes no production store or port, native gateway, host observation,
installation or permission effect, activation, deployment, or external Git delivery.

## Rationale

Store idempotency is not invocation ownership. An explicit fresh-write receipt is the
minimum contract that lets a coordinator prove persist-before-call ordering while
refusing ambiguous pending recovery. Limiting each call to one phase makes every
failure boundary and retry decision externally visible without adding a workflow
engine, loop, thread, lease, or automatic retry.

## Consequences

- Store create and compare-and-exchange distinguish fresh mutation from exact replay.
- Port results bind transaction, exact phase, bounded semantic evidence identity, and
  observed activation identity where applicable before phase success is recorded.
- Existing pending state is never automatically invoked; exact terminal replay invokes
  nothing and mutates nothing.
- The schema-v1 cursor still stores no phase-evidence prefix. This contract verifies
  call ordering only and cannot prove durability, restart recovery, exactly-once
  installation, or overall installation success.
- Persisted phase evidence, production persistence, existing permission-adapter
  composition, native enforcement, and operational recovery remain future work.
