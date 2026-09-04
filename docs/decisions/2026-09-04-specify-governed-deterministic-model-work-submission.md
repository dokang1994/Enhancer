# User continuation request on 2026-09-04 into governed deterministic ModelWork submission specification

Status: Accepted Decision

## Context

RFC-0023 is internally Integrated for test-owned typed ModelWork, but no production
source constructs a new typed envelope and no supported Scheduler command supplies the
model-aware process configuration. RFC-0017 requires the complete profile and
authoritative capability to come from independent governed sources. Widening a legacy
submission command or external receiver would either treat untrusted data as
capability provenance or make typed work reachable before a supported consumer exists.
The completed RFC-0023 task named a governed producer or receiver as the next possible
boundary, and the user requested continuation on 2026-09-04.

## Decision

Accept RFC-0024 as the documentation-only contract for one separate internal
deterministic-fake typed ModelWork producer. Its immutable caller request carries a
canonical submission UUID, task identity, producer, target, expected-response digest,
one complete `ModelExecutionProfile`, queue capacity, and priority. It carries no
required-capability field.

The producer obtains exactly `deterministic-echo` from an explicit closed,
repository-owned source independent of request, profile, target, candidate, envelope,
manifest, queue, CLI, environment, repository content, and ambient configuration. The
profile remains untrusted requirement data and may disagree so RFC-0016 retains its
observable `REQUIRED_CAPABILITY_MISMATCH` refusal.

First use derives the existing stable submission identities, resolves an absent
manifest, reads governed context and the exact active task, requires `model-invoke`,
captures one clock value and repository-memory snapshot, builds one complete typed
envelope and manifest, and delegates to the unchanged durable submission service.
Replay resolves the manifest before clock or repository context, compares every
caller-owned input plus the fixed capability, and neither rewrites the manifest nor
advances the queue revision.

The first implementation may connect only to the existing internal model-aware worker
in test-owned storage. Supported Scheduler composition, an interface-owned complete-
profile format, typed submission or spool publication, and any manifest-authorized
receiver remain separately accepted work. Existing durable schemas are sufficient and
unchanged.

This decision authorizes only RFC, architecture, state-consistency, Roadmap, task,
decision/index, verification, handoff, and Changelog documentation, focused and full
verification, and ordinary local GREEN commits. It authorizes no Java or test-source
change, actual submission or execution, supported entry point, receiver, provider,
network, credential, spend, schema migration, push, merge, release, deployment,
permission change, destructive cleanup, or external effect.

## Consequences

- Capability provenance is explicit and cannot be supplied or inferred by the caller.
- Manifest-first ordering provides durable intent and exact replay without adding a
  second store or cross-store transaction.
- Successful submission means only durable admission, never execution or completion
  authority.
- The future RED-first producer has a real internal test consumer, while supported
  ingress remains unreachable.
- No capability maturity changes until a separately authorized implementation and
  fresh integration evidence exist.
