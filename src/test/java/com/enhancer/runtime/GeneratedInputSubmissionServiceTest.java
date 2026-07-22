package com.enhancer.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.enhancer.bus.MessageEnvelope;
import com.enhancer.bus.WorkPayload;
import com.enhancer.workspace.ApprovedTaskRevision;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class GeneratedInputSubmissionServiceTest {
    private static final String SUBMISSION_ID =
            "00000000-0000-0000-0000-0000000000c1";
    private static final String TASK_ID = "generated-input-task";
    private static final String CAPABILITY = "read-file-worker";
    private static final String PRODUCER = "generated-input-cli";
    private static final String TARGET = "CURRENT_TASK.md";
    private static final String DIGEST = "a".repeat(64);
    private static final Instant FIRST_USE = Instant.parse("2026-07-22T10:15:00Z");
    private static final Instant LATER = Instant.parse("2027-01-01T00:00:00Z");

    @TempDir
    Path manifestRoot;

    @TempDir
    Path queueRoot;

    @Test
    void firstUseGeneratesIdentitiesFromClockAndAdmitsWork() throws Exception {
        AtomicReference<GeneratedSubmissionIdentities> captured =
                new AtomicReference<>();
        GeneratedInputSubmissionService service = service(FIRST_USE);

        DurableSubmissionResult result = service.submit(
                request(CAPABILITY),
                (identities, occurredAt) -> {
                    captured.set(identities);
                    assertEquals(FIRST_USE, occurredAt);
                    return envelope(identities, occurredAt, PRODUCER, DIGEST);
                });

        assertTrue(result.manifestCreated());
        assertTrue(result.queueCreated());
        assertTrue(result.workAdmitted());

        GeneratedSubmissionIdentities ids = captured.get();
        assertEquals(SUBMISSION_ID, ids.submissionId());
        assertEquals(ids.queueId(), result.queueId());

        // Every derived identity is a canonical UUID distinct from the key and each other.
        assertEquals(ids.queueId(), UUID.fromString(ids.queueId()).toString());
        assertEquals(ids.correlationId(), UUID.fromString(ids.correlationId()).toString());
        assertEquals(ids.logicalRunId(), UUID.fromString(ids.logicalRunId()).toString());
        assertNotEquals(SUBMISSION_ID, ids.queueId());
        assertNotEquals(SUBMISSION_ID, ids.correlationId());
        assertNotEquals(SUBMISSION_ID, ids.logicalRunId());
        assertNotEquals(ids.queueId(), ids.correlationId());
        assertNotEquals(ids.queueId(), ids.logicalRunId());
        assertNotEquals(ids.correlationId(), ids.logicalRunId());

        DurableSubmissionManifest stored = new FileSystemSubmissionManifestStore(
                manifestRoot).resolve(SUBMISSION_ID);
        assertEquals(FIRST_USE, stored.workMessage().occurredAt());
    }

    @Test
    void derivationIsDeterministicAcrossFreshStores(@TempDir Path otherManifestRoot,
            @TempDir Path otherQueueRoot) throws Exception {
        AtomicReference<GeneratedSubmissionIdentities> first = new AtomicReference<>();
        service(FIRST_USE).submit(request(CAPABILITY),
                (identities, occurredAt) -> {
                    first.set(identities);
                    return envelope(identities, occurredAt, PRODUCER, DIGEST);
                });

        AtomicReference<GeneratedSubmissionIdentities> second = new AtomicReference<>();
        new GeneratedInputSubmissionService(
                new FileSystemSubmissionManifestStore(otherManifestRoot),
                new FileSystemSchedulerQueueStore(otherQueueRoot),
                Clock.fixed(LATER, ZoneOffset.UTC)).submit(request(CAPABILITY),
                (identities, occurredAt) -> {
                    second.set(identities);
                    return envelope(identities, occurredAt, PRODUCER, DIGEST);
                });

        assertEquals(first.get(), second.get());
    }

    @Test
    void exactReplayReusesStoredManifestWithoutConsultingClockOrFactory()
            throws Exception {
        DurableSubmissionResult first = service(FIRST_USE).submit(
                request(CAPABILITY),
                (identities, occurredAt) ->
                        envelope(identities, occurredAt, PRODUCER, DIGEST));
        assertTrue(first.workAdmitted());

        // A later clock and a factory that must never run prove replay resolves first.
        DurableSubmissionResult replay = new GeneratedInputSubmissionService(
                new FileSystemSubmissionManifestStore(manifestRoot),
                new FileSystemSchedulerQueueStore(queueRoot),
                Clock.fixed(LATER, ZoneOffset.UTC)).submit(
                request(CAPABILITY),
                (identities, occurredAt) -> {
                    throw new AssertionError(
                            "replay must not capture the clock or repository context");
                });

        assertFalse(replay.manifestCreated());
        assertFalse(replay.queueCreated());
        assertFalse(replay.workAdmitted());
        assertEquals(first.queueId(), replay.queueId());
        assertEquals(first.queueRevision(), replay.queueRevision());

        DurableSubmissionManifest stored = new FileSystemSubmissionManifestStore(
                manifestRoot).resolve(SUBMISSION_ID);
        assertEquals(FIRST_USE, stored.workMessage().occurredAt());
    }

    @Test
    void conflictingCallerIntentOnReplayFailsClosed() throws Exception {
        service(FIRST_USE).submit(request(CAPABILITY),
                (identities, occurredAt) ->
                        envelope(identities, occurredAt, PRODUCER, DIGEST));

        GeneratedInputSubmissionService replay = new GeneratedInputSubmissionService(
                new FileSystemSubmissionManifestStore(manifestRoot),
                new FileSystemSchedulerQueueStore(queueRoot),
                Clock.fixed(LATER, ZoneOffset.UTC));

        assertThrows(IllegalArgumentException.class, () -> replay.submit(
                request("different-capability"),
                (identities, occurredAt) -> {
                    throw new AssertionError("must fail before capturing context");
                }));
    }

    @Test
    void firstUseEnvelopeInconsistentWithRequestFailsClosed() throws Exception {
        GeneratedInputSubmissionService service = service(FIRST_USE);

        // The factory returns a producer that disagrees with the caller-owned request.
        assertThrows(IllegalArgumentException.class, () -> service.submit(
                request(CAPABILITY),
                (identities, occurredAt) ->
                        envelope(identities, occurredAt, "wrong-producer", DIGEST)));
    }

    private GeneratedInputSubmissionService service(Instant now) {
        return new GeneratedInputSubmissionService(
                new FileSystemSubmissionManifestStore(manifestRoot),
                new FileSystemSchedulerQueueStore(queueRoot),
                Clock.fixed(now, ZoneOffset.UTC));
    }

    private GeneratedSubmissionRequest request(String capability) {
        return new GeneratedSubmissionRequest(
                SUBMISSION_ID, 4, capability, PRODUCER, TASK_ID, TARGET, DIGEST);
    }

    private MessageEnvelope envelope(
            GeneratedSubmissionIdentities identities,
            Instant occurredAt,
            String producer,
            String digest) {
        return new MessageEnvelope(
                identities.submissionId(),
                identities.correlationId(),
                Optional.empty(),
                identities.logicalRunId(),
                producer,
                occurredAt,
                new WorkPayload(
                        new ApprovedTaskRevision(TASK_ID, "CURRENT_TASK.md", "b".repeat(64)),
                        "c".repeat(64),
                        Set.of("read-file"),
                        Optional.of(new WorkPayload.ExecutionInput(TARGET, digest))));
    }
}
