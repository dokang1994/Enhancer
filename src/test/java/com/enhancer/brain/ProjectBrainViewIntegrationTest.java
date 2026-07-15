package com.enhancer.brain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.enhancer.context.ProjectContext;
import com.enhancer.context.ProjectDocument;
import com.enhancer.loop.AgentLoopStopReason;
import com.enhancer.loop.ApprovedTask;
import com.enhancer.run.PolicyDecision;
import com.enhancer.run.PolicyDecisionStatus;
import com.enhancer.run.RunRecord;
import com.enhancer.tool.ToolRequest;
import com.enhancer.tool.ToolResult;
import com.enhancer.tool.ToolResultStatus;
import com.enhancer.tool.VerificationEvidence;
import com.enhancer.verification.VerificationDecision;
import com.enhancer.verification.VerificationStatus;
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
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ProjectBrainViewIntegrationTest {
    private static final Instant CAPTURED_AT = Instant.parse("2026-07-15T03:00:00Z");
    private static final Instant OBSERVED_AT = CAPTURED_AT.minusSeconds(1);
    private static final String TASK_ID = "gate-6-project-brain-view-integration";
    private static final String TASK_SOURCE = "CURRENT_TASK.md";
    private static final ApprovedTaskRevision TASK_REVISION = new ApprovedTaskRevision(
            TASK_ID,
            TASK_SOURCE,
            "f".repeat(64));
    private static final String ARCHITECTURE_CONTENT = "# Architecture";
    private static final String ROADMAP_CONTENT = "# Roadmap";

    @Test
    void composesSnapshotMemoryAndRunProvenanceWithoutSourceContent() {
        ProjectContext memory = new ProjectContext(List.of(
                new ProjectDocument("ARCHITECTURE.md", 1, ARCHITECTURE_CONTENT),
                new ProjectDocument("ROADMAP.md", 2, ROADMAP_CONTENT)));
        WorkspaceSnapshot snapshot = snapshot(
                documentObservation("ARCHITECTURE.md", sha256(ARCHITECTURE_CONTENT)),
                documentObservation("ROADMAP.md", sha256(ROADMAP_CONTENT)));

        ProjectBrainView view = ProjectBrainView.compose(snapshot, memory, runRecord());

        assertEquals(snapshot.snapshotId(), view.snapshotId());
        assertEquals(TASK_REVISION, view.approvedTaskRevision());
        assertEquals(snapshot.projectRoot(), view.projectRoot());
        assertEquals(snapshot.capturedAt(), view.capturedAt());
        assertEquals(snapshot.observations(), view.workspaceObservations());

        assertEquals(
                List.of("ARCHITECTURE.md", "ROADMAP.md"),
                view.repositoryMemory().stream().map(RepositoryMemoryEntry::path).toList());
        assertEquals(sha256(ARCHITECTURE_CONTENT), view.repositoryMemory().get(0).sourceSha256());
        assertEquals(1, view.repositoryMemory().get(0).readOrder());

        RunProvenance run = view.run();
        assertEquals("logical-run", run.logicalRunId());
        assertEquals(CAPTURED_AT, run.recordedAt());
        assertEquals(TASK_ID, run.taskId());
        assertEquals(VerificationStatus.VERIFIED, run.verificationStatus());
    }

    @Test
    void derivesMemoryFreshnessByComparingDigestsAgainstObservedDocuments() {
        ProjectContext memory = new ProjectContext(List.of(
                new ProjectDocument("ARCHITECTURE.md", 1, ARCHITECTURE_CONTENT),
                new ProjectDocument("ROADMAP.md", 2, ROADMAP_CONTENT),
                new ProjectDocument("AGENTS.md", 3, "# Agents")));
        WorkspaceSnapshot snapshot = snapshot(
                documentObservation("ARCHITECTURE.md", sha256(ARCHITECTURE_CONTENT)),
                documentObservation("ROADMAP.md", sha256("# Roadmap of a different revision")));

        ProjectBrainView view = ProjectBrainView.compose(snapshot, memory, runRecord());

        assertEquals(
                List.of(
                        MemoryFreshness.SNAPSHOT_MATCHED,
                        MemoryFreshness.SNAPSHOT_DIVERGED,
                        MemoryFreshness.NOT_OBSERVED),
                view.repositoryMemory().stream().map(RepositoryMemoryEntry::freshness).toList());
    }

    @Test
    void keepsWorkspaceFreshnessStatesExplicitInsteadOfCollapsingThem() {
        WorkspaceSnapshot snapshot = snapshot(
                documentObservation("ARCHITECTURE.md", sha256(ARCHITECTURE_CONTENT)),
                new WorkspaceSourceObservation(
                        WorkspaceSourceKind.GIT_STATUS,
                        "working-tree",
                        "git-status-adapter",
                        OBSERVED_AT,
                        Optional.of(OBSERVED_AT.minusSeconds(60)),
                        WorkspaceSourceState.STALE,
                        Optional.of("b".repeat(64)),
                        Optional.of("working tree changed after observation")),
                new WorkspaceSourceObservation(
                        WorkspaceSourceKind.DIAGNOSTIC,
                        "compiler",
                        "diagnostic-adapter",
                        OBSERVED_AT,
                        Optional.empty(),
                        WorkspaceSourceState.UNAVAILABLE,
                        Optional.empty(),
                        Optional.of("diagnostic provider is not running")));

        ProjectBrainView view = ProjectBrainView.compose(
                snapshot,
                new ProjectContext(List.of(
                        new ProjectDocument("ARCHITECTURE.md", 1, ARCHITECTURE_CONTENT))),
                runRecord());

        assertEquals(
                List.of(
                        WorkspaceSourceState.UNAVAILABLE,
                        WorkspaceSourceState.STALE,
                        WorkspaceSourceState.AVAILABLE),
                view.workspaceObservations().stream()
                        .map(WorkspaceSourceObservation::state)
                        .toList());
    }

    @Test
    void rejectsARunThatDoesNotMatchTheApprovedTaskRevisionOfTheSnapshot() {
        WorkspaceSnapshot snapshot = snapshot(
                documentObservation("ARCHITECTURE.md", sha256(ARCHITECTURE_CONTENT)));
        ProjectContext memory = new ProjectContext(List.of(
                new ProjectDocument("ARCHITECTURE.md", 1, ARCHITECTURE_CONTENT)));

        assertThrows(
                IllegalArgumentException.class,
                () -> ProjectBrainView.compose(
                        snapshot,
                        memory,
                        runRecord(approvedTask("gate-5-first-operational-cli", TASK_SOURCE))));
        assertThrows(
                IllegalArgumentException.class,
                () -> ProjectBrainView.compose(
                        snapshot,
                        memory,
                        runRecord(approvedTask(TASK_ID, "PROPOSAL.md"))));
    }

    @Test
    void rejectsMissingInputsAndPublishesImmutableCollections() {
        WorkspaceSnapshot snapshot = snapshot(
                documentObservation("ARCHITECTURE.md", sha256(ARCHITECTURE_CONTENT)));
        ProjectContext memory = new ProjectContext(List.of(
                new ProjectDocument("ARCHITECTURE.md", 1, ARCHITECTURE_CONTENT)));

        assertThrows(
                NullPointerException.class,
                () -> ProjectBrainView.compose(null, memory, runRecord()));
        assertThrows(
                NullPointerException.class,
                () -> ProjectBrainView.compose(snapshot, null, runRecord()));
        assertThrows(
                NullPointerException.class,
                () -> ProjectBrainView.compose(snapshot, memory, null));

        ProjectBrainView view = ProjectBrainView.compose(snapshot, memory, runRecord());
        assertThrows(
                UnsupportedOperationException.class,
                () -> view.repositoryMemory().clear());
        assertThrows(
                UnsupportedOperationException.class,
                () -> view.workspaceObservations().clear());
        assertTrue(view.projectRoot().isAbsolute());
    }

    private WorkspaceSnapshot snapshot(WorkspaceSourceObservation... observations) {
        return WorkspaceSnapshot.capture(
                Path.of("."),
                CAPTURED_AT,
                TASK_REVISION,
                List.of(observations));
    }

    private WorkspaceSourceObservation documentObservation(String path, String digest) {
        return new WorkspaceSourceObservation(
                WorkspaceSourceKind.REPOSITORY_DOCUMENT,
                path,
                "context-reader",
                OBSERVED_AT,
                Optional.of(OBSERVED_AT.minusSeconds(2)),
                WorkspaceSourceState.AVAILABLE,
                Optional.of(digest),
                Optional.empty());
    }

    private RunRecord runRecord() {
        return runRecord(approvedTask(TASK_ID, TASK_SOURCE));
    }

    private RunRecord runRecord(ApprovedTask approvedTask) {
        return new RunRecord(
                "logical-run",
                CAPTURED_AT,
                approvedTask,
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
                                ARCHITECTURE_CONTENT,
                                Optional.empty())),
                Optional.of(sha256(ARCHITECTURE_CONTENT)),
                VerificationDecision.verified("content matched"),
                1,
                AgentLoopStopReason.AWAITING_VERIFICATION,
                AgentLoopStopReason.COMPLETED);
    }

    private ApprovedTask approvedTask(String taskId, String sourceDocument) {
        return new ApprovedTask(
                taskId,
                "Integrate the Workspace snapshot into a read-only Project Brain view",
                "Approved by the user on 2026-07-15",
                Set.of("read-file"),
                sourceDocument);
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
