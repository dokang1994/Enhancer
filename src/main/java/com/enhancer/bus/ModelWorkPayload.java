package com.enhancer.bus;

import com.enhancer.model.ModelExecutionProfile;
import com.enhancer.workspace.ApprovedTaskRevision;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Complete typed input for future profiled model work.
 *
 * <p>The retained profile is untrusted requirement data. This payload creates no task,
 * capability, Tool, provider, route, network, credential, spend, or gateway authority.
 */
public record ModelWorkPayload(
        ApprovedTaskRevision taskRevision,
        String snapshotId,
        Set<String> allowedTools,
        ModelInvocationExecutionInput executionInput) implements MessagePayload {

    public static final int MAX_ALLOWED_TOOLS = WorkPayload.MAX_ALLOWED_TOOLS;
    public static final String MODEL_INVOKE_TOOL_NAME = "model-invoke";

    /** Mandatory target, expected response, and complete profile for one model invocation. */
    public record ModelInvocationExecutionInput(
            String targetPath,
            String expectedResponseSha256,
            ModelExecutionProfile executionProfile) {

        public ModelInvocationExecutionInput {
            targetPath = BusContractSupport.bounded(
                    targetPath,
                    "targetPath",
                    BusContractSupport.MAX_REFERENCE_CHARACTERS);
            expectedResponseSha256 = BusContractSupport.sha256(
                    expectedResponseSha256, "expectedResponseSha256");
            Objects.requireNonNull(
                    executionProfile, "executionProfile must not be null");
        }
    }

    public ModelWorkPayload {
        Objects.requireNonNull(taskRevision, "taskRevision must not be null");
        snapshotId = BusContractSupport.sha256(snapshotId, "snapshotId");
        Objects.requireNonNull(allowedTools, "allowedTools must not be null");
        Objects.requireNonNull(executionInput, "executionInput must not be null");
        if (allowedTools.size() > MAX_ALLOWED_TOOLS) {
            throw new IllegalArgumentException(
                    "allowedTools must not contain more than "
                            + MAX_ALLOWED_TOOLS + " entries");
        }
        Set<String> scope = new LinkedHashSet<>();
        for (String toolName : allowedTools) {
            scope.add(BusContractSupport.bounded(
                    toolName,
                    "allowedTools entry",
                    BusContractSupport.MAX_IDENTITY_CHARACTERS));
        }
        if (!scope.contains(MODEL_INVOKE_TOOL_NAME)) {
            throw new IllegalArgumentException(
                    "allowedTools must contain " + MODEL_INVOKE_TOOL_NAME);
        }
        allowedTools = Set.copyOf(scope);
    }
}
