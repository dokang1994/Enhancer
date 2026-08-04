# 2026-08-04: Retain Lease Timeout Records In Agent Runtime State

Status: Accepted Decision

## Context

`DurableAgentRuntime.recover` currently observes the injected clock, detects an expired
lease on the latest `EXECUTING` AgentRun, and persists the AgentRun back to `READY`.
That schema-v2 transition discards the expired lease, so later recovery cannot
distinguish timeout reclamation from an ordinary Ready state or repair a derived event.
Exception text, elapsed time, and a later fence are not authoritative substitutes.

Lease expiry is already a canonical AgentRuntime transition. Its timeout fact must be
atomic with that transition rather than stored in an independent sidecar.

## Decision

- Evolve `AgentRuntimeState` and `FileSystemAgentRuntimeStateStore` to schema v3 and add
  an immutable ordered ledger of at most 256 `LeaseTimeoutRecord` values. Earlier runtime
  schemas remain unsupported until a separately accepted migration task.
- A record retains the exact AgentRun, owner, positive fence, issue and expiry times,
  and observation time. Observation must be at or after expiry. Its authoritative event
  occurrence is the lease's retained `expiresAt`, not the later recovery clock.
- `AgentRuntimeState.reclaimExpiredLease` appends exactly one record for the current
  lease while transitioning the same AgentRun from `EXECUTING` to `READY` in one next
  state revision. The filesystem store requires the timeout ledger to retain an exact
  prefix and grow by at most one, and validates that an append corresponds to that
  exact expired Executing-to-Ready transition. Unexpired, missing, or non-executing
  state adds no record.
- The stable authoritative reference is
  `agent-runtime/<goal>/lease-timeout/<agent-run>/<fence>`. Goal-wide monotonic fences
  make every timeout identity unique; changed record reuse, history rewrite/truncation,
  overflow, foreign binding, or invalid observation fails closed.
- Add event-aware `DurableAgentRuntime` recovery through the existing
  `RuntimeEventRecorder` port. After runtime persistence, it derives
  `TIMEOUT_DETECTED` with `RuntimeTimeoutKind.LEASE`, the record expiry occurrence,
  exact retained Work binding, the Work message as causation, producer
  `durable-agent-runtime`, and one typed `LEASE_TIMEOUT` reference. Recovery replays all
  retained bounded timeout records in order, so a missing event or failed publication
  is repairable after later lease/fence or AgentRun progress without another runtime
  revision.
- Legacy event-free runtime construction still persists lease-timeout records but
  publishes no event. The change grants no execution, retry, queue, Tool, cancellation,
  or terminal-state authority.

## Rationale

The runtime state is already the atomic lease and reclamation authority. Retaining the
typed source in that same revision avoids a cross-store ordering gap and preserves the
exact expired lease after the live AgentRun moves to Ready. A bounded append-only ledger
supports deterministic event recovery while respecting existing attempt and fence
history.

## Consequences

Runtime filesystem schema v3 is intentionally incompatible with earlier artifacts
until a migration is separately designed and verified. Event-aware recovery may
republish references for retained timeout records; consumers deduplicate by deterministic
event identity. Automatic post-reclaim execution, lease policy changes, migration,
cleanup, scanning, concrete publication, and supported CLI event composition remain
outside this decision.
