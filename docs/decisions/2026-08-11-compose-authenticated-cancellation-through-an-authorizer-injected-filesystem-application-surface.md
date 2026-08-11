# 2026-08-11: Compose Authenticated Cancellation Through An Authorizer-Injected Filesystem Application Surface

Status: Accepted Decision

## Context

`AuthenticatedCancellationApplication` already owns the exact retained-`CANCEL`,
trusted authorization, terminal AgentRuntime transition, and source-first
`CANCELLATION_APPLIED` event contract. It accepts the abstract runtime store and optional
event recorder, so focused tests can prove the transition, but no supported application
surface constructs the real filesystem collaborators.

Gate 12's accepted interface order is shared application API first and CLI later. A
supported CLI cannot safely invent approval from an actor string, authorization UUID,
envelope producer, reason, or invocation alone. The first composition must therefore
retain an injected trusted authorizer and expose no caller field that can become
authorization.

The repository already has a `runtime -> application` source dependency through
`AgentLoopAgentRunExecution`. Placing the new facade in `com.enhancer.application` would
introduce the reverse dependency when it delegates to runtime types, so the smallest
acyclic composition remains in `com.enhancer.runtime`.

## Decision

- Add `FileSystemAuthenticatedCancellationApplication` as the first supported shared
  application surface. It accepts one explicit AgentRuntime filesystem root, an
  injected `Clock`, and a mandatory injected `ControlRequestAuthorizer`, then delegates
  `apply(goalId, controlMessageId)` unchanged to the existing
  `AuthenticatedCancellationApplication` transition owner.
- Provide an event-free constructor and a separate event-aware constructor that accepts
  one `FileSystemRuntimeEventPublicationConfiguration`. No default authorizer exists,
  and the facade accepts no actor, authorization identity, decision, credential, token,
  producer, reason, or approval flag.
- `FileSystemRuntimeEventPublicationConfiguration` groups exactly one runtime-event
  root, one publication root, and a capacity from 1 through 4096. Omission is
  structurally event-free; presence is all-or-none. The event-aware facade constructs
  `FileSystemRuntimeEventStore -> RuntimeEventRecorder ->
  FileSystemRuntimeEventPublisher` before delegating to the existing owner.
- The unchanged durable order is exact retained request -> trusted authorizer decision
  -> authorization-bound terminal runtime revision -> deterministic event append or
  exact replay -> opaque reference-point publication or exact replay. Runtime
  persistence failure reaches no event. Event or publisher failure leaves the source
  record recoverable, and retained-record replay bypasses the authorizer and advances no
  runtime revision.
- The future Gate 12 CLI/API/editor/Desktop composition is the downstream consumer. It
  must supply an already trusted `ControlRequestAuthorizer` from its own authenticated
  boundary and may pass only the exact Goal/Control identities plus explicit storage
  configuration to this surface. This increment adds no CLI or concrete identity
  provider.

## Rationale

The facade makes the existing real filesystem path reusable without duplicating
authorization or lifecycle semantics. A mandatory injected authorizer preserves the
trust boundary, while the single configuration value prevents partial event
composition. Keeping the facade in `runtime` avoids a source cycle and matches the
package that already owns the transition and filesystem adapters.

## Consequences

- Approved cancellation can be exercised through one supported filesystem composition,
  optionally reaching the existing concrete runtime-event reference publisher.
- Exact replay repairs the existing event/publication prefix without reauthorization;
  an authorizer may still be called again when it returned a decision but the terminal
  runtime revision never persisted.
- Publisher acceptance remains local point acceptance, not consumer delivery or event
  application; downstream consumers deduplicate by deterministic event identity.
- Cancellation still does not dispose the Scheduler queue, signal a process, invoke
  Message Bus cancellation, cancel a Tool or external effect, or implement
  `PAUSE`/`RESUME`.
- Credential issuance, identity-provider adapters, supported CLI authentication,
  cross-store transactions, migration, cleanup, and retention remain separate work.
