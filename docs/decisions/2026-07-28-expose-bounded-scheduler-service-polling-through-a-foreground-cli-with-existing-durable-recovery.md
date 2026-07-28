# 2026-07-28: Expose Bounded Scheduler Service Polling Through A Foreground CLI With Existing Durable Recovery

Status: Accepted Decision

## Context

Gate 8 now has a finite caller-driven `BoundedSchedulerService`, but no supported entry
point consumes it. The existing `scheduler-cycle` and `scheduler-drain` commands must
retain their one-cycle and immediately-ready-work behavior. A first service entry point
must demonstrate that bounded idle polling composes with the existing durable
checkpoint and lease/fence recovery without introducing a daemon, unauthenticated
control application, or another progress store.

## Decision

Add a separate foreground `scheduler-service` CLI command. It reuses every explicit
`scheduler-cycle` composition input and additionally requires:

- `--max-cycles`, from 1 through 4096;
- `--max-consecutive-idle-cycles`, from 1 through 4096; and
- `--idle-wait-millis`, positive and no greater than one hour.

The command constructs the existing process-isolated `DurableAgentRunWorker`, passes it
to `BoundedSchedulerService`, and runs the service on the invoking thread. The local stop
signal is the invoking thread's interrupt state. Interruption while waiting is handled by
the service contract, restores the flag, and produces the typed `INTERRUPTED` result.
The CLI creates no thread, daemon, supervisor, automatic startup, durable service
progress, or signal/control store.

Bounded output reports the typed stop reason, stable exit code, queue identity and
revision, exact service counts, queue partition counts, and RunRecord count. A failed
work disposition uses the existing Scheduler work-failed exit; finite limits, local stop,
and interruption are successful bounded service termination.

Fresh real-filesystem integrations must prove both:

- a persisted cycle-intent restart prefix is resumed by a new supported service
  invocation; and
- an active execution whose lease expired after a simulated process stop is reclaimed
  under the same Goal/AgentRun identity with a greater fence and reaches one terminal
  disposition.

The second case may re-execute when no RunRecord reference was checkpointed, consistent
with the accepted at-least-once contract. It must not create a second Goal or AgentRun.

## Rationale

A foreground command is the smallest supported consumer of the bounded service contract.
It makes polling explicit and finite while retaining operating-system supervision as a
caller concern. Reusing the existing composition keeps queue state, cycle checkpoint,
runtime lease/fence, RunRecord, invocation spool, and external-effect recovery in their
current owning components.

## Consequences

- `scheduler-cycle` and `scheduler-drain` inputs and behavior remain unchanged.
- The supported service may observe work arriving after idle waits, but never admits or
  creates work.
- Local thread interruption is lifecycle input, not authenticated Gate 12 control.
- The command is an Integrated bounded foreground service connection, not a daemon,
  liveness guarantee, whole-Gate promotion, or general orphan-reclamation mechanism.
- Durable/supervised background lifecycle, broader orphan discovery/reclamation,
  authenticated controls, production external adapters, commit, push, release, and
  deployment remain separate work.
