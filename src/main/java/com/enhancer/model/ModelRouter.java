package com.enhancer.model;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Stateless pure RFC-0029 provider-neutral route selector. */
public final class ModelRouter {

    /** Validates the complete bounded set and selects ordinal zero only after it passes. */
    public ModelRouteSelectionDecision evaluate(
            ProfiledModelRequest profiledRequest,
            ModelRouteOptionSet optionSet,
            Instant evaluationTime) {
        Objects.requireNonNull(profiledRequest, "profiledRequest must not be null");
        Objects.requireNonNull(optionSet, "optionSet must not be null");
        Objects.requireNonNull(evaluationTime, "evaluationTime must not be null");

        List<ModelRouteOption> options = optionSet.options();
        if (options.isEmpty()) {
            return refused(ModelRouteSelectionRefusalReason.NO_ROUTE_OPTIONS, -1);
        }

        for (int ordinal = 0; ordinal < options.size(); ordinal++) {
            ModelRouteOption option = options.get(ordinal);
            if (option.profiledRequest() != profiledRequest
                    || option.suitability().profiledRequest() != profiledRequest) {
                return refused(
                        ModelRouteSelectionRefusalReason.REQUEST_BINDING_MISMATCH,
                        ordinal);
            }
        }
        for (int ordinal = 0; ordinal < options.size(); ordinal++) {
            ModelOutboundContent content =
                    options.get(ordinal).eligibility().proposal().outboundContent();
            if (!content.sourcePromptDigest().equals(
                    ModelOutboundContent.digestOf(profiledRequest.request().prompt()))) {
                return refused(
                        ModelRouteSelectionRefusalReason.SOURCE_CONTENT_BINDING_MISMATCH,
                        ordinal);
            }
        }
        for (int ordinal = 0; ordinal < options.size(); ordinal++) {
            if (!profileBindingsMatch(profiledRequest, options.get(ordinal))) {
                return refused(
                        ModelRouteSelectionRefusalReason.PROFILE_BINDING_MISMATCH,
                        ordinal);
            }
        }
        for (int ordinal = 0; ordinal < options.size(); ordinal++) {
            if (!candidateBindingsMatch(options.get(ordinal))) {
                return refused(
                        ModelRouteSelectionRefusalReason.CANDIDATE_BINDING_MISMATCH,
                        ordinal);
            }
        }
        for (int ordinal = 0; ordinal < options.size(); ordinal++) {
            if (!destinationBindingsMatch(options.get(ordinal))) {
                return refused(
                        ModelRouteSelectionRefusalReason.DESTINATION_BINDING_MISMATCH,
                        ordinal);
            }
        }
        for (int ordinal = 0; ordinal < options.size(); ordinal++) {
            if (!validEligibility(profiledRequest, options.get(ordinal), evaluationTime)) {
                return refused(
                        ModelRouteSelectionRefusalReason.ELIGIBILITY_INVALID_OR_STALE,
                        ordinal);
            }
        }
        for (int ordinal = 0; ordinal < options.size(); ordinal++) {
            if (!validSuitability(profiledRequest, options.get(ordinal), evaluationTime)) {
                return refused(
                        ModelRouteSelectionRefusalReason.SUITABILITY_INVALID_OR_STALE,
                        ordinal);
            }
        }

        Set<String> routeIdentities = new HashSet<>();
        for (int ordinal = 0; ordinal < options.size(); ordinal++) {
            ModelRouteCandidateFacts candidate =
                    options.get(ordinal).eligibility().proposal().candidateFacts();
            String routeIdentity = candidate.candidateIdentity()
                    + "\u0000" + candidate.destinationIdentity();
            if (!routeIdentities.add(routeIdentity)) {
                return refused(
                        ModelRouteSelectionRefusalReason.DUPLICATE_ROUTE_IDENTITY,
                        ordinal);
            }
        }

        ModelRouteOption selectedOption = options.get(0);
        ModelOutboundRouteAdmissionDecision.Eligible eligibility =
                selectedOption.eligibility();
        return new ModelRouteSelectionDecision.Selected(SelectedModelRoute.create(
                profiledRequest,
                selectedOption,
                eligibility,
                selectedOption.suitability(),
                eligibility.proposal().candidateFacts(),
                eligibility.proposal().destinationPolicyFacts(),
                0));
    }

    private static boolean profileBindingsMatch(
            ProfiledModelRequest request,
            ModelRouteOption option) {
        ModelExecutionProfile profile = request.executionProfile();
        ModelRouteCandidateFacts candidate = option.eligibility().proposal().candidateFacts();
        RemoteModelCandidateSuitability suitability = option.suitability();
        if (!profile.modelClass().equals(candidate.modelClass())
                || !profile.modelClass().equals(suitability.modelClass())
                || !profile.requiredCapability().equals(candidate.requiredCapability())
                || !profile.requiredCapability().equals(suitability.requiredCapability())) {
            return false;
        }
        return option.eligibility().proposal().pricingFacts()
                .map(pricing -> profile.modelClass().equals(pricing.modelClass()))
                .orElse(true);
    }

    private static boolean candidateBindingsMatch(ModelRouteOption option) {
        ModelOutboundRouteAdmissionDecision.Eligible eligibility = option.eligibility();
        ModelOutboundRouteProposal proposal = eligibility.proposal();
        ModelRouteCandidateFacts candidate = proposal.candidateFacts();
        RemoteModelCandidateSuitability suitability = option.suitability();
        if (suitability.eligibility() != eligibility
                || suitability.candidateFacts() != candidate
                || !suitability.candidateIdentity().equals(candidate.candidateIdentity())) {
            return false;
        }
        if (proposal.pricingFacts().isPresent()
                && !proposal.pricingFacts().orElseThrow().candidateIdentity()
                        .equals(candidate.candidateIdentity())) {
            return false;
        }
        for (ModelDestinationAllowRule rule
                : proposal.destinationPolicyFacts().allowRules()) {
            if (rule.candidateIdentity().equals(candidate.candidateIdentity())
                    && rule.permittedPurposes().contains(proposal.purpose())) {
                return true;
            }
        }
        return false;
    }

    private static boolean destinationBindingsMatch(ModelRouteOption option) {
        ModelOutboundRouteProposal proposal = option.eligibility().proposal();
        ModelRouteCandidateFacts candidate = proposal.candidateFacts();
        ModelDestinationPolicyFacts destination = proposal.destinationPolicyFacts();
        RemoteModelCandidateSuitability suitability = option.suitability();
        if (!candidate.destinationIdentity().equals(destination.destinationIdentity())
                || !suitability.destinationIdentity().equals(
                        destination.destinationIdentity())) {
            return false;
        }
        if (proposal.pricingFacts().isPresent()
                && !proposal.pricingFacts().orElseThrow().destinationIdentity()
                        .equals(destination.destinationIdentity())) {
            return false;
        }
        return matchingRule(proposal) != null;
    }

    private static boolean validEligibility(
            ProfiledModelRequest request,
            ModelRouteOption option,
            Instant evaluationTime) {
        ModelOutboundRouteAdmissionDecision.Eligible eligibility = option.eligibility();
        ModelOutboundRouteProposal proposal = eligibility.proposal();
        ModelRouteCandidateFacts candidate = proposal.candidateFacts();
        ModelDestinationPolicyFacts destination = proposal.destinationPolicyFacts();
        ModelOutboundContent content = proposal.outboundContent();
        if (!ModelOutboundRouteProposal.SCHEMA_VERSION.equals(proposal.schemaVersion())
                || !ModelRouteCandidateFacts.SCHEMA_VERSION.equals(candidate.schemaVersion())
                || !ModelDestinationPolicyFacts.SCHEMA_VERSION.equals(
                        destination.schemaVersion())
                || !ModelOutboundContent.SCHEMA_VERSION.equals(content.schemaVersion())
                || !fresh(candidate.observedAt(), candidate.expiresAt(), evaluationTime)
                || !fresh(destination.observedAt(), destination.expiresAt(), evaluationTime)
                || !content.outboundPromptDigest().equals(
                        ModelOutboundContent.digestOf(content.outboundPrompt()))) {
            return false;
        }
        String sourcePrompt = request.request().prompt();
        if ((content.transformation() == ModelOutboundContentTransformation.UNCHANGED
                        && !content.outboundPrompt().equals(sourcePrompt))
                || (content.transformation() == ModelOutboundContentTransformation.REDACTED
                        && content.outboundPrompt().equals(sourcePrompt))) {
            return false;
        }

        if (proposal.sourceInspection().isEmpty()
                || proposal.outboundInspection().isEmpty()) {
            return false;
        }
        ModelContentInspectionFacts sourceInspection =
                proposal.sourceInspection().orElseThrow();
        ModelContentInspectionFacts outboundInspection =
                proposal.outboundInspection().orElseThrow();
        if (!validInspection(
                        sourceInspection,
                        content.sourcePromptDigest(),
                        evaluationTime)
                || !validInspection(
                        outboundInspection,
                        content.outboundPromptDigest(),
                        evaluationTime)
                || sourceInspection.status() == ModelContentInspectionStatus.FAILED
                || outboundInspection.status() != ModelContentInspectionStatus.CLEAR) {
            return false;
        }

        boolean redacted = content.transformation()
                == ModelOutboundContentTransformation.REDACTED;
        if (sourceInspection.status()
                == ModelContentInspectionStatus.REDACTION_REQUIRED) {
            if (!redacted || proposal.redactionFacts().isEmpty()) {
                return false;
            }
        } else if (redacted || proposal.redactionFacts().isPresent()) {
            return false;
        }
        if (redacted && !validRedaction(
                proposal.redactionFacts().orElseThrow(),
                sourceInspection,
                content,
                evaluationTime)) {
            return false;
        }

        ModelDestinationAllowRule rule = matchingRule(proposal);
        if (rule == null
                || !rule.permittedRetentionUses().containsAll(
                        proposal.retentionFacts().uses())
                || proposal.retentionFacts().maximumDuration().compareTo(
                        rule.maximumRetentionDuration()) > 0) {
            return false;
        }
        ModelDataClassification effectiveClassification = redacted
                ? maximum(
                        request.executionProfile().dataClassification(),
                        outboundInspection.classification())
                : maximum(
                        request.executionProfile().dataClassification(),
                        sourceInspection.classification(),
                        outboundInspection.classification());
        if (effectiveClassification != eligibility.effectiveClassification()
                || effectiveClassification.ordinal()
                        > rule.maximumDataClassification().ordinal()) {
            return false;
        }

        if (proposal.pricingFacts().isEmpty()) {
            return false;
        }
        ModelRoutePricingFacts pricing = proposal.pricingFacts().orElseThrow();
        ModelCostBudget costBudget = request.executionProfile().costBudget();
        return ModelRoutePricingFacts.SCHEMA_VERSION.equals(pricing.schemaVersion())
                && fresh(pricing.observedAt(), pricing.expiresAt(), evaluationTime)
                && pricing.factSourceIdentity().equals(rule.pricingFactSourceIdentity())
                && pricing.tokenSemanticsIdentity().equals(
                        ModelRoutePricingFacts.TOKEN_SEMANTICS_IDENTITY)
                && pricing.tokenBudgetBasis().equals(
                        request.executionProfile().tokenBudget())
                && pricing.currencyCode().equals(costBudget.currencyCode())
                && pricing.worstCaseChargeMicrounits() <= costBudget.maxMicrounits()
                && pricing.worstCaseChargeMicrounits() == 0;
    }

    private static boolean validSuitability(
            ProfiledModelRequest request,
            ModelRouteOption option,
            Instant evaluationTime) {
        RemoteModelCandidateSuitability suitability = option.suitability();
        ModelExecutionProfile profile = request.executionProfile();
        ModelTokenBudget requiredTokens = profile.tokenBudget();
        ModelTokenBudget maximumTokens = suitability.maximumTokenBudget();
        ModelRoutePricingFacts pricing =
                option.eligibility().proposal().pricingFacts().orElseThrow();
        return RemoteModelCandidateSuitability.CONTRACT_VERSION.equals(
                        suitability.contractVersion())
                && fresh(suitability.observedAt(), suitability.expiresAt(), evaluationTime)
                && suitability.tokenSemanticsIdentity().equals(
                        pricing.tokenSemanticsIdentity())
                && suitability.maximumReasoningRequirement().ordinal()
                        >= profile.reasoningRequirement().ordinal()
                && suitability.maximumContextTokens() >= profile.minimumContextTokens()
                && maximumTokens.maxInputTokens() >= requiredTokens.maxInputTokens()
                && maximumTokens.maxOutputTokens() >= requiredTokens.maxOutputTokens()
                && maximumTokens.maxTotalTokens() >= requiredTokens.maxTotalTokens()
                && suitability.maximumInvocationTime().compareTo(
                        profile.maximumInvocationTime()) >= 0
                && suitability.costCurrencyCode().equals(
                        profile.costBudget().currencyCode())
                && suitability.maximumCostMicrounits()
                        <= profile.costBudget().maxMicrounits()
                && pricing.worstCaseChargeMicrounits()
                        <= suitability.maximumCostMicrounits()
                && suitability.maximumDataClassification().ordinal()
                        >= option.eligibility().effectiveClassification().ordinal();
    }

    private static ModelDestinationAllowRule matchingRule(
            ModelOutboundRouteProposal proposal) {
        ModelRouteCandidateFacts candidate = proposal.candidateFacts();
        String destinationIdentity =
                proposal.destinationPolicyFacts().destinationIdentity();
        for (ModelDestinationAllowRule rule
                : proposal.destinationPolicyFacts().allowRules()) {
            if (rule.candidateIdentity().equals(candidate.candidateIdentity())
                    && rule.destinationIdentity().equals(destinationIdentity)
                    && rule.permittedPurposes().contains(proposal.purpose())) {
                return rule;
            }
        }
        return null;
    }

    private static boolean validInspection(
            ModelContentInspectionFacts inspection,
            String expectedDigest,
            Instant evaluationTime) {
        return ModelContentInspectionFacts.SCHEMA_VERSION.equals(
                        inspection.schemaVersion())
                && fresh(inspection.observedAt(), inspection.expiresAt(), evaluationTime)
                && inspection.coverage() == ModelInspectionCoverage.COMPLETE
                && inspection.contentDigest().equals(expectedDigest);
    }

    private static boolean validRedaction(
            ModelContentRedactionFacts redaction,
            ModelContentInspectionFacts sourceInspection,
            ModelOutboundContent content,
            Instant evaluationTime) {
        return ModelContentRedactionFacts.SCHEMA_VERSION.equals(
                        redaction.schemaVersion())
                && fresh(redaction.observedAt(), redaction.expiresAt(), evaluationTime)
                && redaction.coverage() == ModelInspectionCoverage.COMPLETE
                && redaction.sourcePromptDigest().equals(content.sourcePromptDigest())
                && redaction.outboundPromptDigest().equals(content.outboundPromptDigest())
                && redaction.rulesetIdentity().equals(sourceInspection.rulesetIdentity())
                && redaction.rulesetRevision().equals(sourceInspection.rulesetRevision())
                && redaction.coveredFindingIdentities().equals(
                        sourceInspection.findingIdentities());
    }

    private static boolean fresh(
            Instant observedAt,
            Instant expiresAt,
            Instant evaluationTime) {
        return !observedAt.isAfter(evaluationTime) && evaluationTime.isBefore(expiresAt);
    }

    private static ModelDataClassification maximum(
            ModelDataClassification first,
            ModelDataClassification... remaining) {
        ModelDataClassification maximum = first;
        for (ModelDataClassification candidate : remaining) {
            if (candidate.ordinal() > maximum.ordinal()) {
                maximum = candidate;
            }
        }
        return maximum;
    }

    private static ModelRouteSelectionDecision refused(
            ModelRouteSelectionRefusalReason reason,
            int ordinal) {
        return new ModelRouteSelectionDecision.Refused(reason, ordinal);
    }
}
