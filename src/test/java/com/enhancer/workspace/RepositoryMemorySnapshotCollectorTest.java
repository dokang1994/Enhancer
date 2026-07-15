package com.enhancer.workspace;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.enhancer.context.ProjectContext;
import com.enhancer.context.ProjectDocument;
import com.enhancer.loop.ApprovedTask;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class RepositoryMemorySnapshotCollectorTest {
    private static final Instant CAPTURED_AT = Instant.parse("2026-07-15T05:00:00Z");
    private static final String TASK_CONTENT = "# Current Task\n\n## Task ID\n\ncollector-test\n";
    private static final ApprovedTask APPROVED_TASK = new ApprovedTask(
            "collector-test",
            "Collect repository memory",
            "Approved by the test owner",
            Set.of("read-file"),
            "CURRENT_TASK.md");

    @Test
    void derivesObservationsAndTaskRevisionFromLoadedMemoryOnly() {
        ProjectContext memory = new ProjectContext(List.of(
                new ProjectDocument("CONSTITUTION.md", 1, "# Constitution"),
                new ProjectDocument("CURRENT_TASK.md", 2, TASK_CONTENT)));

        WorkspaceSnapshot snapshot = new RepositoryMemorySnapshotCollector().collect(
                Path.of("."),
                CAPTURED_AT,
                APPROVED_TASK,
                memory);

        assertEquals(CAPTURED_AT, snapshot.capturedAt());
        assertEquals("collector-test", snapshot.approvedTaskRevision().taskId());
        assertEquals("CURRENT_TASK.md", snapshot.approvedTaskRevision().sourceDocument());
        assertEquals(sha256(TASK_CONTENT), snapshot.approvedTaskRevision().sourceSha256());

        assertEquals(2, snapshot.observations().size());
        for (WorkspaceSourceObservation observation : snapshot.observations()) {
            assertEquals(WorkspaceSourceKind.REPOSITORY_DOCUMENT, observation.kind());
            assertEquals("context-reader", observation.provenance());
            assertEquals(CAPTURED_AT, observation.observedAt());
            assertEquals(Optional.empty(), observation.sourceUpdatedAt());
            assertEquals(WorkspaceSourceState.AVAILABLE, observation.state());
            assertEquals(Optional.empty(), observation.reason());
        }
        assertEquals(
                List.of("CONSTITUTION.md", "CURRENT_TASK.md"),
                snapshot.observations().stream()
                        .map(WorkspaceSourceObservation::sourceId)
                        .toList());
        assertEquals(
                Optional.of(sha256("# Constitution")),
                snapshot.observations().get(0).contentSha256());
        assertTrue(snapshot.snapshotId().matches("[0-9a-f]{64}"));
    }

    @Test
    void rejectsMemoryWithoutTheApprovedTaskSourceDocument() {
        ProjectContext memory = new ProjectContext(List.of(
                new ProjectDocument("CONSTITUTION.md", 1, "# Constitution")));

        assertThrows(
                IllegalArgumentException.class,
                () -> new RepositoryMemorySnapshotCollector().collect(
                        Path.of("."),
                        CAPTURED_AT,
                        APPROVED_TASK,
                        memory));
    }

    @Test
    void rejectsMissingInputs() {
        ProjectContext memory = new ProjectContext(List.of(
                new ProjectDocument("CURRENT_TASK.md", 1, TASK_CONTENT)));
        RepositoryMemorySnapshotCollector collector = new RepositoryMemorySnapshotCollector();

        assertThrows(
                NullPointerException.class,
                () -> collector.collect(null, CAPTURED_AT, APPROVED_TASK, memory));
        assertThrows(
                NullPointerException.class,
                () -> collector.collect(Path.of("."), null, APPROVED_TASK, memory));
        assertThrows(
                NullPointerException.class,
                () -> collector.collect(Path.of("."), CAPTURED_AT, null, memory));
        assertThrows(
                NullPointerException.class,
                () -> collector.collect(Path.of("."), CAPTURED_AT, APPROVED_TASK, null));
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
