package com.enhancer.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class SupportedSchedulerModelCompletionIntegrationTest {
    @TempDir
    Path temporaryRoot;

    @ParameterizedTest
    @ValueSource(strings = {
            "scheduler-cycle", "scheduler-drain", "scheduler-service"
    })
    void supportedCommandCompletesTestSeededTypedIntentWithoutOutputDisclosure(
            String command) throws Exception {
        SubmittedModelWorkerEnvironment environment =
                SubmittedModelWorkerEnvironment.verified(
                        temporaryRoot.resolve(command));
        SubmittedModelWorkerEnvironment.CliExecution submission =
                environment.supportedSubmit();
        assertEquals(0, submission.exitCode(), submission.stderr());
        assertTrue(submission.stdout().startsWith("status=ADMITTED\n"));

        Execution execution = execute(environment.schedulerArguments(command));

        assertEquals(0, execution.exitCode(), execution.stderr());
        assertTrue(execution.stdout().contains(expectedStatus(command)));
        assertTrue(execution.stderr().isEmpty());
        assertFalse(execution.stdout().contains("deterministic-fake-v2"));
        assertFalse(execution.stdout().contains("model-invoke"));
        assertFalse(execution.stdout().contains(
                "submitted typed process integration prompt"));
        assertFalse(execution.stdout().contains("profile"));
        assertEquals(
                Set.of(environment.workItemId()),
                environment.queue().completedWorkItemIds());
        assertTrue(environment.queue().failedWorkItemIds().isEmpty());
        assertTrue(environment.checkpointStore().findPending().isEmpty());
        assertEquals(1, environment.runRecordStore().references().size());
        String goalId = environment.soleGoalId();
        AgentRuntimeState runtime = environment.runtimeStore().resolve(goalId);
        assertEquals(1, runtime.agentRuns().size());
        RuntimeAgentRun run = runtime.agentRuns().get(0);
        ResolvedModelRunRecord record = environment.runRecordStore().resolveModel(
                AgentRunRecordIdentity.reference(goalId, run.agentRunId()));
        assertEquals(
                VerificationStatus.VERIFIED,
                record.record().lifecycleRecord().verification().status());
        assertEquals(RuntimeAgentRunStatus.COMPLETED, run.status());
        assertEquals(Optional.of(environment.workItemId()),
                environment.queueState().completedWorkItemIds().stream().findFirst());
    }

    private String expectedStatus(String command) {
        return command.equals("scheduler-cycle")
                ? "status=VERIFIED_COMPLETED"
                : command.equals("scheduler-drain")
                        ? "status=IDLE"
                        : "status=IDLE_LIMIT";
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
