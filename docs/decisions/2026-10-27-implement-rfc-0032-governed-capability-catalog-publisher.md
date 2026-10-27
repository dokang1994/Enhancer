# User continuation request on 2026-10-27 into RFC-0032 governed capability catalog publisher implementation

Status: Accepted Decision

## Context

RFC-0032 is Accepted and defines how independently approved canonical remote-capability
catalog bytes may become RFC-0031's protected path-and-pin input without letting a
request, runtime, catalog, provider, or operator self-approve both bytes and pin. The
completed specification task names implementation or the later same-invocation Router
option consumer as separately authorized continuations, and the user requested that
work continue on 2026-10-27.

An earlier accepted decision freezes generic installation-subsystem derivatives until a
real consumer exists. RFC-0031 is the named Gate 9 consumer for this boundary, so this
work uses that narrow exception while remaining isolated from the generic Gate 16
installation framework.

## Decision

Implement RFC-0032 RED-first under `com.enhancer.model` as an unexposed provider-neutral
publisher boundary. Begin with immutable operation, success, refusal, intent, and result
values plus one package-private uncomposed publisher port. Publication identities use
the existing stable lowercase-label grammar and a maximum of 256 characters; INSTALL
forbids an expected-current binding digest and ROTATE requires exactly one lowercase
whole-file SHA-256 digest. Results expose only bounded non-secret identities and closed
status or refusal values.

Later increments in the same Active Task may extract one package-private pure canonical
catalog codec shared with RFC-0031, add the strict canonical binding codec and an
injected independent evidence-approval port, and implement a test-composed filesystem
publisher with the fixed artifacts, bounded no-follow snapshots, nonblocking lock,
catalog-first/binding-last atomic publication, INSTALL/ROTATE compare-and-swap, exact
replay, refusal precedence, and fail-closed recovery. The publisher remains without a
production caller or supported platform claim.

This decision authorizes bounded Java and test implementation, architecture guards,
documentation synchronization, fresh verification, checkpoint state, read-only
development-subagent review, and ordinary local GREEN commits required by repository
policy. It does not authorize a real installer, operator surface, authenticated
transport, evidence schema or store, runtime binding composition, installed production
catalog, permission mutation, generic installer derivative, provider, credential,
network, spend, route execution, runtime integration, MCP, deployment, cleanup, push,
merge, tag, release, or other external effect.

## Rationale

Starting with the pure closed contract makes authority and safe observability
executable before any writer exists. A shared canonical codec then prevents RFC-0031
and RFC-0032 from accepting different bytes, while an injected evidence verifier keeps
truth approval outside request and catalog content. Keeping the publisher uncomposed
and Gate-9-specific prevents a filesystem implementation from silently becoming an
installer or remote-route authority.

## Consequences

- The first increment creates no filesystem mutation path and no production publisher
  caller.
- The complete task may prove contract behavior in isolated temporary files but cannot
  claim Windows or POSIX permission enforcement, real installation support, sudden-
  power-loss durability, anti-rollback, or Operational maturity.
- RFC-0031 remains the sole production creator of RFC-0030 capability facts, and
  RFC-0030 evaluation plus RFC-0029 option construction remain separately authorized.
- No push, merge, release, deployment, permission change, cleanup, or external state
  change is authorized.
