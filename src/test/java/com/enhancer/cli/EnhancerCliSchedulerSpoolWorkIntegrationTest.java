package com.enhancer.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.enhancer.bus.FileSpoolMessageTransport;
import com.enhancer.bus.TransportMessage;
import com.enhancer.bus.WorkPayload;
import com.enhancer.context.RequiredProjectDocument;
import com.enhancer.runtime.DurableSingleWorkerSchedulerQueue;
import com.enhancer.runtime.FileSystemSchedulerQueueStore;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class EnhancerCliSchedulerSpoolWorkIntegrationTest {
    private static final String TASK_ID = "publish-governed-work";
    private static final String QUEUE_ID = "00000000-0000-0000-0000-000000000741";

    @TempDir
    Path temporaryRoot;

    @Test
    void publishesOneGovernedPointThenSeparatelyReceivesAndAcknowledgesIt()
            throws Exception {
        Path project = temporaryRoot.resolve("project");
        writeGovernedProject(project);
        String digest = writeTarget(project, "governed target\n");
        Path spool = temporaryRoot.resolve("spool");
        Path queue = temporaryRoot.resolve("queue");
        DurableSingleWorkerSchedulerQueue.create(
                QUEUE_ID, 8, new FileSystemSchedulerQueueStore(queue));

        Execution published = execute(spoolArguments(project, spool, digest, "8"));
        assertEquals(0, published.exitCode());
        assertTrue(published.stdout().contains("status=ACCEPTED"));
        String messageFile = value(published.stdout(), "messageFile");
        assertTrue(messageFile.endsWith(FileSpoolMessageTransport.FILE_SUFFIX));
        Path point = spool.resolve(messageFile);
        TransportMessage message = FileSpoolMessageTransport.read(point);
        WorkPayload payload = (WorkPayload) message.envelope().payload();
        assertEquals(TASK_ID, payload.taskRevision().taskId());
        assertEquals("target.txt", payload.executionInput().orElseThrow().targetPath());
        assertEquals(
                digest,
                payload.executionInput().orElseThrow().expectedContentSha256());
        assertTrue(payload.allowedTools().contains("read-file"));

        Execution received = execute(new String[] {
                "scheduler-receive-work",
                "--transport-spool-root", spool.toString(),
                "--message-file", messageFile,
                "--destination-name", "scheduler-work",
                "--queue-root", queue.toString(),
                "--queue-id", QUEUE_ID,
                "--required-capability", "read-file-worker"
        });
        assertEquals(0, received.exitCode());
        assertTrue(received.stdout().contains("status=ADMITTED"));
        assertTrue(received.stdout().contains("spoolStatus=ACKNOWLEDGED"));
        assertFalse(Files.exists(point));
        assertTrue(Files.isRegularFile(spool.resolve(
                messageFile.replace(".transport", ".received"))));
        DurableSingleWorkerSchedulerQueue admittedQueue =
                DurableSingleWorkerSchedulerQueue.recover(
                        QUEUE_ID, new FileSystemSchedulerQueueStore(queue));
        assertEquals(1, admittedQueue.pendingCount());
        assertEquals(
                message.envelope(),
                admittedQueue.claimNext().orElseThrow().workMessage());
    }

    @Test
    void reportsBackpressureAndUnavailableWithoutPartialPublication() throws Exception {
        Path project = temporaryRoot.resolve("refusals-project");
        writeGovernedProject(project);
        String digest = writeTarget(project, "target\n");
        Path spool = temporaryRoot.resolve("bounded");
        assertTrue(execute(spoolArguments(project, spool, digest, "1"))
                .stdout().contains("status=ACCEPTED"));
        Execution backpressured = execute(spoolArguments(project, spool, digest, "1"));
        assertTrue(backpressured.stdout().contains("status=BACKPRESSURED"));
        assertEquals("", value(backpressured.stdout(), "messageFile"));
        try (var files = Files.list(spool)) {
            assertEquals(1, files.filter(Files::isRegularFile).count());
        }

        Path unavailable = Files.writeString(
                temporaryRoot.resolve("occupied"), "x", StandardCharsets.UTF_8);
        Execution refused = execute(spoolArguments(project, unavailable, digest, "1"));
        assertTrue(refused.stdout().contains("status=UNAVAILABLE"));
        assertEquals("", value(refused.stdout(), "messageFile"));
        assertEquals("x", Files.readString(unavailable));
    }

    private String[] spoolArguments(
            Path project,
            Path spool,
            String digest,
            String maximum) {
        return new String[] {
                "scheduler-spool-work",
                "--project-root", project.toString(),
                "--transport-spool-root", spool.toString(),
                "--destination-name", "scheduler-work",
                "--task-id", TASK_ID,
                "--max-pending-publications", maximum,
                "--message-id", "00000000-0000-0000-0000-000000000742",
                "--correlation-id", "governed-correlation",
                "--logical-run-id", "governed-logical-run",
                "--producer", "scheduler-spool-work-test",
                "--occurred-at", "2026-07-28T09:00:00Z",
                "--target-path", "target.txt",
                "--expected-sha256", digest
        };
    }

    private void writeGovernedProject(Path project) throws Exception {
        String task = "# Current Task\n\n## Status\n\nIn Progress\n\n"
                + "## Task\n\nPublish governed work.\n\n"
                + "## Task ID\n\n" + TASK_ID + "\n\n"
                + "## Approval\n\nApproved by integration-test owner.\n\n"
                + "## Allowed Tools\n\n- read-file\n";
        for (RequiredProjectDocument document : RequiredProjectDocument.values()) {
            Path path = project.resolve(document.path());
            Files.createDirectories(path.getParent());
            Files.writeString(
                    path,
                    document == RequiredProjectDocument.CURRENT_TASK
                            ? task
                            : "# " + document.name() + "\n",
                    StandardCharsets.UTF_8);
        }
    }

    private String writeTarget(Path project, String content) throws Exception {
        Files.writeString(project.resolve("target.txt"), content, StandardCharsets.UTF_8);
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(
                content.getBytes(StandardCharsets.UTF_8)));
    }

    private Execution execute(String[] arguments) {
        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        ByteArrayOutputStream stderr = new ByteArrayOutputStream();
        int code = new EnhancerCli().execute(
                arguments,
                new PrintStream(stdout, true, StandardCharsets.UTF_8),
                new PrintStream(stderr, true, StandardCharsets.UTF_8));
        return new Execution(
                code,
                stdout.toString(StandardCharsets.UTF_8),
                stderr.toString(StandardCharsets.UTF_8));
    }

    private String value(String output, String key) {
        return output.lines()
                .filter(line -> line.startsWith(key + "="))
                .map(line -> line.substring(key.length() + 1))
                .findFirst()
                .orElseThrow();
    }

    private record Execution(int exitCode, String stdout, String stderr) {
    }
}
