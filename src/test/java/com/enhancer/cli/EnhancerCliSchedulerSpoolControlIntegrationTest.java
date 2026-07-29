package com.enhancer.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.enhancer.bus.ControlPayload;
import com.enhancer.bus.ControlSignal;
import com.enhancer.bus.FileSpoolMessageTransport;
import com.enhancer.bus.MessageEnvelope;
import com.enhancer.bus.TransportMessage;
import com.enhancer.bus.WorkPayload;
import com.enhancer.runtime.DurableAgentRuntime;
import com.enhancer.runtime.FileSystemAgentRuntimeStateStore;
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

class EnhancerCliSchedulerSpoolControlIntegrationTest {
    private static final String GOAL_ID =
            "00000000-0000-0000-0000-000000000831";
    private static final String AGENT_RUN_ID =
            "00000000-0000-0000-0000-000000000832";
    private static final String WORK_ITEM_ID =
            "00000000-0000-0000-0000-000000000833";
    private static final String WORK_MESSAGE_ID =
            "00000000-0000-0000-0000-000000000834";
    private static final String CONTROL_MESSAGE_ID =
            "00000000-0000-0000-0000-000000000835";
    private static final String DESTINATION = "runtime-controls";

    @TempDir
    Path temporaryRoot;

    @Test
    void publishesDerivedIntentThenSeparatelyReceivesAndPersistsIt()
            throws Exception {
        Path runtimeRoot = temporaryRoot.resolve("runtime");
        Path spoolRoot = temporaryRoot.resolve("spool");
        FileSystemAgentRuntimeStateStore store = activeRuntime(runtimeRoot);

        Execution published = execute(spoolArguments(
                runtimeRoot, spoolRoot, CONTROL_MESSAGE_ID));

        assertEquals(0, published.exitCode());
        assertTrue(published.stdout().contains("status=ACCEPTED"));
        String messageFile = value(published.stdout(), "messageFile");
        Path pending = spoolRoot.resolve(messageFile);
        TransportMessage spooled = FileSpoolMessageTransport.read(pending);
        MessageEnvelope control = spooled.envelope();
        assertEquals(workMessage().correlationId(), control.correlationId());
        assertEquals(workMessage().logicalRunId(), control.logicalRunId());
        assertEquals(Optional.of(WORK_MESSAGE_ID), control.causationId());
        assertEquals(
                new ControlPayload(ControlSignal.PAUSE, "operator intent"),
                control.payload());
        assertTrue(store.resolve(GOAL_ID).controlRequests().isEmpty());

        Execution received = execute(new String[] {
                "scheduler-receive-control",
                "--transport-spool-root", spoolRoot.toString(),
                "--message-file", messageFile,
                "--destination-name", DESTINATION,
                "--runtime-root", runtimeRoot.toString(),
                "--goal-id", GOAL_ID
        });

        assertEquals(0, received.exitCode());
        assertTrue(received.stdout().contains("status=RECORDED"));
        assertTrue(received.stdout().contains("spoolStatus=ACKNOWLEDGED"));
        assertFalse(Files.exists(pending));
        assertTrue(Files.isRegularFile(spoolRoot.resolve(
                messageFile.replace(".transport", ".received"))));
        assertEquals(
                List.of(control),
                DurableAgentRuntime.recover(
                                GOAL_ID, store, Clock.systemUTC())
                        .controlRequests());
    }

    @Test
    void refusesInactiveGoalAndBackpressureWithoutPartialControlPoint()
            throws Exception {
        Path runtimeRoot = temporaryRoot.resolve("refusal-runtime");
        Path inactiveSpool = temporaryRoot.resolve("inactive-spool");
        FileSystemAgentRuntimeStateStore store =
                new FileSystemAgentRuntimeStateStore(runtimeRoot);
        DurableAgentRuntime.create(
                GOAL_ID, workItem(), store, Clock.systemUTC());

        Execution inactive = execute(spoolArguments(
                runtimeRoot, inactiveSpool, CONTROL_MESSAGE_ID));

        assertEquals(CliExitCode.USAGE_OR_CONFIGURATION.code(), inactive.exitCode());
        assertTrue(inactive.stderr().contains("runtime configuration is invalid"));
        assertFalse(Files.exists(inactiveSpool));

        DurableAgentRuntime runtime = DurableAgentRuntime.recover(
                GOAL_ID, store, Clock.systemUTC());
        runtime.beginAgentRun(AGENT_RUN_ID);
        runtime.markReady(AGENT_RUN_ID);
        Path boundedSpool = temporaryRoot.resolve("bounded-spool");
        assertTrue(execute(spoolArguments(
                        runtimeRoot, boundedSpool, CONTROL_MESSAGE_ID))
                .stdout().contains("status=ACCEPTED"));

        Execution backpressured = execute(spoolArguments(
                runtimeRoot,
                boundedSpool,
                "00000000-0000-0000-0000-000000000836"));

        assertEquals(0, backpressured.exitCode());
        assertTrue(backpressured.stdout().contains("status=BACKPRESSURED"));
        assertEquals("", value(backpressured.stdout(), "messageFile"));
        try (var points = Files.list(boundedSpool)) {
            assertEquals(1, points.filter(Files::isRegularFile).count());
        }
        assertTrue(store.resolve(GOAL_ID).controlRequests().isEmpty());
    }

    private String[] spoolArguments(
            Path runtimeRoot,
            Path spoolRoot,
            String messageId) {
        return new String[] {
                "scheduler-spool-control",
                "--runtime-root", runtimeRoot.toString(),
                "--goal-id", GOAL_ID,
                "--transport-spool-root", spoolRoot.toString(),
                "--destination-name", DESTINATION,
                "--max-pending-publications", "1",
                "--message-id", messageId,
                "--producer", "untrusted-control-cli",
                "--occurred-at", "2026-07-29T04:00:00Z",
                "--signal", "PAUSE",
                "--reason", "operator intent"
        };
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

    private static String value(String output, String key) {
        return output.lines()
                .filter(line -> line.startsWith(key + "="))
                .map(line -> line.substring(key.length() + 1))
                .findFirst()
                .orElseThrow();
    }

    private static WorkItem workItem() {
        return new WorkItem(WORK_ITEM_ID, "runtime-worker", workMessage());
    }

    private static MessageEnvelope workMessage() {
        return new MessageEnvelope(
                WORK_MESSAGE_ID,
                "control-cli-publisher-correlation",
                Optional.empty(),
                "control-cli-publisher-logical-run",
                "work-producer",
                Instant.parse("2026-07-29T03:30:00Z"),
                new WorkPayload(
                        new ApprovedTaskRevision(
                                "publish-untrusted-control-intent-from-existing-goal-state",
                                "CURRENT_TASK.md",
                                "a".repeat(64)),
                        "b".repeat(64),
                        Set.of("read-file")));
    }

    private record Execution(int exitCode, String stdout, String stderr) {
    }
}
