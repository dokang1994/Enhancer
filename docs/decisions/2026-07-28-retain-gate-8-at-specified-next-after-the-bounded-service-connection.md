# 2026-07-28: Retain Gate 8 At Specified Next After The Bounded Service Connection

Status: Accepted Decision

## Context

The bounded foreground `scheduler-service` command closes the previously named
supported-service-entry-point gap and adds real cycle-intent restart plus expired-lease
reclamation evidence. Gate 8 nevertheless has cross-gate exit criteria that cannot be
inferred from this connection: worker communication through the message bus, authenticated
control application, model/context budgets, Memory runtime, production external-effect
adapters, and background/supervisor orchestration.

The roadmap also requires explicit orphan detection/reclamation and recovery without an
unrecorded duplicate effect. Existing Gate 8 evidence covers queue-active recovery,
expired runtime leases, checkpointed references, deterministic point recovery for the
lost result-publication acknowledgement window, and explicit external-effect outcomes.
It does not authorize a general filesystem scanner, retention/cleanup policy, or
cross-store orphan collector.

## Decision

Retain Delivery Gate 8 at `Specified - Next`.

Classify the post-service evidence as follows:

- interruption, checkpoint restart, lease expiry, queue disposition acknowledgement,
  deterministic lost-acknowledgement point recovery, supported queue/submission/cycle
  migration, and runtime authority preservation have named real connections;
- bounded foreground service operation is Integrated through `scheduler-service`;
- general orphan discovery/cleanup is not silently required for the accepted at-least-once
  correctness contract, but any future cleanup or inventory feature requires its own
  retention, scan-bound, authority, and cross-store consistency decision;
- message-mediated worker operation remains a Gate 7/8 connection gap because the
  process-isolated work/result spool is a named transport consumer, not a supported
  durable message-bus worker entry point;
- authenticated cancellation/pause/resume application remains Gate 12;
- model/context/cost budgets remain Gate 9, Memory runtime remains Gate 10, production
  adapters remain Gate 11, and background/supervisor worker topology remains Gate 13.

Do not promote the whole gate until the remaining cross-gate exit criteria have named
supported integrations and fresh evidence. Do not implement a background daemon or broad
orphan scanner as a substitute.

## Rationale

The service integration proves finite polling and existing recovery composition, not the
entire event-driven runtime. Keeping ownership explicit prevents Gate 8 from absorbing
security, model-routing, memory, adapter, or orchestration authority merely to improve a
roadmap label.

## Consequences

- Gate 8 stays `Specified - Next`; its bounded service connection remains Integrated.
- The next Gate 8-adjacent implementation candidate is the supported durable
  message-bus-to-worker connection owned jointly with Gate 7.
- General orphan inventory/cleanup, daemon supervision, authenticated controls, budgets,
  Memory, production adapters, and role-based workers remain separate accepted tasks.
- This assessment changes no production behavior, schema, dependency, or authority.
