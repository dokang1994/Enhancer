package com.enhancer.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.enhancer.bus.ControlPayload;
import com.enhancer.bus.ControlSignal;
import com.enhancer.bus.MessageEnvelope;
import com.enhancer.bus.WorkPayload;
import com.enhancer.workspace.ApprovedTaskRevision;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class WorkItemTest {
    private static final String MESSAGE_ID =
            "00000000-0000-0000-0000-00000000000a";
    private static final String WORK_ITEM_ID =
            "00000000-0000-0000-0000-00000000000b";
    private static final String SNAPSHOT_ID = "a".repeat(64);
    private static final ApprovedTaskRevision TASK_REVISION = new ApprovedTaskRevision(
            "gate-8-work-item-admission-contract",
            "CURRENT_TASK.md",
            "b".repeat(64));

    @Test
    void admitsOneGate7WorkEnvelopeWithoutFlatteningAuthority() {
        MessageEnvelope envelope = workEnvelope();

        WorkItem item = new WorkItem(WORK_ITEM_ID, "read-file-worker", envelope);

        assertEquals(WORK_ITEM_ID, item.workItemId());
        assertEquals("read-file-worker", item.requiredCapability());
        assertSame(envelope, item.workMessage());
        assertEquals("logical-run-1", item.logicalRunId());
        assertSame(TASK_REVISION, item.taskRevision());
        assertEquals(SNAPSHOT_ID, item.snapshotId());
        assertSame(((WorkPayload) envelope.payload()).allowedTools(), item.allowedTools());
        assertEquals(Set.of("read-file"), item.allowedTools());
    }

    @Test
    void projectsTheDeclaredExecutionInput() {
        assertEquals(Optional.empty(),
                new WorkItem(WORK_ITEM_ID, "read-file-worker", workEnvelope())
                        .executionInput());

        WorkPayload.ExecutionInput input = new WorkPayload.ExecutionInput(
                "docs/target.md", "e".repeat(64));
        MessageEnvelope declared = envelope(
                MESSAGE_ID,
                new WorkPayload(
                        TASK_REVISION,
                        SNAPSHOT_ID,
                        Set.of("read-file"),
                        Optional.of(input)));

        assertEquals(Optional.of(input),
                new WorkItem(WORK_ITEM_ID, "read-file-worker", declared)
                        .executionInput());
    }

    @Test
    void rejectsNonWorkEnvelopesAndInvalidAdmissionMetadata() {
        MessageEnvelope work = workEnvelope();
        MessageEnvelope control = envelope(
                "00000000-0000-0000-0000-00000000000c",
                new ControlPayload(ControlSignal.CANCEL, "operator request"));

        assertThrows(IllegalArgumentException.class, () ->
                new WorkItem(MESSAGE_ID, "read-file-worker", work));
        assertThrows(IllegalArgumentException.class, () ->
                new WorkItem("not-a-uuid", "read-file-worker", work));
        assertThrows(IllegalArgumentException.class, () ->
                new WorkItem(WORK_ITEM_ID, " ", work));
        assertThrows(IllegalArgumentException.class, () ->
                new WorkItem(
                        WORK_ITEM_ID,
                        "x".repeat(WorkItem.MAX_CAPABILITY_CHARACTERS + 1),
                        work));
        assertThrows(IllegalArgumentException.class, () ->
                new WorkItem(WORK_ITEM_ID, "read-file-worker", control));
        assertThrows(NullPointerException.class, () ->
                new WorkItem(null, "read-file-worker", work));
        assertThrows(NullPointerException.class, () ->
                new WorkItem(WORK_ITEM_ID, null, work));
        assertThrows(NullPointerException.class, () ->
                new WorkItem(WORK_ITEM_ID, "read-file-worker", null));
    }

    private static MessageEnvelope workEnvelope() {
        return envelope(
                MESSAGE_ID,
                new WorkPayload(TASK_REVISION, SNAPSHOT_ID, Set.of("read-file")));
    }

    private static MessageEnvelope envelope(
            String messageId,
            com.enhancer.bus.MessagePayload payload) {
        return new MessageEnvelope(
                messageId,
                "correlation-1",
                Optional.empty(),
                "logical-run-1",
                "gate-8-admission",
                Instant.parse("2026-07-16T10:00:00Z"),
                payload);
    }
}
