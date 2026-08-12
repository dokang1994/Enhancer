# 2026-08-11: Pin Canonical Cancellation Trust Policy Bytes Outside The Request Invocation

Status: Accepted Decision

## Context

`CancellationGrantTrustPolicy` is immutable and public-only, and
`AuditBackedSignedCancellationAuthorizer` already revalidates it before every
pre-runtime approval. A production loader is still absent. Merely accepting a policy
path, key, issuer, or digest from `scheduler-apply-cancel` would let the same invocation
construct its own approval. Portable Java 17 owner/ACL inspection is also insufficient
as the sole cross-platform authority anchor.

## Decision

- Add a read-only `PinnedFileCancellationGrantTrustPolicyLoader` under
  `com.enhancer.runtime`. Construction requires one absolute normalized policy-file path
  and one independently provisioned lowercase SHA-256 pin over the complete exact file
  bytes. These are trusted composition inputs, not request inputs.
- The supported future interface may receive a preconstructed loader only from protected
  installed application metadata outside command parsing. CLI arguments, environment or
  JVM properties, working directory, project repository, retained request, proof, and
  ambient username cannot select or override the file, pin, clock, issuer, key, policy,
  validity, or revocation facts. This task implements the loader seam, not that future
  installed metadata binding or CLI.
- The loader opens one pre-existing exact-real regular non-symbolic file through a
  no-follow bounded read, hashes the bytes actually read, compares them to the injected
  pin, and parses that same byte array. It creates, repairs, rewrites, discovers, scans,
  caches, or reopens no trust source. A replacement can therefore produce only the exact
  pinned policy or denial.
- Use one strict canonical UTF-8 line format beginning with
  `enhancer-cancellation-grant-trust-policy-v1`. It contains configuration identity,
  audience, policy revision, maximum grant lifetime and clock skew in seconds, and one
  through sixty-four lexicographically ordered Ed25519 key entries. Each entry contains
  issuer/key identities, one through 256 ordered unique subjects, canonical Base64 X.509
  SubjectPublicKeyInfo bytes, lowercase SHA-256 fingerprint, validity interval, and an
  optional revocation instant. Field order is fixed, LF is required, CR/comments/blank or
  unknown lines/trailing bytes are rejected, and parse followed by internal canonical
  re-encode must reproduce the exact bytes.
- The verified raw-file SHA-256 becomes
  `CancellationGrantTrustPolicy.configurationRevision`; it is omitted from the file to
  avoid self-reference. `policyRevision` remains the signed-grant compatibility
  boundary. Base64/fingerprint/key type and all existing policy bounds are revalidated
  by `CancellationGrantTrustPolicy`.
- The pin is public but authority-bearing. If it is truly independent, writable policy
  storage can at worst cause denial; portable owner/mode/ACL checks are defense in depth,
  not approval authority. Deployment should still make the file and ancestors
  non-writable to the runtime principal. Provisioning, permissions, rotation, pin
  replacement, anti-rollback across application versions, and external installation
  metadata remain separate authorized operations.
- There is no production writer, private-key field, arbitrary algorithm field,
  permissive fallback, default policy, or cached evergreen snapshot. A future command
  loads one fresh immutable policy before constructing the authorizer; an audit-only
  retry therefore continues to observe current pinned rotation/revocation state, while
  an already durable cancellation remains historical truth.

## Rationale

An exact independently supplied content pin is provider-neutral, portable, and closes
the important replacement/TOCTOU gap because validation and parsing consume one byte
snapshot. Deriving the recorded configuration revision from that pin makes audit
provenance exact. Strict human-authorable text keeps operator provisioning inspectable
without adding a writer or configuration framework.

## Consequences

- Missing, relative, noncanonical, symbolic, nonregular, unreadable, oversized, growing,
  malformed, noncanonical, unpinned, unsupported, duplicate, or trailing configuration
  fails closed before an authorizer or audit is created.
- Key rotation or revocation requires a separately authorized deployment of new
  canonical bytes and its independently protected pin. Old-policy rollback fails after
  the pin advances.
- The loader alone is not a supported production interface. The next task must identify
  and bind protected installed pin metadata while keeping the proof command unable to
  override it.
