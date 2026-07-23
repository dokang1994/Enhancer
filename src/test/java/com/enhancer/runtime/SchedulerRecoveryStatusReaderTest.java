package com.enhancer.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.enhancer.bus.MessageEnvelope;
import com.enhancer.bus.WorkPayload;
import com.enhancer.run.ResolvedRunRecord;
import com.enhancer.run.RunRecord;
import com.enhancer.run.RunRecordStore;
import com.enhancer.run.StoredRunRecord;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import com.enhancer.workspace.ApprovedTaskRevision;
import org.junit.jupiter.api.Test;

class SchedulerRecoveryStatusReaderTest {
    private static final String QUEUE_ID =
            "00000000-0000-0000-0000-000000000961";
    private static final String WORK_ITEM_ID =
            "00000000-0000-0000-0000-000000000962";
    private static final String GOAL_ID =
            "00000000-0000-0000-0000-000000000963";
    private static final String AGENT_RUN_ID =
            "00000000-0000-0000-0000-000000000964";

    @Test
    void readsNoPendingCycleWithoutScanningRuntimeOrRunRecords()
            throws Exception {
        CountingRuntimeStore runtimeStore = new CountingRuntimeStore();
        CountingRunRecordStore runRecordStore =
                new CountingRunRecordStore();
        SchedulerRecoveryStatusReader reader =
                new SchedulerRecoveryStatusReader(
                        queueStore(empty(0), empty(0)),
                        runtimeStore,
                        checkpointStore(Optional.empty(), Optional.empty()),
                        runRecordStore);

        SchedulerRecoveryStatus status = reader.read(QUEUE_ID);

        assertEquals(
                SchedulerRecoveryStatus.RecoveryPhase.NO_PENDING_CYCLE,
                status.phase());
        assertEquals(0, runtimeStore.resolutions);
        assertEquals(0, runRecordStore.resolutions);
    }

    @Test
    void refusesAQueueRevisionThatChangesDuringInspection() {
        SchedulerRecoveryStatusReader reader =
                new SchedulerRecoveryStatusReader(
                        queueStore(empty(0), empty(1)),
                        new CountingRuntimeStore(),
                        checkpointStore(Optional.empty(), Optional.empty()),
                        new CountingRunRecordStore());

        assertThrows(
                ConcurrentSchedulerRecoveryInspectionException.class,
                () -> reader.read(QUEUE_ID));
    }

    @Test
    void refusesACheckpointThatChangesDuringInspection() {
        PendingFinalization appeared = new PendingFinalization(
                GOAL_ID, AGENT_RUN_ID, Optional.empty());
        SchedulerRecoveryStatusReader reader =
                new SchedulerRecoveryStatusReader(
                        queueStore(empty(0), empty(0)),
                        new CountingRuntimeStore(),
                        checkpointStore(
                                Optional.empty(),
                                Optional.of(appeared)),
                        new CountingRunRecordStore());

        assertThrows(
                ConcurrentSchedulerRecoveryInspectionException.class,
                () -> reader.read(QUEUE_ID));
    }

    @Test
    void refusesARuntimeRevisionThatChangesDuringInspection() {
        WorkItem workItem = workItem();
        SchedulerQueueState queue = queueWith(workItem);
        PendingFinalization pending = new PendingFinalization(
                GOAL_ID, AGENT_RUN_ID, Optional.empty());
        AgentRuntimeState initial =
                AgentRuntimeState.initial(GOAL_ID, workItem);
        AgentRuntimeState started =
                initial.beginAgentRun(AGENT_RUN_ID);
        SchedulerRecoveryStatusReader reader =
                new SchedulerRecoveryStatusReader(
                        queueStore(queue, queue),
                        runtimeStore(initial, started),
                        checkpointStore(
                                Optional.of(pending),
                                Optional.of(pending)),
                        new CountingRunRecordStore());

        assertThrows(
                ConcurrentSchedulerRecoveryInspectionException.class,
                () -> reader.read(QUEUE_ID));
    }

    private SchedulerQueueState empty(long revision) {
        return new SchedulerQueueState(
                SchedulerQueueState.CURRENT_SCHEMA_VERSION,
                QUEUE_ID,
                revision,
                4,
                Optional.empty(),
                List.of(),
                List.of(),
                List.of(),
                Optional.empty(),
                java.util.Set.of(),
                java.util.Set.of());
    }

    private SchedulerQueueState queueWith(WorkItem workItem) {
        SingleWorkerSchedulerQueue queue =
                new SingleWorkerSchedulerQueue(4);
        queue.enqueue(new QueuedWork(workItem, List.of()));
        return queue.snapshot(QUEUE_ID, 1);
    }

    private WorkItem workItem() {
        MessageEnvelope envelope = new MessageEnvelope(
                "00000000-0000-0000-0000-000000000965",
                "scheduler-recovery-reader-correlation",
                Optional.empty(),
                "scheduler-recovery-reader-run",
                "scheduler-recovery-reader-test",
                Instant.parse("2026-07-23T07:00:00Z"),
                new WorkPayload(
                        new ApprovedTaskRevision(
                                "scheduler-recovery-reader-test",
                                "CURRENT_TASK.md",
                                "a".repeat(64)),
                        "b".repeat(64),
                        Set.of("read-file")));
        return new WorkItem(
                WORK_ITEM_ID, "read-file-worker", envelope);
    }

    private SchedulerQueueStore queueStore(
            SchedulerQueueState... states) {
        ArrayDeque<SchedulerQueueState> values =
                new ArrayDeque<>(List.of(states));
        return new SchedulerQueueStore() {
            @Override
            public void create(SchedulerQueueState initialState) {
                throw new AssertionError("read must not create a queue");
            }

            @Override
            public void update(SchedulerQueueState nextState) {
                throw new AssertionError("read must not update a queue");
            }

            @Override
            public SchedulerQueueState resolve(String queueId) {
                return values.removeFirst();
            }
        };
    }

    private PendingFinalizationStore checkpointStore(
            Optional<PendingFinalization> first,
            Optional<PendingFinalization> second) {
        ArrayDeque<Optional<PendingFinalization>> values =
                new ArrayDeque<>(List.of(first, second));
        return new PendingFinalizationStore() {
            @Override
            public void record(PendingFinalization pending) {
                throw new AssertionError(
                        "read must not record a checkpoint");
            }

            @Override
            public Optional<PendingFinalization> findPending() {
                return values.removeFirst();
            }

            @Override
            public void clear() {
                throw new AssertionError(
                        "read must not clear a checkpoint");
            }
        };
    }

    private AgentRuntimeStateStore runtimeStore(
            AgentRuntimeState first,
            AgentRuntimeState second) {
        ArrayDeque<AgentRuntimeState> values =
                new ArrayDeque<>(List.of(first, second));
        return new AgentRuntimeStateStore() {
            @Override
            public void create(AgentRuntimeState initialState) {
                throw new AssertionError(
                        "read must not create runtime");
            }

            @Override
            public void update(AgentRuntimeState nextState) {
                throw new AssertionError(
                        "read must not update runtime");
            }

            @Override
            public AgentRuntimeState resolve(String goalId) {
                return values.removeFirst();
            }
        };
    }

    private static final class CountingRuntimeStore
            implements AgentRuntimeStateStore {
        private int resolutions;

        @Override
        public void create(AgentRuntimeState initialState) {
            throw new AssertionError("read must not create runtime");
        }

        @Override
        public void update(AgentRuntimeState nextState) {
            throw new AssertionError("read must not update runtime");
        }

        @Override
        public AgentRuntimeState resolve(String goalId) throws IOException {
            resolutions++;
            throw new MissingAgentRuntimeStateException(goalId);
        }
    }

    private static final class CountingRunRecordStore
            implements RunRecordStore {
        private int resolutions;

        @Override
        public StoredRunRecord persist(RunRecord record) {
            throw new AssertionError("read must not persist a RunRecord");
        }

        @Override
        public ResolvedRunRecord resolve(String reference) {
            resolutions++;
            throw new AssertionError(
                    "read must not resolve without a checkpoint reference");
        }

        @Override
        public List<String> references() {
            throw new AssertionError("read must not scan RunRecords");
        }

        @Override
        public List<String> recentReferences(int limit) {
            throw new AssertionError("read must not scan RunRecords");
        }
    }
}
