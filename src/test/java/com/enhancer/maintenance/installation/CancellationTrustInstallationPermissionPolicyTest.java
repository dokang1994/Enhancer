package com.enhancer.maintenance.installation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class CancellationTrustInstallationPermissionPolicyTest {
    private static final Set<InstallationAccess> MUTATION = EnumSet.of(
            InstallationAccess.CREATE,
            InstallationAccess.WRITE,
            InstallationAccess.REPLACE,
            InstallationAccess.RENAME,
            InstallationAccess.DELETE,
            InstallationAccess.DELETE_CHILD,
            InstallationAccess.CHANGE_OWNER,
            InstallationAccess.CHANGE_PERMISSIONS);

    @Test
    void matrixHasExactlyOneImmutableRuleForEveryArtifactAndPrincipal() {
        var rules = CancellationTrustInstallationPermissionPolicy.rules();

        assertEquals(InstallationArtifactKind.values().length
                * InstallationPrincipalRole.values().length, rules.size());
        Set<String> identities = new HashSet<>();
        for (InstallationPermissionRule rule : rules) {
            assertTrue(identities.add(rule.artifactKind() + ":" + rule.principalRole()));
            assertEquals(rule, CancellationTrustInstallationPermissionPolicy.rule(
                    rule.artifactKind(), rule.principalRole()));
            assertThrows(UnsupportedOperationException.class,
                    () -> rule.allowed().add(InstallationAccess.DELETE));
        }
    }

    @Test
    void operatorAndRuntimeCannotMutateProtectedArtifacts() {
        for (InstallationArtifactKind kind : InstallationArtifactKind.values()) {
            if (kind == InstallationArtifactKind.OPERATOR_CANDIDATE_INBOX) {
                continue;
            }
            for (InstallationPrincipalRole role : Set.of(
                    InstallationPrincipalRole.OPERATOR,
                    InstallationPrincipalRole.RUNTIME)) {
                assertTrue(disjoint(rule(kind, role), MUTATION), kind + " / " + role);
            }
        }
    }

    @Test
    void runtimeCannotAccessOperatorMaintenanceOrAuditArtifacts() {
        for (InstallationArtifactKind kind : Set.of(
                InstallationArtifactKind.OPERATOR_DISTRIBUTION,
                InstallationArtifactKind.MAINTENANCE_LOCK,
                InstallationArtifactKind.POLICY_CANDIDATE,
                InstallationArtifactKind.METADATA_CANDIDATE,
                InstallationArtifactKind.OPERATOR_CANDIDATE_INBOX,
                InstallationArtifactKind.INSTALLATION_AUDIT_ROOT)) {
            assertTrue(rule(kind, InstallationPrincipalRole.RUNTIME).isEmpty(), kind.toString());
        }
    }

    @Test
    void policyAndMetadataPublicationRemainFailClosed() {
        Set<InstallationAccess> policy = rule(
                InstallationArtifactKind.CONTENT_ADDRESSED_POLICY,
                InstallationPrincipalRole.INSTALLER_PUBLISHER);
        assertTrue(policy.contains(InstallationAccess.CREATE));
        assertFalse(policy.contains(InstallationAccess.WRITE));
        assertFalse(policy.contains(InstallationAccess.REPLACE));
        assertFalse(policy.contains(InstallationAccess.DELETE));

        Set<InstallationAccess> metadata = rule(
                InstallationArtifactKind.FIXED_METADATA,
                InstallationPrincipalRole.INSTALLER_PUBLISHER);
        assertTrue(metadata.contains(InstallationAccess.REPLACE));
        assertFalse(metadata.contains(InstallationAccess.WRITE));
    }

    @Test
    void ordinaryInstallAndRotateRulesGrantNoDeletion() {
        for (InstallationPermissionRule rule
                : CancellationTrustInstallationPermissionPolicy.rules()) {
            assertFalse(rule.allowed().contains(InstallationAccess.DELETE),
                    rule.artifactKind() + " / " + rule.principalRole());
        }
    }

    private static Set<InstallationAccess> rule(
            InstallationArtifactKind kind, InstallationPrincipalRole role) {
        return CancellationTrustInstallationPermissionPolicy.rule(kind, role).allowed();
    }

    private static boolean disjoint(
            Set<InstallationAccess> left, Set<InstallationAccess> right) {
        return left.stream().noneMatch(right::contains);
    }
}
