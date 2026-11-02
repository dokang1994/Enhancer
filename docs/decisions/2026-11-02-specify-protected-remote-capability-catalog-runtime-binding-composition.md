# User continuation request on 2026-11-02 into protected remote capability catalog runtime binding composition specification

Status: Accepted Decision

## Context

RFC-0032 is Contract Verified through an uncomposed package-private publisher and one
fixed canonical binding format. RFC-0031 remains the sole production creator of
RFC-0030 remote capability facts, but its constructor still receives an absolute
catalog path and whole-file SHA-256 directly from protected composition that has not
yet been specified.

The completed task named three separate possible continuations: production publisher
composition, runtime binding composition, or the same-invocation RFC-0031/RFC-0030/
RFC-0029 consumer. The user requested continuation on 2026-11-02. Runtime binding
composition is the dependency-safe next boundary because the later consumer must not
invent its own catalog trust inputs, while production publication still requires
separate evidence, installation, and platform-permission authority.

## Decision

Specify RFC-0033 as one provider-neutral read-only protected runtime composition. One
absolute normalized fixed binding path supplied only by protected deployment
composition is the trust-root locator. For each acquisition, the future boundary must
read and canonically validate one bounded no-follow binding snapshot, verify its exact
relationship to the fixed catalog directory and immutable catalog, delegate the exact
path and pin to a fresh RFC-0031 source, and accept a result only after a second exact
binding sample proves the same canonical bytes remained selected across acquisition.

RFC-0031 remains the sole production creator of capability facts. RFC-0033 adds no
alternate catalog parser, capability-facts constructor, Router option, evaluator call,
provider, credential, network, spend, execution, persistence, writer, fallback, cache,
scan, repair, or runtime route. Its only named next consumer is a separately accepted
same-invocation boundary that will pass RFC-0033-acquired facts to RFC-0030 and construct
an RFC-0029 option without persisting or widening authority.

This decision authorizes the documentation-only RFC, architecture and project-document
updates, checkpoint state, fresh verification, and ordinary local GREEN commits required
by repository policy. It does not authorize Java implementation, installed artifacts,
publisher or evidence composition, permission mutation, a production caller, remote
route selection or execution, external state changes, or push.

## Rationale

Reading the fixed binding is the missing trust-input bridge between publication and
evaluation. A fresh, stable sequential observation prevents a request or stale cached
binding from choosing path and pin, while retaining the existing RFC-0031 acquisition
and facts-construction authority. Specifying this bridge before the Router consumer
makes the later caller's provenance complete without prematurely opening provider or
network integration.

## Consequences

- The task is documentation-only and makes no Contract Verified implementation claim.
- The future boundary will fail closed when the binding, selected catalog, or bounded
  before/after observation is unsafe, invalid, unavailable, or changed.
- A rotation after the final sample remains a later observation, not an atomic lock
  shared with the runtime reader; the accepted result proves one stable sequential
  acquisition only.
- Platform permission enforcement, installed configuration, anti-rollback, publisher
  composition, and the same-invocation Router consumer remain separately authorized.
- No provider, credential, network, spend, execution, persistence, MCP, push, release,
  or deployment authority is added.
