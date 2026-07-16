package com.enhancer.brain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.enhancer.loop.AgentLoopStopReason;
import com.enhancer.loop.ApprovedTask;
import com.enhancer.run.PolicyDecision;
import com.enhancer.run.PolicyDecisionStatus;
import com.enhancer.run.ResolvedRunRecord;
import com.enhancer.run.RunRecord;
import com.enhancer.run.StoredRunRecord;
import com.enhancer.tool.ToolRequest;
import com.enhancer.tool.ToolResult;
import com.enhancer.tool.ToolResultStatus;
import com.enhancer.tool.VerificationEvidence;
import com.enhancer.verification.VerificationDecision;
import com.enhancer.workspace.ApprovedTaskRevision;
import com.enhancer.workspace.WorkspaceSnapshot;
import com.enhancer.workspace.WorkspaceSourceKind;
import com.enhancer.workspace.WorkspaceSourceObservation;
import com.enhancer.workspace.WorkspaceSourceState;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import org.junit.jupiter.api.Test;

class RunEvidenceGraphProducerTest {
    private static final Instant CAPTURED_AT = Instant.parse("2026-07-15T08:00:00Z");
    private static final Instant PROJECTED_AT = CAPTURED_AT.plusSeconds(5);
    private static final String TASK_ID = "gate-6-producer-test";
    private static final String RECORD_ID = "00000000-0000-0000-0000-000000000001";
    private static final String RECORD_REFERENCE = "run-record/" + RECORD_ID;
    private static final String RECORD_DIGEST = "e".repeat(64);
    private static final ApprovedTaskRevision TASK_REVISION = new ApprovedTaskRevision(
            TASK_ID,
            "CURRENT_TASK.md",
            "f".repeat(64));

    @Test
    void projectsOnlyWhatTheRunEvidenceProves() {
        WorkspaceSnapshot snapshot = WorkspaceSnapshot.capture(
                Path.of("."),
                CAPTURED_AT,
                TASK_REVISION,
                List.of(
                        observation("ARCHITECTURE.md", WorkspaceSourceState.AVAILABLE,
                                Optional.of("a".repeat(64)), Optional.empty()),
                        observation("ROADMAP.md", WorkspaceSourceState.STALE,
                                Optional.of("b".repeat(64)),
                                Optional.of("changed after observation")),
                        observation("DELETED.md", WorkspaceSourceState.UNAVAILABLE,
                                Optional.empty(), Optional.of("document was deleted")),
                        new WorkspaceSourceObservation(
                                WorkspaceSourceKind.GIT_STATUS,
                                "working-tree",
                                "git-status-adapter",
                                CAPTURED_AT.minusSeconds(1),
                                Optional.empty(),
                                WorkspaceSourceState.AVAILABLE,
                                Optional.of("c".repeat(64)),
                                Optional.empty())));

        ProjectBrainGraph graph = new RunEvidenceGraphProducer().produce(
                snapshot,
                resolvedRecord(TASK_ID),
                PROJECTED_AT);

        assertEquals(snapshot.snapshotId(), graph.sourceSnapshotId());
        assertEquals(PROJECTED_AT, graph.projectedAt());
        assertEquals(5, graph.nodes().size());

        GraphNode task = nodeById(graph, TASK_ID);
        assertEquals(GraphNodeKind.TASK, task.kind());
        assertEquals("CURRENT_TASK.md", task.provenance().sourceRef());
        assertEquals(Optional.of("f".repeat(64)), task.provenance().sourceSha256());
        assertEquals(GraphElementFreshness.CURRENT, task.provenance().freshness());

        GraphNode available = nodeById(graph, "ARCHITECTURE.md");
        assertEquals(GraphNodeKind.ARTIFACT, available.kind());
        assertEquals(GraphElementFreshness.CURRENT, available.provenance().freshness());
        assertEquals(Optional.of("a".repeat(64)), available.provenance().sourceSha256());

        GraphNode stale = nodeById(graph, "ROADMAP.md");
        assertEquals(GraphElementFreshness.STALE, stale.provenance().freshness());
        assertEquals(Optional.of("b".repeat(64)), stale.provenance().sourceSha256());

        GraphNode missing = nodeById(graph, "DELETED.md");
        assertEquals(GraphElementFreshness.SOURCE_MISSING, missing.provenance().freshness());
        assertEquals(Optional.empty(), missing.provenance().sourceSha256());

        GraphNode execution = nodeById(graph, RECORD_REFERENCE);
        assertEquals(GraphNodeKind.EXECUTION, execution.kind());
        assertEquals(RECORD_REFERENCE, execution.provenance().sourceRef());
        assertEquals(Optional.of(RECORD_DIGEST), execution.provenance().sourceSha256());

        assertEquals(1, graph.edges().size());
        GraphEdge recorded = graph.edges().get(0);
        assertEquals(TASK_ID, recorded.fromNodeId());
        assertEquals(GraphEdgeKind.RECORDED_AS, recorded.kind());
        assertEquals(RECORD_REFERENCE, recorded.toNodeId());

        TaskImpact impact = new TaskImpactQuery().query(graph, TASK_ID);
        assertEquals(
                List.of(RECORD_REFERENCE),
                impact.executions().stream().map(GraphNode::nodeId).toList());
        assertEquals(List.of(), impact.decisions());
        assertEquals(List.of(), impact.modifiedArtifacts());
        assertEquals(List.of(), impact.verifyingArtifacts());
        assertTrue(impact.rebuildRequired() == false);
    }

    @Test
    void rejectsARunThatDoesNotMatchTheSnapshotTask() {
        WorkspaceSnapshot snapshot = WorkspaceSnapshot.capture(
                Path.of("."),
                CAPTURED_AT,
                TASK_REVISION,
                List.of(observation("ARCHITECTURE.md", WorkspaceSourceState.AVAILABLE,
                        Optional.of("a".repeat(64)), Optional.empty())));

        assertThrows(
                IllegalArgumentException.class,
                () -> new RunEvidenceGraphProducer().produce(
                        snapshot,
                        resolvedRecord("different-task"),
                        PROJECTED_AT));
    }

    @Test
    void collapsesDocumentAndTargetObservationsIntoOnePreferredArtifact() {
        WorkspaceSourceObservation document = observation(
                "ARCHITECTURE.md",
                WorkspaceSourceState.AVAILABLE,
                Optional.of("a".repeat(64)),
                Optional.empty());
        WorkspaceSourceObservation target = new WorkspaceSourceObservation(
                WorkspaceSourceKind.REPOSITORY_FILE,
                "ARCHITECTURE.md",
                "target-file-reader",
                CAPTURED_AT,
                Optional.empty(),
                WorkspaceSourceState.AVAILABLE,
                Optional.of("b".repeat(64)),
                Optional.empty());
        WorkspaceSnapshot snapshot = WorkspaceSnapshot.capture(
                Path.of("."), CAPTURED_AT, TASK_REVISION, List.of(document, target));

        ProjectBrainGraph graph = new RunEvidenceGraphProducer().produce(
                snapshot, resolvedRecord(TASK_ID), PROJECTED_AT);

        assertEquals(3, graph.nodes().size());
        GraphNode artifact = nodeById(graph, "ARCHITECTURE.md");
        assertEquals(Optional.of("b".repeat(64)), artifact.provenance().sourceSha256());
        assertEquals("ARCHITECTURE.md", artifact.provenance().sourceRef());
    }

    @Test
    void preflightRejectsInvalidGraphInputsWithoutAStoredRun() {
        WorkspaceSnapshot snapshot = WorkspaceSnapshot.capture(
                Path.of("."),
                CAPTURED_AT,
                TASK_REVISION,
                List.of(observation("ARCHITECTURE.md", WorkspaceSourceState.AVAILABLE,
                        Optional.of("a".repeat(64)), Optional.empty())));
        GraphProvenance provenance = new GraphProvenance(
                "DECISION_LOG.md",
                Optional.of("d".repeat(64)),
                GraphElementFreshness.CURRENT);
        GraphNode duplicate = new GraphNode(TASK_ID, GraphNodeKind.DECISION, provenance);

        assertThrows(IllegalArgumentException.class, () ->
                new RunEvidenceGraphProducer().preflight(
                        snapshot, PROJECTED_AT, List.of(duplicate), List.of()));
    }

    @Test
    void rejectsMissingInputs() {
        WorkspaceSnapshot snapshot = WorkspaceSnapshot.capture(
                Path.of("."),
                CAPTURED_AT,
                TASK_REVISION,
                List.of(observation("ARCHITECTURE.md", WorkspaceSourceState.AVAILABLE,
                        Optional.of("a".repeat(64)), Optional.empty())));
        RunEvidenceGraphProducer producer = new RunEvidenceGraphProducer();

        assertThrows(
                NullPointerException.class,
                () -> producer.produce(null, resolvedRecord(TASK_ID), PROJECTED_AT));
        assertThrows(
                NullPointerException.class,
                () -> producer.produce(snapshot, null, PROJECTED_AT));
        assertThrows(
                NullPointerException.class,
                () -> producer.produce(snapshot, resolvedRecord(TASK_ID), null));
    }

    private GraphNode nodeById(ProjectBrainGraph graph, String nodeId) {
        return graph.nodes().stream()
                .filter(node -> node.nodeId().equals(nodeId))
                .findFirst()
                .orElseThrow(() -> new AssertionError("graph does not contain " + nodeId));
    }

    private WorkspaceSourceObservation observation(
            String sourceId,
            WorkspaceSourceState state,
            Optional<String> digest,
            Optional<String> reason) {
        return new WorkspaceSourceObservation(
                WorkspaceSourceKind.REPOSITORY_DOCUMENT,
                sourceId,
                "context-reader",
                CAPTURED_AT.minusSeconds(1),
                state == WorkspaceSourceState.STALE
                        ? Optional.of(CAPTURED_AT.minusSeconds(2))
                        : Optional.empty(),
                state,
                digest,
                reason);
    }

    private ResolvedRunRecord resolvedRecord(String taskId) {
        RunRecord record = new RunRecord(
                "logical-run",
                CAPTURED_AT,
                new ApprovedTask(
                        taskId,
                        "Produce the first run-evidence graph",
                        "Approved by the test owner",
                        Set.of("read-file"),
                        "CURRENT_TASK.md"),
                new ToolRequest(
                        "read-file",
                        "correlation-1",
                        Map.of("path", "ARCHITECTURE.md")),
                new PolicyDecision(
                        PolicyDecisionStatus.ALLOWED,
                        "C:/project",
                        Set.of("read-file"),
                        Set.of(),
                        4096,
                        1000),
                new ToolResult(
                        "read-file",
                        ToolResultStatus.SUCCESS,
                        OptionalInt.empty(),
                        VerificationEvidence.capture(
                                "read succeeded",
                                "content",
                                Optional.empty())),
                Optional.of("a".repeat(64)),
                VerificationDecision.verified("content matched"),
                1,
                AgentLoopStopReason.AWAITING_VERIFICATION,
                AgentLoopStopReason.COMPLETED);
        return new ResolvedRunRecord(
                new StoredRunRecord(
                        RECORD_ID,
                        RECORD_REFERENCE,
                        CAPTURED_AT.plusSeconds(1),
                        2048,
                        RECORD_DIGEST),
                record);
    }
}
