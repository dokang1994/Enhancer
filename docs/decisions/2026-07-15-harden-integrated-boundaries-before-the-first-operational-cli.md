# 2026-07-15: Harden Integrated Boundaries Before The First Operational CLI

Status: Accepted Decision

Context:

- A Gradle 9 compatibility review found that the build relies on Gradle's deprecated automatic JUnit Platform launcher injection.
- Boundary testing found that one interruption-ignoring timed-out Tool can occupy the shared worker, duration values can pass policy validation but fail execution or audit conversion, and persisted envelope metadata is outside the integrity digest.
- RunRecord UTF-8 writing can replace malformed Unicode, startup context loading does not enforce real-root containment or size bounds, and the standard test task depends on a user-profile temporary directory.
- These defects are in Integrated Gate 1 through 4 foundations that Gate 5 would expose through a supported command.

Decision:

- Complete a bounded foundation-hardening task before Gate 5 without changing capability maturity or delivery order.
- Declare the JUnit Platform launcher explicitly and configure a build-local default test temporary directory.
- Isolate Tool invocations so a timed-out invocation cannot starve later work, while retaining bounded structured results and executor shutdown ownership.
- Enforce a timeout domain that is representable in both nanoseconds for execution and positive milliseconds for policy audit records.
- Integrity-protect the complete versioned envelope metadata and payload for Evidence and RunRecords, and use strict UTF-8 encoding for persisted RunRecord strings.
- Apply the Tool boundary's real-path containment, strict decoding, and bounded-read principles to required startup documents.
- Preserve the intentional no-persistence `ReadFileTool` failure for truncated output but classify it as an execution/evidence-capability failure rather than invalid caller input.
- Keep Gate 5 as the sole `Specified - Next` capability and perform no Wrapper major-version upgrade in this task.

Rationale:

The first supported CLI must not operationalize known starvation, audit-integrity, data-preservation, path-containment, or build-compatibility defects. These changes strengthen existing contracts without adding a new product capability or broadening authority.

Consequences:

- Pre-hardening local binary Evidence and RunRecord artifacts are not promised backward compatibility; no released storage format exists yet.
- Timeout validation becomes intentionally stricter at policy construction.
- Startup context loading can reject oversized, malformed, or out-of-root required documents before planning begins.
- Gate 5 remains the next capability task after fresh hardening verification and document synchronization.
