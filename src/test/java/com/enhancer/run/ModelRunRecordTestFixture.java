package com.enhancer.run;

import com.enhancer.bus.MessageEnvelope;
import com.enhancer.bus.ModelWorkPayload;
import com.enhancer.kernel.VerificationDecision;
import com.enhancer.loop.AgentLoopStopReason;
import com.enhancer.loop.ApprovedTask;
import com.enhancer.model.ModelCostBudget;
import com.enhancer.model.ModelDataClassification;
import com.enhancer.model.ModelExecutionProfile;
import com.enhancer.model.ModelInvokeTool;
import com.enhancer.model.ModelLocalityRequirement;
import com.enhancer.model.ModelReasoningRequirement;
import com.enhancer.model.ModelRequest;
import com.enhancer.model.ModelTokenBudget;
import com.enhancer.tool.ToolRequest;
import com.enhancer.tool.ToolResult;
import com.enhancer.tool.ToolResultStatus;
import com.enhancer.tool.VerificationEvidence;
import com.enhancer.workspace.ApprovedTaskRevision;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;

final class ModelRunRecordTestFixture {
    private ModelRunRecordTestFixture() {}

    static ModelRunRecord record() {
        ModelExecutionProfile profile = new ModelExecutionProfile(
                ModelExecutionProfile.SCHEMA_VERSION,
                "profile-capability",
                "reasoning-standard",
                ModelLocalityRequirement.LOCAL_ONLY,
                ModelReasoningRequirement.STANDARD,
                8192,
                new ModelTokenBudget(2048, 2048, 4096),
                new ModelCostBudget("USD", 0),
                Duration.ofSeconds(3),
                ModelDataClassification.INTERNAL);
        ModelWorkPayload payload = new ModelWorkPayload(
                new ApprovedTaskRevision("task-1", "CURRENT_TASK.md", "a".repeat(64)),
                "b".repeat(64),
                Set.of(ModelInvokeTool.NAME),
                new ModelWorkPayload.ModelInvocationExecutionInput(
                        "prompts/request.txt", "c".repeat(64), profile));
        MessageEnvelope message = new MessageEnvelope(
                "22222222-2222-2222-2222-222222222222",
                "work-correlation",
                Optional.empty(),
                "logical-run-1",
                "scheduler",
                Instant.parse("2026-08-31T00:00:00Z"),
                payload);
        ModelRequest request = new ModelRequest(
                "correlation-1",
                "Analyze the repository.",
                "reasoning-standard",
                Duration.ofSeconds(4),
                4096);
        RunRecord lifecycle = new RunRecord(
                "logical-run-1",
                Instant.parse("2026-08-31T00:00:01Z"),
                new ApprovedTask(
                        "task-1",
                        "Invoke a model",
                        "Approved by test",
                        Set.of(ModelInvokeTool.NAME),
                        "CURRENT_TASK.md"),
                new ToolRequest(
                        ModelInvokeTool.NAME,
                        request.correlationId(),
                        Map.of(
                                ModelInvokeTool.PROMPT_PATH_ARGUMENT, "prompts/request.txt",
                                ModelInvokeTool.MODEL_CLASS_ARGUMENT, request.modelClass(),
                                ModelInvokeTool.TIMEOUT_MILLIS_ARGUMENT, "4000",
                                ModelInvokeTool.MAX_RESPONSE_LENGTH_ARGUMENT, "4096")),
                new PolicyDecision(
                        PolicyDecisionStatus.ALLOWED,
                        "C:/project",
                        Set.of(ModelInvokeTool.NAME),
                        Set.of(),
                        262_144,
                        5_000),
                new ToolResult(
                        ModelInvokeTool.NAME,
                        ToolResultStatus.SUCCESS,
                        OptionalInt.empty(),
                        VerificationEvidence.capture(
                                "model succeeded", "response", Optional.empty())),
                Optional.of("c".repeat(64)),
                VerificationDecision.verified("response matched"),
                1,
                AgentLoopStopReason.AWAITING_VERIFICATION,
                AgentLoopStopReason.COMPLETED);
        return new ModelRunRecord(
                "11111111-1111-1111-1111-111111111111",
                "independent-capability",
                message,
                request,
                lifecycle);
    }
}
