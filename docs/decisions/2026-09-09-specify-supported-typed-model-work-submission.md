# User continuation request on 2026-09-09 into supported typed ModelWork submission specification

Status: Accepted Decision

## Context

RFC-0024 implements a governed manifest-first deterministic ModelWork producer and
RFC-0025 connects its typed execution path to the supported foreground Scheduler
commands, but the producer remains package-local and test-owned because no interface
supplies a complete `ModelExecutionProfile`. RFC-0024 orders an interface-owned profile
format with either direct typed submission or spool publication next. The completed
task requested separate authority, and the user requested continuation on 2026-09-09.

## Decision

Accept RFC-0026 as the documentation-only contract for a separate
`scheduler-submit-deterministic-fake-model-work` command. It reads one explicitly named
project-contained, no-link, strict-UTF-8 profile file of at most 4,096 bytes with
exactly thirteen ordered LF-terminated entries mapping to all ten RFC-0014 components.
Missing, partial, duplicate, unknown, reordered, defaulted, normalized, or ambient input
fails before manifest or queue access.

The command maps its exact submission, task, producer, target, expected-response
digest, parsed profile, capacity, and priority input into the existing RFC-0024
producer. It accepts no capability: the closed repository-owned
`deterministic-echo` source remains independent, and profile disagreement remains for
fresh RFC-0016 refusal. Existing schemas retain the complete profile and fixed
capability unchanged.

Direct submission is selected before spool publication because it reaches durable
admission through the existing producer without adding transport points, a receiver,
acknowledgement, capacity release, or transport recovery. Submission and RFC-0025
execution remain separate explicit operator commands.

This decision authorizes only RFC, architecture, state-consistency, Roadmap, task,
decision/index, verification, handoff, and Changelog documentation, focused and full
verification, and ordinary local GREEN commits. It authorizes no Java or test-source
change, actual submission or execution, publisher/receiver, provider, network,
credential, spend, schema migration, push, merge, release, deployment, permission
change, destructive cleanup, or external effect.

## Consequences

- The first supported typed input has one complete strict source and no defaults.
- Capability provenance remains independent of profile requirements and caller input.
- Manifest-first replay compares the parsed semantic profile without persisting its
  transient source path or introducing a new store.
- Existing legacy commands and durable formats remain unchanged.
- RED-first implementation and any later spool publisher/receiver remain separately
  authorized work.
