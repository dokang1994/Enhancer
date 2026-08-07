package com.enhancer.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.enhancer.bus.MessageEnvelope;
import com.enhancer.bus.WorkPayload;
import com.enhancer.kernel.VerificationStatus;
import com.enhancer.run.FileSystemRunRecordStore;
import com.enhancer.runtime.DurableAgentRuntime;
import com.enhancer.runtime.DurableSingleWorkerSchedulerQueue;
import com.enhancer.runtime.FileSystemAgentRuntimeStateStore;
import com.enhancer.runtime.FileSystemPendingFinalizationStore;
import com.enhancer.runtime.FileSystemRuntimeEventPublisher;
import com.enhancer.runtime.FileSystemRuntimeEventStore;
import com.enhancer.runtime.FileSystemSchedulerQueueStore;
import com.enhancer.runtime.PendingFinalization;
import com.enhancer.runtime.QueuedWork;
import com.enhancer.runtime.RuntimeAgentRun;
import com.enhancer.runtime.RuntimeAgentRunStatus;
import com.enhancer.runtime.RuntimeEvent;
import com.enhancer.runtime.RuntimeEventBinding;
import com.enhancer.runtime.RuntimeEventDetail;
import com.enhancer.runtime.RuntimeEventKind;
import com.enhancer.runtime.RuntimeEventReferenceKind;
import com.enhancer.runtime.RuntimeEventStream;
import com.enhancer.runtime.RuntimeGoalStatus;
import com.enhancer.runtime.WorkItem;
import com.enhancer.runtime.WorkItemDisposition;
import com.enhancer.workspace.ApprovedTaskRevision;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class EnhancerCliSchedulerFinalizerRuntimeEventIntegrationTest {
    private static final String QUEUE_ID =
            "00000000-0000-0000-0000-000000001601";
    private static final String WORK_ID =
            "00000000-0000-0000-0000-000000001602";
    private static final String WORK_MESSAGE_ID =
            "00000000-0000-0000-0000-000000001603";
    private static final String GOAL_ID =
            "00000000-0000-0000-0000-000000001604";
    private static final String AGENT_RUN_ID =
            "00000000-0000-0000-0000-000000001605";

    @TempDir
    Path temporaryRoot;

    @ParameterizedTest
    @ValueSource(strings = {
            "scheduler-cycle", "scheduler-drain", "scheduler-service"
    })
    void recoversResultSidePublicationAfterDurableQueueDisposition(
            String commandName) throws Exception {
        Layout layout = layout(commandName);
        WorkItem workItem = prepareVerifiedWork(layout);

        Execution first = execute(layout, commandName);

        assertNotEquals(0, first.exitCode(), first.stdout() + first.stderr());
        DurableAgentRuntime firstRuntime = recoverRuntime(layout);
        assertEquals(RuntimeGoalStatus.COMPLETED, firstRuntime.goal().status());
        RuntimeAgentRun firstRun = firstRuntime.agentRun().orElseThrow();
        assertEquals(RuntimeAgentRunStatus.COMPLETED, firstRun.status());
        long runtimeRevision = firstRuntime.revision();

        DurableSingleWorkerSchedulerQueue firstQueue = recoverQueue(layout);
        assertEquals(Set.of(WORK_ID), firstQueue.completedWorkItemIds());
        assertTrue(firstQueue.failedWorkItemIds().isEmpty());
        long queueRevision = firstQueue.revision();
        assertEquals(1, new FileSystemRunRecordStore(layout.recordRoot())
                .references().size());
        assertTrue(new FileSystemPendingFinalizationStore(layout.checkpointRoot())
                .findPending().isPresent());

        FileSystemRuntimeEventStore eventStore =
                new FileSystemRuntimeEventStore(layout.eventRoot());
        RuntimeEventStream firstStream = eventStore.resolve(GOAL_ID);
        assertEquals(2, firstStream.revision());
        assertEquals(
                List.of(
                        RuntimeEventKind.VERIFICATION_RECORDED,
                        RuntimeEventKind.WORK_ITEM_TERMINATED),
                firstStream.events().stream().map(RuntimeEvent::kind).toList());
        assertResultSideEvents(firstStream, firstRun, workItem);

        Path eventArtifact = layout.eventRoot().resolve(
                GOAL_ID + ".runtime-events");
        byte[] firstEventBytes = Files.readAllBytes(eventArtifact);
        FileTime firstEventTime = Files.getLastModifiedTime(eventArtifact);
        Path verificationPoint = solePendingPoint(layout.publicationRoot());
        Execution verificationRead = executeRead(
                layout, verificationPoint.getFileName().toString());
        assertEquals(0, verificationRead.exitCode(), verificationRead.stderr());
        assertTrue(verificationRead.stdout().contains(
                "kind=VERIFICATION_RECORDED"), verificationRead.stdout());

        Execution acknowledged = executeAcknowledge(
                layout, verificationPoint.getFileName().toString());
        assertEquals(0, acknowledged.exitCode(), acknowledged.stderr());
        assertEquals(1, acknowledgedPoints(layout.publicationRoot()).size());
        assertTrue(pendingPoints(layout.publicationRoot()).isEmpty());

        Execution recovered = execute(layout, commandName);

        assertEquals(0, recovered.exitCode(), recovered.stderr());
        assertEquals(runtimeRevision, recoverRuntime(layout).revision());
        DurableSingleWorkerSchedulerQueue recoveredQueue = recoverQueue(layout);
        assertEquals(queueRevision, recoveredQueue.revision());
        assertEquals(Set.of(WORK_ID), recoveredQueue.completedWorkItemIds());
        assertTrue(recoveredQueue.failedWorkItemIds().isEmpty());
        assertEquals(1, new FileSystemRunRecordStore(layout.recordRoot())
                .references().size());
        assertTrue(new FileSystemPendingFinalizationStore(layout.checkpointRoot())
                .findPending().isEmpty());

        RuntimeEventStream recoveredStream = eventStore.resolve(GOAL_ID);
        assertEquals(firstStream.revision(), recoveredStream.revision());
        assertEquals(firstStream.events(), recoveredStream.events());
        assertTrue(Arrays.equals(firstEventBytes, Files.readAllBytes(eventArtifact)));
        assertEquals(firstEventTime, Files.getLastModifiedTime(eventArtifact));
        Path terminationPoint = solePendingPoint(layout.publicationRoot());
        Execution terminationRead = executeRead(
                layout, terminationPoint.getFileName().toString());
        assertEquals(0, terminationRead.exitCode(), terminationRead.stderr());
        assertTrue(terminationRead.stdout().contains(
                "kind=WORK_ITEM_TERMINATED"), terminationRead.stdout());
        assertEquals(1, acknowledgedPoints(layout.publicationRoot()).size());
    }

    private void assertResultSideEvents(
            RuntimeEventStream stream,
            RuntimeAgentRun run,
            WorkItem workItem) {
        RuntimeEvent verification = stream.events().get(0);
        RuntimeEvent termination = stream.events().get(1);
        RuntimeEventBinding binding = new RuntimeEventBinding(
                GOAL_ID,
                WORK_ID,
                workItem.taskRevision(),
                workItem.snapshotId(),
                workItem.logicalRunId(),
                workItem.workMessage().correlationId());
        String resultMessageId = run.resultMessage().orElseThrow().messageId();
        assertEquals(binding, verification.binding());
        assertEquals(binding, termination.binding());
        assertEquals(AGENT_RUN_ID, verification.agentRunId());
        assertEquals(AGENT_RUN_ID, termination.agentRunId());
        assertEquals(Optional.of(resultMessageId), verification.causationId());
        assertEquals(Optional.of(resultMessageId), termination.causationId());
        assertEquals(
                new RuntimeEventDetail.VerificationRecorded(
                        VerificationStatus.VERIFIED),
                verification.detail());
        assertEquals(
                new RuntimeEventDetail.WorkItemTerminated(
                        WorkItemDisposition.VERIFIED_COMPLETED),
                termination.detail());
        assertEquals(
                List.of(
                        RuntimeEventReferenceKind.RESULT_MESSAGE,
                        RuntimeEventReferenceKind.RUN_RECORD),
                verification.authoritativeReferences().stream()
                        .map(reference -> reference.kind())
                        .toList());
        assertEquals(
                List.of(RuntimeEventReferenceKind.SCHEDULER_QUEUE),
                termination.authoritativeReferences().stream()
                        .map(reference -> reference.kind())
                        .toList());
    }

    private WorkItem prepareVerifiedWork(Layout layout) throws Exception {
        Files.createDirectories(layout.projectRoot());
        byte[] content = "finalizer runtime event target\n"
                .getBytes(StandardCharsets.UTF_8);
        Files.write(layout.projectRoot().resolve("CURRENT_TASK.md"), content);
        String digest = HexFormat.of().formatHex(
                MessageDigest.getInstance("SHA-256").digest(content));
        WorkItem workItem = new WorkItem(
                WORK_ID,
                "read-file-worker",
                new MessageEnvelope(
                        WORK_MESSAGE_ID,
                        "scheduler-finalizer-correlation",
                        Optional.empty(),
                        "scheduler-finalizer-logical-run",
                        "scheduler-finalizer-cli-test",
                        Instant.parse("2026-08-07T01:00:00Z"),
                        new WorkPayload(
                                new ApprovedTaskRevision(
                                        "compose-finalizer-runtime-event-publication",
                                        "CURRENT_TASK.md",
                                        "b".repeat(64)),
                                "c".repeat(64),
                                Set.of("read-file"),
                                Optional.of(new WorkPayload.ExecutionInput(
                                        "CURRENT_TASK.md", digest)))));
        DurableSingleWorkerSchedulerQueue queue =
                DurableSingleWorkerSchedulerQueue.create(
                        QUEUE_ID,
                        8,
                        new FileSystemSchedulerQueueStore(layout.queueRoot()));
        queue.enqueue(new QueuedWork(workItem, List.of()));
        new FileSystemPendingFinalizationStore(layout.checkpointRoot()).record(
                new PendingFinalization(
                        GOAL_ID, AGENT_RUN_ID, Optional.empty()));
        return workItem;
    }

    private DurableAgentRuntime recoverRuntime(Layout layout) throws Exception {
        return DurableAgentRuntime.recover(
                GOAL_ID,
                new FileSystemAgentRuntimeStateStore(layout.runtimeRoot()),
                Clock.systemUTC());
    }

    private DurableSingleWorkerSchedulerQueue recoverQueue(Layout layout)
            throws Exception {
        return DurableSingleWorkerSchedulerQueue.recover(
                QUEUE_ID,
                new FileSystemSchedulerQueueStore(layout.queueRoot()));
    }

    private Execution execute(Layout layout, String commandName) {
        return invoke(arguments(layout, commandName));
    }

    private Execution executeRead(Layout layout, String publicationFile) {
        return invoke(new String[] {
                "runtime-event-read",
                "--runtime-event-root", layout.eventRoot().toString(),
                "--runtime-event-publication-root",
                layout.publicationRoot().toString(),
                "--publication-file", publicationFile
        });
    }

    private Execution executeAcknowledge(Layout layout, String publicationFile) {
        return invoke(new String[] {
                "runtime-event-acknowledge",
                "--runtime-event-root", layout.eventRoot().toString(),
                "--runtime-event-publication-root",
                layout.publicationRoot().toString(),
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
                "--owner-id", "scheduler-finalizer-owner",
                "--max-attempts", "1",
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
                "--runtime-event-publication-root",
                layout.publicationRoot().toString(),
                "--max-pending-runtime-event-publications", "1"));
        return arguments.toArray(String[]::new);
    }

    private Path solePendingPoint(Path root) throws Exception {
        List<Path> points = pendingPoints(root);
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
