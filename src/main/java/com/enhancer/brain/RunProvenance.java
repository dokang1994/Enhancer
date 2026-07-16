package com.enhancer.brain;

import com.enhancer.run.RunRecord;
import com.enhancer.kernel.VerificationStatus;
import java.time.Instant;
import java.util.Objects;

public record RunProvenance(
        String logicalRunId,
        Instant recordedAt,
        String taskId,
        VerificationStatus verificationStatus) {

    public RunProvenance {
        Objects.requireNonNull(logicalRunId, "logicalRunId must not be null");
        Objects.requireNonNull(recordedAt, "recordedAt must not be null");
        Objects.requireNonNull(taskId, "taskId must not be null");
        Objects.requireNonNull(verificationStatus, "verificationStatus must not be null");
        if (logicalRunId.isBlank()) {
            throw new IllegalArgumentException("logicalRunId must not be blank");
        }
        if (taskId.isBlank()) {
            throw new IllegalArgumentException("taskId must not be blank");
        }
    }

    static RunProvenance of(RunRecord run) {
        return new RunProvenance(
                run.logicalRunId(),
                run.recordedAt(),
                run.approvedTask().taskId(),
                run.verification().status());
    }
}
