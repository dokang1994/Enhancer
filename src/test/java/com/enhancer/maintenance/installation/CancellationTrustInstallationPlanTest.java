package com.enhancer.maintenance.installation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

public class CancellationTrustInstallationPlanTest {
    private static final String A = "a".repeat(64);
    private static final String B = "b".repeat(64);
    private static final String C = "c".repeat(64);
    private static final String D = "d".repeat(64);
    private static final String E = "e".repeat(64);
    private static final Path ROOT = Path.of(System.getProperty("java.io.tmpdir"))
            .toAbsolutePath().normalize().resolve("enhancer-installation-contract-values");

    @Test
    void validPlanDerivesProtectedTrustArtifactsAndExactOrder() {
        CancellationTrustInstallationPlan plan = validPlan();

        assertEquals(ROOT.resolve("runtime-v1/lib/enhancer.jar"),
                plan.artifact(InstallationArtifactKind.APPLICATION_JAR).path());
        assertEquals(ROOT.resolve("runtime-v1/lib/enhancer-cancellation-trust-metadata-v1"),
                plan.artifact(InstallationArtifactKind.FIXED_METADATA).path());
        assertEquals(ROOT.resolve("runtime-v1/lib/enhancer-cancellation-trust-policies-v1"),
                plan.artifact(InstallationArtifactKind.TRUST_DIRECTORY).path());
        assertEquals(ROOT.resolve("runtime-v1/lib/enhancer-cancellation-trust-maintenance-v1.lock"),
                plan.artifact(InstallationArtifactKind.MAINTENANCE_LOCK).path());
        assertEquals(ROOT.resolve("runtime-v1/lib/enhancer-cancellation-trust-policies-v1/"
                        + "enhancer-cancellation-trust-policy-" + E + ".conf"),
                plan.artifact(InstallationArtifactKind.CONTENT_ADDRESSED_POLICY).path());
        assertEquals(InstallationPhase.requiredOrder(), plan.requiredOrder());
        assertEquals(InstallationPhase.RECORD_FINAL_EVIDENCE,
                plan.requiredOrder().get(plan.requiredOrder().size() - 1));
        assertTrue(plan.requiredOrder().indexOf(InstallationPhase.PUBLISH_POLICY)
                < plan.requiredOrder().indexOf(InstallationPhase.PUBLISH_METADATA));
        assertTrue(plan.requiredOrder().indexOf(InstallationPhase.PUBLISH_METADATA)
                < plan.requiredOrder().indexOf(InstallationPhase.PROBE_AS_RUNTIME));
        assertTrue(plan.requiredOrder().indexOf(InstallationPhase.PROBE_AS_RUNTIME)
                < plan.requiredOrder().indexOf(InstallationPhase.ACTIVATE));
        assertEquals(InstallationArtifactKind.values().length, plan.artifacts().size());
        assertEquals(List.of(InstallationArtifactKind.values()),
                plan.artifacts().stream().map(InstallationArtifact::kind).toList());
    }

    @Test
    void principalsMustBeBoundedControlFreeRoleCorrectAndDistinct() {
        assertThrows(IllegalArgumentException.class,
                () -> new InstallationPrincipal(InstallationPrincipalRole.OPERATOR, " "));
        assertThrows(IllegalArgumentException.class,
                () -> new InstallationPrincipal(InstallationPrincipalRole.OPERATOR, "sid\nvalue"));
        assertThrows(IllegalArgumentException.class,
                () -> new InstallationPrincipal(
                        InstallationPrincipalRole.OPERATOR, "x".repeat(257)));

        InstallationPrincipal sameInstaller = new InstallationPrincipal(
                InstallationPrincipalRole.INSTALLER_PUBLISHER, "stable-i");
        assertThrows(IllegalArgumentException.class, () -> new InstallationPrincipalSet(
                sameInstaller,
                new InstallationPrincipal(InstallationPrincipalRole.OPERATOR, "stable-i"),
                new InstallationPrincipal(InstallationPrincipalRole.RUNTIME, "stable-r")));
        assertThrows(IllegalArgumentException.class, () -> new InstallationPrincipalSet(
                new InstallationPrincipal(InstallationPrincipalRole.OPERATOR, "wrong-role"),
                new InstallationPrincipal(InstallationPrincipalRole.INSTALLER_PUBLISHER, "i"),
                new InstallationPrincipal(InstallationPrincipalRole.RUNTIME, "r")));
    }

    @Test
    void planRejectsUntrustedOrInconsistentValues() {
        CancellationTrustInstallationPlan valid = validPlan();

        assertThrows(IllegalArgumentException.class, () -> copy(valid,
                ROOT.resolve("relative/../relative"), valid.runtimeDistributionRoot(),
                valid.operatorDistributionRoot(), valid.operation(),
                valid.permissionPolicyRevision(), valid.policySha256(),
                valid.expectedCurrentMetadataSha256()));
        assertThrows(IllegalArgumentException.class, () -> copy(valid,
                valid.applicationJar(), valid.runtimeDistributionRoot(),
                valid.runtimeDistributionRoot().resolve("operator"), valid.operation(),
                valid.permissionPolicyRevision(), valid.policySha256(),
                valid.expectedCurrentMetadataSha256()));
        assertThrows(IllegalArgumentException.class, () -> copy(valid,
                valid.applicationJar(), valid.runtimeDistributionRoot(),
                valid.operatorDistributionRoot(), valid.operation(), "wrong-revision",
                valid.policySha256(), valid.expectedCurrentMetadataSha256()));
        assertThrows(IllegalArgumentException.class, () -> copy(valid,
                valid.applicationJar(), valid.runtimeDistributionRoot(),
                valid.operatorDistributionRoot(), valid.operation(),
                valid.permissionPolicyRevision(), "ABC", valid.expectedCurrentMetadataSha256()));
        assertThrows(IllegalArgumentException.class, () -> copy(valid,
                valid.applicationJar(), valid.runtimeDistributionRoot(),
                valid.operatorDistributionRoot(), InstallationOperation.INSTALL,
                valid.permissionPolicyRevision(), valid.policySha256(), Optional.of(A)));
        assertThrows(IllegalArgumentException.class, () -> copy(valid,
                valid.applicationJar(), valid.runtimeDistributionRoot(),
                valid.operatorDistributionRoot(), InstallationOperation.ROTATE,
                valid.permissionPolicyRevision(), valid.policySha256(), Optional.empty()));
        assertThrows(IllegalArgumentException.class, () -> new CancellationTrustInstallationPlan(
                valid.transactionId(), valid.operation(), valid.principals(),
                valid.installationRoot(), valid.applicationJar(),
                valid.runtimeDistributionRoot(), valid.operatorDistributionRoot(),
                valid.operatorCandidateInbox(), valid.activationPoint(),
                valid.operatorCandidateInbox(), valid.sourceManifestSha256(),
                valid.applicationJarSha256(), valid.runtimeDistributionSha256(),
                valid.operatorDistributionSha256(), valid.permissionPolicyRevision(),
                valid.policySha256(), valid.requestedMetadataSha256(),
                valid.expectedCurrentMetadataSha256()));
    }

    @Test
    void planIsPureAndDoesNotInferInstallationSuccess() {
        CancellationTrustInstallationPlan plan = validPlan();

        assertFalse(plan.getClass().getSimpleName().toLowerCase().contains("result"));
        assertEquals(plan.artifacts(), plan.artifacts());
        assertEquals(plan.requiredOrder(), List.copyOf(plan.requiredOrder()));
    }

    public static CancellationTrustInstallationPlan validPlan() {
        return new CancellationTrustInstallationPlan(
                UUID.fromString("00000000-0000-0000-0000-000000000123"),
                InstallationOperation.ROTATE,
                new InstallationPrincipalSet(
                        new InstallationPrincipal(
                                InstallationPrincipalRole.INSTALLER_PUBLISHER, "stable-i"),
                        new InstallationPrincipal(
                                InstallationPrincipalRole.OPERATOR, "stable-o"),
                        new InstallationPrincipal(
                                InstallationPrincipalRole.RUNTIME, "stable-r")),
                ROOT,
                ROOT.resolve("runtime-v1/lib/enhancer.jar"),
                ROOT.resolve("runtime-v1"),
                ROOT.resolve("operator-v1"),
                ROOT.resolve("operator-inbox"),
                ROOT.resolve("active"),
                ROOT.resolve("audit"),
                A, B, C, D,
                CancellationTrustInstallationPermissionPolicy.REVISION,
                E, B,
                Optional.of(A));
    }

    private static CancellationTrustInstallationPlan copy(
            CancellationTrustInstallationPlan source,
            Path applicationJar,
            Path runtimeRoot,
            Path operatorRoot,
            InstallationOperation operation,
            String revision,
            String policySha256,
            Optional<String> expectedCurrent) {
        return new CancellationTrustInstallationPlan(
                source.transactionId(), operation, source.principals(), source.installationRoot(),
                applicationJar, runtimeRoot, operatorRoot, source.operatorCandidateInbox(),
                source.activationPoint(), source.auditRoot(), source.sourceManifestSha256(),
                source.applicationJarSha256(), source.runtimeDistributionSha256(),
                source.operatorDistributionSha256(), revision, policySha256,
                source.requestedMetadataSha256(), expectedCurrent);
    }
}
