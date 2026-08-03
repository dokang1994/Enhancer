package com.enhancer.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.enhancer.workspace.ApprovedTaskRevision;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class RuntimeEventRecorderTest {
    private static final String GOAL_ID =
            "00000000-0000-0000-0000-000000002101";
    private static final String WORK_ITEM_ID =
            "00000000-0000-0000-0000-000000002102";
    private static final String AGENT_RUN_ID =
            "00000000-0000-0000-0000-000000002103";

    @TempDir
    Path storageRoot;

    @Test
    void appendsBeforePublishingAndExactReplaysAfterPublicationFailure()
            throws Exception {
        FileSystemRuntimeEventStore store =
                new FileSystemRuntimeEventStore(storageRoot);
        RuntimeEvent event = event();
        List<RuntimeEventPublicationReference> attempts = new ArrayList<>();
        AtomicInteger publications = new AtomicInteger();
        RuntimeEventPublisher publisher = reference -> {
            attempts.add(reference);
            RuntimeEventStream persisted = store.resolve(GOAL_ID);
            assertEquals(List.of(event), persisted.events());
            if (publications.getAndIncrement() == 0) {
                throw new IOException("simulated publication failure");
            }
        };
        RuntimeEventRecorder recorder =
                new RuntimeEventRecorder(store, publisher);

        assertThrows(IOException.class, () ->
                recorder.recordAndPublish(event));
        assertEquals(1, store.resolve(GOAL_ID).revision());

        assertEquals(
                RuntimeEventAppendResult.REPLAYED,
                recorder.recordAndPublish(event));
        assertEquals(1, store.resolve(GOAL_ID).revision());
        assertEquals(2, attempts.size());
        assertEquals(attempts.get(0), attempts.get(1));
        assertEquals(
                "runtime-event/" + GOAL_ID + "/" + event.eventId(),
                attempts.get(0).reference());
    }

    @Test
    void appendFailureDoesNotInvokePublisher() {
        AtomicInteger publications = new AtomicInteger();
        RuntimeEventStore failingStore = new RuntimeEventStore() {
            @Override
            public RuntimeEventAppendResult append(RuntimeEvent event)
                    throws IOException {
                throw new IOException("simulated append failure");
            }

            @Override
            public RuntimeEventStream resolve(String goalId) {
                throw new UnsupportedOperationException("not used");
            }
        };
        RuntimeEventRecorder recorder = new RuntimeEventRecorder(
                failingStore,
                reference -> publications.incrementAndGet());

        assertThrows(IOException.class, () ->
                recorder.recordAndPublish(event()));
        assertEquals(0, publications.get());
    }

    @Test
    void firstPersistedOccurrenceIsRecoveredWithoutWeakeningExactReplay()
            throws Exception {
        FileSystemRuntimeEventStore store =
                new FileSystemRuntimeEventStore(storageRoot);
        List<RuntimeEventPublicationReference> published = new ArrayList<>();
        RuntimeEventRecorder recorder =
                new RuntimeEventRecorder(store, published::add);
        RuntimeEvent first = eventAt(
                Instant.parse("2026-08-03T03:00:00Z"),
                RuntimeTimeoutKind.PROCESS);
        RuntimeEvent laterCandidate = eventAt(
                Instant.parse("2026-08-03T04:00:00Z"),
                RuntimeTimeoutKind.PROCESS);

        assertEquals(
                RuntimeEventAppendResult.APPENDED,
                recorder.recordAndPublishUsingFirstOccurrence(first));
        assertEquals(
                RuntimeEventAppendResult.REPLAYED,
                recorder.recordAndPublishUsingFirstOccurrence(laterCandidate));

        RuntimeEventStream stream = store.resolve(GOAL_ID);
        assertEquals(1, stream.revision());
        assertEquals(List.of(first), stream.events());
        assertEquals(
                List.of(
                        RuntimeEventPublicationReference.from(first),
                        RuntimeEventPublicationReference.from(first)),
                published);

        RuntimeEvent changedContent = eventAt(
                Instant.parse("2026-08-03T05:00:00Z"),
                RuntimeTimeoutKind.TOOL);
        assertThrows(
                IOException.class,
                () -> recorder.recordAndPublishUsingFirstOccurrence(
                        changedContent));
        assertEquals(1, store.resolve(GOAL_ID).revision());
        assertEquals(2, published.size());
    }

    private static RuntimeEvent event() {
        return eventAt(
                Instant.parse("2026-08-03T03:00:00Z"),
                RuntimeTimeoutKind.PROCESS);
    }

    private static RuntimeEvent eventAt(
            Instant occurredAt,
            RuntimeTimeoutKind timeoutKind) {
        RuntimeEventBinding binding = new RuntimeEventBinding(
                GOAL_ID,
                WORK_ITEM_ID,
                new ApprovedTaskRevision(
                        "connect-runtime-event-recorder",
                        "CURRENT_TASK.md",
                        "a".repeat(64)),
                "b".repeat(64),
                "logical-run-runtime-event-recorder",
                "correlation-runtime-event-recorder");
        return RuntimeEvent.create(
                occurredAt,
                binding,
                AGENT_RUN_ID,
                Optional.empty(),
                "runtime-event-recorder-test",
                new RuntimeEventDetail.TimeoutDetected(
                        timeoutKind),
                List.of(new RuntimeEventReference(
                        RuntimeEventReferenceKind.RUNTIME_STATE,
                        "agent-runtime/" + GOAL_ID + "/revision/7",
                        Optional.empty())));
    }
}
