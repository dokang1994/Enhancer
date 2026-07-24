package com.enhancer.cli;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.enhancer.bus.BackpressurePolicy;
import com.enhancer.bus.DeliveryDestination;
import com.enhancer.bus.FileSpoolMessageTransport;
import com.enhancer.bus.MessageEnvelope;
import com.enhancer.bus.TransportMessage;
import com.enhancer.bus.WorkPayload;
import com.enhancer.runtime.DurableAgentRuntime;
import com.enhancer.runtime.DurableSingleWorkerSchedulerQueue;
import com.enhancer.runtime.FileSystemAgentRuntimeStateStore;
import com.enhancer.runtime.FileSystemPendingFinalizationStore;
import com.enhancer.runtime.FileSystemSchedulerQueueStore;
import com.enhancer.runtime.PendingFinalization;
import com.enhancer.runtime.QueuedWork;
import com.enhancer.runtime.WorkItem;
import com.enhancer.workspace.ApprovedTaskRevision;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class EnhancerCliSchedulerInvocationStatusIntegrationTest {
    private static final String QUEUE_ID =
            "00000000-0000-0000-0000-000000000b11";
    private static final String WORK_ITEM_ID =
            "00000000-0000-0000-0000-000000000b12";
    private static final String GOAL_ID =
            "00000000-0000-0000-0000-000000000b13";
    private static final String AGENT_RUN_ID =
            "00000000-0000-0000-0000-000000000b14";
    private static final Instant NOW =
            Instant.parse("2026-07-24T13:00:00Z");

    @TempDir
    Path temporaryRoot;

    @Test
    void reportsNoCorrelatedCycleWithoutCreatingInvocationOrOtherRoots()
            throws Exception {
        Path queueRoot = temporaryRoot.resolve("queue");
        Path runtimeRoot = temporaryRoot.resolve("runtime");
        Path checkpointRoot = temporaryRoot.resolve("checkpoint");
        Path recordRoot = temporaryRoot.resolve("records");
        Path invocationRoot = temporaryRoot.resolve("invocations");
        DurableSingleWorkerSchedulerQueue.create(
                QUEUE_ID, 16, new FileSystemSchedulerQueueStore(queueRoot));
        Path queueArtifact = queueRoot.resolve(QUEUE_ID + ".scheduler-queue");
        byte[] before = Files.readAllBytes(queueArtifact);
        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        ByteArrayOutputStream stderr = new ByteArrayOutputStream();

        int exitCode = new EnhancerCli().execute(new String[] {
                "scheduler-invocation-status",
                "--queue-root", queueRoot.toString(),
                "--queue-id", QUEUE_ID,
                "--runtime-root", runtimeRoot.toString(),
                "--cycle-checkpoint-root", checkpointRoot.toString(),
                "--run-record-root", recordRoot.toString(),
                "--invocation-root", invocationRoot.toString(),
                "--limit", "8"
        }, new PrintStream(stdout, true, StandardCharsets.UTF_8),
                new PrintStream(stderr, true, StandardCharsets.UTF_8));

        String output = stdout.toString(StandardCharsets.UTF_8);
        assertEquals(0, exitCode);
        assertEquals("NO_CORRELATED_CYCLE", value(output, "status"));
        assertEquals("false", value(output, "invocationPresent"));
        assertEquals("0", value(output, "totalMessages"));
        assertEquals("8", value(output, "requestedLimit"));
        assertEquals("0", value(output, "returnedMessages"));
        assertFalse(output.length() > EnhancerCli.MAX_DIAGNOSTIC_CHARACTERS);
        assertArrayEquals(before, Files.readAllBytes(queueArtifact));
        assertFalse(Files.exists(runtimeRoot));
        assertFalse(Files.exists(checkpointRoot));
        assertFalse(Files.exists(recordRoot));
        assertFalse(Files.exists(invocationRoot));
        assertEquals("", stderr.toString(StandardCharsets.UTF_8));
    }

    @Test
    void reportsValidatedWorkAndFailsClosedOnCorruptionWithoutMutation()
            throws Exception {
        Path base = temporaryRoot.resolve("work");
        Path queueRoot = base.resolve("queue");
        Path runtimeRoot = base.resolve("runtime");
        Path checkpointRoot = base.resolve("checkpoint");
        Path recordRoot = base.resolve("records");
        Path invocationRoot = base.resolve("invocations");
        WorkItem work = workItem();
        DurableSingleWorkerSchedulerQueue queue =
                DurableSingleWorkerSchedulerQueue.create(
                        QUEUE_ID,
                        16,
                        new FileSystemSchedulerQueueStore(queueRoot));
        queue.enqueue(new QueuedWork(work, List.of()));
        DurableAgentRuntime runtime = DurableAgentRuntime.create(
                GOAL_ID,
                work,
                new FileSystemAgentRuntimeStateStore(runtimeRoot),
                Clock.fixed(NOW, ZoneOffset.UTC));
        runtime.beginAgentRun(AGENT_RUN_ID);
        new FileSystemPendingFinalizationStore(checkpointRoot).record(
                new PendingFinalization(
                        GOAL_ID, AGENT_RUN_ID, Optional.empty()));
        Path workSpool = invocationRoot.resolve(GOAL_ID)
                .resolve(AGENT_RUN_ID).resolve("work");
        assertTrue(new FileSpoolMessageTransport(
                workSpool, BackpressurePolicy.of(1))
                .send(new TransportMessage(
                        DeliveryDestination.queue("work"),
                        work.workMessage()))
                .status()
                .isAccepted());
        byte[] queueBefore = Files.readAllBytes(
                queueRoot.resolve(QUEUE_ID + ".scheduler-queue"));

        Captured available = execute(
                queueRoot,
                runtimeRoot,
                checkpointRoot,
                recordRoot,
                invocationRoot);

        assertEquals(0, available.exitCode());
        assertEquals(
                "WORK_MESSAGE_AWAITING_RESULT",
                value(available.stdout(), "status"));
        assertEquals("1", value(available.stdout(), "totalMessages"));
        assertTrue(available.stdout().length()
                <= EnhancerCli.MAX_DIAGNOSTIC_CHARACTERS);
        Path message;
        try (var entries = Files.list(workSpool)) {
            message = entries.findFirst().orElseThrow();
        }
        byte[] corrupted = Files.readAllBytes(message);
        corrupted[corrupted.length - 1] ^= 1;
        Files.write(message, corrupted);

        Captured refused = execute(
                queueRoot,
                runtimeRoot,
                checkpointRoot,
                recordRoot,
                invocationRoot);

        assertEquals(70, refused.exitCode());
        assertEquals("", refused.stdout());
        assertTrue(refused.stderr().contains("status=ERROR"));
        assertArrayEquals(queueBefore, Files.readAllBytes(
                queueRoot.resolve(QUEUE_ID + ".scheduler-queue")));
        assertArrayEquals(corrupted, Files.readAllBytes(message));
    }

    private static Captured execute(
            Path queueRoot,
            Path runtimeRoot,
            Path checkpointRoot,
            Path recordRoot,
            Path invocationRoot) {
        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        ByteArrayOutputStream stderr = new ByteArrayOutputStream();
        int exitCode = new EnhancerCli().execute(new String[] {
                "scheduler-invocation-status",
                "--queue-root", queueRoot.toString(),
                "--queue-id", QUEUE_ID,
                "--runtime-root", runtimeRoot.toString(),
                "--cycle-checkpoint-root", checkpointRoot.toString(),
                "--run-record-root", recordRoot.toString(),
                "--invocation-root", invocationRoot.toString(),
                "--limit", "8"
        }, new PrintStream(stdout, true, StandardCharsets.UTF_8),
                new PrintStream(stderr, true, StandardCharsets.UTF_8));
        return new Captured(
                exitCode,
                stdout.toString(StandardCharsets.UTF_8),
                stderr.toString(StandardCharsets.UTF_8));
    }

    private static WorkItem workItem() {
        return new WorkItem(
                WORK_ITEM_ID,
                "read-file-worker",
                new MessageEnvelope(
                        "00000000-0000-0000-0000-000000000b15",
                        "invocation-cli-correlation",
                        Optional.empty(),
                        "invocation-cli-run",
                        "invocation-cli-test",
                        NOW,
                        new WorkPayload(
                                new ApprovedTaskRevision(
                                        "invocation-cli-test",
                                        "CURRENT_TASK.md",
                                        "a".repeat(64)),
                                "b".repeat(64),
                                Set.of("read-file"))));
    }

    private static String value(String output, String key) {
        String prefix = key + "=";
        return output.lines()
                .filter(line -> line.startsWith(prefix))
                .map(line -> line.substring(prefix.length()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("missing " + key));
    }

    private record Captured(int exitCode, String stdout, String stderr) {
    }
}
