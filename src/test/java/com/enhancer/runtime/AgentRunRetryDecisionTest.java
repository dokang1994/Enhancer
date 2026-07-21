package com.enhancer.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.junit.jupiter.api.Test;

class AgentRunRetryDecisionTest {

    @Test
    void admittedHasNoReason() {
        AgentRunRetryDecision decision = AgentRunRetryDecision.admitted();
        assertTrue(decision.isAdmitted());
        assertEquals(Optional.empty(), decision.refusalReason());
    }

    @Test
    void refusedCarriesReason() {
        AgentRunRetryDecision decision =
                AgentRunRetryDecision.refused(AgentRunRetryRefusalReason.ATTEMPTS_EXHAUSTED);
        assertFalse(decision.isAdmitted());
        assertEquals(
                Optional.of(AgentRunRetryRefusalReason.ATTEMPTS_EXHAUSTED),
                decision.refusalReason());
    }

    @Test
    void refusedRejectsNullReason() {
        assertThrows(
                NullPointerException.class, () -> AgentRunRetryDecision.refused(null));
    }

    @Test
    void valueEquality() {
        assertEquals(
                AgentRunRetryDecision.refused(AgentRunRetryRefusalReason.NOT_FAILED),
                AgentRunRetryDecision.refused(AgentRunRetryRefusalReason.NOT_FAILED));
        assertEquals(AgentRunRetryDecision.admitted(), AgentRunRetryDecision.admitted());
    }
}
