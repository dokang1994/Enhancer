package com.enhancer.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.enhancer.bus.MessageEnvelope;
import com.enhancer.bus.ModelWorkPayload;
import com.enhancer.context.RequiredProjectDocument;
import com.enhancer.kernel.VerificationStatus;
import com.enhancer.model.DeterministicFakeModelGateway;
import com.enhancer.run.FileSystemRunRecordStore;
import com.enhancer.run.ResolvedModelRunRecord;
import com.enhancer.tool.EvidenceStoragePolicy;
import com.enhancer.tool.FileSystemEvidenceStore;
import com.enhancer.workspace.ApprovedTaskRevision;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class TypedModelProcessExecutionIntegrationTest {
    private static final String QUEUE_ID =
            "00000000-0000-0000-0000-00000000e001";
    private static final String OWNER_ID =
            "00000000-0000-0000-0000-00000000e002";
    private static final Duration LEASE = Duration.ofMinutes(5);
    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-09-03T14:00:00Z"), ZoneOffset.UTC);

    @TempDir
    Path temporaryRoot;

    @Test
    void interruptedPreReferenceAttemptReexecutesInAChildAndCompletes()
            throws Exception {
        Environment environment = environment("pre-reference", true);
        String goalId = ModelAttemptTestFixture.GOAL_ID;
        String agentRunId = ModelAttemptTestFixture.AGENT_RUN_ID;
        environment.checkpointStore().record(new PendingFinalization(
                goalId, agentRunId, Optional.empty()));
        Clock expiredClock = Clock.fixed(
                CLOCK.instant().minus(Duration.ofMinutes(2)), ZoneOffset.UTC);
        new DurableAgentRunDispatcher(
                environment.queue(), environment.runtimeStore(), expiredClock)
                .claimAndLease(
                        goalId,
                        agentRunId,
                        OWNER_ID,
                        Duration.ofMinutes(1))
                .orElseThrow();

        Optional<WorkItemDisposition> disposition =
                worker(environment).runOneCycle(LEASE);

        assertEquals(
                Optional.of(WorkItemDisposition.VERIFIED_COMPLETED),
                disposition);
        String reference = AgentRunRecordIdentity.reference(goalId, agentRunId);
        assertEquals(
                VerificationStatus.VERIFIED,
                environment.runRecordStore().resolveModel(reference)
                        .record().lifecycleRecord().verification().status());
        assertEquals(
                RuntimeAgentRunStatus.COMPLETED,
                DurableAgentRuntime.recover(
                                goalId, environment.runtimeStore(), CLOCK)
                        .agentRun().orElseThrow().status());
        assertTrue(environment.checkpointStore().findPending().isEmpty());
        assertEquals(Set.of(environment.work().workItemId()),
                environment.queue().completedWorkItemIds());
    }

    @Test
    void rejectedResultRetriesWithAFreshAgentRunAndFailsAtTheBound()
            throws Exception {
        Environment environment = environment("retry", false);

        Optional<WorkItemDisposition> disposition =
                worker(environment).runOneCycle(LEASE);

        assertEquals(Optional.of(WorkItemDisposition.FAILED), disposition);
        String goalId = soleGoalId(environment.runtimeRoot());
        AgentRuntimeState runtime = environment.runtimeStore().resolve(goalId);
        assertEquals(2, runtime.agentRuns().size());
        String firstAgentRunId = runtime.agentRuns().get(0).agentRunId();
        String secondAgentRunId = runtime.agentRuns().get(1).agentRunId();
        assertNotEquals(firstAgentRunId, secondAgentRunId);
        for (String agentRunId : List.of(firstAgentRunId, secondAgentRunId)) {
            ResolvedModelRunRecord resolved = environment.runRecordStore().resolveModel(
                    AgentRunRecordIdentity.reference(goalId, agentRunId));
            assertEquals(
                    VerificationStatus.REJECTED,
                    resolved.record().lifecycleRecord().verification().status());
            assertEquals(
                    AgentRunEvidenceIdentity.runId(goalId, agentRunId),
                    resolved.record().modelRequest().correlationId());
        }
        assertEquals(2, environment.runRecordStore().references().size());
        assertEquals(Set.of(environment.work().workItemId()),
                environment.queue().failedWorkItemIds());
        assertFalse(environment.queue().completedWorkItemIds()
                .contains(environment.work().workItemId()));
        assertTrue(environment.checkpointStore().findPending().isEmpty());
    }

    private DurableAgentRunWorker worker(Environment environment) {
        return DurableAgentRunWorker.processIsolated(
                environment.queue(),
                environment.runtimeStore(),
                environment.effectStore(),
                environment.checkpointStore(),
                environment.projectRoot(),
                environment.evidenceRoot(),
                environment.recordRoot(),
                environment.invocationRoot(),
                environment.runRecordStore(),
                environment.runRecordStore(),
                environment.evidenceStore(),
                environment.configuration(),
                OWNER_ID,
                CLOCK,
                Duration.ofSeconds(30),
                AgentRunRetryPolicy.of(2));
    }

    private Environment environment(String name, boolean matchingDigest)
            throws Exception {
        Path root = temporaryRoot.resolve(name);
        Path projectRoot = root.resolve("project");
        String currentTask = "# Current Task\n\n"
                + "## Status\n\nIn Progress\n\n"
                + "## Task\n\nExecute one typed deterministic model attempt.\n\n"
                + "## Task ID\n\n" + ModelAttemptTestFixture.TASK_ID + "\n\n"
                + "## Approval\n\nApproved by the integration fixture.\n\n"
                + "## Allowed Tools\n\n- model-invoke\n";
        for (RequiredProjectDocument document : RequiredProjectDocument.values()) {
            Path path = projectRoot.resolve(document.path());
            if (path.getParent() != null) {
                Files.createDirectories(path.getParent());
            }
            Files.writeString(
                    path,
                    document == RequiredProjectDocument.CURRENT_TASK
                            ? currentTask
                            : "content for " + document.path(),
                    StandardCharsets.UTF_8);
        }
        String prompt = "typed process integration prompt";
        Path promptPath = projectRoot.resolve(ModelAttemptTestFixture.TARGET_PATH);
        Files.createDirectories(promptPath.getParent());
        Files.writeString(promptPath, prompt, StandardCharsets.UTF_8);
        String expectedDigest = matchingDigest
                ? ModelAttemptTestFixture.sha256(
                        ModelAttemptTestFixture.deterministicResponse(prompt))
                : "f".repeat(64);
        WorkItem template = ModelAttemptTestFixture.admitted(
                projectRoot,
                prompt,
                expectedDigest,
                new DeterministicFakeModelGateway()).workItem();
        ModelWorkPayload templatePayload =
                (ModelWorkPayload) template.workMessage().payload();
        ModelWorkPayload payload = new ModelWorkPayload(
                new ApprovedTaskRevision(
                        ModelAttemptTestFixture.TASK_ID,
                        "CURRENT_TASK.md",
                        sha256(currentTask)),
                templatePayload.snapshotId(),
                templatePayload.allowedTools(),
                templatePayload.executionInput());
        MessageEnvelope envelope = template.workMessage();
        WorkItem work = new WorkItem(
                template.workItemId(),
                template.requiredCapability(),
                new MessageEnvelope(
                        envelope.messageId(),
                        envelope.correlationId(),
                        envelope.causationId(),
                        envelope.logicalRunId(),
                        envelope.producer(),
                        envelope.occurredAt(),
                        payload));
        FileSystemSchedulerQueueStore queueStore =
                new FileSystemSchedulerQueueStore(root.resolve("queue"));
        DurableSingleWorkerSchedulerQueue queue =
                DurableSingleWorkerSchedulerQueue.create(QUEUE_ID, 8, queueStore);
        queue.enqueue(new QueuedWork(work, List.of()));
        Path recordRoot = root.resolve("records");
        Path evidenceRoot = root.resolve("evidence");
        return new Environment(
                projectRoot,
                evidenceRoot,
                recordRoot,
                root.resolve("invocations"),
                root.resolve("runtime"),
                work,
                queue,
                new FileSystemAgentRuntimeStateStore(root.resolve("runtime")),
                new FileSystemPendingFinalizationStore(root.resolve("checkpoint")),
                new FileSystemExternalEffectLedgerStore(root.resolve("effects")),
                new FileSystemRunRecordStore(recordRoot),
                new FileSystemEvidenceStore(
                        evidenceRoot,
                        new EvidenceStoragePolicy(
                                EvidenceStoragePolicy.MAX_SUPPORTED_CONTENT_BYTES)),
                new ModelProcessExecutionConfiguration(
                        ModelProcessValidationTestFixture.LIMITS,
                        Set.of(),
                        ModelProcessValidationTestFixture.MAXIMUM_READ_BYTES,
                        ModelProcessValidationTestFixture.TOOL_TIMEOUT));
    }

    private static String soleGoalId(Path runtimeRoot) throws Exception {
        try (var files = Files.list(runtimeRoot)) {
            String name = files.map(path -> path.getFileName().toString())
                    .filter(value -> value.endsWith(".agent-runtime"))
                    .findFirst()
                    .orElseThrow();
            return name.substring(0, name.length() - ".agent-runtime".length());
        }
    }

    private static String sha256(String content) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(content.getBytes(StandardCharsets.UTF_8)));
    }

    private record Environment(
            Path projectRoot,
            Path evidenceRoot,
            Path recordRoot,
            Path invocationRoot,
            Path runtimeRoot,
            WorkItem work,
            DurableSingleWorkerSchedulerQueue queue,
            FileSystemAgentRuntimeStateStore runtimeStore,
            FileSystemPendingFinalizationStore checkpointStore,
            FileSystemExternalEffectLedgerStore effectStore,
            FileSystemRunRecordStore runRecordStore,
            FileSystemEvidenceStore evidenceStore,
            ModelProcessExecutionConfiguration configuration) {}
}
