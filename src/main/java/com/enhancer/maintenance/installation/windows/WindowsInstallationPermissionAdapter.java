package com.enhancer.maintenance.installation.windows;

import com.enhancer.maintenance.installation.ArtifactPermissionEvidence;
import com.enhancer.maintenance.installation.AtomicPublicationEvidence;
import com.enhancer.maintenance.installation.CancellationTrustInstallationPermissionPolicy;
import com.enhancer.maintenance.installation.CancellationTrustInstallationPlan;
import com.enhancer.maintenance.installation.DurabilityEvidence;
import com.enhancer.maintenance.installation.InstallationAccess;
import com.enhancer.maintenance.installation.InstallationArtifact;
import com.enhancer.maintenance.installation.InstallationArtifactKind;
import com.enhancer.maintenance.installation.InstallationEnvironmentEvidence;
import com.enhancer.maintenance.installation.InstallationPermissionAdapter;
import com.enhancer.maintenance.installation.InstallationPermissionException;
import com.enhancer.maintenance.installation.InstallationPermissionFailureReason;
import com.enhancer.maintenance.installation.InstallationPrincipal;
import com.enhancer.maintenance.installation.InstallationPrincipalRole;
import com.enhancer.maintenance.installation.PublicationMode;
import com.enhancer.maintenance.installation.RuntimeProbeEvidence;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Windows contract adapter over an injected gateway. It contains no OS, filesystem,
 * process, shell, native-library, account lookup, or security-descriptor access.
 */
public final class WindowsInstallationPermissionAdapter
        implements InstallationPermissionAdapter {
    private final WindowsInstallationPermissionGateway gateway;
    private final ConcurrentMap<UUID, WindowsEnvironmentSnapshot> resolvedEnvironments =
            new ConcurrentHashMap<>();
    private final ConcurrentMap<PublishedArtifactKey, WindowsFileIdentity>
            publishedIdentities = new ConcurrentHashMap<>();

    public WindowsInstallationPermissionAdapter(WindowsInstallationPermissionGateway gateway) {
        this.gateway = Objects.requireNonNull(gateway, "gateway must not be null");
    }

    @Override
    public InstallationEnvironmentEvidence resolveAndVerify(
            CancellationTrustInstallationPlan plan) throws InstallationPermissionException {
        CancellationTrustInstallationPlan checkedPlan = plan(plan);
        WindowsEnvironmentSnapshot snapshot;
        try {
            snapshot = gateway.resolveEnvironment(checkedPlan);
        } catch (WindowsInstallationGatewayException failure) {
            throw refusal(InstallationPermissionFailureReason.IDENTITY_RESOLUTION_FAILED,
                    "Windows environment resolution failed");
        }
        validateEnvironment(checkedPlan, snapshot);
        WindowsEnvironmentSnapshot retained = resolvedEnvironments.putIfAbsent(
                checkedPlan.transactionId(), snapshot);
        if (retained != null && !retained.equals(snapshot)) {
            throw refusal(InstallationPermissionFailureReason.TOPOLOGY_INVALID,
                    "Windows environment replay differed from retained identity evidence");
        }
        return new InstallationEnvironmentEvidence(
                snapshot.transactionId(), snapshot.adapterId(), snapshot.adapterVersion(),
                snapshot.principals(), snapshot.volume().value(), true, true);
    }

    @Override
    public ArtifactPermissionEvidence applyAndVerify(
            CancellationTrustInstallationPlan plan,
            InstallationArtifact artifact,
            InstallationEnvironmentEvidence environment)
            throws InstallationPermissionException {
        WindowsEnvironmentSnapshot resolved = validateInputs(plan, artifact, environment);
        WindowsArtifactSecuritySnapshot snapshot;
        try {
            snapshot = gateway.applyPermissionProfile(plan, artifact, environment);
        } catch (WindowsInstallationGatewayException failure) {
            throw refusal(InstallationPermissionFailureReason.PERMISSION_APPLICATION_FAILED,
                    "Windows permission application failed");
        }
        return normalizeSecurity(plan, artifact, environment,
                leafIdentity(resolved, artifact.kind()), snapshot,
                InstallationPermissionFailureReason.EFFECTIVE_ACCESS_VERIFICATION_FAILED);
    }

    @Override
    public AtomicPublicationEvidence publishAtomically(
            CancellationTrustInstallationPlan plan,
            InstallationArtifact staged,
            InstallationArtifact target,
            PublicationMode mode,
            InstallationEnvironmentEvidence environment)
            throws InstallationPermissionException {
        validateInputs(plan, staged, environment);
        requirePlannedArtifact(plan, target);
        Objects.requireNonNull(mode, "mode must not be null");
        WindowsPublicationSnapshot snapshot;
        try {
            snapshot = gateway.publishAtomically(plan, staged, target, mode, environment);
        } catch (WindowsInstallationGatewayException failure) {
            throw refusal(InstallationPermissionFailureReason.PUBLICATION_FAILED,
                    "Windows atomic publication failed");
        }
        if (snapshot == null
                || !snapshot.transactionId().equals(plan.transactionId())
                || !snapshot.staged().equals(staged)
                || !snapshot.target().equals(target)
                || snapshot.mode() != mode
                || !snapshot.volume().value().equals(environment.filesystemIdentity())
                || !snapshot.targetIdentity().volume().equals(snapshot.volume())
                || !snapshot.sameVolume()
                || !snapshot.atomic()) {
            throw refusal(InstallationPermissionFailureReason.PUBLICATION_FAILED,
                    "Windows publication evidence did not match the exact plan");
        }
        AtomicPublicationEvidence evidence;
        try {
            evidence = new AtomicPublicationEvidence(plan.transactionId(), staged, target, mode,
                    snapshot.volume().value(), true);
        } catch (IllegalArgumentException failure) {
            throw refusal(InstallationPermissionFailureReason.PUBLICATION_FAILED,
                    "Windows publication mode was invalid for the target");
        }
        PublishedArtifactKey key = new PublishedArtifactKey(
                plan.transactionId(), target.kind());
        WindowsFileIdentity retained = publishedIdentities.putIfAbsent(
                key, snapshot.targetIdentity());
        if (retained != null && !retained.equals(snapshot.targetIdentity())) {
            throw refusal(InstallationPermissionFailureReason.PUBLICATION_FAILED,
                    "Windows publication replay changed the target identity");
        }
        return evidence;
    }

    @Override
    public DurabilityEvidence forceDurable(
            CancellationTrustInstallationPlan plan,
            InstallationArtifact published,
            InstallationEnvironmentEvidence environment)
            throws InstallationPermissionException {
        validateInputs(plan, published, environment);
        WindowsFileIdentity publishedIdentity = publishedIdentity(
                plan, published, InstallationPermissionFailureReason.DURABILITY_FAILED);
        WindowsDurabilitySnapshot snapshot;
        try {
            snapshot = gateway.forceDurable(plan, published, environment);
        } catch (WindowsInstallationGatewayException failure) {
            throw refusal(InstallationPermissionFailureReason.DURABILITY_FAILED,
                    "Windows durability barrier failed");
        }
        if (snapshot == null
                || !snapshot.transactionId().equals(plan.transactionId())
                || !snapshot.artifact().equals(published)
                || !snapshot.identity().equals(publishedIdentity)
                || !snapshot.fileBarrierComplete()
                || !snapshot.parentOrVolumeBarrierComplete()) {
            throw refusal(InstallationPermissionFailureReason.DURABILITY_FAILED,
                    "Windows durability evidence did not match the exact publication");
        }
        return new DurabilityEvidence(plan.transactionId(), published,
                "windows-file-and-parent-or-volume-barriers", true, true);
    }

    @Override
    public ArtifactPermissionEvidence verifyPublished(
            CancellationTrustInstallationPlan plan,
            InstallationArtifact artifact,
            InstallationEnvironmentEvidence environment)
            throws InstallationPermissionException {
        validateInputs(plan, artifact, environment);
        WindowsFileIdentity publishedIdentity = publishedIdentity(
                plan, artifact, InstallationPermissionFailureReason.PUBLISHED_RECHECK_FAILED);
        WindowsArtifactSecuritySnapshot snapshot;
        try {
            snapshot = gateway.inspectPublished(plan, artifact, environment);
        } catch (WindowsInstallationGatewayException failure) {
            throw refusal(InstallationPermissionFailureReason.PUBLISHED_RECHECK_FAILED,
                    "Windows published-artifact inspection failed");
        }
        return normalizeSecurity(plan, artifact, environment, publishedIdentity, snapshot,
                InstallationPermissionFailureReason.PUBLISHED_RECHECK_FAILED);
    }

    @Override
    public RuntimeProbeEvidence probeReadOnlyAsRuntime(
            CancellationTrustInstallationPlan plan,
            InstallationEnvironmentEvidence environment)
            throws InstallationPermissionException {
        validateEnvironmentBinding(plan(plan), environment);
        WindowsRuntimeProbeSnapshot snapshot;
        try {
            snapshot = gateway.probeAsRuntime(plan, environment);
        } catch (WindowsInstallationGatewayException failure) {
            throw refusal(InstallationPermissionFailureReason.RUNTIME_PROBE_FAILED,
                    "Windows runtime probe failed");
        }
        String runtimeIdentity = plan.principals().runtime().stableOperatingSystemIdentity();
        if (snapshot == null
                || !snapshot.transactionId().equals(plan.transactionId())
                || !snapshot.runtimeSid().canonicalValue().equals(runtimeIdentity)
                || !snapshot.metadataSha256().equals(plan.requestedMetadataSha256())
                || !snapshot.policySha256().equals(plan.policySha256())
                || !snapshot.metadataRead()
                || !snapshot.policyRead()
                || snapshot.mutationObserved()) {
            throw refusal(InstallationPermissionFailureReason.RUNTIME_PROBE_FAILED,
                    "Windows runtime probe evidence did not match requested trust bytes");
        }
        return new RuntimeProbeEvidence(plan.transactionId(), plan.principals().runtime(),
                snapshot.metadataSha256(), snapshot.policySha256(), true, false);
    }

    private static void validateEnvironment(
            CancellationTrustInstallationPlan plan, WindowsEnvironmentSnapshot snapshot)
            throws InstallationPermissionException {
        if (snapshot == null
                || !snapshot.transactionId().equals(plan.transactionId())
                || !snapshot.principals().equals(plan.principals())) {
            throw refusal(InstallationPermissionFailureReason.IDENTITY_RESOLUTION_FAILED,
                    "Windows principal evidence did not match the exact plan");
        }
        for (InstallationPrincipalRole role : InstallationPrincipalRole.values()) {
            WindowsPrincipalTokenEvidence token = snapshot.tokens().get(role);
            if (!token.userSid().canonicalValue().equals(principal(plan, role)
                    .stableOperatingSystemIdentity())) {
                throw refusal(InstallationPermissionFailureReason.IDENTITY_RESOLUTION_FAILED,
                        "Windows token SID did not match the exact role identity");
            }
        }
        Map<java.nio.file.Path, WindowsFileIdentity> observedPaths = new java.util.HashMap<>();
        Map<WindowsFileIdentity, java.nio.file.Path> observedIdentities =
                new java.util.HashMap<>();
        for (InstallationArtifact artifact : plan.artifacts()) {
            List<WindowsPathComponentEvidence> components = snapshot.paths().get(artifact.kind());
            List<java.nio.file.Path> expectedPaths = pathComponents(artifact.path());
            WindowsPathComponentEvidence leaf = components.get(components.size() - 1);
            if (components.size() != expectedPaths.size()
                    || !leaf.path().equals(artifact.path())
                    || leaf.objectType() != objectType(artifact.kind())) {
                throw refusal(InstallationPermissionFailureReason.TOPOLOGY_INVALID,
                        "Windows path evidence did not match the exact planned artifact");
            }
            for (int index = 0; index < components.size(); index++) {
                WindowsPathComponentEvidence component = components.get(index);
                if (!component.path().equals(expectedPaths.get(index))) {
                    throw refusal(InstallationPermissionFailureReason.TOPOLOGY_INVALID,
                            "Windows path evidence omitted or reordered a path component");
                }
                WindowsFileIdentity earlierIdentity = observedPaths.putIfAbsent(
                        component.path(), component.identity());
                if (earlierIdentity != null && !earlierIdentity.equals(component.identity())) {
                    throw refusal(InstallationPermissionFailureReason.TOPOLOGY_INVALID,
                            "Windows path identity differed across planned artifacts");
                }
                java.nio.file.Path earlierPath = observedIdentities.putIfAbsent(
                        component.identity(), component.path());
                if (earlierPath != null && !earlierPath.equals(component.path())) {
                    throw refusal(InstallationPermissionFailureReason.TOPOLOGY_INVALID,
                            "Windows file identity was shared by distinct planned paths");
                }
                if (component.reparsePoint()
                        || !component.identity().volume().equals(snapshot.volume())) {
                    throw refusal(InstallationPermissionFailureReason.TOPOLOGY_INVALID,
                            "Windows path topology was not link-free on one volume");
                }
            }
        }
    }

    private static ArtifactPermissionEvidence normalizeSecurity(
            CancellationTrustInstallationPlan plan,
            InstallationArtifact artifact,
            InstallationEnvironmentEvidence environment,
            WindowsFileIdentity expectedIdentity,
            WindowsArtifactSecuritySnapshot snapshot,
            InstallationPermissionFailureReason failureReason)
            throws InstallationPermissionException {
        if (snapshot == null
                || !snapshot.transactionId().equals(plan.transactionId())
                || !snapshot.artifact().equals(artifact)
                || snapshot.objectType() != objectType(artifact.kind())
                || !snapshot.identity().equals(expectedIdentity)) {
            throw refusal(failureReason,
                    "Windows artifact identity evidence did not match the exact plan");
        }
        WindowsDaclEvidence dacl = snapshot.dacl();
        String publisher = plan.principals().installerPublisher()
                .stableOperatingSystemIdentity();
        if (!dacl.owner().canonicalValue().equals(publisher)
                || !dacl.present() || dacl.nullAcl() || !dacl.protectedAcl()
                || dacl.inheritedAceCount() != 0 || !dacl.canonicalOrder()) {
            throw refusal(failureReason,
                    "Windows DACL evidence was not protected, explicit, and canonical");
        }
        Map<InstallationPrincipalRole, Set<InstallationAccess>> allowed =
                new EnumMap<>(InstallationPrincipalRole.class);
        Map<InstallationPrincipalRole, Set<InstallationAccess>> denied =
                new EnumMap<>(InstallationPrincipalRole.class);
        for (InstallationPrincipalRole role : InstallationPrincipalRole.values()) {
            WindowsPrincipalArtifactAccess actual = snapshot.access().get(role);
            WindowsPrincipalArtifactAccess required = WindowsInstallationRightsPolicy.required(
                    artifact.kind(), snapshot.objectType(), role);
            if (!actual.equals(required)) {
                throw refusal(failureReason,
                        "Windows effective access differed from the exact rights closure");
            }
            allowed.put(role, actual.normalizedAllowed());
            denied.put(role, actual.normalizedDenied());
        }
        return new ArtifactPermissionEvidence(plan.transactionId(), artifact,
                CancellationTrustInstallationPermissionPolicy.REVISION, publisher,
                allowed, denied);
    }

    private WindowsEnvironmentSnapshot validateInputs(
            CancellationTrustInstallationPlan plan,
            InstallationArtifact artifact,
            InstallationEnvironmentEvidence environment)
            throws InstallationPermissionException {
        requirePlannedArtifact(plan(plan), artifact);
        return validateEnvironmentBinding(plan, environment);
    }

    private WindowsEnvironmentSnapshot validateEnvironmentBinding(
            CancellationTrustInstallationPlan plan,
            InstallationEnvironmentEvidence environment)
            throws InstallationPermissionException {
        WindowsEnvironmentSnapshot resolved = resolvedEnvironments.get(plan.transactionId());
        if (environment == null || resolved == null
                || !environment.transactionId().equals(plan.transactionId())
                || !environment.resolvedPrincipals().equals(plan.principals())
                || !environment.adapterId().equals(resolved.adapterId())
                || !environment.adapterVersion().equals(resolved.adapterVersion())
                || !environment.filesystemIdentity().equals(resolved.volume().value())
                || !environment.sameFilesystem() || !environment.linksAbsent()) {
            throw refusal(InstallationPermissionFailureReason.TOPOLOGY_INVALID,
                    "environment evidence did not match the exact plan");
        }
        return resolved;
    }

    private static void requirePlannedArtifact(
            CancellationTrustInstallationPlan plan, InstallationArtifact artifact)
            throws InstallationPermissionException {
        if (artifact == null || !plan.artifact(artifact.kind()).equals(artifact)) {
            throw refusal(InstallationPermissionFailureReason.TOPOLOGY_INVALID,
                    "artifact was not the exact planned artifact");
        }
    }

    private static CancellationTrustInstallationPlan plan(
            CancellationTrustInstallationPlan plan) {
        return Objects.requireNonNull(plan, "plan must not be null");
    }

    private static InstallationPrincipal principal(
            CancellationTrustInstallationPlan plan, InstallationPrincipalRole role) {
        return switch (role) {
            case INSTALLER_PUBLISHER -> plan.principals().installerPublisher();
            case OPERATOR -> plan.principals().operator();
            case RUNTIME -> plan.principals().runtime();
        };
    }

    private static WindowsObjectType objectType(InstallationArtifactKind kind) {
        return switch (kind) {
            case INSTALLATION_ANCESTOR, RUNTIME_DISTRIBUTION, OPERATOR_DISTRIBUTION,
                    TRUST_DIRECTORY, OPERATOR_CANDIDATE_INBOX,
                    INSTALLATION_AUDIT_ROOT -> WindowsObjectType.DIRECTORY;
            default -> WindowsObjectType.FILE;
        };
    }

    private static WindowsFileIdentity leafIdentity(
            WindowsEnvironmentSnapshot environment, InstallationArtifactKind kind) {
        List<WindowsPathComponentEvidence> components = environment.paths().get(kind);
        return components.get(components.size() - 1).identity();
    }

    private WindowsFileIdentity publishedIdentity(
            CancellationTrustInstallationPlan plan,
            InstallationArtifact artifact,
            InstallationPermissionFailureReason failureReason)
            throws InstallationPermissionException {
        WindowsFileIdentity identity = publishedIdentities.get(
                new PublishedArtifactKey(plan.transactionId(), artifact.kind()));
        if (identity == null) {
            throw refusal(failureReason,
                    "Windows publication identity was not retained for the artifact");
        }
        return identity;
    }

    private static List<java.nio.file.Path> pathComponents(java.nio.file.Path path) {
        java.util.ArrayList<java.nio.file.Path> result = new java.util.ArrayList<>();
        java.nio.file.Path current = path.getRoot();
        result.add(current);
        for (java.nio.file.Path name : path) {
            current = current.resolve(name);
            result.add(current);
        }
        return List.copyOf(result);
    }

    private static InstallationPermissionException refusal(
            InstallationPermissionFailureReason reason, String detail) {
        return new InstallationPermissionException(reason, detail);
    }

    private record PublishedArtifactKey(
            UUID transactionId, InstallationArtifactKind artifactKind) {
        private PublishedArtifactKey {
            Objects.requireNonNull(transactionId, "transactionId must not be null");
            Objects.requireNonNull(artifactKind, "artifactKind must not be null");
        }
    }
}
