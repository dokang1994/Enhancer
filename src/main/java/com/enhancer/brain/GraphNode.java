package com.enhancer.brain;

import java.util.Objects;

public record GraphNode(
        String nodeId,
        GraphNodeKind kind,
        GraphProvenance provenance) {

    public static final int MAX_NODE_ID_CHARACTERS = 256;

    public GraphNode {
        Objects.requireNonNull(nodeId, "nodeId must not be null");
        Objects.requireNonNull(kind, "kind must not be null");
        Objects.requireNonNull(provenance, "provenance must not be null");
        if (nodeId.isBlank()) {
            throw new IllegalArgumentException("nodeId must not be blank");
        }
        if (nodeId.length() > MAX_NODE_ID_CHARACTERS) {
            throw new IllegalArgumentException(
                    "nodeId must not exceed " + MAX_NODE_ID_CHARACTERS + " characters");
        }
    }
}
