# User continuation request on 2026-08-21 into the complete model invocation input specification

Status: Accepted Decision

## Context

RFC-0014 through RFC-0016 are Accepted and their pure Java contracts are Contract
Verified. RFC-0016 deliberately leaves the direct CLI and Scheduler unsupported because
neither current path supplies both a complete execution profile and an independent
authoritative required-capability source without defaults or conflation.

The completed RFC-0016 implementation task names the next work as specifying the
smallest explicit caller input contract that can supply those values without routing or
remote-transmission authority. The user requested continuation on 2026-08-21.

## Decision

Authorize a documentation-only task to define and accept RFC-0017 as the minimum
provider-neutral model invocation input/source contract. The specification must name
the owner and provenance of every RFC-0014 field, keep the authoritative capability
separate from untrusted profile requirements, define absence and compatibility
behavior, and preserve the existing request, admission, Tool, CLI, Scheduler, gateway,
and durable-schema contracts.

This decision does not authorize Java implementation; command or durable-schema
changes; caller migration or runtime wiring; defaulting, inference, registries, or
ambient lookup; model suitability; gateway or provider execution; routing; network or
remote transmission; credentials; paid services; push, merge, release, deployment, or
destructive cleanup.

## Consequences

- The task may add one accepted RFC and synchronize its architecture, index, task,
  changelog, and verification owners.
- Current CLI and Scheduler behavior remains unchanged and unsupported by the new
  contract until a later separately authorized implementation and migration task.
- A profile remains untrusted requirements data; authoritative capability remains a
  distinct governed projection and cannot be manufactured from profile or model class.
