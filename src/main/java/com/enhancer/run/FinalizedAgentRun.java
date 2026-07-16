package com.enhancer.run;

import com.enhancer.loop.AgentLoopStatus;
import com.enhancer.loop.AgentLoopStopReason;
import com.enhancer.loop.AgentRunState;
import com.enhancer.kernel.VerificationDecision;
import com.enhancer.kernel.VerificationStatus;
import java.util.Objects;

public record FinalizedAgentRun(
        AgentRunState state,
        AgentLoopStopReason stopReason,
        VerificationDecision verification,
        RunRecord record,
        StoredRunRecord storedRecord) {

    public FinalizedAgentRun {
        Objects.requireNonNull(state, "state must not be null");
        Objects.requireNonNull(stopReason, "stopReason must not be null");
        Objects.requireNonNull(verification, "verification must not be null");
        Objects.requireNonNull(record, "record must not be null");
        Objects.requireNonNull(storedRecord, "storedRecord must not be null");
        if (record.finalStopReason() != stopReason) {
            throw new IllegalArgumentException("record and final stop reasons must match");
        }
        if (!record.verification().equals(verification)) {
            throw new IllegalArgumentException("record and final verification must match");
        }
        if (verification.status() == VerificationStatus.VERIFIED) {
            if (state.status() != AgentLoopStatus.COMPLETED
                    || stopReason != AgentLoopStopReason.COMPLETED) {
                throw new IllegalArgumentException(
                        "Verified finalization requires completed state and stop reason");
            }
        }
    }
}
