package com.enhancer.model;

import java.util.Objects;
import java.util.Optional;

/**
 * Invocation-local aggregate for one exact outbound route proposal.
 *
 * <p>Construction validates intrinsic shape only. Policy-significant cross-bindings
 * remain visible to the RFC-0028 evaluator so mismatches fail closed with typed reasons.
 */
public record ModelOutboundRouteProposal(
        String schemaVersion,
        ModelRouteCandidateFacts candidateFacts,
        ModelDestinationPolicyFacts destinationPolicyFacts,
        ModelOutboundPurpose purpose,
        ModelRetentionFacts retentionFacts,
        ModelOutboundContent outboundContent,
        Optional<ModelContentInspectionFacts> sourceInspection,
        Optional<ModelContentInspectionFacts> outboundInspection,
        Optional<ModelContentRedactionFacts> redactionFacts,
        Optional<ModelRoutePricingFacts> pricingFacts) {

    public static final String SCHEMA_VERSION = "model-outbound-route-proposal-v1";

    public ModelOutboundRouteProposal {
        ModelOutboundRouteValueChecks.label(schemaVersion, "schemaVersion");
        Objects.requireNonNull(candidateFacts, "candidateFacts must not be null");
        Objects.requireNonNull(
                destinationPolicyFacts, "destinationPolicyFacts must not be null");
        Objects.requireNonNull(purpose, "purpose must not be null");
        Objects.requireNonNull(retentionFacts, "retentionFacts must not be null");
        Objects.requireNonNull(outboundContent, "outboundContent must not be null");
        Objects.requireNonNull(sourceInspection, "sourceInspection must not be null");
        Objects.requireNonNull(outboundInspection, "outboundInspection must not be null");
        Objects.requireNonNull(redactionFacts, "redactionFacts must not be null");
        Objects.requireNonNull(pricingFacts, "pricingFacts must not be null");
    }

    @Override
    public String toString() {
        return "ModelOutboundRouteProposal[schemaVersion=" + schemaVersion
                + ", candidateIdentity=" + candidateFacts.candidateIdentity()
                + ", destinationIdentity=" + destinationPolicyFacts.destinationIdentity()
                + ", purpose=" + purpose
                + ", retentionUses=" + retentionFacts.uses()
                + ", transformation=" + outboundContent.transformation()
                + ", sourceInspectionPresent=" + sourceInspection.isPresent()
                + ", outboundInspectionPresent=" + outboundInspection.isPresent()
                + ", redactionFactsPresent=" + redactionFacts.isPresent()
                + ", pricingFactsPresent=" + pricingFacts.isPresent() + "]";
    }
}
