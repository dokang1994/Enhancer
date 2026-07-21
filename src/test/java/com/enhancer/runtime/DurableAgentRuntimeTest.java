package com.enhancer.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.enhancer.bus.ControlPayload;
import com.enhancer.bus.ControlSignal;
import com.enhancer.bus.MessageEnvelope;
import com.enhancer.bus.ResultPayload;
import com.enhancer.bus.WorkPayload;
import com.enhancer.kernel.VerificationStatus;
import com.enhancer.workspace.ApprovedTaskRevision;
import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class DurableAgentRuntimeTest {
    private static final String GOAL_ID =
            "00000000-0000-0000-0000-000000000401";
    private static final String AGENT_RUN_ID =
            "00000000-0000-0000-0000-000000000402";
    private static final String WORK_ITEM_ID =
            "00000000-0000-0000-0000-000000000403";
    private static final String WORK_MESSAGE_ID =
            "00000000-0000-0000-0000-000000000404";

    @Test
    void persistsTheForwardLifecycleAndVerifiedTerminalResult()
            throws Exception {
        MemoryAgentRuntimeStateStore store =
                new MemoryAgentRuntimeStateStore();
        WorkItem workItem = workItem();
        DurableAgentRuntime runtime =
                DurableAgentRuntime.create(GOAL_ID, workItem, store);

        assertEquals(0, runtime.revision());
        assertEquals(RuntimeGoalStatus.ACCEPTED, runtime.goal().status());
        assertTrue(runtime.agentRun().isEmpty());

        runtime.beginAgentRun(AGENT_RUN_ID);
        runtime.markReady(AGENT_RUN_ID);
        AgentRunLease lease = runtime.acquireLease(
                AGENT_RUN_ID,
                "worker-primary",
                Duration.ofMinutes(5));
        runtime.completeExecution(
                AGENT_RUN_ID,
                lease.ownerId(),
                lease.fenceToken());
        runtime.recordResult(
                AGENT_RUN_ID,
                resultMessage(VerificationStatus.VERIFIED));

        assertEquals(5, runtime.revision());
        assertEquals(RuntimeGoalStatus.COMPLETED, runtime.goal().status());
        RuntimeAgentRun run = runtime.agentRun().orElseThrow();
        assertEquals(RuntimeAgentRunStatus.COMPLETED, run.status());
        assertEquals(
                "run-record/runtime-verified",
                ((ResultPayload) run.resultMessage()
                        .orElseThrow()
                        .payload())
                        .runRecordReference());

        DurableAgentRuntime recovered =
                DurableAgentRuntime.recover(GOAL_ID, store);
        assertEquals(5, recovered.revision());
        assertEquals(runtime.goal(), recovered.goal());
        assertEquals(runtime.agentRun(), recovered.agentRun());
        assertSame(workItem, recovered.goal().workItem());
    }

    @Test
    void recordsEveryNonVerifiedResultAsExplicitFailure()
            throws Exception {
        for (VerificationStatus status : Set.of(
                VerificationStatus.REJECTED,
                VerificationStatus.UNVERIFIED,
                VerificationStatus.NOT_PERFORMED)) {
            MemoryAgentRuntimeStateStore store =
                    new MemoryAgentRuntimeStateStore();
            DurableAgentRuntime runtime =
                    DurableAgentRuntime.create(GOAL_ID, workItem(), store);
            runtime.beginAgentRun(AGENT_RUN_ID);
            runtime.markReady(AGENT_RUN_ID);
            AgentRunLease lease = runtime.acquireLease(
                    AGENT_RUN_ID,
                    "worker-primary",
                    Duration.ofMinutes(5));
            runtime.completeExecution(
                    AGENT_RUN_ID,
                    lease.ownerId(),
                    lease.fenceToken());

            runtime.recordResult(AGENT_RUN_ID, resultMessage(status));

            assertEquals(RuntimeGoalStatus.FAILED, runtime.goal().status());
            assertEquals(
                    RuntimeAgentRunStatus.FAILED,
                    runtime.agentRun().orElseThrow().status());
        }
    }

    @Test
    void rejectsSkippedRepeatedMismatchedAndPostTerminalTransitions()
            throws Exception {
        DurableAgentRuntime runtime = DurableAgentRuntime.create(
                GOAL_ID,
                workItem(),
                new MemoryAgentRuntimeStateStore());

        assertThrows(IllegalStateException.class, () ->
                runtime.acquireLease(
                        AGENT_RUN_ID,
                        "worker-primary",
                        Duration.ofMinutes(5)));
        runtime.beginAgentRun(AGENT_RUN_ID);
        assertThrows(IllegalStateException.class, () ->
                runtime.beginAgentRun(
                        "00000000-0000-0000-0000-000000000499"));
        assertThrows(IllegalArgumentException.class, () ->
                runtime.markReady(
                        "00000000-0000-0000-0000-000000000499"));
        runtime.markReady(AGENT_RUN_ID);
        AgentRunLease lease = runtime.acquireLease(
                AGENT_RUN_ID,
                "worker-primary",
                Duration.ofMinutes(5));
        runtime.completeExecution(
                AGENT_RUN_ID,
                lease.ownerId(),
                lease.fenceToken());

        MessageEnvelope mismatchedRun = new MessageEnvelope(
                "00000000-0000-0000-0000-000000000491",
                "correlation-runtime-1",
                Optional.of(WORK_MESSAGE_ID),
                "another-logical-run",
                "runtime-test",
                Instant.parse("2026-07-16T18:00:00Z"),
                new ResultPayload(
                        "gate-8-durable-goal-agent-run-lifecycle",
                        "run-record/mismatch",
                        VerificationStatus.VERIFIED));
        assertThrows(IllegalArgumentException.class, () ->
                runtime.recordResult(AGENT_RUN_ID, mismatchedRun));

        assertThrows(IllegalArgumentException.class, () ->
                runtime.recordResult(
                        AGENT_RUN_ID,
                        resultMessage(
                                "wrong-correlation",
                                Optional.of(WORK_MESSAGE_ID),
                                "logical-run-runtime-1",
                                "gate-8-durable-goal-agent-run-lifecycle")));
        assertThrows(IllegalArgumentException.class, () ->
                runtime.recordResult(
                        AGENT_RUN_ID,
                        resultMessage(
                                "correlation-runtime-1",
                                Optional.empty(),
                                "logical-run-runtime-1",
                                "gate-8-durable-goal-agent-run-lifecycle")));
        assertThrows(IllegalArgumentException.class, () ->
                runtime.recordResult(
                        AGENT_RUN_ID,
                        resultMessage(
                                "correlation-runtime-1",
                                Optional.of(WORK_MESSAGE_ID),
                                "logical-run-runtime-1",
                                "another-task")));
        assertThrows(IllegalArgumentException.class, () ->
                runtime.recordResult(
                        AGENT_RUN_ID,
                        new MessageEnvelope(
                                GOAL_ID,
                                "correlation-runtime-1",
                                Optional.of(WORK_MESSAGE_ID),
                                "logical-run-runtime-1",
                                "runtime-test",
                                Instant.parse("2026-07-16T18:00:00Z"),
                                new ResultPayload(
                                        "gate-8-durable-goal-agent-run-lifecycle",
                                        "run-record/identity-collision",
                                        VerificationStatus.VERIFIED))));

        runtime.recordResult(
                AGENT_RUN_ID,
                resultMessage(VerificationStatus.VERIFIED));
        assertThrows(IllegalStateException.class, () ->
                runtime.recordResult(
                        AGENT_RUN_ID,
                        resultMessage(VerificationStatus.VERIFIED)));
    }

    @Test
    void persistenceFailureLeavesThePreviousRevisionVisible()
            throws Exception {
        MemoryAgentRuntimeStateStore store =
                new MemoryAgentRuntimeStateStore();
        DurableAgentRuntime runtime =
                DurableAgentRuntime.create(GOAL_ID, workItem(), store);

        store.failNextUpdate();
        assertThrows(IOException.class, () ->
                runtime.beginAgentRun(AGENT_RUN_ID));
        assertEquals(0, runtime.revision());
        assertEquals(RuntimeGoalStatus.ACCEPTED, runtime.goal().status());
        assertTrue(runtime.agentRun().isEmpty());

        runtime.beginAgentRun(AGENT_RUN_ID);
        store.failNextUpdate();
        assertThrows(IOException.class, () ->
                runtime.markReady(AGENT_RUN_ID));
        assertEquals(1, runtime.revision());
        assertEquals(
                RuntimeAgentRunStatus.PLANNING,
                runtime.agentRun().orElseThrow().status());
    }

    @Test
    void renewsOnlyTheCurrentUnexpiredOwnerAndFence()
            throws Exception {
        MutableClock clock = new MutableClock(
                Instant.parse("2026-07-16T20:00:00Z"));
        DurableAgentRuntime runtime = DurableAgentRuntime.create(
                GOAL_ID,
                workItem(),
                new MemoryAgentRuntimeStateStore(),
                clock);
        runtime.beginAgentRun(AGENT_RUN_ID);
        runtime.markReady(AGENT_RUN_ID);
        AgentRunLease lease = runtime.acquireLease(
                AGENT_RUN_ID,
                "worker-\uD83D\uDE80",
                Duration.ofMinutes(5));

        assertEquals(1, lease.fenceToken());
        assertEquals(clock.instant(), lease.issuedAt());
        assertEquals(
                clock.instant().plus(Duration.ofMinutes(5)),
                lease.expiresAt());
        assertEquals(
                Optional.of(lease),
                runtime.agentRun().orElseThrow().lease());

        clock.advance(Duration.ofMinutes(1));
        AgentRunLease renewed = runtime.renewLease(
                AGENT_RUN_ID,
                lease.ownerId(),
                lease.fenceToken(),
                Duration.ofMinutes(10));
        assertEquals(lease.fenceToken(), renewed.fenceToken());
        assertTrue(renewed.expiresAt().isAfter(lease.expiresAt()));

        assertThrows(IllegalArgumentException.class, () ->
                runtime.renewLease(
                        AGENT_RUN_ID,
                        "another-worker",
                        renewed.fenceToken(),
                        Duration.ofMinutes(10)));
        assertThrows(IllegalArgumentException.class, () ->
                runtime.completeExecution(
                        AGENT_RUN_ID,
                        renewed.ownerId(),
                        renewed.fenceToken() + 1));

        clock.advance(Duration.ofMinutes(10));
        assertThrows(IllegalStateException.class, () ->
                runtime.completeExecution(
                        AGENT_RUN_ID,
                        renewed.ownerId(),
                        renewed.fenceToken()));
    }

    @Test
    void recoversExpiredExecutionToReadyAndFencesTheFormerOwner()
            throws Exception {
        MutableClock clock = new MutableClock(
                Instant.parse("2026-07-16T21:00:00Z"));
        MemoryAgentRuntimeStateStore store =
                new MemoryAgentRuntimeStateStore();
        DurableAgentRuntime runtime = DurableAgentRuntime.create(
                GOAL_ID,
                workItem(),
                store,
                clock);
        runtime.beginAgentRun(AGENT_RUN_ID);
        runtime.markReady(AGENT_RUN_ID);
        AgentRunLease first = runtime.acquireLease(
                AGENT_RUN_ID,
                "worker-first",
                Duration.ofMinutes(5));

        clock.advance(Duration.ofMinutes(4));
        DurableAgentRuntime beforeExpiry = DurableAgentRuntime.recover(
                GOAL_ID,
                store,
                clock);
        assertEquals(
                RuntimeAgentRunStatus.EXECUTING,
                beforeExpiry.agentRun().orElseThrow().status());
        assertEquals(
                Optional.of(first),
                beforeExpiry.agentRun().orElseThrow().lease());

        clock.advance(Duration.ofMinutes(1));
        DurableAgentRuntime reclaimed = DurableAgentRuntime.recover(
                GOAL_ID,
                store,
                clock);
        assertEquals(4, reclaimed.revision());
        assertEquals(
                RuntimeAgentRunStatus.READY,
                reclaimed.agentRun().orElseThrow().status());
        assertTrue(reclaimed.agentRun().orElseThrow().lease().isEmpty());

        AgentRunLease second = reclaimed.acquireLease(
                AGENT_RUN_ID,
                "worker-second",
                Duration.ofMinutes(5));
        assertEquals(first.fenceToken() + 1, second.fenceToken());
        assertThrows(IllegalArgumentException.class, () ->
                reclaimed.completeExecution(
                        AGENT_RUN_ID,
                        first.ownerId(),
                        first.fenceToken()));
        reclaimed.completeExecution(
                AGENT_RUN_ID,
                second.ownerId(),
                second.fenceToken());
    }

    @Test
    void boundsLeaseOwnerAndDuration() throws Exception {
        MutableClock clock = new MutableClock(
                Instant.parse("2026-07-16T22:00:00Z"));
        DurableAgentRuntime runtime = DurableAgentRuntime.create(
                GOAL_ID,
                workItem(),
                new MemoryAgentRuntimeStateStore(),
                clock);
        runtime.beginAgentRun(AGENT_RUN_ID);
        runtime.markReady(AGENT_RUN_ID);

        assertThrows(IllegalArgumentException.class, () ->
                runtime.acquireLease(
                        AGENT_RUN_ID,
                        " ",
                        Duration.ofMinutes(1)));
        assertThrows(IllegalArgumentException.class, () ->
                runtime.acquireLease(
                        AGENT_RUN_ID,
                        "w".repeat(AgentRunLease.MAX_OWNER_CHARACTERS + 1),
                        Duration.ofMinutes(1)));
        assertThrows(IllegalArgumentException.class, () ->
                runtime.acquireLease(
                        AGENT_RUN_ID,
                        "worker",
                        Duration.ZERO));
        assertThrows(IllegalArgumentException.class, () ->
                runtime.acquireLease(
                        AGENT_RUN_ID,
                        "worker",
                        AgentRunLease.MAX_DURATION.plusMillis(1)));
    }

    @Test
    void leasePersistenceFailureLeavesThePreviousRevisionVisible()
            throws Exception {
        MutableClock clock = new MutableClock(
                Instant.parse("2026-07-16T23:00:00Z"));
        MemoryAgentRuntimeStateStore store =
                new MemoryAgentRuntimeStateStore();
        DurableAgentRuntime runtime = DurableAgentRuntime.create(
                GOAL_ID,
                workItem(),
                store,
                clock);
        runtime.beginAgentRun(AGENT_RUN_ID);
        runtime.markReady(AGENT_RUN_ID);

        store.failNextUpdate();
        assertThrows(IOException.class, () ->
                runtime.acquireLease(
                        AGENT_RUN_ID,
                        "worker",
                        Duration.ofMinutes(1)));
        assertEquals(2, runtime.revision());
        assertEquals(
                RuntimeAgentRunStatus.READY,
                runtime.agentRun().orElseThrow().status());

        AgentRunLease lease = runtime.acquireLease(
                AGENT_RUN_ID,
                "worker",
                Duration.ofMinutes(1));
        store.failNextUpdate();
        assertThrows(IOException.class, () ->
                runtime.renewLease(
                        AGENT_RUN_ID,
                        lease.ownerId(),
                        lease.fenceToken(),
                        Duration.ofMinutes(2)));
        assertEquals(3, runtime.revision());
        assertEquals(
                Optional.of(lease),
                runtime.agentRun().orElseThrow().lease());

        store.failNextUpdate();
        assertThrows(IOException.class, () ->
                runtime.completeExecution(
                        AGENT_RUN_ID,
                        lease.ownerId(),
                        lease.fenceToken()));
        assertEquals(3, runtime.revision());
        assertEquals(
                RuntimeAgentRunStatus.EXECUTING,
                runtime.agentRun().orElseThrow().status());
        assertEquals(
                Optional.of(lease),
                runtime.agentRun().orElseThrow().lease());

        clock.advance(Duration.ofMinutes(1));
        store.failNextUpdate();
        assertThrows(IOException.class, runtime::reclaimExpiredLease);
        assertEquals(
                RuntimeAgentRunStatus.EXECUTING,
                runtime.agentRun().orElseThrow().status());
        assertEquals(
                Optional.of(lease),
                runtime.agentRun().orElseThrow().lease());
    }

    @Test
    void persistsBoundControlRequestsWithoutChangingRuntimeAuthority()
            throws Exception {
        MemoryAgentRuntimeStateStore store =
                new MemoryAgentRuntimeStateStore();
        DurableAgentRuntime runtime = DurableAgentRuntime.create(
                GOAL_ID, workItem(), store);
        runtime.beginAgentRun(AGENT_RUN_ID);
        runtime.markReady(AGENT_RUN_ID);
        RuntimeGoal goalBefore = runtime.goal();
        RuntimeAgentRun runBefore = runtime.agentRun().orElseThrow();
        MessageEnvelope pause = controlMessage(
                "00000000-0000-0000-0000-000000000406",
                ControlSignal.PAUSE,
                "operator requested pause");

        assertTrue(runtime.recordControlRequest(pause));
        assertEquals(3, runtime.revision());
        assertEquals(goalBefore, runtime.goal());
        assertEquals(runBefore, runtime.agentRun().orElseThrow());
        assertEquals(0, runtime.lastIssuedFenceToken());
        assertEquals(java.util.List.of(pause), runtime.controlRequests());

        assertFalse(runtime.recordControlRequest(pause));
        assertEquals(3, runtime.revision());
        assertEquals(java.util.List.of(pause), runtime.controlRequests());

        AgentRunLease lease = runtime.acquireLease(
                AGENT_RUN_ID,
                "worker-primary",
                Duration.ofMinutes(5));
        runtime.completeExecution(
                AGENT_RUN_ID, lease.ownerId(), lease.fenceToken());
        runtime.recordResult(
                AGENT_RUN_ID,
                resultMessage(VerificationStatus.VERIFIED));
        assertEquals(java.util.List.of(pause), runtime.controlRequests());
        assertThrows(IllegalStateException.class, () ->
                runtime.recordControlRequest(controlMessage(
                        "00000000-0000-0000-0000-000000000407",
                        ControlSignal.CANCEL,
                        "too late")));
    }

    @Test
    void rejectsUnboundCollidingAndOverCapacityControlRequests()
            throws Exception {
        DurableAgentRuntime runtime = DurableAgentRuntime.create(
                GOAL_ID,
                workItem(),
                new MemoryAgentRuntimeStateStore());
        assertThrows(IllegalStateException.class, () ->
                runtime.recordControlRequest(controlMessage(
                        "00000000-0000-0000-0000-000000000406",
                        ControlSignal.PAUSE,
                        "no run")));
        runtime.beginAgentRun(AGENT_RUN_ID);
        runtime.markReady(AGENT_RUN_ID);

        assertThrows(IllegalArgumentException.class, () ->
                runtime.recordControlRequest(workItem().workMessage()));
        for (String collision : java.util.List.of(
                GOAL_ID, WORK_ITEM_ID, WORK_MESSAGE_ID, AGENT_RUN_ID)) {
            assertThrows(IllegalArgumentException.class, () ->
                    runtime.recordControlRequest(new MessageEnvelope(
                            collision,
                            "correlation-runtime-1",
                            Optional.of(WORK_MESSAGE_ID),
                            "logical-run-runtime-1",
                            "runtime-control-test",
                            Instant.parse("2026-07-16T17:45:00Z"),
                            new ControlPayload(
                                    ControlSignal.PAUSE,
                                    "identity collision"))));
        }
        assertThrows(IllegalArgumentException.class, () ->
                runtime.recordControlRequest(controlMessage(
                        "00000000-0000-0000-0000-000000000406",
                        "other-correlation",
                        Optional.of(WORK_MESSAGE_ID),
                        "logical-run-runtime-1",
                        ControlSignal.PAUSE,
                        "mismatch")));
        assertThrows(IllegalArgumentException.class, () ->
                runtime.recordControlRequest(controlMessage(
                        "00000000-0000-0000-0000-000000000406",
                        "correlation-runtime-1",
                        Optional.empty(),
                        "logical-run-runtime-1",
                        ControlSignal.PAUSE,
                        "mismatch")));
        assertThrows(IllegalArgumentException.class, () ->
                runtime.recordControlRequest(controlMessage(
                        "00000000-0000-0000-0000-000000000406",
                        "correlation-runtime-1",
                        Optional.of(WORK_MESSAGE_ID),
                        "other-logical-run",
                        ControlSignal.PAUSE,
                        "mismatch")));

        MessageEnvelope first = controlMessage(
                "00000000-0000-0000-0000-000000000406",
                ControlSignal.PAUSE,
                "first");
        assertTrue(runtime.recordControlRequest(first));
        assertThrows(IllegalArgumentException.class, () ->
                runtime.recordControlRequest(controlMessage(
                        first.messageId(),
                        ControlSignal.PAUSE,
                        "different content")));
        for (int index = 1;
                index < AgentRuntimeState.MAX_CONTROL_REQUESTS;
                index++) {
            runtime.recordControlRequest(controlMessage(
                    String.format(
                            "00000000-0000-0000-0000-%012d",
                            406 + index),
                    ControlSignal.RESUME,
                    "request-" + index));
        }
        assertEquals(
                AgentRuntimeState.MAX_CONTROL_REQUESTS,
                runtime.controlRequests().size());
        assertThrows(IllegalStateException.class, () ->
                runtime.recordControlRequest(controlMessage(
                        "00000000-0000-0000-0000-000000000999",
                        ControlSignal.CANCEL,
                        "over capacity")));
    }

    @Test
    void controlPersistenceFailureLeavesPreviousRevisionInvisible()
            throws Exception {
        MemoryAgentRuntimeStateStore store =
                new MemoryAgentRuntimeStateStore();
        DurableAgentRuntime runtime = DurableAgentRuntime.create(
                GOAL_ID, workItem(), store);
        runtime.beginAgentRun(AGENT_RUN_ID);
        runtime.markReady(AGENT_RUN_ID);
        store.failNextUpdate();

        assertThrows(IOException.class, () ->
                runtime.recordControlRequest(controlMessage(
                        "00000000-0000-0000-0000-000000000406",
                        ControlSignal.CANCEL,
                        "persist first")));
        assertEquals(2, runtime.revision());
        assertTrue(runtime.controlRequests().isEmpty());
        assertTrue(store.resolve(GOAL_ID).controlRequests().isEmpty());
    }

    @Test
    void rejectsIdentityCollisionsAndExistingOrMissingState()
            throws Exception {
        MemoryAgentRuntimeStateStore store =
                new MemoryAgentRuntimeStateStore();
        WorkItem workItem = workItem();

        assertThrows(IllegalArgumentException.class, () ->
                DurableAgentRuntime.create("not-a-uuid", workItem, store));
        assertThrows(IllegalArgumentException.class, () ->
                DurableAgentRuntime.create(
                        WORK_ITEM_ID,
                        workItem,
                        store));
        DurableAgentRuntime.create(GOAL_ID, workItem, store);
        assertThrows(IOException.class, () ->
                DurableAgentRuntime.create(GOAL_ID, workItem, store));
        assertThrows(MissingAgentRuntimeStateException.class, () ->
                DurableAgentRuntime.recover(
                        "00000000-0000-0000-0000-000000000498",
                        store));

        MemoryAgentRuntimeStateStore secondStore =
                new MemoryAgentRuntimeStateStore();
        DurableAgentRuntime secondRuntime =
                DurableAgentRuntime.create(
                        "00000000-0000-0000-0000-000000000497",
                        workItem,
                        secondStore);
        assertThrows(IllegalArgumentException.class, () ->
                secondRuntime.beginAgentRun(WORK_MESSAGE_ID));
    }

    private static WorkItem workItem() {
        ApprovedTaskRevision revision = new ApprovedTaskRevision(
                "gate-8-durable-goal-agent-run-lifecycle",
                "CURRENT_TASK.md",
                "a".repeat(64));
        MessageEnvelope envelope = new MessageEnvelope(
                WORK_MESSAGE_ID,
                "correlation-runtime-1",
                Optional.empty(),
                "logical-run-runtime-1",
                "runtime-test",
                Instant.parse("2026-07-16T17:30:00Z"),
                new WorkPayload(
                        revision,
                        "b".repeat(64),
                        Set.of("read-file", "verify")));
        return new WorkItem(
                WORK_ITEM_ID,
                "runtime-worker",
                envelope);
    }

    private static MessageEnvelope resultMessage(
            VerificationStatus status) {
        return resultMessage(
                "correlation-runtime-1",
                Optional.of(WORK_MESSAGE_ID),
                "logical-run-runtime-1",
                "gate-8-durable-goal-agent-run-lifecycle",
                status);
    }

    private static MessageEnvelope controlMessage(
            String messageId,
            ControlSignal signal,
            String reason) {
        return controlMessage(
                messageId,
                "correlation-runtime-1",
                Optional.of(WORK_MESSAGE_ID),
                "logical-run-runtime-1",
                signal,
                reason);
    }

    private static MessageEnvelope controlMessage(
            String messageId,
            String correlationId,
            Optional<String> causationId,
            String logicalRunId,
            ControlSignal signal,
            String reason) {
        return new MessageEnvelope(
                messageId,
                correlationId,
                causationId,
                logicalRunId,
                "runtime-control-test",
                Instant.parse("2026-07-16T17:45:00Z"),
                new ControlPayload(signal, reason));
    }

    private static MessageEnvelope resultMessage(
            String correlationId,
            Optional<String> causationId,
            String logicalRunId,
            String taskId) {
        return resultMessage(
                correlationId,
                causationId,
                logicalRunId,
                taskId,
                VerificationStatus.VERIFIED);
    }

    private static MessageEnvelope resultMessage(
            String correlationId,
            Optional<String> causationId,
            String logicalRunId,
            String taskId,
            VerificationStatus status) {
        return new MessageEnvelope(
                "00000000-0000-0000-0000-000000000405",
                correlationId,
                causationId,
                logicalRunId,
                "runtime-test",
                Instant.parse("2026-07-16T18:00:00Z"),
                new ResultPayload(
                        taskId,
                        "run-record/runtime-verified",
                        status));
    }

    private static final class MemoryAgentRuntimeStateStore
            implements AgentRuntimeStateStore {
        private AgentRuntimeState state;
        private boolean failNextUpdate;

        @Override
        public void create(AgentRuntimeState initialState)
                throws IOException {
            if (state != null) {
                throw new IOException("runtime already exists");
            }
            state = initialState;
        }

        @Override
        public void update(AgentRuntimeState nextState)
                throws IOException {
            if (failNextUpdate) {
                failNextUpdate = false;
                throw new IOException("simulated persistence failure");
            }
            if (state == null
                    || nextState.revision() != state.revision() + 1) {
                throw new IOException("revision does not advance by one");
            }
            state = nextState;
        }

        @Override
        public AgentRuntimeState resolve(String goalId)
                throws IOException {
            if (state == null || !state.goal().goalId().equals(goalId)) {
                throw new MissingAgentRuntimeStateException(goalId);
            }
            return state;
        }

        void failNextUpdate() {
            failNextUpdate = true;
        }
    }

    private static final class MutableClock extends Clock {
        private Instant instant;

        private MutableClock(Instant instant) {
            this.instant = instant;
        }

        @Override
        public ZoneId getZone() {
            return ZoneId.of("UTC");
        }

        @Override
        public Clock withZone(ZoneId zone) {
            if (!getZone().equals(zone)) {
                throw new IllegalArgumentException(
                        "MutableClock supports UTC only");
            }
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }

        void advance(Duration duration) {
            instant = instant.plus(duration);
        }
    }
}
