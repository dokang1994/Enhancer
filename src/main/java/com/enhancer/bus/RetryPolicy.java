package com.enhancer.bus;

/**
 * An immutable bounded attempt policy for synchronous delivery retry. A delivery invokes a
 * subscriber's handler at most {@code maxAttempts} times, immediately and with no delay between
 * attempts, before the delivery is dead-lettered; explicit re-delivery applies the same bound
 * again. The bound keeps one publication from turning into unbounded synchronous work.
 */
public record RetryPolicy(int maxAttempts) {

    public static final int MAX_ATTEMPTS = 10;

    public RetryPolicy {
        if (maxAttempts < 1 || maxAttempts > MAX_ATTEMPTS) {
            throw new IllegalArgumentException(
                    "maxAttempts must be between 1 and " + MAX_ATTEMPTS);
        }
    }

    public static RetryPolicy of(int maxAttempts) {
        return new RetryPolicy(maxAttempts);
    }

    /** Returns the policy that invokes a handler exactly once and never retries. */
    public static RetryPolicy singleAttempt() {
        return new RetryPolicy(1);
    }
}
