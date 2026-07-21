# 2026-07-21: Persist Fence-Checked External Effect Outcomes Before AgentRun Retry

Status: Accepted Decision

Context:

- Gate 8 can repeat a WorkItem after interruption or lease expiry, but no durable state
  currently distinguishes an effect that was never attempted from one that may already
  have reached an external system.
- The current runtime already has an exact WorkItem-backed Goal, one executing AgentRun,
  and a persisted owner/fence lease. Retry through another AgentRun is deliberately next
  in the connection sequence and must not arrive before effect ambiguity is explicit.
- Universal exactly-once execution is not available across arbitrary external systems.
  The truthful contract is at-least-once work delivery plus stable effect identity,
  fenced ownership, idempotent records, adapter evidence, and explicit recovery state.

Decision:

- Add a separate schema-v1 external-effect ledger per Goal. It is bounded to 256 ordered
  effects and advances by exactly one persisted revision for each new state change.
- Identify an effect by a stable bounded idempotency key and bind it to the exact Goal,
  AgentRun, WorkItem, bounded operation name, and lowercase SHA-256 semantic operation
  digest. The ledger stores identity and outcome metadata, not external credentials or
  payload content.
- Persist `PREPARED` before returning the effect to a caller that may invoke an external
  adapter. An unresolved prepared record survives restart and is never interpreted as
  safe to repeat automatically.
- Permit exactly four terminal outcomes: `APPLIED`, `DEDUPLICATED`, `COMPENSATED`, and
  `REQUIRES_USER_RECOVERY`. Outcome names report what the owning adapter established;
  the ledger by itself is not evidence of remote state.
- Require the exact currently executing AgentRun and its matching unexpired owner and
  fence token for both preparation and terminal recording. Validate against the durable
  runtime state before any ledger mutation. Stale owner, fence, expiry, or identity
  mismatch fails closed.
- Make exact preparation and exact terminal re-entry idempotent without a revision
  advance. Reject reuse of an idempotency key with different bound data and reject any
  attempt to replace one terminal outcome with another.
- Persist the ledger through a bounded strict-UTF-8 integrity envelope and atomic
  publication. Reject missing, corrupt, oversized, trailing, unsupported, symbolic-link
  root, and non-monotonic state rather than inventing defaults.
- Keep runtime and effect stores as separate atomic boundaries. This increment is
  single-process composition and adds no cross-store transaction, external invocation,
  retry policy, process lock, or power-loss directory-sync claim.

Rationale:

Writing intent before an effect closes the most dangerous retry ambiguity that can be
closed locally: after restart, the system can distinguish definitely unprepared work
from work that may require adapter deduplication, compensation, or human recovery. The
current lease supplies the smallest existing authority boundary for rejecting a delayed
owner. A semantic digest prevents a stable key from silently naming different work.

Consequences:

- A caller can safely re-enter ledger operations after a lost acknowledgement without
  duplicating the record or rewriting its outcome.
- A crash after external application but before terminal recording intentionally leaves
  `PREPARED`; a later controller must not blindly replay it.
- The next AgentRun-retry increment may consult these records, but it still needs its own
  bounded policy, immutable attempt history, and integration evidence.
- An owning Tool or adapter must provide the evidence behind `APPLIED`, `DEDUPLICATED`,
  `COMPENSATED`, or `REQUIRES_USER_RECOVERY`; the ledger does not fabricate that proof.
- Gate 8 remains `Specified - Next`; only this bounded ledger sub-capability may be
  promoted by focused and named integration evidence.
