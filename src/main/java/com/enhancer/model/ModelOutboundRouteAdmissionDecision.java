package com.enhancer.model;

import java.util.Objects;

/** Ephemeral pure RFC-0028 decision for one exact outbound route proposal. */
public sealed interface ModelOutboundRouteAdmissionDecision
        permits ModelOutboundRouteAdmissionDecision.Eligible,
                ModelOutboundRouteAdmissionDecision.Refused {

    /** Eligibility retaining the exact proposal instance and derived classification. */
    record Eligible(
            ModelOutboundRouteProposal proposal,
            ModelDataClassification effectiveClassification)
            implements ModelOutboundRouteAdmissionDecision {
        public Eligible {
            Objects.requireNonNull(proposal, "proposal must not be null");
            Objects.requireNonNull(
                    effectiveClassification,
                    "effectiveClassification must not be null");
        }

        @Override
        public String toString() {
            return "Eligible[effectiveClassification=" + effectiveClassification + "]";
        }
    }

    /** Fail-closed decision retaining only the first matching reason. */
    record Refused(ModelOutboundRouteRejectionReason reason)
            implements ModelOutboundRouteAdmissionDecision {
        public Refused {
            Objects.requireNonNull(reason, "reason must not be null");
        }
    }
}
