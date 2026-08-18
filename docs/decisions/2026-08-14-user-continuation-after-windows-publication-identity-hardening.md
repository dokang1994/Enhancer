# User continuation request on 2026-08-14 after Windows publication identity hardening

Status: Accepted Decision

## Context

The Windows adapter's atomic-publication target identity is now retained across exact
replay, durability, and published-security recheck. The completed task records the next
repository-local prerequisite as a pure platform-neutral installation transaction and
recovery contract backed only by an in-memory test fake. The user requested that the
project continue.

The accepted installation order requires a bounded transaction intent before the first
installation effect, while the retry contract requires exact identity, phase, and
activation-boundary classification. No production transaction state or store port yet
represents that requirement.

## Decision

Authorize the smallest repository-local test-first increment defining a schema-v1
immutable installation transaction state, strict pending/succeeded phase progression,
finite typed store failures, and a revisioned platform-neutral store port. An in-memory
implementation may exist only inside contract tests.

The state begins at the first required phase with the exact planned and normalized
environment binding, and remains effect-free until private staging. A pending step
requires exact revalidation; it does not authorize automatic replay or prove that an
effect occurred. This decision authorizes no
coordinator, production persistence, native gateway, host observation, installation or
permission effect, activation, deployment, or external Git delivery.

## Rationale

A later coordinator needs one explicit persist-first and compare-and-exchange boundary
before it can safely order effect-capable adapters. Defining only the immutable grammar
and port connects that future consumer without pretending that an in-memory fake proves
durability, restart recovery, or platform enforcement.

## Consequences

- Exact plan, environment/filesystem, release, permission-policy, and activation
  identities remain fixed across every revision.
- Phase state alternates `PENDING` and `SUCCEEDED` in the existing required order;
  exact replay is mutation-free and changed or stale reuse fails closed.
- The three documented metadata/activation recovery regions become pure classifications,
  not effect authority or installation-success evidence.
- Production storage, integrity envelopes, platform evidence references, coordination,
  native enforcement, and operational recovery remain future explicitly authorized work.
