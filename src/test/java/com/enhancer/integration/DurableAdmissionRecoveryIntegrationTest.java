package com.enhancer.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.enhancer.bus.DeliveryDestination;
import com.enhancer.bus.DeliveryStatus;
import com.enhancer.bus.InProcessMessageBus;
import com.enhancer.bus.MessageEnvelope;
import com.enhancer.bus.WorkPayload;
import com.enhancer.cli.EnhancerCli;
import com.enhancer.run.FileSystemRunRecordStore;
import com.enhancer.runtime.DurableSingleWorkerSchedulerQueue;
import com.enhancer.runtime.DurableWorkItemAdmissionHandler;
import com.enhancer.runtime.FileSystemSchedulerQueueStore;
import com.enhancer.workspace.ApprovedTaskRevision;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DurableAdmissionRecoveryIntegrationTest {
    private static final String QUEUE_ID =
            "00000000-0000-0000-0000-000000000901";
    private static final String MESSAGE_ID =
            "00000000-0000-0000-0000-000000000902";
    private static final DeliveryDestination DESTINATION =
            DeliveryDestination.queue("durable-admission-recovery");

    @TempDir
    Path temporaryRoot;

    @Test
    void exactFreshBusReplayAfterTerminalCycleChangesNoDurableOutcome()
            throws Exception {
        Path projectRoot = Files.createDirectories(temporaryRoot.resolve("project"));
        byte[] content = "durable admission recovery\n"
                .getBytes(StandardCharsets.UTF_8);
        Files.write(projectRoot.resolve("CURRENT_TASK.md"), content);
        String digest = HexFormat.of().formatHex(
                MessageDigest.getInstance("SHA-256").digest(content));
        Path queueRoot = temporaryRoot.resolve("queue");
        DurableSingleWorkerSchedulerQueue queue =
                DurableSingleWorkerSchedulerQueue.create(
                        QUEUE_ID,
                        8,
                        new FileSystemSchedulerQueueStore(queueRoot));
        InProcessMessageBus firstBus = bus(queue);
        MessageEnvelope work = workMessage(digest, "admission-recovery-test");
        assertEquals(
                DeliveryStatus.DELIVERED,
                firstBus.publish(DESTINATION, work).get(0).status());

        assertEquals(0, executeSchedulerCycle(projectRoot, queueRoot));
        DurableSingleWorkerSchedulerQueue terminal = recover(queueRoot);
        long terminalRevision = terminal.revision();
        int runRecords = new FileSystemRunRecordStore(
                temporaryRoot.resolve("records")).references().size();
        assertEquals(1, terminal.completedWorkItemIds().size());

        InProcessMessageBus restartedBus = bus(terminal);
        assertEquals(
                DeliveryStatus.DELIVERED,
                restartedBus.publish(DESTINATION, work).get(0).status());
        assertTrue(restartedBus.deadLetters().isEmpty());
        assertEquals(terminalRevision, terminal.revision());
        assertEquals(1, terminal.completedWorkItemIds().size());
        assertEquals(
                runRecords,
                new FileSystemRunRecordStore(
                        temporaryRoot.resolve("records")).references().size());

        InProcessMessageBus changedBus = bus(terminal);
        assertEquals(
                DeliveryStatus.FAILED,
                changedBus.publish(
                        DESTINATION,
                        workMessage(digest, "changed-producer"))
                        .get(0)
                        .status());
        assertEquals(1, changedBus.deadLetters().size());
        assertEquals(terminalRevision, terminal.revision());
    }

    private InProcessMessageBus bus(DurableSingleWorkerSchedulerQueue queue) {
        InProcessMessageBus bus = new InProcessMessageBus();
        bus.subscribe(
                DESTINATION,
                "durable-admission-recovery-handler",
                new DurableWorkItemAdmissionHandler("read-file-worker", queue));
        return bus;
    }

    private DurableSingleWorkerSchedulerQueue recover(Path queueRoot) throws Exception {
        return DurableSingleWorkerSchedulerQueue.recover(
                QUEUE_ID, new FileSystemSchedulerQueueStore(queueRoot));
    }

    private int executeSchedulerCycle(Path projectRoot, Path queueRoot) {
        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        ByteArrayOutputStream stderr = new ByteArrayOutputStream();
        return new EnhancerCli().execute(
                new String[] {
                        "scheduler-cycle",
                        "--project-root", projectRoot.toString(),
                        "--queue-root", queueRoot.toString(),
                        "--queue-id", QUEUE_ID,
                        "--runtime-root", temporaryRoot.resolve("runtime").toString(),
                        "--external-effect-root", temporaryRoot.resolve("effects").toString(),
                        "--cycle-checkpoint-root", temporaryRoot.resolve("checkpoint").toString(),
                        "--evidence-root", temporaryRoot.resolve("evidence").toString(),
                        "--run-record-root", temporaryRoot.resolve("records").toString(),
                        "--invocation-root", temporaryRoot.resolve("invocations").toString(),
                        "--owner-id", "admission-recovery-owner",
                        "--max-attempts", "2",
                        "--lease-millis", "300000",
                        "--process-timeout-millis", "30000"
                },
                new PrintStream(stdout, true, StandardCharsets.UTF_8),
                new PrintStream(stderr, true, StandardCharsets.UTF_8));
    }

    private MessageEnvelope workMessage(String digest, String producer) {
        return new MessageEnvelope(
                MESSAGE_ID,
                "admission-recovery-correlation",
                Optional.empty(),
                "admission-recovery-logical-run",
                producer,
                Instant.parse("2026-07-22T13:30:00Z"),
                new WorkPayload(
                        new ApprovedTaskRevision(
                                "persist-exact-durable-work-admission-history",
                                "CURRENT_TASK.md",
                                digest),
                        "c".repeat(64),
                        Set.of("read-file")));
    }
}
