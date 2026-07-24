# 2026-07-24: Assess Gate 8 Maturity Against Every Exit Criterion

Status: Accepted Decision

## Context

Gate 8 now contains durable admission, scheduling, lifecycle, retry, external-effect,
process-isolated execution, supported submission/cycle/drain commands, and bounded
recovery inspection. Individual boundaries range from Contract Verified through
Integrated and Operational sub-paths, while the Roadmap still marks the whole gate
`Specified - Next`.

Accumulating sub-capabilities does not itself prove whole-gate maturity. Gate 8 exit
criteria also name interruption recovery, message-mediated worker communication,
explicit cancellation and other terminal events, authority preservation, duplicate and
lost-acknowledgement recovery, lease expiry, supported state-version migration, and
recorded external-effect outcomes.

## Decision

Perform a bounded evidence audit of every Gate 8 scope item and exit criterion before
selecting another implementation increment or changing whole-gate maturity.

The audit will use named production connections and fresh tests as evidence. Contract
existence, isolated unit tests, planned ownership, or an Operational sub-path will not be
promoted into a whole-gate claim. Each criterion will be classified as satisfied,
partially satisfied, or unsatisfied, with the exact missing connection assigned to its
owning delivery gate.

If any exit criterion lacks a named real connection or required recovery fixture, Gate 8
will remain `Specified - Next`. The assessment may identify the next bounded task, but it
will not implement authenticated controls, production adapters, state migration,
polling/service behavior, or multi-agent execution.

## Rationale

An explicit maturity audit prevents a long list of verified components from being
mistaken for an integrated operating system path. It also exposes cross-gate dependency
gaps before Gate 9 is activated against an assumed operational runtime.

## Consequences

- Whole-gate promotion requires evidence for every exit criterion.
- Missing Gate 11, 12, or 13 connections remain owned by those gates.
- The audit changes no runtime behavior, persistence schema, authority, or external
  state.
