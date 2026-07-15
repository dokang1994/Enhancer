package com.enhancer.workspace;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public final class WorkspaceSnapshot {
    public static final int MAX_OBSERVATIONS = 4096;
    public static final int MAX_PROJECT_ROOT_CHARACTERS = 4096;

    private static final String IDENTITY_VERSION = "workspace-snapshot-v1";
    private static final Comparator<WorkspaceSourceObservation> OBSERVATION_ORDER =
            Comparator.comparing((WorkspaceSourceObservation value) -> value.kind().name())
                    .thenComparing(WorkspaceSourceObservation::sourceId);

    private final String snapshotId;
    private final Path projectRoot;
    private final Instant capturedAt;
    private final ApprovedTaskRevision approvedTaskRevision;
    private final List<WorkspaceSourceObservation> observations;

    private WorkspaceSnapshot(
            Path projectRoot,
            Instant capturedAt,
            ApprovedTaskRevision approvedTaskRevision,
            List<WorkspaceSourceObservation> observations) {
        this.projectRoot = projectRoot;
        this.capturedAt = capturedAt;
        this.approvedTaskRevision = approvedTaskRevision;
        this.observations = observations;
        this.snapshotId = computeIdentity(
                projectRoot,
                capturedAt,
                approvedTaskRevision,
                observations);
    }

    public static WorkspaceSnapshot capture(
            Path projectRoot,
            Instant capturedAt,
            ApprovedTaskRevision approvedTaskRevision,
            Collection<WorkspaceSourceObservation> observations) {
        Objects.requireNonNull(projectRoot, "projectRoot must not be null");
        Objects.requireNonNull(capturedAt, "capturedAt must not be null");
        Objects.requireNonNull(
                approvedTaskRevision,
                "approvedTaskRevision must not be null");
        Objects.requireNonNull(observations, "observations must not be null");
        if (observations.size() > MAX_OBSERVATIONS) {
            throw new IllegalArgumentException(
                    "observations must not exceed " + MAX_OBSERVATIONS + " items");
        }

        Path normalizedRoot = projectRoot.toAbsolutePath().normalize();
        WorkspaceContractSupport.bounded(
                normalizedRoot.toString(),
                "projectRoot",
                MAX_PROJECT_ROOT_CHARACTERS);

        List<WorkspaceSourceObservation> ordered = new ArrayList<>(observations.size());
        Set<SourceIdentity> identities = new HashSet<>();
        for (WorkspaceSourceObservation observation : observations) {
            Objects.requireNonNull(observation, "observations must not contain null");
            if (observation.observedAt().isAfter(capturedAt)) {
                throw new IllegalArgumentException(
                        "observation observedAt must not be after capturedAt");
            }
            SourceIdentity identity = new SourceIdentity(
                    observation.kind(),
                    observation.sourceId());
            if (!identities.add(identity)) {
                throw new IllegalArgumentException(
                        "duplicate Workspace source: "
                                + observation.kind() + "/" + observation.sourceId());
            }
            ordered.add(observation);
        }
        ordered.sort(OBSERVATION_ORDER);

        return new WorkspaceSnapshot(
                normalizedRoot,
                capturedAt,
                approvedTaskRevision,
                List.copyOf(ordered));
    }

    public String snapshotId() {
        return snapshotId;
    }

    public Path projectRoot() {
        return projectRoot;
    }

    public Instant capturedAt() {
        return capturedAt;
    }

    public ApprovedTaskRevision approvedTaskRevision() {
        return approvedTaskRevision;
    }

    public List<WorkspaceSourceObservation> observations() {
        return observations;
    }

    private static String computeIdentity(
            Path projectRoot,
            Instant capturedAt,
            ApprovedTaskRevision taskRevision,
            List<WorkspaceSourceObservation> observations) {
        MessageDigest digest = sha256();
        update(digest, IDENTITY_VERSION);
        update(digest, projectRoot.toString());
        update(digest, capturedAt.toString());
        update(digest, taskRevision.taskId());
        update(digest, taskRevision.sourceDocument());
        update(digest, taskRevision.sourceSha256());
        update(digest, Integer.toString(observations.size()));
        for (WorkspaceSourceObservation observation : observations) {
            update(digest, observation.kind().name());
            update(digest, observation.sourceId());
            update(digest, observation.provenance());
            update(digest, observation.observedAt().toString());
            updateOptional(digest, observation.sourceUpdatedAt().map(Instant::toString));
            update(digest, observation.state().name());
            updateOptional(digest, observation.contentSha256());
            updateOptional(digest, observation.reason());
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    private static MessageDigest sha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private static void updateOptional(MessageDigest digest, Optional<String> value) {
        update(digest, value.isPresent() ? "present" : "absent");
        value.ifPresent(item -> update(digest, item));
    }

    private static void update(MessageDigest digest, String value) {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        digest.update((byte) (bytes.length >>> 24));
        digest.update((byte) (bytes.length >>> 16));
        digest.update((byte) (bytes.length >>> 8));
        digest.update((byte) bytes.length);
        digest.update(bytes);
    }

    private record SourceIdentity(
            WorkspaceSourceKind kind,
            String sourceId) {
    }
}
