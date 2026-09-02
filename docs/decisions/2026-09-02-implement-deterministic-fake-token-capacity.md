# User continuation request on 2026-09-02 into RFC-0021 sequence 1 implementation

Status: Accepted Decision

## Context

RFC-0020's verified standalone candidate evaluator intentionally stops at
`TOKEN_SEMANTICS_UNAVAILABLE`. RFC-0021 has since accepted one fake-only Unicode-
scalar token unit, a versioned token-aware candidate identity, four proven capacities,
and the remaining suitability order. The completed delivery task named RED-first
implementation sequence 1 as the sole next action, and the user requested continuation
on 2026-09-02.

## Decision

Authorize the bounded RED-first implementation of RFC-0021 sequence 1: a pure
well-formed Unicode-scalar counter with checked exact-fake response-count derivation,
fixed `deterministic-fake-v2` candidate facts, and the remaining field-free candidate/
profile suitability predicates.

The implementation may make the isolated evaluator's `Suitable` branch reachable,
but it must retain exact admitted and candidate identities and remain without any
production caller. The counter and candidate facts are fake-specific and must not
change generic `ModelUsage`, the exact gateway rendering, or another model contract.

This decision authorizes corresponding tests, canonical document synchronization,
fresh verification, and ordinary local GREEN commits. It authorizes no concrete-
request budget decision, same-request invocation seam, gateway or Tool call, Scheduler
execution, RunRecord or schema write, runtime/finalizer/retry/recovery integration,
provider, network, credential, paid service, push, merge, release, deployment,
permission change, external effect, or destructive cleanup.

## Consequences

- The exact bound fake candidate advances to stable identity
  `deterministic-fake-v2` only when it exposes the accepted fixed token semantics and
  capacities; historical `deterministic-fake-v1` retains its documented unavailable
  meaning.
- Standalone suitability can truthfully evaluate context, input, output, total, cost,
  and classification requirements and return an ephemeral `Suitable` value.
- One concrete request may still be budget-invalid; no caller may consume `Suitable`
  until a separately accepted exact-request and invocation boundary exists.
- Current typed ModelWork execution guards, durable schemas, gateway behavior, generic
  usage accounting, and capability maturity remain unchanged.
