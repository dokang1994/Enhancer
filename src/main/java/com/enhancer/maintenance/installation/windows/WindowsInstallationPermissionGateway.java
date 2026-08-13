package com.enhancer.maintenance.installation.windows;

import com.enhancer.maintenance.installation.CancellationTrustInstallationPlan;
import com.enhancer.maintenance.installation.InstallationArtifact;
import com.enhancer.maintenance.installation.InstallationEnvironmentEvidence;
import com.enhancer.maintenance.installation.PublicationMode;

/** Injected platform seam. This repository intentionally supplies no implementation. */
public interface WindowsInstallationPermissionGateway {
    WindowsEnvironmentSnapshot resolveEnvironment(CancellationTrustInstallationPlan plan)
            throws WindowsInstallationGatewayException;

    WindowsArtifactSecuritySnapshot applyPermissionProfile(
            CancellationTrustInstallationPlan plan,
            InstallationArtifact artifact,
            InstallationEnvironmentEvidence environment)
            throws WindowsInstallationGatewayException;

    WindowsPublicationSnapshot publishAtomically(
            CancellationTrustInstallationPlan plan,
            InstallationArtifact staged,
            InstallationArtifact target,
            PublicationMode mode,
            InstallationEnvironmentEvidence environment)
            throws WindowsInstallationGatewayException;

    WindowsDurabilitySnapshot forceDurable(
            CancellationTrustInstallationPlan plan,
            InstallationArtifact artifact,
            InstallationEnvironmentEvidence environment)
            throws WindowsInstallationGatewayException;

    WindowsArtifactSecuritySnapshot inspectPublished(
            CancellationTrustInstallationPlan plan,
            InstallationArtifact artifact,
            InstallationEnvironmentEvidence environment)
            throws WindowsInstallationGatewayException;

    WindowsRuntimeProbeSnapshot probeAsRuntime(
            CancellationTrustInstallationPlan plan,
            InstallationEnvironmentEvidence environment)
            throws WindowsInstallationGatewayException;
}
