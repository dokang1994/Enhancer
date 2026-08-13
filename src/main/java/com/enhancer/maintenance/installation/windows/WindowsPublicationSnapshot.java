package com.enhancer.maintenance.installation.windows;

import com.enhancer.maintenance.installation.InstallationArtifact;
import com.enhancer.maintenance.installation.PublicationMode;
import java.util.Objects;
import java.util.UUID;

public record WindowsPublicationSnapshot(
        UUID transactionId,
        InstallationArtifact staged,
        InstallationArtifact target,
        PublicationMode mode,
        WindowsFileIdentity targetIdentity,
        WindowsVolumeIdentity volume,
        boolean sameVolume,
        boolean atomic) {
    public WindowsPublicationSnapshot {
        transactionId = Objects.requireNonNull(transactionId, "transactionId must not be null");
        staged = Objects.requireNonNull(staged, "staged must not be null");
        target = Objects.requireNonNull(target, "target must not be null");
        mode = Objects.requireNonNull(mode, "mode must not be null");
        targetIdentity = Objects.requireNonNull(targetIdentity, "targetIdentity must not be null");
        volume = Objects.requireNonNull(volume, "volume must not be null");
    }
}
