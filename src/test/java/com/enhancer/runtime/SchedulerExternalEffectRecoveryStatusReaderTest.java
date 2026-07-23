package com.enhancer.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.enhancer.bus.MessageEnvelope;
import com.enhancer.bus.WorkPayload;
import com.enhancer.run.ResolvedRunRecord;
import com.enhancer.run.RunRecord;
import com.enhancer.run.RunRecordStore;
import com.enhancer.run.StoredRunRecord;
import com.enhancer.tool.EvidenceStoragePolicy;
import com.enhancer.tool.EvidenceStore;
import com.enhancer.tool.ResolvedEvidence;
import com.enhancer.tool.StoredEvidence;
import com.enhancer.workspace.ApprovedTaskRevision;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class SchedulerExternalEffectRecoveryStatusReaderTest {
    private static final String QUEUE_ID =
            "00000000-0000-0000-0000-000000000991";
    private static final String WORK_ITEM_ID =
            "00000000-0000-0000-0000-000000000992";
    private static final String GOAL_ID =
            "00000000-0000-0000-0000-000000000993";
    private static final String AGENT_RUN_ID =
            "00000000-0000-0000-0000-000000000994";
    private static final String EVIDENCE_RUN_ID =
            "00000000-0000-0000-0000-000000000995";
    private static final String EVIDENCE_ID =
            "00000000-0000-0000-0000-000000000996";
    private static final String EVIDENCE_REFERENCE =
            "evidence/" + EVIDENCE_RUN_ID + "/" + EVIDENCE_ID;
    private static final Instant NOW =
            Instant.parse("2026-07-23T10:00:00Z");

    @Test
    void doesNotReadRuntimeEffectsOrEvidenceWithoutACheckpoint()
            throws Exception {
        CountingRuntimeStore runtimeStore =
                new CountingRuntimeStore(Optional.empty());
        CountingEffectStore effectStore =
                new CountingEffectStore();
        CountingEvidenceStore evidenceStore =
                new CountingEvidenceStore("a".repeat(64));
        SchedulerExternalEffectRecoveryStatusReader reader = reader(
                queueStore(empty(0)),
                checkpointStore(Optional.empty()),
                runtimeStore,
                effectStore,
                evidenceStore);

        SchedulerExternalEffectRecoveryStatus status =
                reader.read(QUEUE_ID);

        assertEquals(
                SchedulerExternalEffectRecoveryStatus.RecoveryPhase
                        .NO_CORRELATED_GOAL,
                status.phase());
        assertEquals(0, runtimeStore.resolutions);
        assertEquals(0, effectStore.resolutions);
        assertEquals(0, evidenceStore.resolutions);
    }

    @Test
    void verifiesEveryTerminalEvidenceDigest() throws Exception {
        AgentRuntimeState runtime = executing(workItem());
        String digest = "a".repeat(64);
        ExternalEffectLedgerState ledger = ledger(
                1,
                List.of(new ExternalEffectRecord(
                        request("terminal"),
                        ExternalEffectStatus.COMPENSATED,
                        Optional.of(new ExternalEffectOutcomeEvidence(
                                EVIDENCE_REFERENCE,
                                digest)))));
        CountingEvidenceStore evidenceStore =
                new CountingEvidenceStore(digest);
        SchedulerExternalEffectRecoveryStatusReader reader = reader(
                queueStore(queueWith(workItem())),
                checkpointStore(Optional.of(pending())),
                new CountingRuntimeStore(Optional.of(runtime)),
                new CountingEffectStore(ledger, ledger),
                evidenceStore);

        SchedulerExternalEffectRecoveryStatus status =
                reader.read(QUEUE_ID);

        assertEquals(
                SchedulerExternalEffectRecoveryStatus.RecoveryPhase
                        .ALL_EFFECTS_COMPENSATED,
                status.phase());
        assertEquals(1, status.verifiedTerminalEvidence());
        assertEquals(1, evidenceStore.resolutions);
    }

    @Test
    void rejectsMismatchedTerminalEvidenceDigest() {
        AgentRuntimeState runtime = executing(workItem());
        ExternalEffectLedgerState ledger = ledger(
                1,
                List.of(new ExternalEffectRecord(
                        request("terminal"),
                        ExternalEffectStatus.APPLIED,
                        Optional.of(new ExternalEffectOutcomeEvidence(
                                EVIDENCE_REFERENCE,
                                "a".repeat(64))))));
        SchedulerExternalEffectRecoveryStatusReader reader = reader(
                queueStore(queueWith(workItem())),
                checkpointStore(Optional.of(pending())),
                new CountingRuntimeStore(Optional.of(runtime)),
                new CountingEffectStore(ledger, ledger),
                new CountingEvidenceStore("b".repeat(64)));

        assertThrows(IOException.class, () -> reader.read(QUEUE_ID));
    }

    @Test
    void refusesSchedulerOrLedgerDriftAcrossTheBoundedSamples() {
        AgentRuntimeState runtime = executing(workItem());
        ExternalEffectLedgerState first = ledger(0, List.of());
        ExternalEffectLedgerState second = ledger(
                1, List.of(new ExternalEffectRecord(
                        request("appeared"),
                        ExternalEffectStatus.PREPARED)));
        SchedulerExternalEffectRecoveryStatusReader ledgerDrift = reader(
                queueStore(queueWith(workItem())),
                checkpointStore(Optional.of(pending())),
                new CountingRuntimeStore(Optional.of(runtime)),
                new CountingEffectStore(first, second),
                new CountingEvidenceStore("a".repeat(64)));

        assertThrows(
                ConcurrentSchedulerExternalEffectInspectionException.class,
                () -> ledgerDrift.read(QUEUE_ID));

        SchedulerQueueState revisionOne = queueWith(workItem());
        SchedulerQueueState revisionTwo =
                new SchedulerQueueState(
                        SchedulerQueueState.CURRENT_SCHEMA_VERSION,
                        revisionOne.queueId(),
                        revisionOne.revision() + 1,
                        revisionOne.maxWorkItems(),
                        revisionOne.logicalRunId(),
                        revisionOne.admissionOrder(),
                        revisionOne.pendingWork(),
                        revisionOne.admittedWork(),
                        revisionOne.activeWork(),
                        revisionOne.completedWorkItemIds(),
                        revisionOne.failedWorkItemIds());
        SchedulerExternalEffectRecoveryStatusReader schedulerDrift = reader(
                queueStore(revisionOne, revisionOne,
                        revisionTwo, revisionTwo),
                checkpointStore(Optional.of(pending())),
                new CountingRuntimeStore(Optional.of(runtime)),
                new CountingEffectStore(first, first),
                new CountingEvidenceStore("a".repeat(64)));

        assertThrows(
                ConcurrentSchedulerExternalEffectInspectionException.class,
                () -> schedulerDrift.read(QUEUE_ID));
    }

    private SchedulerExternalEffectRecoveryStatusReader reader(
            SchedulerQueueStore queueStore,
            PendingFinalizationStore checkpointStore,
            AgentRuntimeStateStore runtimeStore,
            ExternalEffectLedgerStore effectStore,
            EvidenceStore evidenceStore) {
        return new SchedulerExternalEffectRecoveryStatusReader(
                new SchedulerRecoveryStatusReader(
                        queueStore,
                        runtimeStore,
                        checkpointStore,
                        new NoRunRecordStore()),
                runtimeStore,
                effectStore,
                evidenceStore);
    }

    private SchedulerQueueStore queueStore(
            SchedulerQueueState... states) {
        ArrayDeque<SchedulerQueueState> values =
                new ArrayDeque<>(List.of(states));
        SchedulerQueueState fallback = states[states.length - 1];
        return new SchedulerQueueStore() {
            @Override
            public void create(SchedulerQueueState state) {
                throw new AssertionError("read must not create a queue");
            }

            @Override
            public void update(SchedulerQueueState state) {
                throw new AssertionError("read must not update a queue");
            }

            @Override
            public SchedulerQueueState resolve(String queueId) {
                return values.isEmpty()
                        ? fallback : values.removeFirst();
            }
        };
    }

    private PendingFinalizationStore checkpointStore(
            Optional<PendingFinalization> pending) {
        return new PendingFinalizationStore() {
            @Override
            public void record(PendingFinalization value) {
                throw new AssertionError(
                        "read must not record a checkpoint");
            }

            @Override
            public Optional<PendingFinalization> findPending() {
                return pending;
            }

            @Override
            public void clear() {
                throw new AssertionError(
                        "read must not clear a checkpoint");
            }
        };
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
                Set.of(),
                Set.of());
    }

    private SchedulerQueueState queueWith(WorkItem workItem) {
        SingleWorkerSchedulerQueue queue =
                new SingleWorkerSchedulerQueue(4);
        queue.enqueue(new QueuedWork(workItem, List.of()));
        return queue.snapshot(QUEUE_ID, 1);
    }

    private PendingFinalization pending() {
        return new PendingFinalization(
                GOAL_ID, AGENT_RUN_ID, Optional.empty());
    }

    private AgentRuntimeState executing(WorkItem workItem) {
        return AgentRuntimeState.initial(GOAL_ID, workItem)
                .beginAgentRun(AGENT_RUN_ID)
                .markReady(AGENT_RUN_ID)
                .acquireLease(
                        AGENT_RUN_ID,
                        "owner",
                        NOW,
                        Duration.ofMinutes(5));
    }

    private ExternalEffectLedgerState ledger(
            long revision,
            List<ExternalEffectRecord> records) {
        return new ExternalEffectLedgerState(
                ExternalEffectLedgerState.CURRENT_SCHEMA_VERSION,
                GOAL_ID,
                revision,
                records);
    }

    private ExternalEffectRequest request(String key) {
        return new ExternalEffectRequest(
                key,
                GOAL_ID,
                AGENT_RUN_ID,
                WORK_ITEM_ID,
                "adapter",
                "operation",
                "c".repeat(64));
    }

    private WorkItem workItem() {
        return new WorkItem(
                WORK_ITEM_ID,
                "read-file-worker",
                new MessageEnvelope(
                        "00000000-0000-0000-0000-000000000997",
                        "effect-reader-correlation",
                        Optional.empty(),
                        "effect-reader-run",
                        "effect-reader-test",
                        NOW,
                        new WorkPayload(
                                new ApprovedTaskRevision(
                                        "effect-reader-test",
                                        "CURRENT_TASK.md",
                                        "d".repeat(64)),
                                "e".repeat(64),
                                Set.of("read-file"))));
    }

    private static final class CountingRuntimeStore
            implements AgentRuntimeStateStore {
        private final Optional<AgentRuntimeState> state;
        private int resolutions;

        private CountingRuntimeStore(
                Optional<AgentRuntimeState> state) {
            this.state = state;
        }

        @Override
        public void create(AgentRuntimeState initialState) {
            throw new AssertionError("read must not create runtime");
        }

        @Override
        public void update(AgentRuntimeState nextState) {
            throw new AssertionError("read must not update runtime");
        }

        @Override
        public AgentRuntimeState resolve(String goalId)
                throws IOException {
            resolutions++;
            if (state.isEmpty()) {
                throw new MissingAgentRuntimeStateException(goalId);
            }
            return state.orElseThrow();
        }
    }

    private static final class CountingEffectStore
            implements ExternalEffectLedgerStore {
        private final ArrayDeque<ExternalEffectLedgerState> states;
        private int resolutions;

        private CountingEffectStore(
                ExternalEffectLedgerState... states) {
            this.states = new ArrayDeque<>(List.of(states));
        }

        @Override
        public void create(ExternalEffectLedgerState state) {
            throw new AssertionError("read must not create a ledger");
        }

        @Override
        public void update(ExternalEffectLedgerState state) {
            throw new AssertionError("read must not update a ledger");
        }

        @Override
        public ExternalEffectLedgerState resolve(String goalId)
                throws IOException {
            resolutions++;
            if (states.isEmpty()) {
                throw new MissingExternalEffectLedgerException(goalId);
            }
            ExternalEffectLedgerState value = states.size() == 1
                    ? states.peekFirst() : states.removeFirst();
            return value;
        }
    }

    private static final class CountingEvidenceStore
            implements EvidenceStore {
        private final String digest;
        private int resolutions;

        private CountingEvidenceStore(String digest) {
            this.digest = digest;
        }

        @Override
        public String createRun() {
            throw new AssertionError(
                    "read must not create an evidence run");
        }

        @Override
        public StoredEvidence persist(
                String runId,
                String content) {
            throw new AssertionError(
                    "read must not persist evidence");
        }

        @Override
        public ResolvedEvidence resolve(String reference) {
            resolutions++;
            return new ResolvedEvidence(
                    new StoredEvidence(
                            EVIDENCE_RUN_ID,
                            EVIDENCE_ID,
                            EVIDENCE_REFERENCE,
                            NOW,
                            7,
                            digest),
                    "outcome");
        }

        @Override
        public EvidenceStoragePolicy storagePolicy() {
            return new EvidenceStoragePolicy(4096);
        }
    }

    private static final class NoRunRecordStore
            implements RunRecordStore {
        @Override
        public StoredRunRecord persist(RunRecord record) {
            throw new AssertionError(
                    "read must not persist a RunRecord");
        }

        @Override
        public ResolvedRunRecord resolve(String reference) {
            throw new AssertionError(
                    "read must not resolve an absent reference");
        }

        @Override
        public List<String> references() {
            throw new AssertionError(
                    "read must not scan RunRecords");
        }

        @Override
        public List<String> recentReferences(int limit) {
            throw new AssertionError(
                    "read must not scan RunRecords");
        }
    }
}
