package com.enhancer.runtime;

/**
 * Immutable bound on how many terminal AgentRun attempts are permitted for one WorkItem,
 * counting the first attempt. {@code maxAttempts == 1} therefore permits no retry.
 */
public record AgentRunRetryPolicy(int maxAttempts) {

    public static final int MAX_ATTEMPTS = 16;

    public AgentRunRetryPolicy {
        if (maxAttempts < 1 || maxAttempts > MAX_ATTEMPTS) {
            throw new IllegalArgumentException(
                    "maxAttempts must be between 1 and " + MAX_ATTEMPTS);
        }
    }

    public static AgentRunRetryPolicy of(int maxAttempts) {
        return new AgentRunRetryPolicy(maxAttempts);
    }
}
