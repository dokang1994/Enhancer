package com.enhancer.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.enhancer.bus.BackpressurePolicy;
import com.enhancer.bus.DeliveryDestination;
import com.enhancer.bus.FileSpoolMessageTransport;
import com.enhancer.bus.MessageEnvelope;
import com.enhancer.bus.TransportMessage;
import com.enhancer.bus.TransportStatus;
import com.enhancer.bus.WorkPayload;
import com.enhancer.run.FileSystemRunRecordStore;
import com.enhancer.runtime.DurableAgentRuntime;
import com.enhancer.runtime.DurableSingleWorkerSchedulerQueue;
import com.enhancer.runtime.DurableWorkItemAdmissionHandler;
import com.enhancer.runtime.FileSystemAgentRuntimeStateStore;
import com.enhancer.runtime.FileSystemSchedulerQueueStore;
import com.enhancer.workspace.ApprovedTaskRevision;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.FileSystemException;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class EnhancerCliSchedulerReceiveWorkIntegrationTest {
    private static final String QUEUE_ID =
            "00000000-0000-0000-0000-000000000731";
    private static final String MESSAGE_ID =
            "00000000-0000-0000-0000-000000000732";
    private static final String DESTINATION = "scheduler-work";

    @TempDir
    Path temporaryRoot;

    @Test
    void spoolsReceivesExecutesAndExactlyReplaysWithoutDuplicateRuntimeEffects()
            throws Exception {
        Layout layout = layout("connected");
        String digest = writeTarget(layout.projectRoot(), "receiver target\n");
        DurableSingleWorkerSchedulerQueue.create(
                QUEUE_ID,
                8,
                new FileSystemSchedulerQueueStore(layout.queueRoot()));
        FileSpoolMessageTransport transport = new FileSpoolMessageTransport(
                layout.spoolRoot(), BackpressurePolicy.standard());
        assertEquals(
                TransportStatus.ACCEPTED,
                transport.send(new TransportMessage(
                        DeliveryDestination.queue(DESTINATION),
                        workMessage(digest))).status());
        String messageFile = onlyTransportFile(layout.spoolRoot());

        Execution admitted = receive(layout, messageFile);
        assertEquals(0, admitted.exitCode());
        assertTrue(admitted.stdout().contains("status=ADMITTED"));
        assertTrue(admitted.stdout().contains("spoolStatus=ACKNOWLEDGED"));
        assertTrue(admitted.stdout().contains(
                "acknowledgedFile=" + acknowledgedFile(messageFile)));
        assertTrue(admitted.stdout().contains("priority=EXPEDITED"));
        assertFalse(Files.exists(layout.spoolRoot().resolve(messageFile)));
        assertTrue(Files.isRegularFile(
                layout.spoolRoot().resolve(acknowledgedFile(messageFile))));
        Execution service = service(layout);
        assertEquals(0, service.exitCode());
        assertTrue(service.stdout().contains("verifiedCompletedCycles=1"));

        DurableSingleWorkerSchedulerQueue completed = recoverQueue(layout);
        long completedRevision = completed.revision();
        assertEquals(1, completed.completedWorkItemIds().size());
        assertEquals(1, new FileSystemRunRecordStore(layout.recordRoot())
                .references().size());
        String goalId = onlyRuntimeGoalId(layout.runtimeRoot());
        DurableAgentRuntime runtime = DurableAgentRuntime.recover(
                goalId,
                new FileSystemAgentRuntimeStateStore(layout.runtimeRoot()),
                Clock.systemUTC());
        assertEquals(1, runtime.agentRuns().size());

        Execution replayed = receive(layout, messageFile);
        assertEquals(0, replayed.exitCode());
        assertTrue(replayed.stdout().contains("status=REPLAYED"));
        assertTrue(replayed.stdout().contains("spoolStatus=ALREADY_ACKNOWLEDGED"));
        assertTrue(replayed.stdout().contains(
                "acknowledgedFile=" + acknowledgedFile(messageFile)));
        assertEquals(completedRevision, recoverQueue(layout).revision());
        assertEquals(1, new FileSystemRunRecordStore(layout.recordRoot())
                .references().size());
        assertEquals(
                1,
                DurableAgentRuntime.recover(
                        goalId,
                        new FileSystemAgentRuntimeStateStore(layout.runtimeRoot()),
                        Clock.systemUTC()).agentRuns().size());
        assertFalse(Files.exists(layout.spoolRoot().resolve(messageFile)));
        assertTrue(Files.isRegularFile(
                layout.spoolRoot().resolve(acknowledgedFile(messageFile))));
    }

    @Test
    void acknowledgementReleasesPendingTransportCapacity() throws Exception {
        Layout layout = layout("capacity");
        DurableSingleWorkerSchedulerQueue.create(
                QUEUE_ID,
                8,
                new FileSystemSchedulerQueueStore(layout.queueRoot()));
        FileSpoolMessageTransport transport = new FileSpoolMessageTransport(
                layout.spoolRoot(), BackpressurePolicy.of(1));
        TransportMessage message = new TransportMessage(
                DeliveryDestination.queue(DESTINATION),
                workMessage("c".repeat(64)));
        assertEquals(TransportStatus.ACCEPTED, transport.send(message).status());
        String messageFile = onlyTransportFile(layout.spoolRoot());

        assertEquals(0, receive(layout, messageFile).exitCode());
        assertFalse(Files.exists(layout.spoolRoot().resolve(messageFile)));
        assertTrue(Files.isRegularFile(
                layout.spoolRoot().resolve(acknowledgedFile(messageFile))));
        assertEquals(TransportStatus.ACCEPTED, transport.send(message).status());
        String nextMessageFile = onlyTransportFile(layout.spoolRoot());
        assertFalse(nextMessageFile.equals(messageFile));
        assertTrue(Files.isRegularFile(
                layout.spoolRoot().resolve(nextMessageFile)));
    }

    @Test
    void rejectsMissingAndCorruptPointArtifactsBeforeQueueMutation()
            throws Exception {
        Layout layout = layout("refusal");
        DurableSingleWorkerSchedulerQueue queue =
                DurableSingleWorkerSchedulerQueue.create(
                        QUEUE_ID,
                        8,
                        new FileSystemSchedulerQueueStore(layout.queueRoot()));
        String missing = "00000000-0000-0000-0000-000000000733.transport";
        assertEquals(2, receive(layout, missing).exitCode());

        Files.createDirectories(layout.spoolRoot());
        String corrupt = "00000000-0000-0000-0000-000000000734.transport";
        Files.writeString(
                layout.spoolRoot().resolve(corrupt),
                "not a transport frame",
                StandardCharsets.UTF_8);
        assertEquals(70, receive(layout, corrupt).exitCode());
        assertEquals(0, queue.revision());
        assertEquals(0, queue.pendingCount());
    }

    @Test
    void rejectsASymbolicPointArtifactBeforeQueueMutation() throws Exception {
        Layout layout = layout("symbolic-refusal");
        DurableSingleWorkerSchedulerQueue queue =
                DurableSingleWorkerSchedulerQueue.create(
                        QUEUE_ID,
                        8,
                        new FileSystemSchedulerQueueStore(layout.queueRoot()));
        Files.createDirectories(layout.spoolRoot());
        Path target = layout.projectRoot().resolve("target.transport");
        Files.createDirectories(target.getParent());
        Files.writeString(target, "target", StandardCharsets.UTF_8);
        String name = "00000000-0000-0000-0000-000000000735.transport";
        try {
            Files.createSymbolicLink(
                    layout.spoolRoot().resolve(acknowledgedFile(name)),
                    target);
        } catch (UnsupportedOperationException | FileSystemException unavailable) {
            assumeTrue(false, "symbolic links are unavailable");
        }

        assertEquals(2, receive(layout, name).exitCode());
        assertEquals(0, queue.revision());
        assertEquals(0, queue.pendingCount());
    }

    @Test
    void rejectsPendingAndAcknowledgedCollisionBeforeRecoveringAnActiveQueue()
            throws Exception {
        Layout layout = layout("collision-before-recovery");
        DurableSingleWorkerSchedulerQueue queue =
                DurableSingleWorkerSchedulerQueue.create(
                        QUEUE_ID,
                        8,
                        new FileSystemSchedulerQueueStore(layout.queueRoot()));
        new DurableWorkItemAdmissionHandler("read-file-worker", queue)
                .handle(workMessage("c".repeat(64)));
        queue.claimNext().orElseThrow();
        long activeRevision = queue.revision();
        FileSpoolMessageTransport transport = new FileSpoolMessageTransport(
                layout.spoolRoot(), BackpressurePolicy.standard());
        assertEquals(
                TransportStatus.ACCEPTED,
                transport.send(new TransportMessage(
                        DeliveryDestination.queue(DESTINATION),
                        workMessage("c".repeat(64)))).status());
        String messageFile = onlyTransportFile(layout.spoolRoot());
        Files.copy(
                layout.spoolRoot().resolve(messageFile),
                layout.spoolRoot().resolve(acknowledgedFile(messageFile)));

        assertEquals(2, receive(layout, messageFile).exitCode());
        var unchanged = new FileSystemSchedulerQueueStore(layout.queueRoot())
                .resolve(QUEUE_ID);
        assertEquals(activeRevision, unchanged.revision());
        assertTrue(unchanged.activeWork().isPresent());
        assertTrue(Files.isRegularFile(layout.spoolRoot().resolve(messageFile)));
        assertTrue(Files.isRegularFile(
                layout.spoolRoot().resolve(acknowledgedFile(messageFile))));
    }

    @Test
    void rejectsAForeignRouteBeforeRecoveringAnActiveQueue() throws Exception {
        Layout layout = layout("foreign-before-recovery");
        DurableSingleWorkerSchedulerQueue queue =
                DurableSingleWorkerSchedulerQueue.create(
                        QUEUE_ID,
                        8,
                        new FileSystemSchedulerQueueStore(layout.queueRoot()));
        new DurableWorkItemAdmissionHandler("read-file-worker", queue)
                .handle(workMessage("c".repeat(64)));
        queue.claimNext().orElseThrow();
        long activeRevision = queue.revision();
        FileSpoolMessageTransport transport = new FileSpoolMessageTransport(
                layout.spoolRoot(), BackpressurePolicy.standard());
        assertEquals(
                TransportStatus.ACCEPTED,
                transport.send(new TransportMessage(
                        DeliveryDestination.queue("foreign"),
                        workMessage("c".repeat(64)))).status());

        assertEquals(
                2,
                receive(layout, onlyTransportFile(layout.spoolRoot())).exitCode());
        var unchanged = new FileSystemSchedulerQueueStore(layout.queueRoot())
                .resolve(QUEUE_ID);
        assertEquals(activeRevision, unchanged.revision());
        assertTrue(unchanged.activeWork().isPresent());
    }

    private MessageEnvelope workMessage(String digest) {
        return new MessageEnvelope(
                MESSAGE_ID,
                "scheduler-receiver-correlation",
                Optional.empty(),
                "scheduler-receiver-logical-run",
                "scheduler-receiver-integration",
                Instant.parse("2026-07-28T06:00:00Z"),
                new WorkPayload(
                        new ApprovedTaskRevision(
                                "connect-durable-work-spool-to-scheduler-worker",
                                "CURRENT_TASK.md",
                                "a".repeat(64)),
                        "b".repeat(64),
                        Set.of("read-file"),
                        Optional.of(new WorkPayload.ExecutionInput(
                                "CURRENT_TASK.md", digest))));
    }

    private Execution receive(Layout layout, String messageFile) {
        return execute(new String[] {
                "scheduler-receive-work",
                "--transport-spool-root", layout.spoolRoot().toString(),
                "--message-file", messageFile,
                "--destination-name", DESTINATION,
                "--queue-root", layout.queueRoot().toString(),
                "--queue-id", QUEUE_ID,
                "--required-capability", "read-file-worker",
                "--priority", "EXPEDITED"
        });
    }

    private Execution service(Layout layout) {
        return execute(new String[] {
                "scheduler-service",
                "--project-root", layout.projectRoot().toString(),
                "--queue-root", layout.queueRoot().toString(),
                "--queue-id", QUEUE_ID,
                "--runtime-root", layout.runtimeRoot().toString(),
                "--external-effect-root", layout.effectRoot().toString(),
                "--cycle-checkpoint-root", layout.checkpointRoot().toString(),
                "--evidence-root", layout.evidenceRoot().toString(),
                "--run-record-root", layout.recordRoot().toString(),
                "--invocation-root", layout.invocationRoot().toString(),
                "--owner-id", "scheduler-receiver-owner",
                "--max-attempts", "2",
                "--lease-millis", "300000",
                "--process-timeout-millis", "30000",
                "--max-cycles", "8",
                "--max-consecutive-idle-cycles", "1",
                "--idle-wait-millis", "1"
        });
    }

    private Execution execute(String[] arguments) {
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

    private DurableSingleWorkerSchedulerQueue recoverQueue(Layout layout)
            throws Exception {
        return DurableSingleWorkerSchedulerQueue.recover(
                QUEUE_ID,
                new FileSystemSchedulerQueueStore(layout.queueRoot()));
    }

    private String onlyTransportFile(Path root) throws Exception {
        try (var files = Files.list(root)) {
            List<Path> matches = files
                    .filter(path -> path.getFileName().toString()
                            .endsWith(FileSpoolMessageTransport.FILE_SUFFIX))
                    .toList();
            assertEquals(1, matches.size());
            return matches.get(0).getFileName().toString();
        }
    }

    private String onlyRuntimeGoalId(Path root) throws Exception {
        try (var files = Files.list(root)) {
            List<Path> matches = files
                    .filter(path -> path.getFileName().toString()
                            .endsWith(".agent-runtime"))
                    .toList();
            assertEquals(1, matches.size());
            String name = matches.get(0).getFileName().toString();
            return name.substring(0, name.length() - ".agent-runtime".length());
        }
    }

    private String acknowledgedFile(String messageFile) {
        return messageFile.substring(
                0,
                messageFile.length() - FileSpoolMessageTransport.FILE_SUFFIX.length())
                + ".received";
    }

    private String writeTarget(Path projectRoot, String content) throws Exception {
        Files.createDirectories(projectRoot);
        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
        Files.write(projectRoot.resolve("CURRENT_TASK.md"), bytes);
        return HexFormat.of().formatHex(
                MessageDigest.getInstance("SHA-256").digest(bytes));
    }

    private Layout layout(String name) {
        Path root = temporaryRoot.resolve(name);
        return new Layout(
                root.resolve("project"),
                root.resolve("spool"),
                root.resolve("queue"),
                root.resolve("runtime"),
                root.resolve("effects"),
                root.resolve("checkpoint"),
                root.resolve("evidence"),
                root.resolve("records"),
                root.resolve("invocations"));
    }

    private record Layout(
            Path projectRoot,
            Path spoolRoot,
            Path queueRoot,
            Path runtimeRoot,
            Path effectRoot,
            Path checkpointRoot,
            Path evidenceRoot,
            Path recordRoot,
            Path invocationRoot) {
    }

    private record Execution(int exitCode, String stdout, String stderr) {
    }
}
