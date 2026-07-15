package com.enhancer.brain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.enhancer.context.ProjectContext;
import com.enhancer.context.ProjectDocument;
import com.enhancer.workspace.ApprovedTaskRevision;
import com.enhancer.workspace.WorkspaceSnapshot;
import com.enhancer.workspace.WorkspaceSourceKind;
import com.enhancer.workspace.WorkspaceSourceObservation;
import com.enhancer.workspace.WorkspaceSourceState;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class TaskJustificationProjectorTest {
    private static final Instant CAPTURED_AT = Instant.parse("2026-07-15T11:00:00Z");
    private static final String TASK_ID = "gate-6-justification-test";
    private static final String FIRST_DECISION = "2026-07-14: Adopt The First Rule";
    private static final String SECOND_DECISION = "2026-07-15: Adopt The Second Rule";
    private static final GraphProvenance DECISION_PROVENANCE = new GraphProvenance(
            "DECISION_LOG.md",
            Optional.of("a".repeat(64)),
            GraphElementFreshness.CURRENT);
    private static final List<GraphNode> DECISIONS = List.of(
            new GraphNode(FIRST_DECISION, GraphNodeKind.DECISION, DECISION_PROVENANCE),
            new GraphNode(SECOND_DECISION, GraphNodeKind.DECISION, DECISION_PROVENANCE));

    @Test
    void projectsOneJustifiedByEdgePerResolvedReferenceWithTaskDocumentProvenance() {
        String taskDocument = taskDocument(
                "## Justified By\n\n- " + FIRST_DECISION + "\n- " + SECOND_DECISION + "\n");
        ProjectContext memory = memory(taskDocument);
        WorkspaceSnapshot snapshot = snapshot(Optional.of(sha256(taskDocument)));

        List<GraphEdge> edges = new TaskJustificationProjector().project(
                snapshot,
                memory,
                DECISIONS);

        assertEquals(2, edges.size());
        for (GraphEdge edge : edges) {
            assertEquals(TASK_ID, edge.fromNodeId());
            assertEquals(GraphEdgeKind.JUSTIFIED_BY, edge.kind());
            assertEquals("CURRENT_TASK.md", edge.provenance().sourceRef());
            assertEquals(Optional.of(sha256(taskDocument)), edge.provenance().sourceSha256());
            assertEquals(GraphElementFreshness.CURRENT, edge.provenance().freshness());
        }
        assertEquals(
                List.of(FIRST_DECISION, SECOND_DECISION),
                edges.stream().map(GraphEdge::toNodeId).toList());
    }

    @Test
    void returnsNoEdgesWhenTheSectionIsAbsentAndMarksStaleWhenUnobserved() {
        String withoutSection = taskDocument("");
        ProjectContext memory = memory(withoutSection);
        assertEquals(
                List.of(),
                new TaskJustificationProjector().project(
                        snapshot(Optional.of(sha256(withoutSection))),
                        memory,
                        DECISIONS));

        String withSection = taskDocument("## Justified By\n\n- " + FIRST_DECISION + "\n");
        List<GraphEdge> stale = new TaskJustificationProjector().project(
                snapshot(Optional.empty()),
                memory(withSection),
                DECISIONS);
        assertEquals(GraphElementFreshness.STALE, stale.get(0).provenance().freshness());
    }

    @Test
    void rejectsEmptySectionsNonBulletsUnresolvedAndDuplicateReferences() {
        TaskJustificationProjector projector = new TaskJustificationProjector();
        WorkspaceSnapshot snapshot = snapshot(Optional.of("b".repeat(64)));

        assertThrows(IllegalArgumentException.class, () -> projector.project(
                snapshot,
                memory(taskDocument("## Justified By\n\n")),
                DECISIONS));
        assertThrows(IllegalArgumentException.class, () -> projector.project(
                snapshot,
                memory(taskDocument("## Justified By\n\nnot a bullet\n")),
                DECISIONS));
        assertThrows(IllegalArgumentException.class, () -> projector.project(
                snapshot,
                memory(taskDocument("## Justified By\n\n- 2026-07-15: Never Accepted\n")),
                DECISIONS));
        assertThrows(IllegalArgumentException.class, () -> projector.project(
                snapshot,
                memory(taskDocument(
                        "## Justified By\n\n- " + FIRST_DECISION + "\n- " + FIRST_DECISION
                                + "\n")),
                DECISIONS));
        assertThrows(IllegalArgumentException.class, () -> projector.project(
                snapshot,
                new ProjectContext(List.of(
                        new ProjectDocument("ARCHITECTURE.md", 1, "# Architecture"))),
                DECISIONS));
        assertThrows(NullPointerException.class, () -> projector.project(
                null,
                memory(taskDocument("")),
                DECISIONS));
        assertThrows(NullPointerException.class, () -> projector.project(
                snapshot,
                null,
                DECISIONS));
        assertThrows(NullPointerException.class, () -> projector.project(
                snapshot,
                memory(taskDocument("")),
                null));
    }

    @Test
    void projectedEdgesComposeIntoAGraphAndSurfaceInImpactAnswers() {
        String taskDocument = taskDocument(
                "## Justified By\n\n- " + FIRST_DECISION + "\n");
        WorkspaceSnapshot snapshot = snapshot(Optional.of(sha256(taskDocument)));
        List<GraphEdge> edges = new TaskJustificationProjector().project(
                snapshot,
                memory(taskDocument),
                DECISIONS);
        GraphNode task = new GraphNode(
                TASK_ID,
                GraphNodeKind.TASK,
                new GraphProvenance(
                        "CURRENT_TASK.md",
                        Optional.of(sha256(taskDocument)),
                        GraphElementFreshness.CURRENT));

        ProjectBrainGraph graph = ProjectBrainGraph.project(
                snapshot.snapshotId(),
                CAPTURED_AT.plusSeconds(1),
                List.of(task, DECISIONS.get(0), DECISIONS.get(1)),
                edges);
        TaskImpact impact = new TaskImpactQuery().query(graph, TASK_ID);

        assertEquals(
                List.of(FIRST_DECISION),
                impact.decisions().stream().map(GraphNode::nodeId).toList());
    }

    private String taskDocument(String justifiedBySection) {
        return "# Current Task\n\n"
                + "## Status\n\nIn Progress\n\n"
                + "## Task\n\nProject task justification references.\n\n"
                + "## Task ID\n\n" + TASK_ID + "\n\n"
                + justifiedBySection
                + "\n## Approval\n\nApproved by the test owner.\n\n"
                + "## Allowed Tools\n\n- read-file\n";
    }

    private ProjectContext memory(String taskDocument) {
        return new ProjectContext(List.of(
                new ProjectDocument("CURRENT_TASK.md", 1, taskDocument)));
    }

    private WorkspaceSnapshot snapshot(Optional<String> taskDocumentDigest) {
        List<WorkspaceSourceObservation> observations = taskDocumentDigest
                .map(digest -> List.of(new WorkspaceSourceObservation(
                        WorkspaceSourceKind.REPOSITORY_DOCUMENT,
                        "CURRENT_TASK.md",
                        "context-reader",
                        CAPTURED_AT.minusSeconds(1),
                        Optional.<Instant>empty(),
                        WorkspaceSourceState.AVAILABLE,
                        Optional.of(digest),
                        Optional.empty())))
                .orElse(List.of());
        return WorkspaceSnapshot.capture(
                Path.of("."),
                CAPTURED_AT,
                new ApprovedTaskRevision(TASK_ID, "CURRENT_TASK.md", "f".repeat(64)),
                observations);
    }

    private static String sha256(String content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(
                    digest.digest(content.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
