package com.enhancer.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.enhancer.bus.ControlPayload;
import com.enhancer.bus.ControlSignal;
import com.enhancer.bus.MessageEnvelope;
import com.enhancer.bus.ResultPayload;
import com.enhancer.bus.WorkPayload;
import com.enhancer.kernel.VerificationStatus;
import com.enhancer.workspace.ApprovedTaskRevision;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FileSystemAgentRuntimeStateStoreIntegrationTest {
    private static final String GOAL_ID =
            "00000000-0000-0000-0000-000000000501";
    private static final String AGENT_RUN_ID =
            "00000000-0000-0000-0000-000000000502";
    private static final String WORK_ITEM_ID =
            "00000000-0000-0000-0000-000000000503";
    private static final String WORK_MESSAGE_ID =
            "00000000-0000-0000-0000-000000000504";
    private static final int DIGEST_OFFSET =
            Integer.BYTES + Long.BYTES + Integer.BYTES;
    private static final int PAYLOAD_OFFSET = DIGEST_OFFSET + 32;

    @TempDir
    Path storageRoot;

    @Test
    void restoresExactLifecycleAcrossStoreInstances() throws Exception {
        DurableAgentRuntime runtime = DurableAgentRuntime.create(
                GOAL_ID,
                workItem(),
                new FileSystemAgentRuntimeStateStore(storageRoot),
                Clock.fixed(
                        Instant.parse("2026-07-16T20:00:00Z"),
                        ZoneOffset.UTC));
        runtime.beginAgentRun(AGENT_RUN_ID);
        runtime.markReady(AGENT_RUN_ID);
        AgentRunLease lease = runtime.acquireLease(
                AGENT_RUN_ID,
                "filesystem-worker-\uD83D\uDE80",
                Duration.ofMinutes(5));
        runtime.completeExecution(
                AGENT_RUN_ID,
                lease.ownerId(),
                lease.fenceToken());
        runtime.recordResult(
                AGENT_RUN_ID,
                resultMessage(VerificationStatus.VERIFIED));

        DurableAgentRuntime recovered = DurableAgentRuntime.recover(
                GOAL_ID,
                new FileSystemAgentRuntimeStateStore(storageRoot),
                Clock.fixed(
                        Instant.parse("2026-07-16T20:01:00Z"),
                        ZoneOffset.UTC));

        assertEquals(5, recovered.revision());
        assertEquals(RuntimeGoalStatus.COMPLETED, recovered.goal().status());
        assertEquals(workItem(), recovered.goal().workItem());
        RuntimeAgentRun run = recovered.agentRun().orElseThrow();
        assertEquals(RuntimeAgentRunStatus.COMPLETED, run.status());
        assertEquals(
                resultMessage(VerificationStatus.VERIFIED),
                run.resultMessage().orElseThrow());
        assertEquals(1, recovered.lastIssuedFenceToken());
    }

    @Test
    void restoresRetryPendingAttemptAndDecisionHistoryAcrossStoreInstances()
            throws Exception {
        FileSystemAgentRuntimeStateStore store =
                new FileSystemAgentRuntimeStateStore(storageRoot);
        DurableAgentRuntime runtime = DurableAgentRuntime.create(
                GOAL_ID,
                workItem(),
                store,
                Clock.fixed(
                        Instant.parse("2026-07-22T04:00:00Z"),
                        ZoneOffset.UTC));
        runtime.beginAgentRun(AGENT_RUN_ID);
        runtime.markReady(AGENT_RUN_ID);
        AgentRunLease lease = runtime.acquireLease(
                AGENT_RUN_ID, "schema-v2-store-owner", Duration.ofMinutes(5));
        runtime.completeExecution(
                AGENT_RUN_ID, lease.ownerId(), lease.fenceToken());
        runtime.recordResult(
                AGENT_RUN_ID,
                resultMessage(VerificationStatus.REJECTED));
        AgentRunRetryDecisionRecord decision = new AgentRunRetryDecisionRecord(
                AGENT_RUN_ID,
                1,
                3,
                0,
                0,
                "c".repeat(64),
                AgentRunRetryDecision.admitted());
        assertTrue(runtime.recordRetryDecision(decision));

        DurableAgentRuntime recovered = DurableAgentRuntime.recover(
                GOAL_ID,
                new FileSystemAgentRuntimeStateStore(storageRoot),
                Clock.fixed(
                        Instant.parse("2026-07-22T04:01:00Z"),
                        ZoneOffset.UTC));

        assertEquals(RuntimeGoalStatus.RETRY_PENDING, recovered.goal().status());
        assertEquals(1, recovered.completedAttempts());
        assertEquals(runtime.agentRuns(), recovered.agentRuns());
        assertEquals(List.of(decision), recovered.retryDecisions());
        assertEquals(AgentRuntimeState.CURRENT_SCHEMA_VERSION, 2);
    }

    @Test
    void rejectsAValidRevisionThatRewritesAgentRunHistory() throws Exception {
        FileSystemAgentRuntimeStateStore store =
                new FileSystemAgentRuntimeStateStore(storageRoot);
        AgentRuntimeState initial = AgentRuntimeState.initial(GOAL_ID, workItem());
        AgentRuntimeState persisted = initial.beginAgentRun(AGENT_RUN_ID);
        store.create(initial);
        store.update(persisted);

        AgentRuntimeState rewritten = initial
                .beginAgentRun("00000000-0000-0000-0000-000000000507")
                .markReady("00000000-0000-0000-0000-000000000507");

        assertThrows(IOException.class, () -> store.update(rewritten));
        assertEquals(List.of(persisted.agentRun().orElseThrow()),
                store.resolve(GOAL_ID).agentRuns());
    }

    @Test
    void rejectsAValidAppendWhoseDecisionPrefixWasRewritten() throws Exception {
        FileSystemAgentRuntimeStateStore store =
                new FileSystemAgentRuntimeStateStore(storageRoot);
        AgentRuntimeState initial = AgentRuntimeState.initial(GOAL_ID, workItem());
        AgentRuntimeState begun = initial.beginAgentRun(AGENT_RUN_ID);
        AgentRuntimeState ready = begun.markReady(AGENT_RUN_ID);
        AgentRuntimeState executing = ready.acquireLease(
                AGENT_RUN_ID,
                "decision-prefix-owner",
                Instant.parse("2026-07-22T05:00:00Z"),
                Duration.ofMinutes(5));
        AgentRuntimeState awaiting = executing.completeExecution(
                AGENT_RUN_ID,
                "decision-prefix-owner",
                1,
                Instant.parse("2026-07-22T05:01:00Z"));
        AgentRuntimeState pending = awaiting.recordAttemptResult(
                AGENT_RUN_ID,
                resultMessage(VerificationStatus.REJECTED));
        AgentRunRetryDecisionRecord persistedDecision =
                new AgentRunRetryDecisionRecord(
                        AGENT_RUN_ID,
                        1,
                        3,
                        0,
                        0,
                        "c".repeat(64),
                        AgentRunRetryDecision.admitted());
        AgentRuntimeState decided = pending
                .recordRetryDecision(persistedDecision)
                .orElseThrow();
        for (AgentRuntimeState state :
                List.of(initial, begun, ready, executing, awaiting, pending, decided)) {
            if (state.revision() == 0) {
                store.create(state);
            } else {
                store.update(state);
            }
        }

        AgentRunRetryDecisionRecord rewrittenDecision =
                new AgentRunRetryDecisionRecord(
                        AGENT_RUN_ID,
                        1,
                        4,
                        0,
                        0,
                        "d".repeat(64),
                        AgentRunRetryDecision.admitted());
        AgentRuntimeState rewrittenAppend = pending
                .recordRetryDecision(rewrittenDecision)
                .orElseThrow()
                .beginRetryAgentRun(
                        "00000000-0000-0000-0000-000000000507");

        assertThrows(IOException.class, () -> store.update(rewrittenAppend));
        assertEquals(List.of(persistedDecision),
                store.resolve(GOAL_ID).retryDecisions());
    }

    @Test
    void restoresUnexpiredLeaseAndReclaimsItAtExpiry()
            throws Exception {
        Clock issuedClock = Clock.fixed(
                Instant.parse("2026-07-16T21:00:00Z"),
                ZoneOffset.UTC);
        DurableAgentRuntime runtime = DurableAgentRuntime.create(
                GOAL_ID,
                workItem(),
                new FileSystemAgentRuntimeStateStore(storageRoot),
                issuedClock);
        runtime.beginAgentRun(AGENT_RUN_ID);
        runtime.markReady(AGENT_RUN_ID);
        AgentRunLease lease = runtime.acquireLease(
                AGENT_RUN_ID,
                "filesystem-owner-\uD83D\uDE80",
                Duration.ofMinutes(5));

        DurableAgentRuntime beforeExpiry = DurableAgentRuntime.recover(
                GOAL_ID,
                new FileSystemAgentRuntimeStateStore(storageRoot),
                Clock.fixed(
                        Instant.parse("2026-07-16T21:04:59Z"),
                        ZoneOffset.UTC));
        assertEquals(
                Optional.of(lease),
                beforeExpiry.agentRun().orElseThrow().lease());

        DurableAgentRuntime atExpiry = DurableAgentRuntime.recover(
                GOAL_ID,
                new FileSystemAgentRuntimeStateStore(storageRoot),
                Clock.fixed(
                        Instant.parse("2026-07-16T21:05:00Z"),
                        ZoneOffset.UTC));
        assertEquals(
                RuntimeAgentRunStatus.READY,
                atExpiry.agentRun().orElseThrow().status());
        assertTrue(atExpiry.agentRun().orElseThrow().lease().isEmpty());
        assertEquals(4, atExpiry.revision());
        assertEquals(1, atExpiry.lastIssuedFenceToken());
    }

    @Test
    void enforcesCreateAndExactlyOneRevisionUpdates() throws Exception {
        FileSystemAgentRuntimeStateStore store =
                new FileSystemAgentRuntimeStateStore(storageRoot);
        AgentRuntimeState initial =
                AgentRuntimeState.initial(GOAL_ID, workItem());
        store.create(initial);

        assertEquals(
                AgentRuntimeState.CURRENT_SCHEMA_VERSION,
                store.resolve(GOAL_ID).schemaVersion());
        assertThrows(IOException.class, () -> store.create(initial));
        assertThrows(IOException.class, () -> store.update(initial));
    }

    @Test
    void refusesToRemoveAPreviouslyPersistedControlRequest()
            throws Exception {
        FileSystemAgentRuntimeStateStore store =
                new FileSystemAgentRuntimeStateStore(storageRoot);
        DurableAgentRuntime runtime = DurableAgentRuntime.create(
                GOAL_ID, workItem(), store);
        runtime.beginAgentRun(AGENT_RUN_ID);
        runtime.markReady(AGENT_RUN_ID);
        runtime.recordControlRequest(new MessageEnvelope(
                "00000000-0000-0000-0000-000000000506",
                "correlation-runtime-store",
                Optional.of(WORK_MESSAGE_ID),
                "logical-run-runtime-store",
                "runtime-control-test",
                Instant.parse("2026-07-16T18:45:00Z"),
                new ControlPayload(
                        ControlSignal.PAUSE,
                        "retain this request")));
        AgentRuntimeState current = store.resolve(GOAL_ID);
        AgentRuntimeState stripped = new AgentRuntimeState(
                current.schemaVersion(),
                current.revision() + 1,
                current.lastIssuedFenceToken(),
                current.goal(),
                current.agentRun());

        assertThrows(IOException.class, () -> store.update(stripped));
        assertEquals(1, store.resolve(GOAL_ID).controlRequests().size());
    }

    @Test
    void rejectsMissingCorruptTrailingAndUnsupportedState()
            throws Exception {
        FileSystemAgentRuntimeStateStore store =
                new FileSystemAgentRuntimeStateStore(storageRoot);
        store.create(AgentRuntimeState.initial(GOAL_ID, workItem()));
        Path artifact = artifact(GOAL_ID);

        byte[] corrupt = Files.readAllBytes(artifact);
        corrupt[corrupt.length - 1] ^= 0x01;
        Files.write(artifact, corrupt);
        assertThrows(CorruptedAgentRuntimeStateException.class, () ->
                store.resolve(GOAL_ID));

        Files.delete(artifact);
        store.create(AgentRuntimeState.initial(GOAL_ID, workItem()));
        byte[] trailing = Files.readAllBytes(artifact);
        Files.write(
                artifact,
                ByteBuffer.allocate(trailing.length + 1)
                        .put(trailing)
                        .put((byte) 1)
                        .array());
        assertThrows(CorruptedAgentRuntimeStateException.class, () ->
                store.resolve(GOAL_ID));

        Files.delete(artifact);
        store.create(AgentRuntimeState.initial(GOAL_ID, workItem()));
        byte[] unsupported = Files.readAllBytes(artifact);
        ByteBuffer.wrap(unsupported).putInt(
                PAYLOAD_OFFSET,
                AgentRuntimeState.CURRENT_SCHEMA_VERSION + 1);
        replaceDigest(unsupported);
        Files.write(artifact, unsupported);
        CorruptedAgentRuntimeStateException exception = assertThrows(
                CorruptedAgentRuntimeStateException.class,
                () -> store.resolve(GOAL_ID));
        assertTrue(exception.getMessage().contains("version"));

        assertThrows(MissingAgentRuntimeStateException.class, () ->
                store.resolve(
                        "00000000-0000-0000-0000-000000000599"));
    }

    @Test
    void rejectsIntegrityValidStateContainingInvalidUtf8()
            throws Exception {
        FileSystemAgentRuntimeStateStore store =
                new FileSystemAgentRuntimeStateStore(storageRoot);
        store.create(AgentRuntimeState.initial(GOAL_ID, workItem()));
        Path artifact = artifact(GOAL_ID);
        byte[] invalidUtf8 = Files.readAllBytes(artifact);
        int payloadKindOffset =
                PAYLOAD_OFFSET + Integer.BYTES + Integer.BYTES;
        invalidUtf8[payloadKindOffset] = (byte) 0xC3;
        replaceDigest(invalidUtf8);
        Files.write(artifact, invalidUtf8);

        assertThrows(CorruptedAgentRuntimeStateException.class, () ->
                store.resolve(GOAL_ID));
    }

    @Test
    void rejectsIntegrityValidSchemaV1State()
            throws Exception {
        FileSystemAgentRuntimeStateStore store =
                new FileSystemAgentRuntimeStateStore(storageRoot);
        store.create(AgentRuntimeState.initial(GOAL_ID, workItem()));
        Path artifact = artifact(GOAL_ID);
        byte[] previousSchemaV1 = Files.readAllBytes(artifact);
        ByteBuffer.wrap(previousSchemaV1).putInt(PAYLOAD_OFFSET, 1);
        replaceDigest(previousSchemaV1);
        Files.write(artifact, previousSchemaV1);

        assertThrows(CorruptedAgentRuntimeStateException.class, () ->
                store.resolve(GOAL_ID));
    }

    @Test
    void rejectsOversizedAndNonRegularArtifacts() throws Exception {
        FileSystemAgentRuntimeStateStore store =
                new FileSystemAgentRuntimeStateStore(storageRoot);
        Files.createDirectories(storageRoot);
        Path artifact = artifact(GOAL_ID);
        try (FileChannel channel = FileChannel.open(
                artifact,
                StandardOpenOption.CREATE_NEW,
                StandardOpenOption.WRITE)) {
            channel.position(
                    FileSystemAgentRuntimeStateStore.MAX_STATE_BYTES
                            + FileSystemAgentRuntimeStateStore.HEADER_BYTES);
            channel.write(ByteBuffer.wrap(new byte[] {1}));
        }
        assertThrows(CorruptedAgentRuntimeStateException.class, () ->
                store.resolve(GOAL_ID));

        Files.delete(artifact);
        Files.createDirectory(artifact);
        assertThrows(CorruptedAgentRuntimeStateException.class, () ->
                store.resolve(GOAL_ID));
    }

    @Test
    void restoresADeclaredExecutionInputAcrossStoreInstances()
            throws Exception {
        WorkPayload.ExecutionInput input = new WorkPayload.ExecutionInput(
                "docs/target-🚀.md", "e".repeat(64));
        DurableAgentRuntime.create(
                GOAL_ID,
                workItemWithInput(Optional.of(input)),
                new FileSystemAgentRuntimeStateStore(storageRoot),
                Clock.fixed(
                        Instant.parse("2026-07-16T20:00:00Z"),
                        ZoneOffset.UTC));

        DurableAgentRuntime recovered = DurableAgentRuntime.recover(
                GOAL_ID,
                new FileSystemAgentRuntimeStateStore(storageRoot),
                Clock.fixed(
                        Instant.parse("2026-07-16T20:01:00Z"),
                        ZoneOffset.UTC));

        assertEquals(Optional.of(input),
                recovered.goal().workItem().executionInput());
    }

    private Path artifact(String goalId) {
        return storageRoot.resolve(goalId + ".agent-runtime");
    }

    private static WorkItem workItemWithInput(
            Optional<WorkPayload.ExecutionInput> executionInput) {
        WorkItem base = workItem();
        MessageEnvelope message = base.workMessage();
        WorkPayload payload = (WorkPayload) message.payload();
        return new WorkItem(
                base.workItemId(),
                base.requiredCapability(),
                new MessageEnvelope(
                        message.messageId(),
                        message.correlationId(),
                        message.causationId(),
                        message.logicalRunId(),
                        message.producer(),
                        message.occurredAt(),
                        new WorkPayload(
                                payload.taskRevision(),
                                payload.snapshotId(),
                                payload.allowedTools(),
                                executionInput)));
    }

    private static WorkItem workItem() {
        ApprovedTaskRevision revision = new ApprovedTaskRevision(
                "gate-8-durable-goal-agent-run-lifecycle",
                "CURRENT_TASK.md",
                "a".repeat(64));
        return new WorkItem(
                WORK_ITEM_ID,
                "runtime-\uD83D\uDE80",
                new MessageEnvelope(
                        WORK_MESSAGE_ID,
                        "correlation-runtime-store",
                        Optional.empty(),
                        "logical-run-runtime-store",
                        "runtime-store-\uD83D\uDE80",
                        Instant.parse(
                                "2026-07-16T18:30:00.123456789Z"),
                        new WorkPayload(
                                revision,
                                "b".repeat(64),
                                Set.of("read-file", "verify"))));
    }

    private static MessageEnvelope resultMessage(
            VerificationStatus status) {
        return new MessageEnvelope(
                "00000000-0000-0000-0000-000000000505",
                "correlation-runtime-store",
                Optional.of(WORK_MESSAGE_ID),
                "logical-run-runtime-store",
                "runtime-result-\uD83D\uDE80",
                Instant.parse("2026-07-16T19:00:00.123456789Z"),
                new ResultPayload(
                        "gate-8-durable-goal-agent-run-lifecycle",
                        "run-record/runtime-\uD83D\uDE80",
                        status));
    }

    private static void replaceDigest(byte[] envelope) throws Exception {
        ByteBuffer buffer = ByteBuffer.wrap(envelope);
        int magic = buffer.getInt(0);
        long storedAt = buffer.getLong(Integer.BYTES);
        int payloadLength = buffer.getInt(Integer.BYTES + Long.BYTES);
        byte[] payload = new byte[payloadLength];
        System.arraycopy(
                envelope,
                PAYLOAD_OFFSET,
                payload,
                0,
                payloadLength);
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(
                ByteBuffer.allocate(
                                Integer.BYTES
                                        + Long.BYTES
                                        + Integer.BYTES
                                        + payload.length)
                        .putInt(magic)
                        .putLong(storedAt)
                        .putInt(payloadLength)
                        .put(payload)
                        .array());
        System.arraycopy(
                digest,
                0,
                envelope,
                DIGEST_OFFSET,
                digest.length);
    }
}
