package com.enhancer.maintenance.installation;

import java.util.List;

/** Exact fail-closed order required before an installation can be reported externally. */
public enum InstallationPhase {
    RESOLVE_PRINCIPALS,
    VERIFY_SOURCE_AND_TOPOLOGY,
    STAGE_PRIVATE_ARTIFACTS,
    APPLY_AND_VERIFY_STAGED_PERMISSIONS,
    PREPARE_TRUST_DIRECTORY_AND_LOCK,
    PUBLISH_POLICY,
    PUBLISH_METADATA,
    VERIFY_FINAL_BYTES_AND_PERMISSIONS,
    PROBE_AS_RUNTIME,
    ACTIVATE,
    RECORD_FINAL_EVIDENCE;

    private static final List<InstallationPhase> REQUIRED_ORDER = List.of(values());

    public static List<InstallationPhase> requiredOrder() {
        return REQUIRED_ORDER;
    }
}
