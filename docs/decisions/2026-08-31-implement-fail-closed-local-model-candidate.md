# User continuation request on 2026-08-31 into fail-closed local model candidate implementation

Status: Accepted Decision

## Context

RFC-0020 specifies a closed binding for the exact deterministic fake gateway and a
candidate suitability decision boundary. It deliberately stops before truthful token
semantics or capacities exist, so the only valid current result after model class,
capability, and reasoning checks is `TOKEN_SEMANTICS_UNAVAILABLE`. The user requested
continuation on 2026-08-31.

## Decision

Authorize a RED-first implementation of the first RFC-0020 sequence: one opaque
candidate bound only to the exact final `DeterministicFakeModelGateway`, repository-owned
fixed facts, a field-free suitability evaluator, and a sealed result with the ordered
rejection reasons defined by the RFC. Add focused behavior, shape, dependency, and
non-wiring tests, synchronize owned project documents, and commit each verified GREEN
increment.

The evaluator must remain fail closed at `TOKEN_SEMANTICS_UNAVAILABLE`. This decision
does not authorize invented token semantics or capacities, a reachable `Suitable`
evaluation, candidate selection, caller wiring, Tool or gateway invocation, RunRecord
or schema changes, runtime or process wiring, persistence, provider or route selection,
network or remote transmission, credentials, spend, push, merge, release, deployment,
permission change, or destructive cleanup.

## Consequences

- The repository can represent and inspect exactly one proven closed local fake
  candidate without generalizing candidate provenance or gateway types.
- Suitability remains invocation-local eligibility data and grants no execution or
  external-effect authority.
- Deterministic token semantics and proven capacities require a separate accepted task
  before the evaluator may return `Suitable` or any caller may consume that result.
