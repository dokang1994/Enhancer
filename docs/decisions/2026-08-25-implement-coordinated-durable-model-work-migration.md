# User continuation request on 2026-08-25 into the coordinated durable ModelWork migration implementation

Status: Accepted Decision

## Context

RFC-0018 is Accepted and defines one coordinated durable migration family for
submission manifest v3, Scheduler queue v4, AgentRuntime v5, and the already defined
ModelWork-only process spool v2. The completed golden-wire task names this migration as
the next separately authorized work, and the user requested continuation on 2026-08-25.

The current durable stores retain only legacy `WorkPayload`. A partial version change
could strand a mixed closure or make typed ModelWork appear executable through the
legacy RunRecord v1 path, so the migration must be bounded, stopped-owner, consumer-
first, and fail closed.

## Decision

Authorize a bounded RED-first Dynamic Workflow implementing RFC-0018's coordinated
durable ModelWork migration. The implementation may version submission manifest v3,
Scheduler queue v4, and AgentRuntime v5 together with retained ModelWork process-spool
v2; preserve the exact envelope and complete profile while keeping
`WorkItem.requiredCapability` independent; and preserve all legacy envelope, spool,
and detached-cancellation signing bytes.

Migration operates only on an explicit point-resolved plan naming the complete queue,
runtime, manifest, work/result spool, and ingress closure. The owner must already be
stopped behind a pre-existing maintenance fence outside the named migration roots.
Preflight resolves and validates every source and candidate before the first target
write. Publication is consumer-first, detects source drift, and supports idempotent
crash re-entry without rewriting already-current points.

Legacy work containing `read-file`, including mixed Tool scope and absent input, is
migrated losslessly. Any unprofiled legacy model work is refused during full preflight
with `UNMIGRATABLE_LEGACY_MODEL_WORK` and `PROFILE_REQUIRED`, before candidate or target
writes. Legacy read-file spool v1 bytes remain unchanged; spool v2 applies only to an
already typed `ModelWorkPayload` point.

Production migration execution is limited to implementation surfaces and build/JUnit-
owned temporary fixtures. No terminal-history exception for unprofiled legacy model
work is introduced by this decision.

## Consequences

- Typed ModelWork remains untrusted durable retention only. It cannot enter the current
  Scheduler model executor, child process, admission path, or gateway.
- Model RunRecord v2, exact-task and policy sourcing, RFC-0015/RFC-0016 caller wiring,
  provider and network integration, and new ModelWork submission or receive surfaces
  require separate authority.
- Gate 8 whole-gate maturity does not change without separate implementation and fresh
  verification evidence.
- Verified GREEN increments require ordinary local commits. Push, merge, release,
  deployment, and mutation of real user durable artifacts are not authorized.
