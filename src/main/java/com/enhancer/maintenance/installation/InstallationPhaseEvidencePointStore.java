package com.enhancer.maintenance.installation;

import java.util.Objects;
import java.util.Optional;

/**
 * Immutable semantic-evidence point store. This port stores no evidence body and does
 * not independently revalidate a host observation.
 */
public interface InstallationPhaseEvidencePointStore {
    /** Creates one exact point; equal replay is mutation-free and changed reuse fails. */
    Mutation create(InstallationPhaseEvidence evidence)
            throws InstallationPhaseEvidenceStoreException;

    /** Reads only the requested point without listing, scanning, creating, or repair. */
    Optional<InstallationPhaseEvidence> read(InstallationPhaseEvidencePoint point)
            throws InstallationPhaseEvidenceStoreException;

    /** Exact immutable-create outcome. */
    record Mutation(
            InstallationPhaseEvidence evidence,
            MutationDisposition disposition) {
        public Mutation {
            evidence = Objects.requireNonNull(evidence, "evidence must not be null");
            disposition = Objects.requireNonNull(
                    disposition, "disposition must not be null");
        }
    }

    enum MutationDisposition {
        CREATED,
        EXACT_REPLAY
    }
}
