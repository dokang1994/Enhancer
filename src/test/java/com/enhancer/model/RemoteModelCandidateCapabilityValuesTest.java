package com.enhancer.model;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class RemoteModelCandidateCapabilityValuesTest {

    private static final Instant OBSERVED_AT = Instant.parse("2026-10-16T00:00:00Z");
    private static final Instant EXPIRES_AT = Instant.parse("2026-10-16T00:05:00Z");

    @Test
    void refusalVocabularyAndDecisionShapesAreClosedInRfcOrder() {
        assertArrayEquals(
                new RemoteModelCandidateSuitabilityRefusalReason[] {
                        RemoteModelCandidateSuitabilityRefusalReason.SOURCE_CONTENT_BINDING_MISMATCH,
                        RemoteModelCandidateSuitabilityRefusalReason.CANDIDATE_FACTS_INVALID_OR_STALE,
                        RemoteModelCandidateSuitabilityRefusalReason.CAPABILITY_FACTS_INVALID_OR_STALE,
                        RemoteModelCandidateSuitabilityRefusalReason.CANDIDATE_BINDING_MISMATCH,
                        RemoteModelCandidateSuitabilityRefusalReason.DESTINATION_BINDING_MISMATCH,
                        RemoteModelCandidateSuitabilityRefusalReason.PROFILE_BINDING_MISMATCH,
                        RemoteModelCandidateSuitabilityRefusalReason.PRICING_FACTS_INVALID_OR_STALE,
                        RemoteModelCandidateSuitabilityRefusalReason.PRICING_BINDING_MISMATCH,
                        RemoteModelCandidateSuitabilityRefusalReason.TOKEN_SEMANTICS_MISMATCH,
                        RemoteModelCandidateSuitabilityRefusalReason.REASONING_REQUIREMENT_UNSUPPORTED,
                        RemoteModelCandidateSuitabilityRefusalReason.CONTEXT_CAPACITY_INSUFFICIENT,
                        RemoteModelCandidateSuitabilityRefusalReason.INPUT_TOKEN_CAPACITY_INSUFFICIENT,
                        RemoteModelCandidateSuitabilityRefusalReason.OUTPUT_TOKEN_CAPACITY_INSUFFICIENT,
                        RemoteModelCandidateSuitabilityRefusalReason.TOTAL_TOKEN_CAPACITY_INSUFFICIENT,
                        RemoteModelCandidateSuitabilityRefusalReason.INVOCATION_TIME_UNSUPPORTED,
                        RemoteModelCandidateSuitabilityRefusalReason.DATA_CLASSIFICATION_UNSUPPORTED,
                        RemoteModelCandidateSuitabilityRefusalReason.COST_CURRENCY_MISMATCH,
                        RemoteModelCandidateSuitabilityRefusalReason.COST_CEILING_EXCEEDED
                },
                RemoteModelCandidateSuitabilityRefusalReason.values());

        assertTrue(RemoteModelCandidateSuitabilityDecision.class.isSealed());
        assertArrayEquals(
                new Class<?>[] {
                        RemoteModelCandidateSuitabilityDecision.Suitable.class,
                        RemoteModelCandidateSuitabilityDecision.Refused.class
                },
                RemoteModelCandidateSuitabilityDecision.class.getPermittedSubclasses());
        assertTrue(RemoteModelCandidateSuitabilityDecision.Suitable.class.isRecord());
        assertTrue(RemoteModelCandidateSuitabilityDecision.Refused.class.isRecord());
    }

    @Test
    void capabilityFactsRetainEveryBoundedProviderNeutralFact() {
        ModelTokenBudget costBasis = new ModelTokenBudget(10, 5, 15);
        ModelTokenBudget maximumBudget = new ModelTokenBudget(100, 50, 150);
        RemoteModelCandidateCapabilityFacts facts = facts(costBasis, maximumBudget);

        assertEquals(RemoteModelCandidateCapabilityFacts.SCHEMA_VERSION, facts.schemaVersion());
        assertEquals("capability-source", facts.factSourceIdentity());
        assertEquals("capability-r1", facts.sourceRevision());
        assertEquals("capacity-evidence-a", facts.capacityEvidenceIdentity());
        assertEquals(OBSERVED_AT, facts.observedAt());
        assertEquals(EXPIRES_AT, facts.expiresAt());
        assertEquals("candidate-a", facts.candidateIdentity());
        assertEquals("destination-a", facts.destinationIdentity());
        assertEquals("reasoning-standard", facts.modelClass());
        assertEquals("repository-analysis", facts.requiredCapability());
        assertEquals(ModelRoutePricingFacts.TOKEN_SEMANTICS_IDENTITY,
                facts.tokenSemanticsIdentity());
        assertEquals("schedule-a", facts.priceScheduleIdentity());
        assertSame(costBasis, facts.tokenBudgetBasis());
        assertEquals(ModelReasoningRequirement.EXTENDED,
                facts.maximumReasoningRequirement());
        assertEquals(1_000, facts.maximumContextTokens());
        assertSame(maximumBudget, facts.maximumTokenBudget());
        assertEquals(Duration.ofSeconds(60), facts.maximumInvocationTime());
        assertEquals("USD", facts.costCurrencyCode());
        assertEquals(987_654_321, facts.maximumCostMicrounits());
        assertEquals(ModelDataClassification.RESTRICTED,
                facts.maximumDataClassification());
    }

    @Test
    void capabilityFactsRejectInvalidIntrinsicShapes() {
        assertThrows(NullPointerException.class,
                () -> facts(null, new ModelTokenBudget(100, 50, 150)));
        assertThrows(NullPointerException.class,
                () -> facts(new ModelTokenBudget(10, 5, 15), null));
        assertThrows(IllegalArgumentException.class,
                () -> createFacts(
                        OBSERVED_AT,
                        OBSERVED_AT,
                        1_000,
                        new ModelTokenBudget(100, 50, 150),
                        Duration.ofSeconds(60),
                        "USD",
                        1));
        assertThrows(IllegalArgumentException.class,
                () -> createFacts(
                        OBSERVED_AT,
                        EXPIRES_AT,
                        0,
                        new ModelTokenBudget(100, 50, 150),
                        Duration.ofSeconds(60),
                        "USD",
                        1));
        assertThrows(IllegalArgumentException.class,
                () -> createFacts(
                        OBSERVED_AT,
                        EXPIRES_AT,
                        ModelExecutionProfile.MAX_CONTEXT_TOKENS + 1,
                        new ModelTokenBudget(100, 50, 150),
                        Duration.ofSeconds(60),
                        "USD",
                        1));
        assertThrows(IllegalArgumentException.class,
                () -> createFacts(
                        OBSERVED_AT,
                        EXPIRES_AT,
                        1_000,
                        new ModelTokenBudget(100, 50, 150),
                        Duration.ZERO,
                        "USD",
                        1));
        assertThrows(IllegalArgumentException.class,
                () -> createFacts(
                        OBSERVED_AT,
                        EXPIRES_AT,
                        1_000,
                        new ModelTokenBudget(100, 50, 150),
                        Duration.ofNanos(1_000_001),
                        "USD",
                        1));
        assertThrows(IllegalArgumentException.class,
                () -> createFacts(
                        OBSERVED_AT,
                        EXPIRES_AT,
                        1_000,
                        new ModelTokenBudget(100, 50, 150),
                        ModelExecutionProfile.MAX_INVOCATION_TIME.plusMillis(1),
                        "USD",
                        1));
        assertThrows(IllegalArgumentException.class,
                () -> createFacts(
                        OBSERVED_AT,
                        EXPIRES_AT,
                        1_000,
                        new ModelTokenBudget(100, 50, 150),
                        Duration.ofSeconds(60),
                        "usd",
                        1));
        assertThrows(IllegalArgumentException.class,
                () -> createFacts(
                        OBSERVED_AT,
                        EXPIRES_AT,
                        1_000,
                        new ModelTokenBudget(100, 50, 150),
                        Duration.ofSeconds(60),
                        "USD",
                        -1));
    }

    @Test
    void capabilityFactsAndDecisionsUseSafeRenderingAndRejectNullPayloads() {
        RemoteModelCandidateCapabilityFacts facts = facts(
                new ModelTokenBudget(10, 5, 15),
                new ModelTokenBudget(100, 50, 150));
        String rendered = facts.toString();

        assertTrue(rendered.contains("capability-source"));
        assertTrue(rendered.contains("capacity-evidence-a"));
        assertFalse(rendered.contains("987654321"));
        assertFalse(rendered.contains("1000"));
        assertFalse(rendered.contains("https://provider.invalid"));
        assertFalse(rendered.contains("credential-sentinel"));

        assertThrows(NullPointerException.class,
                () -> new RemoteModelCandidateSuitabilityDecision.Suitable(null));
        assertThrows(NullPointerException.class,
                () -> new RemoteModelCandidateSuitabilityDecision.Refused(null));
        RemoteModelCandidateSuitabilityDecision.Refused refused =
                new RemoteModelCandidateSuitabilityDecision.Refused(
                        RemoteModelCandidateSuitabilityRefusalReason.COST_CEILING_EXCEEDED);
        assertEquals(RemoteModelCandidateSuitabilityRefusalReason.COST_CEILING_EXCEEDED,
                refused.reason());
    }

    @Test
    void capabilityFactsExposeNoPublicConstructionPath() {
        assertTrue(Modifier.isFinal(RemoteModelCandidateCapabilityFacts.class.getModifiers()));
        for (Constructor<?> constructor
                : RemoteModelCandidateCapabilityFacts.class.getDeclaredConstructors()) {
            assertFalse(Modifier.isPublic(constructor.getModifiers()));
            assertFalse(Modifier.isProtected(constructor.getModifiers()));
        }
    }

    private static RemoteModelCandidateCapabilityFacts facts(
            ModelTokenBudget costBasis,
            ModelTokenBudget maximumBudget) {
        return RemoteModelCandidateCapabilityFacts.create(
                RemoteModelCandidateCapabilityFacts.SCHEMA_VERSION,
                "capability-source",
                "capability-r1",
                "capacity-evidence-a",
                OBSERVED_AT,
                EXPIRES_AT,
                "candidate-a",
                "destination-a",
                "reasoning-standard",
                "repository-analysis",
                ModelRoutePricingFacts.TOKEN_SEMANTICS_IDENTITY,
                "schedule-a",
                costBasis,
                ModelReasoningRequirement.EXTENDED,
                1_000,
                maximumBudget,
                Duration.ofSeconds(60),
                "USD",
                987_654_321,
                ModelDataClassification.RESTRICTED);
    }

    private static RemoteModelCandidateCapabilityFacts createFacts(
            Instant observedAt,
            Instant expiresAt,
            long maximumContextTokens,
            ModelTokenBudget maximumBudget,
            Duration maximumInvocationTime,
            String currencyCode,
            long maximumCostMicrounits) {
        return RemoteModelCandidateCapabilityFacts.create(
                RemoteModelCandidateCapabilityFacts.SCHEMA_VERSION,
                "capability-source",
                "capability-r1",
                "capacity-evidence-a",
                observedAt,
                expiresAt,
                "candidate-a",
                "destination-a",
                "reasoning-standard",
                "repository-analysis",
                ModelRoutePricingFacts.TOKEN_SEMANTICS_IDENTITY,
                "schedule-a",
                new ModelTokenBudget(10, 5, 15),
                ModelReasoningRequirement.EXTENDED,
                maximumContextTokens,
                maximumBudget,
                maximumInvocationTime,
                currencyCode,
                maximumCostMicrounits,
                ModelDataClassification.RESTRICTED);
    }
}
