package com.enhancer.brain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class ProjectBrainGraphTest {
    private static final Instant PROJECTED_AT = Instant.parse("2026-07-15T06:00:00Z");
    private static final String SNAPSHOT_ID = "c".repeat(64);
    private static final GraphProvenance CURRENT_PROVENANCE = new GraphProvenance(
            "DECISION_LOG.md",
            Optional.of("a".repeat(64)),
            GraphElementFreshness.CURRENT);

    @Test
    void projectsADeterministicallyOrderedImmutableTypedGraph() {
        GraphNode decision = node("decision-1", GraphNodeKind.DECISION);
        GraphNode task = node("task-1", GraphNodeKind.TASK);
        GraphNode code = node("src/main/App.java", GraphNodeKind.ARTIFACT);
        GraphNode test = node("src/test/AppTest.java", GraphNodeKind.ARTIFACT);
        GraphNode execution = node("run-record/1", GraphNodeKind.EXECUTION);
        GraphEdge justifies = edge(task, GraphEdgeKind.JUSTIFIED_BY, decision);
        GraphEdge modifies = edge(task, GraphEdgeKind.MODIFIES, code);
        GraphEdge verifies = edge(code, GraphEdgeKind.VERIFIED_BY, test);
        GraphEdge recorded = edge(task, GraphEdgeKind.RECORDED_AS, execution);

        ProjectBrainGraph graph = ProjectBrainGraph.project(
                SNAPSHOT_ID,
                PROJECTED_AT,
                List.of(execution, test, code, task, decision),
                List.of(recorded, verifies, modifies, justifies));
        ProjectBrainGraph reordered = ProjectBrainGraph.project(
                SNAPSHOT_ID,
                PROJECTED_AT,
                List.of(decision, task, code, test, execution),
                List.of(justifies, modifies, verifies, recorded));

        assertEquals(SNAPSHOT_ID, graph.sourceSnapshotId());
        assertEquals(PROJECTED_AT, graph.projectedAt());
        assertEquals(graph.nodes(), reordered.nodes());
        assertEquals(graph.edges(), reordered.edges());
        assertEquals(
                List.of("src/main/App.java", "src/test/AppTest.java", "decision-1",
                        "run-record/1", "task-1"),
                graph.nodes().stream().map(GraphNode::nodeId).toList());
        assertThrows(
                UnsupportedOperationException.class,
                () -> graph.nodes().add(task));
        assertThrows(
                UnsupportedOperationException.class,
                () -> graph.edges().add(justifies));
    }

    @Test
    void enforcesEndpointKindConsistencyForEveryEdgeKind() {
        GraphNode task = node("task-1", GraphNodeKind.TASK);
        GraphNode decision = node("decision-1", GraphNodeKind.DECISION);
        GraphNode component = node("component-1", GraphNodeKind.COMPONENT);
        GraphNode artifact = node("src/main/App.java", GraphNodeKind.ARTIFACT);
        GraphNode execution = node("run-record/1", GraphNodeKind.EXECUTION);
        List<GraphNode> nodes = List.of(task, decision, component, artifact, execution);

        assertProjects(nodes, edge(task, GraphEdgeKind.JUSTIFIED_BY, decision));
        assertRejects(nodes, edge(artifact, GraphEdgeKind.JUSTIFIED_BY, decision));
        assertRejects(nodes, edge(task, GraphEdgeKind.JUSTIFIED_BY, component));

        GraphNode otherDecision = node("decision-2", GraphNodeKind.DECISION);
        assertProjects(
                List.of(decision, otherDecision),
                edge(decision, GraphEdgeKind.SUPERSEDES, otherDecision));
        assertRejects(nodes, edge(task, GraphEdgeKind.SUPERSEDES, decision));

        assertProjects(nodes, edge(component, GraphEdgeKind.DEPENDS_ON, artifact));
        assertProjects(nodes, edge(artifact, GraphEdgeKind.DEPENDS_ON, component));
        assertRejects(nodes, edge(task, GraphEdgeKind.DEPENDS_ON, artifact));
        assertRejects(nodes, edge(component, GraphEdgeKind.DEPENDS_ON, execution));

        assertProjects(nodes, edge(task, GraphEdgeKind.MODIFIES, artifact));
        assertRejects(nodes, edge(execution, GraphEdgeKind.MODIFIES, artifact));

        GraphNode testArtifact = node("src/test/AppTest.java", GraphNodeKind.ARTIFACT);
        assertProjects(
                List.of(artifact, testArtifact),
                edge(artifact, GraphEdgeKind.VERIFIED_BY, testArtifact));
        assertRejects(nodes, edge(task, GraphEdgeKind.VERIFIED_BY, artifact));

        assertProjects(nodes, edge(task, GraphEdgeKind.RECORDED_AS, execution));
        assertRejects(nodes, edge(decision, GraphEdgeKind.RECORDED_AS, execution));
    }

    @Test
    void rejectsDuplicatesSelfLoopsUnknownEndpointsAndExcessiveCollections() {
        GraphNode task = node("task-1", GraphNodeKind.TASK);
        GraphNode decision = node("decision-1", GraphNodeKind.DECISION);
        GraphEdge justifies = edge(task, GraphEdgeKind.JUSTIFIED_BY, decision);

        assertThrows(IllegalArgumentException.class, () -> ProjectBrainGraph.project(
                SNAPSHOT_ID,
                PROJECTED_AT,
                List.of(task, node("task-1", GraphNodeKind.TASK)),
                List.of()));
        assertThrows(IllegalArgumentException.class, () -> ProjectBrainGraph.project(
                SNAPSHOT_ID,
                PROJECTED_AT,
                List.of(task, decision),
                List.of(justifies, justifies)));
        assertThrows(IllegalArgumentException.class, () -> ProjectBrainGraph.project(
                SNAPSHOT_ID,
                PROJECTED_AT,
                List.of(decision),
                List.of(new GraphEdge(
                        "decision-1",
                        GraphEdgeKind.SUPERSEDES,
                        "decision-1",
                        CURRENT_PROVENANCE))));
        assertThrows(IllegalArgumentException.class, () -> ProjectBrainGraph.project(
                SNAPSHOT_ID,
                PROJECTED_AT,
                List.of(task),
                List.of(justifies)));
        assertThrows(IllegalArgumentException.class, () -> ProjectBrainGraph.project(
                "not-a-digest",
                PROJECTED_AT,
                List.of(task, decision),
                List.of(justifies)));

        List<GraphNode> excessive = IntStream
                .rangeClosed(0, ProjectBrainGraph.MAX_ELEMENTS)
                .mapToObj(index -> node("task-" + index, GraphNodeKind.TASK))
                .toList();
        assertThrows(IllegalArgumentException.class, () -> ProjectBrainGraph.project(
                SNAPSHOT_ID,
                PROJECTED_AT,
                excessive,
                List.of()));
    }

    @Test
    void enforcesProvenanceFreshnessAndRevisionInvariants() {
        assertEquals(false, CURRENT_PROVENANCE.rebuildRequired());
        GraphProvenance stale = new GraphProvenance(
                "DECISION_LOG.md",
                Optional.of("b".repeat(64)),
                GraphElementFreshness.STALE);
        assertTrue(stale.rebuildRequired());
        GraphProvenance missing = new GraphProvenance(
                "DELETED.md",
                Optional.empty(),
                GraphElementFreshness.SOURCE_MISSING);
        assertTrue(missing.rebuildRequired());

        assertThrows(IllegalArgumentException.class, () -> new GraphProvenance(
                "DECISION_LOG.md",
                Optional.empty(),
                GraphElementFreshness.CURRENT));
        assertThrows(IllegalArgumentException.class, () -> new GraphProvenance(
                "DECISION_LOG.md",
                Optional.empty(),
                GraphElementFreshness.STALE));
        assertThrows(IllegalArgumentException.class, () -> new GraphProvenance(
                "DELETED.md",
                Optional.of("a".repeat(64)),
                GraphElementFreshness.SOURCE_MISSING));
        assertThrows(IllegalArgumentException.class, () -> new GraphProvenance(
                "DECISION_LOG.md",
                Optional.of("UPPERCASE".repeat(8)),
                GraphElementFreshness.CURRENT));
        assertThrows(IllegalArgumentException.class, () -> new GraphProvenance(
                " ",
                Optional.of("a".repeat(64)),
                GraphElementFreshness.CURRENT));
        assertThrows(IllegalArgumentException.class, () -> new GraphNode(
                " ",
                GraphNodeKind.TASK,
                CURRENT_PROVENANCE));
        assertFalse(GraphElementFreshness.CURRENT.rebuildRequired());
    }

    private void assertProjects(List<GraphNode> nodes, GraphEdge edge) {
        ProjectBrainGraph graph = ProjectBrainGraph.project(
                SNAPSHOT_ID,
                PROJECTED_AT,
                nodes,
                List.of(edge));
        assertEquals(List.of(edge), graph.edges());
    }

    private void assertRejects(List<GraphNode> nodes, GraphEdge edge) {
        assertThrows(IllegalArgumentException.class, () -> ProjectBrainGraph.project(
                SNAPSHOT_ID,
                PROJECTED_AT,
                nodes,
                List.of(edge)));
    }

    private GraphNode node(String nodeId, GraphNodeKind kind) {
        return new GraphNode(nodeId, kind, CURRENT_PROVENANCE);
    }

    private GraphEdge edge(GraphNode from, GraphEdgeKind kind, GraphNode to) {
        return new GraphEdge(from.nodeId(), kind, to.nodeId(), CURRENT_PROVENANCE);
    }
}
