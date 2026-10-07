package com.enhancer.model;

import java.util.Objects;

/** One immutable RFC-0029 route option retaining exact request and proof identities. */
public record ModelRouteOption(
        ProfiledModelRequest profiledRequest,
        ModelOutboundRouteAdmissionDecision.Eligible eligibility,
        RemoteModelCandidateSuitability suitability) {

    public ModelRouteOption {
        Objects.requireNonNull(profiledRequest, "profiledRequest must not be null");
        Objects.requireNonNull(eligibility, "eligibility must not be null");
        Objects.requireNonNull(suitability, "suitability must not be null");
    }

    @Override
    public String toString() {
        return "ModelRouteOption[suitabilityContractVersion="
                + suitability.contractVersion() + "]";
    }
}
