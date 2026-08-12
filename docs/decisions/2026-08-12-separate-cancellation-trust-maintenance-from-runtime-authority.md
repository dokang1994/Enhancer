# 2026-08-12: Separate Cancellation Trust Maintenance From Runtime Authority

Status: Accepted Decision

## Context

The runtime cancellation command obtains its public trust binding from one protected
fixed metadata sibling of the installed application JAR. The same binding must later be
provisioned and rotated, but making `EnhancerCli`, `scheduler-apply-cancel`, a request,
proof, repository, or runtime principal able to write it would collapse the independent
approval boundary.

Adding a generation or timestamp to the same replaceable metadata would not prevent a
privileged rollback: an actor able to replace the installed application, metadata, and
policy can replace that field too. Portable Java owner/ACL checks likewise cannot be the
approval root. The contract must distinguish crash/concurrency safety from real anti-
rollback and must not claim permission protection it cannot establish.

## Decision

- Keep installed cancellation trust metadata v1 unchanged. Its exact policy path and
  whole-file SHA-256 remain sufficient to identify one immutable public policy binding.
  Do not add a mutable generation, timestamp, or self-declared revision as a false anti-
  rollback signal.
- A future maintenance implementation is a separate operator-only entry point and
  launcher, not an `EnhancerCli` or `scheduler-*` command and not a runtime/API/editor/
  Desktop request path. The operator and runtime principals are deployment identities:
  the runtime principal must not have write, rename, or delete authority over the JAR,
  metadata, policy directory, lock, or installation ancestors. Actual ACL/ownership
  enforcement and deployment remain outside this decision and require explicit user
  authority.
- The operator supplies one candidate public policy file and the installed application
  location only to the separate maintenance surface. It cannot supply the resulting
  pin. The implementation reads one exact-real regular non-symbolic candidate through a
  finite no-follow snapshot, rejects private fields and all noncanonical or unsupported
  policy bytes through the existing public-only loader contract, and computes SHA-256
  over that same snapshot.
- Publish the verified policy first under a content-addressed filename owned by the
  installation trust directory. First publication uses exclusive creation through a
  same-directory candidate, requires an atomic move, and exact-resolves an already
  existing identical digest artifact. It never overwrites a policy in place.
- Build the unchanged canonical metadata v1 bytes from the final exact policy path and
  computed digest. Write and re-read a same-directory candidate, then switch the sole
  fixed metadata point last with a required atomic move. The loadable authority before
  the switch is the previous exact binding and after it is the exact new binding; no
  partial candidate can be selected by runtime.
- INSTALL and ROTATE are distinct operations. INSTALL requires the fixed metadata and
  maintenance lock to be absent and refuses an existing binding. ROTATE requires an
  existing valid binding, one installation-scoped nonblocking exclusive operating-
  system lock, and the caller's expected SHA-256 of the complete current canonical
  metadata bytes as compare-and-swap. It re-reads and checks that digest immediately
  before metadata publication, rejecting a stale writer without changing authority.
- If the requested new exact binding is already installed, ROTATE returns exact-replay
  success without rewriting policy or metadata. Otherwise policy-first/metadata-last
  publication proceeds. Failure before metadata publication keeps the old binding;
  failure reporting after atomic metadata publication is recovered by exact-new replay.
  Automatic rollback is forbidden because it may re-enable removed or revoked trust.
- Retain previous content-addressed public policies and abandoned non-authoritative
  candidates. This contract grants no scan, retention deletion, cleanup, in-place
  repair, fallback, or automatic recovery authority. A separately authorized cleanup
  contract must prove no current binding references a candidate before deletion.
- Rotation semantics remain explicit policy content. Safe key rollover normally uses
  one policy containing old and new public keys, followed by a separately approved
  policy that removes or revokes the old key. The tool may preview exact public
  configuration/policy/key differences but does not invent overlap, revocation, or
  compatibility authority. No proof, signature, private key, credential, or token is
  generated or stored.
- The installation lock and expected-current metadata digest prevent concurrent lost
  updates and stale rotation only. They do not prevent a privileged rollback of the
  application plus metadata and policies. That requires a future independently
  protected monotonic anchor such as signed release state, an OS package manager,
  keystore, TPM, or equivalent, with its own accepted decision and explicit security-
  control/deployment authority.

## Rationale

Separating maintenance from runtime preserves the independent trust root. Computing the
pin from one validated snapshot removes the dangerous path+pin self-assertion. Content-
addressed policy-first and fixed-metadata-last publication makes every crash boundary
resolve to an old or exact-new binding, while a lock plus compare-and-swap addresses
concurrent operator error without pretending to solve privileged rollback.

## Consequences

- The current runtime reader and metadata v1 format require no change.
- The future implementation must provide phase-level failure injection, exact replay,
  stale-writer, symbolic-path, private-field, and old/new binding tests before it may be
  called Implemented or Verified.
- This decision specifies no production writer and performs no installation or
  permission mutation. The current task cannot claim Operational provisioning,
  rotation, permission enforcement, cleanup, or anti-rollback.
