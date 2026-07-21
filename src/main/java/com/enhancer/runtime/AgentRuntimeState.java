package com.enhancer.runtime;

import com.enhancer.bus.ControlPayload;
import com.enhancer.bus.MessageEnvelope;
import com.enhancer.bus.ResultPayload;
import com.enhancer.kernel.VerificationStatus;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Immutable schema-v1 Goal and AgentRun lifecycle state.
 */
public final class AgentRuntimeState {
    public static final int CURRENT_SCHEMA_VERSION = 1;
    public static final int MAX_CONTROL_REQUESTS = 256;

    private final int schemaVersion;
    private final long revision;
    private final long lastIssuedFenceToken;
    private final RuntimeGoal goal;
    private final Optional<RuntimeAgentRun> agentRun;
    private final List<MessageEnvelope> controlRequests;

    AgentRuntimeState(
            int schemaVersion,
            long revision,
            long lastIssuedFenceToken,
            RuntimeGoal goal,
            Optional<RuntimeAgentRun> agentRun) {
        this(
                schemaVersion,
                revision,
                lastIssuedFenceToken,
                goal,
                agentRun,
                List.of());
    }

    AgentRuntimeState(
            int schemaVersion,
            long revision,
            long lastIssuedFenceToken,
            RuntimeGoal goal,
            Optional<RuntimeAgentRun> agentRun,
            List<MessageEnvelope> controlRequests) {
        if (schemaVersion != CURRENT_SCHEMA_VERSION) {
            throw new IllegalArgumentException(
                    "Agent runtime schema version is unsupported");
        }
        if (revision < 0) {
            throw new IllegalArgumentException(
                    "revision must not be negative");
        }
        this.schemaVersion = schemaVersion;
        this.revision = revision;
        if (lastIssuedFenceToken < 0) {
            throw new IllegalArgumentException(
                    "lastIssuedFenceToken must not be negative");
        }
        this.lastIssuedFenceToken = lastIssuedFenceToken;
        this.goal = Objects.requireNonNull(goal, "goal must not be null");
        this.agentRun = Objects.requireNonNull(
                agentRun, "agentRun must not be null");
        this.controlRequests = List.copyOf(Objects.requireNonNull(
                controlRequests, "controlRequests must not be null"));
        validateStructure();
    }

    public static AgentRuntimeState initial(
            String goalId,
            WorkItem workItem) {
        return new AgentRuntimeState(
                CURRENT_SCHEMA_VERSION,
                0,
                0,
                new RuntimeGoal(
                        goalId,
                        workItem,
                        RuntimeGoalStatus.ACCEPTED),
                Optional.empty());
    }

    public int schemaVersion() {
        return schemaVersion;
    }

    public long revision() {
        return revision;
    }

    public long lastIssuedFenceToken() {
        return lastIssuedFenceToken;
    }

    public RuntimeGoal goal() {
        return goal;
    }

    public Optional<RuntimeAgentRun> agentRun() {
        return agentRun;
    }

    public List<MessageEnvelope> controlRequests() {
        return controlRequests;
    }

    AgentRuntimeState beginAgentRun(String agentRunId) {
        if (agentRun.isPresent()
                || goal.status() != RuntimeGoalStatus.ACCEPTED) {
            throw new IllegalStateException(
                    "schema v1 permits exactly one AgentRun");
        }
        String canonicalAgentRunId = RuntimeIdentity.canonicalUuid(
                agentRunId, "agentRunId");
        if (canonicalAgentRunId.equals(
                goal.workItem().workMessage().messageId())) {
            throw new IllegalArgumentException(
                    "agentRunId must be distinct from the work message identity");
        }
        RuntimeAgentRun run = new RuntimeAgentRun(
                canonicalAgentRunId,
                goal.goalId(),
                goal.workItem().workItemId(),
                RuntimeAgentRunStatus.PLANNING,
                Optional.empty(),
                Optional.empty());
        return next(
                goal.withStatus(RuntimeGoalStatus.ACTIVE),
                Optional.of(run));
    }

    AgentRuntimeState markReady(String agentRunId) {
        return transition(
                agentRunId,
                RuntimeAgentRunStatus.PLANNING,
                RuntimeAgentRunStatus.READY);
    }

    AgentRuntimeState acquireLease(
            String agentRunId,
            String ownerId,
            Instant issuedAt,
            Duration duration) {
        RuntimeAgentRun current = requireRun(
                agentRunId,
                RuntimeAgentRunStatus.READY);
        if (lastIssuedFenceToken == Long.MAX_VALUE) {
            throw new IllegalStateException(
                    "AgentRun fence token space is exhausted");
        }
        long nextFenceToken = lastIssuedFenceToken + 1;
        AgentRunLease lease = AgentRunLease.issue(
                ownerId,
                nextFenceToken,
                issuedAt,
                duration);
        return next(
                goal,
                Optional.of(current.executeWith(lease)),
                nextFenceToken);
    }

    AgentRuntimeState renewLease(
            String agentRunId,
            String ownerId,
            long fenceToken,
            Instant renewedAt,
            Duration duration) {
        RuntimeAgentRun current = requireRun(
                agentRunId,
                RuntimeAgentRunStatus.EXECUTING);
        AgentRunLease lease = current.lease().orElseThrow();
        lease.requireCurrent(ownerId, fenceToken, renewedAt);
        return next(
                goal,
                Optional.of(current.renew(
                        lease.renew(renewedAt, duration))),
                lastIssuedFenceToken);
    }

    AgentRuntimeState completeExecution(
            String agentRunId,
            String ownerId,
            long fenceToken,
            Instant completedAt) {
        RuntimeAgentRun current = requireRun(
                agentRunId,
                RuntimeAgentRunStatus.EXECUTING);
        current.lease().orElseThrow().requireCurrent(
                ownerId,
                fenceToken,
                completedAt);
        return next(
                goal,
                Optional.of(current.transition(
                        RuntimeAgentRunStatus.AWAITING_VERIFICATION)),
                lastIssuedFenceToken);
    }

    Optional<AgentRuntimeState> reclaimExpiredLease(Instant observedAt) {
        Objects.requireNonNull(
                observedAt, "observedAt must not be null");
        if (agentRun.isEmpty()
                || agentRun.orElseThrow().status()
                        != RuntimeAgentRunStatus.EXECUTING) {
            return Optional.empty();
        }
        RuntimeAgentRun current = agentRun.orElseThrow();
        AgentRunLease lease = current.lease().orElseThrow();
        if (!lease.isExpiredAt(observedAt)) {
            return Optional.empty();
        }
        return Optional.of(next(
                goal,
                Optional.of(current.transition(
                        RuntimeAgentRunStatus.READY)),
                lastIssuedFenceToken));
    }

    AgentRuntimeState recordResult(
            String agentRunId,
            MessageEnvelope resultMessage) {
        RuntimeAgentRun current = requireRun(
                agentRunId,
                RuntimeAgentRunStatus.AWAITING_VERIFICATION);
        ResultPayload payload = validateResultMessage(
                resultMessage,
                current);
        boolean completed =
                payload.verificationStatus() == VerificationStatus.VERIFIED;
        RuntimeAgentRunStatus runStatus = completed
                ? RuntimeAgentRunStatus.COMPLETED
                : RuntimeAgentRunStatus.FAILED;
        RuntimeGoalStatus goalStatus = completed
                ? RuntimeGoalStatus.COMPLETED
                : RuntimeGoalStatus.FAILED;
        return next(
                goal.withStatus(goalStatus),
                Optional.of(current.terminate(runStatus, resultMessage)));
    }

    Optional<AgentRuntimeState> recordControlRequest(
            MessageEnvelope request) {
        if (goal.status() != RuntimeGoalStatus.ACTIVE
                || agentRun.isEmpty()) {
            throw new IllegalStateException(
                    "control requests require an active Goal and AgentRun");
        }
        validateControlRequest(request, agentRun.orElseThrow());
        for (MessageEnvelope existing : controlRequests) {
            if (existing.messageId().equals(request.messageId())) {
                if (existing.equals(request)) {
                    return Optional.empty();
                }
                throw new IllegalArgumentException(
                        "control request identity already has different content");
            }
        }
        if (controlRequests.size() >= MAX_CONTROL_REQUESTS) {
            throw new IllegalStateException(
                    "control request ledger is at capacity");
        }
        List<MessageEnvelope> nextRequests =
                new ArrayList<>(controlRequests);
        nextRequests.add(request);
        return Optional.of(new AgentRuntimeState(
                schemaVersion,
                revision + 1,
                lastIssuedFenceToken,
                goal,
                agentRun,
                nextRequests));
    }

    static String requireCanonicalGoalId(String goalId) {
        return RuntimeIdentity.canonicalUuid(goalId, "goalId");
    }

    private AgentRuntimeState transition(
            String agentRunId,
            RuntimeAgentRunStatus expected,
            RuntimeAgentRunStatus nextStatus) {
        RuntimeAgentRun current = requireRun(agentRunId, expected);
        return next(
                goal,
                Optional.of(current.transition(nextStatus)));
    }

    private RuntimeAgentRun requireRun(
            String agentRunId,
            RuntimeAgentRunStatus expected) {
        String canonical = RuntimeIdentity.canonicalUuid(
                agentRunId, "agentRunId");
        RuntimeAgentRun current = agentRun.orElseThrow(() ->
                new IllegalStateException("no AgentRun exists"));
        if (!current.agentRunId().equals(canonical)) {
            throw new IllegalArgumentException(
                    "agentRunId does not match the active AgentRun");
        }
        if (current.status() != expected) {
            throw new IllegalStateException(
                    "AgentRun transition requires " + expected);
        }
        return current;
    }

    private ResultPayload validateResultMessage(
            MessageEnvelope resultMessage,
            RuntimeAgentRun run) {
        Objects.requireNonNull(
                resultMessage, "resultMessage must not be null");
        Objects.requireNonNull(run, "run must not be null");
        if (!(resultMessage.payload() instanceof ResultPayload payload)) {
            throw new IllegalArgumentException(
                    "resultMessage must carry ResultPayload");
        }
        WorkItem workItem = goal.workItem();
        MessageEnvelope workMessage = workItem.workMessage();
        if (resultMessage.messageId().equals(goal.goalId())
                || resultMessage.messageId().equals(workItem.workItemId())
                || resultMessage.messageId().equals(run.agentRunId())) {
            throw new IllegalArgumentException(
                    "result message identity must be distinct from runtime identities");
        }
        if (!workItem.logicalRunId().equals(resultMessage.logicalRunId())) {
            throw new IllegalArgumentException(
                    "result logical run does not match Goal work");
        }
        if (!workMessage.correlationId().equals(
                resultMessage.correlationId())) {
            throw new IllegalArgumentException(
                    "result correlation does not match Goal work");
        }
        if (!resultMessage.causationId().equals(
                Optional.of(workMessage.messageId()))) {
            throw new IllegalArgumentException(
                    "result causation must name the Goal work message");
        }
        if (!workItem.taskRevision().taskId().equals(payload.taskId())) {
            throw new IllegalArgumentException(
                    "result task does not match Goal work");
        }
        return payload;
    }

    private AgentRuntimeState next(
            RuntimeGoal nextGoal,
            Optional<RuntimeAgentRun> nextRun) {
        return new AgentRuntimeState(
                schemaVersion,
                revision + 1,
                lastIssuedFenceToken,
                nextGoal,
                nextRun,
                controlRequests);
    }

    private AgentRuntimeState next(
            RuntimeGoal nextGoal,
            Optional<RuntimeAgentRun> nextRun,
            long nextFenceToken) {
        return new AgentRuntimeState(
                schemaVersion,
                revision + 1,
                nextFenceToken,
                nextGoal,
                nextRun,
                controlRequests);
    }

    private void validateControlRequest(
            MessageEnvelope request,
            RuntimeAgentRun run) {
        Objects.requireNonNull(request, "request must not be null");
        if (!(request.payload() instanceof ControlPayload)) {
            throw new IllegalArgumentException(
                    "control request must carry ControlPayload");
        }
        WorkItem workItem = goal.workItem();
        MessageEnvelope workMessage = workItem.workMessage();
        if (request.messageId().equals(goal.goalId())
                || request.messageId().equals(workItem.workItemId())
                || request.messageId().equals(workMessage.messageId())
                || request.messageId().equals(run.agentRunId())) {
            throw new IllegalArgumentException(
                    "control request identity must be distinct from runtime identities");
        }
        if (!workItem.logicalRunId().equals(request.logicalRunId())) {
            throw new IllegalArgumentException(
                    "control logical run does not match Goal work");
        }
        if (!workMessage.correlationId().equals(request.correlationId())) {
            throw new IllegalArgumentException(
                    "control correlation does not match Goal work");
        }
        if (!request.causationId().equals(
                Optional.of(workMessage.messageId()))) {
            throw new IllegalArgumentException(
                    "control causation must name the Goal work message");
        }
    }

    private void validateStructure() {
        if (controlRequests.size() > MAX_CONTROL_REQUESTS) {
            throw new IllegalArgumentException(
                    "control request ledger exceeds capacity");
        }
        Set<String> controlIds = new HashSet<>();
        for (MessageEnvelope request : controlRequests) {
            RuntimeAgentRun run = agentRun.orElseThrow(() ->
                    new IllegalArgumentException(
                            "control requests require an AgentRun"));
            validateControlRequest(request, run);
            if (!controlIds.add(request.messageId())) {
                throw new IllegalArgumentException(
                        "control request identities must be unique");
            }
        }
        if (agentRun.isEmpty()) {
            if (goal.status() != RuntimeGoalStatus.ACCEPTED
                    || revision != 0
                    || lastIssuedFenceToken != 0) {
                throw new IllegalArgumentException(
                        "Goal without AgentRun must be initial Accepted state");
            }
            return;
        }
        RuntimeAgentRun run = agentRun.orElseThrow();
        if (!run.goalId().equals(goal.goalId())
                || !run.workItemId().equals(
                        goal.workItem().workItemId())) {
            throw new IllegalArgumentException(
                    "AgentRun does not belong to the Goal WorkItem");
        }
        switch (run.status()) {
            case COMPLETED -> {
                if (goal.status() != RuntimeGoalStatus.COMPLETED) {
                    throw new IllegalArgumentException(
                            "completed AgentRun requires completed Goal");
                }
                validateResultMessage(
                        run.resultMessage().orElseThrow(),
                        run);
            }
            case FAILED -> {
                if (goal.status() != RuntimeGoalStatus.FAILED) {
                    throw new IllegalArgumentException(
                            "failed AgentRun requires failed Goal");
                }
                validateResultMessage(
                        run.resultMessage().orElseThrow(),
                        run);
            }
            default -> {
                if (goal.status() != RuntimeGoalStatus.ACTIVE) {
                    throw new IllegalArgumentException(
                            "non-terminal AgentRun requires active Goal");
                }
            }
        }
        if (run.status() == RuntimeAgentRunStatus.PLANNING
                && lastIssuedFenceToken != 0) {
            throw new IllegalArgumentException(
                    "planning AgentRun cannot follow an issued fence");
        }
        if (run.status() == RuntimeAgentRunStatus.EXECUTING) {
            long leaseFence = run.lease().orElseThrow().fenceToken();
            if (leaseFence != lastIssuedFenceToken) {
                throw new IllegalArgumentException(
                        "executing lease must carry the latest fence token");
            }
        }
        if ((run.status() == RuntimeAgentRunStatus.AWAITING_VERIFICATION
                || run.status().isTerminal())
                && lastIssuedFenceToken == 0) {
            throw new IllegalArgumentException(
                    "post-execution state requires an issued fence");
        }
        if (revision < 1) {
            throw new IllegalArgumentException(
                    "AgentRun state requires a positive revision");
        }
    }
}
