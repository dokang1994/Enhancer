package com.enhancer.bus;

import com.enhancer.workspace.ApprovedTaskRevision;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public record WorkPayload(
        ApprovedTaskRevision taskRevision,
        String snapshotId,
        Set<String> allowedTools,
        Optional<ExecutionInput> executionInput) implements MessagePayload {

    public static final int MAX_ALLOWED_TOOLS = 256;

    /**
     * Optional caller-supplied statement of the concrete governed work: the relative target
     * path for the read-file Tool and the expected content SHA-256 the verifier checks. Absent
     * input leaves the consumer to fall back to the approved task's own source document.
     */
    public record ExecutionInput(String targetPath, String expectedContentSha256) {
        public ExecutionInput {
            targetPath = BusContractSupport.bounded(
                    targetPath,
                    "targetPath",
                    BusContractSupport.MAX_REFERENCE_CHARACTERS);
            expectedContentSha256 = BusContractSupport.sha256(
                    expectedContentSha256, "expectedContentSha256");
        }
    }

    public WorkPayload(
            ApprovedTaskRevision taskRevision,
            String snapshotId,
            Set<String> allowedTools) {
        this(taskRevision, snapshotId, allowedTools, Optional.empty());
    }

    public WorkPayload {
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
        if (scope.isEmpty()) {
            throw new IllegalArgumentException("allowedTools must not be empty");
        }
        allowedTools = Set.copyOf(scope);
    }
}
