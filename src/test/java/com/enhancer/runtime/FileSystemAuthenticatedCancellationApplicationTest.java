package com.enhancer.runtime;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.enhancer.bus.ControlPayload;
import com.enhancer.bus.ControlSignal;
import com.enhancer.bus.MessageEnvelope;
import com.enhancer.bus.WorkPayload;
import com.enhancer.workspace.ApprovedTaskRevision;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FileSystemAuthenticatedCancellationApplicationTest {
    private static final String GOAL_ID =
            "00000000-0000-0000-0000-000000004201";
    private static final String AGENT_RUN_ID =
            "00000000-0000-0000-0000-000000004202";
    private static final String WORK_ITEM_ID =
            "00000000-0000-0000-0000-000000004203";
    private static final String WORK_MESSAGE_ID =
            "00000000-0000-0000-0000-000000004204";
    private static final String CONTROL_MESSAGE_ID =
            "00000000-0000-0000-0000-000000004205";
    private static final String AUTHORIZATION_ID =
            "00000000-0000-0000-0000-000000004206";
    private static final Clock SOURCE_CLOCK = Clock.fixed(
            Instant.parse("2026-08-11T04:00:00Z"), ZoneOffset.UTC);
    private static final Clock APPLIED_CLOCK = Clock.fixed(
            Instant.parse("2026-08-11T04:00:01Z"), ZoneOffset.UTC);

    @TempDir
    Path temporaryRoot;

    @Test
    void approvedEventAwareCancellationPublishesAndExactReplaysWithoutAuthority()
            throws Exception {
        Path runtimeRoot = temporaryRoot.resolve("approved-runtime");
        Path eventRoot = temporaryRoot.resolve("approved-events");
        Path publicationRoot = temporaryRoot.resolve("approved-publications");
        FileSystemAgentRuntimeStateStore store = readyRuntime(runtimeRoot);
        AtomicInteger authorizations = new AtomicInteger();
        FileSystemRuntimeEventPublicationConfiguration configuration =
                new FileSystemRuntimeEventPublicationConfiguration(
                        eventRoot, publicationRoot, 16);

        CancellationApplicationRecord applied =
                new FileSystemAuthenticatedCancellationApplication(
                        runtimeRoot,
                        APPLIED_CLOCK,
                        (ignoredGoal, request) -> {
                            authorizations.incrementAndGet();
                            return approved(request.messageId());
                        },
                        configuration)
                        .apply(GOAL_ID, CONTROL_MESSAGE_ID);

        AgentRuntimeState persisted = store.resolve(GOAL_ID);
        assertEquals(RuntimeGoalStatus.CANCELLED, persisted.goal().status());
        assertEquals(Optional.of(applied), persisted.cancellationApplication());
        assertEquals(1, authorizations.get());
        RuntimeEventStream stream =
                new FileSystemRuntimeEventStore(eventRoot).resolve(GOAL_ID);
        assertEquals(1, stream.revision());
        assertEquals(1, stream.events().size());
        assertEquals(
                RuntimeEventKind.CANCELLATION_APPLIED,
                stream.events().get(0).kind());
        String pointFile = singleFileName(
                publicationRoot,
                FileSystemRuntimeEventPublisher.FILE_SUFFIX);
        RuntimeEventPointResolution resolution =
                new FileSystemRuntimeEventPointReader(
                        publicationRoot,
                        new FileSystemRuntimeEventStore(eventRoot))
                        .resolve(pointFile);
        assertEquals(stream.events().get(0), resolution.event());

        long runtimeRevision = persisted.revision();
        byte[] runtimeBytes = singleFileBytes(runtimeRoot, ".agent-runtime");
        byte[] eventBytes = singleFileBytes(eventRoot, ".runtime-events");
        byte[] pointBytes = singleFileBytes(
                publicationRoot,
                FileSystemRuntimeEventPublisher.FILE_SUFFIX);

        CancellationApplicationRecord replayed =
                new FileSystemAuthenticatedCancellationApplication(
                        runtimeRoot,
                        Clock.fixed(
                                APPLIED_CLOCK.instant().plusSeconds(30),
                                ZoneOffset.UTC),
                        (ignoredGoal, ignoredRequest) -> {
                            throw new AssertionError(
                                    "exact replay must not reauthorize");
                        },
                        configuration)
                        .apply(GOAL_ID, CONTROL_MESSAGE_ID);

        assertEquals(applied, replayed);
        assertEquals(runtimeRevision, store.resolve(GOAL_ID).revision());
        assertArrayEquals(
                runtimeBytes,
                singleFileBytes(runtimeRoot, ".agent-runtime"));
        assertArrayEquals(
                eventBytes,
                singleFileBytes(eventRoot, ".runtime-events"));
        assertArrayEquals(
                pointBytes,
                singleFileBytes(
                        publicationRoot,
                        FileSystemRuntimeEventPublisher.FILE_SUFFIX));
        assertEquals(
                List.of(pointFile),
                fileNamesWithSuffix(
                        publicationRoot,
                        FileSystemRuntimeEventPublisher.FILE_SUFFIX));
    }

    @Test
    void deniedCancellationLeavesRuntimeAndEventOutputsUntouched()
            throws Exception {
        Path runtimeRoot = temporaryRoot.resolve("denied-runtime");
        Path eventRoot = temporaryRoot.resolve("denied-events");
        Path publicationRoot = temporaryRoot.resolve("denied-publications");
        FileSystemAgentRuntimeStateStore store = readyRuntime(runtimeRoot);
        long sourceRevision = store.resolve(GOAL_ID).revision();
        byte[] sourceBytes = singleFileBytes(runtimeRoot, ".agent-runtime");

        FileSystemAuthenticatedCancellationApplication application =
                new FileSystemAuthenticatedCancellationApplication(
                        runtimeRoot,
                        APPLIED_CLOCK,
                        (ignoredGoal, ignoredRequest) ->
                                new ControlAuthorizationDecision.Denied(
                                "actor lacks cancellation authority"),
                        new FileSystemRuntimeEventPublicationConfiguration(
                                eventRoot, publicationRoot, 16));

        assertThrows(
                ControlAuthorizationDeniedException.class,
                () -> application.apply(GOAL_ID, CONTROL_MESSAGE_ID));
        assertEquals(sourceRevision, store.resolve(GOAL_ID).revision());
        assertArrayEquals(
                sourceBytes,
                singleFileBytes(runtimeRoot, ".agent-runtime"));
        assertFalse(Files.exists(eventRoot));
        assertFalse(Files.exists(publicationRoot));
    }

    @Test
    void eventFreeCancellationCreatesOnlyTheRuntimeArtifact() throws Exception {
        Path runtimeRoot = temporaryRoot.resolve("event-free-runtime");
        Path unusedEventRoot = temporaryRoot.resolve("event-free-events");
        Path unusedPublicationRoot =
                temporaryRoot.resolve("event-free-publications");
        FileSystemAgentRuntimeStateStore store = readyRuntime(runtimeRoot);

        CancellationApplicationRecord applied =
                new FileSystemAuthenticatedCancellationApplication(
                        runtimeRoot,
                        APPLIED_CLOCK,
                        (ignoredGoal, request) -> approved(request.messageId()))
                        .apply(GOAL_ID, CONTROL_MESSAGE_ID);

        assertEquals(
                Optional.of(applied),
                store.resolve(GOAL_ID).cancellationApplication());
        assertFalse(Files.exists(unusedEventRoot));
        assertFalse(Files.exists(unusedPublicationRoot));
    }

    @Test
    void eventConfigurationRequiresBothRootsAndBoundedCapacity() {
        Path eventRoot = temporaryRoot.resolve("events");
        Path publicationRoot = temporaryRoot.resolve("publications");

        assertThrows(
                NullPointerException.class,
                () -> new FileSystemRuntimeEventPublicationConfiguration(
                        null, publicationRoot, 1));
        assertThrows(
                NullPointerException.class,
                () -> new FileSystemRuntimeEventPublicationConfiguration(
                        eventRoot, null, 1));
        assertThrows(
                IllegalArgumentException.class,
                () -> new FileSystemRuntimeEventPublicationConfiguration(
                        eventRoot, publicationRoot, 0));
        assertThrows(
                IllegalArgumentException.class,
                () -> new FileSystemRuntimeEventPublicationConfiguration(
                        eventRoot,
                        publicationRoot,
                        FileSystemRuntimeEventPublisher.MAX_PENDING_PUBLICATIONS + 1));
    }

    private FileSystemAgentRuntimeStateStore readyRuntime(Path runtimeRoot)
            throws Exception {
        FileSystemAgentRuntimeStateStore store =
                new FileSystemAgentRuntimeStateStore(runtimeRoot);
        DurableAgentRuntime runtime = DurableAgentRuntime.create(
                GOAL_ID, workItem(), store, SOURCE_CLOCK);
        runtime.beginAgentRun(AGENT_RUN_ID);
        runtime.markReady(AGENT_RUN_ID);
        runtime.recordControlRequest(controlMessage());
        return store;
    }

    private ControlAuthorizationDecision.Approved approved(
            String controlMessageId) {
        return new ControlAuthorizationDecision.Approved(
                AUTHORIZATION_ID,
                "operator-17",
                GOAL_ID,
                controlMessageId,
                ControlSignal.CANCEL,
                SOURCE_CLOCK.instant());
    }

    private WorkItem workItem() {
        ApprovedTaskRevision revision = new ApprovedTaskRevision(
                "gate-12-filesystem-authenticated-cancel",
                "CURRENT_TASK.md",
                "8c7d6e5f4011223344556677889900aabbccddeeff00112233445566778899aa");
        MessageEnvelope work = new MessageEnvelope(
                WORK_MESSAGE_ID,
                "correlation-filesystem-authenticated-cancel",
                Optional.empty(),
                "logical-run-filesystem-authenticated-cancel",
                "filesystem-authenticated-cancel-test",
                Instant.parse("2026-08-11T03:50:00Z"),
                new WorkPayload(
                        revision,
                        "7b6c5d4e3011223344556677889900aabbccddeeff00112233445566778899aa",
                        Set.of("read-file")));
        return new WorkItem(
                WORK_ITEM_ID,
                "read-file-worker",
                work);
    }

    private MessageEnvelope controlMessage() {
        return new MessageEnvelope(
                CONTROL_MESSAGE_ID,
                "correlation-filesystem-authenticated-cancel",
                Optional.of(WORK_MESSAGE_ID),
                "logical-run-filesystem-authenticated-cancel",
                "untrusted-control-producer",
                Instant.parse("2026-08-11T03:55:00Z"),
                new ControlPayload(ControlSignal.CANCEL, "operator intent"));
    }

    private byte[] singleFileBytes(Path root, String suffix) throws Exception {
        return Files.readAllBytes(root.resolve(singleFileName(root, suffix)));
    }

    private String singleFileName(Path root, String suffix) throws Exception {
        List<String> names = fileNamesWithSuffix(root, suffix);
        assertEquals(1, names.size());
        return names.get(0);
    }

    private List<String> fileNamesWithSuffix(Path root, String suffix)
            throws Exception {
        try (Stream<Path> paths = Files.list(root)) {
            return paths
                    .filter(path -> path.getFileName().toString().endsWith(suffix))
                    .map(path -> path.getFileName().toString())
                    .sorted()
                    .toList();
        }
    }
}
