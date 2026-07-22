package com.enhancer.runtime;

import com.enhancer.bus.MessageEnvelope;
import com.enhancer.bus.ResultPayload;
import com.enhancer.kernel.VerificationStatus;
import java.util.Objects;
import java.util.Optional;

/**
 * Immutable lifecycle state for one attempt retained in runtime schema-v2 history.
 */
public record RuntimeAgentRun(
        String agentRunId,
        String goalId,
        String workItemId,
        RuntimeAgentRunStatus status,
        Optional<AgentRunLease> lease,
        Optional<MessageEnvelope> resultMessage) {

    public RuntimeAgentRun {
        agentRunId = RuntimeIdentity.canonicalUuid(
                agentRunId, "agentRunId");
        goalId = RuntimeIdentity.canonicalUuid(goalId, "goalId");
        workItemId = RuntimeIdentity.canonicalUuid(
                workItemId, "workItemId");
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(lease, "lease must not be null");
        Objects.requireNonNull(
                resultMessage, "resultMessage must not be null");
        if (agentRunId.equals(goalId)
                || agentRunId.equals(workItemId)) {
            throw new IllegalArgumentException(
                    "agentRunId must be distinct from goal and work identities");
        }
        if (status.isTerminal() != resultMessage.isPresent()) {
            throw new IllegalArgumentException(
                    "terminal AgentRun state and result presence must match");
        }
        if ((status == RuntimeAgentRunStatus.EXECUTING)
                != lease.isPresent()) {
            throw new IllegalArgumentException(
                    "only executing AgentRun state may carry a lease");
        }
        resultMessage.ifPresent(message -> validateTerminal(status, message));
    }

    RuntimeAgentRun transition(RuntimeAgentRunStatus nextStatus) {
        return new RuntimeAgentRun(
                agentRunId,
                goalId,
                workItemId,
                nextStatus,
                Optional.empty(),
                Optional.empty());
    }

    RuntimeAgentRun executeWith(AgentRunLease nextLease) {
        return new RuntimeAgentRun(
                agentRunId,
                goalId,
                workItemId,
                RuntimeAgentRunStatus.EXECUTING,
                Optional.of(nextLease),
                Optional.empty());
    }

    RuntimeAgentRun renew(AgentRunLease renewedLease) {
        return new RuntimeAgentRun(
                agentRunId,
                goalId,
                workItemId,
                RuntimeAgentRunStatus.EXECUTING,
                Optional.of(renewedLease),
                Optional.empty());
    }

    RuntimeAgentRun terminate(
            RuntimeAgentRunStatus terminalStatus,
            MessageEnvelope message) {
        return new RuntimeAgentRun(
                agentRunId,
                goalId,
                workItemId,
                terminalStatus,
                Optional.empty(),
                Optional.of(message));
    }

    private static void validateTerminal(
            RuntimeAgentRunStatus status,
            MessageEnvelope message) {
        Objects.requireNonNull(message, "result message must not be null");
        if (!(message.payload() instanceof ResultPayload payload)) {
            throw new IllegalArgumentException(
                    "terminal AgentRun message must carry ResultPayload");
        }
        if (status == RuntimeAgentRunStatus.COMPLETED
                && payload.verificationStatus()
                        != VerificationStatus.VERIFIED) {
            throw new IllegalArgumentException(
                    "completed AgentRun requires Verified result");
        }
        if (status == RuntimeAgentRunStatus.FAILED
                && payload.verificationStatus()
                        == VerificationStatus.VERIFIED) {
            throw new IllegalArgumentException(
                    "failed AgentRun cannot carry Verified result");
        }
    }
}
