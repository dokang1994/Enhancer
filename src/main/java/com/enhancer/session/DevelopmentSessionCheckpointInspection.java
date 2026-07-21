package com.enhancer.session;

import java.util.List;
import java.util.Objects;

/** Recovery view that keeps durable state distinct from current task/artifact comparison. */
public record DevelopmentSessionCheckpointInspection(
        DevelopmentSessionCheckpoint checkpoint,
        boolean taskContractMatches,
        List<String> artifactMismatches) {
    public DevelopmentSessionCheckpointInspection {
        Objects.requireNonNull(checkpoint, "checkpoint must not be null");
        artifactMismatches = List.copyOf(Objects.requireNonNull(
                artifactMismatches, "artifactMismatches must not be null"));
    }
}
