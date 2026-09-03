package com.enhancer.model;

import com.enhancer.tool.ExecutionPolicy;
import java.util.Objects;

/** Field-free exact-request budget preparation for the closed deterministic fake. */
public final class DeterministicFakeExactRequestPreparation {

    public DeterministicFakeExactRequestDecision evaluate(
            ModelCandidateSuitabilityDecision.Suitable suitable,
            ExecutionPolicy executionPolicy) {
        Objects.requireNonNull(suitable, "suitable must not be null");
        Objects.requireNonNull(executionPolicy, "executionPolicy must not be null");

        ProfiledModelRequest profiledRequest = suitable.admitted().profiledRequest();
        ModelRequest request = profiledRequest.request();
        ModelTokenBudget budget = profiledRequest.executionProfile().tokenBudget();
        String prompt = request.prompt();
        long promptUtf16Length = prompt.length();

        final long inputTokens;
        try {
            inputTokens = new DeterministicFakeTokenCounter().count(prompt);
        } catch (IllegalArgumentException exception) {
            return DeterministicFakeExactRequestDecision.refused(
                    suitable,
                    executionPolicy,
                    DeterministicFakeExactRequestRejectionReason.MALFORMED_PROMPT);
        }
        if (inputTokens > budget.maxInputTokens()) {
            return DeterministicFakeExactRequestDecision.refused(
                    suitable,
                    executionPolicy,
                    DeterministicFakeExactRequestRejectionReason
                            .INPUT_TOKEN_BUDGET_EXCEEDED);
        }

        long predictedResponseUtf16Length =
                DeterministicFakeTokenCounter.responseUtf16Length(promptUtf16Length);
        long predictedOutputTokens =
                DeterministicFakeTokenCounter.responseTokenCount(promptUtf16Length, inputTokens);
        if (predictedResponseUtf16Length > request.maxResponseLength()) {
            return DeterministicFakeExactRequestDecision.refused(
                    suitable,
                    executionPolicy,
                    DeterministicFakeExactRequestRejectionReason
                            .PREDICTED_RESPONSE_UTF16_LENGTH_BUDGET_EXCEEDED);
        }
        if (predictedOutputTokens > budget.maxOutputTokens()) {
            return DeterministicFakeExactRequestDecision.refused(
                    suitable,
                    executionPolicy,
                    DeterministicFakeExactRequestRejectionReason
                            .PREDICTED_OUTPUT_TOKEN_BUDGET_EXCEEDED);
        }

        long predictedTotalTokens = Math.addExact(inputTokens, predictedOutputTokens);
        if (predictedTotalTokens > budget.maxTotalTokens()) {
            return DeterministicFakeExactRequestDecision.refused(
                    suitable,
                    executionPolicy,
                    DeterministicFakeExactRequestRejectionReason
                            .PREDICTED_TOTAL_TOKEN_BUDGET_EXCEEDED);
        }
        return DeterministicFakeExactRequestDecision.ready(
                suitable,
                executionPolicy,
                inputTokens,
                predictedResponseUtf16Length,
                predictedOutputTokens,
                predictedTotalTokens);
    }
}
