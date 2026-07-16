package com.enhancer.loop;

import com.enhancer.kernel.VerificationDecision;

/**
 * Explicit application-facing port for the invariant-checked in-memory verified transition.
 * Durable completion still requires application finalization and RunRecord persistence.
 */
public final class VerifiedAgentRunTransition {
    private VerifiedAgentRunTransition() {
    }

    public static AgentRunState apply(
            AgentRunState current,
            VerificationDecision decision) {
        return AgentRunState.completedAfterVerification(
                current,
                decision);
    }
}
