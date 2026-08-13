package com.enhancer.maintenance.installation;

import java.util.Objects;
import java.util.UUID;

/** Successful atomic same-filesystem publication evidence for one exact pair. */
public record AtomicPublicationEvidence(
        UUID transactionId,
        InstallationArtifact staged,
        InstallationArtifact target,
        PublicationMode mode,
        String filesystemIdentity,
        boolean atomic) {

    public AtomicPublicationEvidence {
        transactionId = Objects.requireNonNull(transactionId, "transactionId must not be null");
        staged = Objects.requireNonNull(staged, "staged must not be null");
        target = Objects.requireNonNull(target, "target must not be null");
        mode = Objects.requireNonNull(mode, "mode must not be null");
        filesystemIdentity = InstallationPrincipal.boundedText(
                filesystemIdentity, "filesystemIdentity");
        if (!atomic) {
            throw new IllegalArgumentException("publication evidence must be atomic");
        }
        if (target.kind() == InstallationArtifactKind.CONTENT_ADDRESSED_POLICY
                && mode != PublicationMode.CREATE_EXCLUSIVE) {
            throw new IllegalArgumentException("policy publication must be create-exclusive");
        }
    }
}
