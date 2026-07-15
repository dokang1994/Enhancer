package com.enhancer.brain;

import com.enhancer.context.ProjectContext;
import com.enhancer.context.ProjectDocument;
import com.enhancer.run.RunRecord;
import com.enhancer.workspace.ApprovedTaskRevision;
import com.enhancer.workspace.WorkspaceSnapshot;
import com.enhancer.workspace.WorkspaceSourceKind;
import com.enhancer.workspace.WorkspaceSourceObservation;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Read-only Project Brain aggregate over one Workspace snapshot, the repository memory that was
 * loaded for the same approved task, and the provenance of one recorded run. The view derives its
 * content from those inputs; it collects no source, retains no source payload, and grants no Tool
 * authority.
 */
public final class ProjectBrainView {
    public static final int MAX_MEMORY_ENTRIES = 4096;

    private final WorkspaceSnapshot snapshot;
    private final List<RepositoryMemoryEntry> repositoryMemory;
    private final RunProvenance run;

    private ProjectBrainView(
            WorkspaceSnapshot snapshot,
            List<RepositoryMemoryEntry> repositoryMemory,
            RunProvenance run) {
        this.snapshot = snapshot;
        this.repositoryMemory = repositoryMemory;
        this.run = run;
    }

    public static ProjectBrainView compose(
            WorkspaceSnapshot snapshot,
            ProjectContext repositoryMemory,
            RunRecord run) {
        Objects.requireNonNull(snapshot, "snapshot must not be null");
        Objects.requireNonNull(repositoryMemory, "repositoryMemory must not be null");
        Objects.requireNonNull(run, "run must not be null");
        requireSameApprovedTask(snapshot.approvedTaskRevision(), run);

        return new ProjectBrainView(
                snapshot,
                projectMemory(repositoryMemory, snapshot),
                RunProvenance.of(run));
    }

    public String snapshotId() {
        return snapshot.snapshotId();
    }

    public Path projectRoot() {
        return snapshot.projectRoot();
    }

    public Instant capturedAt() {
        return snapshot.capturedAt();
    }

    public ApprovedTaskRevision approvedTaskRevision() {
        return snapshot.approvedTaskRevision();
    }

    public List<WorkspaceSourceObservation> workspaceObservations() {
        return snapshot.observations();
    }

    public List<RepositoryMemoryEntry> repositoryMemory() {
        return repositoryMemory;
    }

    public RunProvenance run() {
        return run;
    }

    private static void requireSameApprovedTask(ApprovedTaskRevision revision, RunRecord run) {
        String runTaskId = run.approvedTask().taskId();
        if (!revision.taskId().equals(runTaskId)) {
            throw new IllegalArgumentException(
                    "run task " + runTaskId
                            + " does not match snapshot task " + revision.taskId());
        }
        String runSourceDocument = run.approvedTask().sourceDocument();
        if (!revision.sourceDocument().equals(runSourceDocument)) {
            throw new IllegalArgumentException(
                    "run source document " + runSourceDocument
                            + " does not match snapshot source document "
                            + revision.sourceDocument());
        }
    }

    private static List<RepositoryMemoryEntry> projectMemory(
            ProjectContext repositoryMemory,
            WorkspaceSnapshot snapshot) {
        List<ProjectDocument> documents = repositoryMemory.documents();
        if (documents.size() > MAX_MEMORY_ENTRIES) {
            throw new IllegalArgumentException(
                    "repositoryMemory must not exceed " + MAX_MEMORY_ENTRIES + " documents");
        }

        Map<String, Optional<String>> observedDigests = observedDocumentDigests(snapshot);
        List<RepositoryMemoryEntry> entries = new ArrayList<>(documents.size());
        Set<String> paths = new HashSet<>();
        for (ProjectDocument document : documents) {
            Objects.requireNonNull(document, "repositoryMemory must not contain null");
            if (!paths.add(document.path())) {
                throw new IllegalArgumentException(
                        "duplicate repository memory document: " + document.path());
            }
            String digest = sha256(document.content());
            entries.add(new RepositoryMemoryEntry(
                    document.path(),
                    document.readOrder(),
                    digest,
                    freshness(observedDigests.get(document.path()), digest)));
        }
        entries.sort(Comparator.comparingInt(RepositoryMemoryEntry::readOrder)
                .thenComparing(RepositoryMemoryEntry::path));
        return List.copyOf(entries);
    }

    private static Map<String, Optional<String>> observedDocumentDigests(
            WorkspaceSnapshot snapshot) {
        Map<String, Optional<String>> digests = new HashMap<>();
        for (WorkspaceSourceObservation observation : snapshot.observations()) {
            if (observation.kind() == WorkspaceSourceKind.REPOSITORY_DOCUMENT) {
                digests.put(observation.sourceId(), observation.contentSha256());
            }
        }
        return digests;
    }

    private static MemoryFreshness freshness(Optional<String> observedDigest, String memoryDigest) {
        if (observedDigest == null) {
            return MemoryFreshness.NOT_OBSERVED;
        }
        return observedDigest.filter(memoryDigest::equals).isPresent()
                ? MemoryFreshness.SNAPSHOT_MATCHED
                : MemoryFreshness.SNAPSHOT_DIVERGED;
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
