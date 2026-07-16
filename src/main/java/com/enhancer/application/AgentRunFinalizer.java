package com.enhancer.application;

import com.enhancer.kernel.VerificationDecision;
import com.enhancer.kernel.VerificationStatus;
import com.enhancer.loop.AgentLoopStopReason;
import com.enhancer.loop.AgentRunResult;
import com.enhancer.loop.AgentRunState;
import com.enhancer.loop.VerifiedAgentRunTransition;
import com.enhancer.run.FinalizedAgentRun;
import com.enhancer.run.PolicyDecision;
import com.enhancer.run.RunRecord;
import com.enhancer.run.RunRecordStore;
import com.enhancer.run.StoredRunRecord;
import com.enhancer.tool.ToolResult;
import com.enhancer.verification.IndependentVerifier;
import com.enhancer.verification.VerificationRequest;
import java.io.IOException;
import java.time.Clock;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.Optional;

public final class AgentRunFinalizer {
    private final IndependentVerifier verifier;
    private final RunRecordStore runRecordStore;
    private final Clock clock;

    public AgentRunFinalizer(
            IndependentVerifier verifier,
            RunRecordStore runRecordStore) {
        this(verifier, runRecordStore, Clock.systemUTC());
    }

    public AgentRunFinalizer(
            IndependentVerifier verifier,
            RunRecordStore runRecordStore,
            Clock clock) {
        this.verifier = Objects.requireNonNull(verifier, "verifier must not be null");
        this.runRecordStore = Objects.requireNonNull(
                runRecordStore,
                "runRecordStore must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    public FinalizedAgentRun finalizeRun(
            AgentRunResult workerRun,
            Optional<VerificationRequest> verificationRequest) throws IOException {
        Objects.requireNonNull(workerRun, "workerRun must not be null");
        Objects.requireNonNull(
                verificationRequest,
                "verificationRequest must not be null");

        AgentRunState workerState = workerRun.state();
        ToolResult result = workerState.lastResult().orElseThrow(
                () -> new IllegalArgumentException("worker run has no Tool result"));
        VerificationDecision decision;
        Optional<String> expectedContentSha256;
        if (workerRun.stopReason() == AgentLoopStopReason.AWAITING_VERIFICATION) {
            VerificationRequest request = verificationRequest.orElseThrow(
                    () -> new IllegalArgumentException(
                            "verification-waiting run requires a VerificationRequest"));
            validateRequest(workerState, result, request);
            decision = Objects.requireNonNull(
                    verifier.verify(request),
                    "verifier decision must not be null");
            if (decision.status() == VerificationStatus.NOT_PERFORMED) {
                throw new IllegalArgumentException(
                        "verification-waiting run cannot return Not Performed");
            }
            expectedContentSha256 = Optional.of(request.expectedContentSha256());
        } else {
            if (verificationRequest.isPresent()) {
                throw new IllegalArgumentException(
                        "non-verification worker stop cannot accept a VerificationRequest");
            }
            decision = VerificationDecision.notPerformed(
                    "Worker stopped as " + workerRun.stopReason() + " before verification");
            expectedContentSha256 = Optional.empty();
        }

        AgentRunState finalState = decision.status() == VerificationStatus.VERIFIED
                ? VerifiedAgentRunTransition.apply(workerState, decision)
                : workerState;
        AgentLoopStopReason finalStopReason = decision.status() == VerificationStatus.VERIFIED
                ? AgentLoopStopReason.COMPLETED
                : workerRun.stopReason();
        RunRecord record = new RunRecord(
                workerState.executedRequest().correlationId(),
                clock.instant().truncatedTo(ChronoUnit.MILLIS),
                workerState.approvedTask(),
                workerState.executedRequest(),
                PolicyDecision.from(
                        workerRun.executionPolicy(),
                        workerState.executedRequest()),
                result,
                expectedContentSha256,
                decision,
                workerRun.iterations(),
                workerRun.stopReason(),
                finalStopReason);

        StoredRunRecord storedRecord = runRecordStore.persist(record);
        return new FinalizedAgentRun(
                finalState,
                finalStopReason,
                decision,
                record,
                storedRecord);
    }

    private void validateRequest(
            AgentRunState workerState,
            ToolResult result,
            VerificationRequest request) {
        if (!workerState.approvedTask().equals(request.approvedTask())) {
            throw new IllegalArgumentException(
                    "VerificationRequest approved task does not match the worker run");
        }
        if (!workerState.executedRequest().equals(request.toolRequest())) {
            throw new IllegalArgumentException(
                    "VerificationRequest Tool request does not match the worker run");
        }
        if (!result.equals(request.toolResult())) {
            throw new IllegalArgumentException(
                    "VerificationRequest Tool result does not match the worker run");
        }
    }
}
