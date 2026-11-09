package com.enhancer.model;

import java.util.Objects;

/** Closed result of one protected same-invocation remote route-option preparation. */
public sealed interface ProtectedRemoteModelRouteOptionPreparationResult
        permits ProtectedRemoteModelRouteOptionPreparationResult.Prepared,
                ProtectedRemoteModelRouteOptionPreparationResult.AcquisitionRefused,
                ProtectedRemoteModelRouteOptionPreparationResult.SuitabilityRefused {

    /** One exact invocation-local option prepared under one stable binding digest. */
    record Prepared(ModelRouteOption option, String bindingSha256)
            implements ProtectedRemoteModelRouteOptionPreparationResult {
        public Prepared {
            Objects.requireNonNull(option, "option must not be null");
            bindingSha256 = ProtectedRemoteModelRouteOptionPreparationResult
                    .bindingSha256(bindingSha256);
        }

        @Override
        public String toString() {
            return "Prepared[option=<redacted>, bindingSha256=<redacted>]";
        }
    }

    /** Exact RFC-0033 refusal, returned before suitability evaluation. */
    record AcquisitionRefused(
            ProtectedRemoteCapabilityCatalogFactSourceResult.Refused refusal)
            implements ProtectedRemoteModelRouteOptionPreparationResult {
        public AcquisitionRefused {
            Objects.requireNonNull(refusal, "refusal must not be null");
        }

        @Override
        public String toString() {
            return "AcquisitionRefused[reason=" + refusal.reason()
                    + ", sourceReason=" + refusal.sourceReason() + "]";
        }
    }

    /** Exact RFC-0030 refusal plus safe invocation-local binding provenance. */
    record SuitabilityRefused(
            RemoteModelCandidateSuitabilityRefusalReason reason,
            String bindingSha256)
            implements ProtectedRemoteModelRouteOptionPreparationResult {
        public SuitabilityRefused {
            Objects.requireNonNull(reason, "reason must not be null");
            bindingSha256 = ProtectedRemoteModelRouteOptionPreparationResult
                    .bindingSha256(bindingSha256);
        }

        @Override
        public String toString() {
            return "SuitabilityRefused[reason=" + reason
                    + ", bindingSha256=<redacted>]";
        }
    }

    private static String bindingSha256(String value) {
        Objects.requireNonNull(value, "bindingSha256 must not be null");
        if (!ProtectedRemoteCapabilityCatalogFactSourceResult.SHA_256
                .matcher(value).matches()) {
            throw new IllegalArgumentException(
                    "bindingSha256 must be a lowercase SHA-256 digest");
        }
        return value;
    }
}
