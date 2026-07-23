package com.enhancer.runtime;

import com.enhancer.tool.EvidenceStore;
import com.enhancer.tool.ResolvedEvidence;
import java.io.IOException;
import java.util.Objects;
import java.util.Optional;

/**
 * Reads one bounded stable Scheduler/effect observation and integrity-checks every
 * terminal effect's bound evidence.
 */
public final class SchedulerExternalEffectRecoveryStatusReader {
    private final SchedulerRecoveryStatusReader schedulerReader;
    private final AgentRuntimeStateStore runtimeStore;
    private final ExternalEffectLedgerStore effectStore;
    private final EvidenceStore evidenceStore;

    public SchedulerExternalEffectRecoveryStatusReader(
            SchedulerRecoveryStatusReader schedulerReader,
            AgentRuntimeStateStore runtimeStore,
            ExternalEffectLedgerStore effectStore,
            EvidenceStore evidenceStore) {
        this.schedulerReader = Objects.requireNonNull(
                schedulerReader, "schedulerReader must not be null");
        this.runtimeStore = Objects.requireNonNull(
                runtimeStore, "runtimeStore must not be null");
        this.effectStore = Objects.requireNonNull(
                effectStore, "effectStore must not be null");
        this.evidenceStore = Objects.requireNonNull(
                evidenceStore, "evidenceStore must not be null");
    }

    public SchedulerExternalEffectRecoveryStatus read(String queueId)
            throws IOException {
        SchedulerRecoveryStatus firstScheduler =
                schedulerReader.read(queueId);
        Optional<AgentRuntimeState> firstRuntime =
                resolveRuntime(firstScheduler);
        Optional<ExternalEffectLedgerState> firstLedger =
                resolveLedger(firstScheduler);
        int verifiedEvidence = verifyEvidence(firstLedger);

        SchedulerRecoveryStatus secondScheduler =
                schedulerReader.read(queueId);
        Optional<AgentRuntimeState> secondRuntime =
                resolveRuntime(secondScheduler);
        Optional<ExternalEffectLedgerState> secondLedger =
                resolveLedger(secondScheduler);

        requireStableScheduler(firstScheduler, secondScheduler);
        requireStableRuntime(firstRuntime, secondRuntime);
        requireStableLedger(firstLedger, secondLedger);
        return SchedulerExternalEffectRecoveryStatus.project(
                firstScheduler,
                firstRuntime,
                firstLedger,
                verifiedEvidence);
    }

    private Optional<AgentRuntimeState> resolveRuntime(
            SchedulerRecoveryStatus scheduler) throws IOException {
        if (scheduler.runtimeRevision().isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(runtimeStore.resolve(
                scheduler.goalId().orElseThrow()));
    }

    private Optional<ExternalEffectLedgerState> resolveLedger(
            SchedulerRecoveryStatus scheduler) throws IOException {
        if (scheduler.goalId().isEmpty()) {
            return Optional.empty();
        }
        try {
            return Optional.of(effectStore.resolve(
                    scheduler.goalId().orElseThrow()));
        } catch (MissingExternalEffectLedgerException exception) {
            return Optional.empty();
        }
    }

    private int verifyEvidence(
            Optional<ExternalEffectLedgerState> ledger)
            throws IOException {
        if (ledger.isEmpty()) {
            return 0;
        }
        int verified = 0;
        for (ExternalEffectRecord record :
                ledger.orElseThrow().records()) {
            if (!record.status().isTerminal()) {
                continue;
            }
            ExternalEffectOutcomeEvidence binding =
                    record.outcomeEvidence().orElseThrow();
            ResolvedEvidence resolved =
                    evidenceStore.resolve(binding.reference());
            if (!binding.reference().equals(
                            resolved.metadata().reference())
                    || !binding.sha256().equals(
                            resolved.metadata().sha256())) {
                throw new IOException(
                        "terminal external-effect evidence binding "
                                + "does not match stored evidence");
            }
            verified++;
        }
        return verified;
    }

    private void requireStableScheduler(
            SchedulerRecoveryStatus first,
            SchedulerRecoveryStatus second)
            throws ConcurrentSchedulerExternalEffectInspectionException {
        if (!first.queueId().equals(second.queueId())
                || first.queueRevision() != second.queueRevision()
                || first.phase() != second.phase()
                || !first.goalId().equals(second.goalId())
                || !first.agentRunId().equals(second.agentRunId())
                || !first.replacementAgentRunId().equals(
                        second.replacementAgentRunId())
                || !first.runRecordReference().equals(
                        second.runRecordReference())
                || !first.runtimeRevision().equals(
                        second.runtimeRevision())
                || !first.goalStatus().equals(second.goalStatus())
                || !first.agentRunStatus().equals(
                        second.agentRunStatus())
                || !first.queueWorkState().equals(
                        second.queueWorkState())
                || !first.runRecordVerificationStatus().equals(
                        second.runRecordVerificationStatus())
                || first.agentRunAttempts()
                        != second.agentRunAttempts()) {
            throw changed(
                    "Scheduler recovery observation differs between samples");
        }
    }

    private void requireStableRuntime(
            Optional<AgentRuntimeState> first,
            Optional<AgentRuntimeState> second)
            throws ConcurrentSchedulerExternalEffectInspectionException {
        if (first.isEmpty() != second.isEmpty()) {
            throw changed(
                    "AgentRuntime presence differs between samples");
        }
        if (first.isEmpty()) {
            return;
        }
        AgentRuntimeState firstState = first.orElseThrow();
        AgentRuntimeState secondState = second.orElseThrow();
        if (!firstState.goal().goalId().equals(
                        secondState.goal().goalId())
                || firstState.revision() != secondState.revision()) {
            throw changed(
                    "AgentRuntime revision differs between samples");
        }
    }

    private void requireStableLedger(
            Optional<ExternalEffectLedgerState> first,
            Optional<ExternalEffectLedgerState> second)
            throws ConcurrentSchedulerExternalEffectInspectionException {
        if (first.isEmpty() != second.isEmpty()) {
            throw changed(
                    "external-effect ledger presence differs between samples");
        }
        if (first.isEmpty()) {
            return;
        }
        ExternalEffectLedgerState firstState = first.orElseThrow();
        ExternalEffectLedgerState secondState = second.orElseThrow();
        if (!firstState.goalId().equals(secondState.goalId())
                || firstState.revision() != secondState.revision()) {
            throw changed(
                    "external-effect ledger revision differs between samples");
        }
    }

    private ConcurrentSchedulerExternalEffectInspectionException changed(
            String reason) {
        return new ConcurrentSchedulerExternalEffectInspectionException(
                reason);
    }
}
