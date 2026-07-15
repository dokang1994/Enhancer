package com.enhancer.brain;

import java.util.Objects;

public record GraphEdge(
        String fromNodeId,
        GraphEdgeKind kind,
        String toNodeId,
        GraphProvenance provenance) {

    public GraphEdge {
        Objects.requireNonNull(fromNodeId, "fromNodeId must not be null");
        Objects.requireNonNull(kind, "kind must not be null");
        Objects.requireNonNull(toNodeId, "toNodeId must not be null");
        Objects.requireNonNull(provenance, "provenance must not be null");
        if (fromNodeId.isBlank()) {
            throw new IllegalArgumentException("fromNodeId must not be blank");
        }
        if (toNodeId.isBlank()) {
            throw new IllegalArgumentException("toNodeId must not be blank");
        }
        if (fromNodeId.equals(toNodeId)) {
            throw new IllegalArgumentException(
                    "graph edges must not connect a node to itself: " + fromNodeId);
        }
    }
}
