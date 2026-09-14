package com.enhancer.runtime;

import com.enhancer.bus.FileSpoolPublicationOutcome;
import java.util.Objects;

/** Bounded non-secret outcome of one manifest-first typed ModelWork publication attempt. */
public record DeterministicFakeModelWorkPublicationResult(
        String submissionId,
        String queueId,
        boolean manifestCreated,
        String workspaceSnapshotId,
        FileSpoolPublicationOutcome publication) {

    public DeterministicFakeModelWorkPublicationResult {
        submissionId = required(submissionId, "submissionId");
        queueId = required(queueId, "queueId");
        workspaceSnapshotId = required(workspaceSnapshotId, "workspaceSnapshotId");
        Objects.requireNonNull(publication, "publication must not be null");
    }

    private static String required(String value, String name) {
        Objects.requireNonNull(value, name + " must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }
}
