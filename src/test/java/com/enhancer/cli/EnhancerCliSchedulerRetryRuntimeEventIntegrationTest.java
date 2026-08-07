package com.enhancer.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.enhancer.bus.MessageEnvelope;
import com.enhancer.bus.WorkPayload;
import com.enhancer.run.FileSystemRunRecordStore;
import com.enhancer.runtime.AgentRunRetryRefusalReason;
import com.enhancer.runtime.DurableAgentRuntime;
import com.enhancer.runtime.DurableSingleWorkerSchedulerQueue;
import com.enhancer.runtime.FileSystemAgentRuntimeStateStore;
import com.enhancer.runtime.FileSystemPendingFinalizationStore;
import com.enhancer.runtime.FileSystemRuntimeEventPublisher;
import com.enhancer.runtime.FileSystemRuntimeEventStore;
import com.enhancer.runtime.FileSystemSchedulerQueueStore;
import com.enhancer.runtime.PendingFinalization;
import com.enhancer.runtime.QueuedWork;
import com.enhancer.runtime.RuntimeEvent;
import com.enhancer.runtime.RuntimeEventBinding;
import com.enhancer.runtime.RuntimeEventDetail;
import com.enhancer.runtime.RuntimeEventKind;
import com.enhancer.runtime.RuntimeEventReferenceKind;
import com.enhancer.runtime.RuntimeEventStream;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class EnhancerCliSchedulerRetryRuntimeEventIntegrationTest {
    private static final String QUEUE_ID =
            "00000000-0000-0000-0000-000000001501";
    private static final String WORK_ID =
            "00000000-0000-0000-0000-000000001502";
    private static final String WORK_MESSAGE_ID =
            "00000000-0000-0000-0000-000000001503";
    private static final String GOAL_ID =
            "00000000-0000-0000-0000-000000001504";
    private static final String FIRST_AGENT_RUN_ID =
            "00000000-0000-0000-0000-000000001505";

    @TempDir
    Path temporaryRoot;

    @ParameterizedTest
    @ValueSource(strings = {
            "scheduler-cycle", "scheduler-drain", "scheduler-service"
    })
    void publishesRetryDecisionStartAndRefusalFromEverySchedulerExecutionCommand(
            String commandName) throws Exception {
        Layout layout = layout(commandName);
        WorkItem workItem = prepareRetryingWork(layout);

        Execution execution = execute(layout, commandName);

        assertEquals(40, execution.exitCode(), execution.stderr());
        assertTrue(execution.stdout().contains("status=FAILED"), execution.stdout());

        DurableAgentRuntime runtime = DurableAgentRuntime.recover(
                GOAL_ID,
                new FileSystemAgentRuntimeStateStore(layout.runtimeRoot()),
                Clock.systemUTC());
        assertEquals(RuntimeGoalStatus.FAILED, runtime.goal().status());
        assertEquals(2, runtime.agentRuns().size());
        String replacementAgentRunId = runtime.agentRuns().get(1).agentRunId();

        RuntimeEventStream stream = new FileSystemRuntimeEventStore(
                layout.eventRoot()).resolve(GOAL_ID);
        assertEquals(3, stream.revision());
        assertEquals(
                List.of(
                        RuntimeEventKind.RETRY_DECISION_RECORDED,
                        RuntimeEventKind.RETRY_STARTED,
                        RuntimeEventKind.RETRY_DECISION_RECORDED),
                stream.events().stream().map(RuntimeEvent::kind).toList());

        RuntimeEventBinding expectedBinding = new RuntimeEventBinding(
                GOAL_ID,
                workItem.workItemId(),
                workItem.taskRevision(),
                workItem.snapshotId(),
                workItem.logicalRunId(),
                workItem.workMessage().correlationId());
        RuntimeEvent admitted = stream.events().get(0);
        RuntimeEvent started = stream.events().get(1);
        RuntimeEvent refused = stream.events().get(2);
        assertEquals(expectedBinding, admitted.binding());
        assertEquals(expectedBinding, started.binding());
        assertEquals(expectedBinding, refused.binding());
        assertEquals(FIRST_AGENT_RUN_ID, admitted.agentRunId());
        assertEquals(replacementAgentRunId, started.agentRunId());
        assertEquals(replacementAgentRunId, refused.agentRunId());
        assertEquals(
                new RuntimeEventDetail.RetryDecisionRecorded(
                        true, Optional.empty()),
                admitted.detail());
        assertEquals(
                new RuntimeEventDetail.RetryStarted(FIRST_AGENT_RUN_ID),
                started.detail());
        assertEquals(
                new RuntimeEventDetail.RetryDecisionRecorded(
                        false,
                        Optional.of(AgentRunRetryRefusalReason.ATTEMPTS_EXHAUSTED)),
                refused.detail());
        assertEquals(
                runtime.agentRuns().get(0).resultMessage().orElseThrow().messageId(),
                admitted.causationId().orElseThrow());
        assertEquals(admitted.causationId(), started.causationId());
        assertEquals(
                runtime.agentRuns().get(1).resultMessage().orElseThrow().messageId(),
                refused.causationId().orElseThrow());
        assertEquals(
                List.of(
                        RuntimeEventReferenceKind.RETRY_DECISION,
                        RuntimeEventReferenceKind.RUNTIME_STATE),
                admitted.authoritativeReferences().stream()
                        .map(reference -> reference.kind())
                        .toList());
        assertEquals(
                List.of(
                        RuntimeEventReferenceKind.RETRY_DECISION,
                        RuntimeEventReferenceKind.RUNTIME_STATE),
                started.authoritativeReferences().stream()
                        .map(reference -> reference.kind())
                        .toList());
        assertEquals(
                List.of(
                        RuntimeEventReferenceKind.RETRY_DECISION,
                        RuntimeEventReferenceKind.RUNTIME_STATE),
                refused.authoritativeReferences().stream()
                        .map(reference -> reference.kind())
                        .toList());

        List<Path> points = pendingPoints(layout.publicationRoot());
        assertEquals(3, points.size());
        for (Path point : points) {
            Execution read = executeRead(layout, point.getFileName().toString());
            assertEquals(0, read.exitCode(), read.stderr());
            assertTrue(read.stdout().contains("status=AVAILABLE"), read.stdout());
            assertTrue(read.stdout().contains("goalId=" + GOAL_ID), read.stdout());
        }

        DurableSingleWorkerSchedulerQueue queue =
                DurableSingleWorkerSchedulerQueue.recover(
                        QUEUE_ID,
                        new FileSystemSchedulerQueueStore(layout.queueRoot()));
        assertEquals(Set.of(WORK_ID), queue.failedWorkItemIds());
        assertTrue(queue.completedWorkItemIds().isEmpty());
        assertEquals(2, new FileSystemRunRecordStore(layout.recordRoot())
                .references().size());
        assertTrue(new FileSystemPendingFinalizationStore(layout.checkpointRoot())
                .findPending().isEmpty());
    }

    private WorkItem prepareRetryingWork(Layout layout) throws Exception {
        Files.createDirectories(layout.projectRoot());
        Files.writeString(
                layout.projectRoot().resolve("CURRENT_TASK.md"),
                "retry runtime event target\n",
                StandardCharsets.UTF_8);
        WorkItem workItem = new WorkItem(
                WORK_ID,
                "read-file-worker",
                new MessageEnvelope(
                        WORK_MESSAGE_ID,
                        "scheduler-retry-correlation",
                        Optional.empty(),
                        "scheduler-retry-logical-run",
                        "scheduler-retry-cli-test",
                        Instant.parse("2026-08-06T01:00:00Z"),
                        new WorkPayload(
                                new ApprovedTaskRevision(
                                        "compose-retry-runtime-event-publication",
                                        "CURRENT_TASK.md",
                                        "b".repeat(64)),
                                "c".repeat(64),
                                Set.of("read-file"),
                                Optional.of(new WorkPayload.ExecutionInput(
                                        "CURRENT_TASK.md", "a".repeat(64))))));
        DurableSingleWorkerSchedulerQueue queue =
                DurableSingleWorkerSchedulerQueue.create(
                        QUEUE_ID,
                        8,
                        new FileSystemSchedulerQueueStore(layout.queueRoot()));
        queue.enqueue(new QueuedWork(workItem, List.of()));
        new FileSystemPendingFinalizationStore(layout.checkpointRoot()).record(
                new PendingFinalization(
                        GOAL_ID, FIRST_AGENT_RUN_ID, Optional.empty()));
        return workItem;
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
                "--owner-id", "scheduler-retry-owner",
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
                "--runtime-event-publication-root",
                layout.publicationRoot().toString(),
                "--max-pending-runtime-event-publications", "3"));
        return arguments.toArray(String[]::new);
    }

    private List<Path> pendingPoints(Path root) throws Exception {
        if (Files.notExists(root)) {
            return List.of();
        }
        try (Stream<Path> paths = Files.list(root)) {
            return paths.filter(path -> path.getFileName().toString().endsWith(
                            FileSystemRuntimeEventPublisher.FILE_SUFFIX))
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
