package com.enhancer.maintenance.installation;

/** Finite fail-closed reason for a platform adapter contract operation. */
public enum InstallationPermissionFailureReason {
    IDENTITY_RESOLUTION_FAILED,
    TOPOLOGY_INVALID,
    PERMISSION_APPLICATION_FAILED,
    EFFECTIVE_ACCESS_VERIFICATION_FAILED,
    PUBLICATION_FAILED,
    DURABILITY_FAILED,
    PUBLISHED_RECHECK_FAILED,
    RUNTIME_PROBE_FAILED
}
