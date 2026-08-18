# User continuation request on 2026-08-18 after installation transaction contract delivery

Status: Accepted Decision

## Context

The schema-v2 installation transaction state retains an ordered semantic-evidence
identity for each succeeded phase, and the pure coordinator refuses to invoke a port
when it encounters an existing or exact-replayed pending state. The completed delivery
task records the next prerequisite as point-resolvable evidence revalidation and
reconciliation. The user requested that the project continue.

A pending state can represent a phase whose port returned before the succeeded
compare-and-exchange became observable. Invoking that phase again would be unsafe, but
the current contract has no exact point from which a separately persisted phase result
could be resolved and revalidated.

## Decision

Authorize the smallest repository-local test-first increment defining an immutable
point identity for one transaction phase and pending revision, a read-only resolver port
that resolves and revalidates only that exact point, and a pure reconciliation service.
The service may advance an existing pending state to succeeded only from an exact
revalidated phase-evidence binding and one compare-and-exchange. An absent point leaves
the state pending, and corrupt, foreign, unsupported, unavailable, or mismatched evidence
fails closed.

Every implementation remains a test-local fake. This decision authorizes no production
transaction or evidence store, serializer, integrity envelope, filesystem or native
gateway, permission-adapter composition, phase invocation, installation effect,
activation, cleanup, deployment, release, commit, push, or merge.

## Rationale

The transaction cursor deliberately cannot prove whether a pending phase produced an
external result. A deterministic point plus independent read-only revalidation is the
minimum prerequisite for recovering a lost success acknowledgement without treating
state equality as invocation authority or automatically repeating an effect. Keeping
reconciliation separate from the effect-capable coordinator makes the no-reinvocation
boundary structurally testable.

## Consequences

- The point identity binds exactly one transaction, required phase, and canonical
  pending revision; it grants no authority and supports no scan or discovery.
- Reconciliation resolves the transaction and evidence by exact identities, validates
  the returned phase result through the existing state successor, and persists at most
  one pending-to-succeeded transition.
- Missing evidence is an explicit mutation-free outcome; resolver and binding failures
  remain typed and leave the pending cursor unchanged.
- Succeeded or terminal state is observed without starting the next phase, invoking any
  phase port, or mutating the store.
- Durable formats, evidence bodies and integrity, production adapters/stores, automatic
  recovery, exactly-once effects, and installation success remain future work.
