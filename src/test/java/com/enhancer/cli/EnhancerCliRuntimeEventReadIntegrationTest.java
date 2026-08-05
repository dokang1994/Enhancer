package com.enhancer.cli;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import com.enhancer.runtime.FileSystemRuntimeEventPublisher;
import com.enhancer.runtime.FileSystemRuntimeEventStore;
import com.enhancer.runtime.RuntimeEvent;
import com.enhancer.runtime.RuntimeEventBinding;
import com.enhancer.runtime.RuntimeEventDetail;
import com.enhancer.runtime.RuntimeEventPublicationReference;
import com.enhancer.runtime.RuntimeEventReference;
import com.enhancer.runtime.RuntimeEventReferenceKind;
import com.enhancer.runtime.RuntimeTimeoutKind;
import com.enhancer.workspace.ApprovedTaskRevision;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class EnhancerCliRuntimeEventReadIntegrationTest {
    private static final String GOAL_ID =
            "00000000-0000-0000-0000-000000003001";
    private static final String WORK_ITEM_ID =
            "00000000-0000-0000-0000-000000003002";
    private static final String AGENT_RUN_ID =
            "00000000-0000-0000-0000-000000003003";

    @TempDir
    Path temporaryRoot;

    @Test
    void readsOneExactPublicationPointWithoutMutatingEitherArtifact()
            throws Exception {
        Path eventRoot = temporaryRoot.resolve("events");
        Path publicationRoot = temporaryRoot.resolve("publications");
        RuntimeEvent event = event();
        FileSystemRuntimeEventStore store =
                new FileSystemRuntimeEventStore(eventRoot);
        store.append(event);
        RuntimeEventPublicationReference reference =
                RuntimeEventPublicationReference.from(event);
        new FileSystemRuntimeEventPublisher(publicationRoot, 1)
                .publish(reference);
        Path eventArtifact = onlyFile(eventRoot);
        Path point = publicationRoot.resolve(pointName(reference));
        byte[] eventBytes = Files.readAllBytes(eventArtifact);
        byte[] pointBytes = Files.readAllBytes(point);
        FileTime retainedTime =
                FileTime.from(Instant.parse("2026-08-05T01:00:00Z"));
        Files.setLastModifiedTime(eventArtifact, retainedTime);
        Files.setLastModifiedTime(point, retainedTime);

        Invocation first = invoke(
                "runtime-event-read",
                "--runtime-event-root", eventRoot.toString(),
                "--runtime-event-publication-root", publicationRoot.toString(),
                "--publication-file", point.getFileName().toString());
        Invocation second = invoke(
                "runtime-event-read",
                "--runtime-event-root", eventRoot.toString(),
                "--runtime-event-publication-root", publicationRoot.toString(),
                "--publication-file", point.getFileName().toString());

        assertEquals(0, first.exitCode(), first.stderr());
        assertEquals(first.stdout(), second.stdout());
        assertEquals("AVAILABLE", value(first.stdout(), "status"));
        assertEquals(reference.reference(), value(first.stdout(), "reference"));
        assertEquals(GOAL_ID, value(first.stdout(), "goalId"));
        assertEquals(event.eventId(), value(first.stdout(), "eventId"));
        assertEquals("TIMEOUT_DETECTED", value(first.stdout(), "kind"));
        assertEquals(AGENT_RUN_ID, value(first.stdout(), "agentRunId"));
        assertEquals("read-only-consumer-fixture", value(first.stdout(), "producerId"));
        assertEquals("resolve-runtime-event-publication-point-read-only",
                value(first.stdout(), "taskId"));
        assertEquals("1", value(first.stdout(), "streamRevision"));
        assertEquals("1", value(first.stdout(), "authoritativeReferences"));
        assertArrayEquals(eventBytes, Files.readAllBytes(eventArtifact));
        assertArrayEquals(pointBytes, Files.readAllBytes(point));
        assertEquals(retainedTime, Files.getLastModifiedTime(eventArtifact));
        assertEquals(retainedTime, Files.getLastModifiedTime(point));
    }

    @Test
    void missingEventAndCorruptPointFailClosedWithoutCreatingOrRewritingState()
            throws Exception {
        Path missingEventRoot = temporaryRoot.resolve("missing-events");
        Path publicationRoot = temporaryRoot.resolve("orphan-publications");
        RuntimeEventPublicationReference orphan = new RuntimeEventPublicationReference(
                "runtime-event/"
                        + GOAL_ID
                        + "/00000000-0000-0000-0000-000000003099");
        new FileSystemRuntimeEventPublisher(publicationRoot, 2).publish(orphan);
        Path orphanPoint = publicationRoot.resolve(pointName(orphan));
        byte[] orphanBytes = Files.readAllBytes(orphanPoint);

        Invocation missing = invoke(
                "runtime-event-read",
                "--runtime-event-root", missingEventRoot.toString(),
                "--runtime-event-publication-root", publicationRoot.toString(),
                "--publication-file", orphanPoint.getFileName().toString());

        assertNotEquals(0, missing.exitCode());
        assertFalse(Files.exists(missingEventRoot));
        assertArrayEquals(orphanBytes, Files.readAllBytes(orphanPoint));

        Path eventRoot = temporaryRoot.resolve("corrupt-events");
        RuntimeEvent event = event();
        new FileSystemRuntimeEventStore(eventRoot).append(event);
        RuntimeEventPublicationReference reference =
                RuntimeEventPublicationReference.from(event);
        new FileSystemRuntimeEventPublisher(publicationRoot, 2).publish(reference);
        Path corruptPoint = publicationRoot.resolve(pointName(reference));
        byte[] corruptBytes = Files.readAllBytes(corruptPoint);
        corruptBytes[0] ^= 0x01;
        Files.write(corruptPoint, corruptBytes);

        Invocation corrupt = invoke(
                "runtime-event-read",
                "--runtime-event-root", eventRoot.toString(),
                "--runtime-event-publication-root", publicationRoot.toString(),
                "--publication-file", corruptPoint.getFileName().toString());

        assertNotEquals(0, corrupt.exitCode());
        assertArrayEquals(corruptBytes, Files.readAllBytes(corruptPoint));
    }

    private RuntimeEvent event() {
        RuntimeEventBinding binding = new RuntimeEventBinding(
                GOAL_ID,
                WORK_ITEM_ID,
                new ApprovedTaskRevision(
                        "resolve-runtime-event-publication-point-read-only",
                        "CURRENT_TASK.md",
                        "a".repeat(64)),
                "b".repeat(64),
                "logical-run-read-only-consumer",
                "correlation-read-only-consumer");
        return RuntimeEvent.create(
                Instant.parse("2026-08-05T00:30:00Z"),
                binding,
                AGENT_RUN_ID,
                Optional.empty(),
                "read-only-consumer-fixture",
                new RuntimeEventDetail.TimeoutDetected(RuntimeTimeoutKind.LEASE),
                List.of(new RuntimeEventReference(
                        RuntimeEventReferenceKind.RUNTIME_STATE,
                        "agent-runtime/" + GOAL_ID + "/revision/4",
                        Optional.empty())));
    }

    private Path onlyFile(Path root) throws Exception {
        try (var paths = Files.list(root)) {
            return paths.filter(Files::isRegularFile).findFirst().orElseThrow();
        }
    }

    private String pointName(RuntimeEventPublicationReference reference)
            throws Exception {
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(
                reference.reference().getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().formatHex(digest)
                + FileSystemRuntimeEventPublisher.FILE_SUFFIX;
    }

    private Invocation invoke(String... arguments) {
        ByteArrayOutputStream stdoutBytes = new ByteArrayOutputStream();
        ByteArrayOutputStream stderrBytes = new ByteArrayOutputStream();
        int exitCode = new EnhancerCli().execute(
                arguments,
                new PrintStream(stdoutBytes, true, StandardCharsets.UTF_8),
                new PrintStream(stderrBytes, true, StandardCharsets.UTF_8));
        return new Invocation(
                exitCode,
                stdoutBytes.toString(StandardCharsets.UTF_8),
                stderrBytes.toString(StandardCharsets.UTF_8));
    }

    private String value(String output, String name) {
        return output.lines()
                .filter(line -> line.startsWith(name + "="))
                .map(line -> line.substring(name.length() + 1))
                .findFirst()
                .orElseThrow();
    }

    private record Invocation(int exitCode, String stdout, String stderr) {
    }
}
