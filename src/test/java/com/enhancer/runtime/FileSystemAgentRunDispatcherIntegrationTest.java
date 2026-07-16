package com.enhancer.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.enhancer.bus.MessageEnvelope;
import com.enhancer.bus.WorkPayload;
import com.enhancer.workspace.ApprovedTaskRevision;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FileSystemAgentRunDispatcherIntegrationTest {
    private static final String QUEUE_ID =
            "00000000-0000-0000-0000-000000000701";
    private static final String GOAL_ID =
            "00000000-0000-0000-0000-000000000702";
    private static final String AGENT_RUN_ID =
            "00000000-0000-0000-0000-000000000703";
    private static final String WORK_ITEM_ID =
            "00000000-0000-0000-0000-000000000704";
    private static final String WORK_MESSAGE_ID =
            "00000000-0000-0000-0000-000000000705";

    @TempDir
    Path temporaryRoot;

    @Test
    void recoversBothStoresAndResumesTheExactExistingLease()
            throws Exception {
        Path queueRoot = temporaryRoot.resolve("queues");
        Path runtimeRoot = temporaryRoot.resolve("runtime");
        Clock clock = Clock.fixed(
                Instant.parse("2026-07-16T15:00:00Z"),
                ZoneOffset.UTC);
        FileSystemSchedulerQueueStore queueStore =
                new FileSystemSchedulerQueueStore(queueRoot);
        DurableSingleWorkerSchedulerQueue queue =
                DurableSingleWorkerSchedulerQueue.create(
                        QUEUE_ID,
                        8,
                        queueStore);
        WorkItem workItem = workItem();
        queue.enqueue(new QueuedWork(workItem, List.of()));
        AgentRunLease first = new DurableAgentRunDispatcher(
                queue,
                new FileSystemAgentRuntimeStateStore(runtimeRoot),
                clock).claimAndLease(
                        GOAL_ID,
                        AGENT_RUN_ID,
                        "filesystem-dispatch-\uD83D\uDE80",
                        Duration.ofMinutes(5))
                .orElseThrow()
                .lease();

        DurableSingleWorkerSchedulerQueue recoveredQueue =
                DurableSingleWorkerSchedulerQueue.recover(
                        QUEUE_ID,
                        new FileSystemSchedulerQueueStore(queueRoot));
        assertTrue(recoveredQueue.activeWork().isEmpty());
        assertEquals(1, recoveredQueue.pendingCount());

        AgentRunDispatch recovered = new DurableAgentRunDispatcher(
                recoveredQueue,
                new FileSystemAgentRuntimeStateStore(runtimeRoot),
                Clock.fixed(
                        Instant.parse("2026-07-16T15:04:59Z"),
                        ZoneOffset.UTC)).claimAndLease(
                                GOAL_ID,
                                AGENT_RUN_ID,
                                first.ownerId(),
                                Duration.ofMinutes(10))
                .orElseThrow();

        assertEquals(workItem, recovered.workItem());
        assertEquals(first, recovered.lease());
        assertEquals(Optional.of(workItem), recoveredQueue.activeWork());
        DurableAgentRuntime runtime = DurableAgentRuntime.recover(
                GOAL_ID,
                new FileSystemAgentRuntimeStateStore(runtimeRoot),
                Clock.fixed(
                        Instant.parse("2026-07-16T15:04:59Z"),
                        ZoneOffset.UTC));
        assertEquals(3, runtime.revision());
        assertEquals(
                RuntimeAgentRunStatus.EXECUTING,
                runtime.agentRun().orElseThrow().status());
    }

    private static WorkItem workItem() {
        return new WorkItem(
                WORK_ITEM_ID,
                "filesystem-runtime-dispatch",
                new MessageEnvelope(
                        WORK_MESSAGE_ID,
                        "correlation-filesystem-dispatch",
                        Optional.empty(),
                        "logical-filesystem-dispatch",
                        "filesystem-dispatch-test",
                        Instant.parse("2026-07-16T14:00:00Z"),
                        new WorkPayload(
                                new ApprovedTaskRevision(
                                        "gate-8-durable-queue-runtime-dispatch",
                                        "CURRENT_TASK.md",
                                        "a".repeat(64)),
                                "b".repeat(64),
                                Set.of("read-file", "verify"))));
    }
}
