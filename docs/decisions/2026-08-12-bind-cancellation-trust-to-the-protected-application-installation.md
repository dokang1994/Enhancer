# 2026-08-12: Bind Cancellation Trust To The Protected Application Installation

Status: Accepted Decision

## Context

`PinnedFileCancellationGrantTrustPolicyLoader` safely consumes an absolute policy path
and independent whole-file pin, but accepting both from one cancellation invocation
would permit self-approval. Portable Java 17 ACL and owner inspection cannot establish
a cross-platform approval root. The installed application artifact is already a trusted
execution prerequisite and can anchor separately protected deployment metadata.

The shared cancellation application bypasses its authorizer once a terminal application
record is durable so that exact replay can repair event publication after proof expiry,
key rotation, or revocation. CLI composition must not eagerly read proof or trust and
accidentally weaken that recovery contract.

## Decision

- The production composition derives one installation anchor only from
  `EnhancerCli.class` protection-domain CodeSource. Its location must be a `file:` URI
  resolving to one absolute normalized exact-real existing regular non-symbolic JAR.
  Exploded classes, symbolic/Junction-mediated artifacts, missing or ambiguous parents,
  and non-file schemes fail closed.
- The sole metadata point is the fixed sibling
  `enhancer-cancellation-trust-metadata-v1` beside that JAR. No CLI option, environment
  variable, JVM property, current directory, home directory, repository, request,
  proof, or ambient identity can select, discover, or override the anchor or metadata.
  A package-private test seam may inject an application-artifact path but is not a
  production request surface.
- The metadata is one bounded 4 KiB no-follow snapshot of an exact-real existing
  regular non-symbolic file under its exact-real parent. Its strict canonical UTF-8/LF
  form is exactly the header `enhancer-installed-cancellation-trust-v1`, then
  `policyPath=<absolute normalized platform path>`, then
  `policySha256=<64 lowercase hexadecimal characters>`, with a final LF. CR, invalid
  UTF-8, blank/comment/unknown/duplicate/trailing fields, alternate spelling, a
  relative/non-normal path, and excess bytes fail closed.
- The application JAR, metadata file, and their ancestor installation directories are
  deployment-protected authority and must not be writable by the runtime principal.
  If that assumption is violated, an attacker can replace path and pin together.
  Portable owner/mode/ACL checks remain defense in depth, not approval authority. The
  independently pinned policy may be writable without granting approval because any
  changed bytes cause denial, though deployment should protect it too.
- Add `scheduler-apply-cancel` with required `--runtime-root`, `--goal-id`,
  `--control-message-id`, `--proof-file`, and `--authorization-audit-root`, plus the
  existing optional all-or-none runtime-event root, publication root, and bounded
  capacity group. These are request identities or storage locations, never trust
  overrides. Policy/pin/metadata, actor/authorization, issuer/key/subject, algorithm,
  clock/time, policy/revocation, credential, and approval fields are rejected.
- A lazy trusted authorizer reads one exact-real no-follow proof snapshot bounded to the
  existing 16 KiB proof contract only if authorization is actually requested. It then
  loads installed metadata, the freshly pinned public policy, and composes the existing
  signed verifier and deterministic audit store with a trusted UTC clock. A retained
  terminal cancellation therefore reaches no proof or trust read and preserves the
  existing authorization-bypassing suffix recovery.
- Success emits `status=CANCELLATION_APPLIED`, exit `0`, and bounded non-secret fields
  already present in the terminal application record. It does not claim first apply
  versus replay because the shared application result does not expose that distinction,
  and it never emits proof/policy paths, pins, proof/key bytes or digests. Syntax,
  identity, proof-file access, installed metadata, and pinned-policy configuration
  failures exit `2`; authorization denial exits `20`; unexpected audit/runtime/event
  persistence or publication failures exit `70`.

## Rationale

One immutable fixed point next to the executed JAR is the smallest production binding
that cannot be redirected by request input. Strict single-snapshot readers preserve the
existing pin and proof integrity assumptions. Lazy authorizer construction composes the
new boundary without changing the terminal runtime or event schemas and keeps durable
replay independent from expired or unavailable transient authority.

## Consequences

- A missing production metadata file makes the command unavailable rather than falling
  back to repository or caller configuration. Development `gradle run` uses exploded
  classes and therefore also fails closed for this production command unless tests use
  the package-private composition seam.
- Provisioning/writing the metadata or policy, permissions, installer layout changes,
  rotation, application-version anti-rollback, private-key and credential issuance,
  and external identity-provider integration remain separate authorized operations.
- The command applies only the retained AgentRuntime cancellation fact and optional
  event publication. It does not dispose queue work, signal a process, cancel a Tool or
  external effect, or implement `PAUSE`/`RESUME`.
