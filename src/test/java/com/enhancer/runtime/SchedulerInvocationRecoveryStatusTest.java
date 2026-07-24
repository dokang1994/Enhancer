package com.enhancer.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.enhancer.bus.MessageEnvelope;
import com.enhancer.bus.WorkPayload;
import com.enhancer.workspace.ApprovedTaskRevision;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SchedulerInvocationRecoveryStatusTest {
    private static final String QUEUE_ID =
            "00000000-0000-0000-0000-000000000c01";
    private static final String WORK_ITEM_ID =
            "00000000-0000-0000-0000-000000000c02";
    private static final String GOAL_ID =
            "00000000-0000-0000-0000-000000000c03";
    private static final String AGENT_RUN_ID =
            "00000000-0000-0000-0000-000000000c04";
    private static final Instant NOW =
            Instant.parse("2026-07-24T12:00:00Z");

    @TempDir
    Path temporaryRoot;

    @Test
    void appliesPhasePrecedenceAndRejectsResultWithoutWork() throws Exception {
        WorkItem work = workItem();
        QueuedWork queued = new QueuedWork(work, List.of());
        SchedulerQueueState queue = new SchedulerQueueState(
                SchedulerQueueState.CURRENT_SCHEMA_VERSION,
                QUEUE_ID,
                1,
                16,
                Optional.of(work.logicalRunId()),
                List.of(WORK_ITEM_ID),
                List.of(queued),
                List.of(queued),
                Optional.empty(),
                Set.of(),
                Set.of());
        FileSystemAgentRuntimeStateStore store =
                new FileSystemAgentRuntimeStateStore(
                        temporaryRoot.resolve("runtime"));
        DurableAgentRuntime runtime = DurableAgentRuntime.create(
                GOAL_ID,
                work,
                store,
                Clock.fixed(NOW, ZoneOffset.UTC));
        runtime.beginAgentRun(AGENT_RUN_ID);
        AgentRuntimeState state = store.resolve(GOAL_ID);
        SchedulerRecoveryStatus scheduler = SchedulerRecoveryStatus.project(
                queue,
                Optional.of(new PendingFinalization(
                        GOAL_ID, AGENT_RUN_ID, Optional.empty())),
                Optional.of(state),
                Optional.empty());

        assertPhase(
                scheduler,
                state,
                new SchedulerInvocationRecoveryStatus.InvocationSpoolState(
                        false, false, false),
                SchedulerInvocationRecoveryStatus.RecoveryPhase.INVOCATION_ABSENT);
        assertPhase(
                scheduler,
                state,
                new SchedulerInvocationRecoveryStatus.InvocationSpoolState(
                        true, false, false),
                SchedulerInvocationRecoveryStatus.RecoveryPhase.WORK_MESSAGE_ABSENT);
        assertPhase(
                scheduler,
                state,
                new SchedulerInvocationRecoveryStatus.InvocationSpoolState(
                        true, true, false),
                SchedulerInvocationRecoveryStatus.RecoveryPhase
                        .WORK_MESSAGE_AWAITING_RESULT);
        assertPhase(
                scheduler,
                state,
                new SchedulerInvocationRecoveryStatus.InvocationSpoolState(
                        true, true, true),
                SchedulerInvocationRecoveryStatus.RecoveryPhase
                        .RESULT_MESSAGE_PUBLISHED);
        assertThrows(
                IllegalArgumentException.class,
                () -> SchedulerInvocationRecoveryStatus.project(
                        scheduler,
                        Optional.of(state),
                        Optional.of(new SchedulerInvocationRecoveryStatus
                                .InvocationSpoolState(true, false, true))));
    }

    private static void assertPhase(
            SchedulerRecoveryStatus scheduler,
            AgentRuntimeState runtime,
            SchedulerInvocationRecoveryStatus.InvocationSpoolState spool,
            SchedulerInvocationRecoveryStatus.RecoveryPhase expected) {
        assertEquals(
                expected,
                SchedulerInvocationRecoveryStatus.project(
                        scheduler,
                        Optional.of(runtime),
                        Optional.of(spool))
                        .phase());
    }

    private static WorkItem workItem() {
        return new WorkItem(
                WORK_ITEM_ID,
                "read-file-worker",
                new MessageEnvelope(
                        "00000000-0000-0000-0000-000000000c05",
                        "invocation-projection-correlation",
                        Optional.empty(),
                        "invocation-projection-run",
                        "invocation-projection-test",
                        NOW,
                        new WorkPayload(
                                new ApprovedTaskRevision(
                                        "invocation-projection-test",
                                        "CURRENT_TASK.md",
                                        "a".repeat(64)),
                                "b".repeat(64),
                                Set.of("read-file"))));
    }
}
