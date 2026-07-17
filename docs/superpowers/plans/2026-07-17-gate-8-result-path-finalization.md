# Gate 8 RunRecord-Backed Result Path Finalization Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add one durable, idempotent `DurableAgentRunFinalizer` that connects a resolved RunRecord to the terminal AgentRun/Goal state and the matching queue disposition, with autonomous recovery of the post-terminal suffix.

**Architecture:** A new coordinator type in `com.enhancer.runtime` composes the existing durable queue, `AgentRuntimeStateStore`, and `RunRecordStore`. `finalizeAgentRun` drives resolve RunRecord → runtime terminal (`recordResult`) → queue disposition, each step guarded by observed store state; `recoverFinalization` applies only the queue disposition from an already-terminal runtime, needing no reference. No new store, no schema change.

**Tech Stack:** Java 17, JUnit 5, Gradle (via `scripts/gradle.ps1`), no new dependencies.

## Global Constraints

- Java 17; production compiles clean under `-Xlint:all -Werror`.
- No new durable store and no schema change; the finalizer only reads/writes through existing durable wrappers.
- The queue disposition is derived from the **runtime terminal status** (`COMPLETED → completeActiveVerified`, `FAILED → failActive`), never re-derived from the RunRecord, so the two stores cannot diverge.
- `verificationStatus` and task binding are read from the resolved RunRecord; `ApprovedTask` has no source SHA, so binding is `taskId` + `sourceDocument`.
- Fail closed: a missing/corrupt RunRecord or a task/document mismatch records no disposition and leaves the run `AWAITING_VERIFICATION`.
- The forward method is named `finalizeAgentRun` (not `finalize`, to avoid overloading `Object.finalize`).
- Per `.ai/workflow.md:27`, do not commit or push without explicit user authorization. Commit steps are prepared but run only on the user's go-ahead.
- Classify every RED before implementing (a missing symbol is valid RED evidence).

**Build commands (PowerShell shell):**
- Single suite: `.\scripts\gradle.ps1 --no-daemon cleanTest test --tests 'com.enhancer.runtime.DurableAgentRunFinalizerTest'`
- Package: `.\scripts\gradle.ps1 --no-daemon cleanTest test --tests 'com.enhancer.runtime.*'`
- Full regression: `.\scripts\gradle.ps1 --no-daemon clean test --warning-mode all`

---

## File Structure

- Create: `src/main/java/com/enhancer/runtime/DurableAgentRunFinalizer.java` — the coordinator.
- Create: `src/test/java/com/enhancer/runtime/DurableAgentRunFinalizerTest.java` — behavior, recovery, and guards using real filesystem stores under `@TempDir`.
- Modify docs (Task 4): `ARCHITECTURE.md`, `.ai/architecture.md`, `DECISION_LOG.md`, `CURRENT_TASK.md`, `PROJECT_STATE.md`, `SESSION_HANDOFF.md`, `CHANGELOG.md`.

---

### Task 1: Forward finalize path (verified and failed)

**Files:**
- Create: `src/main/java/com/enhancer/runtime/DurableAgentRunFinalizer.java`
- Create: `src/test/java/com/enhancer/runtime/DurableAgentRunFinalizerTest.java`

**Interfaces:**
- Consumes: `DurableSingleWorkerSchedulerQueue` (`activeWork`, `completeActiveVerified`, `failActive`, `claimNext`), `AgentRuntimeStateStore`, `DurableAgentRuntime` (`recover`, `agentRun`, `recordResult`), `RunRecordStore` (`resolve`), `WorkItemDisposition`, `ResultPayload`, `MessageEnvelope`, `VerificationStatus`.
- Produces: `WorkItemDisposition finalizeAgentRun(String goalId, String agentRunId, String runRecordReference) throws IOException`.

- [ ] **Step 1: Write the failing test (create the test class with shared helpers)**

`src/test/java/com/enhancer/runtime/DurableAgentRunFinalizerTest.java`:

```java
package com.enhancer.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.enhancer.bus.MessageEnvelope;
import com.enhancer.bus.WorkPayload;
import com.enhancer.kernel.VerificationDecision;
import com.enhancer.kernel.VerificationCode;
import com.enhancer.kernel.VerificationStatus;
import com.enhancer.loop.AgentLoopStopReason;
import com.enhancer.loop.ApprovedTask;
import com.enhancer.run.FileSystemRunRecordStore;
import com.enhancer.run.PolicyDecision;
import com.enhancer.run.PolicyDecisionStatus;
import com.enhancer.run.RunRecord;
import com.enhancer.tool.ToolFailureCode;
import com.enhancer.tool.ToolRequest;
import com.enhancer.tool.ToolResult;
import com.enhancer.tool.ToolResultStatus;
import com.enhancer.tool.VerificationEvidence;
import com.enhancer.workspace.ApprovedTaskRevision;
import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.nio.file.Path;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.Test;

class DurableAgentRunFinalizerTest {
    private static final String QUEUE_ID = "00000000-0000-0000-0000-000000000401";
    private static final String GOAL_ID = "00000000-0000-0000-0000-000000000402";
    private static final String AGENT_RUN_ID = "00000000-0000-0000-0000-000000000403";
    private static final String WORK_ID = "00000000-0000-0000-0000-000000000411";
    private static final String DEP_ID = "00000000-0000-0000-0000-000000000412";
    private static final String OWNER_ID = "00000000-0000-0000-0000-000000000421";
    private static final String TASK_ID = "gate-8-result-path-finalization";
    private static final Clock CLOCK =
            Clock.fixed(Instant.parse("2026-07-17T12:00:00Z"), ZoneOffset.UTC);

    @TempDir
    Path tempDir;

    @Test
    void verifiedOutcomeCompletesRuntimeAndReleasesDependent() throws Exception {
        Setup s = awaitingVerification(true);
        String reference = persistRunRecord(s, true);

        DurableAgentRunFinalizer finalizer = finalizer(s);
        WorkItemDisposition disposition =
                finalizer.finalizeAgentRun(GOAL_ID, AGENT_RUN_ID, reference);

        assertEquals(WorkItemDisposition.VERIFIED_COMPLETED, disposition);
        DurableAgentRuntime runtime =
                DurableAgentRuntime.recover(GOAL_ID, s.runtimeStore, CLOCK);
        assertEquals(RuntimeAgentRunStatus.COMPLETED,
                runtime.agentRun().orElseThrow().status());
        DurableSingleWorkerSchedulerQueue queue =
                DurableSingleWorkerSchedulerQueue.recover(QUEUE_ID, s.queueStore);
        assertEquals(Set.of(WORK_ID), queue.completedWorkItemIds());
        assertEquals(DEP_ID, queue.claimNext().orElseThrow().workItemId());
    }

    @Test
    void failedOutcomeFailsRuntimeAndBlocksDependent() throws Exception {
        Setup s = awaitingVerification(true);
        String reference = persistRunRecord(s, false);

        DurableAgentRunFinalizer finalizer = finalizer(s);
        WorkItemDisposition disposition =
                finalizer.finalizeAgentRun(GOAL_ID, AGENT_RUN_ID, reference);

        assertEquals(WorkItemDisposition.FAILED, disposition);
        DurableAgentRuntime runtime =
                DurableAgentRuntime.recover(GOAL_ID, s.runtimeStore, CLOCK);
        assertEquals(RuntimeAgentRunStatus.FAILED,
                runtime.agentRun().orElseThrow().status());
        DurableSingleWorkerSchedulerQueue queue =
                DurableSingleWorkerSchedulerQueue.recover(QUEUE_ID, s.queueStore);
        assertEquals(Set.of(WORK_ID), queue.failedWorkItemIds());
        assertTrue(queue.claimNext().isEmpty());
    }

    // ---- shared helpers ----

    private DurableAgentRunFinalizer finalizer(Setup s) {
        return new DurableAgentRunFinalizer(
                DurableSingleWorkerSchedulerQueue.recoverQuietly(QUEUE_ID, s.queueStore),
                s.runtimeStore,
                s.runRecordStore,
                CLOCK);
    }

    private Setup awaitingVerification(boolean withDependent) throws IOException {
        FileSystemSchedulerQueueStore queueStore =
                new FileSystemSchedulerQueueStore(tempDir.resolve("queue"));
        FileSystemAgentRuntimeStateStore runtimeStore =
                new FileSystemAgentRuntimeStateStore(tempDir.resolve("runtime"));
        FileSystemRunRecordStore runRecordStore =
                new FileSystemRunRecordStore(tempDir.resolve("records"));

        DurableSingleWorkerSchedulerQueue queue =
                DurableSingleWorkerSchedulerQueue.create(QUEUE_ID, 8, queueStore);
        WorkItem work = workItem(WORK_ID);
        queue.enqueue(new QueuedWork(work, List.of()));
        if (withDependent) {
            queue.enqueue(new QueuedWork(workItem(DEP_ID), List.of(WORK_ID)));
        }

        DurableAgentRunDispatcher dispatcher =
                new DurableAgentRunDispatcher(queue, runtimeStore, CLOCK);
        AgentRunDispatch dispatch = dispatcher.claimAndLease(
                GOAL_ID, AGENT_RUN_ID, OWNER_ID, Duration.ofMinutes(5)).orElseThrow();

        DurableAgentRuntime runtime =
                DurableAgentRuntime.recover(GOAL_ID, runtimeStore, CLOCK);
        runtime.completeExecution(
                AGENT_RUN_ID, OWNER_ID, dispatch.lease().fenceToken());

        return new Setup(queueStore, runtimeStore, runRecordStore, work);
    }

    private String persistRunRecord(Setup s, boolean verified) throws IOException {
        return s.runRecordStore.persist(runRecord(verified)).reference();
    }

    private RunRecord runRecord(boolean verified) {
        return new RunRecord(
                "logical-run-finalizer-1",
                Instant.parse("2026-07-17T11:00:00Z"),
                new ApprovedTask(
                        TASK_ID,
                        "Finalize the Gate 8 result path",
                        "Approved by test owner",
                        Set.of("read-file"),
                        "CURRENT_TASK.md"),
                new ToolRequest("read-file", "correlation-1", Map.of("path", "target.txt")),
                new PolicyDecision(
                        PolicyDecisionStatus.ALLOWED,
                        "C:/project",
                        Set.of("read-file"),
                        Set.of(),
                        4096,
                        1000),
                verified ? success() : success(),
                Optional.of("a".repeat(64)),
                verified
                        ? VerificationDecision.verified("content matched")
                        : VerificationDecision.rejected(
                                VerificationCode.CONTENT_MISMATCH, "content differed"),
                1,
                AgentLoopStopReason.AWAITING_VERIFICATION,
                verified
                        ? AgentLoopStopReason.COMPLETED
                        : AgentLoopStopReason.AWAITING_VERIFICATION);
    }

    private ToolResult success() {
        return new ToolResult(
                "read-file",
                ToolResultStatus.SUCCESS,
                OptionalInt.empty(),
                VerificationEvidence.capture("read succeeded", "content", Optional.empty()));
    }

    private static WorkItem workItem(String workItemId) {
        ApprovedTaskRevision revision = new ApprovedTaskRevision(
                TASK_ID, "CURRENT_TASK.md", "a".repeat(64));
        MessageEnvelope envelope = new MessageEnvelope(
                incrementUuid(workItemId),
                "correlation-finalizer-1",
                Optional.empty(),
                "logical-run-finalizer-1",
                "finalizer-test",
                Instant.parse("2026-07-17T10:00:00Z"),
                new WorkPayload(revision, "b".repeat(64), Set.of("read-file")));
        return new WorkItem(workItemId, "read-file-worker", envelope);
    }

    private static String incrementUuid(String workItemId) {
        long suffix = Long.parseLong(workItemId.substring(workItemId.length() - 12));
        return String.format("00000000-0000-0000-0002-%012d", suffix);
    }

    private record Setup(
            FileSystemSchedulerQueueStore queueStore,
            FileSystemAgentRuntimeStateStore runtimeStore,
            FileSystemRunRecordStore runRecordStore,
            WorkItem work) {
    }
}
```

Note: `DurableSingleWorkerSchedulerQueue.recoverQuietly` does not exist yet — replace that helper call with `DurableSingleWorkerSchedulerQueue.recover(QUEUE_ID, s.queueStore)` wrapped in a try/catch that rethrows as `IllegalStateException`, OR make `finalizer(...)` declare `throws IOException`. Use the latter: change `private DurableAgentRunFinalizer finalizer(Setup s)` to `throws IOException` and call `DurableSingleWorkerSchedulerQueue.recover(QUEUE_ID, s.queueStore)` directly.

- [ ] **Step 2: Run test to verify it fails**

Run: `.\scripts\gradle.ps1 --no-daemon cleanTest test --tests 'com.enhancer.runtime.DurableAgentRunFinalizerTest'`
Expected: compilation failure — `DurableAgentRunFinalizer` does not exist. Classify: aligned missing implementation.

- [ ] **Step 3: Implement the forward path**

`src/main/java/com/enhancer/runtime/DurableAgentRunFinalizer.java`:

```java
package com.enhancer.runtime;

import com.enhancer.bus.MessageEnvelope;
import com.enhancer.bus.ResultPayload;
import com.enhancer.kernel.VerificationStatus;
import com.enhancer.run.ResolvedRunRecord;
import com.enhancer.run.RunRecord;
import com.enhancer.run.RunRecordStore;
import com.enhancer.workspace.ApprovedTaskRevision;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Connects a resolved RunRecord to the terminal AgentRun/Goal state and the matching Scheduler
 * queue disposition, in a recoverable, idempotent order. Queue and runtime remain separate durable
 * boundaries; no cross-store transaction is claimed. Consumer: the future Scheduler worker/driver.
 */
public final class DurableAgentRunFinalizer {
    private final DurableSingleWorkerSchedulerQueue queue;
    private final AgentRuntimeStateStore runtimeStore;
    private final RunRecordStore runRecordStore;
    private final Clock clock;

    public DurableAgentRunFinalizer(
            DurableSingleWorkerSchedulerQueue queue,
            AgentRuntimeStateStore runtimeStore,
            RunRecordStore runRecordStore,
            Clock clock) {
        this.queue = Objects.requireNonNull(queue, "queue must not be null");
        this.runtimeStore =
                Objects.requireNonNull(runtimeStore, "runtimeStore must not be null");
        this.runRecordStore =
                Objects.requireNonNull(runRecordStore, "runRecordStore must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    public WorkItemDisposition finalizeAgentRun(
            String goalId,
            String agentRunId,
            String runRecordReference) throws IOException {
        String canonicalAgentRunId =
                RuntimeIdentity.canonicalUuid(agentRunId, "agentRunId");
        Objects.requireNonNull(
                runRecordReference, "runRecordReference must not be null");
        DurableAgentRuntime runtime =
                DurableAgentRuntime.recover(goalId, runtimeStore, clock);
        RuntimeAgentRun run = requireRun(runtime, canonicalAgentRunId);
        WorkItem workItem = runtime.goal().workItem();

        switch (run.status()) {
            case AWAITING_VERIFICATION -> {
                ResolvedRunRecord resolved =
                        runRecordStore.resolve(runRecordReference);
                requireBinding(resolved.record(), workItem);
                VerificationStatus status = resolved.record().verification().status();
                MessageEnvelope result = buildResultEnvelope(
                        workItem, canonicalAgentRunId, runRecordReference, status);
                runtime.recordResult(canonicalAgentRunId, result);
            }
            case COMPLETED, FAILED ->
                    assertStoredResultReference(run, runRecordReference);
            default -> throw new IllegalStateException(
                    "AgentRun has not acknowledged execution");
        }
        return applyQueueDisposition(
                workItem.workItemId(),
                runtime.agentRun().orElseThrow().status());
    }

    private RuntimeAgentRun requireRun(
            DurableAgentRuntime runtime,
            String agentRunId) {
        RuntimeAgentRun run = runtime.agentRun().orElseThrow(() ->
                new IllegalStateException("no AgentRun exists"));
        if (!run.agentRunId().equals(agentRunId)) {
            throw new IllegalArgumentException(
                    "agentRunId does not match the Goal's AgentRun");
        }
        return run;
    }

    private void requireBinding(RunRecord record, WorkItem workItem) {
        ApprovedTaskRevision revision = workItem.taskRevision();
        if (!record.approvedTask().taskId().equals(revision.taskId())
                || !record.approvedTask().sourceDocument()
                        .equals(revision.sourceDocument())) {
            throw new IllegalArgumentException(
                    "RunRecord approved task does not match the Goal work");
        }
    }

    private void assertStoredResultReference(
            RuntimeAgentRun run,
            String runRecordReference) {
        MessageEnvelope stored = run.resultMessage().orElseThrow(() ->
                new IllegalStateException("terminal AgentRun has no result"));
        ResultPayload payload = (ResultPayload) stored.payload();
        if (!payload.runRecordReference().equals(runRecordReference)) {
            throw new IllegalStateException(
                    "terminal AgentRun was finalized with a different RunRecord");
        }
    }

    private MessageEnvelope buildResultEnvelope(
            WorkItem workItem,
            String agentRunId,
            String runRecordReference,
            VerificationStatus status) {
        MessageEnvelope work = workItem.workMessage();
        String messageId = UUID.nameUUIDFromBytes(
                ("agent-run-result:" + agentRunId).getBytes(StandardCharsets.UTF_8))
                .toString();
        return new MessageEnvelope(
                messageId,
                work.correlationId(),
                Optional.of(work.messageId()),
                work.logicalRunId(),
                "agent-run-finalizer",
                clock.instant(),
                new ResultPayload(
                        workItem.taskRevision().taskId(),
                        runRecordReference,
                        status));
    }

    private WorkItemDisposition applyQueueDisposition(
            String workItemId,
            RuntimeAgentRunStatus terminalStatus) throws IOException {
        WorkItemDisposition disposition =
                terminalStatus == RuntimeAgentRunStatus.COMPLETED
                        ? WorkItemDisposition.VERIFIED_COMPLETED
                        : WorkItemDisposition.FAILED;
        Optional<WorkItem> active = queue.activeWork();
        if (active.isPresent() && active.orElseThrow().workItemId().equals(workItemId)) {
            if (disposition == WorkItemDisposition.VERIFIED_COMPLETED) {
                queue.completeActiveVerified(workItemId);
            } else {
                queue.failActive(workItemId);
            }
        }
        return disposition;
    }
}
```

Adjust the `finalizer(...)` test helper to `throws IOException` and call `DurableSingleWorkerSchedulerQueue.recover(QUEUE_ID, s.queueStore)` directly (replacing the `recoverQuietly` placeholder from Step 1).

- [ ] **Step 4: Run test to verify it passes**

Run: `.\scripts\gradle.ps1 --no-daemon cleanTest test --tests 'com.enhancer.runtime.DurableAgentRunFinalizerTest'`
Expected: PASS (both verified and failed scenarios).

- [ ] **Step 5: Commit** (only with user authorization)

```bash
git add src/main/java/com/enhancer/runtime/DurableAgentRunFinalizer.java src/test/java/com/enhancer/runtime/DurableAgentRunFinalizerTest.java
git commit -m "feat: add Gate 8 result-path finalizer forward path"
```

---

### Task 2: Autonomous recovery and idempotent re-finalize

**Files:**
- Modify: `src/main/java/com/enhancer/runtime/DurableAgentRunFinalizer.java`
- Modify: `src/test/java/com/enhancer/runtime/DurableAgentRunFinalizerTest.java`

**Interfaces:**
- Produces: `Optional<WorkItemDisposition> recoverFinalization(String goalId) throws IOException`.

- [ ] **Step 1: Write the failing tests**

Add a terminal-result helper and two tests to `DurableAgentRunFinalizerTest.java`:

```java
    @Test
    void recoverFinalizationAppliesDispositionFromTerminalRuntimeWithoutReference()
            throws Exception {
        Setup s = awaitingVerification(true);
        String reference = persistRunRecord(s, true);
        // Simulate a crash after the runtime terminal transition, before the queue disposition.
        DurableAgentRuntime runtime =
                DurableAgentRuntime.recover(GOAL_ID, s.runtimeStore, CLOCK);
        runtime.recordResult(
                AGENT_RUN_ID,
                terminalResultEnvelope(s.work, reference, VerificationStatus.VERIFIED));

        DurableSingleWorkerSchedulerQueue queue =
                DurableSingleWorkerSchedulerQueue.recover(QUEUE_ID, s.queueStore);
        DurableAgentRunFinalizer finalizer = new DurableAgentRunFinalizer(
                queue, s.runtimeStore, s.runRecordStore, CLOCK);

        assertEquals(Optional.of(WorkItemDisposition.VERIFIED_COMPLETED),
                finalizer.recoverFinalization(GOAL_ID));
        assertEquals(DEP_ID, queue.claimNext().orElseThrow().workItemId());
    }

    @Test
    void reFinalizeAfterTerminalIsIdempotent() throws Exception {
        Setup s = awaitingVerification(false);
        String reference = persistRunRecord(s, true);
        finalizer(s).finalizeAgentRun(GOAL_ID, AGENT_RUN_ID, reference);

        // A second finalize with the same reference must not throw and must not change state.
        WorkItemDisposition disposition =
                finalizer(s).finalizeAgentRun(GOAL_ID, AGENT_RUN_ID, reference);
        assertEquals(WorkItemDisposition.VERIFIED_COMPLETED, disposition);
        DurableSingleWorkerSchedulerQueue queue =
                DurableSingleWorkerSchedulerQueue.recover(QUEUE_ID, s.queueStore);
        assertEquals(Set.of(WORK_ID), queue.completedWorkItemIds());

        // recoverFinalization on a fully finalized run is also a no-op disposition report.
        assertEquals(Optional.of(WorkItemDisposition.VERIFIED_COMPLETED),
                finalizer(s).recoverFinalization(GOAL_ID));
    }

    private MessageEnvelope terminalResultEnvelope(
            WorkItem work, String reference, VerificationStatus status) {
        MessageEnvelope workMessage = work.workMessage();
        return new MessageEnvelope(
                java.util.UUID.nameUUIDFromBytes(
                        ("agent-run-result:" + AGENT_RUN_ID)
                                .getBytes(java.nio.charset.StandardCharsets.UTF_8)).toString(),
                workMessage.correlationId(),
                Optional.of(workMessage.messageId()),
                workMessage.logicalRunId(),
                "agent-run-finalizer",
                Instant.parse("2026-07-17T12:00:00Z"),
                new com.enhancer.bus.ResultPayload(TASK_ID, reference, status));
    }
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `.\scripts\gradle.ps1 --no-daemon cleanTest test --tests 'com.enhancer.runtime.DurableAgentRunFinalizerTest'`
Expected: compilation failure — `recoverFinalization` does not exist. Classify: aligned missing implementation.

- [ ] **Step 3: Implement `recoverFinalization`**

Add to `DurableAgentRunFinalizer`:

```java
    public Optional<WorkItemDisposition> recoverFinalization(String goalId)
            throws IOException {
        DurableAgentRuntime runtime =
                DurableAgentRuntime.recover(goalId, runtimeStore, clock);
        RuntimeAgentRun run = runtime.agentRun().orElseThrow(() ->
                new IllegalStateException("no AgentRun exists"));
        if (!run.status().isTerminal()) {
            return Optional.empty();
        }
        return Optional.of(applyQueueDisposition(
                runtime.goal().workItem().workItemId(),
                run.status()));
    }
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `.\scripts\gradle.ps1 --no-daemon cleanTest test --tests 'com.enhancer.runtime.DurableAgentRunFinalizerTest'`
Expected: PASS.

- [ ] **Step 5: Commit** (only with user authorization)

```bash
git add src/main/java/com/enhancer/runtime/DurableAgentRunFinalizer.java src/test/java/com/enhancer/runtime/DurableAgentRunFinalizerTest.java
git commit -m "feat: add autonomous recovery to the result-path finalizer"
```

---

### Task 3: Fail-closed and binding guards

**Files:**
- Modify: `src/test/java/com/enhancer/runtime/DurableAgentRunFinalizerTest.java` (guards already implemented in Task 1; this task pins them)

**Interfaces:** none new — this task adds coverage for the guards implemented in Task 1 (`requireBinding`, fail-closed resolve, wrong-status, terminal reference mismatch).

- [ ] **Step 1: Write the failing tests**

Add to `DurableAgentRunFinalizerTest.java`:

```java
    @Test
    void missingRunRecordFailsClosedAndLeavesRunRecoverable() throws Exception {
        Setup s = awaitingVerification(false);
        String missing = "run-record/00000000-0000-0000-0000-0000000009ff";

        assertThrows(com.enhancer.run.MissingRunRecordException.class, () ->
                finalizer(s).finalizeAgentRun(GOAL_ID, AGENT_RUN_ID, missing));

        DurableAgentRuntime runtime =
                DurableAgentRuntime.recover(GOAL_ID, s.runtimeStore, CLOCK);
        assertEquals(RuntimeAgentRunStatus.AWAITING_VERIFICATION,
                runtime.agentRun().orElseThrow().status());
        DurableSingleWorkerSchedulerQueue queue =
                DurableSingleWorkerSchedulerQueue.recover(QUEUE_ID, s.queueStore);
        assertSame(WORK_ID, queue.activeWork().orElseThrow().workItemId());
    }

    @Test
    void runRecordForADifferentTaskIsRejected() throws Exception {
        Setup s = awaitingVerification(false);
        String reference = s.runRecordStore.persist(new RunRecord(
                "logical-run-finalizer-1",
                Instant.parse("2026-07-17T11:00:00Z"),
                new ApprovedTask(
                        "a-different-task",
                        "Different task",
                        "Approved by test owner",
                        Set.of("read-file"),
                        "CURRENT_TASK.md"),
                new ToolRequest("read-file", "correlation-1", Map.of("path", "t.txt")),
                new PolicyDecision(PolicyDecisionStatus.ALLOWED, "C:/project",
                        Set.of("read-file"), Set.of(), 4096, 1000),
                success(),
                Optional.of("a".repeat(64)),
                VerificationDecision.verified("content matched"),
                1,
                AgentLoopStopReason.AWAITING_VERIFICATION,
                AgentLoopStopReason.COMPLETED)).reference();

        assertThrows(IllegalArgumentException.class, () ->
                finalizer(s).finalizeAgentRun(GOAL_ID, AGENT_RUN_ID, reference));
    }

    @Test
    void reFinalizeWithDifferentReferenceIsRejected() throws Exception {
        Setup s = awaitingVerification(false);
        String first = persistRunRecord(s, true);
        String second = persistRunRecord(s, true);
        finalizer(s).finalizeAgentRun(GOAL_ID, AGENT_RUN_ID, first);

        assertThrows(IllegalStateException.class, () ->
                finalizer(s).finalizeAgentRun(GOAL_ID, AGENT_RUN_ID, second));
    }

    @Test
    void finalizeBeforeExecutionAcknowledgementIsRejected() throws Exception {
        // Reach EXECUTING (leased) but do NOT completeExecution.
        FileSystemSchedulerQueueStore queueStore =
                new FileSystemSchedulerQueueStore(tempDir.resolve("queue"));
        FileSystemAgentRuntimeStateStore runtimeStore =
                new FileSystemAgentRuntimeStateStore(tempDir.resolve("runtime"));
        FileSystemRunRecordStore runRecordStore =
                new FileSystemRunRecordStore(tempDir.resolve("records"));
        DurableSingleWorkerSchedulerQueue queue =
                DurableSingleWorkerSchedulerQueue.create(QUEUE_ID, 8, queueStore);
        queue.enqueue(new QueuedWork(workItem(WORK_ID), List.of()));
        new DurableAgentRunDispatcher(queue, runtimeStore, CLOCK)
                .claimAndLease(GOAL_ID, AGENT_RUN_ID, OWNER_ID, Duration.ofMinutes(5))
                .orElseThrow();
        String reference =
                runRecordStore.persist(runRecord(true)).reference();

        DurableAgentRunFinalizer finalizer = new DurableAgentRunFinalizer(
                DurableSingleWorkerSchedulerQueue.recover(QUEUE_ID, queueStore),
                runtimeStore, runRecordStore, CLOCK);
        assertThrows(IllegalStateException.class, () ->
                finalizer.finalizeAgentRun(GOAL_ID, AGENT_RUN_ID, reference));
    }
```

- [ ] **Step 2: Run tests to verify they pass (guards already implemented in Task 1)**

Run: `.\scripts\gradle.ps1 --no-daemon cleanTest test --tests 'com.enhancer.runtime.DurableAgentRunFinalizerTest'`
Expected: PASS. If any guard test fails, the guard logic in `DurableAgentRunFinalizer` (Task 1 Step 3) is the code under test — fix it there, not the test. (These assertions target `requireBinding`, `resolve` fail-closed propagation, the `default -> throw` branch, and `assertStoredResultReference`, all already present.)

- [ ] **Step 3: Commit** (only with user authorization)

```bash
git add src/test/java/com/enhancer/runtime/DurableAgentRunFinalizerTest.java
git commit -m "test: pin result-path finalizer fail-closed and binding guards"
```

---

### Task 4: Documentation sync, accepted decision, and full regression

**Files:**
- Modify: `DECISION_LOG.md`, `ARCHITECTURE.md`, `.ai/architecture.md`, `CURRENT_TASK.md`, `PROJECT_STATE.md`, `SESSION_HANDOFF.md`, `CHANGELOG.md`

**Interfaces:** none (documentation only). Satisfies the Document Driven Development workflow.

- [ ] **Step 1: Record the accepted decision in `DECISION_LOG.md`**

Add a dated `### 2026-07-17: ...` accepted-decision entry (matching the file's heading/status grammar) covering: RunRecord is resolved input (not persisted here); the idempotent ordered coordinator (resolve → runtime terminal → queue disposition); disposition derived from runtime terminal status (single source of truth); `taskId`+`sourceDocument` binding (no SHA on `ApprovedTask`); fail-closed on missing/corrupt RunRecord; two entry points with autonomous post-terminal recovery; and the pre-terminal recovery window deferred to the connection-#3 worker/driver. Out of scope: worker/Tool execution, retry, automatic failure propagation.

- [ ] **Step 2: Update `ARCHITECTURE.md` and `.ai/architecture.md`**

In `ARCHITECTURE.md`, update the Gate 8 connection table (row 2, "RunRecord-backed result finalization") to mark it Contract Verified and add a short paragraph describing `DurableAgentRunFinalizer`'s ordered, idempotent, recoverable contract. In `.ai/architecture.md`, extend the Gate 8 execution/disposition bullet to note the result-path finalizer connects a resolved RunRecord to the runtime terminal state and the matching queue disposition, with autonomous post-terminal recovery; connection #3 (worker/local IPC) is next.

- [ ] **Step 3: Update `CURRENT_TASK.md`**

Set task `gate-8-result-path-finalization`, status `Completed`, `Justified By` the Step 1 decision, acceptance criteria mapped to the delivered finalizer, out-of-scope list, verification summary, and `Next` = connection #3 (process-isolated worker and local IPC).

- [ ] **Step 4: Update `PROJECT_STATE.md` and `SESSION_HANDOFF.md`**

Record the result-path finalizer as Contract Verified under Gate 8; keep Gate 8 the sole `Specified - Next`. In `SESSION_HANDOFF.md`, update the lead bullets, `Updated At` (2026-07-17), `Completed Work`, `Current Maturity` Gate 8 line, `Next Task` (connection #3), and the Instructions-For-Next-Agent steps. Add a `CHANGELOG.md` entry.

- [ ] **Step 5: Run full regression and structural checks**

Run: `.\scripts\gradle.ps1 --no-daemon clean test --warning-mode all`
Expected: all suites pass (2 existing Windows symbolic-link skips allowed), 0 failures, 0 errors; strict lint clean.
Run: `git diff --check` (no whitespace errors) and confirm exactly one `^Status: Specified - Next` gate marker (Gate 8) in `ROADMAP.md`.

- [ ] **Step 6: Commit** (only with user authorization)

```bash
git add ARCHITECTURE.md .ai/architecture.md DECISION_LOG.md CURRENT_TASK.md PROJECT_STATE.md SESSION_HANDOFF.md CHANGELOG.md
git commit -m "docs: record Gate 8 result-path finalization delivery"
```

---

## Self-Review Notes

- **Spec coverage:** forward finalize verified/failed (T1), autonomous `recoverFinalization` + idempotent re-finalize (T2), fail-closed + binding + wrong-status + terminal-reference-mismatch guards (T3), docs/decision sync (T4). Decisions 1–4 and improvements 1–6 all map to T1–T3.
- **Type consistency:** `finalizeAgentRun(String,String,String) → WorkItemDisposition` and `recoverFinalization(String) → Optional<WorkItemDisposition>` are used identically in production and tests. Disposition is derived once, in `applyQueueDisposition`, from `RuntimeAgentRunStatus`. `ResultPayload`, `MessageEnvelope`, and `ApprovedTaskRevision` signatures match the grounded source.
- **Placeholder scan:** the only forward reference is the `recoverQuietly` placeholder in T1 Step 1, explicitly corrected in the note after the code block and in T1 Step 3.
- **Known construction detail:** a valid `RunRecord` requires the full 11-component constructor with matching stop-reason/verification invariants; the verified case uses `AWAITING_VERIFICATION → COMPLETED` with `VerificationDecision.verified`, the failed case uses `AWAITING_VERIFICATION → AWAITING_VERIFICATION` with `VerificationDecision.rejected`.
