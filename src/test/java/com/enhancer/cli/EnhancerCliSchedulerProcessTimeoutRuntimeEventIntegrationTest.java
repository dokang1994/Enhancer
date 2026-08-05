package com.enhancer.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.enhancer.bus.MessageEnvelope;
import com.enhancer.bus.WorkPayload;
import com.enhancer.run.FileSystemRunRecordStore;
import com.enhancer.runtime.DurableSingleWorkerSchedulerQueue;
import com.enhancer.runtime.DurableWorkItemAdmissionHandler;
import com.enhancer.runtime.FileSystemPendingFinalizationStore;
import com.enhancer.runtime.FileSystemProcessTimeoutFactStore;
import com.enhancer.runtime.FileSystemRuntimeEventPublisher;
import com.enhancer.runtime.FileSystemRuntimeEventStore;
import com.enhancer.runtime.FileSystemSchedulerQueueStore;
import com.enhancer.runtime.PendingFinalization;
import com.enhancer.runtime.ResolvedProcessTimeoutFact;
import com.enhancer.runtime.RuntimeEvent;
import com.enhancer.runtime.RuntimeEventDetail;
import com.enhancer.runtime.RuntimeEventKind;
import com.enhancer.runtime.RuntimeEventReferenceKind;
import com.enhancer.runtime.RuntimeEventStream;
import com.enhancer.runtime.RuntimeTimeoutKind;
import com.enhancer.workspace.ApprovedTaskRevision;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.security.MessageDigest;
import java.time.Instant;
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

class EnhancerCliSchedulerProcessTimeoutRuntimeEventIntegrationTest {
    private static final String QUEUE_ID =
            "00000000-0000-0000-0000-000000001301";
    private static final String MESSAGE_ID =
            "00000000-0000-0000-0000-000000001302";

    @TempDir
    Path temporaryRoot;

    @ParameterizedTest
    @ValueSource(strings = {
            "scheduler-cycle", "scheduler-drain", "scheduler-service"
    })
    void publishesOneDurableProcessTimeoutEventFromEverySchedulerExecutionCommand(
            String commandName) throws Exception {
        Layout layout = layout(commandName);
        admitOneWorkItem(layout);

        Execution execution = execute(layout, commandName);

        assertEquals(70, execution.exitCode(), execution.stderr());
        assertTrue(execution.stderr().contains("did not complete"), execution.stderr());
        PendingFinalization checkpoint = new FileSystemPendingFinalizationStore(
                layout.checkpointRoot()).findPending().orElseThrow();
        ResolvedProcessTimeoutFact fact = timeoutFact(layout, checkpoint);
        RuntimeEventStream stream = new FileSystemRuntimeEventStore(
                layout.eventRoot()).resolve(checkpoint.goalId());
        assertEquals(1, stream.revision());
        RuntimeEvent event = stream.events().get(0);
        assertEquals(RuntimeEventKind.TIMEOUT_DETECTED, event.kind());
        assertEquals(
                new RuntimeEventDetail.TimeoutDetected(RuntimeTimeoutKind.PROCESS),
                event.detail());
        assertEquals(checkpoint.agentRunId(), event.agentRunId());
        assertEquals(fact.fact().occurredAt(), event.occurredAt());
        assertEquals(RuntimeEventReferenceKind.PROCESS_TIMEOUT,
                event.authoritativeReferences().get(0).kind());
        assertEquals(fact.reference(),
                event.authoritativeReferences().get(0).reference());

        Path point = solePendingPoint(layout.publicationRoot());
        Execution read = executeRead(layout, point.getFileName().toString());
        assertEquals(0, read.exitCode(), read.stderr());
        assertTrue(read.stdout().contains("status=AVAILABLE"), read.stdout());
        assertTrue(read.stdout().contains("goalId=" + checkpoint.goalId()),
                read.stdout());
        assertTrue(read.stdout().contains("kind=TIMEOUT_DETECTED"), read.stdout());

        assertTrue(new FileSystemRunRecordStore(layout.recordRoot())
                .references().isEmpty());
        DurableSingleWorkerSchedulerQueue queue = recoverQueue(layout);
        assertTrue(queue.completedWorkItemIds().isEmpty());
        assertTrue(queue.failedWorkItemIds().isEmpty());
    }

    @Test
    void acknowledgedProcessTimeoutPointIsNotRecreatedByExactCycleReplay()
            throws Exception {
        Layout layout = layout("acknowledged-replay");
        admitOneWorkItem(layout);
        assertEquals(70, execute(layout, "scheduler-cycle").exitCode());
        PendingFinalization checkpoint = new FileSystemPendingFinalizationStore(
                layout.checkpointRoot()).findPending().orElseThrow();
        ResolvedProcessTimeoutFact beforeFact = timeoutFact(layout, checkpoint);
        Path eventArtifact = layout.eventRoot().resolve(
                checkpoint.goalId() + ".runtime-events");
        byte[] beforeEvent = Files.readAllBytes(eventArtifact);
        Path pendingPoint = solePendingPoint(layout.publicationRoot());

        Execution acknowledged = executeAcknowledge(
                layout, pendingPoint.getFileName().toString());
        assertEquals(0, acknowledged.exitCode(), acknowledged.stderr());
        assertTrue(acknowledged.stdout().contains("status=ACKNOWLEDGED"),
                acknowledged.stdout());
        Path acknowledgedPoint = soleAcknowledgedPoint(layout.publicationRoot());
        byte[] acknowledgedBytes = Files.readAllBytes(acknowledgedPoint);
        FileTime acknowledgedTime = Files.getLastModifiedTime(acknowledgedPoint);

        Execution replay = execute(layout, "scheduler-cycle");

        assertEquals(70, replay.exitCode(), replay.stderr());
        assertEquals(beforeFact, timeoutFact(layout, checkpoint));
        assertEquals(List.of(), pendingPoints(layout.publicationRoot()));
        assertEquals(List.of(acknowledgedPoint),
                acknowledgedPoints(layout.publicationRoot()));
        assertTrue(java.util.Arrays.equals(
                acknowledgedBytes, Files.readAllBytes(acknowledgedPoint)));
        assertEquals(acknowledgedTime, Files.getLastModifiedTime(acknowledgedPoint));
        assertTrue(java.util.Arrays.equals(beforeEvent, Files.readAllBytes(eventArtifact)));
        assertEquals(1, new FileSystemRuntimeEventStore(layout.eventRoot())
                .resolve(checkpoint.goalId()).revision());
        assertTrue(new FileSystemRunRecordStore(layout.recordRoot())
                .references().isEmpty());
        assertTrue(new FileSystemPendingFinalizationStore(layout.checkpointRoot())
                .findPending().isPresent());
    }

    private void admitOneWorkItem(Layout layout) throws Exception {
        Files.createDirectories(layout.projectRoot());
        byte[] content = "process timeout target\n".getBytes(StandardCharsets.UTF_8);
        Files.write(layout.projectRoot().resolve("CURRENT_TASK.md"), content);
        String digest = HexFormat.of().formatHex(
                MessageDigest.getInstance("SHA-256").digest(content));
        DurableSingleWorkerSchedulerQueue queue =
                DurableSingleWorkerSchedulerQueue.create(
                        QUEUE_ID,
                        8,
                        new FileSystemSchedulerQueueStore(layout.queueRoot()));
        new DurableWorkItemAdmissionHandler("read-file-worker", queue).handle(
                new MessageEnvelope(
                        MESSAGE_ID,
                        "scheduler-timeout-correlation",
                        Optional.empty(),
                        "scheduler-timeout-logical-run",
                        "scheduler-timeout-cli-test",
                        Instant.parse("2026-08-05T06:00:00Z"),
                        new WorkPayload(
                                new ApprovedTaskRevision(
                                        "compose-process-timeout-runtime-event-publication",
                                        "CURRENT_TASK.md",
                                        digest),
                                "b".repeat(64),
                                Set.of("read-file"),
                                Optional.of(new WorkPayload.ExecutionInput(
                                        "CURRENT_TASK.md", digest)))));
    }

    private ResolvedProcessTimeoutFact timeoutFact(
            Layout layout,
            PendingFinalization checkpoint) throws Exception {
        return new FileSystemProcessTimeoutFactStore(
                layout.invocationRoot().resolve(".process-timeouts"))
                .find(checkpoint.goalId(), checkpoint.agentRunId())
                .orElseThrow();
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
                "--owner-id", "scheduler-timeout-owner",
                "--max-attempts", "2",
                "--lease-millis", "300000",
                "--process-timeout-millis", "1"));
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
                "--max-pending-runtime-event-publications", "1"));
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
