package com.enhancer.maintenance.installation;

/** Closed artifact classes governed by the installation permission matrix. */
public enum InstallationArtifactKind {
    INSTALLATION_ANCESTOR,
    APPLICATION_JAR,
    RUNTIME_DISTRIBUTION,
    OPERATOR_DISTRIBUTION,
    FIXED_METADATA,
    TRUST_DIRECTORY,
    CONTENT_ADDRESSED_POLICY,
    MAINTENANCE_LOCK,
    POLICY_CANDIDATE,
    METADATA_CANDIDATE,
    OPERATOR_CANDIDATE_INBOX,
    ACTIVATION_POINT,
    INSTALLATION_AUDIT_ROOT
}
