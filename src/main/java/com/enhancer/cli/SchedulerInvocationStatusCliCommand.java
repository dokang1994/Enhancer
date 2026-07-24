package com.enhancer.cli;

import java.nio.file.Path;
import java.util.Objects;

record SchedulerInvocationStatusCliCommand(
        Path queueRoot,
        String queueId,
        Path runtimeRoot,
        Path cycleCheckpointRoot,
        Path runRecordRoot,
        Path invocationRoot,
        int limit) implements CliCommand {

    static final int MAX_LISTED_MESSAGES = 8;

    SchedulerInvocationStatusCliCommand {
        queueRoot = normalized(queueRoot, "queueRoot");
        runtimeRoot = normalized(runtimeRoot, "runtimeRoot");
        cycleCheckpointRoot = normalized(
                cycleCheckpointRoot, "cycleCheckpointRoot");
        runRecordRoot = normalized(runRecordRoot, "runRecordRoot");
        invocationRoot = normalized(invocationRoot, "invocationRoot");
        queueId = java.util.UUID.fromString(
                Objects.requireNonNull(queueId, "queueId must not be null")).toString();
        if (limit < 1 || limit > MAX_LISTED_MESSAGES) {
            throw new IllegalArgumentException(
                    "limit must be between 1 and " + MAX_LISTED_MESSAGES);
        }
    }

    private static Path normalized(Path value, String field) {
        return Objects.requireNonNull(value, field + " must not be null")
                .toAbsolutePath()
                .normalize();
    }
}
