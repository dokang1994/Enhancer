# User continuation request on 2026-09-18 into RFC-0028 pure outbound route-admission implementation

Status: Accepted Decision

## Context

RFC-0028 now specifies the provider-neutral default-deny boundary required after the
exact RFC-0016 `OUTBOUND_POLICY_REQUIRED` result and before any remote model route can
be eligible. The repository still has no pure RFC-0028 values or evaluator, and its
only executable candidate remains the closed deterministic fake local route.

The user requested continuation on 2026-09-18. Implementing the pure contract is the
next accepted dependency; production fact acquisition, routing, providers, credentials,
network access, and spend remain later decisions.

## Decision

Implement RFC-0028 RED-first as immutable provider-neutral proposal and fact values,
closed eligible/refused outcomes, and one field-free evaluator under
`com.enhancer.model`. The evaluator must invoke RFC-0016 over the exact authority
inputs and continue only from its typed `OUTBOUND_POLICY_REQUIRED` refusal. All other
RFC-0016 outcomes remain terminal for outbound routing.

The pure evaluator must be deterministic and default-deny across exact candidate,
destination, purpose, retention, complete outbound content, inspection, redaction,
effective classification, and pricing bindings. It may make only independently proven
zero-charge proposals eligible and must retain exact input identity without exposing
prompt content in rendering.

No production source or caller is added. The implementation creates no Router,
provider adapter, executable remote candidate, endpoint, credential, network or spend
path, scanner, redactor, pricing feed, runtime integration, persistence, or durable
authority. Verified GREEN increments are committed locally; push, merge, release, and
deployment remain unauthorized.

## Rationale

Separating the pure decision from fact acquisition and effects makes the missing
policy intersection executable without allowing a caller to self-certify disclosure,
locality, freshness, price, or authority. RED-first closed contracts make fail-open
drift visible before any remote consumer exists.

## Consequences

- RFC-0028 gains a pure tested implementation while Gate 9 capability maturity does
  not change.
- Existing RFC-0016 and deterministic-fake local behavior remain unchanged.
- Eligible values remain reachable only from test-owned governed fixtures until a
  separately accepted production fact-source and Router task exists.
- Providers, scanners, redactors, pricing acquisition, spend authorization,
  credentials, network transmission, runtime integration, persistence, MCP, push,
  merge, release, and deployment remain separately governed.
