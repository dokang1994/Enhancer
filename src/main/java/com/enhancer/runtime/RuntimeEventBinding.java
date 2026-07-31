package com.enhancer.runtime;

import com.enhancer.workspace.ApprovedTaskRevision;
import java.util.Objects;

/**
 * Immutable Goal and retained WorkItem provenance shared by every event in one stream.
 */
public record RuntimeEventBinding(
        String goalId,
        String workItemId,
        ApprovedTaskRevision taskRevision,
        String snapshotId,
        String logicalRunId,
        String correlationId) {

    public static final int MAX_RUN_IDENTITY_CHARACTERS = 256;

    public RuntimeEventBinding {
        goalId = RuntimeIdentity.canonicalUuid(goalId, "goalId");
        workItemId = RuntimeIdentity.canonicalUuid(workItemId, "workItemId");
        Objects.requireNonNull(taskRevision, "taskRevision must not be null");
        snapshotId = RuntimeEventContractSupport.sha256(
                snapshotId,
                "snapshotId");
        logicalRunId = RuntimeEventContractSupport.bounded(
                logicalRunId,
                "logicalRunId",
                MAX_RUN_IDENTITY_CHARACTERS);
        correlationId = RuntimeEventContractSupport.bounded(
                correlationId,
                "correlationId",
                MAX_RUN_IDENTITY_CHARACTERS);
    }
}
