# User continuation request on 2026-11-03 into RFC-0033 protected runtime binding implementation

Status: Accepted Decision

## Context

RFC-0033 is Accepted and its specification task is Completed. RFC-0032 owns the sole
fixed canonical installed binding grammar and RFC-0031 remains the sole production
creator of remote candidate capability facts, but no read-only runtime composition
currently connects the fixed binding to that source.

The user requested continuation on 2026-11-03. The completed task names RED-first
RFC-0033 implementation as the next bounded continuation and keeps the later
same-invocation RFC-0030/RFC-0029 consumer, production publication, installation, and
provider execution separate.

## Decision

Implement RFC-0033 RED-first as one provider-neutral read-only source constructed from
one protected absolute normalized fixed binding path. Each acquisition will take a
fresh bounded no-follow canonical first binding sample, construct and invoke one fresh
RFC-0031 source with the exact selected catalog path and pin, and expose that delegated
result only after a second complete sample proves the same canonical binding bytes and
digest remained selected.

The implementation will use RFC-0032's existing binding codec, preserve RFC-0031 as the
sole capability-facts creator, return only closed safe immutable outcomes, and contain
no alternate catalog mapping, scan, cache, fallback, writer, maintenance lock, CLI or
ambient configuration, Router/evaluator consumer, provider, credential, network,
spend, execution, persistence, runtime, or MCP path.

This decision authorizes repository-local Java production and test changes,
architecture/source guards, owning project-document updates, checkpoint state, fresh
verification, and ordinary local GREEN commits required by repository policy. It does
not authorize production construction, installed artifacts, publisher/evidence
composition, deployment or permission changes, the later route-option consumer, any
external effect, or push.

## Rationale

The fixed binding is the missing trust-input bridge between protected publication and
capability-fact acquisition. Reusing its canonical codec avoids a second grammar, while
fresh before/after samples prevent request-selected or stale cached trust inputs from
being exposed as a stable acquisition. Keeping facts creation in RFC-0031 preserves the
existing authority boundary.

## Consequences

- RFC-0033 may become Contract Verified only as an uncomposed read-only source.
- Every failed or changed second sample hides both acquired facts and a delegated
  refusal under one binding-changed-during-acquisition outcome.
- The implementation proves a bounded stable sequential observation only; platform
  permissions, installed configuration, durability, truth, and anti-rollback remain
  separate.
- A production construction source and the same-invocation RFC-0030/RFC-0029 consumer
  require later accepted work.
- No provider, credential, network, spend, execution, persistence, MCP, push, release,
  or deployment authority is added.
