# RFC-0032: Governed Remote Capability Catalog Provisioning And Rotation

Status: Draft

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

## Remaining Draft Closure

Before acceptance, this RFC must define fixed artifact identities and canonical binding
grammar; INSTALL and ROTATE preconditions; lock, compare-and-swap and exact-replay
semantics; publication order; crash and post-publication behavior; closed refusals;
verification; and exclusions. No implementation work may begin while this RFC is Draft.
