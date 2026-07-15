package com.enhancer.bus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.enhancer.verification.VerificationStatus;
import com.enhancer.workspace.ApprovedTaskRevision;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class MessageEnvelopeTest {
    private static final Instant OCCURRED_AT = Instant.parse("2026-07-15T14:00:00Z");
    private static final String MESSAGE_ID = "00000000-0000-0000-0000-00000000000a";
    private static final String CAUSATION_ID = "00000000-0000-0000-0000-00000000000b";
    private static final String SNAPSHOT_ID = "a".repeat(64);
    private static final ApprovedTaskRevision TASK_REVISION = new ApprovedTaskRevision(
            "gate-7-envelope-test",
            "CURRENT_TASK.md",
            "f".repeat(64));

    @Test
    void carriesIdentitiesAndOneTypedPayload() {
        WorkPayload work = new WorkPayload(TASK_REVISION, SNAPSHOT_ID, Set.of("read-file"));
        MessageEnvelope envelope = new MessageEnvelope(
                MESSAGE_ID,
                "correlation-1",
                Optional.of(CAUSATION_ID),
                "logical-run-1",
                "agent-loop",
                OCCURRED_AT,
                work);

        assertEquals("message-envelope-v1", MessageEnvelope.ENVELOPE_VERSION);
        assertEquals(MESSAGE_ID, envelope.messageId());
        assertEquals("correlation-1", envelope.correlationId());
        assertEquals(Optional.of(CAUSATION_ID), envelope.causationId());
        assertEquals("logical-run-1", envelope.logicalRunId());
        assertEquals("agent-loop", envelope.producer());
        assertEquals(OCCURRED_AT, envelope.occurredAt());
        assertEquals(work, envelope.payload());
    }

    @Test
    void coversExactlyFourSealedPayloadKinds() {
        MessagePayload work = new WorkPayload(TASK_REVISION, SNAPSHOT_ID, Set.of("read-file"));
        MessagePayload result = new ResultPayload(
                "gate-7-envelope-test",
                "run-record/00000000-0000-0000-0000-000000000001",
                VerificationStatus.VERIFIED);
        MessagePayload control = new ControlPayload(
                ControlSignal.CANCEL,
                "budget exhausted");
        MessagePayload handoff = new HandoffPayload(
                TASK_REVISION,
                SNAPSHOT_ID,
                "run-record/00000000-0000-0000-0000-000000000002");

        for (MessagePayload payload : new MessagePayload[] {work, result, control, handoff}) {
            assertTrue(payload instanceof WorkPayload
                    || payload instanceof ResultPayload
                    || payload instanceof ControlPayload
                    || payload instanceof HandoffPayload);
        }
        assertEquals(
                4,
                MessagePayload.class.getPermittedSubclasses().length,
                "the payload hierarchy must stay sealed to exactly four kinds");
    }

    @Test
    void preservesAuthorizationAsImmutableDataWithoutCreatingIt() {
        WorkPayload work = new WorkPayload(TASK_REVISION, SNAPSHOT_ID, Set.of("read-file"));

        assertEquals(TASK_REVISION, work.taskRevision());
        assertEquals(SNAPSHOT_ID, work.snapshotId());
        assertEquals(Set.of("read-file"), work.allowedTools());
        assertThrows(
                UnsupportedOperationException.class,
                () -> work.allowedTools().add("write-file"));

        assertThrows(IllegalArgumentException.class, () -> new WorkPayload(
                TASK_REVISION, SNAPSHOT_ID, Set.of()));
        assertThrows(IllegalArgumentException.class, () -> new WorkPayload(
                TASK_REVISION, "not-a-digest", Set.of("read-file")));
        assertThrows(IllegalArgumentException.class, () -> new HandoffPayload(
                TASK_REVISION, "not-a-digest", "run-record/x"));
        assertThrows(IllegalArgumentException.class, () -> new ResultPayload(
                " ", "run-record/x", VerificationStatus.VERIFIED));
        assertThrows(IllegalArgumentException.class, () -> new ControlPayload(
                ControlSignal.PAUSE, " "));
    }

    @Test
    void rejectsInvalidIdentitiesAndSelfCausation() {
        WorkPayload work = new WorkPayload(TASK_REVISION, SNAPSHOT_ID, Set.of("read-file"));

        assertThrows(IllegalArgumentException.class, () -> envelope(
                "not-a-uuid", Optional.empty(), work));
        assertThrows(IllegalArgumentException.class, () -> envelope(
                MESSAGE_ID, Optional.of("not-a-uuid"), work));
        assertThrows(IllegalArgumentException.class, () -> envelope(
                MESSAGE_ID, Optional.of(MESSAGE_ID), work));
        assertThrows(NullPointerException.class, () -> envelope(
                MESSAGE_ID, null, work));
        assertThrows(NullPointerException.class, () -> envelope(
                MESSAGE_ID, Optional.empty(), null));
        assertThrows(IllegalArgumentException.class, () -> new MessageEnvelope(
                MESSAGE_ID, " ", Optional.empty(), "run", "producer", OCCURRED_AT, work));
        assertThrows(IllegalArgumentException.class, () -> new MessageEnvelope(
                MESSAGE_ID, "correlation-1", Optional.empty(), " ", "producer",
                OCCURRED_AT, work));
        assertThrows(IllegalArgumentException.class, () -> new MessageEnvelope(
                MESSAGE_ID, "correlation-1", Optional.empty(), "run", " ",
                OCCURRED_AT, work));
        assertThrows(NullPointerException.class, () -> new MessageEnvelope(
                MESSAGE_ID, "correlation-1", Optional.empty(), "run", "producer",
                null, work));
    }

    private MessageEnvelope envelope(
            String messageId,
            Optional<String> causationId,
            MessagePayload payload) {
        return new MessageEnvelope(
                messageId,
                "correlation-1",
                causationId,
                "logical-run-1",
                "agent-loop",
                OCCURRED_AT,
                payload);
    }
}
