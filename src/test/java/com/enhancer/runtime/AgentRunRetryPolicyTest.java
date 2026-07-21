package com.enhancer.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class AgentRunRetryPolicyTest {

    @Test
    void acceptsLowerBound() {
        assertEquals(1, AgentRunRetryPolicy.of(1).maxAttempts());
    }

    @Test
    void acceptsUpperBound() {
        assertEquals(
                AgentRunRetryPolicy.MAX_ATTEMPTS,
                AgentRunRetryPolicy.of(AgentRunRetryPolicy.MAX_ATTEMPTS).maxAttempts());
    }

    @Test
    void rejectsZero() {
        assertThrows(IllegalArgumentException.class, () -> AgentRunRetryPolicy.of(0));
    }

    @Test
    void rejectsAboveMaximum() {
        assertThrows(
                IllegalArgumentException.class,
                () -> AgentRunRetryPolicy.of(AgentRunRetryPolicy.MAX_ATTEMPTS + 1));
    }
}
