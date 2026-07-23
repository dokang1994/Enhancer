package com.enhancer.cli;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.enhancer.bus.MessageEnvelope;
import com.enhancer.bus.WorkPayload;
import com.enhancer.runtime.DurableSingleWorkerSchedulerQueue;
import com.enhancer.runtime.FileSystemSchedulerQueueStore;
import com.enhancer.runtime.QueuedWork;
import com.enhancer.runtime.SchedulerQueueState;
import com.enhancer.runtime.WorkItem;
import com.enhancer.workspace.ApprovedTaskRevision;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class EnhancerCliSchedulerStatusIntegrationTest {
    private static final String QUEUE_ID =
            "00000000-0000-0000-0000-000000000901";
    private static final String VERIFIED_ID =
            "00000000-0000-0000-0000-000000000911";
    private static final String FAILED_ID =
            "00000000-0000-0000-0000-000000000912";
    private static final String ACTIVE_ID =
            "00000000-0000-0000-0000-000000000913";
    private static final String READY_ID =
            "00000000-0000-0000-0000-000000000914";
    private static final String BLOCKED_ID =
            "00000000-0000-0000-0000-000000000915";

    @TempDir
    Path temporaryRoot;

    @Test
    void reportsEveryStateWithoutRecoveringOrChangingThePersistedQueue()
            throws Exception {
        Path queueRoot = temporaryRoot.resolve("all-states");
        DurableSingleWorkerSchedulerQueue queue = queueWithEveryState(queueRoot);
        Path artifact = artifact(queueRoot, QUEUE_ID);
        byte[] bytesBefore = Files.readAllBytes(artifact);
        FileTime modifiedBefore = Files.getLastModifiedTime(artifact);
        long revisionBefore = queue.revision();

        Captured result = execute(queueRoot, QUEUE_ID, "5");

        assertEquals(0, result.exitCode());
        assertEquals("AVAILABLE", value(result.stdout(), "status"));
        assertEquals("0", value(result.stdout(), "exitCode"));
        assertEquals(Long.toString(revisionBefore),
                value(result.stdout(), "queueRevision"));
        assertEquals("5", value(result.stdout(), "totalWorkItems"));
        assertEquals("1", value(result.stdout(), "readyWorkItems"));
        assertEquals("1", value(result.stdout(), "blockedWorkItems"));
        assertEquals("1", value(result.stdout(), "activeWorkItems"));
        assertEquals("1", value(result.stdout(), "verifiedWorkItems"));
        assertEquals("1", value(result.stdout(), "failedWorkItems"));
        assertEquals("5", value(result.stdout(), "requestedLimit"));
        assertEquals("5", value(result.stdout(), "returnedWorkItems"));
        assertEquals(VERIFIED_ID + ",VERIFIED",
                value(result.stdout(), "workItem.1"));
        assertEquals(FAILED_ID + ",FAILED",
                value(result.stdout(), "workItem.2"));
        assertEquals(ACTIVE_ID + ",ACTIVE",
                value(result.stdout(), "workItem.3"));
        assertEquals(READY_ID + ",READY",
                value(result.stdout(), "workItem.4"));
        assertEquals(BLOCKED_ID + ",BLOCKED",
                value(result.stdout(), "workItem.5"));
        assertEquals("", result.stderr());

        assertArrayEquals(bytesBefore, Files.readAllBytes(artifact));
        assertEquals(modifiedBefore, Files.getLastModifiedTime(artifact));
        SchedulerQueueState after =
                new FileSystemSchedulerQueueStore(queueRoot).resolve(QUEUE_ID);
        assertEquals(revisionBefore, after.revision());
        assertEquals(ACTIVE_ID,
                after.activeWork().orElseThrow().workItem().workItemId());
    }

    @Test
    void reportsAnEmptyQueueWithoutChangingItsArtifact() throws Exception {
        Path queueRoot = temporaryRoot.resolve("empty");
        DurableSingleWorkerSchedulerQueue.create(
                QUEUE_ID,
                4,
                new FileSystemSchedulerQueueStore(queueRoot));
        Path artifact = artifact(queueRoot, QUEUE_ID);
        byte[] bytesBefore = Files.readAllBytes(artifact);

        Captured result = execute(queueRoot, QUEUE_ID, "4");

        assertEquals(0, result.exitCode());
        assertEquals("EMPTY", value(result.stdout(), "status"));
        assertEquals("0", value(result.stdout(), "totalWorkItems"));
        assertEquals("0", value(result.stdout(), "returnedWorkItems"));
        assertArrayEquals(bytesBefore, Files.readAllBytes(artifact));
    }

    @Test
    void missingQueueIsConfigurationFailureAndCreatesNoRoot() {
        Path missingRoot = temporaryRoot.resolve("missing");

        Captured result = execute(missingRoot, QUEUE_ID, "4");

        assertEquals(2, result.exitCode());
        assertTrue(result.stderr().contains("status=ERROR"));
        assertTrue(result.stderr().contains("exitCode=2"));
        assertFalse(Files.exists(missingRoot));
    }

    @Test
    void corruptedQueueIsAnInternalFailureAndIsNotRewritten()
            throws Exception {
        Path queueRoot = temporaryRoot.resolve("corrupt");
        DurableSingleWorkerSchedulerQueue.create(
                QUEUE_ID,
                4,
                new FileSystemSchedulerQueueStore(queueRoot));
        Path artifact = artifact(queueRoot, QUEUE_ID);
        byte[] corruptBytes = new byte[] {1, 2, 3, 4};
        Files.write(artifact, corruptBytes);

        Captured result = execute(queueRoot, QUEUE_ID, "4");

        assertEquals(70, result.exitCode());
        assertTrue(result.stderr().contains("status=ERROR"));
        assertTrue(result.stderr().contains("exitCode=70"));
        assertArrayEquals(corruptBytes, Files.readAllBytes(artifact));
    }

    @Test
    void maximumListedPrefixRemainsInsideTheCliOutputBound() throws Exception {
        Path queueRoot = temporaryRoot.resolve("bounded");
        DurableSingleWorkerSchedulerQueue queue =
                DurableSingleWorkerSchedulerQueue.create(
                        QUEUE_ID,
                        SchedulerStatusCliCommand.MAX_LISTED_WORK_ITEMS,
                        new FileSystemSchedulerQueueStore(queueRoot));
        for (int index = 1;
                index <= SchedulerStatusCliCommand.MAX_LISTED_WORK_ITEMS;
                index++) {
            queue.enqueue(queued(
                    String.format(
                            "00000000-0000-0000-0002-%012d", index),
                    List.of()));
        }

        Captured result = execute(
                queueRoot,
                QUEUE_ID,
                Integer.toString(
                        SchedulerStatusCliCommand.MAX_LISTED_WORK_ITEMS));

        assertEquals(0, result.exitCode());
        assertEquals(
                Integer.toString(
                        SchedulerStatusCliCommand.MAX_LISTED_WORK_ITEMS),
                value(result.stdout(), "returnedWorkItems"));
        assertTrue(result.stdout().contains(
                "workItem.48=00000000-0000-0000-0002-000000000048,READY"));
        assertTrue(result.stdout().length()
                <= EnhancerCli.MAX_DIAGNOSTIC_CHARACTERS);
        assertEquals("", result.stderr());
    }

    private DurableSingleWorkerSchedulerQueue queueWithEveryState(
            Path queueRoot) throws Exception {
        DurableSingleWorkerSchedulerQueue queue =
                DurableSingleWorkerSchedulerQueue.create(
                        QUEUE_ID,
                        8,
                        new FileSystemSchedulerQueueStore(queueRoot));
        queue.enqueue(queued(VERIFIED_ID, List.of()));
        queue.claimNext();
        queue.completeActiveVerified(VERIFIED_ID);
        queue.enqueue(queued(FAILED_ID, List.of()));
        queue.claimNext();
        queue.failActive(FAILED_ID);
        queue.enqueue(queued(ACTIVE_ID, List.of()));
        queue.claimNext();
        queue.enqueue(queued(READY_ID, List.of(VERIFIED_ID)));
        queue.enqueue(queued(BLOCKED_ID, List.of(FAILED_ID)));
        return queue;
    }

    private QueuedWork queued(String workItemId, List<String> dependencies) {
        ApprovedTaskRevision taskRevision = new ApprovedTaskRevision(
                "scheduler-status-test",
                "CURRENT_TASK.md",
                "a".repeat(64));
        long suffix = Long.parseLong(
                workItemId.substring(workItemId.length() - 12));
        MessageEnvelope envelope = new MessageEnvelope(
                String.format("00000000-0000-0000-0003-%012d", suffix),
                "scheduler-status-correlation",
                Optional.empty(),
                "scheduler-status-logical-run",
                "scheduler-status-cli-test",
                Instant.parse("2026-07-23T02:00:00Z"),
                new WorkPayload(
                        taskRevision,
                        "b".repeat(64),
                        Set.of("read-file")));
        return new QueuedWork(
                new WorkItem(workItemId, "read-file-worker", envelope),
                dependencies);
    }

    private Captured execute(Path queueRoot, String queueId, String limit) {
        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        ByteArrayOutputStream stderr = new ByteArrayOutputStream();
        int exitCode = new EnhancerCli().execute(
                new String[] {
                        "scheduler-status",
                        "--queue-root", queueRoot.toString(),
                        "--queue-id", queueId,
                        "--limit", limit
                },
                new PrintStream(stdout, true, StandardCharsets.UTF_8),
                new PrintStream(stderr, true, StandardCharsets.UTF_8));
        return new Captured(
                exitCode,
                stdout.toString(StandardCharsets.UTF_8),
                stderr.toString(StandardCharsets.UTF_8));
    }

    private Path artifact(Path queueRoot, String queueId) {
        return queueRoot.resolve(queueId + ".scheduler-queue");
    }

    private String value(String output, String key) {
        String prefix = key + "=";
        return output.lines()
                .filter(line -> line.startsWith(prefix))
                .map(line -> line.substring(prefix.length()))
                .findFirst()
                .orElseThrow();
    }

    private record Captured(int exitCode, String stdout, String stderr) {
    }
}
