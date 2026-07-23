package com.enhancer.cli;

import java.nio.file.Path;
import java.util.Objects;

record SchedulerExternalEffectStatusCliCommand(
        Path queueRoot,
        String queueId,
        Path runtimeRoot,
        Path cycleCheckpointRoot,
        Path runRecordRoot,
        Path externalEffectRoot,
        Path evidenceRoot,
        int limit) implements CliCommand {

    static final int MAX_LISTED_EFFECTS = 8;

    SchedulerExternalEffectStatusCliCommand {
        queueRoot = normalized(queueRoot, "queueRoot");
        runtimeRoot = normalized(runtimeRoot, "runtimeRoot");
        cycleCheckpointRoot = normalized(
                cycleCheckpointRoot, "cycleCheckpointRoot");
        runRecordRoot = normalized(runRecordRoot, "runRecordRoot");
        externalEffectRoot = normalized(
                externalEffectRoot, "externalEffectRoot");
        evidenceRoot = normalized(evidenceRoot, "evidenceRoot");
        queueId = java.util.UUID.fromString(
                Objects.requireNonNull(
                        queueId, "queueId must not be null")).toString();
        if (limit < 1 || limit > MAX_LISTED_EFFECTS) {
            throw new IllegalArgumentException(
                    "limit must be between 1 and "
                            + MAX_LISTED_EFFECTS);
        }
    }

    private static Path normalized(Path value, String field) {
        return Objects.requireNonNull(
                value, field + " must not be null")
                .toAbsolutePath()
                .normalize();
    }
}
