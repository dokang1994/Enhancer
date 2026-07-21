package com.enhancer.runtime;

import com.enhancer.run.RunRecordStore;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Drives one durable scheduling cycle end to end: claim + lease, execute the approved work to a
 * durable RunRecord, acknowledge execution, then finalize the runtime and queue disposition. A
 * worker-owned durable cycle-intent checkpoint written before the claim makes the cycle a
 * recoverable, idempotent prefix. The dispatcher and finalizer must wrap the same queue
 * instance; the four durable stores stay separate boundaries and no cross-store transaction is
 * claimed.
 */
public final class DurableAgentRunWorker {
    private final DurableAgentRunDispatcher dispatcher;
    private final AgentRunExecution execution;
    private final PendingFinalizationStore checkpoint;
    private final DurableAgentRunFinalizer finalizer;
    private final AgentRuntimeStateStore runtimeStore;
    private final String ownerId;
    private final Clock clock;

    public DurableAgentRunWorker(
            DurableAgentRunDispatcher dispatcher,
            AgentRunExecution execution,
            PendingFinalizationStore checkpoint,
            DurableAgentRunFinalizer finalizer,
            AgentRuntimeStateStore runtimeStore,
            String ownerId,
            Clock clock) {
        this.dispatcher = Objects.requireNonNull(
                dispatcher, "dispatcher must not be null");
        this.execution = Objects.requireNonNull(
                execution, "execution must not be null");
        this.checkpoint = Objects.requireNonNull(
                checkpoint, "checkpoint must not be null");
        this.finalizer = Objects.requireNonNull(
                finalizer, "finalizer must not be null");
        this.runtimeStore = Objects.requireNonNull(
                runtimeStore, "runtimeStore must not be null");
        this.ownerId = Objects.requireNonNull(
                ownerId, "ownerId must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    /**
     * Production composition for the process-isolated worker path.
     *
     * <p>The same queue instance is deliberately shared by dispatch and finalization. Work and
     * results cross per-cycle file spools, while the child process is limited to re-running the
     * current JVM through {@link IsolatedWorkerLauncher}.
     */
    public static DurableAgentRunWorker processIsolated(
            DurableSingleWorkerSchedulerQueue queue,
            AgentRuntimeStateStore runtimeStore,
            PendingFinalizationStore checkpoint,
            Path projectRoot,
            Path evidenceRoot,
            Path runRecordRoot,
            Path invocationRoot,
            RunRecordStore runRecordStore,
            String ownerId,
            Clock clock,
            Duration processTimeout) {
        Objects.requireNonNull(queue, "queue must not be null");
        Objects.requireNonNull(runtimeStore, "runtimeStore must not be null");
        Objects.requireNonNull(checkpoint, "checkpoint must not be null");
        Objects.requireNonNull(runRecordStore, "runRecordStore must not be null");
        Objects.requireNonNull(clock, "clock must not be null");
        AgentRunExecution isolatedExecution = new ProcessIsolatedAgentRunExecution(
                invocationRoot,
                projectRoot,
                evidenceRoot,
                runRecordRoot,
                runRecordStore,
                new IsolatedWorkerLauncher(),
                processTimeout);
        return new DurableAgentRunWorker(
                new DurableAgentRunDispatcher(queue, runtimeStore, clock),
                isolatedExecution,
                checkpoint,
                new DurableAgentRunFinalizer(
                        queue, runtimeStore, runRecordStore, clock),
                runtimeStore,
                ownerId,
                clock);
    }

    public Optional<WorkItemDisposition> runOneCycle(Duration leaseDuration)
            throws IOException {
        Objects.requireNonNull(
                leaseDuration, "leaseDuration must not be null");
        Optional<PendingFinalization> pending = checkpoint.findPending();
        if (pending.isPresent()) {
            return resume(pending.orElseThrow(), leaseDuration);
        }
        String goalId = UUID.randomUUID().toString();
        String agentRunId = UUID.randomUUID().toString();
        while (agentRunId.equals(goalId)) {
            agentRunId = UUID.randomUUID().toString();
        }
        checkpoint.record(new PendingFinalization(
                goalId, agentRunId, Optional.empty()));
        return drive(goalId, agentRunId, Optional.empty(), leaseDuration);
    }

    private Optional<WorkItemDisposition> resume(
            PendingFinalization pending,
            Duration leaseDuration) throws IOException {
        DurableAgentRuntime runtime;
        try {
            runtime = DurableAgentRuntime.recover(
                    pending.goalId(), runtimeStore, clock);
        } catch (MissingAgentRuntimeStateException exception) {
            // The intent was recorded but the cycle stopped before the dispatcher
            // created the runtime (or the queue was empty); re-drive with the same
            // identities; mirrors the dispatcher's own recoverOrCreate tolerance.
            return drive(
                    pending.goalId(),
                    pending.agentRunId(),
                    pending.runRecordReference(),
                    leaseDuration);
        }
        Optional<RuntimeAgentRun> run = runtime.agentRun();
        if (run.isPresent() && run.orElseThrow().status().isTerminal()) {
            WorkItemDisposition disposition = finalizer
                    .recoverFinalization(pending.goalId())
                    .orElseThrow(() -> new IllegalStateException(
                            "terminal AgentRun did not yield a disposition"));
            checkpoint.clear();
            return Optional.of(disposition);
        }
        if (run.isPresent() && run.orElseThrow().status()
                == RuntimeAgentRunStatus.AWAITING_VERIFICATION) {
            WorkItemDisposition disposition = finalizer.finalizeAgentRun(
                    pending.goalId(),
                    pending.agentRunId(),
                    pending.runRecordReference().orElseThrow(() ->
                            new IllegalStateException(
                                    "AWAITING_VERIFICATION requires a recorded "
                                            + "RunRecord reference")));
            checkpoint.clear();
            return Optional.of(disposition);
        }
        // EXECUTING / READY / PLANNING / no AgentRun yet: re-drive with the same
        // identities; a present reference skips re-execution inside drive.
        return drive(
                pending.goalId(),
                pending.agentRunId(),
                pending.runRecordReference(),
                leaseDuration);
    }

    private Optional<WorkItemDisposition> drive(
            String goalId,
            String agentRunId,
            Optional<String> recordedReference,
            Duration leaseDuration) throws IOException {
        Optional<AgentRunDispatch> dispatched = dispatcher.claimAndLease(
                goalId, agentRunId, ownerId, leaseDuration);
        if (dispatched.isEmpty()) {
            // A cycle that claimed nothing leaves no durable trace.
            checkpoint.clear();
            return Optional.empty();
        }
        AgentRunDispatch dispatch = dispatched.orElseThrow();
        String reference;
        if (recordedReference.isPresent()) {
            reference = recordedReference.orElseThrow();
        } else {
            reference = execution.execute(dispatch);
            // Persist the reference before completeExecution so the
            // AWAITING_VERIFICATION window always has a recoverable reference.
            checkpoint.record(new PendingFinalization(
                    goalId, agentRunId, Optional.of(reference)));
        }
        // Process-specific transport artifacts are no longer needed once the reference is
        // checkpointed. If cleanup fails, the checkpoint remains and recovery retries this
        // operation without executing the work again.
        execution.cleanupAfterCheckpoint(dispatch);
        DurableAgentRuntime runtime = DurableAgentRuntime.recover(
                goalId, runtimeStore, clock);
        runtime.completeExecution(
                agentRunId, ownerId, dispatch.lease().fenceToken());
        WorkItemDisposition disposition = finalizer.finalizeAgentRun(
                goalId, agentRunId, reference);
        checkpoint.clear();
        return Optional.of(disposition);
    }
}
