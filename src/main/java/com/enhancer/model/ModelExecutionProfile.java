package com.enhancer.model;

import java.time.Duration;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Immutable provider-neutral requirements for one future model invocation.
 *
 * <p>This value is untrusted requirement data. It selects no provider and grants no
 * task, Tool, network, credential, transmission, or spend authority.
 */
public record ModelExecutionProfile(
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

    public static final String SCHEMA_VERSION = "model-execution-profile-v1";
    public static final int MAX_REQUIRED_CAPABILITY_CHARACTERS = 256;
    public static final int MAX_MODEL_CLASS_CHARACTERS = 64;
    public static final long MAX_CONTEXT_TOKENS = 1_000_000_000L;
    public static final Duration MAX_INVOCATION_TIME = Duration.ofMinutes(5);

    private static final Pattern STABLE_LABEL =
            Pattern.compile("[a-z0-9]+(?:-[a-z0-9]+)*");

    public ModelExecutionProfile {
        Objects.requireNonNull(schemaVersion, "schemaVersion must not be null");
        Objects.requireNonNull(requiredCapability, "requiredCapability must not be null");
        Objects.requireNonNull(modelClass, "modelClass must not be null");
        Objects.requireNonNull(
                localityRequirement, "localityRequirement must not be null");
        Objects.requireNonNull(
                reasoningRequirement, "reasoningRequirement must not be null");
        Objects.requireNonNull(tokenBudget, "tokenBudget must not be null");
        Objects.requireNonNull(costBudget, "costBudget must not be null");
        Objects.requireNonNull(
                maximumInvocationTime, "maximumInvocationTime must not be null");
        Objects.requireNonNull(dataClassification, "dataClassification must not be null");

        if (!SCHEMA_VERSION.equals(schemaVersion)) {
            throw new IllegalArgumentException("schemaVersion is unsupported");
        }
        validateLabel(
                requiredCapability,
                "requiredCapability",
                MAX_REQUIRED_CAPABILITY_CHARACTERS);
        validateLabel(modelClass, "modelClass", MAX_MODEL_CLASS_CHARACTERS);
        if (minimumContextTokens <= 0) {
            throw new IllegalArgumentException("minimumContextTokens must be positive");
        }
        if (minimumContextTokens > MAX_CONTEXT_TOKENS) {
            throw new IllegalArgumentException(
                    "minimumContextTokens exceeds the supported bound "
                            + MAX_CONTEXT_TOKENS);
        }
        if (tokenBudget.maxTotalTokens() > minimumContextTokens) {
            throw new IllegalArgumentException(
                    "maxTotalTokens must not exceed minimumContextTokens");
        }
        if (maximumInvocationTime.isZero() || maximumInvocationTime.isNegative()) {
            throw new IllegalArgumentException("maximumInvocationTime must be positive");
        }
        if (maximumInvocationTime.compareTo(MAX_INVOCATION_TIME) > 0) {
            throw new IllegalArgumentException(
                    "maximumInvocationTime exceeds the supported bound "
                            + MAX_INVOCATION_TIME);
        }
        if (maximumInvocationTime.getNano() % 1_000_000 != 0) {
            throw new IllegalArgumentException(
                    "maximumInvocationTime must use millisecond precision");
        }
    }

    private static void validateLabel(String value, String fieldName, int maximumCharacters) {
        if (value.length() > maximumCharacters || !STABLE_LABEL.matcher(value).matches()) {
            throw new IllegalArgumentException(
                    fieldName + " must be a stable lowercase hyphenated label of at most "
                            + maximumCharacters + " characters");
        }
    }
}
