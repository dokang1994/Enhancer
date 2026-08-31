package com.enhancer.run;

import com.enhancer.bus.MessageEnvelope;
import com.enhancer.bus.ModelWorkPayload;
import com.enhancer.model.ModelInvokeTool;
import com.enhancer.model.ModelRequest;
import com.enhancer.model.ProfiledModelRequest;
import com.enhancer.tool.ToolRequest;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Standalone immutable provenance for one typed model-work lifecycle.
 *
 * <p>This value binds retained work, request, and lifecycle facts. It stores no
 * admission decision and grants no Tool, provider, route, network, credential, or
 * spend authority.
 */
public record ModelRunRecord(
        String workItemId,
        String requiredCapability,
        MessageEnvelope workMessage,
        ModelRequest modelRequest,
        RunRecord lifecycleRecord) {

    public static final int MAX_REQUIRED_CAPABILITY_CHARACTERS = 256;
    private static final Set<String> MODEL_ARGUMENTS = Set.of(
            ModelInvokeTool.PROMPT_PATH_ARGUMENT,
            ModelInvokeTool.MODEL_CLASS_ARGUMENT,
            ModelInvokeTool.TIMEOUT_MILLIS_ARGUMENT,
            ModelInvokeTool.MAX_RESPONSE_LENGTH_ARGUMENT);

    public ModelRunRecord {
        workItemId = canonicalWorkItemId(workItemId);
        requiredCapability = boundedCapability(requiredCapability);
        Objects.requireNonNull(workMessage, "workMessage must not be null");
        Objects.requireNonNull(modelRequest, "modelRequest must not be null");
        Objects.requireNonNull(lifecycleRecord, "lifecycleRecord must not be null");

        if (workItemId.equals(workMessage.messageId())) {
            throw new IllegalArgumentException(
                    "workItemId must not equal the work message identity");
        }
        if (!(workMessage.payload() instanceof ModelWorkPayload payload)) {
            throw new IllegalArgumentException(
                    "workMessage must carry a ModelWorkPayload");
        }
        if (!workMessage.logicalRunId().equals(lifecycleRecord.logicalRunId())) {
            throw new IllegalArgumentException(
                    "workMessage and lifecycleRecord logicalRunId values must match");
        }

        requireTaskBinding(payload, lifecycleRecord);
        requireRequestBinding(payload, modelRequest, lifecycleRecord.toolRequest());
        new ProfiledModelRequest(modelRequest, payload.executionInput().executionProfile());

        lifecycleRecord.expectedContentSha256().ifPresent(expectedDigest -> {
            if (!expectedDigest.equals(payload.executionInput().expectedResponseSha256())) {
                throw new IllegalArgumentException(
                        "lifecycle expected digest must match the typed model input");
            }
        });
    }

    private static void requireTaskBinding(
            ModelWorkPayload payload,
            RunRecord lifecycleRecord) {
        if (!lifecycleRecord.approvedTask().taskId()
                .equals(payload.taskRevision().taskId())) {
            throw new IllegalArgumentException(
                    "lifecycle task identity must match the retained task revision");
        }
        if (!lifecycleRecord.approvedTask().sourceDocument()
                .equals(payload.taskRevision().sourceDocument())) {
            throw new IllegalArgumentException(
                    "lifecycle task source must match the retained task revision");
        }
        if (!lifecycleRecord.approvedTask().allowedTools().equals(payload.allowedTools())) {
            throw new IllegalArgumentException(
                    "lifecycle Tool scope must match the typed model payload");
        }
    }

    private static void requireRequestBinding(
            ModelWorkPayload payload,
            ModelRequest modelRequest,
            ToolRequest toolRequest) {
        if (!ModelInvokeTool.NAME.equals(toolRequest.toolName())) {
            throw new IllegalArgumentException(
                    "model lifecycle must contain a model-invoke Tool request");
        }
        if (!toolRequest.correlationId().equals(modelRequest.correlationId())) {
            throw new IllegalArgumentException(
                    "Tool request and model request correlationId values must match");
        }
        Map<String, String> arguments = toolRequest.arguments();
        if (!arguments.keySet().equals(MODEL_ARGUMENTS)) {
            throw new IllegalArgumentException(
                    "model Tool request must contain exactly the prepared request arguments");
        }
        requireArgument(
                arguments,
                ModelInvokeTool.PROMPT_PATH_ARGUMENT,
                payload.executionInput().targetPath());
        requireArgument(
                arguments,
                ModelInvokeTool.MODEL_CLASS_ARGUMENT,
                modelRequest.modelClass());

        Duration timeout = modelRequest.timeout();
        long timeoutMillis = timeout.toMillis();
        if (!Duration.ofMillis(timeoutMillis).equals(timeout)) {
            throw new IllegalArgumentException(
                    "model request timeout must use millisecond precision");
        }
        requireArgument(
                arguments,
                ModelInvokeTool.TIMEOUT_MILLIS_ARGUMENT,
                Long.toString(timeoutMillis));
        requireArgument(
                arguments,
                ModelInvokeTool.MAX_RESPONSE_LENGTH_ARGUMENT,
                Integer.toString(modelRequest.maxResponseLength()));
    }

    private static void requireArgument(
            Map<String, String> arguments,
            String name,
            String expected) {
        if (!expected.equals(arguments.get(name))) {
            throw new IllegalArgumentException(
                    name + " must match the prepared model request");
        }
    }

    private static String canonicalWorkItemId(String value) {
        Objects.requireNonNull(value, "workItemId must not be null");
        try {
            if (!UUID.fromString(value).toString().equals(value)) {
                throw new IllegalArgumentException(
                        "workItemId must be a canonical UUID");
            }
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(
                    "workItemId must be a canonical UUID", exception);
        }
        return value;
    }

    private static String boundedCapability(String value) {
        Objects.requireNonNull(value, "requiredCapability must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException(
                    "requiredCapability must not be blank");
        }
        if (value.length() > MAX_REQUIRED_CAPABILITY_CHARACTERS) {
            throw new IllegalArgumentException(
                    "requiredCapability must not exceed "
                            + MAX_REQUIRED_CAPABILITY_CHARACTERS + " characters");
        }
        return value;
    }
}
