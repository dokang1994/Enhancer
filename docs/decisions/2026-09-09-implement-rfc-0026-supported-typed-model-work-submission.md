# User continuation request on 2026-09-09 into RFC-0026 supported typed ModelWork submission implementation

Status: Accepted Decision

## Context

RFC-0026 is accepted and the completed specification task names RED-first
implementation as the next separately authorized work. The RFC-0024 producer remains
package-local and test-owned even though RFC-0025 now supplies a supported typed
Scheduler consumer. The user requested continuation on 2026-09-09.

## Decision

Implement RFC-0026 through six sequential GREEN increments: strict bounded profile
reading; exact CLI command parsing; a narrow public filesystem facade and command
connection; durable first-use/replay/recovery integration; real supported submission-
to-Scheduler operator paths; and full regression/document closure.

The work may add Java, tests, documentation, README usage/recovery, and ordinary local
GREEN commits. Capability remains absent from caller input and fixed independently by
RFC-0024. Existing legacy commands and durable formats remain unchanged, and submission
never invokes Scheduler execution.

It may not add a typed spool publisher or receiver, capability inference or precheck,
profile defaults or ambient lookup, provider/router/network/credential/spend authority,
schema changes, implicit/background execution, push, merge, release, deployment,
permission change, destructive cleanup, or external effect.

## Consequences

- Supported typed intent gains one complete, strict, interface-owned input source.
- The RFC-0024 producer remains the sole manifest-first submission owner behind a
  narrow production facade rather than exposing its injected internals.
- Fresh end-to-end evidence must keep submission and RFC-0025 execution as separate
  commands and prove exact recovery without duplicate effects.
- Capability maturity changes only after the applicable supported paths pass fresh
  integration and full regression evidence.
