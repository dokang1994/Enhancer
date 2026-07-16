package com.enhancer.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class DurableAgentRunDispatcherTest {
    private static final String QUEUE_ID =
            "00000000-0000-0000-0000-000000000601";
    private static final String GOAL_ID =
            "00000000-0000-0000-0000-000000000602";
    private static final String AGENT_RUN_ID =
            "00000000-0000-0000-0000-000000000603";
    private static final String WORK_ITEM_ID =
            "00000000-0000-0000-0000-000000000604";
    private static final String WORK_MESSAGE_ID =
            "00000000-0000-0000-0000-000000000605";

    @Test
    void claimsExactWorkAndIdempotentlyReturnsItsPersistedLease()
            throws Exception {
        MemoryQueueStore queueStore = new MemoryQueueStore();
        MemoryAgentStore agentStore = new MemoryAgentStore();
        DurableSingleWorkerSchedulerQueue queue = queue(queueStore);
        WorkItem workItem = workItem(
                WORK_ITEM_ID,
                WORK_MESSAGE_ID,
                "logical-dispatch-1");
        queue.enqueue(new QueuedWork(workItem, List.of()));
        DurableAgentRunDispatcher dispatcher = dispatcher(
                queue,
                agentStore,
                Clock.fixed(
                        Instant.parse("2026-07-16T10:00:00Z"),
                        ZoneId.of("UTC")));

        AgentRunDispatch first = dispatcher.claimAndLease(
                GOAL_ID,
                AGENT_RUN_ID,
                "dispatch-owner-\uD83D\uDE80",
                Duration.ofMinutes(5)).orElseThrow();

        assertEquals(QUEUE_ID, first.queueId());
        assertSame(workItem, first.workItem());
        assertEquals(GOAL_ID, first.goalId());
        assertEquals(AGENT_RUN_ID, first.agentRunId());
        assertEquals("dispatch-owner-\uD83D\uDE80", first.lease().ownerId());
        assertEquals(2, queue.revision());
        assertEquals(Optional.of(workItem), queue.activeWork());
        AgentRuntimeState persisted = agentStore.resolve(GOAL_ID);
        assertEquals(3, persisted.revision());
        assertSame(workItem, persisted.goal().workItem());
        assertEquals(
                RuntimeAgentRunStatus.EXECUTING,
                persisted.agentRun().orElseThrow().status());

        AgentRunDispatch repeated = dispatcher.claimAndLease(
                GOAL_ID,
                AGENT_RUN_ID,
                first.lease().ownerId(),
                Duration.ofMinutes(10)).orElseThrow();

        assertEquals(first, repeated);
        assertEquals(2, queue.revision());
        assertEquals(3, agentStore.resolve(GOAL_ID).revision());
        assertThrows(IllegalArgumentException.class, () ->
                new AgentRunDispatch(
                        QUEUE_ID,
                        workItem,
                        WORK_ITEM_ID,
                        AGENT_RUN_ID,
                        first.lease()));
        assertThrows(IllegalArgumentException.class, () ->
                new AgentRunDispatch(
                        QUEUE_ID,
                        workItem,
                        GOAL_ID,
                        WORK_MESSAGE_ID,
                        first.lease()));
    }

    @Test
    void validatesCallerMetadataBeforeClaimAndReturnsEmptyWithoutReadyWork()
            throws Exception {
        MemoryQueueStore queueStore = new MemoryQueueStore();
        MemoryAgentStore agentStore = new MemoryAgentStore();
        DurableSingleWorkerSchedulerQueue queue = queue(queueStore);
        queue.enqueue(new QueuedWork(
                workItem(
                        WORK_ITEM_ID,
                        WORK_MESSAGE_ID,
                        "logical-dispatch-1"),
                List.of()));
        DurableAgentRunDispatcher dispatcher = dispatcher(
                queue,
                agentStore,
                Clock.systemUTC());

        assertThrows(IllegalArgumentException.class, () ->
                dispatcher.claimAndLease(
                        "not-a-uuid",
                        AGENT_RUN_ID,
                        "owner",
                        Duration.ofMinutes(1)));
        assertThrows(IllegalArgumentException.class, () ->
                dispatcher.claimAndLease(
                        GOAL_ID,
                        "not-a-uuid",
                        "owner",
                        Duration.ofMinutes(1)));
        assertThrows(IllegalArgumentException.class, () ->
                dispatcher.claimAndLease(
                        GOAL_ID,
                        GOAL_ID,
                        "owner",
                        Duration.ofMinutes(1)));
        assertThrows(IllegalArgumentException.class, () ->
                dispatcher.claimAndLease(
                        GOAL_ID,
                        AGENT_RUN_ID,
                        " ",
                        Duration.ofMinutes(1)));
        assertThrows(IllegalArgumentException.class, () ->
                dispatcher.claimAndLease(
                        GOAL_ID,
                        AGENT_RUN_ID,
                        "owner",
                        Duration.ZERO));
        assertEquals(1, queue.revision());
        assertTrue(queue.activeWork().isEmpty());
        assertFalse(agentStore.exists());

        DurableSingleWorkerSchedulerQueue empty =
                DurableSingleWorkerSchedulerQueue.create(
                        "00000000-0000-0000-0000-000000000699",
                        8,
                        new MemoryQueueStore());
        assertTrue(dispatcher(
                empty,
                new MemoryAgentStore(),
                Clock.systemUTC()).claimAndLease(
                        GOAL_ID,
                        AGENT_RUN_ID,
                        "owner",
                        Duration.ofMinutes(1)).isEmpty());
    }

    @Test
    void queueClaimFailureCreatesNoRuntimeState() throws Exception {
        MemoryQueueStore queueStore = new MemoryQueueStore();
        MemoryAgentStore agentStore = new MemoryAgentStore();
        DurableSingleWorkerSchedulerQueue queue = queue(queueStore);
        queue.enqueue(new QueuedWork(
                workItem(
                        WORK_ITEM_ID,
                        WORK_MESSAGE_ID,
                        "logical-dispatch-1"),
                List.of()));
        queueStore.failNextUpdate();

        assertThrows(IOException.class, () -> dispatcher(
                queue,
                agentStore,
                Clock.systemUTC()).claimAndLease(
                        GOAL_ID,
                        AGENT_RUN_ID,
                        "owner",
                        Duration.ofMinutes(1)));

        assertEquals(1, queue.revision());
        assertTrue(queue.activeWork().isEmpty());
        assertFalse(agentStore.exists());
    }

    @Test
    void resumesEveryPersistedRuntimePrefixAfterFailure()
            throws Exception {
        for (int failedWrite = 1; failedWrite <= 4; failedWrite++) {
            MemoryQueueStore queueStore = new MemoryQueueStore();
            MemoryAgentStore agentStore =
                    new MemoryAgentStore(failedWrite);
            DurableSingleWorkerSchedulerQueue queue = queue(queueStore);
            WorkItem workItem = workItem(
                    WORK_ITEM_ID,
                    WORK_MESSAGE_ID,
                    "logical-dispatch-" + failedWrite);
            queue.enqueue(new QueuedWork(workItem, List.of()));
            DurableAgentRunDispatcher dispatcher = dispatcher(
                    queue,
                    agentStore,
                    Clock.fixed(
                            Instant.parse("2026-07-16T11:00:00Z"),
                            ZoneId.of("UTC")));

            assertThrows(IOException.class, () ->
                    dispatcher.claimAndLease(
                            GOAL_ID,
                            AGENT_RUN_ID,
                            "owner",
                            Duration.ofMinutes(5)));
            assertEquals(Optional.of(workItem), queue.activeWork());

            AgentRunDispatch recovered = dispatcher.claimAndLease(
                    GOAL_ID,
                    AGENT_RUN_ID,
                    "owner",
                    Duration.ofMinutes(5)).orElseThrow();

            assertEquals(workItem, recovered.workItem());
            assertEquals(3, agentStore.resolve(GOAL_ID).revision());
            assertEquals(
                    RuntimeAgentRunStatus.EXECUTING,
                    agentStore.resolve(GOAL_ID)
                            .agentRun().orElseThrow().status());
        }
    }

    @Test
    void rejectsMismatchedRuntimeIdentityWorkAndCurrentOwner()
            throws Exception {
        MemoryQueueStore queueStore = new MemoryQueueStore();
        MemoryAgentStore agentStore = new MemoryAgentStore();
        DurableSingleWorkerSchedulerQueue queue = queue(queueStore);
        queue.enqueue(new QueuedWork(
                workItem(
                        WORK_ITEM_ID,
                        WORK_MESSAGE_ID,
                        "logical-dispatch-1"),
                List.of()));
        DurableAgentRunDispatcher dispatcher = dispatcher(
                queue,
                agentStore,
                Clock.fixed(
                        Instant.parse("2026-07-16T12:00:00Z"),
                        ZoneId.of("UTC")));
        dispatcher.claimAndLease(
                GOAL_ID,
                AGENT_RUN_ID,
                "owner-first",
                Duration.ofMinutes(5)).orElseThrow();

        assertThrows(IllegalStateException.class, () ->
                dispatcher.claimAndLease(
                        GOAL_ID,
                        "00000000-0000-0000-0000-000000000690",
                        "owner-first",
                        Duration.ofMinutes(5)));
        assertThrows(IllegalStateException.class, () ->
                dispatcher.claimAndLease(
                        GOAL_ID,
                        AGENT_RUN_ID,
                        "owner-second",
                        Duration.ofMinutes(5)));
        assertEquals(3, agentStore.resolve(GOAL_ID).revision());

        MemoryQueueStore mismatchQueueStore = new MemoryQueueStore();
        MemoryAgentStore mismatchAgentStore = new MemoryAgentStore();
        DurableSingleWorkerSchedulerQueue mismatchQueue =
                queue(mismatchQueueStore);
        mismatchQueue.enqueue(new QueuedWork(
                workItem(
                        WORK_ITEM_ID,
                        WORK_MESSAGE_ID,
                        "logical-dispatch-1"),
                List.of()));
        WorkItem differentWork = workItem(
                "00000000-0000-0000-0000-000000000691",
                "00000000-0000-0000-0000-000000000692",
                "logical-dispatch-1");
        MutableClock mismatchClock = new MutableClock(
                Instant.parse("2026-07-16T12:30:00Z"));
        DurableAgentRuntime mismatchedRuntime =
                DurableAgentRuntime.create(
                        GOAL_ID,
                        differentWork,
                        mismatchAgentStore,
                        mismatchClock);
        mismatchedRuntime.beginAgentRun(AGENT_RUN_ID);
        mismatchedRuntime.markReady(AGENT_RUN_ID);
        mismatchedRuntime.acquireLease(
                AGENT_RUN_ID,
                "mismatched-owner",
                Duration.ofMinutes(1));
        mismatchClock.advance(Duration.ofMinutes(1));

        assertThrows(IllegalStateException.class, () ->
                dispatcher(
                        mismatchQueue,
                        mismatchAgentStore,
                        mismatchClock).claimAndLease(
                                GOAL_ID,
                                AGENT_RUN_ID,
                                "owner",
                                Duration.ofMinutes(5)));
        assertTrue(mismatchQueue.activeWork().isPresent());
        assertEquals(differentWork,
                mismatchAgentStore.resolve(GOAL_ID).goal().workItem());
        assertEquals(3, mismatchAgentStore.resolve(GOAL_ID).revision());
        assertEquals(
                RuntimeAgentRunStatus.EXECUTING,
                mismatchAgentStore.resolve(GOAL_ID)
                        .agentRun().orElseThrow().status());
    }

    @Test
    void rejectsPostExecutionStates() throws Exception {
        for (boolean terminal : List.of(false, true)) {
            MemoryQueueStore queueStore = new MemoryQueueStore();
            MemoryAgentStore agentStore = new MemoryAgentStore();
            DurableSingleWorkerSchedulerQueue queue = queue(queueStore);
            WorkItem workItem = workItem(
                    WORK_ITEM_ID,
                    WORK_MESSAGE_ID,
                    "logical-post-execution-" + terminal);
            queue.enqueue(new QueuedWork(workItem, List.of()));
            MutableClock clock = new MutableClock(
                    Instant.parse("2026-07-16T13:00:00Z"));
            DurableAgentRuntime runtime = DurableAgentRuntime.create(
                    GOAL_ID,
                    workItem,
                    agentStore,
                    clock);
            runtime.beginAgentRun(AGENT_RUN_ID);
            runtime.markReady(AGENT_RUN_ID);
            AgentRunLease lease = runtime.acquireLease(
                    AGENT_RUN_ID,
                    "owner",
                    Duration.ofMinutes(5));
            runtime.completeExecution(
                    AGENT_RUN_ID,
                    lease.ownerId(),
                    lease.fenceToken());
            if (terminal) {
                runtime.recordResult(
                        AGENT_RUN_ID,
                        resultMessage(workItem));
            }

            assertThrows(IllegalStateException.class, () ->
                    dispatcher(queue, agentStore, clock).claimAndLease(
                            GOAL_ID,
                            AGENT_RUN_ID,
                            "owner",
                            Duration.ofMinutes(5)));
            assertTrue(queue.activeWork().isPresent());
        }
    }

    @Test
    void reclaimsExpiredOwnerAndIssuesAGreaterFence()
            throws Exception {
        MemoryQueueStore queueStore = new MemoryQueueStore();
        MemoryAgentStore agentStore = new MemoryAgentStore();
        DurableSingleWorkerSchedulerQueue queue = queue(queueStore);
        queue.enqueue(new QueuedWork(
                workItem(
                        WORK_ITEM_ID,
                        WORK_MESSAGE_ID,
                        "logical-dispatch-1"),
                List.of()));
        MutableClock clock = new MutableClock(
                Instant.parse("2026-07-16T14:00:00Z"));
        DurableAgentRunDispatcher dispatcher =
                dispatcher(queue, agentStore, clock);
        AgentRunLease first = dispatcher.claimAndLease(
                GOAL_ID,
                AGENT_RUN_ID,
                "owner-first",
                Duration.ofMinutes(1)).orElseThrow().lease();

        clock.advance(Duration.ofMinutes(1));
        AgentRunLease second = dispatcher.claimAndLease(
                GOAL_ID,
                AGENT_RUN_ID,
                "owner-second",
                Duration.ofMinutes(1)).orElseThrow().lease();

        assertEquals(first.fenceToken() + 1, second.fenceToken());
        assertEquals("owner-second", second.ownerId());
        assertEquals(5, agentStore.resolve(GOAL_ID).revision());
    }

    private static DurableSingleWorkerSchedulerQueue queue(
            MemoryQueueStore store) throws IOException {
        return DurableSingleWorkerSchedulerQueue.create(
                QUEUE_ID,
                8,
                store);
    }

    private static DurableAgentRunDispatcher dispatcher(
            DurableSingleWorkerSchedulerQueue queue,
            AgentRuntimeStateStore store,
            Clock clock) {
        return new DurableAgentRunDispatcher(
                queue,
                store,
                clock);
    }

    private static WorkItem workItem(
            String workItemId,
            String messageId,
            String logicalRunId) {
        ApprovedTaskRevision revision = new ApprovedTaskRevision(
                "gate-8-durable-queue-runtime-dispatch",
                "CURRENT_TASK.md",
                "a".repeat(64));
        return new WorkItem(
                workItemId,
                "runtime-dispatch",
                new MessageEnvelope(
                        messageId,
                        "correlation-runtime-dispatch",
                        Optional.empty(),
                        logicalRunId,
                        "runtime-dispatch-test",
                        Instant.parse("2026-07-16T09:00:00Z"),
                        new WorkPayload(
                                revision,
                                "b".repeat(64),
                                Set.of("read-file", "verify"))));
    }

    private static MessageEnvelope resultMessage(WorkItem workItem) {
        return new MessageEnvelope(
                "00000000-0000-0000-0000-000000000696",
                workItem.workMessage().correlationId(),
                Optional.of(workItem.workMessage().messageId()),
                workItem.logicalRunId(),
                "runtime-dispatch-result",
                Instant.parse("2026-07-16T13:01:00Z"),
                new ResultPayload(
                        workItem.taskRevision().taskId(),
                        "run-record/runtime-dispatch",
                        VerificationStatus.VERIFIED));
    }

    private static final class MemoryQueueStore
            implements SchedulerQueueStore {
        private SchedulerQueueState state;
        private boolean failNextUpdate;

        @Override
        public void create(SchedulerQueueState initialState)
                throws IOException {
            if (state != null) {
                throw new IOException("queue already exists");
            }
            state = initialState;
        }

        @Override
        public void update(SchedulerQueueState nextState)
                throws IOException {
            if (failNextUpdate) {
                failNextUpdate = false;
                throw new IOException("simulated queue persistence failure");
            }
            state = nextState;
        }

        @Override
        public SchedulerQueueState resolve(String queueId)
                throws IOException {
            if (state == null || !state.queueId().equals(queueId)) {
                throw new MissingSchedulerQueueStateException(queueId);
            }
            return state;
        }

        void failNextUpdate() {
            failNextUpdate = true;
        }
    }

    private static final class MemoryAgentStore
            implements AgentRuntimeStateStore {
        private AgentRuntimeState state;
        private final int failedWrite;
        private int writes;
        private boolean failureConsumed;

        private MemoryAgentStore() {
            this(0);
        }

        private MemoryAgentStore(int failedWrite) {
            this.failedWrite = failedWrite;
        }

        @Override
        public void create(AgentRuntimeState initialState)
                throws IOException {
            failIfConfigured();
            if (state != null) {
                throw new IOException("runtime already exists");
            }
            state = initialState;
        }

        @Override
        public void update(AgentRuntimeState nextState)
                throws IOException {
            failIfConfigured();
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

        boolean exists() {
            return state != null;
        }

        private void failIfConfigured() throws IOException {
            writes++;
            if (!failureConsumed && writes == failedWrite) {
                failureConsumed = true;
                throw new IOException(
                        "simulated runtime persistence failure");
            }
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
