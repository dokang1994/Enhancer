package com.enhancer.brain;

import com.enhancer.run.ResolvedRunRecord;
import com.enhancer.workspace.ApprovedTaskRevision;
import com.enhancer.workspace.WorkspaceSnapshot;
import com.enhancer.workspace.WorkspaceSourceKind;
import com.enhancer.workspace.WorkspaceSourceObservation;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * First Project Brain graph producer. It projects only what one Workspace snapshot and one
 * stored run record actually prove: the approved task, the observed repository artifacts with
 * their freshness, the stored execution, and the recorded-as relationship between them. Edges
 * that no evidence justifies are never emitted.
 */
public final class RunEvidenceGraphProducer {
    private static final String PREFLIGHT_DIGEST = "0".repeat(64);

    public ProjectBrainGraph produce(
            WorkspaceSnapshot snapshot,
            ResolvedRunRecord run,
            Instant projectedAt) {
        return produce(snapshot, run, projectedAt, List.of());
    }

    public ProjectBrainGraph produce(
            WorkspaceSnapshot snapshot,
            ResolvedRunRecord run,
            Instant projectedAt,
            List<GraphNode> additionalNodes) {
        return produce(snapshot, run, projectedAt, additionalNodes, List.of());
    }

    public ProjectBrainGraph produce(
            WorkspaceSnapshot snapshot,
            ResolvedRunRecord run,
            Instant projectedAt,
            List<GraphNode> additionalNodes,
            List<GraphEdge> additionalEdges) {
        Objects.requireNonNull(snapshot, "snapshot must not be null");
        Objects.requireNonNull(run, "run must not be null");
        Objects.requireNonNull(projectedAt, "projectedAt must not be null");
        Objects.requireNonNull(additionalNodes, "additionalNodes must not be null");
        Objects.requireNonNull(additionalEdges, "additionalEdges must not be null");
        requireSameApprovedTask(snapshot.approvedTaskRevision(), run);

        ApprovedTaskRevision revision = snapshot.approvedTaskRevision();
        List<GraphNode> nodes = baseNodes(snapshot);

        String reference = run.metadata().reference();
        GraphProvenance executionProvenance = new GraphProvenance(
                reference,
                Optional.of(run.metadata().sha256()),
                GraphElementFreshness.CURRENT);
        nodes.add(new GraphNode(reference, GraphNodeKind.EXECUTION, executionProvenance));
        nodes.addAll(additionalNodes);

        List<GraphEdge> edges = new ArrayList<>(1 + additionalEdges.size());
        edges.add(new GraphEdge(
                revision.taskId(),
                GraphEdgeKind.RECORDED_AS,
                reference,
                executionProvenance));
        edges.addAll(additionalEdges);

        return ProjectBrainGraph.project(
                snapshot.snapshotId(),
                projectedAt,
                nodes,
                edges);
    }

    public void preflight(
            WorkspaceSnapshot snapshot,
            Instant projectedAt,
            List<GraphNode> additionalNodes,
            List<GraphEdge> additionalEdges) {
        Objects.requireNonNull(snapshot, "snapshot must not be null");
        Objects.requireNonNull(projectedAt, "projectedAt must not be null");
        Objects.requireNonNull(additionalNodes, "additionalNodes must not be null");
        Objects.requireNonNull(additionalEdges, "additionalEdges must not be null");
        ApprovedTaskRevision revision = snapshot.approvedTaskRevision();
        List<GraphNode> nodes = baseNodes(snapshot);
        nodes.addAll(additionalNodes);
        String executionId = uniquePreflightExecutionId(nodes);
        GraphProvenance executionProvenance = new GraphProvenance(
                executionId,
                Optional.of(PREFLIGHT_DIGEST),
                GraphElementFreshness.CURRENT);
        nodes.add(new GraphNode(executionId, GraphNodeKind.EXECUTION, executionProvenance));
        List<GraphEdge> edges = new ArrayList<>(additionalEdges.size() + 1);
        edges.add(new GraphEdge(
                revision.taskId(),
                GraphEdgeKind.RECORDED_AS,
                executionId,
                executionProvenance));
        edges.addAll(additionalEdges);
        ProjectBrainGraph.project(snapshot.snapshotId(), projectedAt, nodes, edges);
    }

    private static List<GraphNode> baseNodes(WorkspaceSnapshot snapshot) {
        ApprovedTaskRevision revision = snapshot.approvedTaskRevision();
        List<GraphNode> nodes = new ArrayList<>();
        nodes.add(new GraphNode(
                revision.taskId(),
                GraphNodeKind.TASK,
                new GraphProvenance(
                        revision.sourceDocument(),
                        Optional.of(revision.sourceSha256()),
                        GraphElementFreshness.CURRENT)));
        Map<String, WorkspaceSourceObservation> artifacts = new LinkedHashMap<>();
        for (WorkspaceSourceObservation observation : snapshot.observations()) {
            if (observation.kind() == WorkspaceSourceKind.REPOSITORY_DOCUMENT) {
                artifacts.putIfAbsent(observation.sourceId(), observation);
            } else if (observation.kind() == WorkspaceSourceKind.REPOSITORY_FILE) {
                artifacts.put(observation.sourceId(), observation);
            }
        }
        for (WorkspaceSourceObservation observation : artifacts.values()) {
            nodes.add(new GraphNode(
                    observation.sourceId(),
                    GraphNodeKind.ARTIFACT,
                    new GraphProvenance(
                            observation.sourceId(),
                            observation.contentSha256(),
                            freshness(observation))));
        }
        return nodes;
    }

    private static String uniquePreflightExecutionId(List<GraphNode> nodes) {
        String prefix = "run-record/preflight";
        String candidate = prefix;
        int suffix = 1;
        while (containsNode(nodes, candidate)) {
            candidate = prefix + "-" + suffix++;
        }
        return candidate;
    }

    private static boolean containsNode(List<GraphNode> nodes, String nodeId) {
        return nodes.stream().anyMatch(node -> node.nodeId().equals(nodeId));
    }

    private static GraphElementFreshness freshness(WorkspaceSourceObservation observation) {
        return switch (observation.state()) {
            case AVAILABLE -> GraphElementFreshness.CURRENT;
            case STALE -> GraphElementFreshness.STALE;
            case UNAVAILABLE -> GraphElementFreshness.SOURCE_MISSING;
        };
    }

    private static void requireSameApprovedTask(
            ApprovedTaskRevision revision,
            ResolvedRunRecord run) {
        String runTaskId = run.record().approvedTask().taskId();
        if (!revision.taskId().equals(runTaskId)) {
            throw new IllegalArgumentException(
                    "run task " + runTaskId
                            + " does not match snapshot task " + revision.taskId());
        }
        String runSourceDocument = run.record().approvedTask().sourceDocument();
        if (!revision.sourceDocument().equals(runSourceDocument)) {
            throw new IllegalArgumentException(
                    "run source document " + runSourceDocument
                            + " does not match snapshot source document "
                            + revision.sourceDocument());
        }
    }
}
