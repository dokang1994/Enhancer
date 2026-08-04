package com.enhancer.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.enhancer.bus.BackpressurePolicy;
import com.enhancer.bus.ControlPayload;
import com.enhancer.bus.ControlSignal;
import com.enhancer.bus.DeliveryDestination;
import com.enhancer.bus.FileSpoolMessageTransport;
import com.enhancer.bus.MessageEnvelope;
import com.enhancer.bus.TransportMessage;
import com.enhancer.bus.WorkPayload;
import com.enhancer.runtime.DurableAgentRuntime;
import com.enhancer.runtime.FileSystemAgentRuntimeStateStore;
import com.enhancer.runtime.FileSystemRuntimeEventPublisher;
import com.enhancer.runtime.FileSystemRuntimeEventStore;
import com.enhancer.runtime.RuntimeAgentRunStatus;
import com.enhancer.runtime.RuntimeEventKind;
import com.enhancer.runtime.RuntimeEventPublicationReference;
import com.enhancer.runtime.RuntimeGoalStatus;
import com.enhancer.runtime.WorkItem;
import com.enhancer.workspace.ApprovedTaskRevision;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class EnhancerCliSchedulerReceiveControlIntegrationTest {
    private static final String GOAL_ID =
            "00000000-0000-0000-0000-000000000811";
    private static final String AGENT_RUN_ID =
            "00000000-0000-0000-0000-000000000812";
    private static final String WORK_ITEM_ID =
            "00000000-0000-0000-0000-000000000813";
    private static final String WORK_MESSAGE_ID =
            "00000000-0000-0000-0000-000000000814";
    private static final String CONTROL_MESSAGE_ID =
            "00000000-0000-0000-0000-000000000815";
    private static final String DESTINATION = "runtime-controls";

    @TempDir
    Path temporaryRoot;

    @Test
    void receivesPersistsAcknowledgesAndExactlyReplaysOneControlPoint()
            throws Exception {
        Path runtimeRoot = temporaryRoot.resolve("runtime");
        Path spoolRoot = temporaryRoot.resolve("spool");
        FileSystemAgentRuntimeStateStore store = activeRuntime(runtimeRoot);
        MessageEnvelope control = controlMessage();
        String messageFile = new FileSpoolMessageTransport(
                        spoolRoot, BackpressurePolicy.of(1))
                .sendWithReference(new TransportMessage(
                        DeliveryDestination.queue(DESTINATION), control))
                .messageFile()
                .orElseThrow();

        Execution recorded = execute(spoolRoot, runtimeRoot, messageFile);

        assertEquals(0, recorded.exitCode());
        assertTrue(recorded.stdout().contains("status=RECORDED"));
        assertTrue(recorded.stdout().contains("spoolStatus=ACKNOWLEDGED"));
        assertTrue(recorded.stdout().contains("goalId=" + GOAL_ID));
        assertTrue(recorded.stdout().contains("messageId=" + CONTROL_MESSAGE_ID));
        assertTrue(recorded.stdout().contains("signal=PAUSE"));
        Path acknowledged = spoolRoot.resolve(
                messageFile.replace(".transport", ".received"));
        assertFalse(Files.exists(spoolRoot.resolve(messageFile)));
        assertTrue(Files.isRegularFile(acknowledged));
        DurableAgentRuntime afterRecord = DurableAgentRuntime.recover(
                GOAL_ID, store, Clock.systemUTC());
        long revision = afterRecord.revision();
        assertEquals(List.of(control), afterRecord.controlRequests());
        assertEquals(RuntimeGoalStatus.ACTIVE, afterRecord.goal().status());
        assertEquals(RuntimeAgentRunStatus.READY,
                afterRecord.agentRun().orElseThrow().status());

        Execution replayed = execute(spoolRoot, runtimeRoot, messageFile);

        assertEquals(0, replayed.exitCode());
        assertTrue(replayed.stdout().contains("status=REPLAYED"));
        assertTrue(replayed.stdout().contains(
                "spoolStatus=ALREADY_ACKNOWLEDGED"));
        DurableAgentRuntime afterReplay = DurableAgentRuntime.recover(
                GOAL_ID, store, Clock.systemUTC());
        assertEquals(revision, afterReplay.revision());
        assertEquals(List.of(control), afterReplay.controlRequests());
        assertFalse(Files.exists(spoolRoot.resolve(messageFile)));
        assertTrue(Files.isRegularFile(acknowledged));
    }

    @Test
    void ambiguousPointFailsBeforeRuntimeMutation() throws Exception {
        Path runtimeRoot = temporaryRoot.resolve("ambiguous-runtime");
        Path spoolRoot = temporaryRoot.resolve("ambiguous-spool");
        FileSystemAgentRuntimeStateStore store = activeRuntime(runtimeRoot);
        String messageFile = new FileSpoolMessageTransport(
                        spoolRoot, BackpressurePolicy.standard())
                .sendWithReference(new TransportMessage(
                        DeliveryDestination.queue(DESTINATION), controlMessage()))
                .messageFile()
                .orElseThrow();
        Files.copy(
                spoolRoot.resolve(messageFile),
                spoolRoot.resolve(messageFile.replace(".transport", ".received")));
        long revision = store.resolve(GOAL_ID).revision();

        Execution execution = execute(spoolRoot, runtimeRoot, messageFile);

        assertEquals(CliExitCode.USAGE_OR_CONFIGURATION.code(), execution.exitCode());
        assertTrue(execution.stderr().contains(
                "must resolve exactly one pending or acknowledged"));
        assertEquals(revision, store.resolve(GOAL_ID).revision());
        assertEquals(List.of(), store.resolve(GOAL_ID).controlRequests());
    }

    @Test
    void missingGoalLeavesThePendingControlPointUnacknowledged() throws Exception {
        Path runtimeRoot = temporaryRoot.resolve("missing-runtime");
        Path spoolRoot = temporaryRoot.resolve("missing-spool");
        String messageFile = new FileSpoolMessageTransport(
                        spoolRoot, BackpressurePolicy.standard())
                .sendWithReference(new TransportMessage(
                        DeliveryDestination.queue(DESTINATION), controlMessage()))
                .messageFile()
                .orElseThrow();

        Execution execution = execute(spoolRoot, runtimeRoot, messageFile);

        assertEquals(CliExitCode.USAGE_OR_CONFIGURATION.code(), execution.exitCode());
        assertTrue(execution.stderr().contains(
                "runtime configuration is invalid"));
        assertTrue(Files.isRegularFile(spoolRoot.resolve(messageFile)));
        assertFalse(Files.exists(spoolRoot.resolve(
                messageFile.replace(".transport", ".received"))));
    }

    @Test
    void optionalEventCompositionPublishesCancelAndExactlyReplaysTheAcknowledgedPoint()
            throws Exception {
        Path runtimeRoot = temporaryRoot.resolve("event-runtime");
        Path spoolRoot = temporaryRoot.resolve("event-spool");
        Path eventRoot = temporaryRoot.resolve("events");
        Path publicationRoot = temporaryRoot.resolve("publications");
        FileSystemAgentRuntimeStateStore store = activeRuntime(runtimeRoot);
        MessageEnvelope cancel = controlMessage(ControlSignal.CANCEL);
        String messageFile = new FileSpoolMessageTransport(
                        spoolRoot, BackpressurePolicy.standard())
                .sendWithReference(new TransportMessage(
                        DeliveryDestination.queue(DESTINATION), cancel))
                .messageFile()
                .orElseThrow();

        Execution recorded = executeWithEvents(
                spoolRoot,
                runtimeRoot,
                eventRoot,
                publicationRoot,
                "4",
                messageFile);

        assertEquals(0, recorded.exitCode());
        assertTrue(recorded.stdout().contains("status=RECORDED"));
        assertTrue(recorded.stdout().contains("spoolStatus=ACKNOWLEDGED"));
        long runtimeRevision = store.resolve(GOAL_ID).revision();
        var eventStream = new FileSystemRuntimeEventStore(eventRoot).resolve(GOAL_ID);
        assertEquals(1, eventStream.revision());
        assertEquals(1, eventStream.events().size());
        assertEquals(
                RuntimeEventKind.CANCELLATION_REQUEST_RECORDED,
                eventStream.events().get(0).kind());
        Path point = onlyPublicationPoint(publicationRoot);
        byte[] pointBytes = Files.readAllBytes(point);
        var retainedTime = java.nio.file.attribute.FileTime.from(
                Instant.parse("2026-08-04T03:00:00Z"));
        Files.setLastModifiedTime(point, retainedTime);

        Execution replayed = executeWithEvents(
                spoolRoot,
                runtimeRoot,
                eventRoot,
                publicationRoot,
                "4",
                messageFile);

        assertEquals(0, replayed.exitCode());
        assertTrue(replayed.stdout().contains("status=REPLAYED"));
        assertTrue(replayed.stdout().contains(
                "spoolStatus=ALREADY_ACKNOWLEDGED"));
        assertEquals(runtimeRevision, store.resolve(GOAL_ID).revision());
        assertEquals(1, new FileSystemRuntimeEventStore(eventRoot)
                .resolve(GOAL_ID).revision());
        assertEquals(point, onlyPublicationPoint(publicationRoot));
        assertEquals(retainedTime, Files.getLastModifiedTime(point));
        org.junit.jupiter.api.Assertions.assertArrayEquals(
                pointBytes, Files.readAllBytes(point));
    }

    @Test
    void publisherCapacityFailureLeavesDurablePrefixesForExactRecovery()
            throws Exception {
        Path runtimeRoot = temporaryRoot.resolve("recovery-runtime");
        Path spoolRoot = temporaryRoot.resolve("recovery-spool");
        Path eventRoot = temporaryRoot.resolve("recovery-events");
        Path publicationRoot = temporaryRoot.resolve("recovery-publications");
        FileSystemAgentRuntimeStateStore store = activeRuntime(runtimeRoot);
        FileSystemRuntimeEventPublisher blockingPublisher =
                new FileSystemRuntimeEventPublisher(publicationRoot, 1);
        blockingPublisher.publish(new RuntimeEventPublicationReference(
                "runtime-event/"
                        + GOAL_ID
                        + "/00000000-0000-0000-0000-000000000899"));
        MessageEnvelope cancel = controlMessage(ControlSignal.CANCEL);
        String messageFile = new FileSpoolMessageTransport(
                        spoolRoot, BackpressurePolicy.standard())
                .sendWithReference(new TransportMessage(
                        DeliveryDestination.queue(DESTINATION), cancel))
                .messageFile()
                .orElseThrow();

        Execution failed = executeWithEvents(
                spoolRoot,
                runtimeRoot,
                eventRoot,
                publicationRoot,
                "1",
                messageFile);

        assertEquals(CliExitCode.INTERNAL_ERROR.code(), failed.exitCode());
        assertTrue(Files.isRegularFile(spoolRoot.resolve(messageFile)));
        assertEquals(List.of(cancel), store.resolve(GOAL_ID).controlRequests());
        long runtimeRevision = store.resolve(GOAL_ID).revision();
        assertEquals(1, new FileSystemRuntimeEventStore(eventRoot)
                .resolve(GOAL_ID).revision());
        Files.delete(onlyPublicationPoint(publicationRoot));

        Execution recovered = executeWithEvents(
                spoolRoot,
                runtimeRoot,
                eventRoot,
                publicationRoot,
                "1",
                messageFile);

        assertEquals(0, recovered.exitCode());
        assertTrue(recovered.stdout().contains("status=REPLAYED"));
        assertTrue(recovered.stdout().contains("spoolStatus=ACKNOWLEDGED"));
        assertEquals(runtimeRevision, store.resolve(GOAL_ID).revision());
        assertEquals(1, new FileSystemRuntimeEventStore(eventRoot)
                .resolve(GOAL_ID).revision());
        assertEquals(1, publicationPointCount(publicationRoot));
    }

    @Test
    void rejectsPartialOrInvalidEventCompositionBeforePointResolution()
            throws Exception {
        Path missingSpool = temporaryRoot.resolve("missing-options-spool");
        Path runtimeRoot = temporaryRoot.resolve("missing-options-runtime");
        Path eventRoot = temporaryRoot.resolve("missing-options-events");
        Path publicationRoot = temporaryRoot.resolve("missing-options-publications");

        Execution partial = executeArguments(new String[] {
                "scheduler-receive-control",
                "--transport-spool-root", missingSpool.toString(),
                "--message-file", "00000000-0000-0000-0000-000000000899.transport",
                "--destination-name", DESTINATION,
                "--runtime-root", runtimeRoot.toString(),
                "--goal-id", GOAL_ID,
                "--runtime-event-root", eventRoot.toString()
        });
        Execution overflow = executeWithEvents(
                missingSpool,
                runtimeRoot,
                eventRoot,
                publicationRoot,
                "4097",
                "00000000-0000-0000-0000-000000000899.transport");

        assertEquals(CliExitCode.USAGE_OR_CONFIGURATION.code(), partial.exitCode());
        assertTrue(partial.stderr().contains("must be supplied together"));
        assertEquals(CliExitCode.USAGE_OR_CONFIGURATION.code(), overflow.exitCode());
        assertTrue(overflow.stderr().contains("must not exceed 4096"));
        assertFalse(Files.exists(runtimeRoot));
        assertFalse(Files.exists(eventRoot));
        assertFalse(Files.exists(publicationRoot));
    }

    private FileSystemAgentRuntimeStateStore activeRuntime(Path runtimeRoot)
            throws Exception {
        FileSystemAgentRuntimeStateStore store =
                new FileSystemAgentRuntimeStateStore(runtimeRoot);
        DurableAgentRuntime runtime = DurableAgentRuntime.create(
                GOAL_ID, workItem(), store, Clock.systemUTC());
        runtime.beginAgentRun(AGENT_RUN_ID);
        runtime.markReady(AGENT_RUN_ID);
        return store;
    }

    private Execution execute(
            Path spoolRoot,
            Path runtimeRoot,
            String messageFile) throws Exception {
        return executeArguments(new String[] {
                "scheduler-receive-control",
                "--transport-spool-root", spoolRoot.toString(),
                "--message-file", messageFile,
                "--destination-name", DESTINATION,
                "--runtime-root", runtimeRoot.toString(),
                "--goal-id", GOAL_ID
        });
    }

    private Execution executeWithEvents(
            Path spoolRoot,
            Path runtimeRoot,
            Path eventRoot,
            Path publicationRoot,
            String capacity,
            String messageFile) throws Exception {
        return executeArguments(new String[] {
                "scheduler-receive-control",
                "--transport-spool-root", spoolRoot.toString(),
                "--message-file", messageFile,
                "--destination-name", DESTINATION,
                "--runtime-root", runtimeRoot.toString(),
                "--goal-id", GOAL_ID,
                "--runtime-event-root", eventRoot.toString(),
                "--runtime-event-publication-root", publicationRoot.toString(),
                "--max-pending-runtime-event-publications", capacity
        });
    }

    private Execution executeArguments(String[] arguments) throws Exception {
        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        ByteArrayOutputStream stderr = new ByteArrayOutputStream();
        int exitCode;
        try (PrintStream out = new PrintStream(
                        stdout, true, StandardCharsets.UTF_8);
                PrintStream err = new PrintStream(
                        stderr, true, StandardCharsets.UTF_8)) {
            exitCode = new EnhancerCli().execute(arguments, out, err);
        }
        return new Execution(
                exitCode,
                stdout.toString(StandardCharsets.UTF_8),
                stderr.toString(StandardCharsets.UTF_8));
    }

    private static WorkItem workItem() {
        return new WorkItem(
                WORK_ITEM_ID,
                "runtime-worker",
                workMessage());
    }

    private static MessageEnvelope workMessage() {
        return new MessageEnvelope(
                WORK_MESSAGE_ID,
                "control-cli-correlation",
                Optional.empty(),
                "control-cli-logical-run",
                "control-cli-test",
                Instant.parse("2026-07-29T01:30:00Z"),
                new WorkPayload(
                        new ApprovedTaskRevision(
                                "connect-control-spool-to-durable-runtime-request",
                                "CURRENT_TASK.md",
                                "a".repeat(64)),
                        "b".repeat(64),
                        Set.of("read-file")));
    }

    private static MessageEnvelope controlMessage() {
        return controlMessage(ControlSignal.PAUSE);
    }

    private static MessageEnvelope controlMessage(ControlSignal signal) {
        return new MessageEnvelope(
                CONTROL_MESSAGE_ID,
                "control-cli-correlation",
                Optional.of(WORK_MESSAGE_ID),
                "control-cli-logical-run",
                "untrusted-control-cli-test",
                Instant.parse("2026-07-29T02:00:00Z"),
                new ControlPayload(signal, "record only"));
    }

    private Path onlyPublicationPoint(Path publicationRoot) throws Exception {
        try (var paths = Files.list(publicationRoot)) {
            List<Path> points = paths.filter(path -> path.getFileName().toString()
                            .endsWith(FileSystemRuntimeEventPublisher.FILE_SUFFIX))
                    .toList();
            assertEquals(1, points.size());
            return points.get(0);
        }
    }

    private long publicationPointCount(Path publicationRoot) throws Exception {
        try (var paths = Files.list(publicationRoot)) {
            return paths.filter(path -> path.getFileName().toString()
                            .endsWith(FileSystemRuntimeEventPublisher.FILE_SUFFIX))
                    .count();
        }
    }

    private record Execution(int exitCode, String stdout, String stderr) {
    }
}
