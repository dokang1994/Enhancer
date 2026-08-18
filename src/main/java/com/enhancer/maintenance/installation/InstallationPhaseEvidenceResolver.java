package com.enhancer.maintenance.installation;

import java.util.Optional;

/**
 * Read-only exact-point port. Implementations must revalidate the resolved evidence and
 * may neither scan for alternatives nor create or mutate evidence.
 */
public interface InstallationPhaseEvidenceResolver {
    Optional<InstallationPhaseEvidence> resolveAndRevalidate(
            InstallationPhaseEvidencePoint point)
            throws InstallationPhaseEvidenceResolutionException;
}
