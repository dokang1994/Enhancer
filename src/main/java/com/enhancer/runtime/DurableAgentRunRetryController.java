package com.enhancer.runtime;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;

/**
 * Persist-first application boundary for deciding and applying one parked AgentRun retry.
 * This controller changes only durable runtime state; it never schedules or executes work.
 */
public final class DurableAgentRunRetryController {
    private static final String LEDGER_DIGEST_FORMAT =
            "enhancer.external-effect-ledger.semantic.v1";

    private final AgentRuntimeStateStore runtimeStore;
    private final ExternalEffectLedgerStore effectLedgerStore;
    private final AgentRunRetryDecider decider;

    public DurableAgentRunRetryController(
            AgentRuntimeStateStore runtimeStore,
            ExternalEffectLedgerStore effectLedgerStore,
            AgentRunRetryDecider decider) {
        this.runtimeStore = Objects.requireNonNull(
                runtimeStore, "runtimeStore must not be null");
        this.effectLedgerStore = Objects.requireNonNull(
                effectLedgerStore, "effectLedgerStore must not be null");
        this.decider = Objects.requireNonNull(
                decider, "decider must not be null");
    }

    /** Resolves exact durable inputs and records their decision before any retry action. */
    public AgentRunRetryDecisionRecord recordDecision(
            String goalId,
            AgentRunRetryPolicy policy) throws IOException {
        Objects.requireNonNull(policy, "policy must not be null");
        DurableAgentRuntime runtime = recover(goalId);
        requireRetryPending(runtime);

        RuntimeAgentRun failedAttempt = runtime.agentRun().orElseThrow(() ->
                new IllegalStateException("retry-pending Goal has no AgentRun"));
        if (failedAttempt.status() != RuntimeAgentRunStatus.FAILED) {
            throw new IllegalStateException(
                    "retry-pending Goal latest AgentRun is not failed");
        }

        ExternalEffectLedgerState ledger = effectLedgerStore.resolve(
                runtime.goal().goalId());
        AgentRunRetryDecision decision = decider.decide(
                failedAttempt,
                runtime.completedAttempts(),
                policy,
                ledger);
        AgentRunRetryDecisionRecord record = new AgentRunRetryDecisionRecord(
                failedAttempt.agentRunId(),
                runtime.completedAttempts(),
                policy.maxAttempts(),
                ledger.revision(),
                ledger.records().size(),
                semanticDigest(ledger.records()),
                decision);
        runtime.recordRetryDecision(record);
        return record;
    }

    /** Appends exactly the caller-checkpointed replacement identity for an admitted decision. */
    public void beginAdmittedRetry(
            String goalId,
            String checkpointedReplacementAgentRunId) throws IOException {
        String replacementId = RuntimeIdentity.canonicalUuid(
                checkpointedReplacementAgentRunId,
                "checkpointedReplacementAgentRunId");
        DurableAgentRuntime runtime = recover(goalId);
        if (runtime.goal().status() == RuntimeGoalStatus.ACTIVE) {
            requireIdempotentAdmittedReplay(runtime, replacementId);
            return;
        }
        requireRetryPending(runtime);
        AgentRunRetryDecisionRecord decision = latestDecision(runtime);
        if (!decision.decision().isAdmitted()) {
            throw new IllegalStateException(
                    "refused retry decision cannot append an AgentRun");
        }
        runtime.beginRetryAgentRun(replacementId);
    }

    /** Applies a refused decision as terminal Goal abandonment. */
    public void abandonRefusedRetry(String goalId) throws IOException {
        DurableAgentRuntime runtime = recover(goalId);
        if (runtime.goal().status() == RuntimeGoalStatus.FAILED) {
            requireRefusedDecision(runtime);
            return;
        }
        requireRetryPending(runtime);
        requireRefusedDecision(runtime);
        runtime.abandonGoal();
    }

    private DurableAgentRuntime recover(String goalId) throws IOException {
        return DurableAgentRuntime.recoverForControlAdmission(
                goalId, runtimeStore, Clock.systemUTC());
    }

    private static void requireRetryPending(DurableAgentRuntime runtime) {
        if (runtime.goal().status() != RuntimeGoalStatus.RETRY_PENDING) {
            throw new IllegalStateException(
                    "Goal must be retry pending: " + runtime.goal().status());
        }
    }

    private static AgentRunRetryDecisionRecord latestDecision(
            DurableAgentRuntime runtime) {
        List<AgentRunRetryDecisionRecord> decisions = runtime.retryDecisions();
        if (decisions.isEmpty()) {
            throw new IllegalStateException("Goal has no recorded retry decision");
        }
        return decisions.get(decisions.size() - 1);
    }

    private static void requireRefusedDecision(DurableAgentRuntime runtime) {
        if (latestDecision(runtime).decision().isAdmitted()) {
            throw new IllegalStateException(
                    "admitted retry decision cannot abandon the Goal");
        }
    }

    private static void requireIdempotentAdmittedReplay(
            DurableAgentRuntime runtime,
            String replacementId) {
        AgentRunRetryDecisionRecord decision = latestDecision(runtime);
        if (!decision.decision().isAdmitted()) {
            throw new IllegalStateException(
                    "refused retry decision cannot append an AgentRun");
        }
        RuntimeAgentRun latest = runtime.agentRun().orElseThrow(() ->
                new IllegalStateException("active Goal has no AgentRun"));
        if (!latest.agentRunId().equals(replacementId)) {
            throw new IllegalArgumentException(
                    "checkpointed replacement AgentRun does not match durable state");
        }
        List<RuntimeAgentRun> attempts = runtime.agentRuns();
        if (attempts.size() < 2
                || !attempts.get(attempts.size() - 2).agentRunId()
                        .equals(decision.agentRunId())) {
            throw new IllegalStateException(
                    "active Goal is not the recorded retry transition");
        }
    }

    private static String semanticDigest(List<ExternalEffectRecord> records) {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
        updateFrame(digest, LEDGER_DIGEST_FORMAT);
        updateFrame(digest, Integer.toString(records.size()));
        for (ExternalEffectRecord record : records) {
            ExternalEffectRequest request = record.request();
            updateFrame(digest, request.idempotencyKey());
            updateFrame(digest, request.goalId());
            updateFrame(digest, request.agentRunId());
            updateFrame(digest, request.workItemId());
            updateFrame(digest, request.operationName());
            updateFrame(digest, request.operationSha256());
            updateFrame(digest, record.status().name());
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    private static void updateFrame(MessageDigest digest, String value) {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        digest.update((byte) (bytes.length >>> 24));
        digest.update((byte) (bytes.length >>> 16));
        digest.update((byte) (bytes.length >>> 8));
        digest.update((byte) bytes.length);
        digest.update(bytes);
    }
}
