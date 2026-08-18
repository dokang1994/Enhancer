# Cancellation Trust Operator Installation And Permission Contract

Status: Platform-Neutral Contracts Verified; No Installer Or Platform Enforcement

This contract governs a future real installation of the separately packaged
cancellation-trust operator. It grants no permission to install, mutate a real path,
change an operating-system permission, deploy, clean, uninstall, or release. The
accepted boundary is `2026-08-13: Separate Operator Intent From Privileged Installation
Publication`.

## Principals And Authority

Three stable operating-system identities are mandatory:

- **Installer/publisher (`I`)**: the privileged, non-runtime identity that owns
  installation topology, applies and verifies permissions, publishes protected final
  artifacts, activates immutable versions, and records installation evidence.
- **Operator (`O`)**: an authenticated human or service identity allowed to request
  INSTALL/ROTATE, execute/read the separate operator distribution, and create a public
  candidate only in an operator-private inbox. It cannot mutate protected final paths.
- **Runtime (`R`)**: the identity running `EnhancerCli`. It reads/executes the application
  and reads fixed metadata and pinned public policy. It cannot access the operator
  launcher or create, write, replace, rename, or delete protected installation content.

Stable SID or numeric UID/GID identity is required. Usernames, group names, current-user
queries, owner strings, environment variables, path possession, launcher possession,
and administrator/root labels are neither authorization nor evidence of effective
access. The explicit `--application-jar` remains mandatory request data and must exactly
match an independently installed publisher-owned allowlist; no ambient or default
destination exists.

The existing maintenance state machine performs same-directory publication and must run
as `I` or behind an `I`-owned authenticated narrow broker. `O` cannot directly run it
with protected-directory mutation rights. On POSIX, granting `O` directory write needed
to replace metadata would also permit replacement/unlink of the sibling JAR; file mode
or sticky-directory claims do not fix that portable boundary.

## Artifact Capability Matrix

`I` operations are still limited to an approved transaction. `O` and `R` denial includes
ownership, ACL, mode, and delete-child changes.

| Artifact | Installer/publisher `I` | Operator `O` | Runtime `R` |
| --- | --- | --- | --- |
| Protected installation ancestors | Traverse/read and approved create/publish/rename/delete-child | Traverse only where required | Traverse/read only |
| Immutable versioned application JAR and runtime launcher/libs | Stage, set permissions, publish, verify, activate; delete only under future uninstall | No write; read only if request validation needs it | Read/execute only |
| Separate operator distribution root, launcher, and libs | Stage, set permissions, publish, verify | Read/execute only | No access |
| Fixed metadata sibling | Create/publish/replace atomically; never in-place write; delete only under future uninstall | Read only | Read only |
| Trust-policy directory | Create/configure/traverse; only `I` adds final entries | Read/traverse | Read/traverse |
| Content-addressed final policy | Exclusive create or exact-resolve; never in-place write/replace/routine delete | Read only | Read only |
| Persistent stateless maintenance lock | Create/open/write/OS-lock; retain | No access | No access |
| Same-directory policy/metadata candidates | Create/write/read/rename; retain on failure | No access | No authority and never selected |
| Operator-private candidate inbox | Bounded no-follow read after authorization | Create/write/read within confined inbox | No access |
| Activation point/service configuration | Publish/switch last and verify | Read/observe only | Read/execute resolved active version |
| Installation audit root | Persist/append and integrity-verify | Bounded receipt only if separately exposed | No access |

Old policies, candidates, versions, the lock, and audit records are not deleted by
install, retry, rotate, or upgrade. Uninstall and cleanup require a separate exact
deletion contract and authority.

## Platform-Neutral Permission Adapter

The pure `com.enhancer.maintenance.installation` package provides the validated
`CancellationTrustInstallationPlan`, fixed revisioned matrix and phase order, bounded
immutable evidence, finite failures, and installer-only `InstallationPermissionAdapter`.
The port is enforcement, not an authorizer. It receives an already authorized typed plan
and requires an eventual platform implementation to:

1. Resolve `I`, `O`, and `R` to stable OS identities.
2. Resolve every path component without symbolic links, junctions, or reparse points and
   retain stable filesystem identities.
3. Verify staging and final publication use the required same filesystem/volume.
4. Apply a versioned permission profile to private staged artifacts before exposure.
5. Verify allowed and denied effective operations for all three principals, including
   parent-directory rename, replace, delete-child, ownership, and permission changes.
6. Publish through a required atomic primitive and perform supported file plus parent-
   directory/volume durability barriers.
7. Re-resolve and verify bytes, file identity, owner, permissions, inheritance, and
   effective access after publication.
8. Fail closed if any identity, access check, atomic operation, durability primitive, or
   negative-access proof required by the supported platform is unavailable.

The repository now has one Windows contract adapter backed only by an injected
`WindowsInstallationPermissionGateway`. No production/default/native gateway exists,
and neither the neutral contracts nor Windows adapter contain filesystem, ACL, process,
shell, or native-library calls. Raw Windows rights are retained separately from typed
authorized installation operations: the publisher's minimal target `DELETE` or parent
add/delete-child closure required for rename/replace does not authorize typed `DELETE`,
cleanup, or uninstall. Operator/runtime raw mutation and delete remain denied. A
successful gateway publication binds its exact resulting target file identity to the
transaction and artifact. Exact replay may return only that identity, and the following
durability plus published-security checks reject the pre-publication leaf or any
different same-volume identity.

The contract has no filesystem calls and no
permissive fallback to Java `Files.getOwner`, username comparison, inherited defaults,
path ownership, ambient privilege, or a broad administrator/root role.

## Windows Evidence

A supported Windows adapter must retain bounded normalized evidence for:

- canonical owner and principal SIDs, token group membership, and protected DACL plus
  inheritance state for each file and parent directory;
- effective read/execute, write/append, add-file/add-directory, rename/replace,
  delete/delete-child, `WRITE_DAC`, and `WRITE_OWNER` outcomes for `I`, `O`, and `R`;
- absence of reparse points at every component and stable volume/file identity from
  opened handles;
- absence from `O`/`R` of effective administrator or take-ownership/restore-style bypass
  privileges relied upon by the model;
- same-volume atomic publication, post-move DACL/inheritance, and repeated effective-
  access checks.

SDDL, service SID, integrity-label, AppLocker, or WDAC text is descriptive evidence only.
If the contract relies on one of those controls, its loaded/enforcing state and actual
effective decision are also required.

## POSIX Evidence

A supported POSIX adapter must retain bounded normalized evidence for:

- numeric UID, primary and supplementary GIDs for `I`, `O`, and `R`;
- `lstat`/opened-handle device and inode, owner UID/GID, mode, POSIX ACL, and default ACL;
- absence of symlinks at every component, same-filesystem publication, and no alternate
  hard-link/path substitution accepted as the planned artifact;
- actual parent-directory read/write/execute, rename, replace, and unlink implications,
  not file-mode checks alone;
- final owner/group/mode/ACL applied to candidates before rename and preserved after it;
- absence from `O`/`R` of root, `CAP_DAC_OVERRIDE`, `CAP_FOWNER`, or supplementary-group
  mutation rights that contradict the plan;
- effective read probe as `R` and denied protected mutation operations for both `O` and
  `R`, using confined pre-provisioned identities.

SELinux/AppArmor may add defense in depth. If required for the boundary, their loaded
and enforcing policy plus actual access decisions become mandatory evidence.

## Installation And Upgrade Order

1. Resolve stable principals and the exact publisher-owned approved destination.
2. Verify the source distribution against a separately trusted signed manifest and
   expected digests. Current unsigned local archives do not prove publisher authenticity.
3. Validate exact-real topology, link/reparse absence, filesystem identity, same-volume
   staging, atomic primitives, durability support, and current installed state.
4. Persist a bounded integrity-protected transaction intent containing transaction ID,
   source/manifest digest, exact destination, stable principals, permission-policy
   digest, requested binding, and expected current/activation identity.
5. Stage runtime and operator distributions in `I`-private same-filesystem immutable
   version directories.
6. Apply and verify all final permissions and positive/negative effective rights while
   artifacts remain private.
7. Create/configure the final trust directory and stateless lock under the same policy.
8. Snapshot and canonical-validate the untrusted public policy candidate once, deriving
   its digest internally.
9. Publish or exact-resolve the content-addressed policy first with final permissions;
   reopen and verify its bytes, identity, and rights.
10. Construct metadata only from that final path/digest, apply final permissions to a
    same-directory candidate, validate through the production loader, and atomically
    publish fixed metadata last.
11. Reopen and verify JAR, metadata, policy, and all effective permission facts.
12. Run a dedicated non-mutating trust-loader probe as the actual `R` identity.
    `scheduler-apply-cancel` is not a probe because it may create audit/runtime/event
    state.
13. Atomically activate the fully verified immutable version last.
14. Re-resolve active state, persist final evidence, and report success only after exact
    transaction equality. Presence of files, candidates, a lock, or audit data alone is
    never success.

Permission failure before publication leaves only private staged state. Failure after a
final publication stops before activation and never broadens permissions to repair the
failure. A running JAR or operator distribution is never upgraded in place.

## Retry And Recovery

The persistent transaction identity binds source/release manifest digest and version,
destination/filesystem identity, stable principals, permission-policy version/digest,
runtime and operator distribution digests, old/requested metadata digests, target policy
digest, current phase, and activation identity.

- Before final metadata, the previous binding or absent INSTALL state remains
  authoritative; exact staged and policy prefixes may be revalidated and reused.
- After metadata but before activation, the new version remains inactive; retry must
  revalidate it and repeat the `R` probe.
- After activation but before response, exact replay requires equality of active target,
  complete transaction, bytes, principals, permissions, and evidence.
- Any conflicting file, digest, permission, principal, filesystem, transaction, or
  activation identity is refusal. Names and partial prefixes never imply completion.
- Installer replay is classified before invoking maintenance. Existing-binding refusal
  is not rewritten as success. ROTATE retains exact-new replay and expected-current CAS.
- No automatic rollback follows trust publication or activation. Switching to an older
  version or trust binding requires separate approval because it may restore vulnerable
  code or revoked trust.

Current Java atomic move plus file `force(true)` is process/crash recovery evidence, not
parent-directory or sudden-power-loss durability. A platform adapter needs directory/
volume barriers and crash testing before making a stronger claim.

### Current Pure Transaction Contract

`InstallationTransactionState` now represents the schema-v2 immutable recovery cursor
without performing an installation. It binds the complete already-authorized plan,
normalized environment/filesystem identity, bounded source release version, permission-
policy digest, expected-current/requested activation identities, one existing required
phase, its `PENDING`/`SUCCEEDED` status, an exact revision, and an immutable ordered
prefix of succeeded phase-evidence identities. Each `InstallationPhaseEvidence` binds
schema, transaction, exact phase, its pending revision, lowercase semantic SHA-256, and
only for activation the exact observed requested identity. Construction starts at the
first required phase with an empty prefix. Pending-to-succeeded appends exactly one
matching value; succeeded-to-next-pending preserves the complete prefix. Changed
identity, historical replacement, reorder, truncate, skip, regression, extra append, or
post-final advancement is structurally invalid.

The pure `InstallationTransactionStore` port exposes only create-exclusive, exact point
resolution, and compare-and-exchange. Its contract makes exact replay mutation-free and
separates transaction conflict, stale revision, invalid transition, absent, unsupported,
corrupt, capacity, unavailable, and reconciliation-required refusal. A test-only in-
memory fake verifies those semantics and the before-metadata, after-metadata-before-
activation, and after-activation recovery classifications.

This contract is not a persistence implementation or evidence of durability, process
restart, successful installation, or safe effect recovery. It does not serialize or
integrity-protect records, retain evidence bodies or resolvable references, invoke an
adapter, or automatically replay a pending effect. Those capabilities remain
prerequisites for any real installer.

### Current Pure Coordinator Contract

`InstallationTransactionStore` mutations now return `CREATED`, `ADVANCED`, or
`EXACT_REPLAY`. Only the first two receipts grant the current
`InstallationTransactionCoordinator` call ownership to invoke the newly stored pending
phase. Store state equality by itself never grants invocation authority.

The coordinator performs at most one phase per explicit `start` or `advance`. The first
two existing phases revalidate the supplied plan/environment/source binding through a
source/preflight port; they do not discover an ambient principal or source. `ACTIVATE`
uses only a distinct activation port. The remaining eight phases use named methods on a
phase-effect port. Every fake result binds the transaction, exact phase, lowercase
semantic evidence digest, and requested activation identity where applicable before the
pending cursor is compare-and-exchanged to succeeded. That same standalone result value
is appended to the state prefix, and the succeeded replacement must be accepted by the
store before an outcome is returned.

Pre-existing or exact-replayed pending state returns reconciliation and invokes no port.
Exact terminal replay invokes no port and mutates no store state. A port refusal,
unexpected port failure, invalid result binding, or store failure stops immediately and
never falls back, rolls back, cleans up, broadens permission, infers success, or
automatically retries pending work.

Exact terminal replay retains all eleven ordered semantic result identities. Those
digests identify values returned by the fake ports; they do not retain or independently
verify evidence content and do not prove storage integrity, durability, restart-safe
reconciliation, exactly-once effects, real source verification, installation success,
or platform enforcement.

### Current Pure Evidence Reconciliation Contract

`InstallationPhaseEvidencePoint` deterministically binds one transaction identity,
required phase, and that phase's canonical pending revision. The read-only
`InstallationPhaseEvidenceResolver` accepts exactly one such point and returns either
one revalidated `InstallationPhaseEvidence` value or explicit absence. Its finite typed
failures distinguish unsupported, corrupt, foreign, and unavailable evidence; it has no
scan, list, discovery, create, or mutation operation.

The separate `InstallationTransactionReconciler` resolves exactly one transaction. A
succeeded or terminal cursor returns unchanged without resolving evidence or advancing
to another phase. For a pending cursor, absence returns unchanged; a present result must
pass the existing exact transaction/phase/revision/activation successor validation
before one compare-and-exchange can record success. Exact succeeded-write replay is a
separate outcome. Resolver failure, mismatched evidence, store failure, or malformed
receipt fails closed and never invokes a preflight, phase-effect, activation, permission,
filesystem, or native port.

Only test-local in-memory fakes implement these ports. The point is a deterministic
lookup identity, not an evidence reference proving that content exists. No evidence
body, serializer, integrity envelope, production store, host revalidation, automatic
recovery loop, exactly-once effect, or installation-success evidence exists yet.

### Current Integrity Format And Cursor Store Contract

The transaction cursor and semantic phase-evidence value have separate deterministic
binary formats. Each schema-v1 envelope contains a distinct
domain magic, envelope schema, bounded body length, SHA-256 over the domain/schema/length
header plus body, and the exact body. Each body starts with its fixed payload kind and
current domain schema, uses fixed field order, length-framed strict UTF-8, enum names
rather than ordinals, explicit canonical booleans/optionals, and bounded collections.

`InstallationTransactionFileFormat` includes the complete plan, principals, normalized
environment, local filesystem provider/dialect marker, release/permission/activation
bindings, exact succeeded-evidence prefix, revision, phase, and status. Decode uses the
existing constructors and accepts only content that re-encodes byte-for-byte identically.
`InstallationPhaseEvidenceFileFormat` also requires the exact expected
`InstallationPhaseEvidencePoint`; a valid envelope from another transaction, phase, or
pending revision is foreign. `InstallationRecordFileNames` derives only one bounded
canonical leaf name from the transaction or complete point and accepts no root.

The codecs and filename derivation themselves use no filesystem API. The transaction
format and leaf now feed one uncomposed `FileSystemInstallationTransactionStore` over a
caller-provisioned pre-existing absolute exact-real non-symbolic directory. Exact reads
open only the derived bounded regular non-symbolic point with `NOFOLLOW_LINKS`. Create
and compare-and-exchange share one stable per-transaction nonblocking OS lock and hold it
through current decode, exact replay or successor validation, same-root candidate write
and `force(true)`, candidate decode, required atomic move, and post-publication decode.
Exact replay performs no rewrite. The adapter creates no root and performs no scan/list.

`InstallationPhaseEvidencePointStore` is a separate pure port exposing only immutable
semantic receipt creation and exact point read. First create and unchanged replay are
distinct, changed reuse conflicts, and absence is explicit. It has no production
implementation and is not the evidence resolver: the current record cannot independently
revalidate a host observation because it contains no evidence body.

The envelope digest detects accidental corruption but supplies no publisher
authentication: a principal able to rewrite bytes can recompute it. It also cannot
prevent replacement with an older valid cursor. Evidence still contains only a semantic
digest and optional activation identity, not a body that a production resolver could
independently revalidate. The cursor root is a caller-supplied protection assumption;
Java path checks and local locks do not prove descriptor-relative confinement or defend
against a privileged hostile writer. File force plus atomic rename does not prove parent-
directory or sudden-power-loss durability. Permission composition, production evidence
persistence/body/revalidation, capacity policy, retention, and anti-rollback remain
separate work.

## Audit Evidence And Deferred Destruction

Each attempt retains bounded integrity-protected evidence: transaction/phase, verified
`O` and executing `I` identities, exact authorized destination/filesystem identity,
source manifest/signature/digests, permission-policy digest, pre/post artifact identities
and hashes, metadata CAS and policy digests, permission/effective-right results, atomic
publication and durability results, runtime probe, activation transition, outcome, and
timestamps. It stores no candidate content, proof, private key, credential, bearer token,
or ambient environment. Audit is evidence, never approval authority.

Uninstall and cleanup remain deferred. A future contract must define service stop,
active-version proof, trust/audit retention, ownership validation, exact deletion set,
partial-removal recovery, and rollback authorization before any deletion. No general
scan or automatic retention deletion is implied.

## Anti-Rollback And Isolation Limits

The operator and runtime distributions currently contain the same project JAR; separate
launchers and filesystem access are authority separation, not bytecode isolation. A
stronger code-isolation claim needs separate modules/JARs and a new decision.

No local metadata generation, timestamp, lock, transaction log, CAS, content-addressed
filename, permission probe, or audit record prevents a privileged actor from restoring
an older JAR with matching old metadata/policy. Privileged anti-rollback requires an
independently protected monotonic signed package/release anchor enforced by an OS package
manager, hardware/keystore/TPM state, or equivalent.

## Future Verification

The platform-neutral layer has fake-adapter contract tests for the fixed matrix, exact
derived plan/order, bounded evidence, deterministic replay, and fail-stop operation
errors. Future platform implementation must add non-skipped isolated Windows and POSIX
identity fixtures. It must cover every capability-matrix allow/deny,
SID/UID reuse, supplementary groups, inheritance/default ACL drift, reparse/symlink/
junction/hard-link substitution, cross-volume refusal, every permission/publication/
durability/probe/activation fault, process-kill recovery, exact replay with no byte/time/
permission rewrite, runtime read and denied mutation, partial old/new layouts, and
automatic-downgrade refusal. A skipped platform test cannot support that platform.
