# 2026-08-11: Authenticate Cancellation Interfaces With Detached Signed Exact-Request Grants

Status: Accepted Decision

Context:

- The shared `FileSystemAuthenticatedCancellationApplication` deliberately requires an
  injected trusted `ControlRequestAuthorizer`, but no supported interface can yet
  construct that port from authenticated evidence.
- A caller-supplied actor, authorization UUID, approval flag, envelope producer or
  reason, ambient operating-system username, repository file, or proof-file possession
  would let the caller manufacture its own authority.
- The existing cancellation application record retains the accepted authorization ID,
  actor, Goal, Control request, AgentRun, and authorization/application times. It does
  not retain issuer, verification key, proof digest, expiry, trust-policy revision, or
  revocation facts. The generic Evidence Store uses random references and content
  integrity; it is not a deterministic authorization authority or replay index.
- An external identity-provider callback could supply strong identity, but it requires
  provider, network, token, session, secret, and revocation integration that the user
  has not authorized.

Decision:

- The first supported authenticated-interface composition will verify a short-lived,
  detached, externally signed exact-request grant. The grant is untrusted input until a
  verifier validates it against separately provisioned public trust policy. Enhancer
  does not issue the grant and never receives or persists its private signing key,
  password, bearer token, session secret, or other private credential.
- The signed, versioned, domain-separated canonical bytes bind all of the following:
  the proof schema/domain and intended Enhancer trust-domain audience; canonical Goal,
  retained Control-message, authorization, issuer, key, and issuer-scoped subject
  identities; explicit `CANCEL`; a SHA-256 digest over a deterministic length-framed
  projection of the complete retained `MessageEnvelope` and `ControlPayload`; the
  policy revision; and issued-at plus expires-at instants. The retained-request
  projection includes message, correlation, optional causation, logical-run, producer,
  occurred-at, signal, and reason fields. Any absent, malformed, non-canonical,
  mismatched, unsupported, expired, or changed binding fails closed.
- `actorId` is derived deterministically from the verified issuer and subject, never
  copied from interface input. `authorizationId` is the grant occurrence and durable
  audit key. `ControlAuthorizationDecision.Approved.authorizedAt` is the signed issue
  time; verification observation time remains a separate audit fact. The trust-policy
  key entry, not a proof-controlled algorithm field, fixes the accepted signature
  algorithm.
- A future immutable trusted configuration is supplied to the composition root from a
  separately authorized operator-owned source. It identifies its configuration and
  policy revisions, audience/trust domain, accepted issuers and subjects/actions,
  public verification keys with stable key IDs and fingerprints, allowed algorithms,
  key validity and revocation effective times, maximum grant lifetime, and bounded
  clock skew. Proof content, the retained request, the project repository, ambient
  username or environment, and the same interface invocation cannot add or replace a
  trust root, key, issuer, policy revision, clock, or revocation override. Public keys
  are non-secret but authority-bearing; private material remains outside Enhancer.
- Verification uses an injected trusted clock. Before a runtime cancellation exists,
  first use and retry both require the identical transient proof digest and current
  signature, audience, exact-request, issue/not-future, expiry, maximum-lifetime, key
  validity, policy, and revocation checks. Rotation or revocation therefore blocks an
  unapplied grant at or after its effective boundary. It does not rewrite or undo a
  cancellation application already durable in AgentRuntime.
- A future authorization-specific immutable audit store persists a successful
  verification before `Approved` is returned. It is deterministically keyed by
  `authorizationId`, uses a versioned integrity envelope, exact-replays identical
  content without revision, and rejects changed reuse or corruption. The record holds
  normalized non-secret signed claims, exact request and proof SHA-256 digests, issuer,
  subject/actor, key ID/fingerprint and fixed algorithm, issued/expires/verified times,
  trust-configuration and policy revisions, revocation snapshot identity or effective
  fact, and verifier version. It never retains the raw proof or signature, a private
  key, password, bearer/session token, or other reusable secret. The generic Evidence
  Store cannot substitute for this deterministic point contract.
- The durable order is retained request resolution -> proof and current trust/time/
  revocation validation -> authorization audit persist/exact replay -> `Approved` ->
  existing terminal runtime revision -> event append/exact replay -> opaque point
  publication/exact replay. There is no cross-store transaction. Proof or audit failure
  changes no runtime/event state. An audit-only prefix is safe but is not evergreen
  authority: retry needs the same transient proof and current validation. Once the
  runtime record is durable, existing replay bypasses authorization and may repair the
  event/publication suffix even after grant expiry, key rotation, or revocation.
  Missing or corrupt pre-runtime audit fails closed and is neither overwritten nor
  reconstructed. A terminal runtime with missing audit remains historical runtime
  truth, exposes degraded audit availability, and requires separately authorized repair
  policy; the audit is never fabricated from the smaller runtime record.
- The first future interface consumer is a separately authorized production CLI
  cancellation-application command, working name `scheduler-apply-cancel`. It will
  accept explicit Goal and retained Control-message identities plus a detached proof
  path as untrusted request input, receive its verifier and trust policy from a separate
  trusted composition root, and call
  `FileSystemAuthenticatedCancellationApplication.apply(goalId, controlMessageId)`.
  Existing spool/receive commands remain untrusted transport/admission surfaces. Later
  API, editor, and Desktop adapters reuse the same shared composition rather than
  inventing interface-specific approval.

Rationale:

This pattern is the smallest provider-neutral composition that proves both an external
approver and the exact retained cancellation without requiring Enhancer to hold a
private credential or contact an identity provider. Separate public trust configuration
prevents proof possession and repository control from becoming self-approval. A
deterministic audit point closes the issuer/key/policy/expiry evidence gap while
preserving the existing source-first terminal runtime and replay contracts.

Consequences:

- Ambient identity, caller fields, unsigned files, repository content, file ownership
  or possession, and the existing injected port alone are not supported authentication
  sources.
- A later implementation task needs explicit user authority for the proof parser and
  verifier, trusted configuration source, public-key and revocation policy, dedicated
  audit value/store, composition, tests, and CLI. Credential issuance, private-key or
  token handling, external IdP integration, and trust-store or permission mutation need
  their own explicit authority.
- No current production or test code, runtime/event schema, credential store, CLI/API/
  editor/Desktop adapter, queue disposition, process signal, Message Bus cancellation,
  Tool/effect cancellation, or `PAUSE`/`RESUME` behavior changes in this decision.
- `authorizationId` can join the new audit point to the existing cancellation record,
  so no runtime schema change is required for the minimum composition. Putting an audit
  reference or digest directly into runtime/event records would require a separate
  schema-evolution and migration decision.
