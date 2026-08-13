package com.enhancer.maintenance.installation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class InstallationPermissionAdapterContractTest {
    @Test
    void recordingFakePreservesTypedPlanAndEvidenceIdentity() throws Exception {
        CancellationTrustInstallationPlan plan = CancellationTrustInstallationPlanTest.validPlan();
        RecordingAdapter fake = new RecordingAdapter(null);
        InstallationEnvironmentEvidence environment = fake.resolveAndVerify(plan);
        InstallationArtifact metadata = plan.artifact(InstallationArtifactKind.FIXED_METADATA);

        ArtifactPermissionEvidence staged = fake.applyAndVerify(plan, metadata, environment);
        AtomicPublicationEvidence publication = fake.publishAtomically(
                plan, plan.artifact(InstallationArtifactKind.METADATA_CANDIDATE), metadata,
                PublicationMode.REPLACE_EXISTING, environment);
        DurabilityEvidence durability = fake.forceDurable(plan, metadata, environment);
        ArtifactPermissionEvidence published = fake.verifyPublished(plan, metadata, environment);
        RuntimeProbeEvidence probe = fake.probeReadOnlyAsRuntime(plan, environment);

        assertEquals(plan.transactionId(), environment.transactionId());
        assertEquals(metadata, staged.artifact());
        assertEquals(metadata, publication.target());
        assertEquals(metadata, durability.artifact());
        assertEquals(staged, published);
        assertEquals(plan.policySha256(), probe.policySha256());
        assertEquals(List.of("resolve", "apply:FIXED_METADATA", "publish:REPLACE_EXISTING",
                "durable:FIXED_METADATA", "verify:FIXED_METADATA", "probe"), fake.calls);
    }

    @Test
    void everyAdapterFailureStopsTheContractSequence() {
        for (InstallationPermissionFailureReason reason
                : InstallationPermissionFailureReason.values()) {
            RecordingAdapter fake = new RecordingAdapter(reason);
            assertThrows(InstallationPermissionException.class,
                    () -> invokeContract(fake, CancellationTrustInstallationPlanTest.validPlan()));
            assertEquals(expectedLastCall(reason),
                    fake.calls.get(fake.calls.size() - 1), reason.toString());
            assertEquals(expectedCallCount(reason), fake.calls.size(), reason.toString());
        }
    }

    @Test
    void successfulEvidenceRejectsUnsupportedOrUnboundedClaims() {
        CancellationTrustInstallationPlan plan = CancellationTrustInstallationPlanTest.validPlan();
        InstallationArtifact policy = plan.artifact(
                InstallationArtifactKind.CONTENT_ADDRESSED_POLICY);

        assertThrows(IllegalArgumentException.class, () -> new AtomicPublicationEvidence(
                plan.transactionId(), policy, policy, PublicationMode.CREATE_EXCLUSIVE,
                "filesystem", false));
        assertThrows(IllegalArgumentException.class, () -> new DurabilityEvidence(
                plan.transactionId(), policy, "barrier", true, false));
        assertThrows(IllegalArgumentException.class, () -> new RuntimeProbeEvidence(
                plan.transactionId(), plan.principals().runtime(), "a".repeat(64),
                plan.policySha256(), true, true));
        assertThrows(IllegalArgumentException.class, () -> new InstallationPermissionException(
                InstallationPermissionFailureReason.TOPOLOGY_INVALID,
                "x".repeat(513)));
        assertThrows(IllegalArgumentException.class, () -> new InstallationEnvironmentEvidence(
                plan.transactionId(), "adapter", "version", plan.principals(),
                "x".repeat(257), true, true));
    }

    @Test
    void permissionEvidencePinsExactAllowedAndDeniedPartition() {
        CancellationTrustInstallationPlan plan = CancellationTrustInstallationPlanTest.validPlan();
        InstallationArtifact artifact = plan.artifact(InstallationArtifactKind.APPLICATION_JAR);
        ArtifactPermissionEvidence evidence = permissionEvidence(plan, artifact);

        for (InstallationPrincipalRole role : InstallationPrincipalRole.values()) {
            SetPair pair = new SetPair(
                    evidence.allowedEffectiveAccess().get(role),
                    evidence.deniedEffectiveAccess().get(role));
            assertTrue(pair.allowed().stream().noneMatch(pair.denied()::contains));
            assertEquals(EnumSet.allOf(InstallationAccess.class), union(pair));
            assertEquals(CancellationTrustInstallationPermissionPolicy
                    .rule(artifact.kind(), role).allowed(), pair.allowed());
        }
    }

    @Test
    void repeatedFakeVerificationIsValueEqualAndDoesNotChangeThePlan() throws Exception {
        CancellationTrustInstallationPlan plan = CancellationTrustInstallationPlanTest.validPlan();
        List<InstallationArtifact> before = plan.artifacts();
        RecordingAdapter fake = new RecordingAdapter(null);
        InstallationEnvironmentEvidence environment = fake.resolveAndVerify(plan);
        InstallationArtifact artifact = plan.artifact(InstallationArtifactKind.FIXED_METADATA);

        ArtifactPermissionEvidence first = fake.verifyPublished(plan, artifact, environment);
        ArtifactPermissionEvidence second = fake.verifyPublished(plan, artifact, environment);

        assertEquals(first, second);
        assertEquals(before, plan.artifacts());
        assertEquals(List.of("resolve", "verify:FIXED_METADATA", "verify:FIXED_METADATA"),
                fake.calls);
    }

    private static void invokeContract(
            InstallationPermissionAdapter adapter,
            CancellationTrustInstallationPlan plan) throws InstallationPermissionException {
        InstallationEnvironmentEvidence environment = adapter.resolveAndVerify(plan);
        InstallationArtifact artifact = plan.artifact(InstallationArtifactKind.FIXED_METADATA);
        adapter.applyAndVerify(plan, artifact, environment);
        adapter.publishAtomically(plan,
                plan.artifact(InstallationArtifactKind.METADATA_CANDIDATE), artifact,
                PublicationMode.REPLACE_EXISTING, environment);
        adapter.forceDurable(plan, artifact, environment);
        adapter.verifyPublished(plan, artifact, environment);
        adapter.probeReadOnlyAsRuntime(plan, environment);
    }

    private static ArtifactPermissionEvidence permissionEvidence(
            CancellationTrustInstallationPlan plan, InstallationArtifact artifact) {
        Map<InstallationPrincipalRole, java.util.Set<InstallationAccess>> allowed =
                new EnumMap<>(InstallationPrincipalRole.class);
        Map<InstallationPrincipalRole, java.util.Set<InstallationAccess>> denied =
                new EnumMap<>(InstallationPrincipalRole.class);
        for (InstallationPrincipalRole role : InstallationPrincipalRole.values()) {
            EnumSet<InstallationAccess> allow = EnumSet.copyOf(
                    CancellationTrustInstallationPermissionPolicy.rule(
                            artifact.kind(), role).allowed());
            EnumSet<InstallationAccess> deny = EnumSet.allOf(InstallationAccess.class);
            deny.removeAll(allow);
            allowed.put(role, allow);
            denied.put(role, deny);
        }
        return new ArtifactPermissionEvidence(plan.transactionId(), artifact,
                plan.permissionPolicyRevision(), plan.principals().installerPublisher()
                        .stableOperatingSystemIdentity(), allowed, denied);
    }

    private static EnumSet<InstallationAccess> union(SetPair pair) {
        EnumSet<InstallationAccess> result = EnumSet.noneOf(InstallationAccess.class);
        result.addAll(pair.allowed());
        result.addAll(pair.denied());
        return result;
    }

    private static String expectedLastCall(InstallationPermissionFailureReason reason) {
        return switch (reason) {
            case IDENTITY_RESOLUTION_FAILED, TOPOLOGY_INVALID -> "resolve";
            case PERMISSION_APPLICATION_FAILED,
                    EFFECTIVE_ACCESS_VERIFICATION_FAILED -> "apply:FIXED_METADATA";
            case PUBLICATION_FAILED -> "publish:REPLACE_EXISTING";
            case DURABILITY_FAILED -> "durable:FIXED_METADATA";
            case PUBLISHED_RECHECK_FAILED -> "verify:FIXED_METADATA";
            case RUNTIME_PROBE_FAILED -> "probe";
        };
    }

    private static int expectedCallCount(InstallationPermissionFailureReason reason) {
        return switch (reason) {
            case IDENTITY_RESOLUTION_FAILED, TOPOLOGY_INVALID -> 1;
            case PERMISSION_APPLICATION_FAILED,
                    EFFECTIVE_ACCESS_VERIFICATION_FAILED -> 2;
            case PUBLICATION_FAILED -> 3;
            case DURABILITY_FAILED -> 4;
            case PUBLISHED_RECHECK_FAILED -> 5;
            case RUNTIME_PROBE_FAILED -> 6;
        };
    }

    private record SetPair(
            java.util.Set<InstallationAccess> allowed,
            java.util.Set<InstallationAccess> denied) { }

    private static final class RecordingAdapter implements InstallationPermissionAdapter {
        private final InstallationPermissionFailureReason failure;
        private final List<String> calls = new ArrayList<>();

        private RecordingAdapter(InstallationPermissionFailureReason failure) {
            this.failure = failure;
        }

        @Override
        public InstallationEnvironmentEvidence resolveAndVerify(
                CancellationTrustInstallationPlan plan) throws InstallationPermissionException {
            call("resolve", InstallationPermissionFailureReason.IDENTITY_RESOLUTION_FAILED,
                    InstallationPermissionFailureReason.TOPOLOGY_INVALID);
            return new InstallationEnvironmentEvidence(plan.transactionId(), "fake-adapter",
                    "fake-v1", plan.principals(), "fake-filesystem", true, true);
        }

        @Override
        public ArtifactPermissionEvidence applyAndVerify(
                CancellationTrustInstallationPlan plan,
                InstallationArtifact artifact,
                InstallationEnvironmentEvidence environment)
                throws InstallationPermissionException {
            call("apply:" + artifact.kind(),
                    InstallationPermissionFailureReason.PERMISSION_APPLICATION_FAILED,
                    InstallationPermissionFailureReason.EFFECTIVE_ACCESS_VERIFICATION_FAILED);
            return permissionEvidence(plan, artifact);
        }

        @Override
        public AtomicPublicationEvidence publishAtomically(
                CancellationTrustInstallationPlan plan,
                InstallationArtifact staged,
                InstallationArtifact target,
                PublicationMode mode,
                InstallationEnvironmentEvidence environment)
                throws InstallationPermissionException {
            call("publish:" + mode, InstallationPermissionFailureReason.PUBLICATION_FAILED);
            return new AtomicPublicationEvidence(plan.transactionId(), staged, target, mode,
                    environment.filesystemIdentity(), true);
        }

        @Override
        public DurabilityEvidence forceDurable(
                CancellationTrustInstallationPlan plan,
                InstallationArtifact published,
                InstallationEnvironmentEvidence environment)
                throws InstallationPermissionException {
            call("durable:" + published.kind(),
                    InstallationPermissionFailureReason.DURABILITY_FAILED);
            return new DurabilityEvidence(plan.transactionId(), published,
                    "fake-file-and-parent", true, true);
        }

        @Override
        public ArtifactPermissionEvidence verifyPublished(
                CancellationTrustInstallationPlan plan,
                InstallationArtifact artifact,
                InstallationEnvironmentEvidence environment)
                throws InstallationPermissionException {
            call("verify:" + artifact.kind(),
                    InstallationPermissionFailureReason.PUBLISHED_RECHECK_FAILED);
            return permissionEvidence(plan, artifact);
        }

        @Override
        public RuntimeProbeEvidence probeReadOnlyAsRuntime(
                CancellationTrustInstallationPlan plan,
                InstallationEnvironmentEvidence environment)
                throws InstallationPermissionException {
            call("probe", InstallationPermissionFailureReason.RUNTIME_PROBE_FAILED);
            return new RuntimeProbeEvidence(plan.transactionId(), plan.principals().runtime(),
                    plan.expectedCurrentMetadataSha256().orElse(plan.policySha256()),
                    plan.policySha256(), true, false);
        }

        private void call(
                String name, InstallationPermissionFailureReason... matching)
                throws InstallationPermissionException {
            calls.add(name);
            for (InstallationPermissionFailureReason candidate : matching) {
                if (failure == candidate) {
                    throw new InstallationPermissionException(failure, "fake failure");
                }
            }
        }
    }
}
