package com.enhancer.maintenance;

/** Finite maintenance failures; each reason owns its only valid category. */
public enum CancellationTrustMaintenanceFailureReason {
    INVALID_INSTALLATION(CancellationTrustMaintenanceFailureCategory.CONFIGURATION),
    INVALID_CANDIDATE_POLICY(CancellationTrustMaintenanceFailureCategory.CONFIGURATION),
    INVALID_CURRENT_BINDING(CancellationTrustMaintenanceFailureCategory.CONFIGURATION),
    EXISTING_BINDING(CancellationTrustMaintenanceFailureCategory.REFUSAL),
    LOCK_CONTENDED(CancellationTrustMaintenanceFailureCategory.REFUSAL),
    STALE_CURRENT_METADATA(CancellationTrustMaintenanceFailureCategory.REFUSAL),
    POLICY_COLLISION(CancellationTrustMaintenanceFailureCategory.REFUSAL),
    LOCK_FAILED(CancellationTrustMaintenanceFailureCategory.DURABILITY),
    CANDIDATE_WRITE_FAILED(CancellationTrustMaintenanceFailureCategory.DURABILITY),
    PUBLICATION_FAILED(CancellationTrustMaintenanceFailureCategory.DURABILITY),
    POST_SWITCH_VERIFICATION_FAILED(CancellationTrustMaintenanceFailureCategory.DURABILITY);

    private final CancellationTrustMaintenanceFailureCategory category;

    CancellationTrustMaintenanceFailureReason(
            CancellationTrustMaintenanceFailureCategory category) {
        this.category = category;
    }

    public CancellationTrustMaintenanceFailureCategory category() {
        return category;
    }
}
