package com.enhancer.brain;

import java.util.Set;

public enum GraphEdgeKind {
    JUSTIFIED_BY(Set.of(GraphNodeKind.TASK), Set.of(GraphNodeKind.DECISION)),
    SUPERSEDES(Set.of(GraphNodeKind.DECISION), Set.of(GraphNodeKind.DECISION)),
    DEPENDS_ON(
            Set.of(GraphNodeKind.COMPONENT, GraphNodeKind.ARTIFACT),
            Set.of(GraphNodeKind.COMPONENT, GraphNodeKind.ARTIFACT)),
    MODIFIES(Set.of(GraphNodeKind.TASK), Set.of(GraphNodeKind.ARTIFACT)),
    VERIFIED_BY(Set.of(GraphNodeKind.ARTIFACT), Set.of(GraphNodeKind.ARTIFACT)),
    RECORDED_AS(Set.of(GraphNodeKind.TASK), Set.of(GraphNodeKind.EXECUTION));

    private final Set<GraphNodeKind> fromKinds;
    private final Set<GraphNodeKind> toKinds;

    GraphEdgeKind(Set<GraphNodeKind> fromKinds, Set<GraphNodeKind> toKinds) {
        this.fromKinds = fromKinds;
        this.toKinds = toKinds;
    }

    public boolean connects(GraphNodeKind from, GraphNodeKind to) {
        return fromKinds.contains(from) && toKinds.contains(to);
    }
}
