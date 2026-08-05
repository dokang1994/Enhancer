package com.enhancer.cli;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

class EnhancerCliRuntimeEventAcknowledgeIntegrationTest {
    private static final String GOAL_ID =
            "00000000-0000-0000-0000-000000003201";
    private static final String WORK_ITEM_ID =
            "00000000-0000-0000-0000-000000003202";
    private static final String AGENT_RUN_ID =
            "00000000-0000-0000-0000-000000003203";

    @TempDir
    Path temporaryRoot;

    @Test
    void acknowledgesAndRecoversLostResponseWithTheSameBoundedMetadata()
            throws Exception {
        Path eventRoot = temporaryRoot.resolve("events");
        Path publicationRoot = temporaryRoot.resolve("publications");
        RuntimeEvent event = event();
        FileSystemRuntimeEventStore store = new FileSystemRuntimeEventStore(eventRoot);
        store.append(event);
        RuntimeEventPublicationReference reference =
                RuntimeEventPublicationReference.from(event);
        new FileSystemRuntimeEventPublisher(publicationRoot, 1).publish(reference);
        String pendingFile = pointName(reference);
        Path pending = publicationRoot.resolve(pendingFile);
        byte[] pointBytes = Files.readAllBytes(pending);
        FileTime pointTime = FileTime.from(Instant.parse("2026-08-05T03:00:00Z"));
        Files.setLastModifiedTime(pending, pointTime);
        Path eventArtifact = onlyFile(eventRoot);
        byte[] eventBytes = Files.readAllBytes(eventArtifact);
        FileTime eventTime = Files.getLastModifiedTime(eventArtifact);

        Invocation first = invoke(
                "runtime-event-acknowledge",
                "--runtime-event-root", eventRoot.toString(),
                "--runtime-event-publication-root", publicationRoot.toString(),
                "--publication-file", pendingFile);
        Invocation replay = invoke(
                "runtime-event-acknowledge",
                "--runtime-event-root", eventRoot.toString(),
                "--runtime-event-publication-root", publicationRoot.toString(),
                "--publication-file", pendingFile);

        String acknowledgedFile = pendingFile.substring(
                        0,
                        pendingFile.length()
                                - FileSystemRuntimeEventPublisher.FILE_SUFFIX.length())
                + FileSystemRuntimeEventPublisher.ACKNOWLEDGED_FILE_SUFFIX;
        Path acknowledged = publicationRoot.resolve(acknowledgedFile);
        assertEquals(0, first.exitCode(), first.stderr());
        assertEquals(0, replay.exitCode(), replay.stderr());
        assertEquals("ACKNOWLEDGED", value(first.stdout(), "status"));
        assertEquals("ALREADY_ACKNOWLEDGED", value(replay.stdout(), "status"));
        assertEquals(acknowledgedFile, value(first.stdout(), "acknowledgedFile"));
        assertEquals(acknowledgedFile, value(replay.stdout(), "acknowledgedFile"));
        assertEquals(reference.reference(), value(first.stdout(), "reference"));
        assertEquals(reference.reference(), value(replay.stdout(), "reference"));
        assertEquals(GOAL_ID, value(first.stdout(), "goalId"));
        assertEquals(event.eventId(), value(first.stdout(), "eventId"));
        assertEquals("TIMEOUT_DETECTED", value(first.stdout(), "kind"));
        assertEquals("1", value(first.stdout(), "streamRevision"));
        assertTrue(first.stdout().length() <= EnhancerCli.MAX_DIAGNOSTIC_CHARACTERS);
        assertFalse(first.stdout().contains("delivered"));
        assertFalse(first.stdout().contains("application"));
        assertFalse(Files.exists(pending));
        assertArrayEquals(pointBytes, Files.readAllBytes(acknowledged));
        assertEquals(pointTime, Files.getLastModifiedTime(acknowledged));
        assertArrayEquals(eventBytes, Files.readAllBytes(eventArtifact));
        assertEquals(eventTime, Files.getLastModifiedTime(eventArtifact));
        assertEquals(1, store.resolve(GOAL_ID).revision());
    }

    @Test
    void missingCorruptAndConflictingPointStateFailsWithoutMutation()
            throws Exception {
        Path missingEventRoot = temporaryRoot.resolve("missing-events");
        Path missingPublicationRoot = temporaryRoot.resolve("missing-publications");
        RuntimeEvent event = event();
        RuntimeEventPublicationReference reference =
                RuntimeEventPublicationReference.from(event);
        String pendingFile = pointName(reference);

        Invocation missing = invoke(
                "runtime-event-acknowledge",
                "--runtime-event-root", missingEventRoot.toString(),
                "--runtime-event-publication-root", missingPublicationRoot.toString(),
                "--publication-file", pendingFile);

        assertNotEquals(0, missing.exitCode());
        assertFalse(Files.exists(missingEventRoot));
        assertFalse(Files.exists(missingPublicationRoot));

        Path eventRoot = temporaryRoot.resolve("invalid-events");
        Path publicationRoot = temporaryRoot.resolve("invalid-publications");
        new FileSystemRuntimeEventStore(eventRoot).append(event);
        new FileSystemRuntimeEventPublisher(publicationRoot, 1).publish(reference);
        Path pending = publicationRoot.resolve(pendingFile);
        byte[] corrupt = Files.readAllBytes(pending);
        corrupt[0] ^= 1;
        Files.write(pending, corrupt);

        Invocation corruptResult = invoke(
                "runtime-event-acknowledge",
                "--runtime-event-root", eventRoot.toString(),
                "--runtime-event-publication-root", publicationRoot.toString(),
                "--publication-file", pendingFile);

        assertNotEquals(0, corruptResult.exitCode());
        assertArrayEquals(corrupt, Files.readAllBytes(pending));

        String acknowledgedFile = pendingFile.substring(
                        0,
                        pendingFile.length()
                                - FileSystemRuntimeEventPublisher.FILE_SUFFIX.length())
                + FileSystemRuntimeEventPublisher.ACKNOWLEDGED_FILE_SUFFIX;
        Path acknowledged = publicationRoot.resolve(acknowledgedFile);
        Files.copy(pending, acknowledged);
        Invocation conflicting = invoke(
                "runtime-event-acknowledge",
                "--runtime-event-root", eventRoot.toString(),
                "--runtime-event-publication-root", publicationRoot.toString(),
                "--publication-file", pendingFile);

        assertNotEquals(0, conflicting.exitCode());
        assertArrayEquals(corrupt, Files.readAllBytes(pending));
        assertArrayEquals(corrupt, Files.readAllBytes(acknowledged));
    }

    private RuntimeEvent event() {
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
                Instant.parse("2026-08-05T02:45:00Z"),
                binding,
                AGENT_RUN_ID,
                Optional.empty(),
                "runtime-event-acknowledgement-cli-test",
                new RuntimeEventDetail.TimeoutDetected(RuntimeTimeoutKind.LEASE),
                List.of(new RuntimeEventReference(
                        RuntimeEventReferenceKind.RUNTIME_STATE,
                        "agent-runtime/" + GOAL_ID + "/revision/12",
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
