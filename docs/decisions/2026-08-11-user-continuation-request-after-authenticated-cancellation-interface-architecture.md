# User continuation request after authenticated cancellation interface architecture on 2026-08-11

Status: Accepted Decision

## Context

The completed architecture task selected a short-lived detached signed exact-request
grant, separately trusted public verification policy, and deterministic non-secret
authorization audit point as the first admissible source for a composed
`ControlRequestAuthorizer`. Its recorded next work requires explicit user authority
before implementation.

The user requested that work continue. The prior application and architecture changes
remain verified but uncommitted, and the recovery checkpoint is empty.

## Decision

Authorize one bounded local implementation task for the reusable authenticated-
cancellation core: the signed-grant value/parser/canonical verification input, an
immutable injected public trust-policy value, public-signature and exact-request
verification, the deterministic authorization audit value/filesystem store, and the
authorizer composition that persists audit before returning approval.

Focused tests may generate ephemeral in-memory signing key pairs solely to exercise the
public verifier. Test private material must not be persisted, printed, exposed through
production APIs, or treated as a production credential source.

This continuation does not authorize a production signer or credential issuer, private-
key/password/token/session storage or input, an external identity provider, network
authentication, trust-store or permission mutation, a production trust-configuration
loader, CLI/API/editor/Desktop adapter, queue disposition, process signalling, Message
Bus cancellation, Tool or external-effect cancellation, `PAUSE`/`RESUME`, runtime/event
schema migration, commit, push, merge, release, deployment, destructive action, paid
service, external message, cleanup/retention, or unrelated work.

## Rationale

The reusable verifier/audit/authorizer core is independently testable and is the
smallest implementation that makes the accepted authentication contract real without
letting an interface invocation choose its own trust root. Deferring the production
trust bootstrap and CLI keeps the unresolved operator-owned configuration source from
becoming same-invocation self-approval.

## Consequences

- Production code may consume public verification material only through an already
  trusted injected immutable policy in this task.
- The audit point must precede approval and preserve the accepted pre-runtime retry,
  corruption, expiry, rotation, and revocation semantics.
- A later architecture/implementation task must fix the operator-owned trust bootstrap
  before composing the production CLI.
- The prior verified working-tree boundary remains preserved and uncommitted.
