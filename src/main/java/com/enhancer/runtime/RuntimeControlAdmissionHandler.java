package com.enhancer.runtime;

import com.enhancer.bus.ControlPayload;
import com.enhancer.bus.ControlSignal;
import com.enhancer.bus.MessageEnvelope;
import com.enhancer.bus.MessageHandler;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Clock;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Gate 7-to-Gate 8 adapter that records a bound control request durably without applying it.
 */
public final class RuntimeControlAdmissionHandler implements MessageHandler {
    private static final String EVENT_PRODUCER_ID =
            "runtime-control-admission";

    private final String goalId;
    private final AgentRuntimeStateStore store;
    private final Clock clock;
    private final Optional<RuntimeEventRecorder> eventRecorder;

    public RuntimeControlAdmissionHandler(
            String goalId,
            AgentRuntimeStateStore store,
            Clock clock) {
        this(goalId, store, clock, Optional.empty());
    }

    public RuntimeControlAdmissionHandler(
            String goalId,
            AgentRuntimeStateStore store,
            Clock clock,
            RuntimeEventRecorder eventRecorder) {
        this(
                goalId,
                store,
                clock,
                Optional.of(Objects.requireNonNull(
                        eventRecorder, "eventRecorder must not be null")));
    }

    private RuntimeControlAdmissionHandler(
            String goalId,
            AgentRuntimeStateStore store,
            Clock clock,
            Optional<RuntimeEventRecorder> eventRecorder) {
        this.goalId = AgentRuntimeState.requireCanonicalGoalId(goalId);
        this.store = Objects.requireNonNull(
                store, "store must not be null");
        this.clock = Objects.requireNonNull(
                clock, "clock must not be null");
        this.eventRecorder = Objects.requireNonNull(
                eventRecorder, "eventRecorder must not be null");
    }

    @Override
    public void handle(MessageEnvelope envelope) {
        try {
            DurableAgentRuntime runtime =
                    DurableAgentRuntime.recoverForControlAdmission(
                            goalId, store, clock);
            runtime.recordControlRequest(envelope);
            if (envelope.payload() instanceof ControlPayload control
                    && control.signal() == ControlSignal.CANCEL
                    && eventRecorder.isPresent()) {
                eventRecorder.orElseThrow().recordAndPublish(
                        cancellationRequestEvent(runtime, envelope));
            }
        } catch (IOException exception) {
            throw new UncheckedIOException(
                    "runtime control admission could not persist or publish its durable facts",
                    exception);
        }
    }

    private RuntimeEvent cancellationRequestEvent(
            DurableAgentRuntime runtime,
            MessageEnvelope request) {
        WorkItem workItem = runtime.goal().workItem();
        String agentRunId = runtime.agentRun()
                .orElseThrow(() -> new IllegalStateException(
                        "control request event requires an AgentRun"))
                .agentRunId();
        RuntimeEventBinding binding = new RuntimeEventBinding(
                runtime.goal().goalId(),
                workItem.workItemId(),
                workItem.taskRevision(),
                workItem.snapshotId(),
                workItem.logicalRunId(),
                workItem.workMessage().correlationId());
        return RuntimeEvent.create(
                request.occurredAt(),
                binding,
                agentRunId,
                Optional.of(request.messageId()),
                EVENT_PRODUCER_ID,
                new RuntimeEventDetail.CancellationRequestRecorded(
                        request.messageId()),
                List.of(
                        new RuntimeEventReference(
                                RuntimeEventReferenceKind.CONTROL_MESSAGE,
                                "control-message/" + request.messageId(),
                                Optional.empty()),
                        new RuntimeEventReference(
                                RuntimeEventReferenceKind.RUNTIME_STATE,
                                "agent-runtime/"
                                        + runtime.goal().goalId()
                                        + "/revision/"
                                        + runtime.revision(),
                                Optional.empty())));
    }
}
