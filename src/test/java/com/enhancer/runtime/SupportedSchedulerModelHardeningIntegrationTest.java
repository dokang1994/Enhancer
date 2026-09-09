package com.enhancer.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.enhancer.cli.EnhancerCli;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SupportedSchedulerModelHardeningIntegrationTest {
    @TempDir
    Path temporaryRoot;

    @Test
    void supportedDeniedModelInvokeRemainsAPreCallRefusalWithoutEffects()
            throws Exception {
        SubmittedModelWorkerEnvironment environment =
                SubmittedModelWorkerEnvironment.deniedModelInvoke(
                        temporaryRoot.resolve("denied-model-invoke"));
        SubmittedModelWorkerEnvironment.CliExecution submission =
                environment.supportedSubmit();
        assertEquals(0, submission.exitCode(), submission.stderr());
        assertTrue(submission.stdout().startsWith("status=ADMITTED\n"));

        Execution refusal = execute(
                environment.schedulerArguments("scheduler-cycle"));

        assertNotEquals(0, refusal.exitCode(), refusal.stdout() + refusal.stderr());
        PendingFinalization pending =
                environment.checkpointStore().findPending().orElseThrow();
        assertEquals(Optional.empty(), pending.runRecordReference());
        assertEquals(Optional.empty(), pending.replacementAgentRunId());
        AgentRuntimeState runtime = environment.runtimeStore().resolve(pending.goalId());
        assertEquals(RuntimeGoalStatus.ACTIVE, runtime.goal().status());
        assertEquals(1, runtime.agentRuns().size());
        RuntimeAgentRun run = runtime.agentRuns().get(0);
        assertEquals(RuntimeAgentRunStatus.EXECUTING, run.status());
        assertTrue(run.lease().isPresent());
        assertTrue(run.resultMessage().isEmpty());
        assertEquals(0, runtime.completedAttempts());
        assertTrue(runtime.retryDecisions().isEmpty());
        SchedulerQueueState queue = environment.queueState();
        assertEquals(
                Optional.of(environment.workItemId()),
                queue.activeWork().map(queued -> queued.workItem().workItemId()));
        assertTrue(queue.pendingWork().isEmpty());
        assertTrue(queue.completedWorkItemIds().isEmpty());
        assertTrue(queue.failedWorkItemIds().isEmpty());
        assertTrue(environment.runRecordStore().references().isEmpty());
        ExternalEffectLedgerState effects =
                environment.effectStore().resolve(pending.goalId());
        assertEquals(0L, effects.revision());
        assertTrue(effects.records().isEmpty());
        assertEquals(0L, environment.evidenceFileCount());
        assertEquals(0L, environment.resultPointFileCount());
        assertFalse(refusal.stdout().contains("model-invoke"));
        assertFalse(refusal.stderr().contains("model-invoke"));
    }

    @Test
    void supportedCapabilityMismatchRemainsAPreCallRefusalWithoutEffects()
            throws Exception {
        SubmittedModelWorkerEnvironment environment =
                SubmittedModelWorkerEnvironment.capabilityMismatch(
                        temporaryRoot.resolve("capability-mismatch"));
        SubmittedModelWorkerEnvironment.CliExecution submission =
                environment.supportedSubmit();
        assertEquals(0, submission.exitCode(), submission.stderr());
        assertTrue(submission.stdout().startsWith("status=ADMITTED\n"));

        Execution refusal = execute(
                environment.schedulerArguments("scheduler-cycle"));

        assertNotEquals(0, refusal.exitCode(), refusal.stdout() + refusal.stderr());
        PendingFinalization pending =
                environment.checkpointStore().findPending().orElseThrow();
        assertEquals(Optional.empty(), pending.runRecordReference());
        assertEquals(Optional.empty(), pending.replacementAgentRunId());
        AgentRuntimeState runtime = environment.runtimeStore().resolve(pending.goalId());
        assertEquals(RuntimeGoalStatus.ACTIVE, runtime.goal().status());
        assertEquals(1, runtime.agentRuns().size());
        RuntimeAgentRun run = runtime.agentRuns().get(0);
        assertEquals(RuntimeAgentRunStatus.EXECUTING, run.status());
        assertTrue(run.lease().isPresent());
        assertTrue(run.resultMessage().isEmpty());
        assertEquals(0, runtime.completedAttempts());
        assertTrue(runtime.retryDecisions().isEmpty());
        SchedulerQueueState queue = environment.queueState();
        assertEquals(
                Optional.of(environment.workItemId()),
                queue.activeWork().map(queued -> queued.workItem().workItemId()));
        assertTrue(queue.pendingWork().isEmpty());
        assertTrue(queue.completedWorkItemIds().isEmpty());
        assertTrue(queue.failedWorkItemIds().isEmpty());
        assertTrue(environment.runRecordStore().references().isEmpty());
        ExternalEffectLedgerState effects =
                environment.effectStore().resolve(pending.goalId());
        assertEquals(0L, effects.revision());
        assertTrue(effects.records().isEmpty());
        assertEquals(0L, environment.evidenceFileCount());
        assertEquals(0L, environment.resultPointFileCount());
    }

    @Test
    void supportedModelRecoveryRepublishesTheExactRuntimeEventWithoutReinvocation()
            throws Exception {
        SubmittedModelWorkerEnvironment environment =
                SubmittedModelWorkerEnvironment.verified(
                        temporaryRoot.resolve("event-recovery"));
        SubmittedModelWorkerEnvironment.CliExecution submission =
                environment.supportedSubmit();
        assertEquals(0, submission.exitCode(), submission.stderr());
        assertTrue(submission.stdout().startsWith("status=ADMITTED\n"));

        Execution first = execute(environment.schedulerArgumentsWithRuntimeEvents(
                "scheduler-cycle", 1));

        assertNotEquals(0, first.exitCode(), first.stdout() + first.stderr());
        String goalId = environment.soleGoalId();
        AgentRuntimeState firstRuntime = environment.runtimeStore().resolve(goalId);
        assertEquals(RuntimeGoalStatus.COMPLETED, firstRuntime.goal().status());
        assertEquals(1, firstRuntime.agentRuns().size());
        assertEquals(
                RuntimeAgentRunStatus.COMPLETED,
                firstRuntime.agentRuns().get(0).status());
        long runtimeRevision = firstRuntime.revision();
        SchedulerQueueState firstQueue = environment.queueState();
        assertEquals(Set.of(environment.workItemId()), firstQueue.completedWorkItemIds());
        assertTrue(firstQueue.failedWorkItemIds().isEmpty());
        long queueRevision = firstQueue.revision();
        assertEquals(1, environment.runRecordStore().references().size());
        assertTrue(environment.checkpointStore().findPending().isPresent());
        long evidenceFileCount = environment.evidenceFileCount();
        long resultPointFileCount = environment.resultPointFileCount();

        FileSystemRuntimeEventStore eventStore =
                new FileSystemRuntimeEventStore(environment.eventRoot());
        RuntimeEventStream firstStream = eventStore.resolve(goalId);
        assertEquals(2L, firstStream.revision());
        assertEquals(
                List.of(
                        RuntimeEventKind.VERIFICATION_RECORDED,
                        RuntimeEventKind.WORK_ITEM_TERMINATED),
                firstStream.events().stream().map(RuntimeEvent::kind).toList());
        Path eventArtifact = environment.eventRoot().resolve(
                goalId + ".runtime-events");
        byte[] firstEventBytes = Files.readAllBytes(eventArtifact);
        FileTime firstEventTime = Files.getLastModifiedTime(eventArtifact);
        Path verificationPoint = solePendingPoint(environment.publicationRoot());
        Execution acknowledged = execute(new String[] {
                "runtime-event-acknowledge",
                "--runtime-event-root", environment.eventRoot().toString(),
                "--runtime-event-publication-root",
                environment.publicationRoot().toString(),
                "--publication-file", verificationPoint.getFileName().toString()
        });
        assertEquals(0, acknowledged.exitCode(), acknowledged.stderr());
        assertTrue(pendingPoints(environment.publicationRoot()).isEmpty());

        Execution recovered = execute(environment.schedulerArgumentsWithRuntimeEvents(
                "scheduler-cycle", 1));

        assertEquals(0, recovered.exitCode(), recovered.stderr());
        assertEquals(runtimeRevision, environment.runtimeStore().resolve(goalId).revision());
        assertEquals(queueRevision, environment.queueState().revision());
        assertEquals(1, environment.runRecordStore().references().size());
        assertEquals(evidenceFileCount, environment.evidenceFileCount());
        assertEquals(resultPointFileCount, environment.resultPointFileCount());
        assertTrue(environment.checkpointStore().findPending().isEmpty());
        RuntimeEventStream recoveredStream = eventStore.resolve(goalId);
        assertEquals(firstStream.revision(), recoveredStream.revision());
        assertEquals(firstStream.events(), recoveredStream.events());
        assertTrue(Arrays.equals(firstEventBytes, Files.readAllBytes(eventArtifact)));
        assertEquals(firstEventTime, Files.getLastModifiedTime(eventArtifact));
        assertEquals(1, pendingPoints(environment.publicationRoot()).size());
        assertEquals(1, acknowledgedPoints(environment.publicationRoot()).size());
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

    private Execution execute(String[] arguments) {
        ByteArrayOutputStream standardOutput = new ByteArrayOutputStream();
        ByteArrayOutputStream standardError = new ByteArrayOutputStream();
        int exitCode = new EnhancerCli().execute(
                arguments,
                new PrintStream(standardOutput, true, StandardCharsets.UTF_8),
                new PrintStream(standardError, true, StandardCharsets.UTF_8));
        return new Execution(
                exitCode,
                standardOutput.toString(StandardCharsets.UTF_8),
                standardError.toString(StandardCharsets.UTF_8));
    }

    private record Execution(int exitCode, String stdout, String stderr) {
    }
}
