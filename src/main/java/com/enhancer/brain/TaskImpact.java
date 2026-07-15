package com.enhancer.brain;

import java.util.List;
import java.util.Objects;

public record TaskImpact(
        String taskNodeId,
        String sourceSnapshotId,
        List<GraphNode> decisions,
        List<GraphNode> modifiedArtifacts,
        List<GraphNode> verifyingArtifacts,
        List<GraphNode> executions,
        boolean rebuildRequired) {

    public TaskImpact {
        Objects.requireNonNull(taskNodeId, "taskNodeId must not be null");
        Objects.requireNonNull(sourceSnapshotId, "sourceSnapshotId must not be null");
        Objects.requireNonNull(decisions, "decisions must not be null");
        Objects.requireNonNull(modifiedArtifacts, "modifiedArtifacts must not be null");
        Objects.requireNonNull(verifyingArtifacts, "verifyingArtifacts must not be null");
        Objects.requireNonNull(executions, "executions must not be null");
        if (taskNodeId.isBlank()) {
            throw new IllegalArgumentException("taskNodeId must not be blank");
        }
        if (sourceSnapshotId.isBlank()) {
            throw new IllegalArgumentException("sourceSnapshotId must not be blank");
        }
        decisions = List.copyOf(decisions);
        modifiedArtifacts = List.copyOf(modifiedArtifacts);
        verifyingArtifacts = List.copyOf(verifyingArtifacts);
        executions = List.copyOf(executions);
    }
}
