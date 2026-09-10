# User continuation request on 2026-09-10 into manifest-authorized typed ModelWork spool ingress specification

Status: Accepted Decision

## Context

RFC-0026 makes deterministic-fake typed intent reachable through one supported direct
submission command, while RFC-0018 already reserves payload-sensitive
transport-spool v2 and requires any future typed receiver to obtain capability and
priority from a pre-existing exact manifest. The existing legacy Work spool commands
accept caller capability, priority, queue, and destination values and therefore cannot
be widened safely. The Gate 9 Roadmap still identifies typed spool ingress as later
work, and the user requested continuation on 2026-09-10.

## Decision

Accept RFC-0027 as the documentation-only contract for separate
`scheduler-spool-deterministic-fake-model-work` and
`scheduler-receive-deterministic-fake-model-work` commands.

The publisher reuses one extracted RFC-0024 manifest-preparation boundary. It validates
the exact RFC-0026 complete-profile and submission inputs, resolves or durably creates
the immutable manifest, and only then publishes
`queue(manifest.queueId)` plus `manifest.workMessage`. It accepts no capability,
queue, destination, derived identity, time, snapshot, Tool scope, execution policy, or
provider authority and never creates or admits a queue.

The four-option receiver point-resolves one named pending or acknowledged transport
point, decodes exact ModelWork v2, resolves the manifest by message ID, requires exact
envelope and derived-route equality, and takes queue identity, capacity, required
capability, and priority only from that manifest. It delivers through the real Message
Bus into exact durable admission and acknowledges the point only afterward. The
profile's capability remains untrusted requirements data for later fresh RFC-0016
admission.

The existing random-point file transport retains bounded at-least-once publication.
An uncertain publisher response may leave multiple exact points, but receiver replay
admits queue work once and acknowledges each explicitly named point. No exactly-once
publication, scan, receipt, outbox, cleanup, or retention claim is made. Existing
binary formats are sufficient.

This decision authorizes only RFC, architecture, state-consistency, Roadmap, task,
decision/index, verification, handoff, and Changelog documentation, focused and full
verification, and ordinary local GREEN commits. It authorizes no Java or test-source
change, actual publication/receive/execution, legacy command widening, provider,
network, credentials, spend, MCP, schema migration, background service, push, merge,
release, deployment, permission change, destructive cleanup, or external effect.

## Consequences

- Direct RFC-0026 submission remains unchanged and preferred for same-process use.
- A separate local process can later publish typed intent without granting queue or
  capability authority to transport or CLI input.
- Manifest-only and every receive/acknowledgement crash prefix have explicit replay
  behavior before implementation.
- Legacy Work payload bytes, options, capability source, and receive behavior stay
  unchanged.
- Stronger publication identity, remote trust, directory consumption, cleanup,
  retention, and implementation remain separately authorized.
