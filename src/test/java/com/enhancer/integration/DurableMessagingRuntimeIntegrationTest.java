package com.enhancer.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.enhancer.bus.DeliveryDestination;
import com.enhancer.bus.DeliveryStatus;
import com.enhancer.bus.InProcessMessageBus;
import com.enhancer.context.ProjectContext;
import com.enhancer.context.ProjectContextReader;
import com.enhancer.context.RequiredProjectDocument;
import com.enhancer.loop.ApprovedTask;
import com.enhancer.loop.ApprovedTaskReader;
import com.enhancer.runtime.DurableSingleWorkerSchedulerQueue;
import com.enhancer.runtime.DurableWorkItemAdmissionHandler;
import com.enhancer.runtime.FileSystemSchedulerQueueStore;
import com.enhancer.runtime.WorkItem;
import com.enhancer.runtime.WorkMessagePublisher;
import com.enhancer.workspace.RepositoryMemorySnapshotCollector;
import com.enhancer.workspace.WorkspaceSnapshot;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DurableMessagingRuntimeIntegrationTest {
    private static final String TASK_ID =
            "connect-work-admission-to-durable-queue";
    private static final String MESSAGE_ID =
            "00000000-0000-0000-0000-000000000711";
    private static final String QUEUE_ID =
            "00000000-0000-0000-0000-000000000712";
    private static final Instant OCCURRED_AT =
            Instant.parse("2026-07-22T11:00:00Z");
    private static final DeliveryDestination DESTINATION =
            DeliveryDestination.queue("durable-read-file-worker");

    @TempDir
    Path temporaryRoot;

    @Test
    void carriesRealApprovedWorkspaceThroughBusIntoRecoverableDurableQueue()
            throws Exception {
        Path projectRoot = temporaryRoot.resolve("project");
        writeGovernedProject(projectRoot);
        ProjectContext context = new ProjectContextReader().read(projectRoot);
        ApprovedTask approvedTask = new ApprovedTaskReader().read(context);
        WorkspaceSnapshot snapshot = new RepositoryMemorySnapshotCollector().collect(
                projectRoot, OCCURRED_AT, approvedTask, context);
        Path queueRoot = temporaryRoot.resolve("queue");
        DurableSingleWorkerSchedulerQueue queue =
                DurableSingleWorkerSchedulerQueue.create(
                        QUEUE_ID,
                        8,
                        new FileSystemSchedulerQueueStore(queueRoot));
        InProcessMessageBus bus = new InProcessMessageBus();
        bus.subscribe(
                DESTINATION,
                "durable-gate-8-admission",
                new DurableWorkItemAdmissionHandler("read-file-worker", queue));
        WorkMessagePublisher publisher = new WorkMessagePublisher(bus, DESTINATION);

        assertEquals(
                DeliveryStatus.DELIVERED,
                publisher.publish(
                                snapshot,
                                approvedTask,
                                MESSAGE_ID,
                                "correlation-durable-admission",
                                Optional.empty(),
                                "logical-run-durable-admission",
                                "workspace-durable-admission",
                                OCCURRED_AT)
                        .get(0)
                        .status());
        assertEquals(
                List.of(DeliveryStatus.DUPLICATE),
                bus.replay(bus.journal()).stream()
                        .map(outcome -> outcome.status())
                        .toList());

        DurableSingleWorkerSchedulerQueue recovered =
                DurableSingleWorkerSchedulerQueue.recover(
                        QUEUE_ID,
                        new FileSystemSchedulerQueueStore(queueRoot));
        WorkItem admitted = recovered.claimNext().orElseThrow();
        assertEquals(bus.journal().get(0).envelope(), admitted.workMessage());
        assertEquals(snapshot.approvedTaskRevision(), admitted.taskRevision());
        assertEquals(snapshot.snapshotId(), admitted.snapshotId());
        assertEquals(approvedTask.allowedTools(), admitted.allowedTools());
        assertEquals("logical-run-durable-admission", admitted.logicalRunId());
    }

    private void writeGovernedProject(Path projectRoot) throws Exception {
        for (RequiredProjectDocument document : RequiredProjectDocument.values()) {
            Path path = projectRoot.resolve(document.path());
            Files.createDirectories(path.getParent());
            String content = document == RequiredProjectDocument.CURRENT_TASK
                    ? activeTask()
                    : "# " + document.name() + "\n";
            Files.writeString(path, content, StandardCharsets.UTF_8);
        }
    }

    private String activeTask() {
        return "# Current Task\n\n"
                + "## Status\n\nIn Progress\n\n"
                + "## Task\n\nPersist delivered work in the Scheduler queue.\n\n"
                + "## Task ID\n\n" + TASK_ID + "\n\n"
                + "## Approval\n\nApproved by the integration-test owner.\n\n"
                + "## Allowed Tools\n\n- read-file\n";
    }
}
