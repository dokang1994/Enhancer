package com.enhancer.maintenance.installation;

/**
 * Privileged enforcement port for an already-authorized plan. It does not authorize,
 * orchestrate, persist, activate, or report overall installation success.
 */
public interface InstallationPermissionAdapter {
    InstallationEnvironmentEvidence resolveAndVerify(
            CancellationTrustInstallationPlan plan) throws InstallationPermissionException;

    ArtifactPermissionEvidence applyAndVerify(
            CancellationTrustInstallationPlan plan,
            InstallationArtifact artifact,
            InstallationEnvironmentEvidence environment)
            throws InstallationPermissionException;

    AtomicPublicationEvidence publishAtomically(
            CancellationTrustInstallationPlan plan,
            InstallationArtifact staged,
            InstallationArtifact target,
            PublicationMode mode,
            InstallationEnvironmentEvidence environment)
            throws InstallationPermissionException;

    DurabilityEvidence forceDurable(
            CancellationTrustInstallationPlan plan,
            InstallationArtifact published,
            InstallationEnvironmentEvidence environment)
            throws InstallationPermissionException;

    ArtifactPermissionEvidence verifyPublished(
            CancellationTrustInstallationPlan plan,
            InstallationArtifact artifact,
            InstallationEnvironmentEvidence environment)
            throws InstallationPermissionException;

    RuntimeProbeEvidence probeReadOnlyAsRuntime(
            CancellationTrustInstallationPlan plan,
            InstallationEnvironmentEvidence environment)
            throws InstallationPermissionException;
}
