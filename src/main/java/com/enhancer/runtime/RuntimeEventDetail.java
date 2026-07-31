package com.enhancer.runtime;

import com.enhancer.kernel.VerificationStatus;
import java.util.Objects;
import java.util.Optional;

/** Closed kind-specific runtime-event-v1 detail contract. */
public sealed interface RuntimeEventDetail {
    RuntimeEventKind kind();

    record RetryDecisionRecorded(
            boolean admitted,
            Optional<AgentRunRetryRefusalReason> refusalReason)
            implements RuntimeEventDetail {
        public RetryDecisionRecorded {
            Objects.requireNonNull(
                    refusalReason,
                    "refusalReason must not be null");
            if (admitted == refusalReason.isPresent()) {
                throw new IllegalArgumentException(
                        "an admitted retry has no refusal reason and a refusal requires one");
            }
        }

        @Override
        public RuntimeEventKind kind() {
            return RuntimeEventKind.RETRY_DECISION_RECORDED;
        }
    }

    record RetryStarted(String previousAgentRunId)
            implements RuntimeEventDetail {
        public RetryStarted {
            previousAgentRunId = RuntimeIdentity.canonicalUuid(
                    previousAgentRunId,
                    "previousAgentRunId");
        }

        @Override
        public RuntimeEventKind kind() {
            return RuntimeEventKind.RETRY_STARTED;
        }
    }

    record StagnationDetected(int iterations, int threshold)
            implements RuntimeEventDetail {
        public StagnationDetected {
            if (iterations <= 0 || threshold <= 0 || iterations < threshold) {
                throw new IllegalArgumentException(
                        "stagnation iterations and threshold must be positive "
                                + "and iterations must meet the threshold");
            }
        }

        @Override
        public RuntimeEventKind kind() {
            return RuntimeEventKind.STAGNATION_DETECTED;
        }
    }

    record TimeoutDetected(RuntimeTimeoutKind timeoutKind)
            implements RuntimeEventDetail {
        public TimeoutDetected {
            Objects.requireNonNull(timeoutKind, "timeoutKind must not be null");
        }

        @Override
        public RuntimeEventKind kind() {
            return RuntimeEventKind.TIMEOUT_DETECTED;
        }
    }

    record CancellationRequestRecorded(String controlMessageId)
            implements RuntimeEventDetail {
        public CancellationRequestRecorded {
            controlMessageId = RuntimeIdentity.canonicalUuid(
                    controlMessageId,
                    "controlMessageId");
        }

        @Override
        public RuntimeEventKind kind() {
            return RuntimeEventKind.CANCELLATION_REQUEST_RECORDED;
        }
    }

    record CancellationApplied(String controlMessageId)
            implements RuntimeEventDetail {
        public CancellationApplied {
            controlMessageId = RuntimeIdentity.canonicalUuid(
                    controlMessageId,
                    "controlMessageId");
        }

        @Override
        public RuntimeEventKind kind() {
            return RuntimeEventKind.CANCELLATION_APPLIED;
        }
    }

    record VerificationRecorded(VerificationStatus status)
            implements RuntimeEventDetail {
        public VerificationRecorded {
            Objects.requireNonNull(status, "status must not be null");
        }

        @Override
        public RuntimeEventKind kind() {
            return RuntimeEventKind.VERIFICATION_RECORDED;
        }
    }

    record WorkItemTerminated(WorkItemDisposition disposition)
            implements RuntimeEventDetail {
        public WorkItemTerminated {
            Objects.requireNonNull(
                    disposition,
                    "disposition must not be null");
        }

        @Override
        public RuntimeEventKind kind() {
            return RuntimeEventKind.WORK_ITEM_TERMINATED;
        }
    }
}
