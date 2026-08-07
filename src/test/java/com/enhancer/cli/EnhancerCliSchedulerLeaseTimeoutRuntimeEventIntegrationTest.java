package com.enhancer.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.enhancer.bus.MessageEnvelope;
import com.enhancer.bus.WorkPayload;
import com.enhancer.run.FileSystemRunRecordStore;
import com.enhancer.runtime.AgentRunDispatch;
import com.enhancer.runtime.DurableAgentRunDispatcher;
import com.enhancer.runtime.DurableAgentRuntime;
import com.enhancer.runtime.DurableSingleWorkerSchedulerQueue;
import com.enhancer.runtime.FileSystemAgentRuntimeStateStore;
import com.enhancer.runtime.FileSystemPendingFinalizationStore;
import com.enhancer.runtime.FileSystemRuntimeEventPublisher;
import com.enhancer.runtime.FileSystemRuntimeEventStore;
import com.enhancer.runtime.FileSystemSchedulerQueueStore;
import com.enhancer.runtime.LeaseTimeoutRecord;
import com.enhancer.runtime.PendingFinalization;
import com.enhancer.runtime.QueuedWork;
import com.enhancer.runtime.RuntimeEvent;
import com.enhancer.runtime.RuntimeEventBinding;
import com.enhancer.runtime.RuntimeEventDetail;
import com.enhancer.runtime.RuntimeEventKind;
import com.enhancer.runtime.RuntimeEventReference;
import com.enhancer.runtime.RuntimeEventReferenceKind;
import com.enhancer.runtime.RuntimeEventStream;
import com.enhancer.runtime.RuntimeTimeoutKind;
import com.enhancer.runtime.WorkItem;
import com.enhancer.workspace.ApprovedTaskRevision;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class EnhancerCliSchedulerLeaseTimeoutRuntimeEventIntegrationTest {
    private static final String QUEUE_ID =
            "00000000-0000-0000-0000-000000001401";
    private static final String WORK_ID =
            "00000000-0000-0000-0000-000000001402";
    private static final String WORK_MESSAGE_ID =
            "00000000-0000-0000-0000-000000001403";
    private static final String GOAL_ID =
            "00000000-0000-0000-0000-000000001404";
    private static final String AGENT_RUN_ID =
            "00000000-0000-0000-0000-000000001405";
    private static final String MISSING_RUN_RECORD =
            "run-record/00000000-0000-0000-0000-000000001406";
    private static final Instant LEASE_ISSUED_AT =
            Instant.parse("2026-01-01T00:00:00Z");

    @TempDir
    Path temporaryRoot;

    @ParameterizedTest
    @ValueSource(strings = {
            "scheduler-cycle", "scheduler-drain", "scheduler-service"
    })
    void publishesOneRetainedLeaseTimeoutFromEverySchedulerExecutionCommand(
            String commandName) throws Exception {
        Layout layout = layout(commandName);
        SeededLease seeded = seedExpiredLease(layout, Optional.empty());

        Execution execution = execute(layout, commandName);

        assertEquals(0, execution.exitCode(), execution.stderr());
        DurableAgentRuntime runtime = DurableAgentRuntime.recover(
                GOAL_ID, seeded.runtimeStore(), Clock.systemUTC());
        assertEquals(1, runtime.leaseTimeouts().size());
        LeaseTimeoutRecord timeout = runtime.leaseTimeouts().get(0);
        assertEquals(seeded.dispatch().lease().expiresAt(), timeout.expiresAt());

        RuntimeEventStream stream = new FileSystemRuntimeEventStore(
                layout.eventRoot()).resolve(GOAL_ID);
        assertEquals(3, stream.revision());
        assertEquals(
                List.of(
                        RuntimeEventKind.TIMEOUT_DETECTED,
                        RuntimeEventKind.VERIFICATION_RECORDED,
                        RuntimeEventKind.WORK_ITEM_TERMINATED),
                stream.events().stream().map(RuntimeEvent::kind).toList());
        assertLeaseTimeoutEvent(stream.events().get(0), seeded.workItem(), timeout);
        List<Path> points = pendingPoints(layout.publicationRoot());
        assertEquals(3, points.size());
        StringBuilder pointOutput = new StringBuilder();
        for (Path point : points) {
            Execution read = executeRead(layout, point.getFileName().toString());
            assertEquals(0, read.exitCode(), read.stderr());
            assertTrue(read.stdout().contains("status=AVAILABLE"), read.stdout());
            assertTrue(read.stdout().contains("goalId=" + GOAL_ID), read.stdout());
            pointOutput.append(read.stdout());
        }
        assertTrue(pointOutput.toString().contains("kind=TIMEOUT_DETECTED"));
        assertTrue(pointOutput.toString().contains("kind=VERIFICATION_RECORDED"));
        assertTrue(pointOutput.toString().contains("kind=WORK_ITEM_TERMINATED"));

        assertEquals(Set.of(WORK_ID), recoverQueue(layout).completedWorkItemIds());
        assertEquals(1, new FileSystemRunRecordStore(layout.recordRoot())
                .references().size());
    }

    @Test
    void exactCycleReplayRepairsMissingArtifactsWithoutReexecutionOrAckRecreation()
            throws Exception {
        Layout layout = layout("exact-replay");
        SeededLease seeded = seedExpiredLease(
                layout, Optional.of(MISSING_RUN_RECORD));

        Execution first = execute(layout, "scheduler-cycle");

        assertNotEquals(0, first.exitCode());
        DurableAgentRuntime afterFirst = DurableAgentRuntime.recover(
                GOAL_ID, seeded.runtimeStore(), Clock.systemUTC());
        long sourceRevision = afterFirst.revision();
        List<LeaseTimeoutRecord> timeouts = afterFirst.leaseTimeouts();
        assertEquals(1, timeouts.size());
        assertTrue(new FileSystemRunRecordStore(layout.recordRoot())
                .references().isEmpty());
        Path eventArtifact = layout.eventRoot().resolve(
                GOAL_ID + ".runtime-events");
        Path pendingPoint = solePendingPoint(layout.publicationRoot());
        Files.delete(eventArtifact);
        Files.delete(pendingPoint);

        Execution repaired = execute(layout, "scheduler-cycle");

        assertNotEquals(0, repaired.exitCode());
        DurableAgentRuntime afterRepair = DurableAgentRuntime.recover(
                GOAL_ID, seeded.runtimeStore(), Clock.systemUTC());
        assertEquals(sourceRevision, afterRepair.revision());
        assertEquals(timeouts, afterRepair.leaseTimeouts());
        assertEquals(1, new FileSystemRuntimeEventStore(layout.eventRoot())
                .resolve(GOAL_ID).revision());
        Path repairedPoint = solePendingPoint(layout.publicationRoot());
        assertTrue(new FileSystemRunRecordStore(layout.recordRoot())
                .references().isEmpty());

        Execution acknowledged = executeAcknowledge(
                layout, repairedPoint.getFileName().toString());
        assertEquals(0, acknowledged.exitCode(), acknowledged.stderr());
        Path acknowledgedPoint = soleAcknowledgedPoint(layout.publicationRoot());
        byte[] acknowledgedBytes = Files.readAllBytes(acknowledgedPoint);
        FileTime acknowledgedTime = Files.getLastModifiedTime(acknowledgedPoint);

        Execution replayed = execute(layout, "scheduler-cycle");

        assertNotEquals(0, replayed.exitCode());
        DurableAgentRuntime afterReplay = DurableAgentRuntime.recover(
                GOAL_ID, seeded.runtimeStore(), Clock.systemUTC());
        assertEquals(sourceRevision, afterReplay.revision());
        assertEquals(timeouts, afterReplay.leaseTimeouts());
        assertEquals(List.of(), pendingPoints(layout.publicationRoot()));
        assertEquals(List.of(acknowledgedPoint),
                acknowledgedPoints(layout.publicationRoot()));
        assertTrue(java.util.Arrays.equals(
                acknowledgedBytes, Files.readAllBytes(acknowledgedPoint)));
        assertEquals(acknowledgedTime,
                Files.getLastModifiedTime(acknowledgedPoint));
        assertEquals(1, new FileSystemRuntimeEventStore(layout.eventRoot())
                .resolve(GOAL_ID).revision());
        assertTrue(new FileSystemRunRecordStore(layout.recordRoot())
                .references().isEmpty());
    }

    private SeededLease seedExpiredLease(
            Layout layout,
            Optional<String> recordedReference) throws Exception {
        String digest = writeTarget(layout.projectRoot());
        WorkItem workItem = workItem(digest);
        DurableSingleWorkerSchedulerQueue queue = createQueue(layout);
        queue.enqueue(new QueuedWork(workItem, List.of()));
        new FileSystemPendingFinalizationStore(layout.checkpointRoot()).record(
                new PendingFinalization(
                        GOAL_ID, AGENT_RUN_ID, recordedReference));
        FileSystemAgentRuntimeStateStore runtimeStore =
                new FileSystemAgentRuntimeStateStore(layout.runtimeRoot());
        AgentRunDispatch dispatch = new DurableAgentRunDispatcher(
                queue,
                runtimeStore,
                Clock.fixed(LEASE_ISSUED_AT, ZoneOffset.UTC))
                .claimAndLease(
                        GOAL_ID,
                        AGENT_RUN_ID,
                        "stopped-lease-owner",
                        Duration.ofMinutes(5))
                .orElseThrow();
        return new SeededLease(workItem, dispatch, runtimeStore);
    }

    private void assertLeaseTimeoutEvent(
            RuntimeEvent event,
            WorkItem workItem,
            LeaseTimeoutRecord timeout) {
        assertEquals(RuntimeEventKind.TIMEOUT_DETECTED, event.kind());
        assertEquals(
                new RuntimeEventDetail.TimeoutDetected(RuntimeTimeoutKind.LEASE),
                event.detail());
        assertEquals(new RuntimeEventBinding(
                        GOAL_ID,
                        workItem.workItemId(),
                        workItem.taskRevision(),
                        workItem.snapshotId(),
                        workItem.logicalRunId(),
                        workItem.workMessage().correlationId()),
                event.binding());
        assertEquals(AGENT_RUN_ID, event.agentRunId());
        assertEquals(timeout.expiresAt(), event.occurredAt());
        assertEquals(Optional.of(WORK_MESSAGE_ID), event.causationId());
        assertEquals("durable-agent-runtime", event.producerId());
        assertEquals(List.of(new RuntimeEventReference(
                        RuntimeEventReferenceKind.LEASE_TIMEOUT,
                        timeout.reference(GOAL_ID),
                        Optional.empty())),
                event.authoritativeReferences());
    }

    private WorkItem workItem(String expectedDigest) {
        return new WorkItem(
                WORK_ID,
                "read-file-worker",
                new MessageEnvelope(
                        WORK_MESSAGE_ID,
                        "scheduler-lease-timeout-correlation",
                        Optional.empty(),
                        "scheduler-lease-timeout-logical-run",
                        "scheduler-lease-timeout-cli-test",
                        Instant.parse("2026-08-05T07:00:00Z"),
                        new WorkPayload(
                                new ApprovedTaskRevision(
                                        "compose-lease-timeout-runtime-event-publication",
                                        "CURRENT_TASK.md",
                                        "b".repeat(64)),
                                "c".repeat(64),
                                Set.of("read-file"),
                                Optional.of(new WorkPayload.ExecutionInput(
                                        "CURRENT_TASK.md", expectedDigest)))));
    }

    private String writeTarget(Path projectRoot) throws Exception {
        Files.createDirectories(projectRoot);
        byte[] content = "lease timeout target\n".getBytes(StandardCharsets.UTF_8);
        Files.write(projectRoot.resolve("CURRENT_TASK.md"), content);
        return HexFormat.of().formatHex(
                MessageDigest.getInstance("SHA-256").digest(content));
    }

    private DurableSingleWorkerSchedulerQueue createQueue(Layout layout)
            throws Exception {
        return DurableSingleWorkerSchedulerQueue.create(
                QUEUE_ID,
                8,
                new FileSystemSchedulerQueueStore(layout.queueRoot()));
    }

    private DurableSingleWorkerSchedulerQueue recoverQueue(Layout layout)
            throws Exception {
        return DurableSingleWorkerSchedulerQueue.recover(
                QUEUE_ID,
                new FileSystemSchedulerQueueStore(layout.queueRoot()));
    }

    private Path solePendingPoint(Path root) throws Exception {
        List<Path> points = pendingPoints(root);
        assertEquals(1, points.size());
        return points.get(0);
    }

    private Path soleAcknowledgedPoint(Path root) throws Exception {
        List<Path> points = acknowledgedPoints(root);
        assertEquals(1, points.size());
        return points.get(0);
    }

    private List<Path> pendingPoints(Path root) throws Exception {
        return points(root, FileSystemRuntimeEventPublisher.FILE_SUFFIX);
    }

    private List<Path> acknowledgedPoints(Path root) throws Exception {
        return points(root, FileSystemRuntimeEventPublisher.ACKNOWLEDGED_FILE_SUFFIX);
    }

    private List<Path> points(Path root, String suffix) throws Exception {
        if (Files.notExists(root)) {
            return List.of();
        }
        try (Stream<Path> paths = Files.list(root)) {
            return paths.filter(path -> path.getFileName().toString().endsWith(suffix))
                    .sorted()
                    .toList();
        }
    }

    private Execution execute(Layout layout, String commandName) {
        return invoke(arguments(layout, commandName));
    }

    private Execution executeRead(Layout layout, String publicationFile) {
        return invoke(new String[] {
                "runtime-event-read",
                "--runtime-event-root", layout.eventRoot().toString(),
                "--runtime-event-publication-root", layout.publicationRoot().toString(),
                "--publication-file", publicationFile
        });
    }

    private Execution executeAcknowledge(Layout layout, String publicationFile) {
        return invoke(new String[] {
                "runtime-event-acknowledge",
                "--runtime-event-root", layout.eventRoot().toString(),
                "--runtime-event-publication-root", layout.publicationRoot().toString(),
                "--publication-file", publicationFile
        });
    }

    private Execution invoke(String[] arguments) {
        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        ByteArrayOutputStream stderr = new ByteArrayOutputStream();
        int exitCode = new EnhancerCli().execute(
                arguments,
                new PrintStream(stdout, true, StandardCharsets.UTF_8),
                new PrintStream(stderr, true, StandardCharsets.UTF_8));
        return new Execution(
                exitCode,
                stdout.toString(StandardCharsets.UTF_8),
                stderr.toString(StandardCharsets.UTF_8));
    }

    private String[] arguments(Layout layout, String commandName) {
        List<String> arguments = new ArrayList<>(List.of(
                commandName,
                "--project-root", layout.projectRoot().toString(),
                "--queue-root", layout.queueRoot().toString(),
                "--queue-id", QUEUE_ID,
                "--runtime-root", layout.runtimeRoot().toString(),
                "--external-effect-root", layout.effectRoot().toString(),
                "--cycle-checkpoint-root", layout.checkpointRoot().toString(),
                "--evidence-root", layout.evidenceRoot().toString(),
                "--run-record-root", layout.recordRoot().toString(),
                "--invocation-root", layout.invocationRoot().toString(),
                "--owner-id", "scheduler-lease-timeout-owner",
                "--max-attempts", "2",
                "--lease-millis", "300000",
                "--process-timeout-millis", "30000"));
        if (commandName.equals("scheduler-drain")) {
            arguments.addAll(List.of("--max-cycles", "8"));
        } else if (commandName.equals("scheduler-service")) {
            arguments.addAll(List.of(
                    "--max-cycles", "8",
                    "--max-consecutive-idle-cycles", "1",
                    "--idle-wait-millis", "1"));
        }
        arguments.addAll(List.of(
                "--runtime-event-root", layout.eventRoot().toString(),
                "--runtime-event-publication-root", layout.publicationRoot().toString(),
                "--max-pending-runtime-event-publications", "3"));
        return arguments.toArray(String[]::new);
    }

    private Layout layout(String name) {
        Path root = temporaryRoot.resolve(name);
        return new Layout(
                root.resolve("project"),
                root.resolve("queue"),
                root.resolve("runtime"),
                root.resolve("effects"),
                root.resolve("checkpoint"),
                root.resolve("evidence"),
                root.resolve("records"),
                root.resolve("invocations"),
                root.resolve("runtime-events"),
                root.resolve("runtime-event-publications"));
    }

    private record SeededLease(
            WorkItem workItem,
            AgentRunDispatch dispatch,
            FileSystemAgentRuntimeStateStore runtimeStore) {
    }

    private record Layout(
            Path projectRoot,
            Path queueRoot,
            Path runtimeRoot,
            Path effectRoot,
            Path checkpointRoot,
            Path evidenceRoot,
            Path recordRoot,
            Path invocationRoot,
            Path eventRoot,
            Path publicationRoot) {
    }

    private record Execution(int exitCode, String stdout, String stderr) {
    }
}
