# User continuation request on 2026-08-14 after pure installation transaction coordination

Status: Accepted Decision

## Context

The pure one-phase coordinator and outcome-bearing point-store writes are Contract
Verified with test-only fakes. Each port result binds a semantic evidence SHA-256, but
the schema-v1 state discards that result when its pending phase becomes succeeded. A
future persisted store therefore could not retain the exact accepted phase result or
distinguish it from a changed succeeded replacement.

The user requested that the project continue. Read-only scope and security review found
that the smallest prerequisite is a platform-neutral immutable evidence prefix, not a
production store, permission-adapter composition, native gateway, or real effect.

## Decision

Authorize a test-first schema-v2 installation transaction state that retains one
bounded semantic-evidence binding for every succeeded phase in the exact required
order. Each binding carries its own schema version, transaction, phase, pending
revision, lowercase SHA-256, and only for activation the exact observed activation
identity. The coordinator passes the port result directly into the state successor and
the fake store validates the exact prefix-monotonic transition.

This decision authorizes no evidence body or resolver, serializer, integrity envelope,
production persistence or port, pending reconciliation, permission-adapter change,
native or host inspection, installation effect, activation, deployment, or external
Git delivery.

## Rationale

Persist-first invocation ordering does not retain the result accepted afterward. A
schema-versioned ordered prefix is the minimum value contract that closes that link
without inventing durability or effect evidence. Binding the pending revision makes the
result's exact transition point explicit, while keeping plan and environment facts in
their owning transaction state avoids duplicating immutable authority data.

## Consequences

- Schema v2 rejects schema v1 rather than silently reinterpreting it; no migration is
  needed or authorized while no production serializer or store exists.
- Pending-to-succeeded appends exactly one matching binding; succeeded-to-next-pending
  preserves the complete prefix; history cannot be reordered, truncated, or replaced.
- Exact terminal replay retains eleven ordered semantic identities and invokes no port.
- The prefix records only values returned by injected ports. It does not establish
  evidence-content verification, storage integrity, durability, restart-safe recovery,
  exactly-once effects, or installation success.
- Point-resolvable evidence revalidation, production persistence, permission-adapter
  composition, native enforcement, and operational recovery remain future work.
