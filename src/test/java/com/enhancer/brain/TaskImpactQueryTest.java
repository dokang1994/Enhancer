package com.enhancer.brain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class TaskImpactQueryTest {
    private static final Instant PROJECTED_AT = Instant.parse("2026-07-15T07:00:00Z");
    private static final String SNAPSHOT_ID = "d".repeat(64);
    private static final GraphProvenance CURRENT_PROVENANCE = new GraphProvenance(
            "DECISION_LOG.md",
            Optional.of("a".repeat(64)),
            GraphElementFreshness.CURRENT);

    @Test
    void answersTheTaskToDecisionToCodeToTestChainDeterministically() {
        GraphNode task = node("task-1", GraphNodeKind.TASK);
        GraphNode decision = node("decision-1", GraphNodeKind.DECISION);
        GraphNode olderDecision = node("decision-0", GraphNodeKind.DECISION);
        GraphNode code = node("src/main/App.java", GraphNodeKind.ARTIFACT);
        GraphNode otherCode = node("src/main/Other.java", GraphNodeKind.ARTIFACT);
        GraphNode sharedTest = node("src/test/AppTest.java", GraphNodeKind.ARTIFACT);
        GraphNode unrelatedCode = node("src/main/Unrelated.java", GraphNodeKind.ARTIFACT);
        GraphNode unrelatedTest = node("src/test/UnrelatedTest.java", GraphNodeKind.ARTIFACT);
        GraphNode execution = node("run-record/1", GraphNodeKind.EXECUTION);

        ProjectBrainGraph graph = ProjectBrainGraph.project(
                SNAPSHOT_ID,
                PROJECTED_AT,
                List.of(task, decision, olderDecision, code, otherCode, sharedTest,
                        unrelatedCode, unrelatedTest, execution),
                List.of(
                        edge(task, GraphEdgeKind.JUSTIFIED_BY, decision),
                        edge(task, GraphEdgeKind.JUSTIFIED_BY, olderDecision),
                        edge(task, GraphEdgeKind.MODIFIES, code),
                        edge(task, GraphEdgeKind.MODIFIES, otherCode),
                        edge(code, GraphEdgeKind.VERIFIED_BY, sharedTest),
                        edge(otherCode, GraphEdgeKind.VERIFIED_BY, sharedTest),
                        edge(unrelatedCode, GraphEdgeKind.VERIFIED_BY, unrelatedTest),
                        edge(task, GraphEdgeKind.RECORDED_AS, execution)));

        TaskImpact impact = new TaskImpactQuery().query(graph, "task-1");

        assertEquals("task-1", impact.taskNodeId());
        assertEquals(SNAPSHOT_ID, impact.sourceSnapshotId());
        assertEquals(
                List.of("decision-0", "decision-1"),
                impact.decisions().stream().map(GraphNode::nodeId).toList());
        assertEquals(
                List.of("src/main/App.java", "src/main/Other.java"),
                impact.modifiedArtifacts().stream().map(GraphNode::nodeId).toList());
        assertEquals(
                List.of("src/test/AppTest.java"),
                impact.verifyingArtifacts().stream().map(GraphNode::nodeId).toList());
        assertEquals(
                List.of("run-record/1"),
                impact.executions().stream().map(GraphNode::nodeId).toList());
        assertFalse(impact.rebuildRequired());
        assertThrows(
                UnsupportedOperationException.class,
                () -> impact.decisions().add(decision));
        assertThrows(
                UnsupportedOperationException.class,
                () -> impact.verifyingArtifacts().add(sharedTest));
    }

    @Test
    void derivesRebuildRequiredFromAnyTraversedElement() {
        GraphNode task = node("task-1", GraphNodeKind.TASK);
        GraphNode code = node("src/main/App.java", GraphNodeKind.ARTIFACT);
        GraphNode staleTest = new GraphNode(
                "src/test/AppTest.java",
                GraphNodeKind.ARTIFACT,
                new GraphProvenance(
                        "src/test/AppTest.java",
                        Optional.of("b".repeat(64)),
                        GraphElementFreshness.STALE));

        ProjectBrainGraph staleTarget = ProjectBrainGraph.project(
                SNAPSHOT_ID,
                PROJECTED_AT,
                List.of(task, code, staleTest),
                List.of(
                        edge(task, GraphEdgeKind.MODIFIES, code),
                        edge(code, GraphEdgeKind.VERIFIED_BY, staleTest)));
        assertTrue(new TaskImpactQuery().query(staleTarget, "task-1").rebuildRequired());

        GraphNode test = node("src/test/AppTest.java", GraphNodeKind.ARTIFACT);
        ProjectBrainGraph staleEdge = ProjectBrainGraph.project(
                SNAPSHOT_ID,
                PROJECTED_AT,
                List.of(task, code, test),
                List.of(
                        edge(task, GraphEdgeKind.MODIFIES, code),
                        new GraphEdge(
                                code.nodeId(),
                                GraphEdgeKind.VERIFIED_BY,
                                test.nodeId(),
                                new GraphProvenance(
                                        "DELETED.md",
                                        Optional.empty(),
                                        GraphElementFreshness.SOURCE_MISSING))));
        assertTrue(new TaskImpactQuery().query(staleEdge, "task-1").rebuildRequired());

        ProjectBrainGraph unrelatedStale = ProjectBrainGraph.project(
                SNAPSHOT_ID,
                PROJECTED_AT,
                List.of(task, code, staleTest),
                List.of(edge(task, GraphEdgeKind.MODIFIES, code)));
        assertFalse(new TaskImpactQuery().query(unrelatedStale, "task-1").rebuildRequired());
    }

    @Test
    void returnsEmptyCollectionsForAnEdgelessTask() {
        GraphNode task = node("task-1", GraphNodeKind.TASK);
        ProjectBrainGraph graph = ProjectBrainGraph.project(
                SNAPSHOT_ID,
                PROJECTED_AT,
                List.of(task),
                List.of());

        TaskImpact impact = new TaskImpactQuery().query(graph, "task-1");

        assertEquals(List.of(), impact.decisions());
        assertEquals(List.of(), impact.modifiedArtifacts());
        assertEquals(List.of(), impact.verifyingArtifacts());
        assertEquals(List.of(), impact.executions());
        assertFalse(impact.rebuildRequired());
    }

    @Test
    void rejectsMissingUnknownAndNonTaskIdentities() {
        GraphNode task = node("task-1", GraphNodeKind.TASK);
        GraphNode decision = node("decision-1", GraphNodeKind.DECISION);
        ProjectBrainGraph graph = ProjectBrainGraph.project(
                SNAPSHOT_ID,
                PROJECTED_AT,
                List.of(task, decision),
                List.of(edge(task, GraphEdgeKind.JUSTIFIED_BY, decision)));
        TaskImpactQuery query = new TaskImpactQuery();

        assertThrows(NullPointerException.class, () -> query.query(null, "task-1"));
        assertThrows(NullPointerException.class, () -> query.query(graph, null));
        assertThrows(IllegalArgumentException.class, () -> query.query(graph, "missing"));
        assertThrows(IllegalArgumentException.class, () -> query.query(graph, "decision-1"));
    }

    private GraphNode node(String nodeId, GraphNodeKind kind) {
        return new GraphNode(nodeId, kind, CURRENT_PROVENANCE);
    }

    private GraphEdge edge(GraphNode from, GraphEdgeKind kind, GraphNode to) {
        return new GraphEdge(from.nodeId(), kind, to.nodeId(), CURRENT_PROVENANCE);
    }
}
