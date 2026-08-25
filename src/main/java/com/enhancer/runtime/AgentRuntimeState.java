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

/** Immutable schema-v5 Goal, AgentRun, control, cancellation, and lease-timeout state. */
public final class AgentRuntimeState {
    public static final int PREVIOUS_SCHEMA_VERSION = 4;
    public static final int CURRENT_SCHEMA_VERSION = 5;
    public static final int MAX_ATTEMPTS_PER_GOAL = AgentRunRetryPolicy.MAX_ATTEMPTS;
    public static final int MAX_CONTROL_REQUESTS = 256;
    public static final int MAX_LEASE_TIMEOUTS = 256;

    private final int schemaVersion;
    private final long revision;
    private final long lastIssuedFenceToken;
    private final RuntimeGoal goal;
    private final List<RuntimeAgentRun> agentRuns;
    private final List<AgentRunRetryDecisionRecord> retryDecisions;
    private final List<MessageEnvelope> controlRequests;
    private final List<LeaseTimeoutRecord> leaseTimeouts;
    private final Optional<CancellationApplicationRecord> cancellationApplication;

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
                agentRun.stream().toList(),
                List.of(),
                List.of());
    }

    AgentRuntimeState(
            int schemaVersion,
            long revision,
            long lastIssuedFenceToken,
            RuntimeGoal goal,
            Optional<RuntimeAgentRun> agentRun,
            List<MessageEnvelope> controlRequests) {
        this(
                schemaVersion,
                revision,
                lastIssuedFenceToken,
                goal,
                agentRun.stream().toList(),
                List.of(),
                controlRequests);
    }

    AgentRuntimeState(
            int schemaVersion,
            long revision,
            long lastIssuedFenceToken,
            RuntimeGoal goal,
            List<RuntimeAgentRun> agentRuns,
            List<AgentRunRetryDecisionRecord> retryDecisions,
            List<MessageEnvelope> controlRequests) {
        this(
                schemaVersion,
                revision,
                lastIssuedFenceToken,
                goal,
                agentRuns,
                retryDecisions,
                controlRequests,
                List.of());
    }

    AgentRuntimeState(
            int schemaVersion,
            long revision,
            long lastIssuedFenceToken,
            RuntimeGoal goal,
            List<RuntimeAgentRun> agentRuns,
            List<AgentRunRetryDecisionRecord> retryDecisions,
            List<MessageEnvelope> controlRequests,
            List<LeaseTimeoutRecord> leaseTimeouts) {
        this(
                schemaVersion,
                revision,
                lastIssuedFenceToken,
                goal,
                agentRuns,
                retryDecisions,
                controlRequests,
                leaseTimeouts,
                Optional.empty());
    }

    AgentRuntimeState(
            int schemaVersion,
            long revision,
            long lastIssuedFenceToken,
            RuntimeGoal goal,
            List<RuntimeAgentRun> agentRuns,
            List<AgentRunRetryDecisionRecord> retryDecisions,
            List<MessageEnvelope> controlRequests,
            List<LeaseTimeoutRecord> leaseTimeouts,
            Optional<CancellationApplicationRecord> cancellationApplication) {
        if (schemaVersion != CURRENT_SCHEMA_VERSION) {
            throw new IllegalArgumentException(
                    "Agent runtime schema version is unsupported");
        }
        if (revision < 0) {
            throw new IllegalArgumentException("revision must not be negative");
        }
        if (lastIssuedFenceToken < 0) {
            throw new IllegalArgumentException(
                    "lastIssuedFenceToken must not be negative");
        }
        this.schemaVersion = schemaVersion;
        this.revision = revision;
        this.lastIssuedFenceToken = lastIssuedFenceToken;
        this.goal = Objects.requireNonNull(goal, "goal must not be null");
        this.agentRuns = List.copyOf(Objects.requireNonNull(
                agentRuns, "agentRuns must not be null"));
        this.retryDecisions = List.copyOf(Objects.requireNonNull(
                retryDecisions, "retryDecisions must not be null"));
        this.controlRequests = List.copyOf(Objects.requireNonNull(
                controlRequests, "controlRequests must not be null"));
        this.leaseTimeouts = List.copyOf(Objects.requireNonNull(
                leaseTimeouts, "leaseTimeouts must not be null"));
        this.cancellationApplication = Objects.requireNonNull(
                cancellationApplication,
                "cancellationApplication must not be null");
        validateStructure();
    }

    public static AgentRuntimeState initial(String goalId, WorkItem workItem) {
        return new AgentRuntimeState(
                CURRENT_SCHEMA_VERSION,
                0,
                0,
                new RuntimeGoal(goalId, workItem, RuntimeGoalStatus.ACCEPTED),
                List.of(),
                List.of(),
                List.of());
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
        return agentRuns.isEmpty()
                ? Optional.empty()
                : Optional.of(agentRuns.get(agentRuns.size() - 1));
    }

    public List<RuntimeAgentRun> agentRuns() {
        return agentRuns;
    }

    public List<AgentRunRetryDecisionRecord> retryDecisions() {
        return retryDecisions;
    }

    public int completedAttempts() {
        return (int) agentRuns.stream()
                .filter(run -> run.status().hasResult())
                .count();
    }

    public List<MessageEnvelope> controlRequests() {
        return controlRequests;
    }

    public List<LeaseTimeoutRecord> leaseTimeouts() {
        return leaseTimeouts;
    }

    public Optional<CancellationApplicationRecord> cancellationApplication() {
        return cancellationApplication;
    }

    AgentRuntimeState beginAgentRun(String agentRunId) {
        if (!agentRuns.isEmpty() || goal.status() != RuntimeGoalStatus.ACCEPTED) {
            throw new IllegalStateException("first AgentRun requires an Accepted Goal");
        }
        RuntimeAgentRun run = newPlanningRun(agentRunId);
        return next(
                goal.withStatus(RuntimeGoalStatus.ACTIVE),
                List.of(run),
                retryDecisions,
                lastIssuedFenceToken);
    }

    AgentRuntimeState beginRetryAgentRun(String agentRunId) {
        RuntimeAgentRun latest = requireLatestFailedRetryPending();
        AgentRunRetryDecisionRecord decision = requireLatestDecision(latest.agentRunId());
        if (!decision.decision().isAdmitted()) {
            throw new IllegalStateException("refused retry decision cannot append an AgentRun");
        }
        if (agentRuns.size() >= MAX_ATTEMPTS_PER_GOAL
                || agentRuns.size() >= decision.maxAttempts()) {
            throw new IllegalStateException("AgentRun attempt budget is exhausted");
        }
        RuntimeAgentRun replacement = newPlanningRun(agentRunId);
        List<RuntimeAgentRun> nextRuns = new ArrayList<>(agentRuns);
        nextRuns.add(replacement);
        return next(
                goal.withStatus(RuntimeGoalStatus.ACTIVE),
                nextRuns,
                retryDecisions,
                lastIssuedFenceToken);
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
        RuntimeAgentRun current = requireRun(agentRunId, RuntimeAgentRunStatus.READY);
        if (lastIssuedFenceToken == Long.MAX_VALUE) {
            throw new IllegalStateException("AgentRun fence token space is exhausted");
        }
        long nextFenceToken = lastIssuedFenceToken + 1;
        AgentRunLease lease = AgentRunLease.issue(
                ownerId, nextFenceToken, issuedAt, duration);
        return replaceLatest(current.executeWith(lease), goal, nextFenceToken);
    }

    AgentRuntimeState renewLease(
            String agentRunId,
            String ownerId,
            long fenceToken,
            Instant renewedAt,
            Duration duration) {
        RuntimeAgentRun current = requireRun(
                agentRunId, RuntimeAgentRunStatus.EXECUTING);
        AgentRunLease lease = current.lease().orElseThrow();
        lease.requireCurrent(ownerId, fenceToken, renewedAt);
        return replaceLatest(
                current.renew(lease.renew(renewedAt, duration)),
                goal,
                lastIssuedFenceToken);
    }

    AgentRuntimeState completeExecution(
            String agentRunId,
            String ownerId,
            long fenceToken,
            Instant completedAt) {
        RuntimeAgentRun current = requireRun(
                agentRunId, RuntimeAgentRunStatus.EXECUTING);
        current.lease().orElseThrow().requireCurrent(
                ownerId, fenceToken, completedAt);
        return replaceLatest(
                current.transition(RuntimeAgentRunStatus.AWAITING_VERIFICATION),
                goal,
                lastIssuedFenceToken);
    }

    Optional<AgentRuntimeState> reclaimExpiredLease(Instant observedAt) {
        Objects.requireNonNull(observedAt, "observedAt must not be null");
        Optional<RuntimeAgentRun> latest = agentRun();
        if (latest.isEmpty()
                || latest.orElseThrow().status() != RuntimeAgentRunStatus.EXECUTING) {
            return Optional.empty();
        }
        RuntimeAgentRun current = latest.orElseThrow();
        AgentRunLease lease = current.lease().orElseThrow();
        if (!lease.isExpiredAt(observedAt)) {
            return Optional.empty();
        }
        if (leaseTimeouts.size() >= MAX_LEASE_TIMEOUTS) {
            throw new IllegalStateException("lease timeout history is at capacity");
        }
        LeaseTimeoutRecord timeout = new LeaseTimeoutRecord(
                current.agentRunId(),
                lease.ownerId(),
                lease.fenceToken(),
                lease.issuedAt(),
                lease.expiresAt(),
                observedAt);
        List<LeaseTimeoutRecord> nextTimeouts = new ArrayList<>(leaseTimeouts);
        nextTimeouts.add(timeout);
        List<RuntimeAgentRun> nextRuns = new ArrayList<>(agentRuns);
        nextRuns.set(
                nextRuns.size() - 1,
                current.transition(RuntimeAgentRunStatus.READY));
        return Optional.of(new AgentRuntimeState(
                schemaVersion,
                revision + 1,
                lastIssuedFenceToken,
                goal,
                nextRuns,
                retryDecisions,
                controlRequests,
                nextTimeouts,
                cancellationApplication));
    }

    AgentRuntimeState recordAttemptResult(
            String agentRunId,
            MessageEnvelope resultMessage) {
        RuntimeAgentRun current = requireRun(
                agentRunId, RuntimeAgentRunStatus.AWAITING_VERIFICATION);
        ResultPayload payload = validateResultMessage(resultMessage, current);
        boolean completed =
                payload.verificationStatus() == VerificationStatus.VERIFIED;
        RuntimeAgentRun terminal = current.terminate(
                completed ? RuntimeAgentRunStatus.COMPLETED
                        : RuntimeAgentRunStatus.FAILED,
                resultMessage);
        RuntimeGoal nextGoal = goal.withStatus(
                completed ? RuntimeGoalStatus.COMPLETED
                        : RuntimeGoalStatus.RETRY_PENDING);
        return replaceLatest(terminal, nextGoal, lastIssuedFenceToken);
    }

    AgentRuntimeState recordResult(
            String agentRunId,
            MessageEnvelope resultMessage) {
        return recordAttemptResult(agentRunId, resultMessage);
    }

    Optional<AgentRuntimeState> recordRetryDecision(
            AgentRunRetryDecisionRecord decision) {
        Objects.requireNonNull(decision, "decision must not be null");
        for (AgentRunRetryDecisionRecord existing : retryDecisions) {
            if (existing.agentRunId().equals(decision.agentRunId())) {
                if (existing.equals(decision)) {
                    return Optional.empty();
                }
                throw new IllegalArgumentException(
                        "retry decision already exists with different content");
            }
        }
        RuntimeAgentRun latest = requireLatestFailedRetryPending();
        if (!latest.agentRunId().equals(decision.agentRunId())) {
            throw new IllegalArgumentException(
                    "retry decision must name the latest failed AgentRun");
        }
        if (decision.completedAttempts() != completedAttempts()) {
            throw new IllegalArgumentException(
                    "retry decision completed-attempt count does not match history");
        }
        if (retryDecisions.size() >= MAX_ATTEMPTS_PER_GOAL) {
            throw new IllegalStateException("retry decision history is at capacity");
        }
        List<AgentRunRetryDecisionRecord> nextDecisions =
                new ArrayList<>(retryDecisions);
        nextDecisions.add(decision);
        return Optional.of(next(
                goal,
                agentRuns,
                nextDecisions,
                lastIssuedFenceToken));
    }

    AgentRuntimeState abandonGoal() {
        RuntimeAgentRun latest = requireLatestFailedRetryPending();
        AgentRunRetryDecisionRecord decision = requireLatestDecision(latest.agentRunId());
        if (decision.decision().isAdmitted()) {
            throw new IllegalStateException("admitted retry decision cannot abandon Goal");
        }
        return next(
                goal.withStatus(RuntimeGoalStatus.FAILED),
                agentRuns,
                retryDecisions,
                lastIssuedFenceToken);
    }

    Optional<AgentRuntimeState> recordControlRequest(MessageEnvelope request) {
        if (goal.status() != RuntimeGoalStatus.ACTIVE || agentRuns.isEmpty()) {
            throw new IllegalStateException(
                    "control requests require an active Goal and AgentRun");
        }
        validateControlRequest(request);
        for (MessageEnvelope existing : controlRequests) {
            if (existing.messageId().equals(request.messageId())) {
                if (existing.equals(request)) {
                    return Optional.empty();
                }
                throw new IllegalArgumentException(
                        "control request identity already has different content");
            }
        }
        requireUniqueMessageIdentity(request.messageId());
        if (controlRequests.size() >= MAX_CONTROL_REQUESTS) {
            throw new IllegalStateException("control request ledger is at capacity");
        }
        List<MessageEnvelope> nextRequests = new ArrayList<>(controlRequests);
        nextRequests.add(request);
        return Optional.of(new AgentRuntimeState(
                schemaVersion,
                revision + 1,
                lastIssuedFenceToken,
                goal,
                agentRuns,
                retryDecisions,
                nextRequests,
                leaseTimeouts,
                cancellationApplication));
    }

    Optional<AgentRuntimeState> applyCancellation(
            CancellationApplicationRecord record) {
        Objects.requireNonNull(record, "record must not be null");
        if (cancellationApplication.isPresent()) {
            if (cancellationApplication.orElseThrow().equals(record)) {
                return Optional.empty();
            }
            throw new IllegalArgumentException(
                    "cancellation application already exists with different content");
        }
        if (!record.goalId().equals(goal.goalId())) {
            throw new IllegalArgumentException(
                    "cancellation application Goal does not match runtime");
        }
        if (allIdentityValues().contains(record.authorizationId())) {
            throw new IllegalArgumentException(
                    "authorization identity must be distinct from runtime identities");
        }
        MessageEnvelope request = controlRequests.stream()
                .filter(candidate -> candidate.messageId()
                        .equals(record.controlMessageId()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "cancellation application requires a retained Control request"));
        if (!(request.payload() instanceof ControlPayload payload)
                || payload.signal() != com.enhancer.bus.ControlSignal.CANCEL) {
            throw new IllegalArgumentException(
                    "cancellation application requires a retained CANCEL request");
        }
        RuntimeAgentRun latest = agentRun().orElseThrow(() ->
                new IllegalStateException("cancellation requires an AgentRun"));
        if (!latest.agentRunId().equals(record.agentRunId())) {
            throw new IllegalArgumentException(
                    "cancellation application AgentRun does not match current runtime");
        }
        RuntimeAgentRun cancelledRun;
        if (goal.status() == RuntimeGoalStatus.ACTIVE) {
            if (latest.status().isTerminal()) {
                throw new IllegalStateException(
                        "active Goal cancellation requires a non-terminal AgentRun");
            }
            cancelledRun = latest.cancel();
        } else if (goal.status() == RuntimeGoalStatus.RETRY_PENDING
                && latest.status() == RuntimeAgentRunStatus.FAILED) {
            cancelledRun = latest;
        } else {
            throw new IllegalStateException(
                    "cancellation requires an active or retry-pending Goal");
        }
        List<RuntimeAgentRun> nextRuns = new ArrayList<>(agentRuns);
        nextRuns.set(nextRuns.size() - 1, cancelledRun);
        return Optional.of(new AgentRuntimeState(
                schemaVersion,
                revision + 1,
                lastIssuedFenceToken,
                goal.withStatus(RuntimeGoalStatus.CANCELLED),
                nextRuns,
                retryDecisions,
                controlRequests,
                leaseTimeouts,
                Optional.of(record)));
    }

    static String requireCanonicalGoalId(String goalId) {
        return RuntimeIdentity.canonicalUuid(goalId, "goalId");
    }

    private AgentRuntimeState transition(
            String agentRunId,
            RuntimeAgentRunStatus expected,
            RuntimeAgentRunStatus nextStatus) {
        RuntimeAgentRun current = requireRun(agentRunId, expected);
        return replaceLatest(
                current.transition(nextStatus), goal, lastIssuedFenceToken);
    }

    private RuntimeAgentRun requireRun(
            String agentRunId,
            RuntimeAgentRunStatus expected) {
        String canonical = RuntimeIdentity.canonicalUuid(agentRunId, "agentRunId");
        RuntimeAgentRun current = agentRun().orElseThrow(() ->
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

    private RuntimeAgentRun requireLatestFailedRetryPending() {
        if (goal.status() != RuntimeGoalStatus.RETRY_PENDING) {
            throw new IllegalStateException("Goal must be retry pending");
        }
        RuntimeAgentRun latest = agentRun().orElseThrow();
        if (latest.status() != RuntimeAgentRunStatus.FAILED) {
            throw new IllegalStateException("latest AgentRun must be failed");
        }
        return latest;
    }

    private AgentRunRetryDecisionRecord requireLatestDecision(String agentRunId) {
        if (retryDecisions.isEmpty()) {
            throw new IllegalStateException("latest failed AgentRun has no retry decision");
        }
        AgentRunRetryDecisionRecord latest =
                retryDecisions.get(retryDecisions.size() - 1);
        if (!latest.agentRunId().equals(agentRunId)) {
            throw new IllegalStateException(
                    "latest retry decision does not match latest failed AgentRun");
        }
        return latest;
    }

    private RuntimeAgentRun newPlanningRun(String agentRunId) {
        String canonical = RuntimeIdentity.canonicalUuid(agentRunId, "agentRunId");
        Set<String> identities = allIdentityValues();
        if (!identities.add(canonical)) {
            throw new IllegalArgumentException(
                    "agentRunId must be distinct from retained runtime and message identities");
        }
        return new RuntimeAgentRun(
                canonical,
                goal.goalId(),
                goal.workItem().workItemId(),
                RuntimeAgentRunStatus.PLANNING,
                Optional.empty(),
                Optional.empty());
    }

    private ResultPayload validateResultMessage(
            MessageEnvelope resultMessage,
            RuntimeAgentRun run) {
        Objects.requireNonNull(resultMessage, "resultMessage must not be null");
        if (!(resultMessage.payload() instanceof ResultPayload payload)) {
            throw new IllegalArgumentException("resultMessage must carry ResultPayload");
        }
        WorkItem workItem = goal.workItem();
        MessageEnvelope workMessage = workItem.workMessage();
        if (allIdentityValues().contains(resultMessage.messageId())) {
            throw new IllegalArgumentException(
                    "result message identity must be distinct from retained identities");
        }
        if (!workItem.logicalRunId().equals(resultMessage.logicalRunId())) {
            throw new IllegalArgumentException("result logical run does not match Goal work");
        }
        if (!workMessage.correlationId().equals(resultMessage.correlationId())) {
            throw new IllegalArgumentException("result correlation does not match Goal work");
        }
        if (!resultMessage.causationId().equals(Optional.of(workMessage.messageId()))) {
            throw new IllegalArgumentException(
                    "result causation must name the Goal work message");
        }
        if (!workItem.taskRevision().taskId().equals(payload.taskId())) {
            throw new IllegalArgumentException("result task does not match Goal work");
        }
        return payload;
    }

    private AgentRuntimeState replaceLatest(
            RuntimeAgentRun replacement,
            RuntimeGoal nextGoal,
            long nextFenceToken) {
        List<RuntimeAgentRun> nextRuns = new ArrayList<>(agentRuns);
        nextRuns.set(nextRuns.size() - 1, replacement);
        return next(nextGoal, nextRuns, retryDecisions, nextFenceToken);
    }

    private AgentRuntimeState next(
            RuntimeGoal nextGoal,
            List<RuntimeAgentRun> nextRuns,
            List<AgentRunRetryDecisionRecord> nextDecisions,
            long nextFenceToken) {
        return new AgentRuntimeState(
                schemaVersion,
                revision + 1,
                nextFenceToken,
                nextGoal,
                nextRuns,
                nextDecisions,
                controlRequests,
                leaseTimeouts,
                cancellationApplication);
    }

    private void validateControlRequest(MessageEnvelope request) {
        Objects.requireNonNull(request, "request must not be null");
        if (!(request.payload() instanceof ControlPayload)) {
            throw new IllegalArgumentException(
                    "control request must carry ControlPayload");
        }
        WorkItem workItem = goal.workItem();
        MessageEnvelope workMessage = workItem.workMessage();
        if (!workItem.logicalRunId().equals(request.logicalRunId())) {
            throw new IllegalArgumentException("control logical run does not match Goal work");
        }
        if (!workMessage.correlationId().equals(request.correlationId())) {
            throw new IllegalArgumentException("control correlation does not match Goal work");
        }
        if (!request.causationId().equals(Optional.of(workMessage.messageId()))) {
            throw new IllegalArgumentException(
                    "control causation must name the Goal work message");
        }
    }

    private void requireUniqueMessageIdentity(String messageId) {
        if (allIdentityValues().contains(messageId)) {
            throw new IllegalArgumentException(
                    "message identity must be distinct from retained identities");
        }
    }

    private Set<String> allIdentityValues() {
        Set<String> identities = new HashSet<>();
        identities.add(goal.goalId());
        identities.add(goal.workItem().workItemId());
        identities.add(goal.workItem().workMessage().messageId());
        for (RuntimeAgentRun run : agentRuns) {
            identities.add(run.agentRunId());
            run.resultMessage().ifPresent(message -> identities.add(message.messageId()));
        }
        for (MessageEnvelope request : controlRequests) {
            identities.add(request.messageId());
        }
        return identities;
    }

    private void validateStructure() {
        if (agentRuns.size() > MAX_ATTEMPTS_PER_GOAL) {
            throw new IllegalArgumentException("AgentRun history exceeds capacity");
        }
        if (retryDecisions.size() > MAX_ATTEMPTS_PER_GOAL) {
            throw new IllegalArgumentException("retry decision history exceeds capacity");
        }
        if (controlRequests.size() > MAX_CONTROL_REQUESTS) {
            throw new IllegalArgumentException("control request ledger exceeds capacity");
        }
        if (leaseTimeouts.size() > MAX_LEASE_TIMEOUTS) {
            throw new IllegalArgumentException("lease timeout history exceeds capacity");
        }
        if (agentRuns.isEmpty()) {
            if (goal.status() != RuntimeGoalStatus.ACCEPTED
                    || revision != 0
                    || lastIssuedFenceToken != 0
                    || !retryDecisions.isEmpty()
                    || !controlRequests.isEmpty()
                    || !leaseTimeouts.isEmpty()
                    || cancellationApplication.isPresent()) {
                throw new IllegalArgumentException(
                        "Goal without AgentRun must be initial Accepted state");
            }
            return;
        }
        if (goal.status() == RuntimeGoalStatus.ACCEPTED || revision < 1) {
            throw new IllegalArgumentException("AgentRun history requires non-Accepted Goal");
        }

        Set<String> identities = new HashSet<>();
        identities.add(goal.goalId());
        identities.add(goal.workItem().workItemId());
        identities.add(goal.workItem().workMessage().messageId());
        List<RuntimeAgentRun> failedRuns = new ArrayList<>();
        for (int index = 0; index < agentRuns.size(); index++) {
            RuntimeAgentRun run = Objects.requireNonNull(
                    agentRuns.get(index), "agentRuns must not contain null");
            if (!run.goalId().equals(goal.goalId())
                    || !run.workItemId().equals(goal.workItem().workItemId())) {
                throw new IllegalArgumentException(
                        "AgentRun does not belong to the Goal WorkItem");
            }
            if (!identities.add(run.agentRunId())) {
                throw new IllegalArgumentException("runtime identities must be unique");
            }
            if (index < agentRuns.size() - 1
                    && run.status() != RuntimeAgentRunStatus.FAILED) {
                throw new IllegalArgumentException(
                        "every earlier AgentRun must be terminal Failed");
            }
            if (run.status().hasResult()) {
                MessageEnvelope result = run.resultMessage().orElseThrow();
                validateStoredResultMessage(result, run);
                if (!identities.add(result.messageId())) {
                    throw new IllegalArgumentException(
                            "result message identities must be unique");
                }
            }
            if (run.status() == RuntimeAgentRunStatus.FAILED) {
                failedRuns.add(run);
            }
        }

        RuntimeAgentRun latest = agentRun().orElseThrow();
        validateGoalProjection(latest, failedRuns);
        validateDecisionHistory(failedRuns, latest);

        Set<String> controlIds = new HashSet<>();
        for (MessageEnvelope request : controlRequests) {
            Objects.requireNonNull(request, "controlRequests must not contain null");
            validateControlRequest(request);
            if (!controlIds.add(request.messageId())
                    || !identities.add(request.messageId())) {
                throw new IllegalArgumentException(
                        "control request identities must be globally unique");
            }
        }

        if (cancellationApplication.isPresent()) {
            CancellationApplicationRecord cancellation =
                    cancellationApplication.orElseThrow();
            MessageEnvelope source = controlRequests.stream()
                    .filter(request -> request.messageId()
                            .equals(cancellation.controlMessageId()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(
                            "cancellation application source request is absent"));
            if (!(source.payload() instanceof ControlPayload payload)
                    || payload.signal()
                            != com.enhancer.bus.ControlSignal.CANCEL
                    || !cancellation.goalId().equals(goal.goalId())
                    || !cancellation.agentRunId().equals(latest.agentRunId())
                    || !identities.add(cancellation.authorizationId())
                    || goal.status() != RuntimeGoalStatus.CANCELLED) {
                throw new IllegalArgumentException(
                        "cancellation application does not bind to terminal runtime state");
            }
        } else if (goal.status() == RuntimeGoalStatus.CANCELLED) {
            throw new IllegalArgumentException(
                    "cancelled Goal requires a cancellation application record");
        }

        Set<Long> timeoutFences = new HashSet<>();
        Set<String> runIds = new HashSet<>();
        for (RuntimeAgentRun run : agentRuns) {
            runIds.add(run.agentRunId());
        }
        for (LeaseTimeoutRecord timeout : leaseTimeouts) {
            Objects.requireNonNull(timeout, "leaseTimeouts must not contain null");
            if (!runIds.contains(timeout.agentRunId())
                    || !timeoutFences.add(timeout.fenceToken())
                    || timeout.fenceToken() > lastIssuedFenceToken) {
                throw new IllegalArgumentException(
                        "lease timeout history has an invalid run or fence binding");
            }
        }

        if (latest.status() == RuntimeAgentRunStatus.EXECUTING) {
            if (latest.lease().orElseThrow().fenceToken() != lastIssuedFenceToken) {
                throw new IllegalArgumentException(
                        "executing lease must carry the latest fence token");
            }
        }
        if ((completedAttempts() > 0
                || latest.status() == RuntimeAgentRunStatus.AWAITING_VERIFICATION
                || latest.status() == RuntimeAgentRunStatus.EXECUTING)
                && lastIssuedFenceToken == 0) {
            throw new IllegalArgumentException(
                    "post-execution history requires an issued fence");
        }
        if (agentRuns.size() == 1
                && latest.status() == RuntimeAgentRunStatus.PLANNING
                && lastIssuedFenceToken != 0) {
            throw new IllegalArgumentException(
                    "first pre-execution AgentRun cannot follow an issued fence");
        }
    }

    private void validateGoalProjection(
            RuntimeAgentRun latest,
            List<RuntimeAgentRun> failedRuns) {
        switch (goal.status()) {
            case ACTIVE -> {
                if (latest.status().isTerminal()) {
                    throw new IllegalArgumentException(
                            "active Goal requires a non-terminal latest AgentRun");
                }
            }
            case RETRY_PENDING -> {
                if (latest.status() != RuntimeAgentRunStatus.FAILED) {
                    throw new IllegalArgumentException(
                            "retry-pending Goal requires a failed latest AgentRun");
                }
            }
            case COMPLETED -> {
                if (latest.status() != RuntimeAgentRunStatus.COMPLETED) {
                    throw new IllegalArgumentException(
                            "completed Goal requires a completed latest AgentRun");
                }
            }
            case FAILED -> {
                if (latest.status() != RuntimeAgentRunStatus.FAILED
                        || failedRuns.isEmpty()) {
                    throw new IllegalArgumentException(
                            "failed Goal requires a failed latest AgentRun");
                }
            }
            case CANCELLED -> {
                if (latest.status() != RuntimeAgentRunStatus.CANCELLED
                        && latest.status() != RuntimeAgentRunStatus.FAILED) {
                    throw new IllegalArgumentException(
                            "cancelled Goal requires a cancelled or failed latest AgentRun");
                }
            }
            case ACCEPTED -> throw new IllegalArgumentException(
                    "Accepted Goal cannot retain AgentRun history");
        }
    }

    private void validateDecisionHistory(
            List<RuntimeAgentRun> failedRuns,
            RuntimeAgentRun latest) {
        if (retryDecisions.size() > failedRuns.size()) {
            throw new IllegalArgumentException(
                    "retry decisions cannot exceed failed attempts");
        }
        for (int index = 0; index < retryDecisions.size(); index++) {
            AgentRunRetryDecisionRecord decision = Objects.requireNonNull(
                    retryDecisions.get(index),
                    "retryDecisions must not contain null");
            if (!decision.agentRunId().equals(failedRuns.get(index).agentRunId())) {
                throw new IllegalArgumentException(
                        "retry decisions must follow failed AgentRun order");
            }
            if (decision.completedAttempts() != index + 1) {
                throw new IllegalArgumentException(
                        "retry decision attempt count must match failed history");
            }
            if (index < failedRuns.size() - 1 && !decision.decision().isAdmitted()) {
                throw new IllegalArgumentException(
                        "replacement AgentRun requires an admitted prior decision");
            }
        }
        int priorFailedAttempts = latest.status() == RuntimeAgentRunStatus.FAILED
                ? failedRuns.size() - 1
                : failedRuns.size();
        if (retryDecisions.size() < priorFailedAttempts) {
            throw new IllegalArgumentException(
                    "every earlier failed AgentRun requires a retry decision");
        }
        if (goal.status() == RuntimeGoalStatus.ACTIVE
                || goal.status() == RuntimeGoalStatus.COMPLETED) {
            if (retryDecisions.size() != failedRuns.size()) {
                throw new IllegalArgumentException(
                        "continued history requires every failed decision");
            }
        }
        if (goal.status() == RuntimeGoalStatus.FAILED) {
            if (retryDecisions.size() != failedRuns.size()
                    || retryDecisions.get(retryDecisions.size() - 1)
                            .decision().isAdmitted()) {
                throw new IllegalArgumentException(
                        "failed Goal requires a refused latest retry decision");
            }
        }
        if (goal.status() == RuntimeGoalStatus.CANCELLED) {
            int requiredDecisions = latest.status() == RuntimeAgentRunStatus.FAILED
                    ? failedRuns.size() - 1
                    : failedRuns.size();
            if (retryDecisions.size() != requiredDecisions) {
                throw new IllegalArgumentException(
                        "cancelled Goal retry history does not match prior failures");
            }
        }
    }

    private void validateStoredResultMessage(
            MessageEnvelope result,
            RuntimeAgentRun run) {
        if (!(result.payload() instanceof ResultPayload payload)) {
            throw new IllegalArgumentException("terminal AgentRun must carry ResultPayload");
        }
        WorkItem workItem = goal.workItem();
        MessageEnvelope workMessage = workItem.workMessage();
        if (!workItem.logicalRunId().equals(result.logicalRunId())
                || !workMessage.correlationId().equals(result.correlationId())
                || !result.causationId().equals(Optional.of(workMessage.messageId()))
                || !workItem.taskRevision().taskId().equals(payload.taskId())) {
            throw new IllegalArgumentException(
                    "terminal AgentRun result does not bind to Goal work");
        }
        if (run.status() == RuntimeAgentRunStatus.COMPLETED
                && payload.verificationStatus() != VerificationStatus.VERIFIED) {
            throw new IllegalArgumentException(
                    "completed AgentRun requires Verified result");
        }
        if (run.status() == RuntimeAgentRunStatus.FAILED
                && payload.verificationStatus() == VerificationStatus.VERIFIED) {
            throw new IllegalArgumentException(
                    "failed AgentRun cannot carry Verified result");
        }
    }
}
