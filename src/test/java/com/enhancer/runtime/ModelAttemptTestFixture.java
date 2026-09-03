package com.enhancer.runtime;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import com.enhancer.bus.MessageEnvelope;
import com.enhancer.bus.ModelWorkPayload;
import com.enhancer.loop.ApprovedTask;
import com.enhancer.model.DeterministicFakeExactRequestDecision;
import com.enhancer.model.DeterministicFakeExactRequestPreparation;
import com.enhancer.model.DeterministicFakeModelCandidate;
import com.enhancer.model.DeterministicFakeModelGateway;
import com.enhancer.model.ModelCandidateSuitability;
import com.enhancer.model.ModelCandidateSuitabilityDecision;
import com.enhancer.model.ModelCostBudget;
import com.enhancer.model.ModelDataClassification;
import com.enhancer.model.ModelExecutionProfile;
import com.enhancer.model.ModelInvocationAdmissionDecision;
import com.enhancer.model.ModelLocalityRequirement;
import com.enhancer.model.ModelReasoningRequirement;
import com.enhancer.model.ModelRequest;
import com.enhancer.model.ModelTokenBudget;
import com.enhancer.model.ProfiledModelRequest;
import com.enhancer.tool.CancellationToken;
import com.enhancer.tool.ExecutionPolicy;
import com.enhancer.workspace.ApprovedTaskRevision;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;
import java.util.Set;

final class ModelAttemptTestFixture {
    static final String GOAL_ID = "00000000-0000-0000-0000-00000000a001";
    static final String AGENT_RUN_ID = "00000000-0000-0000-0000-00000000a002";
    static final String WORK_ITEM_ID = "00000000-0000-0000-0000-00000000a003";
    static final String TARGET_PATH = "prompts/request.txt";
    static final String TASK_ID = "model-attempt-test";
    static final String LOGICAL_RUN_ID = "typed-model-logical-run";

    private ModelAttemptTestFixture() {}

    static Fixture admitted(
            Path projectRoot,
            String prompt,
            String expectedDigest,
            DeterministicFakeModelGateway gateway) {
        return admitted(
                projectRoot,
                prompt,
                expectedDigest,
                gateway,
                new ModelTokenBudget(20_000, 20_000, 40_000),
                "deterministic-fake",
                CancellationToken.none());
    }

    static Fixture admitted(
            Path projectRoot,
            String prompt,
            String expectedDigest,
            DeterministicFakeModelGateway gateway,
            ModelTokenBudget budget,
            String modelClass,
            CancellationToken cancellationToken) {
        String evidenceRunId = AgentRunEvidenceIdentity.runId(GOAL_ID, AGENT_RUN_ID);
        ModelExecutionProfile profile = new ModelExecutionProfile(
                ModelExecutionProfile.SCHEMA_VERSION,
                "deterministic-echo",
                modelClass,
                ModelLocalityRequirement.LOCAL_ONLY,
                ModelReasoningRequirement.MINIMAL,
                budget.maxTotalTokens(),
                budget,
                new ModelCostBudget("USD", 0),
                Duration.ofSeconds(1),
                ModelDataClassification.PUBLIC);
        ModelWorkPayload payload = new ModelWorkPayload(
                new ApprovedTaskRevision(TASK_ID, "CURRENT_TASK.md", "a".repeat(64)),
                "b".repeat(64),
                Set.of("model-invoke"),
                new ModelWorkPayload.ModelInvocationExecutionInput(
                        TARGET_PATH, expectedDigest, profile));
        MessageEnvelope envelope = new MessageEnvelope(
                "00000000-0000-0000-0000-00000000a004",
                "work-correlation",
                Optional.empty(),
                LOGICAL_RUN_ID,
                "scheduler",
                Instant.parse("2026-09-03T01:02:03.004005006Z"),
                payload);
        WorkItem workItem = new WorkItem(WORK_ITEM_ID, "deterministic-echo", envelope);
        ApprovedTask task = new ApprovedTask(
                TASK_ID,
                "Execute one typed deterministic model attempt.",
                "Approved by the test task.",
                Set.of("model-invoke"),
                "CURRENT_TASK.md");
        ExecutionPolicy policy = new ExecutionPolicy(
                projectRoot,
                Set.of("model-invoke"),
                Set.of(),
                64 * 1024,
                Duration.ofSeconds(2),
                cancellationToken);
        ModelRequest request = new ModelRequest(
                evidenceRunId,
                prompt,
                modelClass,
                Duration.ofSeconds(1),
                20_000);
        ProfiledModelRequest profiled = new ProfiledModelRequest(request, profile);
        ModelInvocationAdmissionDecision.Admitted admitted =
                new ModelInvocationAdmissionDecision.Admitted(profiled);
        SchedulerModelInvocationPreparation preparation =
                new SchedulerModelInvocationPreparation(task, policy, profiled, admitted);
        ModelCandidateSuitabilityDecision suitability = new ModelCandidateSuitability()
                .evaluate(admitted, DeterministicFakeModelCandidate.bind(gateway));
        DeterministicFakeExactRequestDecision.Ready ready = null;
        if (suitability instanceof ModelCandidateSuitabilityDecision.Suitable suitable) {
            DeterministicFakeExactRequestDecision decision =
                    new DeterministicFakeExactRequestPreparation().evaluate(suitable, policy);
            if (decision instanceof DeterministicFakeExactRequestDecision.Ready value) {
                ready = value;
            }
        }
        return new Fixture(workItem, preparation, ready);
    }

    static DeterministicFakeExactRequestDecision.Ready requireReady(Fixture fixture) {
        return assertInstanceOf(
                DeterministicFakeExactRequestDecision.Ready.class, fixture.ready());
    }

    static String deterministicResponse(String prompt) {
        return "deterministic-fake-v1\n"
                + "model-class=deterministic-fake\n"
                + "prompt-sha256=" + sha256(prompt) + "\n"
                + "prompt-length=" + prompt.length() + "\n"
                + "echo=" + prompt;
    }

    static String sha256(String content) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(content.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new AssertionError(exception);
        }
    }

    record Fixture(
            WorkItem workItem,
            SchedulerModelInvocationPreparation preparation,
            DeterministicFakeExactRequestDecision.Ready ready) {}
}
