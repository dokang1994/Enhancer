# Current Task

## Status

Completed

## Task

Add the third Delivery Gate 7 increment: deterministic delivery-failure isolation and dead-letter capture over the in-process bus. A subscriber handler that throws must be isolated so fan-out continues, recorded as a typed `FAILED` outcome, and captured in an ordered immutable dead-letter record, without any automatic retry, cancellation, ordering, backpressure, threading, persistence, or transport.

## Task ID

gate-7-delivery-failure-dead-letter

## Justified By

- 2026-07-15: Deliver Gate 7 In-Process Messaging As A Deterministic Journal-Replayable Bus

## Context

The Contract Verified `InProcessMessageBus` delivers envelopes synchronously and deterministically with per-subscription idempotency and journal replay, but it has no failure semantics: a subscriber handler that throws aborts fan-out to the remaining subscribers and propagates out of `publish`, and the failure is recorded nowhere even though the idempotency key was already consumed. The Roadmap names retry, cancellation propagation, dead-letter, ordering, and backpressure as the next Gate 7 concerns.

This increment adds only the smallest coherent failure foundation the later retry and cancellation increments build on: failure isolation and dead-letter capture. When a handler throws, the bus records a typed `FAILED` outcome for that subscriber, captures an immutable dead-letter entry with a bounded reason, and continues delivering to the remaining subscribers in registration order. A failed delivery consumes the idempotency key deterministically — it is not automatically re-delivered; re-publishing or replaying the same envelope to the same subscriber reports `DUPLICATE`, invokes no handler, and creates no further dead letter. Automatic retry and re-delivery from the dead-letter record, cancellation propagation, ordering, backpressure, persistence, and IPC transport remain later increments.

## Acceptance Criteria

- Add a `DeliveryStatus.FAILED` kind for a subscriber whose handler threw.
- Isolate a handler failure: when one subscriber's handler throws a `RuntimeException`, the bus records a `FAILED` outcome for that subscriber, continues delivering to the remaining subscribers in registration order, and never lets the exception escape `publish` or `replay`.
- Add an immutable `DeadLetter(destination, subscriberId, envelope, reason)` with a bounded non-blank reason; the bus derives the reason from the exception message, or the exception type when the message is null or blank, and truncates it to the bound.
- Expose the ordered, immutable dead-letter record through `deadLetters()`.
- Make a failed delivery deterministic and terminal for this increment: it consumes the idempotency key, so re-publishing or replaying the same envelope to the same subscriber reports `DUPLICATE`, invokes no handler, and adds no further dead letter.
- Preserve every existing invariant: topic registration order, single-consumer queue, unrouted reporting, the ordered journal, replay determinism, immutable published collections, and unmutated authorization and provenance across every hop.
- Validate all null, blank, and length invariants.
- Add focused RED tests before the new types and behavior exist, classify the failure, then implement the minimum Java 17 change.
- Run focused bus tests, full Gradle regression with `--warning-mode all`, fresh XML inspection, Java 17 `-Xlint:all -Werror`, and `git diff --check`.
- Record the failure handling as Contract Verified only after fresh evidence passes and keep Gate 7 `Specified - Next`.

## Out Of Scope

- Automatic retry, re-delivery, or replay from the dead-letter record
- Cancellation propagation, ordering beyond registration, and backpressure
- Threads, asynchronous scheduling, timers, or concurrency
- Dead-letter or journal persistence and durable recovery across process restart
- IPC transport interface and any local-process or remote adapter
- Bus wiring into the CLI, Agent Loop, or any production path
- Payload content, evidence bodies, chat history, or Tool authority
- Commit, push, PR, merge, release, deployment, or publication

## Approval

Approved by the user on 2026-07-15 through the request to proceed with the next Gate 7 increment ("이어서 진행해줘").

## Allowed Tools

- None: this is a build-time contract increment verified by JUnit; it runs no governed Enhancer run and grants no new Enhancer Tool authority.

## Verification Plan

- Write focused failure-isolation, dead-letter, and idempotent-terminal-failure tests before the new behavior exists.
- Confirm focused compilation fails only because of the intentionally absent `FAILED` kind, `DeadLetter` type, and `deadLetters()` accessor, and classify the failure.
- Implement the minimum immutable failure-handling change.
- Run focused bus tests and inspect fresh XML output.
- Run the complete Gradle suite with `--warning-mode all` and inspect fresh XML counts.
- Run Java 17 production compilation with `-Xlint:all -Werror`.
- Confirm the Roadmap retains exactly one `Specified - Next` gate status marker at Gate 7 and run `git diff --check`.
- Review the final diff and synchronize all affected project documents.

## Implementation Result

- Added `DeliveryStatus.FAILED` and the immutable `DeadLetter(destination, subscriberId, envelope, reason)` under `com.enhancer.bus`.
- Isolated handler failures in `InProcessMessageBus.dispatch`: a `RuntimeException` from one subscriber's handler is caught, recorded as a `FAILED` outcome, captured as a `DeadLetter` with a bounded reason (exception message, or the exception type when the message is null or blank, truncated to 512 characters), and fan-out continues to the remaining subscribers in registration order; the exception never escapes `publish` or `replay`.
- Exposed the ordered immutable dead-letter record through `deadLetters()`.
- Kept a failed delivery deterministic and terminal: the idempotency key is consumed before the handler runs, so re-publishing or replaying the same envelope to the same subscriber reports `DUPLICATE`, invokes no handler, and adds no further dead letter.
- Preserved every prior invariant: topic order, single-consumer queue, unrouted reporting, journal, replay determinism, immutable published collections, and unmutated authorization and provenance.

## Verification

- RED: focused test compilation failed with 8 expected errors naming only the intentionally absent `DeliveryStatus.FAILED` constant, `DeadLetter` type, and `deadLetters()` accessor; no error came from any non-bus file. Classified as aligned missing implementation.
- Focused GREEN: the bus suite passed with `InProcessMessageBusTest` at 10 tests (7 prior plus 3 new) and no skips, failures, or errors, confirmed against fresh XML.
- Full regression: `clean test --warning-mode all` passed 44 suites and 166 tests: 164 passed, 2 existing Windows symbolic-link setup skips, 0 failures, and 0 errors; Gradle emitted no deprecation warning.
- Java 17 production compilation of all 113 sources passed with `-Xlint:all -Werror` and no warning or error.
- Structural verification retained exactly one `Specified - Next` gate status marker at Gate 7; `git diff --check` passed for tracked and newly added files.
- Not run: no production caller wires the bus into the CLI or Agent Loop, so no handler failure has been dead-lettered on a real path; failure isolation, dead-letter capture, and terminal idempotency are proven by the in-process contract tests.

## Next Task

Activate the next Gate 7 increment under separate explicit activation: automatic retry with a bounded attempt policy and re-delivery from the dead-letter record, then cancellation propagation, ordering, and backpressure, and finally the IPC transport interface.
