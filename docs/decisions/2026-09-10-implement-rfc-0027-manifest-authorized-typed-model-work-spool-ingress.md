# User continuation request on 2026-09-10 into RFC-0027 manifest-authorized typed ModelWork spool ingress implementation

Status: Accepted Decision

## Context

RFC-0027 is accepted and its completed specification task names RED-first
implementation as separately authorized next work. RFC-0026 already provides direct
typed submission, but no supported local file-spool path separates governed manifest
preparation from the Scheduler queue writer. The user requested continuation on
2026-09-10.

## Decision

Implement RFC-0027 through six sequential GREEN increments: shared package-local
manifest preparation; exact typed-publisher CLI parsing; manifest-only filesystem
publication; manifest-authorized typed receive; bounded no-follow receive and
acknowledgement recovery; and real publisher-to-receiver-to-Scheduler integration plus
full closure.

The publisher must persist or exact-replay the RFC-0024 manifest before exposing a
transport point and must never access the queue. The receiver must accept only the
four RFC-0027 locators/roots, derive envelope, route, queue, capacity, capability, and
priority authority from the exact manifest, admit through the real Message Bus, and
acknowledge only after durable admission. Capability remains independent of the
untrusted profile.

The existing random-point file transport keeps bounded at-least-once publication.
Uncertain retries may create multiple exact points, but each explicitly received point
must converge on one idempotent queue admission. Existing direct and legacy command
behavior and all durable binary formats remain unchanged.

This decision authorizes scoped Java/test implementation, owned documentation and
README synchronization, focused and full verification, and ordinary local GREEN
commits. It authorizes no exactly-once receipt/outbox, scan, cleanup, retention,
background consumer, provider/router/registry, remote transport, network, credentials,
spend, MCP, schema migration, push, merge, release, deployment, permission change,
destructive cleanup, or other external effect.

## Consequences

- RFC-0024 manifest construction becomes one shared internal source for direct and
  spooled deterministic-fake intent.
- Typed transport and CLI input gain no capability, queue, route, or execution
  authority.
- Every supported publish/receive crash prefix requires fresh real-filesystem evidence.
- Capability maturity changes only after supported-path and full regression evidence.
