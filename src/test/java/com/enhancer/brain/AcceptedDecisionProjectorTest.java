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

class AcceptedDecisionProjectorTest {
    private static final Instant CAPTURED_AT = Instant.parse("2026-07-15T09:00:00Z");
    private static final ApprovedTaskRevision TASK_REVISION = new ApprovedTaskRevision(
            "gate-6-decision-test",
            "CURRENT_TASK.md",
            "f".repeat(64));
    private static final String DECISION_LOG = "# Decision Log\n\n"
            + "## Accepted Decisions\n\n"
            + "### 2026-07-14: Adopt The First Rule\n\n"
            + "Status: Accepted Decision\n\n"
            + "Decision:\n\n- adopt.\n\n"
            + "### 2026-07-15: Explore A Candidate\n\n"
            + "Status: Proposal\n\n"
            + "Context:\n\n- exploring.\n\n"
            + "### 2026-07-15: Adopt The Second Rule\n\n"
            + "Status: Accepted Decision\n\n"
            + "Decision:\n\n- adopt again.\n";

    @Test
    void projectsOnlyAcceptedDecisionsInDocumentOrderWithMatchedFreshness() {
        ProjectContext memory = memory(DECISION_LOG);
        WorkspaceSnapshot snapshot = snapshot(Optional.of(sha256(DECISION_LOG)));

        List<GraphNode> nodes = new AcceptedDecisionProjector().project(snapshot, memory);

        assertEquals(
                List.of(
                        "2026-07-14: Adopt The First Rule",
                        "2026-07-15: Adopt The Second Rule"),
                nodes.stream().map(GraphNode::nodeId).toList());
        for (GraphNode node : nodes) {
            assertEquals(GraphNodeKind.DECISION, node.kind());
            assertEquals("DECISION_LOG.md", node.provenance().sourceRef());
            assertEquals(Optional.of(sha256(DECISION_LOG)), node.provenance().sourceSha256());
            assertEquals(GraphElementFreshness.CURRENT, node.provenance().freshness());
        }
    }

    @Test
    void marksNodesStaleWhenTheSnapshotDivergedOrNeverObservedTheDecisionLog() {
        ProjectContext memory = memory(DECISION_LOG);

        List<GraphNode> diverged = new AcceptedDecisionProjector().project(
                snapshot(Optional.of("b".repeat(64))),
                memory);
        assertEquals(GraphElementFreshness.STALE, diverged.get(0).provenance().freshness());

        List<GraphNode> unobserved = new AcceptedDecisionProjector().project(
                snapshot(Optional.empty()),
                memory);
        assertEquals(GraphElementFreshness.STALE, unobserved.get(0).provenance().freshness());
        assertEquals(
                Optional.of(sha256(DECISION_LOG)),
                unobserved.get(0).provenance().sourceSha256());
    }

    @Test
    void rejectsMissingDocumentDuplicateHeadingsAndNullInputs() {
        AcceptedDecisionProjector projector = new AcceptedDecisionProjector();
        WorkspaceSnapshot snapshot = snapshot(Optional.of("a".repeat(64)));

        ProjectContext withoutLog = new ProjectContext(List.of(
                new ProjectDocument("CONSTITUTION.md", 1, "# Constitution")));
        assertThrows(
                IllegalArgumentException.class,
                () -> projector.project(snapshot, withoutLog));

        String duplicated = "### 2026-07-15: Same Title\n\nStatus: Accepted Decision\n\n"
                + "### 2026-07-15: Same Title\n\nStatus: Accepted Decision\n";
        assertThrows(
                IllegalArgumentException.class,
                () -> projector.project(snapshot, memory(duplicated)));

        assertThrows(
                NullPointerException.class,
                () -> projector.project(null, memory(DECISION_LOG)));
        assertThrows(
                NullPointerException.class,
                () -> projector.project(snapshot, null));
    }

    @Test
    void projectedNodesComposeIntoAProjectBrainGraph() {
        ProjectContext memory = memory(DECISION_LOG);
        WorkspaceSnapshot snapshot = snapshot(Optional.of(sha256(DECISION_LOG)));
        List<GraphNode> decisions = new AcceptedDecisionProjector().project(snapshot, memory);

        ProjectBrainGraph graph = ProjectBrainGraph.project(
                snapshot.snapshotId(),
                CAPTURED_AT.plusSeconds(1),
                decisions,
                List.of());

        assertEquals(2, graph.nodes().size());
        assertEquals(List.of(), graph.edges());
    }

    private ProjectContext memory(String decisionLogContent) {
        return new ProjectContext(List.of(
                new ProjectDocument("DECISION_LOG.md", 1, decisionLogContent)));
    }

    private WorkspaceSnapshot snapshot(Optional<String> decisionLogDigest) {
        List<WorkspaceSourceObservation> observations = decisionLogDigest
                .map(digest -> List.of(new WorkspaceSourceObservation(
                        WorkspaceSourceKind.REPOSITORY_DOCUMENT,
                        "DECISION_LOG.md",
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
                TASK_REVISION,
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
