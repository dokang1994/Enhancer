package com.enhancer.run;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.enhancer.bus.MessageEnvelope;
import com.enhancer.bus.ModelWorkPayload;
import com.enhancer.bus.WorkPayload;
import com.enhancer.kernel.VerificationDecision;
import com.enhancer.loop.AgentLoopStopReason;
import com.enhancer.loop.ApprovedTask;
import com.enhancer.model.ModelCostBudget;
import com.enhancer.model.ModelDataClassification;
import com.enhancer.model.ModelExecutionProfile;
import com.enhancer.model.ModelLocalityRequirement;
import com.enhancer.model.ModelReasoningRequirement;
import com.enhancer.model.ModelRequest;
import com.enhancer.model.ModelTokenBudget;
import com.enhancer.model.ModelInvokeTool;
import com.enhancer.tool.ToolFailureCode;
import com.enhancer.tool.ToolRequest;
import com.enhancer.tool.ToolResult;
import com.enhancer.tool.ToolResultStatus;
import com.enhancer.tool.VerificationEvidence;
import com.enhancer.workspace.ApprovedTaskRevision;
import java.lang.reflect.Modifier;
import java.lang.reflect.RecordComponent;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ModelRunRecordTest {
    private static final String WORK_ITEM_ID = "11111111-1111-1111-1111-111111111111";
    private static final String MESSAGE_ID = "22222222-2222-2222-2222-222222222222";
    private static final String TASK_DIGEST = "a".repeat(64);
    private static final String SNAPSHOT_ID = "b".repeat(64);
    private static final String EXPECTED_DIGEST = "c".repeat(64);
    private static final String LOGICAL_RUN_ID = "logical-run-1";
    private static final String MODEL_CLASS = "reasoning-standard";
    private static final String TARGET_PATH = "prompts/request.txt";

    @Test
    void retainsExactlyTheStandaloneModelProvenanceWithoutAnAuthorityPort() {
        MessageEnvelope message = modelMessage(payload(
                "profile-capability", MODEL_CLASS, Duration.ofSeconds(3)));
        ModelRequest request = request(MODEL_CLASS, Duration.ofSeconds(4), 4096);
        RunRecord lifecycle = successfulLifecycle(
                LOGICAL_RUN_ID,
                "task-1",
                "CURRENT_TASK.md",
                Set.of(ModelInvokeTool.NAME),
                request,
                TARGET_PATH,
                Optional.of(EXPECTED_DIGEST));

        ModelRunRecord record = new ModelRunRecord(
                WORK_ITEM_ID,
                "independent-capability",
                message,
                request,
                lifecycle);

        assertSame(message, record.workMessage());
        assertSame(request, record.modelRequest());
        assertSame(lifecycle, record.lifecycleRecord());
        assertEquals("independent-capability", record.requiredCapability());
        assertTrue(ModelRunRecord.class.isRecord());
        assertTrue(Modifier.isPublic(ModelRunRecord.class.getModifiers()));
        assertTrue(Modifier.isFinal(ModelRunRecord.class.getModifiers()));
        assertArrayEquals(
                new String[] {
                    "workItemId", "requiredCapability", "workMessage",
                    "modelRequest", "lifecycleRecord"
                },
                Arrays.stream(ModelRunRecord.class.getRecordComponents())
                        .map(RecordComponent::getName)
                        .toArray(String[]::new));
        assertArrayEquals(
                new Class<?>[] {
                    String.class, String.class, MessageEnvelope.class,
                    ModelRequest.class, RunRecord.class
                },
                Arrays.stream(ModelRunRecord.class.getRecordComponents())
                        .map(RecordComponent::getType)
                        .toArray(Class<?>[]::new));
        assertArrayEquals(new Class<?>[0], ModelRunRecord.class.getInterfaces());
    }

    @Test
    void rejectsMissingOrInvalidTopLevelIdentityValues() {
        Fixture fixture = fixture();
        assertThrows(NullPointerException.class, () -> new ModelRunRecord(
                null, fixture.capability(), fixture.message(), fixture.request(), fixture.lifecycle()));
        assertThrows(NullPointerException.class, () -> new ModelRunRecord(
                WORK_ITEM_ID, null, fixture.message(), fixture.request(), fixture.lifecycle()));
        assertThrows(NullPointerException.class, () -> new ModelRunRecord(
                WORK_ITEM_ID, fixture.capability(), null, fixture.request(), fixture.lifecycle()));
        assertThrows(NullPointerException.class, () -> new ModelRunRecord(
                WORK_ITEM_ID, fixture.capability(), fixture.message(), null, fixture.lifecycle()));
        assertThrows(NullPointerException.class, () -> new ModelRunRecord(
                WORK_ITEM_ID, fixture.capability(), fixture.message(), fixture.request(), null));
        assertThrows(IllegalArgumentException.class, () -> fixture.withWorkItemId("not-a-uuid"));
        assertThrows(IllegalArgumentException.class, () -> fixture.withWorkItemId(MESSAGE_ID));
        assertThrows(IllegalArgumentException.class, () -> fixture.withCapability(" "));
        assertThrows(IllegalArgumentException.class, () -> fixture.withCapability("x".repeat(257)));
    }

    @Test
    void rejectsLegacyPayloadAndTaskOrRunBindingDrift() {
        Fixture fixture = fixture();
        WorkPayload legacyPayload = new WorkPayload(
                taskRevision("task-1", "CURRENT_TASK.md"),
                SNAPSHOT_ID,
                Set.of(ModelInvokeTool.NAME));
        MessageEnvelope legacyMessage = new MessageEnvelope(
                MESSAGE_ID,
                "work-correlation",
                Optional.empty(),
                LOGICAL_RUN_ID,
                "scheduler",
                Instant.parse("2026-08-31T00:00:00Z"),
                legacyPayload);

        assertThrows(IllegalArgumentException.class, () -> fixture.withMessage(legacyMessage));
        assertThrows(IllegalArgumentException.class, () -> fixture.withMessage(modelMessage(
                payload("profile-capability", MODEL_CLASS, Duration.ofSeconds(3)),
                "different-logical-run")));
        assertThrows(IllegalArgumentException.class, () -> fixture.withLifecycle(successfulLifecycle(
                LOGICAL_RUN_ID, "different-task", "CURRENT_TASK.md",
                Set.of(ModelInvokeTool.NAME), fixture.request(), TARGET_PATH,
                Optional.of(EXPECTED_DIGEST))));
        assertThrows(IllegalArgumentException.class, () -> fixture.withLifecycle(successfulLifecycle(
                LOGICAL_RUN_ID, "task-1", "OTHER_TASK.md",
                Set.of(ModelInvokeTool.NAME), fixture.request(), TARGET_PATH,
                Optional.of(EXPECTED_DIGEST))));
        assertThrows(IllegalArgumentException.class, () -> fixture.withLifecycle(successfulLifecycle(
                LOGICAL_RUN_ID, "task-1", "CURRENT_TASK.md",
                Set.of(ModelInvokeTool.NAME, "read-file"), fixture.request(), TARGET_PATH,
                Optional.of(EXPECTED_DIGEST))));
    }

    @Test
    void rejectsToolRequestAndPreparedRequestBindingDrift() {
        Fixture fixture = fixture();
        assertThrows(IllegalArgumentException.class, () -> fixture.withLifecycle(lifecycle(
                fixture.request(), "read-file", "correlation-1", modelArguments(fixture.request(), TARGET_PATH),
                Optional.of(EXPECTED_DIGEST))));
        assertThrows(IllegalArgumentException.class, () -> fixture.withLifecycle(lifecycle(
                fixture.request(), ModelInvokeTool.NAME, "different-correlation",
                modelArguments(fixture.request(), TARGET_PATH), Optional.of(EXPECTED_DIGEST))));
        assertThrows(IllegalArgumentException.class, () -> fixture.withLifecycle(lifecycle(
                fixture.request(), ModelInvokeTool.NAME, "correlation-1",
                modelArguments(fixture.request(), "other.txt"), Optional.of(EXPECTED_DIGEST))));

        ModelRequest otherClass = request("reasoning-extended", Duration.ofSeconds(4), 4096);
        assertThrows(IllegalArgumentException.class, () -> fixture.withRequest(otherClass));

        ModelRequest otherTimeout = request(MODEL_CLASS, Duration.ofSeconds(5), 4096);
        assertThrows(IllegalArgumentException.class, () -> fixture.withRequest(otherTimeout));

        ModelRequest otherCeiling = request(MODEL_CLASS, Duration.ofSeconds(4), 8192);
        assertThrows(IllegalArgumentException.class, () -> fixture.withRequest(otherCeiling));
    }

    @Test
    void enforcesProfileAlignmentButKeepsCapabilityProjectionIndependent() {
        Fixture fixture = fixture();
        assertEquals("independent-capability", fixture.record().requiredCapability());
        assertEquals(
                "profile-capability",
                ((ModelWorkPayload) fixture.message().payload())
                        .executionInput().executionProfile().requiredCapability());

        assertThrows(IllegalArgumentException.class, () -> fixture.withMessage(modelMessage(
                payload("profile-capability", "reasoning-extended", Duration.ofSeconds(3)))));
        assertThrows(IllegalArgumentException.class, () -> fixture.withMessage(modelMessage(
                payload("profile-capability", MODEL_CLASS, Duration.ofSeconds(5)))));
    }

    @Test
    void bindsPresentDigestAndAllowsFailedLifecycleToRetainTypedExpectedDigest() {
        Fixture fixture = fixture();
        assertThrows(IllegalArgumentException.class, () -> fixture.withLifecycle(successfulLifecycle(
                LOGICAL_RUN_ID, "task-1", "CURRENT_TASK.md", Set.of(ModelInvokeTool.NAME),
                fixture.request(), TARGET_PATH, Optional.of("d".repeat(64)))));

        RunRecord failed = failedLifecycle(fixture.request());
        ModelRunRecord record = new ModelRunRecord(
                WORK_ITEM_ID, fixture.capability(), fixture.message(), fixture.request(), failed);
        assertTrue(record.lifecycleRecord().expectedContentSha256().isEmpty());
        assertEquals(
                EXPECTED_DIGEST,
                ((ModelWorkPayload) record.workMessage().payload())
                        .executionInput().expectedResponseSha256());
    }

    private static Fixture fixture() {
        MessageEnvelope message = modelMessage(payload(
                "profile-capability", MODEL_CLASS, Duration.ofSeconds(3)));
        ModelRequest request = request(MODEL_CLASS, Duration.ofSeconds(4), 4096);
        RunRecord lifecycle = successfulLifecycle(
                LOGICAL_RUN_ID, "task-1", "CURRENT_TASK.md", Set.of(ModelInvokeTool.NAME),
                request, TARGET_PATH, Optional.of(EXPECTED_DIGEST));
        return new Fixture("independent-capability", message, request, lifecycle);
    }

    private static ModelWorkPayload payload(
            String profileCapability, String modelClass, Duration maximumInvocationTime) {
        ModelExecutionProfile profile = new ModelExecutionProfile(
                ModelExecutionProfile.SCHEMA_VERSION,
                profileCapability,
                modelClass,
                ModelLocalityRequirement.LOCAL_ONLY,
                ModelReasoningRequirement.STANDARD,
                8192,
                new ModelTokenBudget(2048, 2048, 4096),
                new ModelCostBudget("USD", 0),
                maximumInvocationTime,
                ModelDataClassification.INTERNAL);
        return new ModelWorkPayload(
                taskRevision("task-1", "CURRENT_TASK.md"),
                SNAPSHOT_ID,
                Set.of(ModelInvokeTool.NAME),
                new ModelWorkPayload.ModelInvocationExecutionInput(
                        TARGET_PATH, EXPECTED_DIGEST, profile));
    }

    private static ApprovedTaskRevision taskRevision(String taskId, String sourceDocument) {
        return new ApprovedTaskRevision(taskId, sourceDocument, TASK_DIGEST);
    }

    private static MessageEnvelope modelMessage(ModelWorkPayload payload) {
        return modelMessage(payload, LOGICAL_RUN_ID);
    }

    private static MessageEnvelope modelMessage(ModelWorkPayload payload, String logicalRunId) {
        return new MessageEnvelope(
                MESSAGE_ID,
                "work-correlation",
                Optional.empty(),
                logicalRunId,
                "scheduler",
                Instant.parse("2026-08-31T00:00:00Z"),
                payload);
    }

    private static ModelRequest request(String modelClass, Duration timeout, int ceiling) {
        return new ModelRequest(
                "correlation-1", "Analyze the repository.", modelClass, timeout, ceiling);
    }

    private static RunRecord successfulLifecycle(
            String logicalRunId,
            String taskId,
            String sourceDocument,
            Set<String> allowedTools,
            ModelRequest request,
            String targetPath,
            Optional<String> expectedDigest) {
        return lifecycle(
                logicalRunId,
                taskId,
                sourceDocument,
                allowedTools,
                request,
                ModelInvokeTool.NAME,
                request.correlationId(),
                modelArguments(request, targetPath),
                expectedDigest,
                successfulResult(),
                VerificationDecision.verified("response matched"),
                AgentLoopStopReason.AWAITING_VERIFICATION,
                AgentLoopStopReason.COMPLETED);
    }

    private static RunRecord lifecycle(
            ModelRequest request,
            String toolName,
            String correlationId,
            Map<String, String> arguments,
            Optional<String> expectedDigest) {
        return lifecycle(
                LOGICAL_RUN_ID,
                "task-1",
                "CURRENT_TASK.md",
                Set.of(ModelInvokeTool.NAME, toolName),
                request,
                toolName,
                correlationId,
                arguments,
                expectedDigest,
                toolName.equals(ModelInvokeTool.NAME) ? successfulResult() : successfulResult(toolName),
                VerificationDecision.verified("response matched"),
                AgentLoopStopReason.AWAITING_VERIFICATION,
                AgentLoopStopReason.COMPLETED);
    }

    private static RunRecord failedLifecycle(ModelRequest request) {
        ToolResult result = new ToolResult(
                ModelInvokeTool.NAME,
                ToolResultStatus.FAILURE,
                OptionalInt.empty(),
                Optional.of(ToolFailureCode.TEMPORARY_FAILURE),
                VerificationEvidence.capture("model failed", "temporary failure", Optional.empty()));
        return lifecycle(
                LOGICAL_RUN_ID,
                "task-1",
                "CURRENT_TASK.md",
                Set.of(ModelInvokeTool.NAME),
                request,
                ModelInvokeTool.NAME,
                request.correlationId(),
                modelArguments(request, TARGET_PATH),
                Optional.empty(),
                result,
                VerificationDecision.notPerformed("worker failed"),
                AgentLoopStopReason.FAILED,
                AgentLoopStopReason.FAILED);
    }

    private static RunRecord lifecycle(
            String logicalRunId,
            String taskId,
            String sourceDocument,
            Set<String> allowedTools,
            ModelRequest request,
            String toolName,
            String correlationId,
            Map<String, String> arguments,
            Optional<String> expectedDigest,
            ToolResult result,
            VerificationDecision verification,
            AgentLoopStopReason workerStop,
            AgentLoopStopReason finalStop) {
        return new RunRecord(
                logicalRunId,
                Instant.parse("2026-08-31T00:00:01Z"),
                new ApprovedTask(
                        taskId,
                        "Invoke a model",
                        "Approved by test",
                        allowedTools,
                        sourceDocument),
                new ToolRequest(toolName, correlationId, arguments),
                new PolicyDecision(
                        PolicyDecisionStatus.ALLOWED,
                        "C:/project",
                        allowedTools,
                        Set.of(),
                        262_144,
                        5_000),
                result,
                expectedDigest,
                verification,
                1,
                workerStop,
                finalStop);
    }

    private static Map<String, String> modelArguments(ModelRequest request, String targetPath) {
        return Map.of(
                ModelInvokeTool.PROMPT_PATH_ARGUMENT, targetPath,
                ModelInvokeTool.MODEL_CLASS_ARGUMENT, request.modelClass(),
                ModelInvokeTool.TIMEOUT_MILLIS_ARGUMENT, Long.toString(request.timeout().toMillis()),
                ModelInvokeTool.MAX_RESPONSE_LENGTH_ARGUMENT,
                        Integer.toString(request.maxResponseLength()));
    }

    private static ToolResult successfulResult() {
        return successfulResult(ModelInvokeTool.NAME);
    }

    private static ToolResult successfulResult(String toolName) {
        return new ToolResult(
                toolName,
                ToolResultStatus.SUCCESS,
                OptionalInt.empty(),
                VerificationEvidence.capture("model succeeded", "response", Optional.empty()));
    }

    private record Fixture(
            String capability,
            MessageEnvelope message,
            ModelRequest request,
            RunRecord lifecycle) {

        private ModelRunRecord record() {
            return new ModelRunRecord(WORK_ITEM_ID, capability, message, request, lifecycle);
        }

        private ModelRunRecord withWorkItemId(String workItemId) {
            return new ModelRunRecord(workItemId, capability, message, request, lifecycle);
        }

        private ModelRunRecord withCapability(String replacement) {
            return new ModelRunRecord(WORK_ITEM_ID, replacement, message, request, lifecycle);
        }

        private ModelRunRecord withMessage(MessageEnvelope replacement) {
            return new ModelRunRecord(WORK_ITEM_ID, capability, replacement, request, lifecycle);
        }

        private ModelRunRecord withRequest(ModelRequest replacement) {
            return new ModelRunRecord(WORK_ITEM_ID, capability, message, replacement, lifecycle);
        }

        private ModelRunRecord withLifecycle(RunRecord replacement) {
            return new ModelRunRecord(WORK_ITEM_ID, capability, message, request, replacement);
        }
    }
}
