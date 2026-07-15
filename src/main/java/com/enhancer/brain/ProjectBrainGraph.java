package com.enhancer.brain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Immutable metadata-only Project Brain graph projection keyed to one Workspace snapshot
 * identity. Git and canonical repository documents remain authoritative; a projection is
 * rebuildable evidence about them and never a second source of truth.
 */
public final class ProjectBrainGraph {
    public static final int MAX_ELEMENTS = 4096;
    public static final String PROJECTION_VERSION = "project-brain-graph-v1";

    private static final Pattern SHA_256 = Pattern.compile("[0-9a-f]{64}");
    private static final Comparator<GraphNode> NODE_ORDER =
            Comparator.comparing((GraphNode node) -> node.kind().name())
                    .thenComparing(GraphNode::nodeId);
    private static final Comparator<GraphEdge> EDGE_ORDER =
            Comparator.comparing(GraphEdge::fromNodeId)
                    .thenComparing(edge -> edge.kind().name())
                    .thenComparing(GraphEdge::toNodeId);

    private final String sourceSnapshotId;
    private final Instant projectedAt;
    private final List<GraphNode> nodes;
    private final List<GraphEdge> edges;

    private ProjectBrainGraph(
            String sourceSnapshotId,
            Instant projectedAt,
            List<GraphNode> nodes,
            List<GraphEdge> edges) {
        this.sourceSnapshotId = sourceSnapshotId;
        this.projectedAt = projectedAt;
        this.nodes = nodes;
        this.edges = edges;
    }

    public static ProjectBrainGraph project(
            String sourceSnapshotId,
            Instant projectedAt,
            Collection<GraphNode> nodes,
            Collection<GraphEdge> edges) {
        Objects.requireNonNull(sourceSnapshotId, "sourceSnapshotId must not be null");
        Objects.requireNonNull(projectedAt, "projectedAt must not be null");
        Objects.requireNonNull(nodes, "nodes must not be null");
        Objects.requireNonNull(edges, "edges must not be null");
        if (!SHA_256.matcher(sourceSnapshotId).matches()) {
            throw new IllegalArgumentException(
                    "sourceSnapshotId must be 64 lowercase hexadecimal characters");
        }
        if (nodes.size() > MAX_ELEMENTS) {
            throw new IllegalArgumentException(
                    "nodes must not exceed " + MAX_ELEMENTS + " items");
        }
        if (edges.size() > MAX_ELEMENTS) {
            throw new IllegalArgumentException(
                    "edges must not exceed " + MAX_ELEMENTS + " items");
        }

        Map<String, GraphNodeKind> nodeKinds = new HashMap<>();
        List<GraphNode> orderedNodes = new ArrayList<>(nodes.size());
        for (GraphNode node : nodes) {
            Objects.requireNonNull(node, "nodes must not contain null");
            if (nodeKinds.putIfAbsent(node.nodeId(), node.kind()) != null) {
                throw new IllegalArgumentException("duplicate graph node: " + node.nodeId());
            }
            orderedNodes.add(node);
        }
        orderedNodes.sort(NODE_ORDER);

        Set<EdgeIdentity> edgeIdentities = new HashSet<>();
        List<GraphEdge> orderedEdges = new ArrayList<>(edges.size());
        for (GraphEdge edge : edges) {
            Objects.requireNonNull(edge, "edges must not contain null");
            GraphNodeKind fromKind = nodeKinds.get(edge.fromNodeId());
            GraphNodeKind toKind = nodeKinds.get(edge.toNodeId());
            if (fromKind == null || toKind == null) {
                throw new IllegalArgumentException(
                        "graph edge references an unknown node: "
                                + edge.fromNodeId() + " -> " + edge.toNodeId());
            }
            if (!edge.kind().connects(fromKind, toKind)) {
                throw new IllegalArgumentException(
                        "graph edge " + edge.kind() + " cannot connect "
                                + fromKind + " to " + toKind);
            }
            if (!edgeIdentities.add(new EdgeIdentity(
                    edge.fromNodeId(),
                    edge.kind(),
                    edge.toNodeId()))) {
                throw new IllegalArgumentException(
                        "duplicate graph edge: " + edge.fromNodeId()
                                + " -" + edge.kind() + "-> " + edge.toNodeId());
            }
            orderedEdges.add(edge);
        }
        orderedEdges.sort(EDGE_ORDER);

        return new ProjectBrainGraph(
                sourceSnapshotId,
                projectedAt,
                List.copyOf(orderedNodes),
                List.copyOf(orderedEdges));
    }

    public String sourceSnapshotId() {
        return sourceSnapshotId;
    }

    public Instant projectedAt() {
        return projectedAt;
    }

    public List<GraphNode> nodes() {
        return nodes;
    }

    public List<GraphEdge> edges() {
        return edges;
    }

    private record EdgeIdentity(
            String fromNodeId,
            GraphEdgeKind kind,
            String toNodeId) {
    }
}
