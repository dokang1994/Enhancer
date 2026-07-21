package com.enhancer.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.enhancer.bus.MessageEnvelope;
import com.enhancer.bus.WorkPayload;
import com.enhancer.workspace.ApprovedTaskRevision;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FileSystemExternalEffectLedgerIntegrationTest {
    private static final String GOAL_ID =
            "00000000-0000-0000-0000-000000001101";
    private static final String AGENT_RUN_ID =
            "00000000-0000-0000-0000-000000001102";
    private static final String WORK_ITEM_ID =
            "00000000-0000-0000-0000-000000001103";
    private static final String WORK_MESSAGE_ID =
            "00000000-0000-0000-0000-000000001104";
    private static final int DIGEST_OFFSET =
            Integer.BYTES + Long.BYTES + Integer.BYTES;
    private static final int PAYLOAD_OFFSET = DIGEST_OFFSET + 32;

    @TempDir
    Path temporaryRoot;

    @Test
    void recoversPreparedAndTerminalEffectsAcrossFreshStores()
            throws Exception {
        Path runtimeRoot = temporaryRoot.resolve("runtime");
        Path effectRoot = temporaryRoot.resolve("effects");
        Clock clock = Clock.fixed(
                Instant.parse("2026-07-21T10:00:00Z"),
                ZoneOffset.UTC);
        FileSystemAgentRuntimeStateStore runtimeStore =
                new FileSystemAgentRuntimeStateStore(runtimeRoot);
        DurableAgentRuntime runtime = DurableAgentRuntime.create(
                GOAL_ID, workItem(), runtimeStore, clock);
        runtime.beginAgentRun(AGENT_RUN_ID);
        runtime.markReady(AGENT_RUN_ID);
        AgentRunLease lease = runtime.acquireLease(
                AGENT_RUN_ID, "filesystem-effect-🚀", Duration.ofMinutes(5));
        DurableExternalEffectLedger ledger =
                DurableExternalEffectLedger.create(
                        GOAL_ID,
                        runtimeStore,
                        new FileSystemExternalEffectLedgerStore(effectRoot),
                        clock);
        ExternalEffectRequest applied = request(
                "effect-applied", "publish-🚀", "c".repeat(64));
        ExternalEffectRequest unresolved = request(
                "effect-unresolved", "notify-🚧", "d".repeat(64));
        ledger.prepare(applied, lease.ownerId(), lease.fenceToken());
        ledger.recordOutcome(
                applied.idempotencyKey(),
                ExternalEffectStatus.DEDUPLICATED,
                lease.ownerId(),
                lease.fenceToken());
        ledger.prepare(unresolved, lease.ownerId(), lease.fenceToken());

        DurableExternalEffectLedger recovered =
                DurableExternalEffectLedger.recover(
                        GOAL_ID,
                        new FileSystemAgentRuntimeStateStore(runtimeRoot),
                        new FileSystemExternalEffectLedgerStore(effectRoot),
                        clock);

        assertEquals(3, recovered.revision());
        assertEquals(2, recovered.records().size());
        assertEquals(ExternalEffectStatus.DEDUPLICATED,
                recovered.records().get(0).status());
        assertEquals("publish-🚀",
                recovered.records().get(0).request().operationName());
        assertEquals(ExternalEffectStatus.PREPARED,
                recovered.records().get(1).status());
        assertThrows(IllegalArgumentException.class, () ->
                recovered.prepare(
                        unresolved,
                        lease.ownerId(),
                        lease.fenceToken() + 1));
    }

    @Test
    void persistenceFailureLeavesThePreviousFilesystemRevisionAuthoritative()
            throws Exception {
        Path runtimeRoot = temporaryRoot.resolve("runtime-failure");
        Path effectRoot = temporaryRoot.resolve("effects-failure");
        Clock clock = Clock.fixed(
                Instant.parse("2026-07-21T11:00:00Z"),
                ZoneOffset.UTC);
        FileSystemAgentRuntimeStateStore runtimeStore =
                new FileSystemAgentRuntimeStateStore(runtimeRoot);
        DurableAgentRuntime runtime = DurableAgentRuntime.create(
                GOAL_ID, workItem(), runtimeStore, clock);
        runtime.beginAgentRun(AGENT_RUN_ID);
        runtime.markReady(AGENT_RUN_ID);
        AgentRunLease lease = runtime.acquireLease(
                AGENT_RUN_ID, "filesystem-effect", Duration.ofMinutes(5));
        FailingUpdateEffectStore effectStore = new FailingUpdateEffectStore(
                new FileSystemExternalEffectLedgerStore(effectRoot));
        DurableExternalEffectLedger ledger = DurableExternalEffectLedger.create(
                GOAL_ID, runtimeStore, effectStore, clock);
        effectStore.failNextUpdate();

        assertThrows(IOException.class, () -> ledger.prepare(
                request("effect-failure", "publish", "e".repeat(64)),
                lease.ownerId(),
                lease.fenceToken()));
        assertEquals(0, ledger.revision());
        assertTrue(ledger.records().isEmpty());
        assertEquals(
                0,
                new FileSystemExternalEffectLedgerStore(effectRoot)
                        .resolve(GOAL_ID)
                        .revision());
    }

    @Test
    void rejectsMissingCorruptTrailingOversizedAndUnsupportedState()
            throws Exception {
        Path effectRoot = temporaryRoot.resolve("effects-invalid");
        FileSystemExternalEffectLedgerStore store =
                new FileSystemExternalEffectLedgerStore(effectRoot);
        assertThrows(MissingExternalEffectLedgerException.class, () ->
                store.resolve(GOAL_ID));

        store.create(ExternalEffectLedgerState.initial(GOAL_ID));
        Path artifact = effectRoot.resolve(GOAL_ID + ".external-effects");
        byte[] corrupt = Files.readAllBytes(artifact);
        corrupt[corrupt.length - 1] ^= 1;
        Files.write(artifact, corrupt);
        assertThrows(CorruptedExternalEffectLedgerException.class, () ->
                store.resolve(GOAL_ID));

        Files.delete(artifact);
        store.create(ExternalEffectLedgerState.initial(GOAL_ID));
        byte[] trailing = Files.readAllBytes(artifact);
        Files.write(artifact, ByteBuffer.allocate(trailing.length + 1)
                .put(trailing)
                .put((byte) 1)
                .array());
        assertThrows(CorruptedExternalEffectLedgerException.class, () ->
                store.resolve(GOAL_ID));

        Files.delete(artifact);
        store.create(ExternalEffectLedgerState.initial(GOAL_ID));
        byte[] unsupported = Files.readAllBytes(artifact);
        ByteBuffer.wrap(unsupported).putInt(
                PAYLOAD_OFFSET,
                ExternalEffectLedgerState.CURRENT_SCHEMA_VERSION + 1);
        replaceDigest(unsupported);
        Files.write(artifact, unsupported);
        CorruptedExternalEffectLedgerException exception = assertThrows(
                CorruptedExternalEffectLedgerException.class,
                () -> store.resolve(GOAL_ID));
        assertTrue(exception.getMessage().contains("version"));

        Files.write(artifact, new byte[
                FileSystemExternalEffectLedgerStore.HEADER_BYTES
                        + FileSystemExternalEffectLedgerStore.MAX_STATE_BYTES + 1]);
        assertThrows(CorruptedExternalEffectLedgerException.class, () ->
                store.resolve(GOAL_ID));
    }

    @Test
    void rejectsNonMonotonicStoreUpdates() throws Exception {
        FileSystemExternalEffectLedgerStore store =
                new FileSystemExternalEffectLedgerStore(
                        temporaryRoot.resolve("effects-revision"));
        ExternalEffectLedgerState initial =
                ExternalEffectLedgerState.initial(GOAL_ID);
        store.create(initial);

        assertThrows(IOException.class, () -> store.create(initial));
        assertThrows(IOException.class, () -> store.update(initial));
    }

    @Test
    void rejectsHistoryRewriteEvenWhenRevisionAdvancesByOne()
            throws Exception {
        FileSystemExternalEffectLedgerStore store =
                new FileSystemExternalEffectLedgerStore(
                        temporaryRoot.resolve("effects-history"));
        ExternalEffectRequest request = request(
                "effect-history", "publish", "f".repeat(64));
        ExternalEffectRecord prepared = new ExternalEffectRecord(
                request, ExternalEffectStatus.PREPARED);
        ExternalEffectLedgerState invalidInitial =
                new ExternalEffectLedgerState(
                        ExternalEffectLedgerState.CURRENT_SCHEMA_VERSION,
                        GOAL_ID,
                        0,
                        java.util.List.of(prepared));
        assertThrows(IOException.class, () -> store.create(invalidInitial));

        store.create(ExternalEffectLedgerState.initial(GOAL_ID));
        ExternalEffectLedgerState preparedState =
                new ExternalEffectLedgerState(
                        ExternalEffectLedgerState.CURRENT_SCHEMA_VERSION,
                        GOAL_ID,
                        1,
                        java.util.List.of(prepared));
        store.update(preparedState);
        ExternalEffectLedgerState removedHistory =
                new ExternalEffectLedgerState(
                        ExternalEffectLedgerState.CURRENT_SCHEMA_VERSION,
                        GOAL_ID,
                        2,
                        java.util.List.of());
        assertThrows(IOException.class, () -> store.update(removedHistory));

        ExternalEffectLedgerState appliedState =
                new ExternalEffectLedgerState(
                        ExternalEffectLedgerState.CURRENT_SCHEMA_VERSION,
                        GOAL_ID,
                        2,
                        java.util.List.of(new ExternalEffectRecord(
                                request, ExternalEffectStatus.APPLIED)));
        store.update(appliedState);
        ExternalEffectLedgerState rewrittenOutcome =
                new ExternalEffectLedgerState(
                        ExternalEffectLedgerState.CURRENT_SCHEMA_VERSION,
                        GOAL_ID,
                        3,
                        java.util.List.of(new ExternalEffectRecord(
                                request, ExternalEffectStatus.COMPENSATED)));
        assertThrows(IOException.class, () -> store.update(rewrittenOutcome));
    }

    private static ExternalEffectRequest request(
            String key,
            String operation,
            String digest) {
        return new ExternalEffectRequest(
                key,
                GOAL_ID,
                AGENT_RUN_ID,
                WORK_ITEM_ID,
                operation,
                digest);
    }

    private static WorkItem workItem() {
        return new WorkItem(
                WORK_ITEM_ID,
                "filesystem-effect-worker",
                new MessageEnvelope(
                        WORK_MESSAGE_ID,
                        "correlation-filesystem-effects",
                        Optional.empty(),
                        "logical-run-filesystem-effects",
                        "filesystem-effect-test",
                        Instant.parse("2026-07-21T09:00:00Z"),
                        new WorkPayload(
                                new ApprovedTaskRevision(
                                        "persist-fence-checked-external-effect-ledger",
                                        "CURRENT_TASK.md",
                                        "a".repeat(64)),
                                "b".repeat(64),
                                Set.of("read-file"))));
    }

    private static void replaceDigest(byte[] envelope) throws Exception {
        ByteBuffer buffer = ByteBuffer.wrap(envelope);
        int magic = buffer.getInt(0);
        long storedAt = buffer.getLong(Integer.BYTES);
        int payloadLength = buffer.getInt(Integer.BYTES + Long.BYTES);
        byte[] payload = new byte[payloadLength];
        System.arraycopy(envelope, PAYLOAD_OFFSET, payload, 0, payloadLength);
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(
                ByteBuffer.allocate(
                        Integer.BYTES + Long.BYTES + Integer.BYTES + payload.length)
                        .putInt(magic)
                        .putLong(storedAt)
                        .putInt(payloadLength)
                        .put(payload)
                        .array());
        System.arraycopy(digest, 0, envelope, DIGEST_OFFSET, digest.length);
    }

    private static final class FailingUpdateEffectStore
            implements ExternalEffectLedgerStore {
        private final ExternalEffectLedgerStore delegate;
        private boolean failNextUpdate;

        private FailingUpdateEffectStore(ExternalEffectLedgerStore delegate) {
            this.delegate = delegate;
        }

        @Override
        public void create(ExternalEffectLedgerState initialState)
                throws IOException {
            delegate.create(initialState);
        }

        @Override
        public void update(ExternalEffectLedgerState nextState)
                throws IOException {
            if (failNextUpdate) {
                failNextUpdate = false;
                throw new IOException("simulated filesystem publication failure");
            }
            delegate.update(nextState);
        }

        @Override
        public ExternalEffectLedgerState resolve(String goalId)
                throws IOException {
            return delegate.resolve(goalId);
        }

        void failNextUpdate() {
            failNextUpdate = true;
        }
    }
}
