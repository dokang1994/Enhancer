package com.enhancer.maintenance.installation;

import java.util.Objects;
import java.util.UUID;

/** Successful required file and parent/volume durability barriers for one artifact. */
public record DurabilityEvidence(
        UUID transactionId,
        InstallationArtifact artifact,
        String mechanism,
        boolean fileBarrierComplete,
        boolean parentOrVolumeBarrierComplete) {

    public DurabilityEvidence {
        transactionId = Objects.requireNonNull(transactionId, "transactionId must not be null");
        artifact = Objects.requireNonNull(artifact, "artifact must not be null");
        mechanism = InstallationPrincipal.boundedText(mechanism, "mechanism");
        if (!fileBarrierComplete || !parentOrVolumeBarrierComplete) {
            throw new IllegalArgumentException("required durability barriers must complete");
        }
    }
}
