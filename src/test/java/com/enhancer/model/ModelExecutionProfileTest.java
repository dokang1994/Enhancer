package com.enhancer.model;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.RecordComponent;
import java.time.Duration;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class ModelExecutionProfileTest {

    @Test
    void profileRetainsEveryExactRequirementWithDeterministicValueSemantics() {
        ModelTokenBudget tokenBudget = new ModelTokenBudget(4096, 2048, 8192);
        ModelCostBudget costBudget = new ModelCostBudget("USD", 25_000_000L);
        ModelExecutionProfile profile = profile(
                ModelExecutionProfile.SCHEMA_VERSION,
                "repository-analysis",
                "reasoning-standard",
                ModelLocalityRequirement.LOCAL_ONLY,
                ModelReasoningRequirement.STANDARD,
                16_384,
                tokenBudget,
                costBudget,
                Duration.ofSeconds(30),
                ModelDataClassification.CONFIDENTIAL);

        assertEquals("model-execution-profile-v1", profile.schemaVersion());
        assertEquals("repository-analysis", profile.requiredCapability());
        assertEquals("reasoning-standard", profile.modelClass());
        assertEquals(ModelLocalityRequirement.LOCAL_ONLY, profile.localityRequirement());
        assertEquals(ModelReasoningRequirement.STANDARD, profile.reasoningRequirement());
        assertEquals(16_384, profile.minimumContextTokens());
        assertEquals(tokenBudget, profile.tokenBudget());
        assertEquals(costBudget, profile.costBudget());
        assertEquals(Duration.ofSeconds(30), profile.maximumInvocationTime());
        assertEquals(ModelDataClassification.CONFIDENTIAL, profile.dataClassification());

        ModelExecutionProfile equalProfile = profile(
                ModelExecutionProfile.SCHEMA_VERSION,
                "repository-analysis",
                "reasoning-standard",
                ModelLocalityRequirement.LOCAL_ONLY,
                ModelReasoningRequirement.STANDARD,
                16_384,
                new ModelTokenBudget(4096, 2048, 8192),
                new ModelCostBudget("USD", 25_000_000L),
                Duration.ofSeconds(30),
                ModelDataClassification.CONFIDENTIAL);
        assertEquals(profile, equalProfile);
        assertEquals(profile.hashCode(), equalProfile.hashCode());
        assertNotEquals(profile, profile(
                ModelExecutionProfile.SCHEMA_VERSION,
                "code-review",
                "reasoning-standard",
                ModelLocalityRequirement.LOCAL_ONLY,
                ModelReasoningRequirement.STANDARD,
                16_384,
                tokenBudget,
                costBudget,
                Duration.ofSeconds(30),
                ModelDataClassification.CONFIDENTIAL));
    }

    @Test
    void vocabulariesStayExactlyClosedAndRepositoryOrdered() {
        assertArrayEquals(
                new ModelLocalityRequirement[] {
                        ModelLocalityRequirement.LOCAL_ONLY,
                        ModelLocalityRequirement.POLICY_CONSTRAINED
                },
                ModelLocalityRequirement.values());
        assertArrayEquals(
                new ModelReasoningRequirement[] {
                        ModelReasoningRequirement.MINIMAL,
                        ModelReasoningRequirement.STANDARD,
                        ModelReasoningRequirement.EXTENDED
                },
                ModelReasoningRequirement.values());
        assertArrayEquals(
                new ModelDataClassification[] {
                        ModelDataClassification.PUBLIC,
                        ModelDataClassification.INTERNAL,
                        ModelDataClassification.CONFIDENTIAL,
                        ModelDataClassification.RESTRICTED
                },
                ModelDataClassification.values());
    }

    @Test
    void profileRejectsUnsupportedSchemaAndEveryMissingComponent() {
        ModelTokenBudget tokenBudget = new ModelTokenBudget(1, 1, 2);
        ModelCostBudget costBudget = new ModelCostBudget("USD", 0);

        assertThrows(NullPointerException.class, () -> profile(
                null, "capability", "model-class", ModelLocalityRequirement.LOCAL_ONLY,
                ModelReasoningRequirement.MINIMAL, 2, tokenBudget, costBudget,
                Duration.ofMillis(1), ModelDataClassification.PUBLIC));
        assertThrows(IllegalArgumentException.class, () -> profile(
                "model-execution-profile-v2", "capability", "model-class",
                ModelLocalityRequirement.LOCAL_ONLY, ModelReasoningRequirement.MINIMAL,
                2, tokenBudget, costBudget, Duration.ofMillis(1),
                ModelDataClassification.PUBLIC));
        assertThrows(NullPointerException.class, () -> profile(
                ModelExecutionProfile.SCHEMA_VERSION, null, "model-class",
                ModelLocalityRequirement.LOCAL_ONLY, ModelReasoningRequirement.MINIMAL,
                2, tokenBudget, costBudget, Duration.ofMillis(1),
                ModelDataClassification.PUBLIC));
        assertThrows(NullPointerException.class, () -> profile(
                ModelExecutionProfile.SCHEMA_VERSION, "capability", null,
                ModelLocalityRequirement.LOCAL_ONLY, ModelReasoningRequirement.MINIMAL,
                2, tokenBudget, costBudget, Duration.ofMillis(1),
                ModelDataClassification.PUBLIC));
        assertThrows(NullPointerException.class, () -> profile(
                ModelExecutionProfile.SCHEMA_VERSION, "capability", "model-class", null,
                ModelReasoningRequirement.MINIMAL, 2, tokenBudget, costBudget,
                Duration.ofMillis(1), ModelDataClassification.PUBLIC));
        assertThrows(NullPointerException.class, () -> profile(
                ModelExecutionProfile.SCHEMA_VERSION, "capability", "model-class",
                ModelLocalityRequirement.LOCAL_ONLY, null, 2, tokenBudget, costBudget,
                Duration.ofMillis(1), ModelDataClassification.PUBLIC));
        assertThrows(NullPointerException.class, () -> profile(
                ModelExecutionProfile.SCHEMA_VERSION, "capability", "model-class",
                ModelLocalityRequirement.LOCAL_ONLY, ModelReasoningRequirement.MINIMAL,
                2, null, costBudget, Duration.ofMillis(1),
                ModelDataClassification.PUBLIC));
        assertThrows(NullPointerException.class, () -> profile(
                ModelExecutionProfile.SCHEMA_VERSION, "capability", "model-class",
                ModelLocalityRequirement.LOCAL_ONLY, ModelReasoningRequirement.MINIMAL,
                2, tokenBudget, null, Duration.ofMillis(1),
                ModelDataClassification.PUBLIC));
        assertThrows(NullPointerException.class, () -> profile(
                ModelExecutionProfile.SCHEMA_VERSION, "capability", "model-class",
                ModelLocalityRequirement.LOCAL_ONLY, ModelReasoningRequirement.MINIMAL,
                2, tokenBudget, costBudget, null, ModelDataClassification.PUBLIC));
        assertThrows(NullPointerException.class, () -> profile(
                ModelExecutionProfile.SCHEMA_VERSION, "capability", "model-class",
                ModelLocalityRequirement.LOCAL_ONLY, ModelReasoningRequirement.MINIMAL,
                2, tokenBudget, costBudget, Duration.ofMillis(1), null));
    }

    @Test
    void capabilityAndModelClassUseDistinctBoundedStableLabels() {
        ModelExecutionProfile boundary = profile(
                ModelExecutionProfile.SCHEMA_VERSION,
                "a".repeat(ModelExecutionProfile.MAX_REQUIRED_CAPABILITY_CHARACTERS),
                "m".repeat(ModelExecutionProfile.MAX_MODEL_CLASS_CHARACTERS),
                ModelLocalityRequirement.POLICY_CONSTRAINED,
                ModelReasoningRequirement.EXTENDED,
                2,
                new ModelTokenBudget(1, 1, 2),
                new ModelCostBudget("KRW", 0),
                Duration.ofMinutes(5),
                ModelDataClassification.RESTRICTED);
        assertEquals(
                ModelExecutionProfile.MAX_REQUIRED_CAPABILITY_CHARACTERS,
                boundary.requiredCapability().length());
        assertEquals(
                ModelExecutionProfile.MAX_MODEL_CLASS_CHARACTERS,
                boundary.modelClass().length());

        for (String invalid : new String[] {
                "", " ", "Upper", "under_score", "two--parts", "-leading",
                "trailing-", "\u00e9", "a".repeat(
                        ModelExecutionProfile.MAX_REQUIRED_CAPABILITY_CHARACTERS + 1)
        }) {
            assertThrows(IllegalArgumentException.class, () -> profile(
                    ModelExecutionProfile.SCHEMA_VERSION, invalid, "model-class",
                    ModelLocalityRequirement.LOCAL_ONLY, ModelReasoningRequirement.MINIMAL,
                    2, new ModelTokenBudget(1, 1, 2), new ModelCostBudget("USD", 0),
                    Duration.ofMillis(1), ModelDataClassification.PUBLIC));
        }
        for (String invalid : new String[] {
                "", " ", "Upper", "under_score", "two--parts", "-leading",
                "trailing-", "\ubaa8\ub378", "m".repeat(
                        ModelExecutionProfile.MAX_MODEL_CLASS_CHARACTERS + 1)
        }) {
            assertThrows(IllegalArgumentException.class, () -> profile(
                    ModelExecutionProfile.SCHEMA_VERSION, "capability", invalid,
                    ModelLocalityRequirement.LOCAL_ONLY, ModelReasoningRequirement.MINIMAL,
                    2, new ModelTokenBudget(1, 1, 2), new ModelCostBudget("USD", 0),
                    Duration.ofMillis(1), ModelDataClassification.PUBLIC));
        }
    }

    @Test
    void tokenBudgetRejectsInvalidBoundsAndUnsafeRelationships() {
        assertThrows(IllegalArgumentException.class, () -> new ModelTokenBudget(0, 1, 1));
        assertThrows(IllegalArgumentException.class, () -> new ModelTokenBudget(1, 0, 1));
        assertThrows(IllegalArgumentException.class, () -> new ModelTokenBudget(1, 1, 0));
        assertThrows(IllegalArgumentException.class, () -> new ModelTokenBudget(-1, 1, 1));
        assertThrows(IllegalArgumentException.class, () -> new ModelTokenBudget(
                ModelTokenBudget.MAX_TOKENS + 1, 1, 2));
        assertThrows(IllegalArgumentException.class, () -> new ModelTokenBudget(
                1, ModelTokenBudget.MAX_TOKENS + 1, 2));
        assertThrows(IllegalArgumentException.class, () -> new ModelTokenBudget(
                1, 1, ModelTokenBudget.MAX_TOKENS + 1));
        assertThrows(IllegalArgumentException.class, () -> new ModelTokenBudget(1, 1, 1));
        assertThrows(IllegalArgumentException.class, () -> new ModelTokenBudget(
                Long.MAX_VALUE, Long.MAX_VALUE, Long.MAX_VALUE));

        ModelTokenBudget boundary = new ModelTokenBudget(
                500_000_000, 500_000_000, ModelTokenBudget.MAX_TOKENS);
        assertEquals(ModelTokenBudget.MAX_TOKENS, boundary.maxTotalTokens());
    }

    @Test
    void profileBoundsContextAndRequiresTheTotalBudgetToFit() {
        ModelTokenBudget budget = new ModelTokenBudget(1, 1, 2);
        assertThrows(IllegalArgumentException.class, () -> profileWithContext(0, budget));
        assertThrows(IllegalArgumentException.class, () -> profileWithContext(-1, budget));
        assertThrows(IllegalArgumentException.class, () -> profileWithContext(
                ModelExecutionProfile.MAX_CONTEXT_TOKENS + 1, budget));
        assertThrows(IllegalArgumentException.class, () -> profileWithContext(1, budget));
        assertEquals(2, profileWithContext(2, budget).minimumContextTokens());
    }

    @Test
    void costBudgetUsesBoundedIntegerMicrounitsAndAnExplicitCurrency() {
        assertThrows(NullPointerException.class, () -> new ModelCostBudget(null, 0));
        for (String invalid : new String[] {"", "usd", "US", "USDD", "U1D", "€€€"}) {
            assertThrows(IllegalArgumentException.class, () -> new ModelCostBudget(invalid, 0));
        }
        assertThrows(IllegalArgumentException.class, () -> new ModelCostBudget("USD", -1));
        assertThrows(IllegalArgumentException.class, () -> new ModelCostBudget(
                "USD", ModelCostBudget.MAX_MICROUNITS + 1));

        assertEquals(0, new ModelCostBudget("USD", 0).maxMicrounits());
        assertEquals(
                ModelCostBudget.MAX_MICROUNITS,
                new ModelCostBudget("KRW", ModelCostBudget.MAX_MICROUNITS)
                        .maxMicrounits());
    }

    @Test
    void invocationTimeIsPositiveMillisecondPreciseAndAtMostFiveMinutes() {
        for (Duration invalid : new Duration[] {
                Duration.ZERO,
                Duration.ofMillis(-1),
                Duration.ofNanos(1),
                Duration.ofMillis(1).plusNanos(1),
                ModelExecutionProfile.MAX_INVOCATION_TIME.plusMillis(1),
                Duration.ofSeconds(Long.MAX_VALUE)
        }) {
            assertThrows(IllegalArgumentException.class, () -> profileWithTime(invalid));
        }
        assertEquals(
                Duration.ofMillis(1),
                profileWithTime(Duration.ofMillis(1)).maximumInvocationTime());
        assertEquals(
                ModelExecutionProfile.MAX_INVOCATION_TIME,
                profileWithTime(ModelExecutionProfile.MAX_INVOCATION_TIME)
                        .maximumInvocationTime());
    }

    @Test
    void recordShapeContainsOnlyTheTenProviderNeutralRequirementComponents() {
        assertTrue(ModelExecutionProfile.class.isRecord());
        assertArrayEquals(
                new String[] {
                        "schemaVersion",
                        "requiredCapability",
                        "modelClass",
                        "localityRequirement",
                        "reasoningRequirement",
                        "minimumContextTokens",
                        "tokenBudget",
                        "costBudget",
                        "maximumInvocationTime",
                        "dataClassification"
                },
                Arrays.stream(ModelExecutionProfile.class.getRecordComponents())
                        .map(RecordComponent::getName)
                        .toArray(String[]::new));
        assertArrayEquals(
                new String[] {"maxInputTokens", "maxOutputTokens", "maxTotalTokens"},
                Arrays.stream(ModelTokenBudget.class.getRecordComponents())
                        .map(RecordComponent::getName)
                        .toArray(String[]::new));
        assertArrayEquals(
                new String[] {"currencyCode", "maxMicrounits"},
                Arrays.stream(ModelCostBudget.class.getRecordComponents())
                        .map(RecordComponent::getName)
                        .toArray(String[]::new));

        Set<String> names = Arrays.stream(ModelExecutionProfile.class.getRecordComponents())
                .map(RecordComponent::getName)
                .map(String::toLowerCase)
                .collect(Collectors.toUnmodifiableSet());
        for (String forbidden : new String[] {
                "prompt", "response", "task", "tool", "provider", "endpoint",
                "destination", "credential", "price", "tokenizer", "route", "result"
        }) {
            assertFalse(
                    names.stream().anyMatch(name -> name.contains(forbidden)),
                    () -> "profile must not contain authority or provider field: " + forbidden);
        }
    }

    private static ModelExecutionProfile profileWithContext(
            long minimumContextTokens,
            ModelTokenBudget tokenBudget) {
        return profile(
                ModelExecutionProfile.SCHEMA_VERSION,
                "capability",
                "model-class",
                ModelLocalityRequirement.LOCAL_ONLY,
                ModelReasoningRequirement.MINIMAL,
                minimumContextTokens,
                tokenBudget,
                new ModelCostBudget("USD", 0),
                Duration.ofMillis(1),
                ModelDataClassification.PUBLIC);
    }

    private static ModelExecutionProfile profileWithTime(Duration maximumInvocationTime) {
        return profile(
                ModelExecutionProfile.SCHEMA_VERSION,
                "capability",
                "model-class",
                ModelLocalityRequirement.LOCAL_ONLY,
                ModelReasoningRequirement.MINIMAL,
                2,
                new ModelTokenBudget(1, 1, 2),
                new ModelCostBudget("USD", 0),
                maximumInvocationTime,
                ModelDataClassification.PUBLIC);
    }

    private static ModelExecutionProfile profile(
            String schemaVersion,
            String requiredCapability,
            String modelClass,
            ModelLocalityRequirement localityRequirement,
            ModelReasoningRequirement reasoningRequirement,
            long minimumContextTokens,
            ModelTokenBudget tokenBudget,
            ModelCostBudget costBudget,
            Duration maximumInvocationTime,
            ModelDataClassification dataClassification) {
        return new ModelExecutionProfile(
                schemaVersion,
                requiredCapability,
                modelClass,
                localityRequirement,
                reasoningRequirement,
                minimumContextTokens,
                tokenBudget,
                costBudget,
                maximumInvocationTime,
                dataClassification);
    }
}
