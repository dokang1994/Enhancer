# Current Task

## Status

Completed

## Task

Add the sixth Delivery Gate 7 increment: delivery ordering over the in-process bus. A publication runs to completion before any publication it causes is delivered, so delivery order equals publication order and no subscriber observes an effect before its cause. Re-entrant publications are queued and drained by a single loop rather than dispatched nested, without threads, timers, priorities, competing consumers, backpressure, persistence, or transport.

## Task ID

gate-7-delivery-ordering

## Justified By

- 2026-07-16: Order Delivery By Running Each Publication To Completion Before Its Cascade

## Context

Topic fan-out already follows registration order, the journal already follows publication order, and replay already re-dispatches in journal order, so the ordering the Roadmap names has no content unless it addresses the one real hazard left. That hazard is re-entrant publication: `publish` dispatches synchronously, so a handler that publishes during its own delivery causes a nested dispatch, the child is delivered in full before the parent's fan-out finishes, and every subscriber registered after the publishing one observes the effect before its cause.

This increment replaces nested dispatch with a pending queue and a single drain loop: a top-level `publish` or `replay` drains to exhaustion and returns the whole ordered cascade, while a call made from inside a handler only enqueues and reports `ENQUEUED`. Admission — the cancellation check and the journal append — moves into the drain loop, so the journal's order is the bus's own total delivery order and a cancellation raised mid-cascade refuses work still queued behind it. Backpressure over the now-explicit pending queue, priorities, competing consumers, persistence, and IPC transport remain later increments.

## Acceptance Criteria

- Add a `DeliveryStatus.ENQUEUED` kind for a re-entrant publication accepted for delivery after the current drain completes.
- Add `DeliveryStatus.isScopeLevel()` covering `UNROUTED`, `CANCELLED`, and `ENQUEUED`, and keep `DeliveryOutcome` validating that exactly the scope-level statuses name no subscriber.
- Run each publication to completion: a publication made from inside a handler is queued and delivered only after the current publication's fan-out finishes, so no subscriber observes an effect before its cause.
- Return the whole ordered cascade from the top-level `publish` or `replay` that drained it, so no outcome is lost, and report `ENQUEUED` from the re-entrant call.
- Queue cascaded publications in FIFO publication order.
- Route `publish` and `replay` through the same submission and drain path, distinguishing them only by whether the entry is journaled, so ordering holds identically on both and replay still appends nothing to the journal.
- Move admission into the drain loop: an entry is journaled at the moment it is admitted, and a correlation cancelled during a cascade refuses entries still queued behind it, which are neither delivered nor journaled.
- Keep a fan-out atomic: a cancellation raised during a fan-out does not stop that fan-out.
- Abandon a cascade entirely if an `Error` escapes a drain, leaving no queued entry behind.
- Preserve every existing invariant: topic registration order, single-consumer queue, unrouted reporting, cancellation refusal and its dominance, failure isolation with continued fan-out, bounded retry, dead-letter attempt accounting, explicit re-delivery, journal immutability, replay determinism and idempotency, and unmutated authorization and provenance across every hop.
- Validate all null, blank, and scope-level outcome invariants.
- Add focused RED tests before the new types and behavior exist, classify the failure, then implement the minimum Java 17 change.
- Run focused bus tests, full Gradle regression with `--warning-mode all`, fresh XML inspection, Java 17 `-Xlint:all -Werror`, and `git diff --check`.
- Record delivery ordering as Contract Verified only after fresh evidence passes and keep Gate 7 `Specified - Next`.

## Out Of Scope

- Backpressure, pending-queue bounds, and rejection or blocking when the queue grows
- Priority ordering, per-key partitioning, and competing queue consumers
- Threads, asynchronous scheduling, timers, or concurrency
- Journal, queue, or cancellation-state persistence and durable recovery across process restart
- IPC transport interface and any local-process or remote adapter
- Bus wiring into the CLI, Agent Loop, or any production path
- Payload content, evidence bodies, chat history, or Tool authority
- Commit, push, PR, merge, release, deployment, or publication

## Approval

Approved by the user on 2026-07-16 through the request to continue ("이어서 진행해줘"), activating the Roadmap's named next Gate 7 increment.

## Allowed Tools

- None: this is a build-time contract increment verified by JUnit; it runs no governed Enhancer run and grants no new Enhancer Tool authority.

## Verification Plan

- Write focused run-to-completion, enqueued-reporting, FIFO-cascade, cancel-during-cascade, and scope-level invariant tests before the new behavior exists.
- Confirm focused compilation fails only because of the intentionally absent `ENQUEUED` kind and `isScopeLevel` accessor, and classify the failure.
- Implement the minimum queue-and-drain change.
- Run focused bus tests and inspect fresh XML output.
- Run the complete Gradle suite with `--warning-mode all` and inspect fresh XML counts.
- Run Java 17 production compilation with `-Xlint:all -Werror`.
- Confirm the Roadmap retains exactly one `Specified - Next` gate status marker at Gate 7 and run `git diff --check`.
- Review the final diff and synchronize all affected project documents.

## Implementation Result

- Added `DeliveryStatus.ENQUEUED` and `DeliveryStatus.isScopeLevel()` covering `UNROUTED`, `CANCELLED`, and `ENQUEUED`; `DeliveryOutcome` now validates its subscriberId invariant through that accessor instead of enumerating statuses.
- Added a private `Pending(destination, envelope, journal)` submission record, an `ArrayDeque` pending queue, and a `draining` flag to `InProcessMessageBus`.
- Added `submit`, which queues submissions in publication order and either reports `ENQUEUED` for each (when a drain is already running) or drains.
- Added `drain`, the single loop that admits and dispatches queued submissions to exhaustion and returns the whole ordered cascade; it performs the cancellation check and the journal append, so admission order is the bus's own delivery order, and it clears the queue in a `finally` block so an `Error` abandons the cascade instead of leaking queued entries into a later publication.
- Routed `publish` and `replay` through `submit`, distinguishing them only by the `journal` flag, so ordering holds identically on both and replay still appends nothing to the journal; removed the now-redundant cancellation check from `dispatch`, whose sole caller is the drain loop.
- Left `redeliver` unchanged: it targets exactly one subscription, so a publication from its handler has no fan-out to be nested inside.
- Preserved every prior invariant: topic registration order, single-consumer queue, unrouted reporting, cancellation refusal and dominance, failure isolation with continued fan-out, bounded retry, dead-letter attempt accounting, explicit re-delivery, journal immutability, replay determinism and idempotency, and unmutated authorization and provenance.

## Verification

- RED, first pass: focused test compilation failed with 8 expected errors naming only the intentionally absent `DeliveryStatus.ENQUEUED` constant and `isScopeLevel()` accessor; production compilation passed and no error came from any non-bus file.
- RED, second pass: because the compile failure hid whether the ordering hazard was real, only the two absent symbols were added so the suite could run. Three focused tests then failed behaviourally against the defect itself — `deliversACascadeOnlyAfterTheCurrentFanOutCompletes` and `refusesAQueuedPublicationCancelledDuringTheCascade` observed `[first, child, second]` where `[first, second, child]` was required, proving a cascaded child really was delivered inside its parent's fan-out, and `reportsAReEntrantPublicationAsEnqueued` observed `DELIVERED` where `ENQUEUED` was required. Classified as aligned missing implementation.
- Focused GREEN: the bus suite passed with `InProcessMessageBusTest` at 25 tests (20 prior plus 5 new) and `MessageEnvelopeTest` at 4, with no skips, failures, or errors, confirmed against fresh XML.
- Full regression: `clean test --warning-mode all` passed 44 suites and 181 tests: 179 passed, 2 existing Windows symbolic-link setup skips, 0 failures, and 0 errors; Gradle emitted no deprecation warning.
- Java 17 production compilation of all 114 sources passed with `-Xlint:all -Werror` and no warning or error.
- Structural verification retained exactly one `Specified - Next` gate status marker at Gate 7; `git diff --check` passed for tracked and newly added files.
- Not run: no production caller wires the bus into the CLI or Agent Loop, so no cascade has been ordered on a real path; run-to-completion, FIFO cascade order, admission-order journaling, and mid-cascade cancellation are proven by the in-process contract tests.

## Next Task

Activate the next Gate 7 increment under separate explicit activation: backpressure over the now-explicit unbounded pending queue, and finally the IPC transport interface.
