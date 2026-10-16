package com.enhancer.model;

import java.time.Instant;
import java.util.Objects;

/** Stateless pure RFC-0030 evaluator for one exact remote candidate and request. */
public final class RemoteModelCandidateSuitabilityEvaluator {

    /** Returns the first RFC-0030 refusal or a new invocation-local suitability proof. */
    public RemoteModelCandidateSuitabilityDecision evaluate(
            ProfiledModelRequest profiledRequest,
            ModelOutboundRouteAdmissionDecision.Eligible eligibility,
            RemoteModelCandidateCapabilityFacts capabilityFacts,
            Instant evaluationTime) {
        Objects.requireNonNull(profiledRequest, "profiledRequest must not be null");
        Objects.requireNonNull(eligibility, "eligibility must not be null");
        Objects.requireNonNull(capabilityFacts, "capabilityFacts must not be null");
        Objects.requireNonNull(evaluationTime, "evaluationTime must not be null");

        ModelOutboundRouteProposal proposal = eligibility.proposal();
        ModelRouteCandidateFacts candidate = proposal.candidateFacts();
        ModelExecutionProfile profile = profiledRequest.executionProfile();

        if (!proposal.outboundContent().sourcePromptDigest().equals(
                ModelOutboundContent.digestOf(profiledRequest.request().prompt()))) {
            return refused(RemoteModelCandidateSuitabilityRefusalReason
                    .SOURCE_CONTENT_BINDING_MISMATCH);
        }
        if (!ModelRouteCandidateFacts.SCHEMA_VERSION.equals(candidate.schemaVersion())
                || !fresh(candidate.observedAt(), candidate.expiresAt(), evaluationTime)) {
            return refused(RemoteModelCandidateSuitabilityRefusalReason
                    .CANDIDATE_FACTS_INVALID_OR_STALE);
        }
        if (!RemoteModelCandidateCapabilityFacts.SCHEMA_VERSION.equals(
                        capabilityFacts.schemaVersion())
                || !fresh(
                        capabilityFacts.observedAt(),
                        capabilityFacts.expiresAt(),
                        evaluationTime)) {
            return refused(RemoteModelCandidateSuitabilityRefusalReason
                    .CAPABILITY_FACTS_INVALID_OR_STALE);
        }

        ModelDestinationAllowRule candidateRule = candidateRule(proposal);
        if (!capabilityFacts.candidateIdentity().equals(candidate.candidateIdentity())
                || candidateRule == null) {
            return refused(RemoteModelCandidateSuitabilityRefusalReason
                    .CANDIDATE_BINDING_MISMATCH);
        }
        String destinationIdentity =
                proposal.destinationPolicyFacts().destinationIdentity();
        ModelDestinationAllowRule routeRule = matchingRule(proposal);
        if (!candidate.destinationIdentity().equals(destinationIdentity)
                || !capabilityFacts.destinationIdentity().equals(destinationIdentity)
                || routeRule == null) {
            return refused(RemoteModelCandidateSuitabilityRefusalReason
                    .DESTINATION_BINDING_MISMATCH);
        }
        if (!profile.modelClass().equals(candidate.modelClass())
                || !profile.modelClass().equals(capabilityFacts.modelClass())
                || !profile.requiredCapability().equals(candidate.requiredCapability())
                || !profile.requiredCapability().equals(
                        capabilityFacts.requiredCapability())) {
            return refused(RemoteModelCandidateSuitabilityRefusalReason
                    .PROFILE_BINDING_MISMATCH);
        }

        if (proposal.pricingFacts().isEmpty()) {
            return refused(RemoteModelCandidateSuitabilityRefusalReason
                    .PRICING_FACTS_INVALID_OR_STALE);
        }
        ModelRoutePricingFacts pricing = proposal.pricingFacts().orElseThrow();
        if (!ModelRoutePricingFacts.SCHEMA_VERSION.equals(pricing.schemaVersion())
                || !fresh(pricing.observedAt(), pricing.expiresAt(), evaluationTime)) {
            return refused(RemoteModelCandidateSuitabilityRefusalReason
                    .PRICING_FACTS_INVALID_OR_STALE);
        }
        if (!pricing.factSourceIdentity().equals(routeRule.pricingFactSourceIdentity())
                || !pricing.candidateIdentity().equals(candidate.candidateIdentity())
                || !pricing.destinationIdentity().equals(destinationIdentity)
                || !pricing.modelClass().equals(profile.modelClass())
                || !capabilityFacts.priceScheduleIdentity().equals(
                        pricing.priceScheduleIdentity())
                || !capabilityFacts.tokenBudgetBasis().equals(
                        pricing.tokenBudgetBasis())
                || !pricing.tokenBudgetBasis().equals(profile.tokenBudget())) {
            return refused(RemoteModelCandidateSuitabilityRefusalReason
                    .PRICING_BINDING_MISMATCH);
        }
        if (!ModelRoutePricingFacts.TOKEN_SEMANTICS_IDENTITY.equals(
                        pricing.tokenSemanticsIdentity())
                || !capabilityFacts.tokenSemanticsIdentity().equals(
                        pricing.tokenSemanticsIdentity())) {
            return refused(RemoteModelCandidateSuitabilityRefusalReason
                    .TOKEN_SEMANTICS_MISMATCH);
        }
        if (capabilityFacts.maximumReasoningRequirement().ordinal()
                < profile.reasoningRequirement().ordinal()) {
            return refused(RemoteModelCandidateSuitabilityRefusalReason
                    .REASONING_REQUIREMENT_UNSUPPORTED);
        }
        if (capabilityFacts.maximumContextTokens() < profile.minimumContextTokens()) {
            return refused(RemoteModelCandidateSuitabilityRefusalReason
                    .CONTEXT_CAPACITY_INSUFFICIENT);
        }
        ModelTokenBudget maximumTokens = capabilityFacts.maximumTokenBudget();
        ModelTokenBudget requiredTokens = profile.tokenBudget();
        if (maximumTokens.maxInputTokens() < requiredTokens.maxInputTokens()) {
            return refused(RemoteModelCandidateSuitabilityRefusalReason
                    .INPUT_TOKEN_CAPACITY_INSUFFICIENT);
        }
        if (maximumTokens.maxOutputTokens() < requiredTokens.maxOutputTokens()) {
            return refused(RemoteModelCandidateSuitabilityRefusalReason
                    .OUTPUT_TOKEN_CAPACITY_INSUFFICIENT);
        }
        if (maximumTokens.maxTotalTokens() < requiredTokens.maxTotalTokens()) {
            return refused(RemoteModelCandidateSuitabilityRefusalReason
                    .TOTAL_TOKEN_CAPACITY_INSUFFICIENT);
        }
        if (capabilityFacts.maximumInvocationTime().compareTo(
                profile.maximumInvocationTime()) < 0) {
            return refused(RemoteModelCandidateSuitabilityRefusalReason
                    .INVOCATION_TIME_UNSUPPORTED);
        }
        if (capabilityFacts.maximumDataClassification().ordinal()
                < eligibility.effectiveClassification().ordinal()) {
            return refused(RemoteModelCandidateSuitabilityRefusalReason
                    .DATA_CLASSIFICATION_UNSUPPORTED);
        }
        if (!capabilityFacts.costCurrencyCode().equals(pricing.currencyCode())
                || !capabilityFacts.costCurrencyCode().equals(
                        profile.costBudget().currencyCode())) {
            return refused(RemoteModelCandidateSuitabilityRefusalReason
                    .COST_CURRENCY_MISMATCH);
        }
        if (pricing.worstCaseChargeMicrounits()
                        > capabilityFacts.maximumCostMicrounits()
                || capabilityFacts.maximumCostMicrounits()
                        > profile.costBudget().maxMicrounits()) {
            return refused(RemoteModelCandidateSuitabilityRefusalReason
                    .COST_CEILING_EXCEEDED);
        }

        return new RemoteModelCandidateSuitabilityDecision.Suitable(
                RemoteModelCandidateSuitability.create(
                        RemoteModelCandidateSuitability.CONTRACT_VERSION,
                        profiledRequest,
                        eligibility,
                        candidate,
                        capabilityFacts.factSourceIdentity(),
                        capabilityFacts.sourceRevision(),
                        capabilityFacts.observedAt(),
                        capabilityFacts.expiresAt(),
                        capabilityFacts.candidateIdentity(),
                        capabilityFacts.destinationIdentity(),
                        capabilityFacts.tokenSemanticsIdentity(),
                        capabilityFacts.capacityEvidenceIdentity(),
                        capabilityFacts.modelClass(),
                        capabilityFacts.requiredCapability(),
                        capabilityFacts.maximumReasoningRequirement(),
                        capabilityFacts.maximumContextTokens(),
                        capabilityFacts.maximumTokenBudget(),
                        capabilityFacts.maximumInvocationTime(),
                        capabilityFacts.costCurrencyCode(),
                        capabilityFacts.maximumCostMicrounits(),
                        capabilityFacts.maximumDataClassification()));
    }

    private static ModelDestinationAllowRule candidateRule(
            ModelOutboundRouteProposal proposal) {
        String candidateIdentity = proposal.candidateFacts().candidateIdentity();
        for (ModelDestinationAllowRule rule
                : proposal.destinationPolicyFacts().allowRules()) {
            if (rule.candidateIdentity().equals(candidateIdentity)
                    && rule.permittedPurposes().contains(proposal.purpose())) {
                return rule;
            }
        }
        return null;
    }

    private static ModelDestinationAllowRule matchingRule(
            ModelOutboundRouteProposal proposal) {
        String candidateIdentity = proposal.candidateFacts().candidateIdentity();
        String destinationIdentity =
                proposal.destinationPolicyFacts().destinationIdentity();
        for (ModelDestinationAllowRule rule
                : proposal.destinationPolicyFacts().allowRules()) {
            if (rule.candidateIdentity().equals(candidateIdentity)
                    && rule.destinationIdentity().equals(destinationIdentity)
                    && rule.permittedPurposes().contains(proposal.purpose())) {
                return rule;
            }
        }
        return null;
    }

    private static boolean fresh(
            Instant observedAt,
            Instant expiresAt,
            Instant evaluationTime) {
        return !observedAt.isAfter(evaluationTime) && evaluationTime.isBefore(expiresAt);
    }

    private static RemoteModelCandidateSuitabilityDecision refused(
            RemoteModelCandidateSuitabilityRefusalReason reason) {
        return new RemoteModelCandidateSuitabilityDecision.Refused(reason);
    }
}
