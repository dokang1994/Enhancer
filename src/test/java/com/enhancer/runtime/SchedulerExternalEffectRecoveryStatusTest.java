package com.enhancer.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.enhancer.bus.MessageEnvelope;
import com.enhancer.bus.WorkPayload;
import com.enhancer.workspace.ApprovedTaskRevision;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class SchedulerExternalEffectRecoveryStatusTest {
    private static final String QUEUE_ID =
            "00000000-0000-0000-0000-000000000981";
    private static final String WORK_ITEM_ID =
            "00000000-0000-0000-0000-000000000982";
    private static final String GOAL_ID =
            "00000000-0000-0000-0000-000000000983";
    private static final String AGENT_RUN_ID =
            "00000000-0000-0000-0000-000000000984";
    private static final String OTHER_AGENT_RUN_ID =
            "00000000-0000-0000-0000-000000000985";
    private static final String OTHER_WORK_ITEM_ID =
            "00000000-0000-0000-0000-000000000986";
    private static final Instant NOW =
            Instant.parse("2026-07-23T09:00:00Z");

    @Test
    void distinguishesUncorrelatedIntentRuntimeAndEmptyLedgerPrefixes() {
        WorkItem workItem = workItem();
        PendingFinalization pending = new PendingFinalization(
                GOAL_ID, AGENT_RUN_ID, Optional.empty());
        AgentRuntimeState runtime = executing(workItem);

        SchedulerExternalEffectRecoveryStatus uncorrelated =
                SchedulerExternalEffectRecoveryStatus.project(
                        scheduler(Optional.empty(), Optional.empty()),
                        Optional.empty(),
                        Optional.empty(),
                        0);
        SchedulerExternalEffectRecoveryStatus intent =
                SchedulerExternalEffectRecoveryStatus.project(
                        scheduler(
                                Optional.of(pending),
                                Optional.empty()),
                        Optional.empty(),
                        Optional.empty(),
                        0);
        SchedulerExternalEffectRecoveryStatus creation =
                SchedulerExternalEffectRecoveryStatus.project(
                        scheduler(
                                Optional.of(pending),
                                Optional.of(runtime)),
                        Optional.of(runtime),
                        Optional.empty(),
                        0);
        SchedulerExternalEffectRecoveryStatus empty =
                SchedulerExternalEffectRecoveryStatus.project(
                        scheduler(
                                Optional.of(pending),
                                Optional.of(runtime)),
                        Optional.of(runtime),
                        Optional.of(
                                ExternalEffectLedgerState.initial(
                                        GOAL_ID)),
                        0);

        assertEquals(
                SchedulerExternalEffectRecoveryStatus.RecoveryPhase
                        .NO_CORRELATED_GOAL,
                uncorrelated.phase());
        assertEquals(
                SchedulerExternalEffectRecoveryStatus.RecoveryPhase
                        .LEDGER_NOT_RECORDED,
                intent.phase());
        assertEquals(
                SchedulerExternalEffectRecoveryStatus.RecoveryPhase
                        .LEDGER_CREATION_PENDING,
                creation.phase());
        assertEquals(
                SchedulerExternalEffectRecoveryStatus.RecoveryPhase
                        .EMPTY_LEDGER,
                empty.phase());
        assertEquals(Optional.of(GOAL_ID), empty.goalId());
        assertEquals(Optional.of(0L), empty.ledgerRevision());
        assertEquals(0, empty.effects().size());
    }

    @Test
    void appliesRetrySafetyPrecedenceAcrossEffectOutcomes() {
        AgentRuntimeState runtime = executing(workItem());
        SchedulerRecoveryStatus scheduler = scheduler(
                Optional.of(new PendingFinalization(
                        GOAL_ID,
                        AGENT_RUN_ID,
                        Optional.empty())),
                Optional.of(runtime));

        SchedulerExternalEffectRecoveryStatus prepared = project(
                scheduler,
                runtime,
                List.of(
                        terminal("compensated",
                                ExternalEffectStatus.COMPENSATED),
                        terminal("applied",
                                ExternalEffectStatus.APPLIED),
                        terminal("user",
                                ExternalEffectStatus.REQUIRES_USER_RECOVERY),
                        prepared("prepared")),
                3);
        SchedulerExternalEffectRecoveryStatus user = project(
                scheduler,
                runtime,
                List.of(
                        terminal("compensated",
                                ExternalEffectStatus.COMPENSATED),
                        terminal("applied",
                                ExternalEffectStatus.APPLIED),
                        terminal("user",
                                ExternalEffectStatus.REQUIRES_USER_RECOVERY)),
                3);
        SchedulerExternalEffectRecoveryStatus nonCompensated = project(
                scheduler,
                runtime,
                List.of(
                        terminal("compensated",
                                ExternalEffectStatus.COMPENSATED),
                        terminal("deduplicated",
                                ExternalEffectStatus.DEDUPLICATED)),
                2);
        SchedulerExternalEffectRecoveryStatus compensated = project(
                scheduler,
                runtime,
                List.of(terminal(
                        "compensated",
                        ExternalEffectStatus.COMPENSATED)),
                1);

        assertEquals(
                SchedulerExternalEffectRecoveryStatus.RecoveryPhase
                        .PREPARED_EFFECT_REQUIRES_RECOVERY,
                prepared.phase());
        assertEquals(
                SchedulerExternalEffectRecoveryStatus.RecoveryPhase
                        .USER_RECOVERY_REQUIRED,
                user.phase());
        assertEquals(
                SchedulerExternalEffectRecoveryStatus.RecoveryPhase
                        .NON_COMPENSATED_EFFECT_RECORDED,
                nonCompensated.phase());
        assertEquals(
                SchedulerExternalEffectRecoveryStatus.RecoveryPhase
                        .ALL_EFFECTS_COMPENSATED,
                compensated.phase());
        assertEquals(
                1,
                prepared.count(ExternalEffectStatus.PREPARED));
        assertEquals(3, prepared.verifiedTerminalEvidence());
        assertEquals("prepared",
                prepared.effects().get(3).idempotencyKey());
    }

    @Test
    void rejectsEffectsOutsideTheCorrelatedWorkAndAgentRunHistory() {
        AgentRuntimeState runtime = executing(workItem());
        SchedulerRecoveryStatus scheduler = scheduler(
                Optional.of(new PendingFinalization(
                        GOAL_ID,
                        AGENT_RUN_ID,
                        Optional.empty())),
                Optional.of(runtime));

        assertThrows(IllegalArgumentException.class, () ->
                project(
                        scheduler,
                        runtime,
                        List.of(prepared(
                                "wrong-work",
                                AGENT_RUN_ID,
                                OTHER_WORK_ITEM_ID)),
                        0));
        assertThrows(IllegalArgumentException.class, () ->
                project(
                        scheduler,
                        runtime,
                        List.of(prepared(
                                "wrong-run",
                                OTHER_AGENT_RUN_ID,
                                WORK_ITEM_ID)),
                        0));
    }

    @Test
    void requiresEveryTerminalEffectEvidenceToHaveBeenVerified() {
        AgentRuntimeState runtime = executing(workItem());
        SchedulerRecoveryStatus scheduler = scheduler(
                Optional.of(new PendingFinalization(
                        GOAL_ID,
                        AGENT_RUN_ID,
                        Optional.empty())),
                Optional.of(runtime));
        ExternalEffectLedgerState ledger = ledger(List.of(
                terminal("applied", ExternalEffectStatus.APPLIED)));

        assertThrows(IllegalArgumentException.class, () ->
                SchedulerExternalEffectRecoveryStatus.project(
                        scheduler,
                        Optional.of(runtime),
                        Optional.of(ledger),
                        0));
    }

    private SchedulerExternalEffectRecoveryStatus project(
            SchedulerRecoveryStatus scheduler,
            AgentRuntimeState runtime,
            List<ExternalEffectRecord> records,
            int verifiedEvidence) {
        return SchedulerExternalEffectRecoveryStatus.project(
                scheduler,
                Optional.of(runtime),
                Optional.of(ledger(records)),
                verifiedEvidence);
    }

    private ExternalEffectLedgerState ledger(
            List<ExternalEffectRecord> records) {
        return new ExternalEffectLedgerState(
                ExternalEffectLedgerState.CURRENT_SCHEMA_VERSION,
                GOAL_ID,
                records.size(),
                records);
    }

    private ExternalEffectRecord prepared(String key) {
        return prepared(key, AGENT_RUN_ID, WORK_ITEM_ID);
    }

    private ExternalEffectRecord prepared(
            String key,
            String agentRunId,
            String workItemId) {
        return new ExternalEffectRecord(new ExternalEffectRequest(
                key,
                GOAL_ID,
                agentRunId,
                workItemId,
                "adapter",
                "operation",
                "c".repeat(64)),
                ExternalEffectStatus.PREPARED);
    }

    private ExternalEffectRecord terminal(
            String key,
            ExternalEffectStatus status) {
        return new ExternalEffectRecord(
                prepared(key).request(),
                status,
                Optional.of(new ExternalEffectOutcomeEvidence(
                        "evidence/00000000-0000-0000-0000-000000000987/"
                                + evidenceId(key),
                        "d".repeat(64))));
    }

    private String evidenceId(String key) {
        int suffix = Math.abs(key.hashCode() % 900) + 100;
        return "00000000-0000-0000-0000-000000000" + suffix;
    }

    private SchedulerRecoveryStatus scheduler(
            Optional<PendingFinalization> pending,
            Optional<AgentRuntimeState> runtime) {
        WorkItem workItem = workItem();
        SchedulerQueueState queue = pending.isEmpty()
                ? SchedulerQueueState.initial(QUEUE_ID, 4)
                : queueState(workItem);
        return SchedulerRecoveryStatus.project(
                queue, pending, runtime, Optional.empty());
    }

    private SchedulerQueueState queueState(WorkItem workItem) {
        SingleWorkerSchedulerQueue queue =
                new SingleWorkerSchedulerQueue(4);
        queue.enqueue(new QueuedWork(workItem, List.of()));
        return queue.snapshot(QUEUE_ID, 1);
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

    private WorkItem workItem() {
        MessageEnvelope envelope = new MessageEnvelope(
                "00000000-0000-0000-0000-000000000988",
                "external-effect-recovery-correlation",
                Optional.empty(),
                "external-effect-recovery-run",
                "external-effect-recovery-test",
                NOW,
                new WorkPayload(
                        new ApprovedTaskRevision(
                                "external-effect-recovery-test",
                                "CURRENT_TASK.md",
                                "a".repeat(64)),
                        "b".repeat(64),
                        Set.of("read-file")));
        return new WorkItem(
                WORK_ITEM_ID, "read-file-worker", envelope);
    }
}
