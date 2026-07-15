package com.enhancer.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.enhancer.loop.AgentLoopStopReason;
import com.enhancer.tool.ToolFailureCode;
import com.enhancer.verification.VerificationStatus;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class CliExitCodeTest {
    @Test
    void mapsStableProcessOutcomes() {
        assertEquals(0, CliExitCode.COMPLETED.code());
        assertEquals(2, CliExitCode.USAGE_OR_CONFIGURATION.code());
        assertEquals(10, CliExitCode.VERIFICATION_FAILED.code());
        assertEquals(20, CliExitCode.POLICY_DENIED.code());
        assertEquals(21, CliExitCode.TOOL_FAILED.code());
        assertEquals(30, CliExitCode.STAGNATED.code());
        assertEquals(31, CliExitCode.MAX_ITERATIONS.code());
        assertEquals(70, CliExitCode.INTERNAL_ERROR.code());

        assertEquals(
                CliExitCode.COMPLETED,
                CliExitCode.from(
                        AgentLoopStopReason.COMPLETED,
                        VerificationStatus.VERIFIED,
                        Optional.empty()));
        assertEquals(
                CliExitCode.VERIFICATION_FAILED,
                CliExitCode.from(
                        AgentLoopStopReason.AWAITING_VERIFICATION,
                        VerificationStatus.REJECTED,
                        Optional.empty()));
        assertEquals(
                CliExitCode.POLICY_DENIED,
                CliExitCode.from(
                        AgentLoopStopReason.FAILED,
                        VerificationStatus.NOT_PERFORMED,
                        Optional.of(ToolFailureCode.POLICY_DENIED)));
        assertEquals(
                CliExitCode.TOOL_FAILED,
                CliExitCode.from(
                        AgentLoopStopReason.FAILED,
                        VerificationStatus.NOT_PERFORMED,
                        Optional.of(ToolFailureCode.INVALID_REQUEST)));
        assertEquals(
                CliExitCode.STAGNATED,
                CliExitCode.from(
                        AgentLoopStopReason.STAGNATED,
                        VerificationStatus.NOT_PERFORMED,
                        Optional.of(ToolFailureCode.TIMED_OUT)));
        assertEquals(
                CliExitCode.MAX_ITERATIONS,
                CliExitCode.from(
                        AgentLoopStopReason.MAX_ITERATIONS,
                        VerificationStatus.NOT_PERFORMED,
                        Optional.of(ToolFailureCode.TIMED_OUT)));
    }
}
