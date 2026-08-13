# Installed Cancellation Trust Maintenance Contract

Status: Contract Verified State Machine, Operator Main, And Separate Packaging; Installed Operation Deferred

This document specifies the operator-owned maintenance surface and governs the unexposed
state-machine library for the public
cancellation trust artifacts consumed by `scheduler-apply-cancel`. It is not an
implementation guide that grants installation, permission, deployment, cleanup,
private-key, or credential authority. The current accepted boundary is
`2026-08-12: Separate Cancellation Trust Maintenance From Runtime Authority`.
The three-principal installer/publisher, operator, and runtime permission contract and
its platform-neutral pure Java plan/adapter port are specified separately in
[`cancellation-trust-operator-installation-permissions.md`](cancellation-trust-operator-installation-permissions.md).

## Security Boundary

The runtime reader remains read-only. `EnhancerCli`, every `scheduler-*` command,
requests, proofs, audit records, AgentRuntime, event publication, environment/JVM
properties, working directory, repository content, and ambient identity cannot invoke
or configure maintenance.

A maintenance launcher deployment must run only under a separately authorized operator
principal. The repository supplies a distinct Java main, Gradle selector, and separate
custom distribution with one Gradle-generated launcher pair. Repository verification
installs that distribution only below build output, copies it below JUnit `@TempDir`,
and selects only temporary fake application installations. It does not install into an
operating system or real application installation. A future deployment must make the
runtime principal unable to write, rename, or delete the application JAR, fixed
metadata, trust-policy directory, maintenance lock, or installation ancestors. This
specification does not infer or implement those permissions and does not treat portable
Java owner/ACL inspection as approval.

Because fixed metadata is beside the application JAR, the specified future deployment
does not grant the operator direct directory mutation. A distinct privileged installer/
publisher validates the operator request against publisher-owned configuration and owns
final permission application and publication; the runtime remains read-only.

The surface accepts an exact installed application path, one candidate public policy
path, an operation (`INSTALL` or `ROTATE`), and for ROTATE the expected SHA-256 of the
complete currently installed canonical metadata bytes. It accepts no new policy pin,
private field, secret, proof, signature, issuer override, key override, fallback path,
runtime root, Goal, Control message, audit root, event root, or request authority.

The concrete operator argument forms are exactly:

- `install --application-jar <absolute-normalized> --candidate-policy <absolute-normalized>`
- `rotate --application-jar <absolute-normalized> --candidate-policy <absolute-normalized> --expected-current-metadata-sha256 <lowercase-sha256>`

It rejects alternate order/case, unknown/duplicate/extra fields, relative or normalized-
different paths, control characters, and overlong paths before mutation. Direct JVM
success exits `0`; configuration exits `2`, safe refusal exits `20`, and durability or
unexpected failure exits `70`. The Gradle task selects the main but does not promise
unchanged child-code propagation through Gradle itself.

## Separate Distribution

The custom Gradle distribution and base name are exactly
`enhancer-cancellation-trust-maintenance`. Its `bin/` contains only the Unix and Windows
launchers with that name, both generated from Gradle `CreateStartScripts` for
`CancellationTrustMaintenanceOperator`. Its `lib/` contains the project JAR and the
same runtime dependency collection used to generate the script classpath. Duplicate
archive paths fail assembly.

The generated scripts define no default application arguments or JVM options and derive
no operation, application/candidate path, metadata digest, trust input, permission, or
authority. They forward explicit caller arguments to the existing typed main. Standard
launcher JVM-option environment variables remain ordinary JVM configuration rather
than maintenance authority; tests remove them and fix `JAVA_HOME` to the test JVM.
Installed-layout subprocess tests prove exit `0`, `2`, `20`, and `70`, INSTALL, and
exact ROTATE replay after copying the entire build distribution below JUnit `@TempDir`.
ZIP and TAR assembly make no signed, published, deployed, Operational, or Released
claim.

## Artifacts And Identities

- Fixed runtime-selected metadata:
  `enhancer-cancellation-trust-metadata-v1`, using the existing canonical v1 format.
- Immutable public policy: a content-addressed exact file in the operator-owned trust
  directory. Its stable identity is the lowercase SHA-256 of its complete canonical
  bytes; the concrete filename must include that digest and a fixed suffix.
- Maintenance lock: `enhancer-cancellation-trust-maintenance-v1.lock` beside the exact
  application JAR. Its file may preexist and is retained; existence and bytes carry no
  state or authority. Its operating-system lock is nonblocking and held across current-
  state validation through the metadata switch and post-switch exact verification.
- Trust directory: the preexisting exact-real non-symbolic
  `enhancer-cancellation-trust-policies-v1` sibling of the application JAR.
- Content-addressed policy:
  `enhancer-cancellation-trust-policy-<lowercase-sha256>.conf` in that directory.
- Candidates: unique same-directory non-authoritative files. They are never discovered
  by runtime, never selected as fallback, and may survive failure until separately
  authorized cleanup.

The digest of the complete canonical metadata bytes is the compare-and-swap identity
for a current binding. It is not an application-version or privileged anti-rollback
counter.

## Candidate Validation

1. Resolve the application, maintenance directory, candidate policy, and their parents
   as absolute normalized exact-real non-symbolic paths. Reject aliases, links/Junctions,
   nonregular files, absent parents, and containment violations.
2. Open the candidate policy once with no-follow and enforce the existing 4 MiB bound.
   Hash and validate the same immutable byte snapshot.
3. Require the strict canonical public-only Ed25519 policy v1 contract. Reject unknown,
   private, secret, credential, signature, arbitrary-algorithm, alternate-encoding,
   duplicate, malformed, noncanonical, oversized, or trailing content.
4. Derive the lowercase SHA-256 pin internally. The operator cannot provide or override
   it. Form the content-addressed final policy path from that digest.
5. Construct canonical metadata v1 from only that exact final path and digest, then
   re-parse both policy and metadata with the production readers before publication.

## State Machine

### INSTALL

1. Open or create the stateless fixed lock artifact and acquire its operating-system
   lock without waiting. Refuse active contention or an existing fixed metadata binding;
   installation never becomes rotation by convenience.
2. Validate and derive the candidate policy and canonical metadata as above.
3. Publish or exact-resolve the content-addressed policy through a same-directory
   candidate and required atomic move. A digest filename containing different bytes is
   corruption and fails closed. The fixed cooperative lock, immediate target check, and
   exact re-resolution before metadata commit provide no-replacement semantics within
   this maintenance protocol; portable Java atomic-move target-exists behavior is not a
   defense against a privileged actor that ignores the protocol.
4. Write, force, re-read, and production-validate a unique metadata candidate beside
   the fixed metadata path.
5. Recheck that fixed metadata remains absent.
6. Atomically publish the metadata candidate to the fixed path without replacement,
   production-load the exact binding, and report `INSTALLED`.

### ROTATE

1. Acquire the fixed installation lock without waiting. Refuse contention.
2. Production-load the existing fixed metadata and pinned policy. Refuse missing,
   corrupt, or unpinned state.
3. Validate and derive the new policy and metadata. If the complete new metadata bytes
   equal the current bytes, production-load them and report `EXACT_REPLAY` without a
   write or timestamp change, including recovery after an exact atomic switch whose
   success report was lost. Otherwise compare the complete current metadata digest in
   constant time with required expected-current and refuse stale state.
4. Publish or exact-resolve the new content-addressed policy as in INSTALL.
5. Write, force, re-read, and production-validate a unique same-directory metadata
   candidate.
6. Re-read the current metadata and compare its complete digest with expected-current
   immediately before commit. Refuse drift.
7. Atomically replace only the fixed metadata path, production-load the new binding,
   and report `ROTATED`. Never rewrite the previous policy.

## Failure And Recovery Matrix

| Failure boundary | Authoritative binding | Safe retry |
| --- | --- | --- |
| Before lock | Previous exact binding or absent INSTALL state | Retry after contention/authority is resolved |
| Candidate validation | Previous exact binding or absent INSTALL state | Correct candidate; no runtime artifact changed |
| Policy candidate write/force/validation | Previous exact binding or absent INSTALL state | Exact policy publication may be retried |
| Policy atomic publication | Previous binding; new unreferenced exact policy may exist | Exact-resolve the digest file and continue |
| Metadata candidate write/force/validation | Previous binding; new policy may be unreferenced | Recreate a unique candidate and continue |
| Final CAS recheck | Previous binding | Restart with freshly observed expected-current digest |
| Metadata atomic switch | Atomic old or exact-new binding only | Re-read fixed metadata; never infer from candidate |
| Post-switch validation/reporting | Exact-new binding if switch occurred | Same request returns `EXACT_REPLAY` |

No failure triggers fallback, old-metadata restoration, policy overwrite, or deletion.
If post-switch production loading fails, maintenance reports a hard failure and leaves
the fixed bytes untouched for separately authorized investigation; it does not guess a
rollback.

## Concurrency And Idempotency

The operating-system lock serializes cooperative maintenance processes. The expected-
current metadata digest provides compare-and-swap against stale operator intent and is
checked after lock acquisition and immediately before the metadata switch. Neither
mechanism protects against a privileged actor that ignores the protocol.

INSTALL of an existing binding is always refusal. ROTATE to exact current bytes is
`EXACT_REPLAY`, including a retry whose expected-current digest identifies the
immediately preceding exact binding after the switch response was lost. When requested
bytes differ from current, a stale expected-current digest is always refusal even if the
candidate is otherwise valid. No stale request may mutate authority, and no operation
advances a self-declared counter.

## Rotation Semantics

Any future launcher or preview UI may report exact public differences in configuration
identity, audience, policy revision, key IDs/fingerprints, subject sets, validity, and
revocation facts; this state-machine library does not provide a diff UI or decide that a
change is safe. A usual non-disruptive rollover is two separately approved
rotations: first publish old+new keys, then publish removal or revocation of the old key
after outstanding grants and clock skew have elapsed. Emergency revocation may
intentionally deny unapplied grants; durable terminal cancellations remain historical
truth and replay without reauthorization.

## Rollback And Retention Limits

The current replaceable installation trust boundary cannot detect a privileged rollback
of the application JAR together with old metadata and policy artifacts. Metadata
generations, timestamps, locks, and local history are replaceable by the same privileged
actor and are not anti-rollback anchors. A future design needs an independently protected
monotonic source such as signed release/installer state, an OS package manager, keystore,
TPM, or equivalent.

Old public policies and abandoned candidates are retained. Cleanup, retention bounds,
rollback tooling, installer integration, ACL mutation, backups, and parent-directory
power-loss durability require separate decisions, authority, and verification.

## Future Implementation Verification

A future implementation is not complete without focused failure injection at every
matrix row, real-filesystem atomic-move checks, lock contention and stale-CAS tests,
exact replay byte/timestamp checks, symbolic/Junction/private-field rejection, runtime
surface separation checks, old/new-only loadability checks, preserved old artifacts,
and full Java/architecture regression. Tests use isolated temporary installation trees;
they do not grant authority to mutate a real installation or its permissions.
