# User continuation request on 2026-08-19 into the RFC-0013 model gateway implementation

Status: Accepted Decision

## Context

The 2026-08-18 recommendation track completed with all four increments delivered to
`main`: the installation-track freeze, the accepted RFC-0013 model gateway minimum
slice, the host-independent continuous verification workflow, and the Constitution
1.2.0 commit-cadence amendment. The completed task recorded implementation of the
RFC-0013 slice as its next step, the worktree is clean, and the checkpoint is empty.
The user requested that the project continue.

## Decision

Authorize one bounded test-first task implementing the RFC-0013 minimum slice exactly
as accepted: the new `com.enhancer.model` leaf package with the provider-neutral
`ModelGateway` port, immutable bounded request/response/usage records, the typed
failure contract, the injected credential-supplier boundary with no default provider,
the deterministic fake as the only executed gateway, one bounded never-invoked
provider adapter shape, and the `model-invoke` Tool composed into the existing
executor. The shared five-second CLI tool timeout becomes a per-tool composition
value, with the gateway timeout strictly inside the `model-invoke` tool timeout. The
slice is promoted by one governed CLI run against the deterministic fake that
persists a lifecycle-valid replayable RunRecord whose evidence reference resolves.

Verified GREEN increment boundaries inside the approved task authorize ordinary local
commits under Constitution 1.2.0. This decision authorizes no push, merge, tag,
release, deployment, network connection, credential, paid-service invocation, MCP,
routing, caching, streaming, real provider invocation, or change to the `loop`,
`run`, `verification`, `runtime`, or `bus` packages.

## Rationale

RFC-0013 is the accepted specification and names this implementation as its recorded
follow-up, so continuing into it is the smallest next increment that advances the
absent model-invocation capability. Executing only the deterministic fake keeps the
slice hermetic and reproducible while the port, budget, and credential boundaries
make later real-provider work an isolated adapter decision rather than a rewrite.

## Consequences

- Delivery Gate 9 gains its first executable vertical slice with reproducible
  evidence digests and no external dependency.
- Model output remains untrusted data: it grants no authority, widens no scope, and
  alters no document, task, or policy.
- Provider wire formats stay behind the package-private adapter shape and never
  reach a persisted type.
- Real provider invocation remains a separate explicit user authorization with its
  own accepted decision naming destination, purpose, and data classification.
- Push of the delivered increments to `main` remains a separate explicit user
  request.
