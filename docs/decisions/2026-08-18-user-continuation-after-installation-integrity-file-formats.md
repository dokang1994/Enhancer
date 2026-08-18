# User continuation request on 2026-08-18 after installation integrity file formats

Status: Accepted Decision

## Context

The deterministic transaction-cursor and phase-evidence formats are Completed locally.
They provide bounded canonical bytes and exact leaf names but intentionally perform no
filesystem access. The recorded next prerequisite is immutable evidence creation and a
locked transaction-cursor compare-and-exchange boundary. The user requested that the
project continue.

Two bounded read-only reviews agreed that the existing `InstallationTransactionStore`
already is the correct cursor CAS port and that a second public CAS abstraction would be
duplicative. They differed appropriately at the evidence boundary: a semantic evidence
record can be stored immutably, but without an evidence body or host observation it
cannot truthfully implement `InstallationPhaseEvidenceResolver.resolveAndRevalidate`.

## Decision

Authorize a repository-local, test-first `FileSystemInstallationTransactionStore`
implementing the existing cursor port over one caller-provisioned, pre-existing,
absolute exact-real non-symbolic directory. Create and compare-and-exchange use the same
stable per-transaction nonblocking operating-system lock and retain it across exact
current-state resolution, validation, same-root candidate write and validation,
required atomic publication, and post-publication resolution. Reads are bounded,
no-follow, exact-point, and regular-file only. Exact replay is detected under the lock
and performs no rewrite. Lock contention is a distinct typed refusal.

Also authorize the smaller pure `InstallationPhaseEvidencePointStore` contract for
create-exclusive semantic evidence and exact point reads. First creation and unchanged
replay are distinct; changed point reuse conflicts; absence is explicit. This task does
not provide a filesystem implementation of that port and does not implement or compose
the evidence resolver.

Filesystem tests may mutate only JUnit-owned temporary directories. No permission
adapter, installer, operator, CLI, runtime, build distribution, or real installation
path is composed.

## Rationale

The cursor format and existing store semantics are sufficient to make cooperative local
process CAS concrete now; another gateway-shaped CAS port would move rather than close
the race boundary. Holding one stable lock across read, semantic successor validation,
and atomic replacement makes stale cooperating writers fail closed and keeps exact
replay mutation-free.

Evidence has a different maturity. Persisting its semantic receipt is useful and can be
specified without pretending that a digest is independently revalidated host evidence.
Separating `read` from `resolveAndRevalidate` preserves that distinction until a bounded
phase-specific body/reference and revalidation contract is accepted.

## Consequences

- Cursor create and CAS have a concrete uncomposed filesystem implementation with typed
  missing, conflict, corruption/schema, capacity, contention, unavailable, and uncertain
  publication outcomes.
- Atomic move support is mandatory; there is no non-atomic fallback. File data is forced
  before publication and the published record is decoded again.
- These Java path checks and OS locks coordinate cooperating local processes only. They
  are not descriptor-relative native confinement, distributed locking, permission
  enforcement, publisher authentication, or rollback protection.
- File `force(true)` plus atomic namespace replacement does not prove parent-directory or
  sudden-power-loss durability. A permission/native adapter and a directory-durability
  contract remain required before production installation composition.
- Phase evidence gains only an immutable semantic point-store port and test-local fake.
  Production evidence persistence, evidence bodies, host revalidation, resolver
  implementation, automatic reconciliation wiring, capacity policy, and retention
  remain future work.
