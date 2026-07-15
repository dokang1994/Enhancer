package com.enhancer.bus;

import com.enhancer.workspace.ApprovedTaskRevision;
import java.util.Objects;

public record HandoffPayload(
        ApprovedTaskRevision taskRevision,
        String snapshotId,
        String runRecordReference) implements MessagePayload {

    public HandoffPayload {
        Objects.requireNonNull(taskRevision, "taskRevision must not be null");
        snapshotId = BusContractSupport.sha256(snapshotId, "snapshotId");
        runRecordReference = BusContractSupport.bounded(
                runRecordReference,
                "runRecordReference",
                BusContractSupport.MAX_REFERENCE_CHARACTERS);
    }
}
