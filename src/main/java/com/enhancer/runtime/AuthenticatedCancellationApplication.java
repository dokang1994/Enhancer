package com.enhancer.runtime;

import com.enhancer.bus.ControlPayload;
import com.enhancer.bus.ControlSignal;
import com.enhancer.bus.MessageEnvelope;
import java.io.IOException;
import java.time.Clock;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Authenticates one retained CANCEL request and persists its terminal application. */
public final class AuthenticatedCancellationApplication {
    private static final String EVENT_PRODUCER_ID =
            "authenticated-cancellation-application";

    private final AgentRuntimeStateStore store;
    private final Clock clock;
    private final ControlRequestAuthorizer authorizer;
    private final Optional<RuntimeEventRecorder> eventRecorder;

    public AuthenticatedCancellationApplication(
            AgentRuntimeStateStore store,
            Clock clock,
            ControlRequestAuthorizer authorizer) {
        this(store, clock, authorizer, Optional.empty());
    }

    public AuthenticatedCancellationApplication(
            AgentRuntimeStateStore store,
            Clock clock,
            ControlRequestAuthorizer authorizer,
            RuntimeEventRecorder eventRecorder) {
        this(
                store,
                clock,
                authorizer,
                Optional.of(Objects.requireNonNull(
                        eventRecorder, "eventRecorder must not be null")));
    }

    private AuthenticatedCancellationApplication(
            AgentRuntimeStateStore store,
            Clock clock,
            ControlRequestAuthorizer authorizer,
            Optional<RuntimeEventRecorder> eventRecorder) {
        this.store = Objects.requireNonNull(store, "store must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        this.authorizer = Objects.requireNonNull(
                authorizer, "authorizer must not be null");
        this.eventRecorder = Objects.requireNonNull(
                eventRecorder, "eventRecorder must not be null");
    }

    public CancellationApplicationRecord apply(
            String goalId,
            String controlMessageId) throws IOException {
        String canonicalGoalId = AgentRuntimeState.requireCanonicalGoalId(goalId);
        String canonicalControlId = RuntimeIdentity.canonicalUuid(
                controlMessageId, "controlMessageId");
        DurableAgentRuntime runtime = DurableAgentRuntime.recoverForControlAdmission(
                canonicalGoalId, store, clock);
        if (runtime.cancellationApplication().isPresent()) {
            CancellationApplicationRecord existing =
                    runtime.cancellationApplication().orElseThrow();
            if (!existing.controlMessageId().equals(canonicalControlId)) {
                throw new IllegalStateException(
                        "Goal is already cancelled by a different Control request");
            }
            recordCancellationAppliedEvent(runtime, existing);
            return existing;
        }
        MessageEnvelope request = runtime.controlRequests().stream()
                .filter(candidate -> candidate.messageId().equals(canonicalControlId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "cancellation requires an exact retained Control request"));
        if (!(request.payload() instanceof ControlPayload payload)
                || payload.signal() != ControlSignal.CANCEL) {
            throw new IllegalArgumentException(
                    "cancellation application requires a retained CANCEL request");
        }
        ControlAuthorizationDecision decision = Objects.requireNonNull(
                authorizer.authorize(request),
                "authorizer decision must not be null");
        if (decision instanceof ControlAuthorizationDecision.Denied denied) {
            throw new ControlAuthorizationDeniedException(denied.reason());
        }
        ControlAuthorizationDecision.Approved approved =
                (ControlAuthorizationDecision.Approved) decision;
        if (!approved.goalId().equals(canonicalGoalId)
                || !approved.controlMessageId().equals(canonicalControlId)
                || approved.signal() != ControlSignal.CANCEL) {
            throw new IllegalArgumentException(
                    "control authorization does not match the cancellation request");
        }
        String agentRunId = runtime.agentRun()
                .orElseThrow(() -> new IllegalStateException(
                        "cancellation requires an AgentRun"))
                .agentRunId();
        CancellationApplicationRecord record = new CancellationApplicationRecord(
                approved.authorizationId(),
                approved.actorId(),
                canonicalGoalId,
                canonicalControlId,
                agentRunId,
                approved.authorizedAt(),
                clock.instant());
        runtime.applyCancellation(record);
        CancellationApplicationRecord persisted =
                runtime.cancellationApplication().orElseThrow();
        recordCancellationAppliedEvent(runtime, persisted);
        return persisted;
    }

    private void recordCancellationAppliedEvent(
            DurableAgentRuntime runtime,
            CancellationApplicationRecord record) throws IOException {
        if (eventRecorder.isEmpty()) {
            return;
        }
        WorkItem workItem = runtime.goal().workItem();
        RuntimeEventBinding binding = new RuntimeEventBinding(
                runtime.goal().goalId(),
                workItem.workItemId(),
                workItem.taskRevision(),
                workItem.snapshotId(),
                workItem.logicalRunId(),
                workItem.workMessage().correlationId());
        RuntimeEvent event = RuntimeEvent.create(
                record.appliedAt(),
                binding,
                record.agentRunId(),
                Optional.of(record.controlMessageId()),
                EVENT_PRODUCER_ID,
                new RuntimeEventDetail.CancellationApplied(
                        record.controlMessageId()),
                List.of(
                        new RuntimeEventReference(
                                RuntimeEventReferenceKind.CONTROL_MESSAGE,
                                "control-message/" + record.controlMessageId(),
                                Optional.empty()),
                        new RuntimeEventReference(
                                RuntimeEventReferenceKind.CONTROL_APPLICATION,
                                record.reference(),
                                Optional.empty())));
        eventRecorder.orElseThrow().recordAndPublish(event);
    }
}
