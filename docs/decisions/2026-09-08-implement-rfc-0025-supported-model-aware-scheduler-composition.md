# User continuation request on 2026-09-08 into RFC-0025 supported model-aware Scheduler composition implementation

Status: Accepted Decision

## Context

RFC-0025 is accepted and the completed specification task names RED-first
implementation as the next separately authorized work. The existing supported
Scheduler cycle, drain, and service commands still construct only the legacy worker,
while RFC-0023 typed execution and RFC-0024 governed submission connect only in tests.
The user requested continuation on 2026-09-08.

## Decision

Implement RFC-0025 through six sequential GREEN increments: exact optional CLI and
configuration values; a bounded public deterministic-fake worker composition with
combined runtime-event support; shared supported Scheduler command selection; real CLI
typed completion for cycle, drain, and service from test-seeded intent; refusal,
recovery, event, and reachability hardening; and final regression/document closure.

The model group supplies only the accepted closed selector, bounded gateway/response/
prompt-read/Tool limits, and bounded denied Tools. It never accepts profile, capability,
task, target, digest, provider, endpoint, credential, cost, or network authority.
Omission retains the exact legacy composition and supported output remains unchanged.

The work may add Java, tests, documentation, and ordinary local GREEN commits. It may
not add typed submission or receive, an interface profile format, a provider/router,
network or credentials, durable schema changes, new runtime events, cancellation
propagation, background execution, push, merge, release, deployment, permission change,
destructive cleanup, or an external effect.

## Consequences

- Supported foreground Scheduler commands can explicitly select the already accepted
  deterministic-fake typed execution path without gaining typed ingress authority.
- Every process configuration source remains explicit and bounded.
- Legacy payload execution, output, recovery, and optional runtime events remain
  regression obligations rather than inferred compatibility.
- Capability maturity changes only after fresh supported CLI integration evidence.
- Interface-owned complete-profile input and supported typed submission remain the next
  separately authorized contract.
