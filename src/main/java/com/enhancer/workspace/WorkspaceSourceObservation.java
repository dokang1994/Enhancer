package com.enhancer.workspace;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

public record WorkspaceSourceObservation(
        WorkspaceSourceKind kind,
        String sourceId,
        String provenance,
        Instant observedAt,
        Optional<Instant> sourceUpdatedAt,
        WorkspaceSourceState state,
        Optional<String> contentSha256,
        Optional<String> reason) {

    public static final int MAX_SOURCE_ID_CHARACTERS = 1024;
    public static final int MAX_PROVENANCE_CHARACTERS = 512;
    public static final int MAX_REASON_CHARACTERS = 512;

    public WorkspaceSourceObservation {
        Objects.requireNonNull(kind, "kind must not be null");
        sourceId = WorkspaceContractSupport.bounded(
                sourceId,
                "sourceId",
                MAX_SOURCE_ID_CHARACTERS);
        provenance = WorkspaceContractSupport.bounded(
                provenance,
                "provenance",
                MAX_PROVENANCE_CHARACTERS);
        Objects.requireNonNull(observedAt, "observedAt must not be null");
        Objects.requireNonNull(sourceUpdatedAt, "sourceUpdatedAt must not be null");
        Objects.requireNonNull(state, "state must not be null");
        Objects.requireNonNull(contentSha256, "contentSha256 must not be null");
        Objects.requireNonNull(reason, "reason must not be null");

        sourceUpdatedAt.ifPresent(updatedAt -> {
            if (updatedAt.isAfter(observedAt)) {
                throw new IllegalArgumentException(
                        "sourceUpdatedAt must not be after observedAt");
            }
        });
        contentSha256 = contentSha256.map(value -> WorkspaceContractSupport.sha256(
                value,
                "contentSha256"));
        reason = reason.map(value -> WorkspaceContractSupport.bounded(
                value,
                "reason",
                MAX_REASON_CHARACTERS));

        switch (state) {
            case AVAILABLE -> {
                if (contentSha256.isEmpty()) {
                    throw new IllegalArgumentException(
                            "Available source requires contentSha256");
                }
                if (reason.isPresent()) {
                    throw new IllegalArgumentException(
                            "Available source cannot carry an unavailability reason");
                }
            }
            case STALE -> {
                if (contentSha256.isEmpty() || sourceUpdatedAt.isEmpty()) {
                    throw new IllegalArgumentException(
                            "Stale source requires contentSha256 and sourceUpdatedAt");
                }
                if (reason.isEmpty()) {
                    throw new IllegalArgumentException("Stale source requires a reason");
                }
            }
            case UNAVAILABLE -> {
                if (contentSha256.isPresent()) {
                    throw new IllegalArgumentException(
                            "Unavailable source cannot carry contentSha256");
                }
                if (reason.isEmpty()) {
                    throw new IllegalArgumentException(
                            "Unavailable source requires a reason");
                }
            }
        }
    }
}
