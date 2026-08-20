# User continuation request on 2026-08-20 into the RFC-0014 model execution profile value layer implementation

Status: Accepted Decision

## Context

RFC-0014 is accepted, verified as a specification, and delivered on `main`. Its named
immediate consumer is a pure immutable value layer under `com.enhancer.model`. The user
requested that work continue after the specification delivery.

## Decision

Authorize RED-first contract tests and the minimum Java 17 implementation of the exact
RFC-0014 `ModelExecutionProfile` hierarchy under `com.enhancer.model`, followed by
focused and full regression verification, lifecycle-document synchronization, and
ordinary local commits at verified GREEN increment boundaries.

The implementation remains an uncomposed provider-neutral value layer. It may validate
requirements but cannot select a provider, authorize network or remote transmission,
expose credentials, approve spend, widen Tool or task authority, change current gateway
or Scheduler behavior, or introduce implicit defaults.

## Rationale

Implementing the accepted pure value contract before request composition preserves the
distinction between requirement data and execution authority and allows its bounds,
vocabularies, cross-field invariants, and forbidden-field shape to be verified without
routing or provider concerns.

## Consequences

- New production types and tests are limited to the model-package value layer.
- `ModelRequest`, `ModelGateway`, Tool, CLI, Scheduler, adapter, and durable schema
  contracts remain unchanged.
- Composition, routing, providers, network transmission, credentials, paid services,
  pricing, tokenization, and maturity beyond this value layer require later authority.
- Push, merge, release, deployment, history rewrite, permission changes, and destructive
  cleanup remain unauthorized.
