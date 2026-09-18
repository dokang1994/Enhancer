package com.enhancer.model;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ModelOutboundRouteValuesTest {

    private static final Instant OBSERVED_AT = Instant.parse("2026-09-18T00:00:00Z");
    private static final Instant EXPIRES_AT = Instant.parse("2026-09-18T00:05:00Z");

    @Test
    void vocabulariesAreClosedAndInRfcOrder() {
        assertArrayEquals(
                new ModelRouteKind[] {ModelRouteKind.REMOTE},
                ModelRouteKind.values());
        assertArrayEquals(
                new ModelOutboundPurpose[] {ModelOutboundPurpose.MODEL_INFERENCE},
                ModelOutboundPurpose.values());
        assertArrayEquals(
                new ModelRetentionUse[] {
                        ModelRetentionUse.REQUEST_PROCESSING,
                        ModelRetentionUse.ABUSE_MONITORING,
                        ModelRetentionUse.SERVICE_LOGGING,
                        ModelRetentionUse.HUMAN_REVIEW,
                        ModelRetentionUse.MODEL_TRAINING,
                        ModelRetentionUse.CACHE
                },
                ModelRetentionUse.values());
        assertArrayEquals(
                new ModelOutboundContentTransformation[] {
                        ModelOutboundContentTransformation.UNCHANGED,
                        ModelOutboundContentTransformation.REDACTED
                },
                ModelOutboundContentTransformation.values());
        assertArrayEquals(
                new ModelContentInspectionStatus[] {
                        ModelContentInspectionStatus.CLEAR,
                        ModelContentInspectionStatus.REDACTION_REQUIRED,
                        ModelContentInspectionStatus.FAILED
                },
                ModelContentInspectionStatus.values());
        assertArrayEquals(
                new ModelInspectionCoverage[] {
                        ModelInspectionCoverage.COMPLETE,
                        ModelInspectionCoverage.PARTIAL
                },
                ModelInspectionCoverage.values());
        assertArrayEquals(
                new ModelOutboundRouteRejectionReason[] {
                        ModelOutboundRouteRejectionReason.TASK_TOOL_NOT_ALLOWED,
                        ModelOutboundRouteRejectionReason.EXECUTION_POLICY_TOOL_NOT_ALLOWED,
                        ModelOutboundRouteRejectionReason.REQUIRED_CAPABILITY_MISMATCH,
                        ModelOutboundRouteRejectionReason.GATEWAY_TIMEOUT_NOT_WITHIN_EXECUTION_POLICY,
                        ModelOutboundRouteRejectionReason.PROFILE_REQUIRES_LOCAL_EXECUTION,
                        ModelOutboundRouteRejectionReason.CANDIDATE_FACTS_INVALID_OR_STALE,
                        ModelOutboundRouteRejectionReason.DESTINATION_FACTS_INVALID_OR_STALE,
                        ModelOutboundRouteRejectionReason.CANDIDATE_DESTINATION_MISMATCH,
                        ModelOutboundRouteRejectionReason.CANDIDATE_MODEL_CLASS_MISMATCH,
                        ModelOutboundRouteRejectionReason.CANDIDATE_CAPABILITY_MISMATCH,
                        ModelOutboundRouteRejectionReason.ROUTE_NOT_ALLOWED,
                        ModelOutboundRouteRejectionReason.PURPOSE_NOT_ALLOWED,
                        ModelOutboundRouteRejectionReason.RETENTION_NOT_ALLOWED,
                        ModelOutboundRouteRejectionReason.CONTENT_BINDING_MISMATCH,
                        ModelOutboundRouteRejectionReason.CONTENT_INSPECTION_REQUIRED,
                        ModelOutboundRouteRejectionReason.CONTENT_INSPECTION_FAILED,
                        ModelOutboundRouteRejectionReason.REDACTION_REQUIRED,
                        ModelOutboundRouteRejectionReason.REDACTION_INVALID_OR_INCOMPLETE,
                        ModelOutboundRouteRejectionReason.EFFECTIVE_CLASSIFICATION_NOT_ALLOWED,
                        ModelOutboundRouteRejectionReason.PRICING_FACTS_REQUIRED,
                        ModelOutboundRouteRejectionReason.PRICING_FACTS_INVALID_OR_STALE,
                        ModelOutboundRouteRejectionReason.COST_CURRENCY_MISMATCH,
                        ModelOutboundRouteRejectionReason.COST_CEILING_EXCEEDED,
                        ModelOutboundRouteRejectionReason.SPEND_AUTHORITY_REQUIRED
                },
                ModelOutboundRouteRejectionReason.values());
    }

    @Test
    void proposalRetainsExactOpaqueFactsAndDefensiveCollections() {
        ModelRouteCandidateFacts candidate = candidateFacts();
        ModelDestinationAllowRule rule = allowRule();
        LinkedHashSet<ModelDestinationAllowRule> mutableRules =
                new LinkedHashSet<>(Set.of(rule));
        ModelDestinationPolicyFacts destination = ModelDestinationPolicyFacts.create(
                ModelDestinationPolicyFacts.SCHEMA_VERSION,
                "destination-a",
                "outbound-policy-source",
                "policy-r1",
                OBSERVED_AT,
                EXPIRES_AT,
                mutableRules);
        mutableRules.clear();

        ModelOutboundContent content = ModelOutboundContent.unchanged("sensitive prompt");
        ModelContentInspectionFacts inspection = inspectionFacts(
                content.outboundPromptDigest(),
                ModelContentInspectionStatus.CLEAR,
                Set.of());
        ModelRoutePricingFacts pricing = pricingFacts();
        ModelRetentionFacts retention = new ModelRetentionFacts(
                Set.of(ModelRetentionUse.REQUEST_PROCESSING),
                Duration.ofMinutes(1));
        ModelOutboundRouteProposal proposal = new ModelOutboundRouteProposal(
                ModelOutboundRouteProposal.SCHEMA_VERSION,
                candidate,
                destination,
                ModelOutboundPurpose.MODEL_INFERENCE,
                retention,
                content,
                Optional.of(inspection),
                Optional.of(inspection),
                Optional.empty(),
                Optional.of(pricing));

        assertSame(candidate, proposal.candidateFacts());
        assertSame(destination, proposal.destinationPolicyFacts());
        assertSame(content, proposal.outboundContent());
        assertSame(inspection, proposal.sourceInspection().orElseThrow());
        assertSame(pricing, proposal.pricingFacts().orElseThrow());
        assertEquals(Set.of(rule), destination.allowRules());
        assertThrows(
                UnsupportedOperationException.class,
                () -> destination.allowRules().clear());
        assertEquals(ModelRouteKind.REMOTE, candidate.routeKind());
    }

    @Test
    void outboundContentDerivesExactDigestsAndNeverRendersPrompts() {
        String source = "secret-source-sentinel";
        String redacted = "safe-output-sentinel";
        ModelOutboundContent unchanged = ModelOutboundContent.unchanged(source);
        ModelOutboundContent transformed = ModelOutboundContent.redacted(source, redacted);

        assertEquals(sha256(source), unchanged.sourcePromptDigest());
        assertEquals(sha256(source), unchanged.outboundPromptDigest());
        assertEquals(source, unchanged.outboundPrompt());
        assertEquals(ModelOutboundContentTransformation.UNCHANGED, unchanged.transformation());
        assertEquals(sha256(source), transformed.sourcePromptDigest());
        assertEquals(sha256(redacted), transformed.outboundPromptDigest());
        assertEquals(ModelOutboundContentTransformation.REDACTED, transformed.transformation());
        assertFalse(unchanged.toString().contains(source));
        assertFalse(transformed.toString().contains(redacted));

        assertThrows(IllegalArgumentException.class, () -> ModelOutboundContent.redacted(source, source));
        assertThrows(IllegalArgumentException.class, () -> ModelOutboundContent.unchanged(" "));
        assertThrows(
                IllegalArgumentException.class,
                () -> ModelOutboundContent.unchanged(
                        "x".repeat(ModelRequest.MAX_PROMPT_CHARACTERS + 1)));
    }

    @Test
    void factsRejectInvalidIntrinsicShapesAndCopyFindingCoverage() {
        assertThrows(
                IllegalArgumentException.class,
                () -> ModelRouteCandidateFacts.create(
                        ModelRouteCandidateFacts.SCHEMA_VERSION,
                        "candidate-a",
                        "candidate-source",
                        "revision-1",
                        EXPIRES_AT,
                        OBSERVED_AT,
                        "reasoning-standard",
                        "repository-analysis",
                        "destination-a"));
        assertThrows(
                IllegalArgumentException.class,
                () -> new ModelRetentionFacts(Set.of(), Duration.ofSeconds(1)));
        assertThrows(
                IllegalArgumentException.class,
                () -> new ModelRetentionFacts(
                        Set.of(ModelRetentionUse.REQUEST_PROCESSING), Duration.ZERO));

        LinkedHashSet<String> findings = new LinkedHashSet<>(Set.of("finding-a"));
        ModelContentInspectionFacts inspection = inspectionFacts(
                "a".repeat(64),
                ModelContentInspectionStatus.REDACTION_REQUIRED,
                findings);
        findings.clear();
        assertEquals(Set.of("finding-a"), inspection.findingIdentities());
        assertThrows(
                UnsupportedOperationException.class,
                () -> inspection.findingIdentities().clear());
    }

    @Test
    void decisionIsSealedRetainsExactProposalAndUsesSafeRendering() {
        ModelOutboundRouteProposal proposal = proposalWithSecret("never-render-this-prompt");
        ModelOutboundRouteAdmissionDecision.Eligible eligible =
                new ModelOutboundRouteAdmissionDecision.Eligible(
                        proposal, ModelDataClassification.INTERNAL);
        ModelOutboundRouteAdmissionDecision.Refused refused =
                new ModelOutboundRouteAdmissionDecision.Refused(
                        ModelOutboundRouteRejectionReason.ROUTE_NOT_ALLOWED);

        assertTrue(ModelOutboundRouteAdmissionDecision.class.isSealed());
        assertArrayEquals(
                new Class<?>[] {
                        ModelOutboundRouteAdmissionDecision.Eligible.class,
                        ModelOutboundRouteAdmissionDecision.Refused.class
                },
                ModelOutboundRouteAdmissionDecision.class.getPermittedSubclasses());
        assertSame(proposal, eligible.proposal());
        assertEquals(ModelDataClassification.INTERNAL, eligible.effectiveClassification());
        assertEquals(ModelOutboundRouteRejectionReason.ROUTE_NOT_ALLOWED, refused.reason());
        assertFalse(eligible.toString().contains("never-render-this-prompt"));
        assertFalse(proposal.toString().contains("never-render-this-prompt"));
    }

    private static ModelOutboundRouteProposal proposalWithSecret(String prompt) {
        ModelOutboundContent content = ModelOutboundContent.unchanged(prompt);
        ModelContentInspectionFacts inspection = inspectionFacts(
                content.outboundPromptDigest(), ModelContentInspectionStatus.CLEAR, Set.of());
        return new ModelOutboundRouteProposal(
                ModelOutboundRouteProposal.SCHEMA_VERSION,
                candidateFacts(),
                ModelDestinationPolicyFacts.create(
                        ModelDestinationPolicyFacts.SCHEMA_VERSION,
                        "destination-a",
                        "outbound-policy-source",
                        "policy-r1",
                        OBSERVED_AT,
                        EXPIRES_AT,
                        Set.of(allowRule())),
                ModelOutboundPurpose.MODEL_INFERENCE,
                new ModelRetentionFacts(
                        Set.of(ModelRetentionUse.REQUEST_PROCESSING), Duration.ofMinutes(1)),
                content,
                Optional.of(inspection),
                Optional.of(inspection),
                Optional.empty(),
                Optional.of(pricingFacts()));
    }

    private static ModelRouteCandidateFacts candidateFacts() {
        return ModelRouteCandidateFacts.create(
                ModelRouteCandidateFacts.SCHEMA_VERSION,
                "candidate-a",
                "candidate-source",
                "revision-1",
                OBSERVED_AT,
                EXPIRES_AT,
                "reasoning-standard",
                "repository-analysis",
                "destination-a");
    }

    private static ModelDestinationAllowRule allowRule() {
        return ModelDestinationAllowRule.create(
                "candidate-a",
                "destination-a",
                Set.of(ModelOutboundPurpose.MODEL_INFERENCE),
                Set.of(ModelRetentionUse.REQUEST_PROCESSING),
                Duration.ofMinutes(5),
                ModelDataClassification.INTERNAL,
                "pricing-source");
    }

    private static ModelContentInspectionFacts inspectionFacts(
            String digest,
            ModelContentInspectionStatus status,
            Set<String> findings) {
        return ModelContentInspectionFacts.create(
                ModelContentInspectionFacts.SCHEMA_VERSION,
                "scanner-a",
                "scanner-r1",
                "ruleset-a",
                "ruleset-r1",
                OBSERVED_AT,
                EXPIRES_AT,
                digest,
                ModelDataClassification.INTERNAL,
                ModelInspectionCoverage.COMPLETE,
                status,
                findings);
    }

    private static ModelRoutePricingFacts pricingFacts() {
        return ModelRoutePricingFacts.create(
                ModelRoutePricingFacts.SCHEMA_VERSION,
                "pricing-source",
                "pricing-r1",
                OBSERVED_AT,
                EXPIRES_AT,
                "candidate-a",
                "destination-a",
                "reasoning-standard",
                "profile-token-budget-v1",
                "schedule-a",
                new ModelTokenBudget(10, 5, 15),
                "USD",
                0);
    }

    private static String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new AssertionError(exception);
        }
    }
}
