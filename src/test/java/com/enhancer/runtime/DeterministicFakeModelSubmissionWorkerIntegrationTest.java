package com.enhancer.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.enhancer.bus.ModelWorkPayload;
import com.enhancer.kernel.VerificationStatus;
import com.enhancer.run.ResolvedModelRunRecord;
import java.io.IOException;
import java.time.Duration;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DeterministicFakeModelSubmissionWorkerIntegrationTest {
    private static final Duration LEASE = Duration.ofMinutes(5);

    @TempDir
    java.nio.file.Path temporaryRoot;

    @Test
    void submittedTypedIntentRunsInTheExistingInternalWorkerToVerifiedCompletion()
            throws Exception {
        SubmittedModelWorkerEnvironment environment =
                SubmittedModelWorkerEnvironment.verified(
                        temporaryRoot.resolve("verified"));

        DurableSubmissionResult submitted = environment.submit();

        assertTrue(submitted.manifestCreated());
        assertTrue(submitted.queueCreated());
        assertTrue(submitted.workAdmitted());
        assertEquals(1L, submitted.queueRevision());
        DurableSubmissionManifest manifest = environment.manifest();
        assertEquals("deterministic-echo", manifest.requiredCapability());
        ModelWorkPayload payload = (ModelWorkPayload) manifest.workMessage().payload();
        assertEquals(
                "deterministic-echo",
                payload.executionInput().executionProfile().requiredCapability());

        Optional<WorkItemDisposition> disposition =
                environment.worker().runOneCycle(LEASE);

        assertEquals(Optional.of(WorkItemDisposition.VERIFIED_COMPLETED), disposition);
        String goalId = environment.soleGoalId();
        AgentRuntimeState runtime = environment.runtimeStore().resolve(goalId);
        assertEquals(RuntimeGoalStatus.COMPLETED, runtime.goal().status());
        assertEquals(1, runtime.agentRuns().size());
        String agentRunId = runtime.agentRuns().get(0).agentRunId();
        ResolvedModelRunRecord record = environment.runRecordStore().resolveModel(
                AgentRunRecordIdentity.reference(goalId, agentRunId));
        assertEquals(
                VerificationStatus.VERIFIED,
                record.record().lifecycleRecord().verification().status());
        assertEquals(
                RuntimeAgentRunStatus.COMPLETED,
                runtime.agentRuns().get(0).status());
        assertEquals(
                Set.of(environment.workItemId()),
                environment.queue().completedWorkItemIds());
        assertTrue(environment.queue().failedWorkItemIds().isEmpty());
        assertTrue(environment.checkpointStore().findPending().isEmpty());
    }

    @Test
    void submittedCapabilityMismatchRemainsAnActivePreCallRefusalWithoutEffects()
            throws Exception {
        SubmittedModelWorkerEnvironment environment =
                SubmittedModelWorkerEnvironment.capabilityMismatch(
                        temporaryRoot.resolve("capability-mismatch"));

        DurableSubmissionResult submitted = environment.submit();
        assertTrue(submitted.workAdmitted());
        DurableSubmissionManifest manifest = environment.manifest();
        assertEquals("deterministic-echo", manifest.requiredCapability());
        ModelWorkPayload payload = (ModelWorkPayload) manifest.workMessage().payload();
        assertEquals(
                "profile-only-capability",
                payload.executionInput().executionProfile().requiredCapability());

        IOException refusal = assertThrows(
                IOException.class,
                () -> environment.worker().runOneCycle(LEASE));

        assertTrue(refusal.getMessage().contains("without publishing a result"));
        PendingFinalization pending =
                environment.checkpointStore().findPending().orElseThrow();
        assertEquals(Optional.empty(), pending.runRecordReference());
        assertEquals(Optional.empty(), pending.replacementAgentRunId());
        AgentRuntimeState runtime = environment.runtimeStore().resolve(pending.goalId());
        assertEquals(RuntimeGoalStatus.ACTIVE, runtime.goal().status());
        assertEquals(1, runtime.agentRuns().size());
        assertEquals(RuntimeAgentRunStatus.EXECUTING,
                runtime.agentRuns().get(0).status());
        assertTrue(runtime.agentRuns().get(0).lease().isPresent());
        assertTrue(runtime.agentRuns().get(0).resultMessage().isEmpty());
        assertEquals(0, runtime.completedAttempts());
        assertTrue(runtime.retryDecisions().isEmpty());
        SchedulerQueueState queue = environment.queueState();
        assertEquals(Optional.of(environment.workItemId()),
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
}
