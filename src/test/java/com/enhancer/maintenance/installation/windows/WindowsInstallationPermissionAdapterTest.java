package com.enhancer.maintenance.installation.windows;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.enhancer.maintenance.installation.ArtifactPermissionEvidence;
import com.enhancer.maintenance.installation.AtomicPublicationEvidence;
import com.enhancer.maintenance.installation.CancellationTrustInstallationPermissionPolicy;
import com.enhancer.maintenance.installation.CancellationTrustInstallationPlan;
import com.enhancer.maintenance.installation.CancellationTrustInstallationPlanTest;
import com.enhancer.maintenance.installation.DurabilityEvidence;
import com.enhancer.maintenance.installation.InstallationAccess;
import com.enhancer.maintenance.installation.InstallationArtifact;
import com.enhancer.maintenance.installation.InstallationArtifactKind;
import com.enhancer.maintenance.installation.InstallationEnvironmentEvidence;
import com.enhancer.maintenance.installation.InstallationPermissionException;
import com.enhancer.maintenance.installation.InstallationPermissionFailureReason;
import com.enhancer.maintenance.installation.InstallationPrincipalRole;
import com.enhancer.maintenance.installation.PublicationMode;
import com.enhancer.maintenance.installation.RuntimeProbeEvidence;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class WindowsInstallationPermissionAdapterTest {
    private static final WindowsSid I = new WindowsSid("S-1-5-21-100");
    private static final WindowsSid O = new WindowsSid("S-1-5-21-200");
    private static final WindowsSid R = new WindowsSid("S-1-5-21-300");

    @Test
    void exactFakeGatewayEvidenceConvertsAtEveryTypedStage() throws Exception {
        CancellationTrustInstallationPlan plan = plan();
        RecordingGateway gateway = new RecordingGateway(plan);
        WindowsInstallationPermissionAdapter adapter =
                new WindowsInstallationPermissionAdapter(gateway);
        InstallationEnvironmentEvidence environment = adapter.resolveAndVerify(plan);
        InstallationArtifact metadata = plan.artifact(InstallationArtifactKind.FIXED_METADATA);

        ArtifactPermissionEvidence applied =
                adapter.applyAndVerify(plan, metadata, environment);
        AtomicPublicationEvidence published = adapter.publishAtomically(
                plan, plan.artifact(InstallationArtifactKind.METADATA_CANDIDATE),
                metadata, PublicationMode.REPLACE_EXISTING, environment);
        DurabilityEvidence durability = adapter.forceDurable(plan, metadata, environment);
        ArtifactPermissionEvidence rechecked =
                adapter.verifyPublished(plan, metadata, environment);
        RuntimeProbeEvidence probe = adapter.probeReadOnlyAsRuntime(plan, environment);

        assertEquals("windows-volume-v1", environment.filesystemIdentity());
        assertEquals(plan.principals(), environment.resolvedPrincipals());
        assertEquals(metadata, applied.artifact());
        assertEquals(applied, rechecked);
        assertEquals(metadata, published.target());
        assertEquals(metadata, durability.artifact());
        assertEquals(plan.requestedMetadataSha256(), probe.metadataSha256());
        assertEquals(plan.policySha256(), probe.policySha256());
        assertEquals(List.of("resolve", "apply:FIXED_METADATA",
                "publish:REPLACE_EXISTING", "durable:FIXED_METADATA",
                "inspect:FIXED_METADATA", "probe"), gateway.calls);
    }

    @Test
    void repeatedResolutionAndVerificationAreDeterministic() throws Exception {
        CancellationTrustInstallationPlan plan = plan();
        RecordingGateway gateway = new RecordingGateway(plan);
        WindowsInstallationPermissionAdapter adapter =
                new WindowsInstallationPermissionAdapter(gateway);
        InstallationEnvironmentEvidence firstEnvironment = adapter.resolveAndVerify(plan);
        InstallationEnvironmentEvidence secondEnvironment = adapter.resolveAndVerify(plan);
        InstallationArtifact metadata = plan.artifact(InstallationArtifactKind.FIXED_METADATA);

        assertEquals(firstEnvironment, secondEnvironment);
        assertEquals(adapter.applyAndVerify(plan, metadata, firstEnvironment),
                adapter.applyAndVerify(plan, metadata, secondEnvironment));
    }

    @Test
    void rawPublisherDeleteClosureDoesNotAuthorizeTypedDelete() throws Exception {
        CancellationTrustInstallationPlan plan = plan();
        RecordingGateway gateway = new RecordingGateway(plan);
        WindowsInstallationPermissionAdapter adapter =
                new WindowsInstallationPermissionAdapter(gateway);
        InstallationEnvironmentEvidence environment = adapter.resolveAndVerify(plan);
        InstallationArtifact metadata = plan.artifact(InstallationArtifactKind.FIXED_METADATA);

        WindowsArtifactSecuritySnapshot raw = gateway.snapshot(metadata);
        WindowsPrincipalArtifactAccess publisher = raw.access()
                .get(InstallationPrincipalRole.INSTALLER_PUBLISHER);
        ArtifactPermissionEvidence normalized =
                adapter.applyAndVerify(plan, metadata, environment);

        assertTrue(publisher.targetRights().allowed().contains(WindowsRawAccessRight.DELETE));
        assertFalse(normalized.allowedEffectiveAccess()
                .get(InstallationPrincipalRole.INSTALLER_PUBLISHER)
                .contains(InstallationAccess.DELETE));
        assertTrue(normalized.deniedEffectiveAccess()
                .get(InstallationPrincipalRole.INSTALLER_PUBLISHER)
                .contains(InstallationAccess.DELETE));
        for (InstallationPrincipalRole role : List.of(
                InstallationPrincipalRole.OPERATOR, InstallationPrincipalRole.RUNTIME)) {
            WindowsPrincipalArtifactAccess access = raw.access().get(role);
            assertFalse(access.targetRights().allowed().contains(WindowsRawAccessRight.DELETE));
            assertFalse(access.parentRights().allowed()
                    .contains(WindowsRawAccessRight.DELETE_CHILD));
        }
    }

    @Test
    void canonicalSidTokenAndPrivilegeContractsFailClosed() {
        assertThrows(IllegalArgumentException.class, () -> new WindowsSid("s-1-5-21-1"));
        assertThrows(IllegalArgumentException.class, () -> new WindowsSid("S-01-5-21-1"));
        assertThrows(IllegalArgumentException.class, () -> new WindowsPrincipalTokenEvidence(
                InstallationPrincipalRole.OPERATOR, O,
                List.of(new WindowsTokenGroupEvidence(
                        new WindowsSid("S-1-5-32-544"), true, false)), privileges()));
        Map<WindowsTokenPrivilege, WindowsPrivilegeState> dangerous = privileges();
        dangerous.put(WindowsTokenPrivilege.SE_RESTORE, WindowsPrivilegeState.ENABLED);
        assertThrows(IllegalArgumentException.class, () -> new WindowsPrincipalTokenEvidence(
                InstallationPrincipalRole.RUNTIME, R, List.of(), dangerous));
        assertThrows(IllegalArgumentException.class, () -> new WindowsPrincipalTokenEvidence(
                InstallationPrincipalRole.RUNTIME, new WindowsSid("S-1-5-18"),
                List.of(), privileges()));
        Map<WindowsTokenPrivilege, WindowsPrivilegeState> publisherPrivileges = privileges();
        publisherPrivileges.put(WindowsTokenPrivilege.SE_RESTORE,
                WindowsPrivilegeState.ENABLED);
        new WindowsPrincipalTokenEvidence(InstallationPrincipalRole.INSTALLER_PUBLISHER,
                I, List.of(new WindowsTokenGroupEvidence(
                        new WindowsSid("S-1-5-32-544"), true, false)),
                publisherPrivileges);
        new WindowsTokenGroupEvidence(new WindowsSid("S-1-5-32-545"), false, false);
    }

    @Test
    void topologyDaclAndRawAccessDriftMapToFiniteRefusals() throws Exception {
        CancellationTrustInstallationPlan plan = plan();
        RecordingGateway reparse = new RecordingGateway(plan);
        reparse.environment = reparse.environmentWithReparse();
        assertReason(InstallationPermissionFailureReason.TOPOLOGY_INVALID,
                () -> new WindowsInstallationPermissionAdapter(reparse).resolveAndVerify(plan));

        RecordingGateway missingComponent = new RecordingGateway(plan);
        missingComponent.environment = missingComponent.environmentWithMissingComponent();
        assertReason(InstallationPermissionFailureReason.TOPOLOGY_INVALID,
                () -> new WindowsInstallationPermissionAdapter(missingComponent)
                        .resolveAndVerify(plan));

        RecordingGateway dacl = new RecordingGateway(plan);
        WindowsInstallationPermissionAdapter adapter =
                new WindowsInstallationPermissionAdapter(dacl);
        InstallationEnvironmentEvidence environment = adapter.resolveAndVerify(plan);
        dacl.snapshotOverride = dacl.snapshotWithInheritedAcl(
                plan.artifact(InstallationArtifactKind.FIXED_METADATA));
        assertReason(InstallationPermissionFailureReason.EFFECTIVE_ACCESS_VERIFICATION_FAILED,
                () -> adapter.applyAndVerify(plan,
                        plan.artifact(InstallationArtifactKind.FIXED_METADATA), environment));

        RecordingGateway surplus = new RecordingGateway(plan);
        WindowsInstallationPermissionAdapter surplusAdapter =
                new WindowsInstallationPermissionAdapter(surplus);
        InstallationEnvironmentEvidence surplusEnvironment =
                surplusAdapter.resolveAndVerify(plan);
        surplus.snapshotOverride = surplus.snapshotWithSurplusOperatorDelete(
                plan.artifact(InstallationArtifactKind.FIXED_METADATA));
        assertReason(InstallationPermissionFailureReason.EFFECTIVE_ACCESS_VERIFICATION_FAILED,
                () -> surplusAdapter.applyAndVerify(plan,
                        plan.artifact(InstallationArtifactKind.FIXED_METADATA),
                        surplusEnvironment));
    }

    @Test
    void transactionSidVolumeArtifactAndFileIdentityDriftFailClosed() throws Exception {
        CancellationTrustInstallationPlan plan = plan();

        RecordingGateway transaction = new RecordingGateway(plan);
        transaction.environment = transaction.environmentWithTransaction(
                java.util.UUID.randomUUID());
        assertReason(InstallationPermissionFailureReason.IDENTITY_RESOLUTION_FAILED,
                () -> new WindowsInstallationPermissionAdapter(transaction)
                        .resolveAndVerify(plan));

        RecordingGateway sid = new RecordingGateway(plan);
        sid.environment = sid.environmentWithOperatorSid(new WindowsSid("S-1-5-21-999"));
        assertReason(InstallationPermissionFailureReason.IDENTITY_RESOLUTION_FAILED,
                () -> new WindowsInstallationPermissionAdapter(sid).resolveAndVerify(plan));

        RecordingGateway volume = new RecordingGateway(plan);
        volume.environment = volume.environmentWithPathVolume(
                new WindowsVolumeIdentity("windows-volume-v2"));
        assertReason(InstallationPermissionFailureReason.TOPOLOGY_INVALID,
                () -> new WindowsInstallationPermissionAdapter(volume)
                        .resolveAndVerify(plan));

        for (int drift = 0; drift < 3; drift++) {
            RecordingGateway artifactGateway = new RecordingGateway(plan);
            WindowsInstallationPermissionAdapter adapter =
                    new WindowsInstallationPermissionAdapter(artifactGateway);
            InstallationEnvironmentEvidence environment = adapter.resolveAndVerify(plan);
            InstallationArtifact metadata = plan.artifact(InstallationArtifactKind.FIXED_METADATA);
            WindowsArtifactSecuritySnapshot exact = artifactGateway.snapshot(metadata);
            artifactGateway.snapshotOverride = switch (drift) {
                case 0 -> new WindowsArtifactSecuritySnapshot(java.util.UUID.randomUUID(),
                        exact.artifact(), exact.objectType(), exact.identity(), exact.dacl(),
                        exact.access());
                case 1 -> new WindowsArtifactSecuritySnapshot(exact.transactionId(),
                        plan.artifact(InstallationArtifactKind.APPLICATION_JAR),
                        exact.objectType(), exact.identity(), exact.dacl(), exact.access());
                default -> new WindowsArtifactSecuritySnapshot(exact.transactionId(),
                        exact.artifact(), exact.objectType(),
                        new WindowsFileIdentity(exact.identity().volume(), "f".repeat(32)),
                        exact.dacl(), exact.access());
            };
            assertReason(InstallationPermissionFailureReason
                            .EFFECTIVE_ACCESS_VERIFICATION_FAILED,
                    () -> adapter.applyAndVerify(plan, metadata, environment));
        }
    }

    @Test
    void publicationDurabilityAndProbeIdentityMismatchesFailClosed() throws Exception {
        CancellationTrustInstallationPlan plan = plan();
        RecordingGateway gateway = new RecordingGateway(plan);
        WindowsInstallationPermissionAdapter adapter =
                new WindowsInstallationPermissionAdapter(gateway);
        InstallationEnvironmentEvidence environment = adapter.resolveAndVerify(plan);
        InstallationArtifact metadata = plan.artifact(InstallationArtifactKind.FIXED_METADATA);

        gateway.publicationAtomic = false;
        assertReason(InstallationPermissionFailureReason.PUBLICATION_FAILED,
                () -> adapter.publishAtomically(plan,
                        plan.artifact(InstallationArtifactKind.METADATA_CANDIDATE), metadata,
                        PublicationMode.REPLACE_EXISTING, environment));
        gateway.publicationAtomic = true;
        gateway.parentBarrier = false;
        assertReason(InstallationPermissionFailureReason.DURABILITY_FAILED,
                () -> adapter.forceDurable(plan, metadata, environment));
        gateway.parentBarrier = true;
        gateway.probeMetadataSha256 = "f".repeat(64);
        assertReason(InstallationPermissionFailureReason.RUNTIME_PROBE_FAILED,
                () -> adapter.probeReadOnlyAsRuntime(plan, environment));
    }

    @Test
    void gatewayFailuresStopAtTheirActualStageWithoutFallback() {
        for (WindowsInstallationGatewayFailureReason reason
                : WindowsInstallationGatewayFailureReason.values()) {
            CancellationTrustInstallationPlan plan = plan();
            RecordingGateway gateway = new RecordingGateway(plan);
            gateway.failure = reason;
            WindowsInstallationPermissionAdapter adapter =
                    new WindowsInstallationPermissionAdapter(gateway);
            assertThrows(InstallationPermissionException.class,
                    () -> invokeSequence(adapter, plan));
            assertEquals(expectedLastCall(reason),
                    gateway.calls.get(gateway.calls.size() - 1), reason.toString());
        }
    }

    private static CancellationTrustInstallationPlan plan() {
        CancellationTrustInstallationPlan original = CancellationTrustInstallationPlanTest
                .validPlan();
        return new CancellationTrustInstallationPlan(
                original.transactionId(), original.operation(),
                new com.enhancer.maintenance.installation.InstallationPrincipalSet(
                        new com.enhancer.maintenance.installation.InstallationPrincipal(
                                InstallationPrincipalRole.INSTALLER_PUBLISHER,
                                I.canonicalValue()),
                        new com.enhancer.maintenance.installation.InstallationPrincipal(
                                InstallationPrincipalRole.OPERATOR, O.canonicalValue()),
                        new com.enhancer.maintenance.installation.InstallationPrincipal(
                                InstallationPrincipalRole.RUNTIME, R.canonicalValue())),
                original.installationRoot(), original.applicationJar(),
                original.runtimeDistributionRoot(), original.operatorDistributionRoot(),
                original.operatorCandidateInbox(), original.activationPoint(),
                original.auditRoot(), original.sourceManifestSha256(),
                original.applicationJarSha256(), original.runtimeDistributionSha256(),
                original.operatorDistributionSha256(), original.permissionPolicyRevision(),
                original.policySha256(), original.requestedMetadataSha256(),
                original.expectedCurrentMetadataSha256());
    }

    private static void invokeSequence(
            WindowsInstallationPermissionAdapter adapter,
            CancellationTrustInstallationPlan plan) throws InstallationPermissionException {
        InstallationEnvironmentEvidence environment = adapter.resolveAndVerify(plan);
        InstallationArtifact metadata = plan.artifact(InstallationArtifactKind.FIXED_METADATA);
        adapter.applyAndVerify(plan, metadata, environment);
        adapter.publishAtomically(plan,
                plan.artifact(InstallationArtifactKind.METADATA_CANDIDATE), metadata,
                PublicationMode.REPLACE_EXISTING, environment);
        adapter.forceDurable(plan, metadata, environment);
        adapter.verifyPublished(plan, metadata, environment);
        adapter.probeReadOnlyAsRuntime(plan, environment);
    }

    private static String expectedLastCall(WindowsInstallationGatewayFailureReason reason) {
        return switch (reason) {
            case RESOLVE_ENVIRONMENT -> "resolve";
            case APPLY_PERMISSION_PROFILE -> "apply:FIXED_METADATA";
            case PUBLISH_ATOMICALLY -> "publish:REPLACE_EXISTING";
            case FORCE_DURABLE -> "durable:FIXED_METADATA";
            case INSPECT_PUBLISHED -> "inspect:FIXED_METADATA";
            case PROBE_AS_RUNTIME -> "probe";
        };
    }

    private static Map<WindowsTokenPrivilege, WindowsPrivilegeState> privileges() {
        Map<WindowsTokenPrivilege, WindowsPrivilegeState> result =
                new EnumMap<>(WindowsTokenPrivilege.class);
        for (WindowsTokenPrivilege privilege : WindowsTokenPrivilege.values()) {
            result.put(privilege, WindowsPrivilegeState.ABSENT);
        }
        return result;
    }

    private static void assertReason(
            InstallationPermissionFailureReason reason, Throwing action) {
        InstallationPermissionException failure =
                assertThrows(InstallationPermissionException.class, action::run);
        assertEquals(reason, failure.reason());
    }

    @FunctionalInterface
    private interface Throwing {
        void run() throws InstallationPermissionException;
    }

    private static final class RecordingGateway implements WindowsInstallationPermissionGateway {
        private final CancellationTrustInstallationPlan plan;
        private final List<String> calls = new ArrayList<>();
        private WindowsEnvironmentSnapshot environment;
        private WindowsArtifactSecuritySnapshot snapshotOverride;
        private WindowsInstallationGatewayFailureReason failure;
        private boolean publicationAtomic = true;
        private boolean parentBarrier = true;
        private String probeMetadataSha256;

        private RecordingGateway(CancellationTrustInstallationPlan plan) {
            this.plan = plan;
            this.environment = environment(false);
            this.probeMetadataSha256 = plan.requestedMetadataSha256();
        }

        @Override
        public WindowsEnvironmentSnapshot resolveEnvironment(
                CancellationTrustInstallationPlan ignored)
                throws WindowsInstallationGatewayException {
            call("resolve", WindowsInstallationGatewayFailureReason.RESOLVE_ENVIRONMENT);
            return environment;
        }

        @Override
        public WindowsArtifactSecuritySnapshot applyPermissionProfile(
                CancellationTrustInstallationPlan ignored,
                InstallationArtifact artifact,
                InstallationEnvironmentEvidence environmentEvidence)
                throws WindowsInstallationGatewayException {
            call("apply:" + artifact.kind(),
                    WindowsInstallationGatewayFailureReason.APPLY_PERMISSION_PROFILE);
            return snapshotOverride == null ? snapshot(artifact) : snapshotOverride;
        }

        @Override
        public WindowsPublicationSnapshot publishAtomically(
                CancellationTrustInstallationPlan ignored,
                InstallationArtifact staged,
                InstallationArtifact target,
                PublicationMode mode,
                InstallationEnvironmentEvidence environmentEvidence)
                throws WindowsInstallationGatewayException {
            call("publish:" + mode,
                    WindowsInstallationGatewayFailureReason.PUBLISH_ATOMICALLY);
            return new WindowsPublicationSnapshot(plan.transactionId(), staged, target, mode,
                    identity(target.path()), new WindowsVolumeIdentity("windows-volume-v1"),
                    true, publicationAtomic);
        }

        @Override
        public WindowsDurabilitySnapshot forceDurable(
                CancellationTrustInstallationPlan ignored,
                InstallationArtifact artifact,
                InstallationEnvironmentEvidence environmentEvidence)
                throws WindowsInstallationGatewayException {
            call("durable:" + artifact.kind(),
                    WindowsInstallationGatewayFailureReason.FORCE_DURABLE);
            return new WindowsDurabilitySnapshot(plan.transactionId(), artifact,
                    identity(artifact.path()), true, parentBarrier);
        }

        @Override
        public WindowsArtifactSecuritySnapshot inspectPublished(
                CancellationTrustInstallationPlan ignored,
                InstallationArtifact artifact,
                InstallationEnvironmentEvidence environmentEvidence)
                throws WindowsInstallationGatewayException {
            call("inspect:" + artifact.kind(),
                    WindowsInstallationGatewayFailureReason.INSPECT_PUBLISHED);
            return snapshotOverride == null ? snapshot(artifact) : snapshotOverride;
        }

        @Override
        public WindowsRuntimeProbeSnapshot probeAsRuntime(
                CancellationTrustInstallationPlan ignored,
                InstallationEnvironmentEvidence environmentEvidence)
                throws WindowsInstallationGatewayException {
            call("probe", WindowsInstallationGatewayFailureReason.PROBE_AS_RUNTIME);
            return new WindowsRuntimeProbeSnapshot(plan.transactionId(), R,
                    probeMetadataSha256, plan.policySha256(), true, true, false);
        }

        private WindowsEnvironmentSnapshot environmentWithReparse() {
            return environment(true);
        }

        private WindowsEnvironmentSnapshot environmentWithTransaction(
                java.util.UUID transactionId) {
            WindowsEnvironmentSnapshot exact = environment(false);
            return new WindowsEnvironmentSnapshot(transactionId, exact.principals(),
                    exact.tokens(), exact.volume(), exact.paths(), exact.adapterId(),
                    exact.adapterVersion());
        }

        private WindowsEnvironmentSnapshot environmentWithOperatorSid(WindowsSid sid) {
            WindowsEnvironmentSnapshot exact = environment(false);
            Map<InstallationPrincipalRole, WindowsPrincipalTokenEvidence> tokens =
                    new EnumMap<>(exact.tokens());
            tokens.put(InstallationPrincipalRole.OPERATOR,
                    new WindowsPrincipalTokenEvidence(InstallationPrincipalRole.OPERATOR,
                            sid, List.of(), privileges()));
            return new WindowsEnvironmentSnapshot(exact.transactionId(), exact.principals(),
                    tokens, exact.volume(), exact.paths(), exact.adapterId(),
                    exact.adapterVersion());
        }

        private WindowsEnvironmentSnapshot environmentWithPathVolume(
                WindowsVolumeIdentity volume) {
            WindowsEnvironmentSnapshot exact = environment(false);
            Map<InstallationArtifactKind, List<WindowsPathComponentEvidence>> paths =
                    new EnumMap<>(exact.paths());
            InstallationArtifact metadata = plan.artifact(InstallationArtifactKind.FIXED_METADATA);
            List<WindowsPathComponentEvidence> components = new ArrayList<>(
                    paths.get(metadata.kind()));
            WindowsPathComponentEvidence leaf = components.get(components.size() - 1);
            components.set(components.size() - 1, new WindowsPathComponentEvidence(
                    leaf.path(), leaf.objectType(),
                    new WindowsFileIdentity(volume, "f".repeat(32)), false));
            paths.put(metadata.kind(), List.copyOf(components));
            return new WindowsEnvironmentSnapshot(exact.transactionId(), exact.principals(),
                    exact.tokens(), exact.volume(), paths, exact.adapterId(),
                    exact.adapterVersion());
        }

        private WindowsEnvironmentSnapshot environmentWithMissingComponent() {
            WindowsEnvironmentSnapshot exact = environment(false);
            Map<InstallationArtifactKind, List<WindowsPathComponentEvidence>> paths =
                    new EnumMap<>(exact.paths());
            InstallationArtifact metadata = plan.artifact(InstallationArtifactKind.FIXED_METADATA);
            List<WindowsPathComponentEvidence> components = new ArrayList<>(
                    paths.get(metadata.kind()));
            components.remove(1);
            paths.put(metadata.kind(), List.copyOf(components));
            return new WindowsEnvironmentSnapshot(exact.transactionId(), exact.principals(),
                    exact.tokens(), exact.volume(), paths, exact.adapterId(),
                    exact.adapterVersion());
        }

        private WindowsEnvironmentSnapshot environment(boolean reparse) {
            Map<InstallationPrincipalRole, WindowsPrincipalTokenEvidence> tokens =
                    new EnumMap<>(InstallationPrincipalRole.class);
            tokens.put(InstallationPrincipalRole.INSTALLER_PUBLISHER,
                    new WindowsPrincipalTokenEvidence(
                            InstallationPrincipalRole.INSTALLER_PUBLISHER, I,
                            List.of(), privileges()));
            tokens.put(InstallationPrincipalRole.OPERATOR,
                    new WindowsPrincipalTokenEvidence(
                            InstallationPrincipalRole.OPERATOR, O,
                            List.of(), privileges()));
            tokens.put(InstallationPrincipalRole.RUNTIME,
                    new WindowsPrincipalTokenEvidence(
                            InstallationPrincipalRole.RUNTIME, R,
                            List.of(), privileges()));
            Map<InstallationArtifactKind, List<WindowsPathComponentEvidence>> paths =
                    new EnumMap<>(InstallationArtifactKind.class);
            for (InstallationArtifact artifact : plan.artifacts()) {
                List<WindowsPathComponentEvidence> components = new ArrayList<>();
                Path current = artifact.path().getRoot();
                components.add(new WindowsPathComponentEvidence(current,
                        WindowsObjectType.DIRECTORY, identity(current), false));
                for (Path name : artifact.path()) {
                    current = current.resolve(name);
                    boolean leaf = current.equals(artifact.path());
                    components.add(new WindowsPathComponentEvidence(current,
                            leaf ? objectType(artifact.kind()) : WindowsObjectType.DIRECTORY,
                            identity(current), reparse && leaf
                                    && artifact.kind() == InstallationArtifactKind.FIXED_METADATA));
                }
                paths.put(artifact.kind(), List.copyOf(components));
            }
            return new WindowsEnvironmentSnapshot(plan.transactionId(), plan.principals(),
                    tokens, new WindowsVolumeIdentity("windows-volume-v1"), paths,
                    "windows-fake-gateway", "windows-fake-v1");
        }

        private WindowsArtifactSecuritySnapshot snapshot(InstallationArtifact artifact) {
            Map<InstallationPrincipalRole, WindowsPrincipalArtifactAccess> access =
                    new EnumMap<>(InstallationPrincipalRole.class);
            for (InstallationPrincipalRole role : InstallationPrincipalRole.values()) {
                access.put(role, WindowsInstallationRightsPolicy.required(
                        artifact.kind(), objectType(artifact.kind()), role));
            }
            return new WindowsArtifactSecuritySnapshot(plan.transactionId(), artifact,
                    objectType(artifact.kind()), identity(artifact.path()),
                    new WindowsDaclEvidence(I, true, false, true, 0, true), access);
        }

        private WindowsArtifactSecuritySnapshot snapshotWithInheritedAcl(
                InstallationArtifact artifact) {
            WindowsArtifactSecuritySnapshot exact = snapshot(artifact);
            return new WindowsArtifactSecuritySnapshot(exact.transactionId(), exact.artifact(),
                    exact.objectType(), exact.identity(),
                    new WindowsDaclEvidence(I, true, false, true, 1, true), exact.access());
        }

        private WindowsArtifactSecuritySnapshot snapshotWithSurplusOperatorDelete(
                InstallationArtifact artifact) {
            WindowsArtifactSecuritySnapshot exact = snapshot(artifact);
            Map<InstallationPrincipalRole, WindowsPrincipalArtifactAccess> changed =
                    new EnumMap<>(exact.access());
            WindowsPrincipalArtifactAccess operator = changed.get(
                    InstallationPrincipalRole.OPERATOR);
            EnumSet<WindowsRawAccessRight> allowed = EnumSet.copyOf(
                    operator.targetRights().allowed());
            allowed.add(WindowsRawAccessRight.DELETE);
            EnumSet<WindowsRawAccessRight> denied = EnumSet.allOf(WindowsRawAccessRight.class);
            denied.removeAll(allowed);
            changed.put(InstallationPrincipalRole.OPERATOR,
                    new WindowsPrincipalArtifactAccess(InstallationPrincipalRole.OPERATOR,
                            new WindowsRawAccessPartition(allowed, denied),
                            operator.parentRights(), operator.normalizedAllowed(),
                            operator.normalizedDenied()));
            return new WindowsArtifactSecuritySnapshot(exact.transactionId(), exact.artifact(),
                    exact.objectType(), exact.identity(), exact.dacl(), changed);
        }

        private WindowsFileIdentity identity(Path path) {
            String hex = Integer.toUnsignedString(path.toString().hashCode(), 16);
            return new WindowsFileIdentity(new WindowsVolumeIdentity("windows-volume-v1"),
                    "0".repeat(32 - hex.length()) + hex);
        }

        private void call(String name, WindowsInstallationGatewayFailureReason stage)
                throws WindowsInstallationGatewayException {
            calls.add(name);
            if (failure == stage) {
                throw new WindowsInstallationGatewayException(stage, "fake failure");
            }
        }
    }

    private static WindowsObjectType objectType(InstallationArtifactKind kind) {
        return switch (kind) {
            case INSTALLATION_ANCESTOR, RUNTIME_DISTRIBUTION, OPERATOR_DISTRIBUTION,
                    TRUST_DIRECTORY, OPERATOR_CANDIDATE_INBOX,
                    INSTALLATION_AUDIT_ROOT -> WindowsObjectType.DIRECTORY;
            default -> WindowsObjectType.FILE;
        };
    }
}
