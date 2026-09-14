# User continuation request on 2026-09-14 after RFC-0027 completion

Status: Accepted Decision

## Context

The last Gate 8 whole-gate assessment predated its complete runtime-event owner
connections, bounded foreground service, supported migrations and recovery fixtures,
authenticated cancellation application core, and RFC-0027 typed spool ingress. Fresh
post-RFC-0027 characterization now supports every Gate 8 exit criterion for the bounded
event-driven single-agent runtime.

The remaining partial scope is already owned by other gates: durable Message Bus
journaling and retention by Gate 7, model and context budgets by Gate 9, Memory and
Reflection by Gate 10, production effect adapters by Gate 11, authenticated interfaces
and remaining controls by Gate 12, and background or role workers by Gate 13. Treating
those later capabilities as Gate 8 blockers creates a circular Roadmap dependency and
encourages unowned implementation inside the wrong gate.

## Decision

Define the Gate 8 completion boundary as the bounded event-driven single-agent runtime:
durable Goal and AgentRun lifecycle, Scheduler admission and selection, fenced leases,
explicit runtime events, retry and terminal disposition, recovery checkpoints and
supported migrations, Message-Bus-mediated Work and Result paths, authority
preservation, and explicit external-effect outcome recording.

Promote that Gate 8 boundary to Integrated. Retain its separately invoked cycle, drain,
service, direct submission, generated submission, and typed spool workflows as
Operational sub-paths rather than claiming the whole gate is Operational or Released.

Move the sole `Specified - Next` Roadmap marker to Gate 9. Its dependency is satisfied
by the existing Operational explicit workflows over the Integrated event-driven
single-agent foundation; it does not require real providers, MCP, Memory, production
adapters, authenticated interfaces, or multi-agent workers to be implemented inside
Gate 8 first.

Keep all cross-gate extensions with their existing owners. This decision changes no
production behavior, durable format, runtime authority, or external state and does not
activate a Gate 9 implementation task by itself.

## Rationale

Capability maturity follows named current evidence, not the age of a Roadmap marker or
the number of accumulated types. Fresh tests cover all six Gate 8 exit criteria, while
the remaining items are explicit dependencies of later product layers. Advancing the
planning marker exposes the real next decision surface without overstating providers,
MCP, interfaces, Memory, adapters, background execution, or release readiness.

## Consequences

- `PROJECT_STATE.md` owns the Integrated Gate 8 judgment and the current Gate 9
  maturity.
- `ROADMAP.md` owns the revised Gate 8 boundary and sole Gate 9 planning marker.
- The next task is a bounded Gate 9 maturity-baseline audit of existing RFC-0013 through
  RFC-0027 evidence before another model or MCP mechanism is selected.
- Push, merge, release, deployment, provider/network work, paid services, credentials,
  MCP, Memory, plugins, interfaces, multi-agent, background execution, cleanup, and
  destructive actions remain separately authorized.
