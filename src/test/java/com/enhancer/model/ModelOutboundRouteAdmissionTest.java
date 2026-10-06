package com.enhancer.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.enhancer.loop.ApprovedTask;
import com.enhancer.tool.CancellationToken;
import com.enhancer.tool.ExecutionPolicy;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ModelOutboundRouteAdmissionTest {

    private static final String CAPABILITY = "repository-analysis";
    private static final String MODEL_CLASS = "reasoning-standard";
    private static final String CANDIDATE = "candidate-a";
    private static final String DESTINATION = "destination-a";
    private static final String PROMPT = "Analyze the repository without leaking secrets.";
    private static final Instant OBSERVED_AT = Instant.parse("2026-09-18T00:00:00Z");
    private static final Instant EVALUATION_TIME = Instant.parse("2026-09-18T00:01:00Z");
    private static final Instant EXPIRES_AT = Instant.parse("2026-09-18T00:05:00Z");

    @Test
    void rejectsEveryMissingInputAsProgrammingErrorAndHasNoState() {
        Scenario scenario = new Scenario();
        ModelOutboundRouteAdmission admission = new ModelOutboundRouteAdmission();

        assertEquals(0, ModelOutboundRouteAdmission.class.getDeclaredFields().length);
        assertThrows(NullPointerException.class, () -> admission.evaluate(
                null, scenario.task, scenario.policy, scenario.capability,
                scenario.proposal(), scenario.evaluationTime));
        assertThrows(NullPointerException.class, () -> admission.evaluate(
                scenario.profiledRequest, null, scenario.policy, scenario.capability,
                scenario.proposal(), scenario.evaluationTime));
        assertThrows(NullPointerException.class, () -> admission.evaluate(
                scenario.profiledRequest, scenario.task, null, scenario.capability,
                scenario.proposal(), scenario.evaluationTime));
        assertThrows(NullPointerException.class, () -> admission.evaluate(
                scenario.profiledRequest, scenario.task, scenario.policy, null,
                scenario.proposal(), scenario.evaluationTime));
        assertThrows(NullPointerException.class, () -> admission.evaluate(
                scenario.profiledRequest, scenario.task, scenario.policy,
                scenario.capability, null, scenario.evaluationTime));
        assertThrows(NullPointerException.class, () -> admission.evaluate(
                scenario.profiledRequest, scenario.task, scenario.policy,
                scenario.capability, scenario.proposal(), null));
    }

    @Test
    void preservesEveryRfc0016OutcomeBeforeOutboundFacts() {
        Scenario scenario = new Scenario();
        scenario.task = approvedTask(Set.of("read-file"));
        scenario.assertRefused(ModelOutboundRouteRejectionReason.TASK_TOOL_NOT_ALLOWED);

        scenario = new Scenario();
        scenario.policy = executionPolicy(Set.of("read-file"), Duration.ofSeconds(31));
        scenario.assertRefused(
                ModelOutboundRouteRejectionReason.EXECUTION_POLICY_TOOL_NOT_ALLOWED);

        scenario = new Scenario();
        scenario.capability = "code-review";
        scenario.assertRefused(
                ModelOutboundRouteRejectionReason.REQUIRED_CAPABILITY_MISMATCH);

        scenario = new Scenario();
        scenario.policy = executionPolicy(
                Set.of(ModelInvokeTool.NAME), scenario.profiledRequest.request().timeout());
        scenario.assertRefused(
                ModelOutboundRouteRejectionReason
                        .GATEWAY_TIMEOUT_NOT_WITHIN_EXECUTION_POLICY);

        scenario = new Scenario();
        scenario.profiledRequest = profiledRequest(
                ModelLocalityRequirement.LOCAL_ONLY,
                ModelDataClassification.INTERNAL,
                0);
        scenario.assertRefused(
                ModelOutboundRouteRejectionReason.PROFILE_REQUIRES_LOCAL_EXECUTION);
    }

    @Test
    void refusesInvalidAuthorityRoutePurposeAndRetentionInOrder() {
        Scenario scenario = new Scenario();
        scenario.candidate = candidate(
                "model-route-candidate-facts-v2",
                OBSERVED_AT,
                EXPIRES_AT,
                DESTINATION,
                MODEL_CLASS,
                CAPABILITY);
        scenario.assertRefused(
                ModelOutboundRouteRejectionReason.CANDIDATE_FACTS_INVALID_OR_STALE);

        scenario = new Scenario();
        scenario.destination = destination(
                "model-destination-policy-facts-v2",
                OBSERVED_AT,
                EXPIRES_AT,
                Set.of(defaultRule()));
        scenario.assertRefused(
                ModelOutboundRouteRejectionReason.DESTINATION_FACTS_INVALID_OR_STALE);

        scenario = new Scenario();
        scenario.candidate = candidate(
                ModelRouteCandidateFacts.SCHEMA_VERSION,
                OBSERVED_AT,
                EXPIRES_AT,
                "destination-b",
                MODEL_CLASS,
                CAPABILITY);
        scenario.assertRefused(
                ModelOutboundRouteRejectionReason.CANDIDATE_DESTINATION_MISMATCH);

        scenario = new Scenario();
        scenario.candidate = candidate(
                ModelRouteCandidateFacts.SCHEMA_VERSION,
                OBSERVED_AT,
                EXPIRES_AT,
                DESTINATION,
                "reasoning-extended",
                CAPABILITY);
        scenario.assertRefused(
                ModelOutboundRouteRejectionReason.CANDIDATE_MODEL_CLASS_MISMATCH);

        scenario = new Scenario();
        scenario.candidate = candidate(
                ModelRouteCandidateFacts.SCHEMA_VERSION,
                OBSERVED_AT,
                EXPIRES_AT,
                DESTINATION,
                MODEL_CLASS,
                "code-review");
        scenario.assertRefused(
                ModelOutboundRouteRejectionReason.CANDIDATE_CAPABILITY_MISMATCH);

        scenario = new Scenario();
        scenario.destination = destination(
                ModelDestinationPolicyFacts.SCHEMA_VERSION,
                OBSERVED_AT,
                EXPIRES_AT,
                Set.of());
        scenario.assertRefused(ModelOutboundRouteRejectionReason.ROUTE_NOT_ALLOWED);

        scenario = new Scenario();
        scenario.destination = destination(
                ModelDestinationPolicyFacts.SCHEMA_VERSION,
                OBSERVED_AT,
                EXPIRES_AT,
                Set.of(rule(
                        Set.of(),
                        Set.of(ModelRetentionUse.REQUEST_PROCESSING),
                        Duration.ofMinutes(5),
                        ModelDataClassification.INTERNAL,
                        "pricing-source")));
        scenario.assertRefused(ModelOutboundRouteRejectionReason.PURPOSE_NOT_ALLOWED);

        scenario = new Scenario();
        scenario.retention = new ModelRetentionFacts(
                Set.of(ModelRetentionUse.CACHE), Duration.ofMinutes(1));
        scenario.assertRefused(ModelOutboundRouteRejectionReason.RETENTION_NOT_ALLOWED);
    }

    @Test
    void refusesUnsafeContentInspectionRedactionAndClassification() {
        Scenario scenario = new Scenario();
        scenario.content = ModelOutboundContent.unchanged("different source prompt");
        scenario.assertRefused(
                ModelOutboundRouteRejectionReason.CONTENT_BINDING_MISMATCH);

        scenario = new Scenario();
        scenario.sourceInspection = Optional.empty();
        scenario.assertRefused(
                ModelOutboundRouteRejectionReason.CONTENT_INSPECTION_REQUIRED);

        scenario = new Scenario();
        scenario.sourceInspection = Optional.of(inspection(
                scenario.content.sourcePromptDigest(),
                ModelContentInspectionStatus.FAILED,
                ModelInspectionCoverage.COMPLETE,
                Set.of()));
        scenario.assertRefused(
                ModelOutboundRouteRejectionReason.CONTENT_INSPECTION_FAILED);

        scenario = redactionScenario();
        scenario.content = ModelOutboundContent.unchanged(PROMPT);
        scenario.outboundInspection = Optional.of(inspection(
                scenario.content.outboundPromptDigest(),
                ModelContentInspectionStatus.CLEAR,
                ModelInspectionCoverage.COMPLETE,
                Set.of()));
        scenario.redaction = Optional.empty();
        scenario.assertRefused(ModelOutboundRouteRejectionReason.REDACTION_REQUIRED);

        scenario = redactionScenario();
        scenario.redaction = Optional.of(redaction(
                scenario.content,
                ModelInspectionCoverage.PARTIAL,
                Set.of()));
        scenario.assertRefused(
                ModelOutboundRouteRejectionReason.REDACTION_INVALID_OR_INCOMPLETE);

        scenario = new Scenario();
        scenario.destination = destination(
                ModelDestinationPolicyFacts.SCHEMA_VERSION,
                OBSERVED_AT,
                EXPIRES_AT,
                Set.of(rule(
                        Set.of(ModelOutboundPurpose.MODEL_INFERENCE),
                        Set.of(ModelRetentionUse.REQUEST_PROCESSING),
                        Duration.ofMinutes(5),
                        ModelDataClassification.PUBLIC,
                        "pricing-source")));
        scenario.assertRefused(
                ModelOutboundRouteRejectionReason
                        .EFFECTIVE_CLASSIFICATION_NOT_ALLOWED);
    }

    @Test
    void refusesMissingInvalidOrUnauthorizedPricingAndAdmitsOnlyZeroCharge() {
        Scenario scenario = new Scenario();
        scenario.pricing = Optional.empty();
        scenario.assertRefused(ModelOutboundRouteRejectionReason.PRICING_FACTS_REQUIRED);

        scenario = new Scenario();
        scenario.pricing = Optional.of(pricing(
                "model-route-pricing-facts-v2",
                "pricing-source",
                "USD",
                0,
                scenario.profiledRequest.executionProfile().tokenBudget()));
        scenario.assertRefused(
                ModelOutboundRouteRejectionReason.PRICING_FACTS_INVALID_OR_STALE);

        scenario = new Scenario();
        scenario.pricing = Optional.of(pricing(
                ModelRoutePricingFacts.SCHEMA_VERSION,
                "pricing-source",
                "KRW",
                0,
                scenario.profiledRequest.executionProfile().tokenBudget()));
        scenario.assertRefused(ModelOutboundRouteRejectionReason.COST_CURRENCY_MISMATCH);

        scenario = new Scenario();
        scenario.profiledRequest = profiledRequest(
                ModelLocalityRequirement.POLICY_CONSTRAINED,
                ModelDataClassification.INTERNAL,
                10);
        scenario.pricing = Optional.of(pricing(
                ModelRoutePricingFacts.SCHEMA_VERSION,
                "pricing-source",
                "USD",
                11,
                scenario.profiledRequest.executionProfile().tokenBudget()));
        scenario.assertRefused(ModelOutboundRouteRejectionReason.COST_CEILING_EXCEEDED);

        scenario = new Scenario();
        scenario.profiledRequest = profiledRequest(
                ModelLocalityRequirement.POLICY_CONSTRAINED,
                ModelDataClassification.INTERNAL,
                10);
        scenario.pricing = Optional.of(pricing(
                ModelRoutePricingFacts.SCHEMA_VERSION,
                "pricing-source",
                "USD",
                1,
                scenario.profiledRequest.executionProfile().tokenBudget()));
        scenario.assertRefused(ModelOutboundRouteRejectionReason.SPEND_AUTHORITY_REQUIRED);

        scenario = new Scenario();
        ModelOutboundRouteProposal exactProposal = scenario.proposal();
        ModelOutboundRouteAdmissionDecision.Eligible eligible = assertInstanceOf(
                ModelOutboundRouteAdmissionDecision.Eligible.class,
                scenario.evaluate(exactProposal));
        assertSame(exactProposal, eligible.proposal());
        assertEquals(
                ModelDataClassification.INTERNAL,
                eligible.effectiveClassification());
    }

    @Test
    void freshnessUsesInclusiveObservationAndExclusiveExpiryAndFirstMatchWins() {
        Scenario scenario = new Scenario();
        scenario.evaluationTime = OBSERVED_AT;
        assertInstanceOf(
                ModelOutboundRouteAdmissionDecision.Eligible.class,
                scenario.evaluate(scenario.proposal()));

        scenario = new Scenario();
        scenario.evaluationTime = EXPIRES_AT;
        scenario.assertRefused(
                ModelOutboundRouteRejectionReason.CANDIDATE_FACTS_INVALID_OR_STALE);

        scenario = new Scenario();
        scenario.candidate = candidate(
                "model-route-candidate-facts-v2",
                OBSERVED_AT,
                EXPIRES_AT,
                "destination-b",
                "reasoning-extended",
                "code-review");
        scenario.destination = destination(
                ModelDestinationPolicyFacts.SCHEMA_VERSION,
                OBSERVED_AT,
                EXPIRES_AT,
                Set.of());
        scenario.sourceInspection = Optional.empty();
        scenario.pricing = Optional.empty();
        scenario.assertRefused(
                ModelOutboundRouteRejectionReason.CANDIDATE_FACTS_INVALID_OR_STALE);
    }

    private static final class Scenario {
        private ProfiledModelRequest profiledRequest = profiledRequest(
                ModelLocalityRequirement.POLICY_CONSTRAINED,
                ModelDataClassification.INTERNAL,
                0);
        private ApprovedTask task = approvedTask(Set.of(ModelInvokeTool.NAME));
        private ExecutionPolicy policy = executionPolicy(
                Set.of(ModelInvokeTool.NAME), Duration.ofSeconds(31));
        private String capability = CAPABILITY;
        private Instant evaluationTime = EVALUATION_TIME;
        private ModelRouteCandidateFacts candidate = candidate(
                ModelRouteCandidateFacts.SCHEMA_VERSION,
                OBSERVED_AT,
                EXPIRES_AT,
                DESTINATION,
                MODEL_CLASS,
                CAPABILITY);
        private ModelDestinationPolicyFacts destination = destination(
                ModelDestinationPolicyFacts.SCHEMA_VERSION,
                OBSERVED_AT,
                EXPIRES_AT,
                Set.of(defaultRule()));
        private ModelRetentionFacts retention = new ModelRetentionFacts(
                Set.of(ModelRetentionUse.REQUEST_PROCESSING), Duration.ofMinutes(1));
        private ModelOutboundContent content = ModelOutboundContent.unchanged(PROMPT);
        private Optional<ModelContentInspectionFacts> sourceInspection = Optional.of(
                inspection(
                        content.sourcePromptDigest(),
                        ModelContentInspectionStatus.CLEAR,
                        ModelInspectionCoverage.COMPLETE,
                        Set.of()));
        private Optional<ModelContentInspectionFacts> outboundInspection = Optional.of(
                inspection(
                        content.outboundPromptDigest(),
                        ModelContentInspectionStatus.CLEAR,
                        ModelInspectionCoverage.COMPLETE,
                        Set.of()));
        private Optional<ModelContentRedactionFacts> redaction = Optional.empty();
        private Optional<ModelRoutePricingFacts> pricing = Optional.of(pricing(
                ModelRoutePricingFacts.SCHEMA_VERSION,
                "pricing-source",
                "USD",
                0,
                profiledRequest.executionProfile().tokenBudget()));

        private ModelOutboundRouteProposal proposal() {
            return new ModelOutboundRouteProposal(
                    ModelOutboundRouteProposal.SCHEMA_VERSION,
                    candidate,
                    destination,
                    ModelOutboundPurpose.MODEL_INFERENCE,
                    retention,
                    content,
                    sourceInspection,
                    outboundInspection,
                    redaction,
                    pricing);
        }

        private ModelOutboundRouteAdmissionDecision evaluate(
                ModelOutboundRouteProposal proposal) {
            return new ModelOutboundRouteAdmission().evaluate(
                    profiledRequest,
                    task,
                    policy,
                    capability,
                    proposal,
                    evaluationTime);
        }

        private void assertRefused(ModelOutboundRouteRejectionReason expected) {
            ModelOutboundRouteAdmissionDecision.Refused refused = assertInstanceOf(
                    ModelOutboundRouteAdmissionDecision.Refused.class,
                    evaluate(proposal()));
            assertEquals(expected, refused.reason());
        }
    }

    private static Scenario redactionScenario() {
        Scenario scenario = new Scenario();
        scenario.content = ModelOutboundContent.redacted(PROMPT, "Analyze public structure.");
        scenario.sourceInspection = Optional.of(inspection(
                scenario.content.sourcePromptDigest(),
                ModelContentInspectionStatus.REDACTION_REQUIRED,
                ModelInspectionCoverage.COMPLETE,
                Set.of("finding-a")));
        scenario.outboundInspection = Optional.of(inspection(
                scenario.content.outboundPromptDigest(),
                ModelContentInspectionStatus.CLEAR,
                ModelInspectionCoverage.COMPLETE,
                Set.of()));
        scenario.redaction = Optional.of(redaction(
                scenario.content,
                ModelInspectionCoverage.COMPLETE,
                Set.of("finding-a")));
        return scenario;
    }

    private static ProfiledModelRequest profiledRequest(
            ModelLocalityRequirement locality,
            ModelDataClassification classification,
            long costCeiling) {
        ModelTokenBudget tokenBudget = new ModelTokenBudget(10, 5, 15);
        ModelExecutionProfile profile = new ModelExecutionProfile(
                ModelExecutionProfile.SCHEMA_VERSION,
                CAPABILITY,
                MODEL_CLASS,
                locality,
                ModelReasoningRequirement.STANDARD,
                15,
                tokenBudget,
                new ModelCostBudget("USD", costCeiling),
                Duration.ofSeconds(20),
                classification);
        return new ProfiledModelRequest(
                new ModelRequest(
                        "correlation-1",
                        PROMPT,
                        MODEL_CLASS,
                        Duration.ofSeconds(30),
                        4096),
                profile);
    }

    private static ApprovedTask approvedTask(Set<String> allowedTools) {
        return new ApprovedTask(
                "admit-outbound-route",
                "Evaluate one outbound route proposal.",
                "User continuation authority.",
                allowedTools,
                "CURRENT_TASK.md");
    }

    private static ExecutionPolicy executionPolicy(
            Set<String> allowedTools,
            Duration timeout) {
        return new ExecutionPolicy(
                Path.of("."),
                allowedTools,
                Set.of(),
                1024,
                timeout,
                CancellationToken.none());
    }

    private static ModelRouteCandidateFacts candidate(
            String schemaVersion,
            Instant observedAt,
            Instant expiresAt,
            String destination,
            String modelClass,
            String capability) {
        return ModelRouteCandidateFacts.create(
                schemaVersion,
                CANDIDATE,
                "candidate-source",
                "candidate-r1",
                observedAt,
                expiresAt,
                modelClass,
                capability,
                destination);
    }

    private static ModelDestinationPolicyFacts destination(
            String schemaVersion,
            Instant observedAt,
            Instant expiresAt,
            Set<ModelDestinationAllowRule> rules) {
        return ModelDestinationPolicyFacts.create(
                schemaVersion,
                DESTINATION,
                "outbound-policy-source",
                "policy-r1",
                observedAt,
                expiresAt,
                rules);
    }

    private static ModelDestinationAllowRule defaultRule() {
        return rule(
                Set.of(ModelOutboundPurpose.MODEL_INFERENCE),
                Set.of(ModelRetentionUse.REQUEST_PROCESSING),
                Duration.ofMinutes(5),
                ModelDataClassification.INTERNAL,
                "pricing-source");
    }

    private static ModelDestinationAllowRule rule(
            Set<ModelOutboundPurpose> purposes,
            Set<ModelRetentionUse> retentionUses,
            Duration retentionDuration,
            ModelDataClassification maximumClassification,
            String pricingSource) {
        return ModelDestinationAllowRule.create(
                CANDIDATE,
                DESTINATION,
                purposes,
                retentionUses,
                retentionDuration,
                maximumClassification,
                pricingSource);
    }

    private static ModelContentInspectionFacts inspection(
            String digest,
            ModelContentInspectionStatus status,
            ModelInspectionCoverage coverage,
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
                coverage,
                status,
                findings);
    }

    private static ModelContentRedactionFacts redaction(
            ModelOutboundContent content,
            ModelInspectionCoverage coverage,
            Set<String> findings) {
        return ModelContentRedactionFacts.create(
                ModelContentRedactionFacts.SCHEMA_VERSION,
                content.sourcePromptDigest(),
                content.outboundPromptDigest(),
                "redactor-a",
                "redactor-r1",
                "ruleset-a",
                "ruleset-r1",
                OBSERVED_AT,
                EXPIRES_AT,
                coverage,
                findings);
    }

    private static ModelRoutePricingFacts pricing(
            String schemaVersion,
            String sourceIdentity,
            String currency,
            long charge,
            ModelTokenBudget tokenBudget) {
        return ModelRoutePricingFacts.create(
                schemaVersion,
                sourceIdentity,
                "pricing-r1",
                OBSERVED_AT,
                EXPIRES_AT,
                CANDIDATE,
                DESTINATION,
                MODEL_CLASS,
                "profile-token-budget-v1",
                "schedule-a",
                tokenBudget,
                currency,
                charge);
    }
}
