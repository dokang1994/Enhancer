package com.enhancer.cli;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.enhancer.bus.MessageEnvelope;
import com.enhancer.bus.WorkPayload;
import com.enhancer.runtime.AgentRunLease;
import com.enhancer.runtime.DurableAgentRuntime;
import com.enhancer.runtime.DurableExternalEffectLedger;
import com.enhancer.runtime.DurableSingleWorkerSchedulerQueue;
import com.enhancer.runtime.ExternalEffectOutcomeEvidence;
import com.enhancer.runtime.ExternalEffectRequest;
import com.enhancer.runtime.ExternalEffectStatus;
import com.enhancer.runtime.FileSystemAgentRuntimeStateStore;
import com.enhancer.runtime.FileSystemExternalEffectLedgerStore;
import com.enhancer.runtime.FileSystemPendingFinalizationStore;
import com.enhancer.runtime.FileSystemSchedulerQueueStore;
import com.enhancer.runtime.PendingFinalization;
import com.enhancer.runtime.QueuedWork;
import com.enhancer.runtime.WorkItem;
import com.enhancer.tool.EvidenceStoragePolicy;
import com.enhancer.tool.FileSystemEvidenceStore;
import com.enhancer.tool.StoredEvidence;
import com.enhancer.workspace.ApprovedTaskRevision;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class EnhancerCliSchedulerExternalEffectStatusIntegrationTest {
    private static final String QUEUE_ID =
            "00000000-0000-0000-0000-000000000a81";
    private static final String WORK_ITEM_ID =
            "00000000-0000-0000-0000-000000000a82";
    private static final String GOAL_ID =
            "00000000-0000-0000-0000-000000000a83";
    private static final String AGENT_RUN_ID =
            "00000000-0000-0000-0000-000000000a84";
    private static final String OWNER = "effect-status-owner";
    private static final Instant NOW =
            Instant.parse("2026-07-23T11:00:00Z");
    private static final Clock CLOCK =
            Clock.fixed(NOW, ZoneOffset.UTC);

    @TempDir
    Path temporaryRoot;

    @Test
    void reportsNoCorrelatedGoalWithoutCreatingOrReadingEffectRoots()
            throws Exception {
        Roots roots = roots("none");
        DurableSingleWorkerSchedulerQueue.create(
                QUEUE_ID,
                16,
                new FileSystemSchedulerQueueStore(roots.queue()));
        Path queueArtifact =
                roots.queue().resolve(QUEUE_ID + ".scheduler-queue");
        byte[] queueBefore = Files.readAllBytes(queueArtifact);

        Captured result = execute(roots, 8);

        assertEquals(0, result.exitCode());
        assertEquals("NO_CORRELATED_GOAL",
                value(result.stdout(), "status"));
        assertEquals("false",
                value(result.stdout(), "effectLedgerPresent"));
        assertEquals("0", value(result.stdout(), "totalEffects"));
        assertArrayEquals(
                queueBefore, Files.readAllBytes(queueArtifact));
        assertFalse(Files.exists(roots.runtime()));
        assertFalse(Files.exists(roots.checkpoint()));
        assertFalse(Files.exists(roots.records()));
        assertFalse(Files.exists(roots.effects()));
        assertFalse(Files.exists(roots.evidence()));
    }

    @Test
    void distinguishesIntentRuntimeAndEmptyLedgerPrefixes()
            throws Exception {
        Roots roots = roots("prefixes");
        WorkItem workItem = workItem();
        DurableSingleWorkerSchedulerQueue queue =
                DurableSingleWorkerSchedulerQueue.create(
                        QUEUE_ID,
                        16,
                        new FileSystemSchedulerQueueStore(roots.queue()));
        queue.enqueue(new QueuedWork(workItem, List.of()));
        new FileSystemPendingFinalizationStore(
                roots.checkpoint()).record(new PendingFinalization(
                        GOAL_ID,
                        AGENT_RUN_ID,
                        Optional.empty()));

        Captured intent = execute(roots, 8);

        assertEquals(0, intent.exitCode());
        assertEquals("LEDGER_NOT_RECORDED",
                value(intent.stdout(), "status"));
        assertFalse(Files.exists(roots.effects()));
        assertFalse(Files.exists(roots.evidence()));

        DurableAgentRuntime.create(
                GOAL_ID,
                workItem,
                new FileSystemAgentRuntimeStateStore(roots.runtime()),
                CLOCK);
        Captured creation = execute(roots, 8);

        assertEquals(0, creation.exitCode());
        assertEquals("LEDGER_CREATION_PENDING",
                value(creation.stdout(), "status"));
        assertFalse(Files.exists(roots.effects()));

        new FileSystemExternalEffectLedgerStore(
                roots.effects()).create(
                        com.enhancer.runtime.ExternalEffectLedgerState
                                .initial(GOAL_ID));
        byte[] ledgerBefore = Files.readAllBytes(
                roots.effects().resolve(
                        GOAL_ID + ".external-effects"));
        Captured empty = execute(roots, 8);

        assertEquals(0, empty.exitCode());
        assertEquals("EMPTY_LEDGER",
                value(empty.stdout(), "status"));
        assertEquals("0", value(empty.stdout(), "ledgerRevision"));
        assertArrayEquals(
                ledgerBefore,
                Files.readAllBytes(roots.effects().resolve(
                        GOAL_ID + ".external-effects")));
        assertFalse(Files.exists(roots.evidence()));
    }

    @Test
    void reportsPreparedAndEvidenceVerifiedUserRecoveryWithoutMutation()
            throws Exception {
        ExecutionFixture fixture = executingFixture("outcomes");
        ExternalEffectRequest request = request("effect-key");
        fixture.ledger().prepare(
                request, OWNER, fixture.lease().fenceToken());
        Path ledgerArtifact = fixture.roots().effects().resolve(
                GOAL_ID + ".external-effects");
        byte[] preparedBefore = Files.readAllBytes(ledgerArtifact);

        Captured prepared = execute(fixture.roots(), 8);

        assertEquals(0, prepared.exitCode());
        assertEquals("PREPARED_EFFECT_REQUIRES_RECOVERY",
                value(prepared.stdout(), "status"));
        assertEquals("1", value(prepared.stdout(), "preparedEffects"));
        assertEquals("effect-key,PREPARED," + AGENT_RUN_ID,
                value(prepared.stdout(), "effect.1"));
        assertArrayEquals(
                preparedBefore, Files.readAllBytes(ledgerArtifact));

        FileSystemEvidenceStore evidenceStore =
                evidenceStore(fixture.roots());
        String runId = evidenceStore.createRun();
        StoredEvidence stored =
                evidenceStore.persist(runId, "redacted outcome");
        fixture.ledger().recordOutcome(
                "effect-key",
                ExternalEffectStatus.REQUIRES_USER_RECOVERY,
                new ExternalEffectOutcomeEvidence(
                        stored.reference(), stored.sha256()),
                OWNER,
                fixture.lease().fenceToken());
        byte[] terminalBefore = Files.readAllBytes(ledgerArtifact);
        Path evidenceArtifact = fixture.roots().evidence()
                .resolve(stored.runId())
                .resolve(stored.evidenceId() + ".evidence");
        byte[] evidenceBefore = Files.readAllBytes(evidenceArtifact);

        Captured terminal = execute(fixture.roots(), 8);

        assertEquals(0, terminal.exitCode());
        assertEquals("USER_RECOVERY_REQUIRED",
                value(terminal.stdout(), "status"));
        assertEquals("1",
                value(terminal.stdout(), "userRecoveryEffects"));
        assertEquals("1",
                value(terminal.stdout(), "verifiedTerminalEvidence"));
        assertArrayEquals(
                terminalBefore, Files.readAllBytes(ledgerArtifact));
        assertArrayEquals(
                evidenceBefore, Files.readAllBytes(evidenceArtifact));
        assertFalse(terminal.stdout().contains("redacted outcome"));
    }

    @Test
    void corruptTerminalEvidenceFailsClosedWithoutChangingArtifacts()
            throws Exception {
        ExecutionFixture fixture = executingFixture("corrupt");
        ExternalEffectRequest request = request("corrupt-key");
        fixture.ledger().prepare(
                request, OWNER, fixture.lease().fenceToken());
        FileSystemEvidenceStore evidenceStore =
                evidenceStore(fixture.roots());
        StoredEvidence stored = evidenceStore.persist(
                evidenceStore.createRun(), "outcome");
        fixture.ledger().recordOutcome(
                "corrupt-key",
                ExternalEffectStatus.APPLIED,
                new ExternalEffectOutcomeEvidence(
                        stored.reference(), stored.sha256()),
                OWNER,
                fixture.lease().fenceToken());
        Path evidenceArtifact = fixture.roots().evidence()
                .resolve(stored.runId())
                .resolve(stored.evidenceId() + ".evidence");
        byte[] corrupted = Files.readAllBytes(evidenceArtifact);
        corrupted[corrupted.length - 1] ^= 1;
        Files.write(evidenceArtifact, corrupted);
        byte[] ledgerBefore = Files.readAllBytes(
                fixture.roots().effects().resolve(
                        GOAL_ID + ".external-effects"));

        Captured result = execute(fixture.roots(), 8);

        assertEquals(70, result.exitCode());
        assertEquals("", result.stdout());
        assertTrue(result.stderr().contains("status=ERROR"));
        assertArrayEquals(corrupted, Files.readAllBytes(evidenceArtifact));
        assertArrayEquals(
                ledgerBefore,
                Files.readAllBytes(fixture.roots().effects().resolve(
                        GOAL_ID + ".external-effects")));
    }

    @Test
    void maximumLedgerPrefixRemainsWithinTheCliOutputBound()
            throws Exception {
        ExecutionFixture fixture = executingFixture("bounded");
        for (int index = 0; index < 8; index++) {
            fixture.ledger().prepare(
                    request("effect-" + index + "-" + "x".repeat(220)),
                    OWNER,
                    fixture.lease().fenceToken());
        }

        Captured result = execute(fixture.roots(), 8);

        assertEquals(0, result.exitCode());
        assertEquals("8", value(result.stdout(), "returnedEffects"));
        assertTrue(result.stdout().length()
                <= EnhancerCli.MAX_DIAGNOSTIC_CHARACTERS);
    }

    private ExecutionFixture executingFixture(String name)
            throws Exception {
        Roots roots = roots(name);
        WorkItem workItem = workItem();
        DurableSingleWorkerSchedulerQueue queue =
                DurableSingleWorkerSchedulerQueue.create(
                        QUEUE_ID,
                        16,
                        new FileSystemSchedulerQueueStore(roots.queue()));
        queue.enqueue(new QueuedWork(workItem, List.of()));
        FileSystemAgentRuntimeStateStore runtimeStore =
                new FileSystemAgentRuntimeStateStore(roots.runtime());
        DurableAgentRuntime runtime = DurableAgentRuntime.create(
                GOAL_ID, workItem, runtimeStore, CLOCK);
        runtime.beginAgentRun(AGENT_RUN_ID);
        runtime.markReady(AGENT_RUN_ID);
        AgentRunLease lease = runtime.acquireLease(
                AGENT_RUN_ID, OWNER, Duration.ofMinutes(5));
        new FileSystemPendingFinalizationStore(
                roots.checkpoint()).record(new PendingFinalization(
                        GOAL_ID,
                        AGENT_RUN_ID,
                        Optional.empty()));
        DurableExternalEffectLedger ledger =
                DurableExternalEffectLedger.create(
                        GOAL_ID,
                        runtimeStore,
                        new FileSystemExternalEffectLedgerStore(
                                roots.effects()),
                        CLOCK);
        return new ExecutionFixture(roots, ledger, lease);
    }

    private ExternalEffectRequest request(String key) {
        return new ExternalEffectRequest(
                key,
                GOAL_ID,
                AGENT_RUN_ID,
                WORK_ITEM_ID,
                "deterministic-adapter",
                "operation",
                "c".repeat(64));
    }

    private WorkItem workItem() {
        return new WorkItem(
                WORK_ITEM_ID,
                "read-file-worker",
                new MessageEnvelope(
                        "00000000-0000-0000-0000-000000000a85",
                        "effect-status-correlation",
                        Optional.empty(),
                        "effect-status-run",
                        "effect-status-test",
                        NOW,
                        new WorkPayload(
                                new ApprovedTaskRevision(
                                        "effect-status-test",
                                        "CURRENT_TASK.md",
                                        "a".repeat(64)),
                                "b".repeat(64),
                                Set.of("read-file"))));
    }

    private FileSystemEvidenceStore evidenceStore(Roots roots) {
        return new FileSystemEvidenceStore(
                roots.evidence(), new EvidenceStoragePolicy(4096));
    }

    private Captured execute(Roots roots, int limit) {
        ByteArrayOutputStream stdoutBytes =
                new ByteArrayOutputStream();
        ByteArrayOutputStream stderrBytes =
                new ByteArrayOutputStream();
        int exitCode = new EnhancerCli().execute(
                new String[] {
                        "scheduler-external-effect-status",
                        "--queue-root", roots.queue().toString(),
                        "--queue-id", QUEUE_ID,
                        "--runtime-root", roots.runtime().toString(),
                        "--cycle-checkpoint-root",
                        roots.checkpoint().toString(),
                        "--run-record-root", roots.records().toString(),
                        "--external-effect-root",
                        roots.effects().toString(),
                        "--evidence-root", roots.evidence().toString(),
                        "--limit", Integer.toString(limit)
                },
                new PrintStream(
                        stdoutBytes, true, StandardCharsets.UTF_8),
                new PrintStream(
                        stderrBytes, true, StandardCharsets.UTF_8));
        return new Captured(
                exitCode,
                stdoutBytes.toString(StandardCharsets.UTF_8),
                stderrBytes.toString(StandardCharsets.UTF_8));
    }

    private Roots roots(String name) {
        Path base = temporaryRoot.resolve(name);
        return new Roots(
                base.resolve("queue"),
                base.resolve("runtime"),
                base.resolve("checkpoint"),
                base.resolve("records"),
                base.resolve("effects"),
                base.resolve("evidence"));
    }

    private String value(String output, String key) {
        String prefix = key + "=";
        return output.lines()
                .filter(line -> line.startsWith(prefix))
                .map(line -> line.substring(prefix.length()))
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        "missing output key " + key + " in " + output));
    }

    private record Roots(
            Path queue,
            Path runtime,
            Path checkpoint,
            Path records,
            Path effects,
            Path evidence) {
    }

    private record ExecutionFixture(
            Roots roots,
            DurableExternalEffectLedger ledger,
            AgentRunLease lease) {
    }

    private record Captured(
            int exitCode,
            String stdout,
            String stderr) {
    }
}
