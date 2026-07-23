# 2026-07-23: Execute External Effects Through A Persist-First Evidence-Bound Adapter Boundary

Status: Accepted Decision

Context:

- The current Goal-scoped external-effect ledger durably records a stable semantic intent
  as `PREPARED` before exposure and one current-owner/fence-checked terminal status, but it
  deliberately invokes no adapter and terminal status alone is not evidence of remote
  state.
- Calling an adapter directly from `DurableExternalEffectLedger` would make a persistence
  boundary own Tool/adapter authority, payload handling, and external failure policy.
- An application executor that records only a transient adapter result would lose the
  evidence needed to explain an exact terminal replay after restart or a lost
  acknowledgement.
- Universal exactly-once execution cannot be promised across arbitrary external systems.
  The safe local contract must expose every interruption window and leave ambiguous work
  recoverable rather than silently repeating it.

Decision:

- Add a separate application-layer external-effect executor around the existing durable
  ledger, an `ExternalEffectAdapter` port, and the existing `EvidenceStore`. The executor
  owns ordering and composition; the ledger continues to own only durable effect state,
  and the adapter continues to own external invocation.
- Bind each prepared intent to a bounded stable adapter identity as well as the existing
  Goal, AgentRun, WorkItem, idempotency key, operation name, and semantic operation digest.
  The adapter supplies a canonical digest of its opaque operation input, and the executor
  must compare it with the request before preparation. Payload content and credentials
  remain adapter-owned and never enter the ledger or evidence by default.
- Upgrade the external-effect ledger schema in the implementation increment rather than
  rewriting schema-v1 artifacts in place. A prepared record has no outcome evidence. A
  terminal record binds the terminal status to the adapter identity, one resolvable
  Evidence Store reference, and the evidence SHA-256. Exact successor and prefix checks
  must prevent evidence removal, replacement, or rebinding.
- Execute a new effect in this order: validate caller, adapter identity, semantic digest,
  current AgentRun owner/fence, and evidence bounds; durably record `PREPARED`; invoke the
  adapter once; require one typed terminal adapter result; durably persist its redacted
  complete outcome evidence; re-check the current owner/fence through the ledger; then
  publish the evidence-bound terminal record. No terminal status is written from prose,
  exceptions, or an unpersisted adapter claim.
- If an exact terminal record already exists, resolve and integrity-check its bound
  evidence and return it without invoking the adapter or advancing the ledger revision.
  If `PREPARED` already existed when the call began, refuse automatic execution. A later
  explicit recovery boundary may inspect, deduplicate, compensate, or request user
  recovery, but the first executor must not reinterpret uncertainty as permission to
  retry.
- Leave the ledger at `PREPARED` when adapter invocation, evidence persistence, terminal
  publication, or the terminal lease check fails. Evidence persisted before a failed
  terminal publication may be orphaned but grants no state transition. Lease expiry or a
  stale fence fails closed even when the adapter may have acted.
- The first implementation consumer is the existing fenced Gate 8 AgentRun execution
  interval. A named real-filesystem integration must connect an executing runtime lease,
  the real ledger and Evidence Store, the executor, and a deterministic adapter, proving
  persist-before-invoke, evidence-before-terminal, terminal restart replay without a
  second invocation, and `PREPARED` recovery after every injected failure prefix. A real
  network, Git, cloud, or production mutation adapter remains Gate 11 work and requires
  its own Tool authority and outbound-data policy.

Alternatives considered:

- Invoke the adapter from `DurableExternalEffectLedger`: rejected because it merges
  persistence and external authority, makes the ledger depend on payload and credential
  handling, and obscures which boundary owns invocation failure.
- Add an application executor but retain only a transient typed adapter result: rejected
  because a terminal record cannot reproduce or validate its evidence after restart, and
  a lost acknowledgement would turn status into an unsupported assertion.
- Add an application executor and bind durable evidence to the terminal record: selected
  because it preserves one owner per responsibility, keeps payload and credentials out of
  durable state, makes exact terminal replay inspectable, and exposes rather than hides
  the unavoidable `PREPARED` ambiguity window.

Rationale:

Persisting intent before invocation prevents an effect from occurring without a durable
warning that it may have occurred. Persisting and binding evidence before terminal state
makes the final status replayable and auditable. Refusing automatic execution from an
already prepared record is the smallest truthful recovery rule until an owning adapter
defines safe inspection, deduplication, or compensation.

Consequences:

- The implementation requires a deliberate external-effect ledger schema revision and
  explicit rejection or migration handling for older artifacts.
- A crash after remote application but before terminal publication remains ambiguous by
  design; it leaves `PREPARED` and blocks automatic AgentRun retry under the existing
  retry policy.
- Evidence must be bounded, redacted, integrity-checked, and resolvable, but it must not
  contain credentials or duplicate the operation payload merely to make the record look
  complete.
- The executor and deterministic adapter tests create no production external authority.
  Each real adapter still requires an accepted Tool/adapter task, policy scope, secret and
  outbound-data review, and adapter-specific recovery evidence.
- Gate 8 remains `Specified - Next`; this decision authorizes only the bounded
  implementation task named in `CURRENT_TASK.md` after this assessment completes.
