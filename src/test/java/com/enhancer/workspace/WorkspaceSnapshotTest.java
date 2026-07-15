package com.enhancer.workspace;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class WorkspaceSnapshotTest {
    private static final Pattern SHA_256 = Pattern.compile("[0-9a-f]{64}");
    private static final Instant CAPTURED_AT = Instant.parse("2026-07-15T03:00:00Z");
    private static final ApprovedTaskRevision TASK_REVISION = new ApprovedTaskRevision(
            "gate-6-test",
            "CURRENT_TASK.md",
            "f".repeat(64));

    @Test
    void computesCanonicalIdentityAndSortsAnImmutableObservationSnapshot() {
        WorkspaceSourceObservation repository = observation(
                WorkspaceSourceKind.REPOSITORY_DOCUMENT,
                "ARCHITECTURE.md",
                "a".repeat(64));
        WorkspaceSourceObservation selected = observation(
                WorkspaceSourceKind.SELECTED_FILE,
                "src/main/App.java#10:20",
                "b".repeat(64));

        WorkspaceSnapshot first = WorkspaceSnapshot.capture(
                Path.of("."),
                CAPTURED_AT,
                TASK_REVISION,
                List.of(selected, repository));
        WorkspaceSnapshot reordered = WorkspaceSnapshot.capture(
                Path.of("."),
                CAPTURED_AT,
                TASK_REVISION,
                List.of(repository, selected));

        assertTrue(first.projectRoot().isAbsolute());
        assertEquals(first.projectRoot().normalize(), first.projectRoot());
        assertTrue(SHA_256.matcher(first.snapshotId()).matches());
        assertEquals(first.snapshotId(), reordered.snapshotId());
        assertEquals(List.of(repository, selected), first.observations());
        assertThrows(UnsupportedOperationException.class, () -> first.observations().add(selected));
    }

    @Test
    void identityChangesForEveryIdentityBearingSnapshotInput() {
        WorkspaceSourceObservation source = observation(
                WorkspaceSourceKind.REPOSITORY_FILE,
                "src/main/App.java",
                "a".repeat(64));
        WorkspaceSnapshot baseline = snapshot(TASK_REVISION, CAPTURED_AT, source);

        assertNotEquals(
                baseline.snapshotId(),
                snapshot(TASK_REVISION, CAPTURED_AT.plusMillis(1), source).snapshotId());
        assertNotEquals(
                baseline.snapshotId(),
                snapshot(new ApprovedTaskRevision(
                        TASK_REVISION.taskId(),
                        TASK_REVISION.sourceDocument(),
                        "e".repeat(64)), CAPTURED_AT, source).snapshotId());
        assertNotEquals(
                baseline.snapshotId(),
                snapshot(TASK_REVISION, CAPTURED_AT, observation(
                        WorkspaceSourceKind.REPOSITORY_FILE,
                        "src/main/App.java",
                        "b".repeat(64))).snapshotId());
    }

    @Test
    void identityIncludesProjectTaskAndEveryObservationMetadataField() {
        WorkspaceSourceObservation baselineObservation = observation(
                WorkspaceSourceKind.REPOSITORY_FILE,
                "src/main/App.java",
                "test-adapter",
                CAPTURED_AT.minusSeconds(1),
                Optional.of(CAPTURED_AT.minusSeconds(2)),
                WorkspaceSourceState.AVAILABLE,
                Optional.of("a".repeat(64)),
                Optional.empty());
        WorkspaceSnapshot baseline = WorkspaceSnapshot.capture(
                Path.of("."),
                CAPTURED_AT,
                TASK_REVISION,
                List.of(baselineObservation));

        assertIdentityChanges(baseline, Path.of("different-root"), TASK_REVISION,
                baselineObservation);
        assertIdentityChanges(baseline, Path.of("."), new ApprovedTaskRevision(
                "different-task", TASK_REVISION.sourceDocument(), TASK_REVISION.sourceSha256()),
                baselineObservation);
        assertIdentityChanges(baseline, Path.of("."), new ApprovedTaskRevision(
                TASK_REVISION.taskId(), "TASK.md", TASK_REVISION.sourceSha256()),
                baselineObservation);
        assertIdentityChanges(baseline, Path.of("."), TASK_REVISION, observation(
                WorkspaceSourceKind.ACTIVE_FILE,
                baselineObservation.sourceId(),
                baselineObservation.provenance(),
                baselineObservation.observedAt(),
                baselineObservation.sourceUpdatedAt(),
                baselineObservation.state(),
                baselineObservation.contentSha256(),
                baselineObservation.reason()));
        assertIdentityChanges(baseline, Path.of("."), TASK_REVISION, observation(
                baselineObservation.kind(),
                "src/main/Other.java",
                baselineObservation.provenance(),
                baselineObservation.observedAt(),
                baselineObservation.sourceUpdatedAt(),
                baselineObservation.state(),
                baselineObservation.contentSha256(),
                baselineObservation.reason()));
        assertIdentityChanges(baseline, Path.of("."), TASK_REVISION, observation(
                baselineObservation.kind(),
                baselineObservation.sourceId(),
                "different-adapter",
                baselineObservation.observedAt(),
                baselineObservation.sourceUpdatedAt(),
                baselineObservation.state(),
                baselineObservation.contentSha256(),
                baselineObservation.reason()));
        assertIdentityChanges(baseline, Path.of("."), TASK_REVISION, observation(
                baselineObservation.kind(),
                baselineObservation.sourceId(),
                baselineObservation.provenance(),
                baselineObservation.observedAt().minusNanos(1),
                baselineObservation.sourceUpdatedAt(),
                baselineObservation.state(),
                baselineObservation.contentSha256(),
                baselineObservation.reason()));
        assertIdentityChanges(baseline, Path.of("."), TASK_REVISION, observation(
                baselineObservation.kind(),
                baselineObservation.sourceId(),
                baselineObservation.provenance(),
                baselineObservation.observedAt(),
                Optional.of(CAPTURED_AT.minusSeconds(3)),
                baselineObservation.state(),
                baselineObservation.contentSha256(),
                baselineObservation.reason()));
        assertIdentityChanges(baseline, Path.of("."), TASK_REVISION, observation(
                baselineObservation.kind(),
                baselineObservation.sourceId(),
                baselineObservation.provenance(),
                baselineObservation.observedAt(),
                baselineObservation.sourceUpdatedAt(),
                WorkspaceSourceState.STALE,
                baselineObservation.contentSha256(),
                Optional.of("source is stale")));
    }

    @Test
    void rejectsDuplicateExcessiveOrFutureObservations() {
        WorkspaceSourceObservation source = observation(
                WorkspaceSourceKind.REPOSITORY_FILE,
                "src/main/App.java",
                "a".repeat(64));
        assertThrows(IllegalArgumentException.class, () -> WorkspaceSnapshot.capture(
                Path.of("."),
                CAPTURED_AT,
                TASK_REVISION,
                List.of(source, source)));

        List<WorkspaceSourceObservation> excessive = IntStream
                .rangeClosed(0, WorkspaceSnapshot.MAX_OBSERVATIONS)
                .mapToObj(index -> observation(
                        WorkspaceSourceKind.REPOSITORY_FILE,
                        "file-" + index,
                        "a".repeat(64)))
                .toList();
        assertThrows(IllegalArgumentException.class, () -> WorkspaceSnapshot.capture(
                Path.of("."),
                CAPTURED_AT,
                TASK_REVISION,
                excessive));

        WorkspaceSourceObservation future = new WorkspaceSourceObservation(
                WorkspaceSourceKind.GIT_STATUS,
                "working-tree",
                "git-status-adapter",
                CAPTURED_AT.plusNanos(1),
                Optional.empty(),
                WorkspaceSourceState.AVAILABLE,
                Optional.of("a".repeat(64)),
                Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> WorkspaceSnapshot.capture(
                Path.of("."),
                CAPTURED_AT,
                TASK_REVISION,
                List.of(future)));
    }

    @Test
    void snapshotsCallerCollectionsInsteadOfRetainingMutableState() {
        List<WorkspaceSourceObservation> mutable = new ArrayList<>();
        mutable.add(observation(
                WorkspaceSourceKind.REPOSITORY_FILE,
                "src/main/App.java",
                "a".repeat(64)));
        WorkspaceSnapshot snapshot = WorkspaceSnapshot.capture(
                Path.of("."),
                CAPTURED_AT,
                TASK_REVISION,
                mutable);

        mutable.clear();

        assertEquals(1, snapshot.observations().size());
    }

    private WorkspaceSnapshot snapshot(
            ApprovedTaskRevision taskRevision,
            Instant capturedAt,
            WorkspaceSourceObservation observation) {
        return WorkspaceSnapshot.capture(
                Path.of("."),
                capturedAt,
                taskRevision,
                List.of(observation));
    }

    private void assertIdentityChanges(
            WorkspaceSnapshot baseline,
            Path projectRoot,
            ApprovedTaskRevision taskRevision,
            WorkspaceSourceObservation observation) {
        WorkspaceSnapshot changed = WorkspaceSnapshot.capture(
                projectRoot,
                CAPTURED_AT,
                taskRevision,
                List.of(observation));
        assertNotEquals(baseline.snapshotId(), changed.snapshotId());
    }

    private WorkspaceSourceObservation observation(
            WorkspaceSourceKind kind,
            String sourceId,
            String digest) {
        return observation(
                kind,
                sourceId,
                "test-adapter",
                CAPTURED_AT.minusSeconds(1),
                Optional.of(CAPTURED_AT.minusSeconds(2)),
                WorkspaceSourceState.AVAILABLE,
                Optional.of(digest),
                Optional.empty());
    }

    private WorkspaceSourceObservation observation(
            WorkspaceSourceKind kind,
            String sourceId,
            String provenance,
            Instant observedAt,
            Optional<Instant> sourceUpdatedAt,
            WorkspaceSourceState state,
            Optional<String> contentSha256,
            Optional<String> reason) {
        return new WorkspaceSourceObservation(
                kind,
                sourceId,
                provenance,
                observedAt,
                sourceUpdatedAt,
                state,
                contentSha256,
                reason);
    }
}
