package com.enhancer.runtime;

import java.util.Objects;

/** Result of exact event resolution followed by deterministic point acknowledgement. */
public record RuntimeEventPointAcknowledgement(
        RuntimeEventPointAcknowledgementStatus status,
        String acknowledgedFile,
        RuntimeEventPointResolution resolution) {

    public RuntimeEventPointAcknowledgement {
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(acknowledgedFile, "acknowledgedFile must not be null");
        Objects.requireNonNull(resolution, "resolution must not be null");
        if (!acknowledgedFile.matches(
                "[0-9a-f]{64}\\Q"
                        + FileSystemRuntimeEventPublisher.ACKNOWLEDGED_FILE_SUFFIX
                        + "\\E")) {
            throw new IllegalArgumentException(
                    "acknowledgedFile must be one canonical acknowledged runtime-event point");
        }
        if (!FileSystemRuntimeEventPublisher.acknowledgedPointName(
                        resolution.reference())
                .equals(acknowledgedFile)) {
            throw new IllegalArgumentException(
                    "acknowledgedFile does not match the resolved runtime event");
        }
    }
}
