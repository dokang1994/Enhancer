package com.enhancer.bus;

import com.enhancer.workspace.ApprovedTaskRevision;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

public record WorkPayload(
        ApprovedTaskRevision taskRevision,
        String snapshotId,
        Set<String> allowedTools) implements MessagePayload {

    public WorkPayload {
        Objects.requireNonNull(taskRevision, "taskRevision must not be null");
        snapshotId = BusContractSupport.sha256(snapshotId, "snapshotId");
        Objects.requireNonNull(allowedTools, "allowedTools must not be null");
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
