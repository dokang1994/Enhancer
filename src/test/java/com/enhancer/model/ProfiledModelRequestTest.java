package com.enhancer.model;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Modifier;
import java.lang.reflect.RecordComponent;
import java.time.Duration;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

class ProfiledModelRequestTest {

    @Test
    void retainsBothCompleteValuesWithDeterministicValueSemantics() {
        ModelRequest request = request(
                "reasoning-standard", Duration.ofSeconds(30), 4096);
        ModelExecutionProfile executionProfile = profile(
                "repository-analysis",
                "reasoning-standard",
                Duration.ofSeconds(20),
                4096,
                2048,
                8192,
                16_384);

        ProfiledModelRequest profiledRequest =
                new ProfiledModelRequest(request, executionProfile);

        assertSame(request, profiledRequest.request());
        assertSame(executionProfile, profiledRequest.executionProfile());

        ProfiledModelRequest equalValue = new ProfiledModelRequest(
                request("reasoning-standard", Duration.ofSeconds(30), 4096),
                profile(
                        "repository-analysis",
                        "reasoning-standard",
                        Duration.ofSeconds(20),
                        4096,
                        2048,
                        8192,
                        16_384));
        assertEquals(profiledRequest, equalValue);
        assertEquals(profiledRequest.hashCode(), equalValue.hashCode());
        assertNotEquals(
                profiledRequest,
                new ProfiledModelRequest(
                        request,
                        profile(
                                "code-review",
                                "reasoning-standard",
                                Duration.ofSeconds(20),
                                4096,
                                2048,
                                8192,
                                16_384)));
    }

    @Test
    void rejectsEitherMissingCompleteValue() {
        ModelRequest request = request(
                "reasoning-standard", Duration.ofSeconds(30), 4096);
        ModelExecutionProfile executionProfile = profile(
                "repository-analysis",
                "reasoning-standard",
                Duration.ofSeconds(20),
                1,
                1,
                2,
                2);

        assertThrows(
                NullPointerException.class,
                () -> new ProfiledModelRequest(null, executionProfile));
        assertThrows(
                NullPointerException.class,
                () -> new ProfiledModelRequest(request, null));
    }

    @Test
    void rejectsUnequalModelClasses() {
        ModelRequest request = request(
                "reasoning-standard", Duration.ofSeconds(30), 4096);
        ModelExecutionProfile executionProfile = profile(
                "repository-analysis",
                "reasoning-extended",
                Duration.ofSeconds(20),
                1,
                1,
                2,
                2);

        assertThrows(
                IllegalArgumentException.class,
                () -> new ProfiledModelRequest(request, executionProfile));
    }

    @Test
    void acceptsProfileTimeAtOrBelowRequestTimeoutAndRejectsGreaterTime() {
        ModelRequest request = request(
                "reasoning-standard", Duration.ofSeconds(30), 4096);

        assertEquals(
                Duration.ofSeconds(29),
                new ProfiledModelRequest(
                                request,
                                profileWithTime(Duration.ofSeconds(29)))
                        .executionProfile()
                        .maximumInvocationTime());
        assertEquals(
                Duration.ofSeconds(30),
                new ProfiledModelRequest(
                                request,
                                profileWithTime(Duration.ofSeconds(30)))
                        .executionProfile()
                        .maximumInvocationTime());
        assertThrows(
                IllegalArgumentException.class,
                () -> new ProfiledModelRequest(
                        request,
                        profileWithTime(Duration.ofMillis(30_001))));
    }

    @Test
    void keepsCapabilityAndCharacterAndTokenLimitsIndependent() {
        ProfiledModelRequest largerTokenLimit = new ProfiledModelRequest(
                request("reasoning-standard", Duration.ofSeconds(30), 1),
                profile(
                        "capability-unrelated-to-model-class",
                        "reasoning-standard",
                        Duration.ofSeconds(30),
                        1,
                        1000,
                        1001,
                        2000));
        assertEquals(1, largerTokenLimit.request().maxResponseLength());
        assertEquals(
                1000,
                largerTokenLimit.executionProfile().tokenBudget().maxOutputTokens());

        ProfiledModelRequest largerCharacterLimit = new ProfiledModelRequest(
                request("reasoning-standard", Duration.ofSeconds(30), 100_000),
                profile(
                        "different-capability",
                        "reasoning-standard",
                        Duration.ofSeconds(30),
                        1,
                        1,
                        2,
                        2));
        assertEquals(100_000, largerCharacterLimit.request().maxResponseLength());
        assertEquals(
                1,
                largerCharacterLimit.executionProfile().tokenBudget().maxOutputTokens());
        assertEquals(
                "different-capability",
                largerCharacterLimit.executionProfile().requiredCapability());
    }

    @Test
    void exposesExactlyTheTwoDataComponentsAndImplementsNoAuthorityPort() {
        assertTrue(ProfiledModelRequest.class.isRecord());
        assertTrue(Modifier.isPublic(ProfiledModelRequest.class.getModifiers()));
        assertTrue(Modifier.isFinal(ProfiledModelRequest.class.getModifiers()));
        assertArrayEquals(
                new String[] {"request", "executionProfile"},
                Arrays.stream(ProfiledModelRequest.class.getRecordComponents())
                        .map(RecordComponent::getName)
                        .toArray(String[]::new));
        assertArrayEquals(
                new Class<?>[] {ModelRequest.class, ModelExecutionProfile.class},
                Arrays.stream(ProfiledModelRequest.class.getRecordComponents())
                        .map(RecordComponent::getType)
                        .toArray(Class<?>[]::new));
        assertArrayEquals(new Class<?>[0], ProfiledModelRequest.class.getInterfaces());
    }

    private static ModelExecutionProfile profileWithTime(Duration maximumInvocationTime) {
        return profile(
                "repository-analysis",
                "reasoning-standard",
                maximumInvocationTime,
                1,
                1,
                2,
                2);
    }

    private static ModelRequest request(
            String modelClass,
            Duration timeout,
            int maxResponseLength) {
        return new ModelRequest(
                "correlation-1",
                "Analyze the repository.",
                modelClass,
                timeout,
                maxResponseLength);
    }

    private static ModelExecutionProfile profile(
            String requiredCapability,
            String modelClass,
            Duration maximumInvocationTime,
            long maxInputTokens,
            long maxOutputTokens,
            long maxTotalTokens,
            long minimumContextTokens) {
        return new ModelExecutionProfile(
                ModelExecutionProfile.SCHEMA_VERSION,
                requiredCapability,
                modelClass,
                ModelLocalityRequirement.LOCAL_ONLY,
                ModelReasoningRequirement.STANDARD,
                minimumContextTokens,
                new ModelTokenBudget(maxInputTokens, maxOutputTokens, maxTotalTokens),
                new ModelCostBudget("USD", 0),
                maximumInvocationTime,
                ModelDataClassification.INTERNAL);
    }
}
