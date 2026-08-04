# 2026-08-04: Apply Authenticated Cancellation As A Terminal AgentRuntime Transition

Status: Accepted Decision

## Context

The durable AgentRuntime ledger already retains exact bound Control envelopes, but the
accepted request-admission decision deliberately treats producer and reason as
diagnostic data. `CANCELLATION_REQUEST_RECORDED` therefore proves durable intent, not
authentication or permission to change execution state. Gate 12 owns the missing
authenticated application boundary, while AgentRuntime owns the Goal, current AgentRun,
lease/fence, and persist-before-exposure transition.

An applied-cancellation event cannot honestly derive from the request alone. The
application must retain an exact positive authorization result and a terminal runtime
transition before event append or publication. It must also recover after publication
failure without calling an authenticator again or changing the source revision.

## Decision

- Introduce a trusted `ControlRequestAuthorizer` port owned by Gate 12 composition. It
  receives the already-retained exact request and returns a typed approved or denied
  decision. Approval binds a canonical authorization identity, bounded actor identity,
  exact Goal identity, exact Control message identity, `CANCEL`, and authorization time.
  Existing envelope producer, reason, transport acceptance, and durable admission are
  never authentication.
- `AuthenticatedCancellationApplication` resolves the named Goal and exact retained
  request. A first application requires a `CANCEL` payload and a matching approved
  authorizer decision; denial, non-CANCEL input, foreign identity/action, or changed
  replay fails before runtime mutation. No credential, secret, or token is persisted.
- Evolve AgentRuntime to schema v4 with at most one immutable
  `CancellationApplicationRecord`. It retains the authorization identity, actor,
  Control message, target current AgentRun, authorization time, and application time.
  The record reference is
  `agent-runtime/<goal>/cancellation/<control-message>`.
- Persist the record atomically with Goal `ACTIVE|RETRY_PENDING -> CANCELLED`. A current
  non-terminal AgentRun transitions to `CANCELLED` and drops any lease; a latest failed
  attempt remains failed when its retry-pending Goal is cancelled. Completed, failed,
  or already differently cancelled state rejects application. The applied state blocks
  later AgentRun, lease, result, retry, and new-control transitions. It does not kill an
  in-flight process, call Message Bus cancellation, dispose a Scheduler queue item,
  cancel a Tool/effect, or create any new authority.
- Exact application replay resolves the retained record before calling the authorizer
  and advances no runtime revision. Changed authorization content for an already
  applied request is not accepted as replay. Runtime schemas v1 through v3 remain
  unsupported; migration is separate work.
- Event-aware application records `CANCELLATION_APPLIED` only after the record-bearing
  runtime revision is durable. It uses application time, exact Work/Goal/AgentRun
  binding, causal Control message identity, producer
  `authenticated-cancellation-application`, and ordered `CONTROL_MESSAGE` plus
  `CONTROL_APPLICATION` authoritative references. Retained-record re-entry repairs a
  missing event or publication failure without reauthorization or another source
  revision.

## Consequences

- Cancellation becomes a typed terminal AgentRuntime fact and stale fenced completion
  fails closed after lease removal, but the current process may continue until its own
  bounded execution boundary returns.
- Scheduler queue disposition remains a separate later connection, so this increment
  does not claim end-to-end cancelled-queue release or dependency semantics.
- `PAUSE`, `RESUME`, credential issuance/login, supported CLI/API/Desktop/editor
  composition, concrete event transport, Tool/effect cancellation, process signalling,
  and earlier-schema migration remain outside this task.
