package com.enhancer.cli;

import java.nio.file.Path;
import java.util.Objects;

record SchedulerRecoveryStatusCliCommand(
        Path queueRoot,
        String queueId,
        Path runtimeRoot,
        Path cycleCheckpointRoot,
        Path runRecordRoot) implements CliCommand {

    SchedulerRecoveryStatusCliCommand {
        queueRoot = normalized(queueRoot, "queueRoot");
        runtimeRoot = normalized(runtimeRoot, "runtimeRoot");
        cycleCheckpointRoot = normalized(
                cycleCheckpointRoot, "cycleCheckpointRoot");
        runRecordRoot = normalized(runRecordRoot, "runRecordRoot");
        queueId = java.util.UUID.fromString(
                Objects.requireNonNull(
                        queueId, "queueId must not be null")).toString();
    }

    private static Path normalized(Path value, String field) {
        return Objects.requireNonNull(
                value, field + " must not be null")
                .toAbsolutePath()
                .normalize();
    }
}
