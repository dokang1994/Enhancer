package com.enhancer.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.enhancer.bus.DeliveryDestination;
import com.enhancer.bus.DeliveryOutcome;
import com.enhancer.bus.DeliveryStatus;
import com.enhancer.bus.InProcessMessageBus;
import com.enhancer.context.ProjectContext;
import com.enhancer.context.ProjectContextReader;
import com.enhancer.context.RequiredProjectDocument;
import com.enhancer.loop.ApprovedTask;
import com.enhancer.loop.ApprovedTaskReader;
import com.enhancer.runtime.WorkItem;
import com.enhancer.runtime.WorkItemAdmissionHandler;
import com.enhancer.runtime.WorkMessagePublisher;
import com.enhancer.workspace.RepositoryMemorySnapshotCollector;
import com.enhancer.workspace.WorkspaceSnapshot;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MessagingRuntimeIntegrationTest {
    private static final String TASK_ID = "gate-7-runtime-integration-test";
    private static final String MESSAGE_ID =
            "00000000-0000-0000-0000-00000000000a";
    private static final String WORK_ITEM_ID =
            "00000000-0000-0000-0000-00000000000b";
    private static final Instant OCCURRED_AT = Instant.parse("2026-07-16T12:00:00Z");

    @TempDir
    Path temporaryRoot;

    @Test
    void carriesARealApprovedWorkspaceThroughTheBusIntoRuntimeAdmission()
            throws Exception {
        Path projectRoot = temporaryRoot.resolve("project");
        writeGovernedProject(projectRoot);
        ProjectContext context = new ProjectContextReader().read(projectRoot);
        ApprovedTask approvedTask = new ApprovedTaskReader().read(context);
        WorkspaceSnapshot snapshot = new RepositoryMemorySnapshotCollector().collect(
                projectRoot,
                OCCURRED_AT,
                approvedTask,
                context);

        InProcessMessageBus bus = new InProcessMessageBus();
        DeliveryDestination workQueue = DeliveryDestination.queue("read-file-worker");
        List<WorkItem> admitted = new ArrayList<>();
        bus.subscribe(
                workQueue,
                "gate-8-work-admission",
                new WorkItemAdmissionHandler(
                        "read-file-worker",
                        () -> WORK_ITEM_ID,
                        admitted::add));
        WorkMessagePublisher publisher = new WorkMessagePublisher(bus, workQueue);

        ApprovedTask mismatched = new ApprovedTask(
                "different-task",
                approvedTask.description(),
                approvedTask.approvalEvidence(),
                approvedTask.allowedTools(),
                approvedTask.sourceDocument());
        assertThrows(IllegalArgumentException.class, () -> publisher.publish(
                snapshot,
                mismatched,
                MESSAGE_ID,
                "correlation-1",
                Optional.empty(),
                "logical-run-1",
                "workspace-runtime-bridge",
                OCCURRED_AT));
        ApprovedTask mismatchedSource = new ApprovedTask(
                approvedTask.taskId(),
                approvedTask.description(),
                approvedTask.approvalEvidence(),
                approvedTask.allowedTools(),
                "AGENTS.md");
        assertThrows(IllegalArgumentException.class, () -> publisher.publish(
                snapshot,
                mismatchedSource,
                MESSAGE_ID,
                "correlation-1",
                Optional.empty(),
                "logical-run-1",
                "workspace-runtime-bridge",
                OCCURRED_AT));
        assertThrows(IllegalArgumentException.class, () -> publisher.publish(
                snapshot,
                approvedTask,
                MESSAGE_ID,
                "correlation-1",
                Optional.empty(),
                "logical-run-1",
                "workspace-runtime-bridge",
                OCCURRED_AT.minusNanos(1)));
        assertEquals(List.of(), bus.journal());

        List<DeliveryOutcome> outcomes = publisher.publish(
                snapshot,
                approvedTask,
                MESSAGE_ID,
                "correlation-1",
                Optional.empty(),
                "logical-run-1",
                "workspace-runtime-bridge",
                OCCURRED_AT);

        assertEquals(1, outcomes.size());
        assertEquals(DeliveryStatus.DELIVERED, outcomes.get(0).status());
        assertEquals(1, bus.journal().size());
        assertEquals(1, admitted.size());
        WorkItem workItem = admitted.get(0);
        assertEquals(WORK_ITEM_ID, workItem.workItemId());
        assertEquals("read-file-worker", workItem.requiredCapability());
        assertSame(bus.journal().get(0).envelope(), workItem.workMessage());
        assertEquals(snapshot.snapshotId(), workItem.snapshotId());
        assertEquals(snapshot.approvedTaskRevision(), workItem.taskRevision());
        assertEquals(approvedTask.allowedTools(), workItem.allowedTools());
        assertEquals(Optional.empty(), workItem.executionInput());
        assertEquals("logical-run-1", workItem.logicalRunId());
        assertEquals("correlation-1", workItem.workMessage().correlationId());
        assertEquals("workspace-runtime-bridge", workItem.workMessage().producer());
        assertEquals(OCCURRED_AT, workItem.workMessage().occurredAt());

        List<DeliveryOutcome> replay = bus.replay(bus.journal());
        assertEquals(1, replay.size());
        assertEquals(DeliveryStatus.DUPLICATE, replay.get(0).status());
        assertEquals(1, admitted.size());

        com.enhancer.bus.WorkPayload.ExecutionInput executionInput =
                new com.enhancer.bus.WorkPayload.ExecutionInput(
                        "docs/target.md", "e".repeat(64));
        List<DeliveryOutcome> declared = publisher.publish(
                snapshot,
                approvedTask,
                Optional.of(executionInput),
                "00000000-0000-0000-0000-0000000000ff",
                "correlation-1",
                Optional.empty(),
                "logical-run-1",
                "workspace-runtime-bridge",
                OCCURRED_AT);
        assertEquals(DeliveryStatus.DELIVERED, declared.get(0).status());
        assertEquals(2, admitted.size());
        assertEquals(Optional.of(executionInput), admitted.get(1).executionInput());
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
                + "## Task\n\nIntegrate the messaging runtime path.\n\n"
                + "## Task ID\n\n" + TASK_ID + "\n\n"
                + "## Approval\n\nApproved by the integration-test owner.\n\n"
                + "## Allowed Tools\n\n- read-file\n- inspect-metadata\n";
    }
}
