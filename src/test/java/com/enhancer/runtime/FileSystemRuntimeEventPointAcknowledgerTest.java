package com.enhancer.runtime;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.enhancer.workspace.ApprovedTaskRevision;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FileSystemRuntimeEventPointAcknowledgerTest {
    private static final String GOAL_ID =
            "00000000-0000-0000-0000-000000003101";
    private static final String WORK_ITEM_ID =
            "00000000-0000-0000-0000-000000003102";
    private static final String AGENT_RUN_ID =
            "00000000-0000-0000-0000-000000003103";

    @TempDir
    Path temporaryRoot;

    @Test
    void acknowledgesExactPointReleasesCapacityAndPublisherReplayDoesNotReopenIt()
            throws Exception {
        Path eventRoot = temporaryRoot.resolve("events");
        Path publicationRoot = temporaryRoot.resolve("publications");
        FileSystemRuntimeEventStore store = new FileSystemRuntimeEventStore(eventRoot);
        RuntimeEvent first = event("1", "3104");
        RuntimeEvent second = event("2", "3105");
        store.append(first);
        store.append(second);
        RuntimeEventPublicationReference firstReference =
                RuntimeEventPublicationReference.from(first);
        RuntimeEventPublicationReference secondReference =
                RuntimeEventPublicationReference.from(second);
        FileSystemRuntimeEventPublisher publisher =
                new FileSystemRuntimeEventPublisher(publicationRoot, 1);
        publisher.publish(firstReference);
        String firstFile = FileSystemRuntimeEventPublisher.pointName(firstReference);
        Path firstPending = publicationRoot.resolve(firstFile);
        byte[] pointBytes = Files.readAllBytes(firstPending);
        FileTime retainedTime = FileTime.from(Instant.parse("2026-08-05T02:00:00Z"));
        Files.setLastModifiedTime(firstPending, retainedTime);
        Path eventArtifact = eventRoot.resolve(GOAL_ID + ".runtime-events");
        byte[] eventBytes = Files.readAllBytes(eventArtifact);
        FileTime eventTime = Files.getLastModifiedTime(eventArtifact);

        RuntimeEventPointAcknowledgement acknowledgement =
                new FileSystemRuntimeEventPointAcknowledger(publicationRoot, store)
                        .acknowledge(firstFile);

        Path firstAcknowledged = publicationRoot.resolve(
                FileSystemRuntimeEventPublisher.acknowledgedPointName(firstReference));
        assertEquals(
                RuntimeEventPointAcknowledgementStatus.ACKNOWLEDGED,
                acknowledgement.status());
        assertEquals(firstAcknowledged.getFileName().toString(),
                acknowledgement.acknowledgedFile());
        assertEquals(first, acknowledgement.resolution().event());
        assertFalse(Files.exists(firstPending));
        assertTrue(Files.isRegularFile(firstAcknowledged));
        assertArrayEquals(pointBytes, Files.readAllBytes(firstAcknowledged));
        assertEquals(retainedTime, Files.getLastModifiedTime(firstAcknowledged));

        publisher.publish(secondReference);
        publisher.publish(firstReference);

        assertFalse(Files.exists(firstPending));
        assertTrue(Files.exists(publicationRoot.resolve(
                FileSystemRuntimeEventPublisher.pointName(secondReference))));
        assertArrayEquals(pointBytes, Files.readAllBytes(firstAcknowledged));
        assertEquals(retainedTime, Files.getLastModifiedTime(firstAcknowledged));
        assertArrayEquals(eventBytes, Files.readAllBytes(eventArtifact));
        assertEquals(eventTime, Files.getLastModifiedTime(eventArtifact));
        assertEquals(2, store.resolve(GOAL_ID).revision());
    }

    @Test
    void acknowledgedReentryRevalidatesWithoutMovingAndConflictsFailClosed()
            throws Exception {
        Path eventRoot = temporaryRoot.resolve("reentry-events");
        Path publicationRoot = temporaryRoot.resolve("reentry-publications");
        FileSystemRuntimeEventStore store = new FileSystemRuntimeEventStore(eventRoot);
        RuntimeEvent event = event("3", "3106");
        store.append(event);
        RuntimeEventPublicationReference reference =
                RuntimeEventPublicationReference.from(event);
        new FileSystemRuntimeEventPublisher(publicationRoot, 1).publish(reference);
        String pendingFile = FileSystemRuntimeEventPublisher.pointName(reference);
        FileSystemRuntimeEventPointAcknowledger acknowledger =
                new FileSystemRuntimeEventPointAcknowledger(publicationRoot, store);
        RuntimeEventPointAcknowledgement first = acknowledger.acknowledge(pendingFile);
        Path acknowledged = publicationRoot.resolve(first.acknowledgedFile());
        byte[] acknowledgedBytes = Files.readAllBytes(acknowledged);
        FileTime retainedTime = FileTime.from(Instant.parse("2026-08-05T02:30:00Z"));
        Files.setLastModifiedTime(acknowledged, retainedTime);

        RuntimeEventPointAcknowledgement replay =
                new FileSystemRuntimeEventPointAcknowledger(publicationRoot, store)
                        .acknowledge(pendingFile);

        assertEquals(
                RuntimeEventPointAcknowledgementStatus.ALREADY_ACKNOWLEDGED,
                replay.status());
        assertEquals(first.resolution(), replay.resolution());
        assertArrayEquals(acknowledgedBytes, Files.readAllBytes(acknowledged));
        assertEquals(retainedTime, Files.getLastModifiedTime(acknowledged));

        Path pending = publicationRoot.resolve(pendingFile);
        Files.copy(acknowledged, pending);
        byte[] pendingBytes = Files.readAllBytes(pending);
        assertThrows(IOException.class, () -> acknowledger.acknowledge(pendingFile));
        assertArrayEquals(pendingBytes, Files.readAllBytes(pending));
        assertArrayEquals(acknowledgedBytes, Files.readAllBytes(acknowledged));
    }

    @Test
    void missingCorruptAndSymbolicStateFailWithoutAcknowledgement() throws Exception {
        Path eventRoot = temporaryRoot.resolve("invalid-events");
        Path publicationRoot = temporaryRoot.resolve("invalid-publications");
        FileSystemRuntimeEventStore store = new FileSystemRuntimeEventStore(eventRoot);
        RuntimeEvent event = event("4", "3107");
        store.append(event);
        RuntimeEventPublicationReference reference =
                RuntimeEventPublicationReference.from(event);
        String pendingFile = FileSystemRuntimeEventPublisher.pointName(reference);
        FileSystemRuntimeEventPointAcknowledger acknowledger =
                new FileSystemRuntimeEventPointAcknowledger(publicationRoot, store);

        assertThrows(IOException.class, () -> acknowledger.acknowledge(pendingFile));
        assertFalse(Files.exists(publicationRoot));

        new FileSystemRuntimeEventPublisher(publicationRoot, 1).publish(reference);
        Path pending = publicationRoot.resolve(pendingFile);
        byte[] corrupt = Files.readAllBytes(pending);
        corrupt[0] ^= 1;
        Files.write(pending, corrupt);
        assertThrows(IOException.class, () -> acknowledger.acknowledge(pendingFile));
        assertArrayEquals(corrupt, Files.readAllBytes(pending));
        assertFalse(Files.exists(publicationRoot.resolve(
                FileSystemRuntimeEventPublisher.acknowledgedPointName(reference))));

        Files.delete(pending);
        Path target = temporaryRoot.resolve("symbolic-target");
        Files.write(target, corrupt);
        try {
            Files.createSymbolicLink(pending, target);
        } catch (IOException | UnsupportedOperationException | SecurityException exception) {
            assumeTrue(false, "symbolic links unavailable on this host");
        }
        assertThrows(IOException.class, () -> acknowledger.acknowledge(pendingFile));
        assertArrayEquals(corrupt, Files.readAllBytes(target));
    }

    private RuntimeEvent event(String revision, String eventSuffix) {
        RuntimeEventBinding binding = new RuntimeEventBinding(
                GOAL_ID,
                WORK_ITEM_ID,
                new ApprovedTaskRevision(
                        "acknowledge-runtime-event-publication-point",
                        "CURRENT_TASK.md",
                        "a".repeat(64)),
                "b".repeat(64),
                "logical-run-runtime-event-acknowledgement",
                "correlation-runtime-event-acknowledgement");
        return RuntimeEvent.create(
                Instant.parse("2026-08-05T01:00:00Z").plusSeconds(Long.parseLong(revision)),
                binding,
                AGENT_RUN_ID,
                Optional.empty(),
                "runtime-event-acknowledgement-test",
                new RuntimeEventDetail.TimeoutDetected(RuntimeTimeoutKind.LEASE),
                List.of(new RuntimeEventReference(
                        RuntimeEventReferenceKind.RUNTIME_STATE,
                        "agent-runtime/" + GOAL_ID + "/revision/" + eventSuffix,
                        Optional.empty())));
    }
}
