# User continuation request on 2026-08-18 after pure installation evidence reconciliation

Status: Accepted Decision

## Context

The pure evidence-point and reconciliation contracts are Completed locally. They define
an exact lookup identity and a read-only resolver boundary, but neither the transaction
cursor nor the semantic phase-evidence value has a deterministic integrity-checked byte
format suitable for a later filesystem adapter. The user requested that the project
continue.

Two bounded read-only reviews independently concluded that implementing a production
store or resolver now would overstate authority and recovery. The current resolver has
no evidence body, while cursor CAS locking, symlink confinement, atomic publication,
durability, and installer-only permissions require separate explicit contracts.

## Decision

Authorize the smallest repository-local test-first increment defining deterministic
bounded binary formats for the complete installation transaction cursor and one phase-
evidence value, plus pure exact filename derivation for their future point artifacts.
Each format uses a distinct domain magic and payload kind, explicit envelope and domain
schemas, bounded length-framed strict UTF-8 fields, stable enum names, canonical field
order, and a SHA-256 digest over the domain-separated header and body. Decode reconstructs
the existing domain values so all state and evidence invariants run again. Evidence
decode additionally requires the caller's exact `InstallationPhaseEvidencePoint` and
rejects valid but foreign content.

The implementation remains byte-array and string based. This decision authorizes no
`Files`/`FileChannel` use, filesystem root, production transaction store or evidence
resolver, evidence writer/body, lock/CAS adapter, atomic move, durability claim,
permission/native adapter composition, real installation effect, commit, push, merge,
release, or deployment.

## Rationale

A later protected filesystem adapter needs one canonical representation before it can
prove exact replay, changed reuse, cursor CAS, or point binding. A pure codec makes
corruption, truncation, unsupported schema, noncanonical text, and cross-point
substitution testable without silently granting persistence or installation authority.
Domain separation prevents a valid cursor envelope from being decoded as evidence or
vice versa.

## Consequences

- Cursor and evidence bytes are deterministic, bounded, strict, independently typed,
  and rejected on digest, length, schema, kind, canonicality, or trailing-byte failure.
- Path strings retain an explicit local filesystem-provider/dialect marker; a foreign
  dialect is refused rather than silently reinterpreted.
- The exact filename contract contains no root, path traversal, discovery, scan,
  creation, or mutation behavior.
- The envelope digest detects accidental/torn/corrupt content only. It is not a MAC or
  signature, cannot resist an authorized byte rewriter, and cannot prevent rollback to
  an older valid cursor.
- Production evidence persistence/order, mutable cursor locking and CAS, exact-real
  roots, no-follow reads, atomic publication, directory durability, permissions,
  retention, and adapter composition remain future explicitly authorized work.
