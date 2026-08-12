# 2026-08-12: Implement Cancellation Trust Maintenance With A Persistent Stateless Lock

Status: Accepted Decision

## Context

The accepted operator-maintenance design requires a fixed installation-scoped
nonblocking operating-system lock and forbids automatic cleanup. One earlier decision
bullet also said INSTALL required the lock artifact to be absent. Those statements
cannot both govern a recoverable implementation: an operating-system lock file normally
survives release, and treating its existence as state would prevent every later retry or
rotation unless the implementation deleted it.

The implementation also needs concrete fixed installation-relative identities and one
shared production parser contract so that candidate public policy bytes are hashed and
validated from the same bounded snapshot.

## Decision

- The fixed lock file is `enhancer-cancellation-trust-maintenance-v1.lock`, directly
  beside the exact installed application JAR. Its file bytes and mere existence carry no
  state, authority, generation, success, or current-operation meaning. INSTALL and ROTATE
  create or open it without following links, attempt one exclusive operating-system lock
  without waiting, hold that lock through post-switch verification, and refuse only
  active contention or an invalid lock path. The lock file is retained after release.
- This decision refines the earlier INSTALL wording: fixed metadata must be absent for
  INSTALL, but the stateless lock file may already exist. No cleanup or deletion authority
  is created.
- The fixed trust directory is `enhancer-cancellation-trust-policies-v1`, directly beside
  the application JAR and required to preexist as an exact-real non-symbolic directory.
  Maintenance does not create an installation layout or permissions.
- An immutable policy filename is
  `enhancer-cancellation-trust-policy-<lowercase-sha256>.conf` inside that directory.
  Unique non-authoritative policy candidates use the same directory; metadata candidates
  sit beside the fixed metadata file. Publication requires same-directory atomic moves.
  Portable Java's target-exists behavior for `ATOMIC_MOVE` is provider-dependent, so
  policy no-replacement is guaranteed for cooperating maintenance processes by the fixed
  lock, an immediate target check, and exact resolution before metadata commit. This is
  not protection from a privileged or noncooperating path-replacement actor.
- Production policy loading exposes one bounded exact-file canonical snapshot operation
  returning its internally computed digest and defensive bytes. Pinned runtime loading
  delegates to the same operation and then compares its independently installed pin.
  Production metadata loading similarly exposes the exact canonical bytes and digest,
  plus canonical encoding/parsing for maintenance candidate revalidation.
- ROTATE production-loads the current binding, validates the candidate, and constructs
  the requested exact metadata before classifying replay or stale intent. If current
  metadata bytes already equal the requested new bytes, it returns `EXACT_REPLAY`
  without a write even when expected-current names the immediately preceding binding;
  this recovers an atomic switch whose success report was lost. If current and requested
  bytes differ, expected-current must match both initially observed and immediately pre-
  switch current metadata in constant time or rotation is refused without changing
  authority. Replay precedence never permits stale mutation.
- The implementation is an unexposed `com.enhancer.maintenance` library surface with
  distinct `install` and `rotate` methods. It is not wired to the application plugin,
  `EnhancerCli`, a `scheduler-*` command, runtime request input, environment, properties,
  or deployment launcher in this task.

## Rationale

Separating lock ownership from lock-file existence gives cooperative serialization and
safe retries without inventing mutable state or deletion authority. Concrete fixed names
remove request-selected artifact identity. Reusing the production canonical snapshot
contracts prevents a second permissive parser and binds the internally derived digest to
the exact bytes that were validated.

## Consequences

- An abandoned but unlocked maintenance lock file is normal and has no recovery meaning.
- Deployment must still protect the JAR parent, metadata, trust directory, lock, and
  ancestors from the runtime principal; this implementation does not inspect or change
  those permissions.
- The library can be verified in isolated temporary installation trees, but invoking it
  against a real installation or adding a launcher remains separately authorized.
- Lock/CAS remains cooperative concurrency protection, not privileged rollback
  protection or a distributed lock.
