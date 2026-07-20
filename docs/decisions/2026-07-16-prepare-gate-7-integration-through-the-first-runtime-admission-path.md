# 2026-07-16: Prepare Gate 7 Integration Through The First Runtime Admission Path

Status: Accepted Decision

Context:

- Gate 7 is Contract Verified, but Architecture requires a capability to connect real upstream and downstream collaborators in an integration test before it can be called Integrated.
- Gate 6 already produces a real repository-derived `ApprovedTask` and immutable `WorkspaceSnapshot`; Gate 8 now admits one unchanged Gate 7 work envelope as an immutable `WorkItem`.
- Wiring the bus into the supported CLI merely to exercise delivery would add behavior without a Scheduler consumer, while selecting a concrete IPC adapter would prematurely decide endpoint, serialization, authentication, and threading policy.

Decision:

- Add a small production publisher that derives one `WorkPayload` from a matching `ApprovedTask` and `WorkspaceSnapshot`, constructs one existing versioned envelope from explicit deterministic metadata, and publishes it through `InProcessMessageBus` to an explicit destination.
- Reject task-identity or source-document mismatch before publication. Allowed Tools come only from the repository-derived approved task; the publisher creates no approval or authority.
- Add a production `MessageHandler` adapter that turns one delivered work envelope into one `WorkItem` using an injected work-identity supplier, a bounded required capability, and an injected downstream sink.
- Prove the path in a named integration test using the real Context Reader, snapshot collector, bus, journal/replay behavior, admission adapter, and WorkItem contract.
- Keep Gate 7 at Contract Verified during this implementation task. A separate fresh maturity assessment decides whether the evidence supports gate-level Integrated promotion.

Rationale:

This is the narrowest real vertical connection available now: Gate 6 supplies approved provenance and authority scope, Gate 7 carries and delivers it unchanged, and Gate 8 admits it without widening authority. Explicit metadata and injected boundaries keep the integration deterministic and avoid inventing a Scheduler, production CLI behavior, or transport policy.

Consequences:

- Gate 7 gains named integration evidence with real upstream and downstream production collaborators, but no maturity claim changes automatically.
- The publisher is an in-process application boundary, not a supported entry point or a concrete transport adapter.
- The admission handler does not store, order, execute, retry, or recover work; the dependency-ready single-worker Scheduler queue remains a separate Gate 8 task.
- Any mismatch or invalid envelope fails before downstream admission; bus retry and dead-letter behavior remains unchanged.
