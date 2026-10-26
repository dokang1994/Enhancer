# RFC-0032: Governed Remote Capability Catalog Provisioning And Rotation

Status: Accepted

## Purpose

Define how a future privileged publisher may turn one independently approved canonical
capability catalog into the protected absolute path and whole-file SHA-256 pin consumed
by RFC-0031, and how that exact binding may later be rotated without granting the
request, runtime, catalog, or remote provider authority to approve itself.

This RFC is a contract only. It does not implement a publisher, install or rotate a
catalog, create an installation layout, mutate permissions, connect an RFC-0031 source,
or make a remote route reachable. A matching digest establishes the exact approved
bytes; it does not establish that their remote-world claims are true, provider-attested,
currently available, or fresh beyond their declared exclusive expiry.

## Relationship To Existing Contracts

RFC-0030 remains the sole remote-candidate suitability evaluator. RFC-0031 remains the
sole production creator of its capability facts and continues to accept only one
protected absolute normalized catalog path and one independently installed lowercase
whole-file SHA-256 pin. RFC-0032 neither changes the catalog grammar nor moves catalog
reading into RFC-0030, RFC-0029, a provider, or the request path.

The permitted authority flow is:

```text
independent evidence author approves every catalog claim and the exact candidate bytes
-> authorized operator requests INSTALL or ROTATE for one allowlisted installation
-> privileged publisher validates one exact candidate snapshot and derives its digest
-> privileged publisher publishes immutable catalog first and fixed binding last
-> protected runtime composition reads the fixed binding
-> RFC-0031 rereads and verifies that exact catalog at evaluationTime
```

No stage grants provider, credential, network, transmission, spend, execution,
persistence, retry, fallback, Router-caller, or MCP authority. The later same-invocation
RFC-0031-to-RFC-0030-to-RFC-0029 consumer remains a separate contract.

## Separated Authorities

Four authorities must remain distinct even if a future deployment hosts more than one
role in a privileged process:

1. The **evidence author** owns the factual basis for every record: route, model and
   capability bindings; observation and exclusive expiry; token semantics, price
   schedule and conservative cost; supported reasoning, context, token and timeout
   maxima; classification approval; and the complete approved candidate bytes.
2. The **operator-intent authority** may select an approved evidence identity, one
   candidate input, the operation, one independently allowlisted installation identity,
   and for ROTATE the expected current binding digest. It cannot select the final
   catalog path, supply the resulting catalog pin, weaken validation, or write protected
   final paths.
3. The **installer/publisher authority** alone may validate the approved candidate,
   derive its SHA-256 from the same bounded snapshot, create protected candidates, and
   atomically publish final artifacts inside the allowlisted installation. It cannot
   invent evidence, expand installation scope, query a provider for truth, or authorize
   remote execution.
4. The **runtime-reader authority** may read the protected fixed binding and immutable
   catalog. It cannot write, rename, delete, repair, rotate, discover alternatives, or
   choose another trust root.

The operator and runtime principals are deployment identities, not strings accepted
from a request. A launcher name, username, file owner label, Java ACL view, or process
boundary alone proves none of these authorities. A future authenticated operator-to-
publisher transport and platform permission adapter require separate accepted contracts.

## Trusted Composition And Intent

Protected deployment composition, not an operator field, request, environment variable,
JVM property, working directory, repository path, filename convention, or catalog
content, supplies one stable installation identity and its allowlisted exact installation
root. The publisher must reject a requested path that does not resolve to that existing
protected identity; an explicit path is comparison data, never authority.

A future typed publication intent contains exactly:

- operation `INSTALL` or `ROTATE`;
- stable allowlisted installation identity;
- exact candidate input identity;
- independently approved evidence identity; and
- for ROTATE only, the lowercase SHA-256 of the complete expected current canonical
  binding bytes.

It contains no resulting catalog path, resulting pin, provider endpoint, credential,
secret, fallback, cleanup instruction, or remote-execution permission. The authenticated
transport, durable representation, signature, and producer of this intent are out of
scope; until they exist, no production publisher may be composed.

## Independent Evidence And Truthfulness

Before publication, a separately governed evidence approval must bind its stable
identity and installation scope to the candidate's exact canonical whole-file digest,
catalog identity, complete record set, and the independent basis for every RFC-0031
capability value. The publisher verifies that approval against the internally derived
digest and complete parsed catalog. It refuses absent, unauthorized, expired,
scope-mismatched, or byte-mismatched approval.

The publisher's parser, digest, permission checks, and successful publication prove
only structural validity, exact bytes, and installation state. They cannot prove the
factual quality of the evidence. Copying values from request content, RFC-0028
eligibility, provider labels, live availability, a prior result, a filename, filesystem
time, environment, or defaults is not evidence. If the separate evidence authority
cannot truthfully approve every record, no production catalog may be installed.

Evidence approval is not a credential or provider signature. Its concrete schema,
storage, authenticated transport, signing, revocation, and audit persistence remain
separate unless a later accepted task explicitly defines them. This RFC requires the
semantic binding without pretending that a document-only contract supplies it.

## Candidate Snapshot Boundary

The publisher reads one exact-real regular non-symbolic candidate through the same
1-byte-through-4-MiB, no-follow, single-snapshot, size-drift, strict UTF-8, complete-
catalog validation and canonical re-encoding rules required by RFC-0031. It validates
all records before any publication and computes SHA-256 over that same byte array.

It never accepts a caller-supplied resulting pin, rewrites a noncanonical candidate,
validates only a selected row, reopens the input to hash different bytes, or accepts a
catalog-declared revision. The internally computed lowercase digest becomes both the
immutable artifact identity and the pin written into the protected binding. From the
runtime reader's perspective that pin is independent because it comes only from the
protected publisher binding, never from request or catalog content.

Candidates must not contain credentials, tokens, private keys, endpoint secrets, or
other authority-bearing material. Provisioning never generates, retrieves, copies,
persists, logs, or exposes such material.

## Threat Boundary

The contract must fail closed against untrusted request, runtime, provider, catalog, and
repository inputs; symbolic links, junctions and reparse indirection; non-regular files;
candidate drift; path substitution; cross-volume publication; stale concurrent intent;
partial writes; and lost success responses. Exact-real identity and no-link checks are
repeated at security-sensitive boundaries rather than inferred from a prior pathname.

The runtime principal must lack write, rename, and delete authority over the protected
installation root, binding, catalog directory, immutable catalogs, maintenance lock,
and relevant ancestors. The operator may write only an operator-private candidate inbox.
The publisher alone may write protected final paths. These are required deployment
properties, not capabilities supplied or verified by this documentation-only RFC.

Windows support later requires effective SID/token-group, owner, protected DACL and
inheritance, bypass-privilege, reparse, volume and file-identity evidence. POSIX support
later requires effective numeric UID/GID and supplementary groups, mode/ACL/default-ACL,
capability, device/inode and parent-directory evidence. A platform is unsupported until
non-skipped isolated tests and real installation evidence establish its positive and
negative effective-access contract. Portable owner or ACL inspection is insufficient.

Atomic publication and a cooperative lock can bound crash consistency and lost updates;
they cannot prevent a privileged actor from replacing the application, binding and
catalog together. A generation, timestamp, filename, audit row, or self-declared catalog
revision is not anti-rollback. Privileged rollback protection requires a separately
protected monotonic anchor such as an OS package manager, signed release state,
keystore, TPM, or equivalent under separate authority.

## Safe Observability

Future results may expose only the operation, a closed outcome or refusal, bounded
non-secret installation/evidence/catalog identities, and approved digest identities
needed for compare-and-swap or exact replay. Rendering must not expose absolute paths,
raw catalog or binding bytes, capability values, raw capacities, prices, timestamps,
exceptions, platform security descriptors, request content, credentials, or secrets.

An audit record or success message is not publication authority. The fixed binding and
its exact target remain the only loadable selection point, and a future observer must
reread them rather than infer success from logs or candidate existence.

## Rejected Trust Shortcuts

- Treating digest equality as factual truth, provider attestation, availability, or
  anti-rollback is rejected.
- Allowing the operator, request, runtime, catalog, Router, evaluator, provider,
  environment, CLI, repository, or working directory to select both bytes and final pin
  is rejected as self-approval.
- Latest-file discovery, directory scans, fallback pins, grace periods, caches, repair,
  canonicalization of invalid input, and selected-row-only validation are rejected.
- In-place overwrite, binding-first publication, cross-volume move, and automatic
  rollback are rejected because they create partial or silently downgraded authority.
- Treating a separate launcher, username, ownership string, portable Java permission
  view, generation, timestamp, or lock-file existence as an authorization or security
  boundary is rejected.
- Automatic deletion of candidates, previous catalogs, locks, evidence, or audit data
  is rejected without a separate retention and cleanup contract.

## Fixed Installation Artifacts

Protected composition supplies one existing exact-real installation root. The following
identities are fixed directly below that root and cannot be renamed or selected by an
intent:

| Artifact | Fixed identity and role |
|---|---|
| binding | `enhancer-remote-capability-catalog-binding-v1`; the sole authoritative selection point |
| catalog directory | `enhancer-remote-capability-catalogs-v1`; pre-existing exact-real directory for immutable catalogs |
| maintenance lock | `enhancer-remote-capability-catalog-maintenance-v1.lock`; stateless installation-scoped cooperative lock |

An immutable catalog filename is
`enhancer-remote-capability-catalog-<lowercase-sha256>.conf` in the fixed catalog
directory. The digest is computed from the validated exact candidate snapshot. A
matching existing artifact may be reused only after its exact-real identity, complete
bytes, canonical form and digest are revalidated; the same name with different or
unsafe bytes is a conflict. Catalogs are never overwritten in place.

The lock is opened without following links and acquired once as a nonblocking exclusive
operating-system lock. Its bytes and mere existence carry no state, generation,
authority, operation or recovery meaning. The publisher retains it after release and
holds the acquired lock from the first current-binding observation through final
post-publication verification. Contention or an unsafe lock path refuses without wait,
repair or mutation of authority.

Unique non-authoritative catalog candidates are created exclusively inside the fixed
catalog directory. Unique binding candidates are created exclusively beside the fixed
binding. Candidates and targets must remain on the same filesystem/volume. A candidate
filename, temporary file, immutable catalog, audit entry or lock never becomes
authoritative until the fixed binding selects the exact installed catalog and pin.

## Canonical Binding Grammar

The fixed binding is one no-follow snapshot bounded from 1 byte through 4 KiB under its
exact-real parent. Its only canonical form is strict UTF-8 without BOM, LF only, exactly
three nonempty lines in this order, and one final LF:

```text
enhancer-installed-remote-capability-catalog-v1
catalogPath=<absolute normalized exact installed platform path>
catalogSha256=<64 lowercase hexadecimal characters>
```

The path is the exact absolute normalized path of the final immutable catalog after
publication; it must remain an immediate child of the fixed exact-real catalog directory
and its basename must contain the same digest as `catalogSha256`. Empty values, CR,
invalid UTF-8, controls, blank/comment/unknown/duplicate/trailing fields, alternate
spelling, a relative or non-normal path, path escape, a mismatched basename, or excess
bytes are invalid. Parsing must re-encode the values and require byte-for-byte equality.

The lowercase SHA-256 of the complete canonical binding bytes is its compare-and-swap
identity. It is not written inside the binding and is not an anti-rollback generation.
Protected runtime composition obtains only the path and pin from this fixed binding,
revalidates their exact installed relationship, and constructs RFC-0031 with them. A
missing or invalid binding makes the production source unavailable without discovery or
fallback.

## Closed Operations And Outcomes

Only `INSTALL` and `ROTATE` exist. A future successful result is exactly `INSTALLED`,
`ROTATED`, or `EXACT_REPLAY`. The operation never returns a partially trusted binding.

### INSTALL

INSTALL forbids an expected-current digest. Under the lock, the publisher requires the
fixed binding to be absent, validates the candidate and independent evidence, determines
the exact immutable target and canonical new binding, then rechecks binding absence
immediately before publication.

If a valid current binding already equals the fully validated requested new binding,
INSTALL returns `EXACT_REPLAY` without writing. This is the only existing-binding replay
case and recovers a lost success response. Any other existing binding refuses as an
already-installed authority; malformed or unsafe current state refuses without repair.

### ROTATE

ROTATE requires one valid current binding and an expected-current digest. The publisher
reads and validates the complete current binding and selected catalog before candidate
publication. If the fully validated requested new binding already equals the current
binding, it returns `EXACT_REPLAY` without writing; replay precedence permits recovery
when the expected digest names the immediately preceding binding whose successful
switch response was lost.

Otherwise the expected digest must equal in constant time both the initially observed
current canonical binding digest and a fresh digest read immediately before the switch.
A mismatch is stale intent and refuses without changing authority. Missing, malformed,
unsafe or drifted current state refuses rather than becoming INSTALL, repair, discovery,
fallback or rollback.

## Publication State Machine

For either operation, the future publisher performs the applicable steps in this fixed
order:

1. resolve the allowlisted installation, fixed directory, binding and lock identities;
2. acquire the fixed nonblocking exclusive lock without following indirection;
3. read and classify the current binding under the operation-specific rules;
4. take one bounded exact candidate snapshot, validate the complete RFC-0031 catalog,
   compute its digest, and validate the independent evidence approval for those exact
   bytes and installation scope;
5. derive and validate the immutable target path and canonical requested binding;
6. return `EXACT_REPLAY` if the current authority already equals the requested binding;
7. exclusively create, write, durability-barrier where supported, reread and validate a
   same-directory immutable-catalog candidate;
8. publish the immutable catalog first with a required same-filesystem atomic move, or
   exact-resolve and revalidate an already identical target;
9. exclusively create, write, durability-barrier where supported, reread and validate a
   same-directory binding candidate;
10. re-read current authority and enforce INSTALL absence or both ROTATE compare-and-
    swap checks immediately before the switch;
11. publish the fixed binding last with the required operation-specific atomic move;
12. re-read the fixed binding, exact installed catalog and digest through the future
    runtime-composition/RFC-0031-compatible contract before releasing the lock.

INSTALL requires atomic no-replacement publication of the binding. ROTATE requires an
atomic replacement of the fixed binding. If a platform cannot prove the required
same-filesystem move, target-existence behavior, exact path identity, no-link behavior,
and positive and negative effective access, it is unsupported. A cooperating-process
lock plus immediate rechecks closes lost updates but makes no claim against a privileged
or noncooperating path replacement actor.

The pre-switch binding is the sole authority before step 11; the exact new binding is
the sole authority after a successful atomic switch. Immutable catalog publication
alone grants nothing. The publisher never modifies the old binding or catalog in place
and never exposes a binding before its selected catalog is final and revalidated.

## Failure And Recovery Semantics

- Failure before immutable catalog publication leaves an absent or old binding as the
  complete authority.
- Failure after catalog publication but before the binding switch leaves the absent or
  old binding authoritative. The unselected catalog and candidates are retained.
- A conclusively failed binding move leaves prior authority unchanged. A move that may
  have succeeded, or a post-switch verification failure, returns a distinct
  post-publication-state-unknown refusal and performs no automatic rollback.
- After an atomic switch, the exact new binding is authoritative even if reporting,
  audit or later verification fails. A fresh reread and the same fully validated request
  recover only as `EXACT_REPLAY`.
- Automatic rollback, fallback to a previous catalog, permission broadening, in-place
  repair, retry with weaker checks, and inference from audit or candidate existence are
  forbidden.
- Previous immutable catalogs, abandoned candidates, the lock, evidence and audit are
  retained. Retention, inventory, deletion, cleanup, uninstall and secure disposal need
  a separate accepted contract that first proves no authoritative binding references an
  artifact.

Atomic move is not a portable sudden-power-loss durability guarantee. A future platform
adapter must define and verify supported file and parent-directory durability barriers;
otherwise it may claim process-crash atomicity only and must re-establish authority by a
fresh binding/catalog read after restart. This RFC defines no automatic recovery writer.

## Closed Refusal Contract

After programming and composition errors are rejected, a future result uses the first
applicable reason in this order:

1. `INSTALLATION_ROOT_UNAVAILABLE_OR_UNSAFE`
2. `MAINTENANCE_LOCK_UNAVAILABLE_OR_CONTENDED`
3. `CURRENT_BINDING_REQUIRED`
4. `CURRENT_BINDING_ALREADY_EXISTS`
5. `CURRENT_BINDING_INVALID_OR_UNSAFE`
6. `CANDIDATE_UNAVAILABLE_OR_UNSAFE`
7. `CANDIDATE_OUTSIDE_SIZE_BOUND`
8. `CANDIDATE_CHANGED_DURING_READ`
9. `CATALOG_INVALID`
10. `EVIDENCE_NOT_APPROVED_OR_MISMATCHED`
11. `EXPECTED_CURRENT_MISMATCH`
12. `IMMUTABLE_CATALOG_CONFLICT`
13. `ATOMIC_PUBLICATION_UNSUPPORTED_OR_FAILED`
14. `POST_PUBLICATION_STATE_UNKNOWN`

Operation-inapplicable current-state reasons are skipped. Exact replay is classified
only after current state, candidate and evidence are fully validated, and before stale
expected-current refusal. The future implementation must specify equivalent internal
phase detail without enlarging this public vocabulary or exposing unsafe diagnostics.

## Verification Contract

A later implementation starts RED-first with pure values and an unexposed publisher
port. Isolated tests must cover every programming input, fixed identity, canonical
binding byte, catalog boundary inherited from RFC-0031, evidence mismatch, refusal and
precedence combination; INSTALL, ROTATE, exact replay and stale intent; active lock
contention; same-name conflict; every failure before and after each publication phase;
candidate/current drift; symbolic, junction and reparse paths; cross-volume rejection;
atomic-move unavailability; post-switch uncertainty; no rollback, repair, discovery,
fallback or cleanup; and safe rendering.

Architecture tests must prove no request, runtime, Router, evaluator, provider,
environment, CLI or catalog can provide the installation root, final path or pin; the
runtime principal has no writer API; the publisher has no provider, credential, network,
spend, execution or consumer dependency; RFC-0031 remains the sole capability-facts
creator; and source/evaluator consumers remain absent unless separately authorized.

Temporary filesystem tests establish contract behavior only. Operational support for a
platform requires non-skipped effective-identity, permission, indirection, atomicity,
same-volume, durability and negative-access suites plus real installation evidence under
separate authority. Documentation verification cannot establish those claims.

For this documentation-only task, RFC/decision/index/document-ownership/Architecture-
mirror/Roadmap/Planner/Assisted-Loop/dynamic-workflow governance, reference inspection,
`git diff --check`, and the full README-owned Java 17 regression are the verification
boundary.

## Exclusions

- Java or test-source implementation, publisher/writer, installed catalog or binding,
  runtime binding reader/composition, operator CLI/launcher, broker, platform adapter,
  installer, real installation, permission mutation, deployment or cleanup;
- an evidence schema, evidence store, evidence-authoring implementation, signed catalog,
  signing or private keys, authenticated transport, audit persistence, revocation,
  retention, uninstall, rollback or privileged anti-rollback anchor;
- provider or adapter construction, endpoint, protocol, credential, secret, network,
  transmission, availability, quality, latency, tokenizer, live probing or provider
  attestation;
- RFC-0031 source caller, RFC-0030 evaluator caller, RFC-0029 route-option consumer,
  spend authorization, billing, execution, retry, fallback or durable route provenance;
- Tool/evidence/RunRecord/Scheduler/runtime-event integration, durable schema or
  migration, MCP, plugin, interface, background work, capability promotion, push, merge,
  tag, release or another external effect.

## Codex Implementation Prompt

Implement RFC-0032 only under a separately accepted task, RED-first, as an unexposed
provider-neutral publisher boundary with fixed protected artifacts, canonical binding,
publisher-derived digest, complete RFC-0031 validation, separately approved evidence,
immutable-catalog-first/fixed-binding-last atomic publication, lock/CAS/exact-replay and
closed failures. Add no real installer, permission mutation, operator surface, source or
evaluator consumer, provider, credential, network, spend, execution, runtime, MCP,
deployment or cleanup path.
