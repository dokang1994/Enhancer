package com.enhancer.runtime;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.enhancer.workspace.ApprovedTaskRevision;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FileSystemRuntimeEventPublisherTest {
    private static final String GOAL_ID =
            "00000000-0000-0000-0000-000000002201";
    private static final String WORK_ITEM_ID =
            "00000000-0000-0000-0000-000000002202";
    private static final String AGENT_RUN_ID =
            "00000000-0000-0000-0000-000000002203";

    @TempDir
    Path temporaryRoot;

    @Test
    void publishesOneIntegrityCheckedPointAndExactReplayDoesNotRewrite()
            throws Exception {
        Path publicationRoot = temporaryRoot.resolve("publications");
        FileSystemRuntimeEventPublisher publisher =
                new FileSystemRuntimeEventPublisher(publicationRoot, 1);
        RuntimeEventPublicationReference reference = reference("2204");

        publisher.publish(reference);

        Path point = publicationRoot.resolve(
                FileSystemRuntimeEventPublisher.pointName(reference));
        byte[] firstBytes = Files.readAllBytes(point);
        FileTime retainedTime = FileTime.from(
                Instant.parse("2026-08-04T01:00:00Z"));
        Files.setLastModifiedTime(point, retainedTime);

        publisher.publish(reference);

        assertArrayEquals(firstBytes, Files.readAllBytes(point));
        assertEquals(retainedTime, Files.getLastModifiedTime(point));
        assertEquals(reference, publisher.resolveAcceptedPoint(reference));
        assertEquals(1, acceptedPointCount(publicationRoot));
        String envelopeText = new String(firstBytes, StandardCharsets.ISO_8859_1);
        assertTrue(envelopeText.contains(reference.reference()));
        assertFalse(envelopeText.contains("TIMEOUT_DETECTED"));
    }

    @Test
    void capacityRefusesOnlyANewPointAndLeavesNoCandidate() throws Exception {
        Path publicationRoot = temporaryRoot.resolve("bounded-publications");
        FileSystemRuntimeEventPublisher publisher =
                new FileSystemRuntimeEventPublisher(publicationRoot, 1);
        RuntimeEventPublicationReference first = reference("2205");
        RuntimeEventPublicationReference second = reference("2206");
        publisher.publish(first);

        assertThrows(IOException.class, () -> publisher.publish(second));

        assertEquals(first, publisher.resolveAcceptedPoint(first));
        assertFalse(Files.exists(publicationRoot.resolve(
                FileSystemRuntimeEventPublisher.pointName(second))));
        try (var paths = Files.list(publicationRoot)) {
            assertFalse(paths.anyMatch(path ->
                    path.getFileName().toString().startsWith(".pending-")));
        }
    }

    @Test
    void corruptExistingPointFailsClosedWithoutRewrite() throws Exception {
        Path publicationRoot = temporaryRoot.resolve("corrupt-publications");
        FileSystemRuntimeEventPublisher publisher =
                new FileSystemRuntimeEventPublisher(publicationRoot, 1);
        RuntimeEventPublicationReference reference = reference("2207");
        publisher.publish(reference);
        Path point = publicationRoot.resolve(
                FileSystemRuntimeEventPublisher.pointName(reference));
        byte[] corrupt = Files.readAllBytes(point);
        corrupt[0] ^= 0x01;
        Files.write(point, corrupt);

        assertThrows(IOException.class, () -> publisher.publish(reference));
        assertArrayEquals(corrupt, Files.readAllBytes(point));
    }

    @Test
    void symbolicPointFailsClosedWhenHostAllowsSymbolicLinks() throws Exception {
        Path publicationRoot = temporaryRoot.resolve("symbolic-publications");
        Files.createDirectories(publicationRoot);
        RuntimeEventPublicationReference reference = reference("2208");
        Path target = temporaryRoot.resolve("symbolic-target");
        Files.writeString(target, "foreign");
        Path point = publicationRoot.resolve(
                FileSystemRuntimeEventPublisher.pointName(reference));
        try {
            Files.createSymbolicLink(point, target);
        } catch (IOException | UnsupportedOperationException | SecurityException exception) {
            assumeTrue(false, "symbolic links unavailable on this host");
        }

        FileSystemRuntimeEventPublisher publisher =
                new FileSystemRuntimeEventPublisher(publicationRoot, 1);
        assertThrows(IOException.class, () -> publisher.publish(reference));
        assertEquals("foreign", Files.readString(target));
    }

    @Test
    void unusableRootAndInvalidCapacityFailBeforeFalseSuccess() throws Exception {
        Path rootFile = temporaryRoot.resolve("root-file");
        Files.writeString(rootFile, "not-a-directory");
        RuntimeEventPublicationReference reference = reference("2209");

        assertThrows(
                IllegalArgumentException.class,
                () -> new FileSystemRuntimeEventPublisher(temporaryRoot, 0));
        assertThrows(
                IllegalArgumentException.class,
                () -> new FileSystemRuntimeEventPublisher(temporaryRoot, 4097));
        FileSystemRuntimeEventPublisher publisher =
                new FileSystemRuntimeEventPublisher(rootFile, 1);
        assertThrows(IOException.class, () -> publisher.publish(reference));
        assertEquals("not-a-directory", Files.readString(rootFile));
    }

    @Test
    void recorderPersistsEventBeforeConcreteReferencePoint() throws Exception {
        Path eventRoot = temporaryRoot.resolve("events");
        Path publicationRoot = temporaryRoot.resolve("points");
        FileSystemRuntimeEventStore eventStore =
                new FileSystemRuntimeEventStore(eventRoot);
        FileSystemRuntimeEventPublisher publisher =
                new FileSystemRuntimeEventPublisher(publicationRoot, 4);
        RuntimeEventRecorder recorder = new RuntimeEventRecorder(eventStore, publisher);
        RuntimeEvent event = event();

        assertEquals(
                RuntimeEventAppendResult.APPENDED,
                recorder.recordAndPublish(event));

        assertEquals(List.of(event), eventStore.resolve(GOAL_ID).events());
        RuntimeEventPublicationReference reference =
                RuntimeEventPublicationReference.from(event);
        assertEquals(reference, publisher.resolveAcceptedPoint(reference));
    }

    private long acceptedPointCount(Path root) throws IOException {
        try (var paths = Files.list(root)) {
            return paths.filter(path -> path.getFileName().toString()
                            .endsWith(FileSystemRuntimeEventPublisher.FILE_SUFFIX))
                    .count();
        }
    }

    private RuntimeEventPublicationReference reference(String eventSuffix) {
        return new RuntimeEventPublicationReference(
                "runtime-event/"
                        + GOAL_ID
                        + "/00000000-0000-0000-0000-00000000"
                        + eventSuffix);
    }

    private RuntimeEvent event() {
        RuntimeEventBinding binding = new RuntimeEventBinding(
                GOAL_ID,
                WORK_ITEM_ID,
                new ApprovedTaskRevision(
                        "publish-runtime-event-references-to-filesystem-points",
                        "CURRENT_TASK.md",
                        "a".repeat(64)),
                "b".repeat(64),
                "logical-run-filesystem-publisher",
                "correlation-filesystem-publisher");
        return RuntimeEvent.create(
                Instant.parse("2026-08-04T02:00:00Z"),
                binding,
                AGENT_RUN_ID,
                Optional.empty(),
                "filesystem-publisher-test",
                new RuntimeEventDetail.TimeoutDetected(RuntimeTimeoutKind.LEASE),
                List.of(new RuntimeEventReference(
                        RuntimeEventReferenceKind.RUNTIME_STATE,
                        "agent-runtime/" + GOAL_ID + "/revision/9",
                        Optional.empty())));
    }
}
