package com.enhancer.workspace;

import com.enhancer.context.ProjectContext;
import com.enhancer.context.ProjectDocument;
import com.enhancer.loop.ApprovedTask;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Read-only Workspace source adapter over repository memory that the Context Reader already
 * loaded. It derives metadata and digests from the given {@link ProjectContext}; it reads no
 * files, retains no document content, and grants no Tool authority.
 */
public final class RepositoryMemorySnapshotCollector {
    private static final String PROVENANCE = "context-reader";

    public WorkspaceSnapshot collect(
            Path projectRoot,
            Instant capturedAt,
            ApprovedTask approvedTask,
            ProjectContext repositoryMemory) {
        return collect(projectRoot, capturedAt, approvedTask, repositoryMemory, List.of());
    }

    public WorkspaceSnapshot collect(
            Path projectRoot,
            Instant capturedAt,
            ApprovedTask approvedTask,
            ProjectContext repositoryMemory,
            List<WorkspaceSourceObservation> additionalObservations) {
        Objects.requireNonNull(projectRoot, "projectRoot must not be null");
        Objects.requireNonNull(capturedAt, "capturedAt must not be null");
        Objects.requireNonNull(approvedTask, "approvedTask must not be null");
        Objects.requireNonNull(repositoryMemory, "repositoryMemory must not be null");
        Objects.requireNonNull(
                additionalObservations,
                "additionalObservations must not be null");

        List<WorkspaceSourceObservation> observations =
                new ArrayList<>(repositoryMemory.documents().size());
        String sourceDocumentSha256 = null;
        for (ProjectDocument document : repositoryMemory.documents()) {
            String digest = sha256(document.content());
            if (document.path().equals(approvedTask.sourceDocument())) {
                sourceDocumentSha256 = digest;
            }
            observations.add(new WorkspaceSourceObservation(
                    WorkspaceSourceKind.REPOSITORY_DOCUMENT,
                    document.path(),
                    PROVENANCE,
                    capturedAt,
                    Optional.empty(),
                    WorkspaceSourceState.AVAILABLE,
                    Optional.of(digest),
                    Optional.empty()));
        }
        if (sourceDocumentSha256 == null) {
            throw new IllegalArgumentException(
                    "repositoryMemory does not contain the approved task source document: "
                            + approvedTask.sourceDocument());
        }
        observations.addAll(additionalObservations);

        return WorkspaceSnapshot.capture(
                projectRoot,
                capturedAt,
                new ApprovedTaskRevision(
                        approvedTask.taskId(),
                        approvedTask.sourceDocument(),
                        sourceDocumentSha256),
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
