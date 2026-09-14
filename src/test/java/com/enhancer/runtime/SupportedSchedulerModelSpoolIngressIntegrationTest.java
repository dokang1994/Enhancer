package com.enhancer.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.enhancer.cli.EnhancerCli;
import com.enhancer.kernel.VerificationStatus;
import com.enhancer.run.ResolvedModelRunRecord;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class SupportedSchedulerModelSpoolIngressIntegrationTest {
    @TempDir
    Path temporaryRoot;

    @ParameterizedTest
    @ValueSource(strings = {
            "scheduler-cycle", "scheduler-drain", "scheduler-service"
    })
    void separateSpoolReceiveAndSchedulerCommandsReachVerifiedCompletion(
            String command) throws Exception {
        SubmittedModelWorkerEnvironment environment =
                SubmittedModelWorkerEnvironment.verified(
                        temporaryRoot.resolve(command));

        publishAndReceive(environment);
        Execution execution = execute(environment.schedulerArguments(command));

        assertEquals(0, execution.exitCode(), execution.stderr());
        assertTrue(execution.stdout().contains(expectedStatus(command)));
        assertTrue(execution.stderr().isEmpty());
        assertFalse(execution.stdout().contains("deterministic-fake-v2"));
        assertFalse(execution.stdout().contains("model-invoke"));
        assertFalse(execution.stdout().contains(
                "submitted typed process integration prompt"));
        assertFalse(execution.stdout().contains("profile"));
        assertEquals(Set.of(environment.workItemId()),
                environment.queue().completedWorkItemIds());
        assertTrue(environment.queue().failedWorkItemIds().isEmpty());
        assertTrue(environment.checkpointStore().findPending().isEmpty());
        assertEquals(1, environment.runRecordStore().references().size());
        String goalId = environment.soleGoalId();
        AgentRuntimeState runtime = environment.runtimeStore().resolve(goalId);
        RuntimeAgentRun run = runtime.agentRuns().get(0);
        ResolvedModelRunRecord record = environment.runRecordStore().resolveModel(
                AgentRunRecordIdentity.reference(goalId, run.agentRunId()));
        assertEquals(VerificationStatus.VERIFIED,
                record.record().lifecycleRecord().verification().status());
        assertEquals(RuntimeAgentRunStatus.COMPLETED, run.status());
    }

    @Test
    void capabilityMismatchAfterSpoolReceiveRemainsAPreCallRefusalWithoutEffects()
            throws Exception {
        SubmittedModelWorkerEnvironment environment =
                SubmittedModelWorkerEnvironment.capabilityMismatch(
                        temporaryRoot.resolve("capability-mismatch"));

        publishAndReceive(environment);
        Execution refusal = execute(environment.schedulerArguments("scheduler-cycle"));

        assertNotEquals(0, refusal.exitCode(), refusal.stdout() + refusal.stderr());
        PendingFinalization pending =
                environment.checkpointStore().findPending().orElseThrow();
        assertEquals(Optional.empty(), pending.runRecordReference());
        assertEquals(Optional.empty(), pending.replacementAgentRunId());
        SchedulerQueueState queue = environment.queueState();
        assertEquals(Optional.of(environment.workItemId()),
                queue.activeWork().map(queued -> queued.workItem().workItemId()));
        assertTrue(queue.completedWorkItemIds().isEmpty());
        assertTrue(queue.failedWorkItemIds().isEmpty());
        assertTrue(environment.runRecordStore().references().isEmpty());
        assertEquals(0L, environment.evidenceFileCount());
        assertEquals(0L, environment.resultPointFileCount());
        ExternalEffectLedgerState effects = environment.effectStore()
                .resolve(pending.goalId());
        assertEquals(0L, effects.revision());
        assertTrue(effects.records().isEmpty());
    }

    private void publishAndReceive(SubmittedModelWorkerEnvironment environment) {
        SubmittedModelWorkerEnvironment.CliExecution publication =
                environment.supportedSpool();
        assertEquals(0, publication.exitCode(), publication.stderr());
        assertTrue(publication.stdout().startsWith("status=ACCEPTED\n"));
        SubmittedModelWorkerEnvironment.CliExecution receive =
                environment.supportedReceive(
                        outputValue(publication.stdout(), "messageFile"));
        assertEquals(0, receive.exitCode(), receive.stderr());
        assertTrue(receive.stdout().startsWith("status=ADMITTED\n"));
        assertTrue(receive.stdout().contains("spoolStatus=ACKNOWLEDGED\n"));
    }

    private String expectedStatus(String command) {
        return command.equals("scheduler-cycle")
                ? "status=VERIFIED_COMPLETED"
                : command.equals("scheduler-drain")
                        ? "status=IDLE"
                        : "status=IDLE_LIMIT";
    }

    private String outputValue(String output, String key) {
        String prefix = key + "=";
        return output.lines()
                .filter(line -> line.startsWith(prefix))
                .map(line -> line.substring(prefix.length()))
                .findFirst()
                .orElseThrow();
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
