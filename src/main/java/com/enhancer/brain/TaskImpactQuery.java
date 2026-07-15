package com.enhancer.brain;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * First rebuildable task-to-decision-to-code-to-test impact query. It answers over exactly one
 * projected graph and traverses only the named chain; the result carries the graph's source
 * snapshot identity and a derived rebuild status so consumers know when the answer is stale.
 */
public final class TaskImpactQuery {

    public TaskImpact query(ProjectBrainGraph graph, String taskNodeId) {
        Objects.requireNonNull(graph, "graph must not be null");
        Objects.requireNonNull(taskNodeId, "taskNodeId must not be null");

        Map<String, GraphNode> nodesById = new HashMap<>();
        for (GraphNode node : graph.nodes()) {
            nodesById.put(node.nodeId(), node);
        }
        GraphNode task = nodesById.get(taskNodeId);
        if (task == null) {
            throw new IllegalArgumentException("graph does not contain node: " + taskNodeId);
        }
        if (task.kind() != GraphNodeKind.TASK) {
            throw new IllegalArgumentException(
                    "impact queries start from a task node, not " + task.kind());
        }

        boolean rebuildRequired = task.provenance().rebuildRequired();
        List<GraphNode> decisions = new ArrayList<>();
        List<GraphNode> modifiedArtifacts = new ArrayList<>();
        List<GraphNode> executions = new ArrayList<>();
        Set<String> modifiedArtifactIds = new LinkedHashSet<>();
        for (GraphEdge edge : graph.edges()) {
            if (!edge.fromNodeId().equals(taskNodeId)) {
                continue;
            }
            GraphNode target = nodesById.get(edge.toNodeId());
            switch (edge.kind()) {
                case JUSTIFIED_BY -> decisions.add(target);
                case MODIFIES -> {
                    modifiedArtifacts.add(target);
                    modifiedArtifactIds.add(target.nodeId());
                }
                case RECORDED_AS -> executions.add(target);
                default -> {
                    continue;
                }
            }
            rebuildRequired = rebuildRequired
                    || edge.provenance().rebuildRequired()
                    || target.provenance().rebuildRequired();
        }

        Set<String> verifyingArtifactIds = new LinkedHashSet<>();
        List<GraphNode> verifyingArtifacts = new ArrayList<>();
        for (GraphEdge edge : graph.edges()) {
            if (edge.kind() != GraphEdgeKind.VERIFIED_BY
                    || !modifiedArtifactIds.contains(edge.fromNodeId())) {
                continue;
            }
            GraphNode target = nodesById.get(edge.toNodeId());
            if (verifyingArtifactIds.add(target.nodeId())) {
                verifyingArtifacts.add(target);
            }
            rebuildRequired = rebuildRequired
                    || edge.provenance().rebuildRequired()
                    || target.provenance().rebuildRequired();
        }

        return new TaskImpact(
                taskNodeId,
                graph.sourceSnapshotId(),
                decisions,
                modifiedArtifacts,
                verifyingArtifacts,
                executions,
                rebuildRequired);
    }
}
