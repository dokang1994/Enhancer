package com.enhancer.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.enhancer.kernel.VerificationStatus;
import com.enhancer.workspace.ApprovedTaskRevision;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class RuntimeEventTest {
    private static final String GOAL_ID =
            "00000000-0000-0000-0000-000000002001";
    private static final String WORK_ITEM_ID =
            "00000000-0000-0000-0000-000000002002";
    private static final String AGENT_RUN_ID =
            "00000000-0000-0000-0000-000000002003";
    private static final String CONTROL_MESSAGE_ID =
            "00000000-0000-0000-0000-000000002004";
    private static final Instant OCCURRED_AT =
            Instant.parse("2026-07-31T06:00:00.123456789Z");

    @Test
    void exposesOnlyTheAcceptedRuntimeEventTaxonomy() {
        assertEquals(
                List.of(
                        "RETRY_DECISION_RECORDED",
                        "RETRY_STARTED",
                        "STAGNATION_DETECTED",
                        "TIMEOUT_DETECTED",
                        "CANCELLATION_REQUEST_RECORDED",
                        "CANCELLATION_APPLIED",
                        "VERIFICATION_RECORDED",
                        "WORK_ITEM_TERMINATED"),
                Arrays.stream(RuntimeEventKind.values())
                        .map(Enum::name)
                        .toList());
    }

    @Test
    void sealedDetailsBindEachAcceptedKindAndRejectInvalidStates() {
        assertEquals(
                RuntimeEventKind.RETRY_DECISION_RECORDED,
                new RuntimeEventDetail.RetryDecisionRecorded(
                        true, Optional.empty()).kind());
        assertEquals(
                RuntimeEventKind.RETRY_STARTED,
                new RuntimeEventDetail.RetryStarted(AGENT_RUN_ID).kind());
        assertEquals(
                RuntimeEventKind.STAGNATION_DETECTED,
                new RuntimeEventDetail.StagnationDetected(4, 3).kind());
        assertEquals(
                RuntimeEventKind.TIMEOUT_DETECTED,
                new RuntimeEventDetail.TimeoutDetected(
                        RuntimeTimeoutKind.LEASE).kind());
        assertEquals(
                RuntimeEventKind.CANCELLATION_REQUEST_RECORDED,
                new RuntimeEventDetail.CancellationRequestRecorded(
                        CONTROL_MESSAGE_ID).kind());
        assertEquals(
                RuntimeEventKind.CANCELLATION_APPLIED,
                new RuntimeEventDetail.CancellationApplied(
                        CONTROL_MESSAGE_ID).kind());
        assertEquals(
                RuntimeEventKind.VERIFICATION_RECORDED,
                new RuntimeEventDetail.VerificationRecorded(
                        VerificationStatus.VERIFIED).kind());
        assertEquals(
                RuntimeEventKind.WORK_ITEM_TERMINATED,
                new RuntimeEventDetail.WorkItemTerminated(
                        WorkItemDisposition.VERIFIED_COMPLETED).kind());

        assertThrows(IllegalArgumentException.class, () ->
                new RuntimeEventDetail.RetryDecisionRecorded(
                        true,
                        Optional.of(
                                AgentRunRetryRefusalReason.ATTEMPTS_EXHAUSTED)));
        assertThrows(IllegalArgumentException.class, () ->
                new RuntimeEventDetail.RetryDecisionRecorded(
                        false, Optional.empty()));
        assertThrows(IllegalArgumentException.class, () ->
                new RuntimeEventDetail.StagnationDetected(2, 3));
        assertThrows(IllegalArgumentException.class, () ->
                new RuntimeEventDetail.RetryStarted("not-a-uuid"));
    }

    @Test
    void derivesIdentityFromKindGoalRunAndOrderedAuthoritativeReferences() {
        List<RuntimeEventReference> references = new ArrayList<>(List.of(
                reference(RuntimeEventReferenceKind.RUNTIME_STATE, "runtime/7"),
                reference(RuntimeEventReferenceKind.RETRY_DECISION, "retry/2")));
        RuntimeEvent first = event(
                new RuntimeEventDetail.RetryDecisionRecorded(
                        true, Optional.empty()),
                references);
        RuntimeEvent replay = event(
                new RuntimeEventDetail.RetryDecisionRecorded(
                        true, Optional.empty()),
                references);
        RuntimeEvent reordered = event(
                new RuntimeEventDetail.RetryDecisionRecorded(
                        true, Optional.empty()),
                List.of(references.get(1), references.get(0)));

        assertEquals(first.eventId(), replay.eventId());
        assertNotEquals(first.eventId(), reordered.eventId());
        references.clear();
        assertEquals(2, first.authoritativeReferences().size());
        assertThrows(UnsupportedOperationException.class, () ->
                first.authoritativeReferences().add(
                        reference(RuntimeEventReferenceKind.EVIDENCE, "e/1")));
    }

    @Test
    void streamRejectsChangedContentUnderAReusedIdentityAndEventRejectsKindMismatch() {
        RuntimeEvent original = event(
                new RuntimeEventDetail.TimeoutDetected(
                        RuntimeTimeoutKind.PROCESS),
                List.of(reference(
                        RuntimeEventReferenceKind.RUNTIME_STATE,
                        "runtime/8")));

        RuntimeEvent changed = new RuntimeEvent(
                RuntimeEvent.SCHEMA_VERSION,
                original.eventId(),
                original.kind(),
                original.occurredAt().plusSeconds(1),
                original.binding(),
                original.agentRunId(),
                original.causationId(),
                original.producerId(),
                original.detail(),
                original.authoritativeReferences());
        RuntimeEventStream stream =
                RuntimeEventStream.initial(binding()).append(original);
        assertThrows(IllegalArgumentException.class, () ->
                stream.append(changed));
        assertThrows(IllegalArgumentException.class, () -> new RuntimeEvent(
                RuntimeEvent.SCHEMA_VERSION,
                original.eventId(),
                RuntimeEventKind.VERIFICATION_RECORDED,
                original.occurredAt(),
                original.binding(),
                original.agentRunId(),
                original.causationId(),
                original.producerId(),
                original.detail(),
                original.authoritativeReferences()));
    }

    @Test
    void streamAppendsMonotonicallyReplaysExactlyAndRejectsForeignBinding() {
        RuntimeEvent first = event(
                new RuntimeEventDetail.VerificationRecorded(
                        VerificationStatus.VERIFIED),
                List.of(reference(
                        RuntimeEventReferenceKind.EVIDENCE,
                        "verification/1")));
        RuntimeEventStream initial = RuntimeEventStream.initial(binding());
        RuntimeEventStream appended = initial.append(first);

        assertEquals(0, initial.revision());
        assertEquals(1, appended.revision());
        assertEquals(List.of(first), appended.events());
        assertSame(appended, appended.append(first));

        RuntimeEvent foreign = RuntimeEvent.create(
                OCCURRED_AT,
                new RuntimeEventBinding(
                        GOAL_ID,
                        "00000000-0000-0000-0000-000000002099",
                        taskRevision(),
                        "b".repeat(64),
                        "logical-run-runtime-events",
                        "correlation-runtime-events"),
                AGENT_RUN_ID,
                Optional.empty(),
                "runtime-event-test",
                new RuntimeEventDetail.VerificationRecorded(
                        VerificationStatus.REJECTED),
                List.of(reference(
                        RuntimeEventReferenceKind.EVIDENCE,
                        "verification/2")));
        assertThrows(IllegalArgumentException.class, () ->
                appended.append(foreign));
    }

    @Test
    void rejectsInvalidBindingsReferenceBoundsAndMalformedProducerText() {
        assertThrows(IllegalArgumentException.class, () ->
                new RuntimeEventBinding(
                        "not-a-goal",
                        WORK_ITEM_ID,
                        taskRevision(),
                        "b".repeat(64),
                        "logical-run-runtime-events",
                        "correlation-runtime-events"));
        assertThrows(IllegalArgumentException.class, () ->
                new RuntimeEventBinding(
                        GOAL_ID,
                        WORK_ITEM_ID,
                        taskRevision(),
                        "not-a-snapshot",
                        "logical-run-runtime-events",
                        "correlation-runtime-events"));
        assertThrows(IllegalArgumentException.class, () -> event(
                new RuntimeEventDetail.TimeoutDetected(
                        RuntimeTimeoutKind.TOOL),
                List.of()));
        assertThrows(IllegalArgumentException.class, () -> event(
                new RuntimeEventDetail.TimeoutDetected(
                        RuntimeTimeoutKind.TOOL),
                List.of(
                        reference(RuntimeEventReferenceKind.RUN_RECORD, "r/1"),
                        reference(RuntimeEventReferenceKind.RUN_RECORD, "r/2"),
                        reference(RuntimeEventReferenceKind.RUN_RECORD, "r/3"),
                        reference(RuntimeEventReferenceKind.RUN_RECORD, "r/4"),
                        reference(RuntimeEventReferenceKind.RUN_RECORD, "r/5"))));
        assertThrows(IllegalArgumentException.class, () ->
                RuntimeEvent.create(
                        OCCURRED_AT,
                        binding(),
                        AGENT_RUN_ID,
                        Optional.empty(),
                        "\uD800",
                        new RuntimeEventDetail.TimeoutDetected(
                                RuntimeTimeoutKind.TOOL),
                        List.of(reference(
                                RuntimeEventReferenceKind.RUNTIME_STATE,
                                "runtime/invalid-producer"))));
    }

    @Test
    void streamIsBoundedAndSuccessorRequiresAnUnchangedPrefix() {
        RuntimeEventStream stream = RuntimeEventStream.initial(binding());
        for (int index = 0; index < RuntimeEventStream.MAX_EVENTS; index++) {
            stream = stream.append(event(
                    new RuntimeEventDetail.TimeoutDetected(
                            RuntimeTimeoutKind.TOOL),
                    List.of(reference(
                            RuntimeEventReferenceKind.RUN_RECORD,
                            "run/" + index))));
        }
        RuntimeEventStream full = stream;
        assertThrows(IllegalStateException.class, () -> full.append(event(
                new RuntimeEventDetail.TimeoutDetected(
                        RuntimeTimeoutKind.TOOL),
                List.of(reference(
                        RuntimeEventReferenceKind.RUN_RECORD,
                        "run/overflow")))));

        RuntimeEvent first = event(
                new RuntimeEventDetail.CancellationApplied(CONTROL_MESSAGE_ID),
                List.of(reference(
                        RuntimeEventReferenceKind.CONTROL_MESSAGE,
                        "control/1")));
        RuntimeEvent replacement = event(
                new RuntimeEventDetail.CancellationApplied(CONTROL_MESSAGE_ID),
                List.of(reference(
                        RuntimeEventReferenceKind.CONTROL_MESSAGE,
                        "control/2")));
        RuntimeEventStream accepted = RuntimeEventStream.initial(binding())
                .append(first);
        RuntimeEventStream rewritten = new RuntimeEventStream(
                RuntimeEventStream.CURRENT_SCHEMA_VERSION,
                binding(),
                2,
                List.of(replacement, first));
        assertEquals(false, rewritten.isValidSuccessorOf(accepted));
    }

    private static RuntimeEvent event(
            RuntimeEventDetail detail,
            List<RuntimeEventReference> references) {
        return RuntimeEvent.create(
                OCCURRED_AT,
                binding(),
                AGENT_RUN_ID,
                Optional.empty(),
                "runtime-event-test",
                detail,
                references);
    }

    private static RuntimeEventBinding binding() {
        return new RuntimeEventBinding(
                GOAL_ID,
                WORK_ITEM_ID,
                taskRevision(),
                "b".repeat(64),
                "logical-run-runtime-events",
                "correlation-runtime-events");
    }

    private static ApprovedTaskRevision taskRevision() {
        return new ApprovedTaskRevision(
                "implement-runtime-event-store-contract",
                "CURRENT_TASK.md",
                "a".repeat(64));
    }

    private static RuntimeEventReference reference(
            RuntimeEventReferenceKind kind,
            String value) {
        return new RuntimeEventReference(
                kind, value, Optional.of("c".repeat(64)));
    }
}
