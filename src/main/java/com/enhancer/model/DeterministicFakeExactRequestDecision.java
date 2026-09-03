package com.enhancer.model;

import com.enhancer.tool.ExecutionPolicy;
import java.util.Objects;

/** Opaque process-local outcome of exact deterministic-fake request preparation. */
public abstract sealed class DeterministicFakeExactRequestDecision
        permits DeterministicFakeExactRequestDecision.Ready,
                DeterministicFakeExactRequestDecision.Refused {

    private DeterministicFakeExactRequestDecision() {}

    static Ready ready(
            ModelCandidateSuitabilityDecision.Suitable suitable,
            ExecutionPolicy executionPolicy,
            long inputTokens,
            long predictedResponseUtf16Length,
            long predictedOutputTokens,
            long predictedTotalTokens) {
        return new Ready(
                suitable,
                executionPolicy,
                inputTokens,
                predictedResponseUtf16Length,
                predictedOutputTokens,
                predictedTotalTokens);
    }

    static Refused refused(
            ModelCandidateSuitabilityDecision.Suitable suitable,
            ExecutionPolicy executionPolicy,
            DeterministicFakeExactRequestRejectionReason reason) {
        return new Refused(suitable, executionPolicy, reason);
    }

    /** Successful budget evaluation retaining the exact input identities and counts. */
    public static final class Ready extends DeterministicFakeExactRequestDecision {
        private final ModelCandidateSuitabilityDecision.Suitable suitable;
        private final ExecutionPolicy executionPolicy;
        private final long inputTokens;
        private final long predictedResponseUtf16Length;
        private final long predictedOutputTokens;
        private final long predictedTotalTokens;

        private Ready(
                ModelCandidateSuitabilityDecision.Suitable suitable,
                ExecutionPolicy executionPolicy,
                long inputTokens,
                long predictedResponseUtf16Length,
                long predictedOutputTokens,
                long predictedTotalTokens) {
            this.suitable = Objects.requireNonNull(suitable, "suitable must not be null");
            this.executionPolicy = Objects.requireNonNull(
                    executionPolicy, "executionPolicy must not be null");
            this.inputTokens = requireNonNegative(inputTokens, "inputTokens");
            this.predictedResponseUtf16Length = requireNonNegative(
                    predictedResponseUtf16Length,
                    "predictedResponseUtf16Length");
            this.predictedOutputTokens = requireNonNegative(
                    predictedOutputTokens, "predictedOutputTokens");
            this.predictedTotalTokens = requireNonNegative(
                    predictedTotalTokens, "predictedTotalTokens");
        }

        public ModelCandidateSuitabilityDecision.Suitable suitable() {
            return suitable;
        }

        public ExecutionPolicy executionPolicy() {
            return executionPolicy;
        }

        public long inputTokens() {
            return inputTokens;
        }

        public long predictedResponseUtf16Length() {
            return predictedResponseUtf16Length;
        }

        public long predictedOutputTokens() {
            return predictedOutputTokens;
        }

        public long predictedTotalTokens() {
            return predictedTotalTokens;
        }

        @Override
        public String toString() {
            return "DeterministicFakeExactRequestDecision.Ready[redacted]";
        }
    }

    /** First fail-closed budget refusal retaining only exact inputs and a closed reason. */
    public static final class Refused extends DeterministicFakeExactRequestDecision {
        private final ModelCandidateSuitabilityDecision.Suitable suitable;
        private final ExecutionPolicy executionPolicy;
        private final DeterministicFakeExactRequestRejectionReason reason;

        private Refused(
                ModelCandidateSuitabilityDecision.Suitable suitable,
                ExecutionPolicy executionPolicy,
                DeterministicFakeExactRequestRejectionReason reason) {
            this.suitable = Objects.requireNonNull(suitable, "suitable must not be null");
            this.executionPolicy = Objects.requireNonNull(
                    executionPolicy, "executionPolicy must not be null");
            this.reason = Objects.requireNonNull(reason, "reason must not be null");
        }

        public ModelCandidateSuitabilityDecision.Suitable suitable() {
            return suitable;
        }

        public ExecutionPolicy executionPolicy() {
            return executionPolicy;
        }

        public DeterministicFakeExactRequestRejectionReason reason() {
            return reason;
        }

        @Override
        public String toString() {
            return "DeterministicFakeExactRequestDecision.Refused[reason=" + reason + "]";
        }
    }

    private static long requireNonNegative(long value, String name) {
        if (value < 0L) {
            throw new IllegalArgumentException(name + " must not be negative");
        }
        return value;
    }
}
