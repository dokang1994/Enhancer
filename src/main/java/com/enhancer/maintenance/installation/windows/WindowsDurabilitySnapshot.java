package com.enhancer.maintenance.installation.windows;

import com.enhancer.maintenance.installation.InstallationArtifact;
import java.util.Objects;
import java.util.UUID;

public record WindowsDurabilitySnapshot(
        UUID transactionId,
        InstallationArtifact artifact,
        WindowsFileIdentity identity,
        boolean fileBarrierComplete,
        boolean parentOrVolumeBarrierComplete) {
    public WindowsDurabilitySnapshot {
        transactionId = Objects.requireNonNull(transactionId, "transactionId must not be null");
        artifact = Objects.requireNonNull(artifact, "artifact must not be null");
        identity = Objects.requireNonNull(identity, "identity must not be null");
    }
}
