package com.enhancer.model;

import com.enhancer.loop.ApprovedTask;
import com.enhancer.tool.ExecutionPolicy;
import java.time.Instant;
import java.util.Objects;

/** Stateless pure RFC-0028 default-deny evaluator for one exact remote proposal. */
public final class ModelOutboundRouteAdmission {

    /** Returns the first matching RFC-0016/RFC-0028 refusal or exact eligibility. */
    public ModelOutboundRouteAdmissionDecision evaluate(
            ProfiledModelRequest profiledRequest,
            ApprovedTask approvedTask,
            ExecutionPolicy executionPolicy,
            String authoritativeRequiredCapability,
            ModelOutboundRouteProposal proposal,
            Instant evaluationTime) {
        Objects.requireNonNull(profiledRequest, "profiledRequest must not be null");
        Objects.requireNonNull(approvedTask, "approvedTask must not be null");
        Objects.requireNonNull(executionPolicy, "executionPolicy must not be null");
        Objects.requireNonNull(
                authoritativeRequiredCapability,
                "authoritativeRequiredCapability must not be null");
        Objects.requireNonNull(proposal, "proposal must not be null");
        Objects.requireNonNull(evaluationTime, "evaluationTime must not be null");

        ModelInvocationAdmissionDecision invocationDecision =
                new ModelInvocationAdmission().evaluate(
                        profiledRequest,
                        approvedTask,
                        executionPolicy,
                        authoritativeRequiredCapability);
        if (invocationDecision instanceof ModelInvocationAdmissionDecision.Admitted) {
            return refused(
                    ModelOutboundRouteRejectionReason.PROFILE_REQUIRES_LOCAL_EXECUTION);
        }
        ModelInvocationRejectionReason invocationReason =
                ((ModelInvocationAdmissionDecision.Rejected) invocationDecision).reason();
        if (invocationReason != ModelInvocationRejectionReason.OUTBOUND_POLICY_REQUIRED) {
            return refused(mapInvocationReason(invocationReason));
        }

        ModelRouteCandidateFacts candidate = proposal.candidateFacts();
        if (!ModelRouteCandidateFacts.SCHEMA_VERSION.equals(candidate.schemaVersion())
                || !fresh(candidate.observedAt(), candidate.expiresAt(), evaluationTime)) {
            return refused(
                    ModelOutboundRouteRejectionReason
                            .CANDIDATE_FACTS_INVALID_OR_STALE);
        }

        ModelDestinationPolicyFacts destination = proposal.destinationPolicyFacts();
        if (!ModelDestinationPolicyFacts.SCHEMA_VERSION.equals(
                        destination.schemaVersion())
                || !fresh(
                        destination.observedAt(),
                        destination.expiresAt(),
                        evaluationTime)) {
            return refused(
                    ModelOutboundRouteRejectionReason
                            .DESTINATION_FACTS_INVALID_OR_STALE);
        }
        if (!candidate.destinationIdentity().equals(destination.destinationIdentity())) {
            return refused(
                    ModelOutboundRouteRejectionReason.CANDIDATE_DESTINATION_MISMATCH);
        }
        if (!candidate.modelClass().equals(
                profiledRequest.executionProfile().modelClass())) {
            return refused(
                    ModelOutboundRouteRejectionReason.CANDIDATE_MODEL_CLASS_MISMATCH);
        }
        if (!candidate.requiredCapability().equals(authoritativeRequiredCapability)) {
            return refused(
                    ModelOutboundRouteRejectionReason.CANDIDATE_CAPABILITY_MISMATCH);
        }

        ModelDestinationAllowRule selectedRule = null;
        boolean routeExists = false;
        for (ModelDestinationAllowRule rule : destination.allowRules()) {
            if (rule.candidateIdentity().equals(candidate.candidateIdentity())
                    && rule.destinationIdentity().equals(destination.destinationIdentity())) {
                routeExists = true;
                if (rule.permittedPurposes().contains(proposal.purpose())) {
                    selectedRule = rule;
                    break;
                }
            }
        }
        if (!routeExists) {
            return refused(ModelOutboundRouteRejectionReason.ROUTE_NOT_ALLOWED);
        }
        if (selectedRule == null) {
            return refused(ModelOutboundRouteRejectionReason.PURPOSE_NOT_ALLOWED);
        }

        ModelRetentionFacts retention = proposal.retentionFacts();
        if (!selectedRule.permittedRetentionUses().containsAll(retention.uses())
                || retention.maximumDuration().compareTo(
                        selectedRule.maximumRetentionDuration()) > 0) {
            return refused(ModelOutboundRouteRejectionReason.RETENTION_NOT_ALLOWED);
        }

        ModelOutboundContent content = proposal.outboundContent();
        String sourcePrompt = profiledRequest.request().prompt();
        if (!ModelOutboundRouteProposal.SCHEMA_VERSION.equals(proposal.schemaVersion())
                || !ModelOutboundContent.SCHEMA_VERSION.equals(content.schemaVersion())
                || !content.sourcePromptDigest().equals(
                        ModelOutboundContent.digestOf(sourcePrompt))
                || (content.transformation()
                                == ModelOutboundContentTransformation.UNCHANGED
                        && !content.outboundPrompt().equals(sourcePrompt))
                || (content.transformation()
                                == ModelOutboundContentTransformation.REDACTED
                        && content.outboundPrompt().equals(sourcePrompt))) {
            return refused(ModelOutboundRouteRejectionReason.CONTENT_BINDING_MISMATCH);
        }

        if (proposal.sourceInspection().isEmpty()
                || proposal.outboundInspection().isEmpty()) {
            return refused(
                    ModelOutboundRouteRejectionReason.CONTENT_INSPECTION_REQUIRED);
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
            return refused(
                    ModelOutboundRouteRejectionReason.CONTENT_INSPECTION_FAILED);
        }

        boolean redacted = content.transformation()
                == ModelOutboundContentTransformation.REDACTED;
        if (sourceInspection.status()
                == ModelContentInspectionStatus.REDACTION_REQUIRED) {
            if (!redacted || proposal.redactionFacts().isEmpty()) {
                return refused(ModelOutboundRouteRejectionReason.REDACTION_REQUIRED);
            }
        } else if (redacted || proposal.redactionFacts().isPresent()) {
            return refused(
                    ModelOutboundRouteRejectionReason
                            .REDACTION_INVALID_OR_INCOMPLETE);
        }

        if (redacted) {
            ModelContentRedactionFacts redaction =
                    proposal.redactionFacts().orElseThrow();
            if (!validRedaction(
                    redaction,
                    sourceInspection,
                    content,
                    evaluationTime)) {
                return refused(
                        ModelOutboundRouteRejectionReason
                                .REDACTION_INVALID_OR_INCOMPLETE);
            }
        }

        ModelDataClassification effectiveClassification = redacted
                ? maximum(
                        profiledRequest.executionProfile().dataClassification(),
                        outboundInspection.classification())
                : maximum(
                        profiledRequest.executionProfile().dataClassification(),
                        sourceInspection.classification(),
                        outboundInspection.classification());
        if (effectiveClassification.ordinal()
                > selectedRule.maximumDataClassification().ordinal()) {
            return refused(
                    ModelOutboundRouteRejectionReason
                            .EFFECTIVE_CLASSIFICATION_NOT_ALLOWED);
        }

        if (proposal.pricingFacts().isEmpty()) {
            return refused(ModelOutboundRouteRejectionReason.PRICING_FACTS_REQUIRED);
        }
        ModelRoutePricingFacts pricing = proposal.pricingFacts().orElseThrow();
        if (!validPricing(
                pricing,
                selectedRule,
                candidate,
                destination,
                profiledRequest,
                evaluationTime)) {
            return refused(
                    ModelOutboundRouteRejectionReason.PRICING_FACTS_INVALID_OR_STALE);
        }
        ModelCostBudget costBudget = profiledRequest.executionProfile().costBudget();
        if (!pricing.currencyCode().equals(costBudget.currencyCode())) {
            return refused(ModelOutboundRouteRejectionReason.COST_CURRENCY_MISMATCH);
        }
        if (pricing.worstCaseChargeMicrounits() > costBudget.maxMicrounits()) {
            return refused(ModelOutboundRouteRejectionReason.COST_CEILING_EXCEEDED);
        }
        if (pricing.worstCaseChargeMicrounits() > 0) {
            return refused(ModelOutboundRouteRejectionReason.SPEND_AUTHORITY_REQUIRED);
        }
        return new ModelOutboundRouteAdmissionDecision.Eligible(
                proposal, effectiveClassification);
    }

    private static ModelOutboundRouteRejectionReason mapInvocationReason(
            ModelInvocationRejectionReason reason) {
        return switch (reason) {
            case TASK_TOOL_NOT_ALLOWED ->
                    ModelOutboundRouteRejectionReason.TASK_TOOL_NOT_ALLOWED;
            case EXECUTION_POLICY_TOOL_NOT_ALLOWED ->
                    ModelOutboundRouteRejectionReason.EXECUTION_POLICY_TOOL_NOT_ALLOWED;
            case REQUIRED_CAPABILITY_MISMATCH ->
                    ModelOutboundRouteRejectionReason.REQUIRED_CAPABILITY_MISMATCH;
            case GATEWAY_TIMEOUT_NOT_WITHIN_EXECUTION_POLICY ->
                    ModelOutboundRouteRejectionReason
                            .GATEWAY_TIMEOUT_NOT_WITHIN_EXECUTION_POLICY;
            case OUTBOUND_POLICY_REQUIRED -> throw new IllegalArgumentException(
                    "OUTBOUND_POLICY_REQUIRED is the guarded continuation");
        };
    }

    private static boolean fresh(
            Instant observedAt,
            Instant expiresAt,
            Instant evaluationTime) {
        return !observedAt.isAfter(evaluationTime) && evaluationTime.isBefore(expiresAt);
    }

    private static boolean validInspection(
            ModelContentInspectionFacts inspection,
            String expectedDigest,
            Instant evaluationTime) {
        return ModelContentInspectionFacts.SCHEMA_VERSION.equals(
                        inspection.schemaVersion())
                && fresh(
                        inspection.observedAt(),
                        inspection.expiresAt(),
                        evaluationTime)
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
                && redaction.outboundPromptDigest().equals(
                        content.outboundPromptDigest())
                && redaction.rulesetIdentity().equals(sourceInspection.rulesetIdentity())
                && redaction.rulesetRevision().equals(sourceInspection.rulesetRevision())
                && redaction.coveredFindingIdentities().equals(
                        sourceInspection.findingIdentities());
    }

    private static boolean validPricing(
            ModelRoutePricingFacts pricing,
            ModelDestinationAllowRule selectedRule,
            ModelRouteCandidateFacts candidate,
            ModelDestinationPolicyFacts destination,
            ProfiledModelRequest profiledRequest,
            Instant evaluationTime) {
        return ModelRoutePricingFacts.SCHEMA_VERSION.equals(pricing.schemaVersion())
                && fresh(pricing.observedAt(), pricing.expiresAt(), evaluationTime)
                && pricing.factSourceIdentity().equals(
                        selectedRule.pricingFactSourceIdentity())
                && pricing.candidateIdentity().equals(candidate.candidateIdentity())
                && pricing.destinationIdentity().equals(destination.destinationIdentity())
                && pricing.modelClass().equals(
                        profiledRequest.executionProfile().modelClass())
                && pricing.tokenSemanticsIdentity().equals(
                        ModelRoutePricingFacts.TOKEN_SEMANTICS_IDENTITY)
                && pricing.tokenBudgetBasis().equals(
                        profiledRequest.executionProfile().tokenBudget());
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

    private static ModelOutboundRouteAdmissionDecision refused(
            ModelOutboundRouteRejectionReason reason) {
        return new ModelOutboundRouteAdmissionDecision.Refused(reason);
    }
}
