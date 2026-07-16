package com.enhancer.cli;

import com.enhancer.loop.AgentLoopStopReason;
import com.enhancer.tool.ToolFailureCode;
import com.enhancer.kernel.VerificationStatus;
import java.util.Objects;
import java.util.Optional;

public enum CliExitCode {
    COMPLETED(0),
    USAGE_OR_CONFIGURATION(2),
    VERIFICATION_FAILED(10),
    POLICY_DENIED(20),
    TOOL_FAILED(21),
    STAGNATED(30),
    MAX_ITERATIONS(31),
    INTERNAL_ERROR(70);

    private final int code;

    CliExitCode(int code) {
        this.code = code;
    }

    public int code() {
        return code;
    }

    static CliExitCode from(
            AgentLoopStopReason stopReason,
            VerificationStatus verificationStatus,
            Optional<ToolFailureCode> failureCode) {
        Objects.requireNonNull(stopReason, "stopReason must not be null");
        Objects.requireNonNull(
                verificationStatus,
                "verificationStatus must not be null");
        Objects.requireNonNull(failureCode, "failureCode must not be null");

        return switch (stopReason) {
            case COMPLETED -> verificationStatus == VerificationStatus.VERIFIED
                    ? COMPLETED
                    : INTERNAL_ERROR;
            case AWAITING_VERIFICATION -> VERIFICATION_FAILED;
            case FAILED -> failureCode.filter(code -> code == ToolFailureCode.POLICY_DENIED)
                    .map(ignored -> POLICY_DENIED)
                    .orElse(TOOL_FAILED);
            case STAGNATED -> STAGNATED;
            case MAX_ITERATIONS -> MAX_ITERATIONS;
        };
    }
}
