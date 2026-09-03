package com.enhancer.model;

import java.util.Objects;

/** Opaque process-local result of one exact deterministic-fake invocation attempt. */
public abstract sealed class DeterministicFakeExactRequestInvocationResult
        permits DeterministicFakeExactRequestInvocationResult.Succeeded,
                DeterministicFakeExactRequestInvocationResult.Refused,
                DeterministicFakeExactRequestInvocationResult.GatewayFailed {

    private DeterministicFakeExactRequestInvocationResult() {}

    static Succeeded succeeded(
            DeterministicFakeExactRequestDecision.Ready ready,
            ModelResponse response) {
        return new Succeeded(ready, response);
    }

    static Refused refused(
            DeterministicFakeExactRequestDecision.Ready ready,
            DeterministicFakeExactRequestInvocationRejectionReason reason) {
        return new Refused(ready, reason);
    }

    static GatewayFailed gatewayFailed(
            DeterministicFakeExactRequestDecision.Ready ready,
            ModelFailureCode failureCode) {
        return new GatewayFailed(ready, failureCode);
    }

    /** The exact gateway returned one still-untrusted response. */
    public static final class Succeeded
            extends DeterministicFakeExactRequestInvocationResult {
        private final DeterministicFakeExactRequestDecision.Ready ready;
        private final ModelResponse response;

        private Succeeded(
                DeterministicFakeExactRequestDecision.Ready ready,
                ModelResponse response) {
            this.ready = Objects.requireNonNull(ready, "ready must not be null");
            this.response = Objects.requireNonNull(response, "response must not be null");
        }

        public DeterministicFakeExactRequestDecision.Ready ready() {
            return ready;
        }

        public ModelResponse response() {
            return response;
        }

        @Override
        public String toString() {
            return "DeterministicFakeExactRequestInvocationResult.Succeeded[redacted]";
        }
    }

    /** The first deterministic pre-call policy refusal. */
    public static final class Refused
            extends DeterministicFakeExactRequestInvocationResult {
        private final DeterministicFakeExactRequestDecision.Ready ready;
        private final DeterministicFakeExactRequestInvocationRejectionReason reason;

        private Refused(
                DeterministicFakeExactRequestDecision.Ready ready,
                DeterministicFakeExactRequestInvocationRejectionReason reason) {
            this.ready = Objects.requireNonNull(ready, "ready must not be null");
            this.reason = Objects.requireNonNull(reason, "reason must not be null");
        }

        public DeterministicFakeExactRequestDecision.Ready ready() {
            return ready;
        }

        public DeterministicFakeExactRequestInvocationRejectionReason reason() {
            return reason;
        }

        @Override
        public String toString() {
            return "DeterministicFakeExactRequestInvocationResult.Refused[reason="
                    + reason + "]";
        }
    }

    /** One exact closed gateway code with no retained exception diagnostic. */
    public static final class GatewayFailed
            extends DeterministicFakeExactRequestInvocationResult {
        private final DeterministicFakeExactRequestDecision.Ready ready;
        private final ModelFailureCode failureCode;

        private GatewayFailed(
                DeterministicFakeExactRequestDecision.Ready ready,
                ModelFailureCode failureCode) {
            this.ready = Objects.requireNonNull(ready, "ready must not be null");
            this.failureCode = Objects.requireNonNull(
                    failureCode, "failureCode must not be null");
        }

        public DeterministicFakeExactRequestDecision.Ready ready() {
            return ready;
        }

        public ModelFailureCode failureCode() {
            return failureCode;
        }

        @Override
        public String toString() {
            return "DeterministicFakeExactRequestInvocationResult.GatewayFailed[code="
                    + failureCode + "]";
        }
    }
}
