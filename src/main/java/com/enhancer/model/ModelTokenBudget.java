package com.enhancer.model;

/** Positive bounded token requirements for one model invocation. */
public record ModelTokenBudget(
        long maxInputTokens,
        long maxOutputTokens,
        long maxTotalTokens) {

    public static final long MAX_TOKENS = 1_000_000_000L;

    public ModelTokenBudget {
        validateTokens(maxInputTokens, "maxInputTokens");
        validateTokens(maxOutputTokens, "maxOutputTokens");
        validateTokens(maxTotalTokens, "maxTotalTokens");
        if (maxInputTokens > maxTotalTokens - maxOutputTokens) {
            throw new IllegalArgumentException(
                    "maxInputTokens plus maxOutputTokens must not exceed maxTotalTokens");
        }
    }

    private static void validateTokens(long tokens, String fieldName) {
        if (tokens <= 0) {
            throw new IllegalArgumentException(fieldName + " must be positive");
        }
        if (tokens > MAX_TOKENS) {
            throw new IllegalArgumentException(
                    fieldName + " exceeds the supported bound " + MAX_TOKENS);
        }
    }
}
