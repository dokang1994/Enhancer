package com.enhancer.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.enhancer.kernel.VerificationCode;
import com.enhancer.kernel.VerificationDecision;
import com.enhancer.kernel.VerificationStatus;
import com.enhancer.loop.AgentLoopStopReason;
import com.enhancer.model.DeterministicFakeModelGateway;
import com.enhancer.run.ModelRunRecord;
import com.enhancer.run.ModelRunRecordStore;
import com.enhancer.run.FileSystemRunRecordStore;
import com.enhancer.run.PolicyDecisionStatus;
import com.enhancer.run.StoredRunRecord;
import com.enhancer.tool.ToolFailureCode;
import com.enhancer.tool.ToolResult;
import com.enhancer.tool.ToolResultStatus;
import com.enhancer.tool.VerificationEvidence;
import com.enhancer.verification.IndependentVerifier;
import com.enhancer.verification.VerificationRequest;
import java.time.Clock;
import java.nio.file.Path;
import java.nio.file.Files;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.OptionalInt;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;

class ModelRunRecordFinalizerTest {
    private static final Instant NOW = Instant.parse("2026-09-03T10:11:12.123456789Z");

    @TempDir
    Path temporaryRoot;

    @Test
    void verifiesAndPersistsOnlyOneExactV2LifecycleAtTheDeterministicIdentity()
            throws Exception {
        ModelAttemptTestFixture.Fixture fixture = fixture();
        DeterministicFakeReturnedOutcomeTool tool = successfulTool(fixture);
        ToolResult toolResult = tool.execute(
                tool.request(), fixture.preparation().executionPolicy());
        IndependentVerifier verifier = mock(IndependentVerifier.class);
        when(verifier.verify(any())).thenReturn(
                VerificationDecision.verified("exact response matched"));
        ModelRunRecordStore store = mock(ModelRunRecordStore.class);
        String recordId = AgentRunRecordIdentity.recordId(
                ModelAttemptTestFixture.GOAL_ID, ModelAttemptTestFixture.AGENT_RUN_ID);
        StoredRunRecord stored = stored(recordId);
        when(store.persistModel(eq(recordId), any())).thenReturn(stored);
        ModelRunRecordFinalizer finalizer = new ModelRunRecordFinalizer(
                verifier, store, Clock.fixed(NOW, ZoneOffset.UTC));

        ModelRunRecordFinalizer.Published published = finalizer.finalizeRun(
                ModelAttemptTestFixture.GOAL_ID,
                ModelAttemptTestFixture.AGENT_RUN_ID,
                fixture.workItem(),
                fixture.preparation(),
                ModelAttemptTestFixture.requireReady(fixture),
                tool.request(),
                toolResult);

        ArgumentCaptor<ModelRunRecord> record = ArgumentCaptor.forClass(ModelRunRecord.class);
        verify(store).persistModel(eq(recordId), record.capture());
        verify(store, never()).persistModel(any(ModelRunRecord.class));
        verify(verifier).verify(any(VerificationRequest.class));
        assertSame(record.getValue(), published.record());
        assertSame(stored, published.storedRecord());
        assertSame(fixture.preparation().profiledRequest().request(),
                record.getValue().modelRequest());
        assertEquals(ModelAttemptTestFixture.LOGICAL_RUN_ID,
                record.getValue().lifecycleRecord().logicalRunId());
        assertEquals(NOW.truncatedTo(ChronoUnit.MILLIS),
                record.getValue().lifecycleRecord().recordedAt());
        assertEquals(PolicyDecisionStatus.ALLOWED,
                record.getValue().lifecycleRecord().policyDecision().status());
        assertEquals(1, record.getValue().lifecycleRecord().iterations());
        assertEquals(VerificationStatus.VERIFIED,
                record.getValue().lifecycleRecord().verification().status());
        assertEquals(AgentLoopStopReason.AWAITING_VERIFICATION,
                record.getValue().lifecycleRecord().workerStopReason());
        assertEquals(AgentLoopStopReason.COMPLETED,
                record.getValue().lifecycleRecord().finalStopReason());
        assertEquals(fixture.workItem().modelExecutionInput().orElseThrow()
                        .expectedResponseSha256(),
                record.getValue().lifecycleRecord().expectedContentSha256().orElseThrow());
    }

    @Test
    void mapsRejectedAndUnverifiedSuccessWithoutTreatingEitherAsCompletion()
            throws Exception {
        assertNonVerifiedSuccess(VerificationDecision.rejected(
                VerificationCode.CONTENT_MISMATCH, "mismatch"));
        assertNonVerifiedSuccess(VerificationDecision.unverified(
                VerificationCode.EVIDENCE_UNAVAILABLE, "unavailable"));
    }

    @Test
    void failedToolSkipsVerifierAndPersistsNotPerformedWithoutNestedDigest()
            throws Exception {
        ModelAttemptTestFixture.Fixture fixture = fixture();
        DeterministicFakeReturnedOutcomeTool tool = successfulTool(fixture);
        ToolResult failure = new ToolResult(
                "model-invoke",
                ToolResultStatus.FAILURE,
                OptionalInt.empty(),
                Optional.of(ToolFailureCode.EXECUTION_FAILED),
                VerificationEvidence.capture(
                        "Model result materialization failed",
                        "tool-failure-code=EXECUTION_FAILED",
                        Optional.empty()));
        IndependentVerifier verifier = mock(IndependentVerifier.class);
        ModelRunRecordStore store = mock(ModelRunRecordStore.class);
        String recordId = AgentRunRecordIdentity.recordId(
                ModelAttemptTestFixture.GOAL_ID, ModelAttemptTestFixture.AGENT_RUN_ID);
        when(store.persistModel(eq(recordId), any())).thenReturn(stored(recordId));
        ModelRunRecordFinalizer finalizer = new ModelRunRecordFinalizer(
                verifier, store, Clock.fixed(NOW, ZoneOffset.UTC));

        ModelRunRecordFinalizer.Published published = finalizer.finalizeRun(
                ModelAttemptTestFixture.GOAL_ID,
                ModelAttemptTestFixture.AGENT_RUN_ID,
                fixture.workItem(),
                fixture.preparation(),
                ModelAttemptTestFixture.requireReady(fixture),
                tool.request(),
                failure);

        verifyNoInteractions(verifier);
        assertEquals(VerificationStatus.NOT_PERFORMED,
                published.record().lifecycleRecord().verification().status());
        assertFalse(published.record().lifecycleRecord().expectedContentSha256().isPresent());
        assertEquals(AgentLoopStopReason.FAILED,
                published.record().lifecycleRecord().workerStopReason());
        assertEquals(AgentLoopStopReason.FAILED,
                published.record().lifecycleRecord().finalStopReason());
    }

    @Test
    void refusesNotPerformedVerificationForSuccessBeforePersistence() throws Exception {
        ModelAttemptTestFixture.Fixture fixture = fixture();
        DeterministicFakeReturnedOutcomeTool tool = successfulTool(fixture);
        ToolResult result = tool.execute(
                tool.request(), fixture.preparation().executionPolicy());
        IndependentVerifier verifier = request -> VerificationDecision.notPerformed("invalid");
        ModelRunRecordStore store = mock(ModelRunRecordStore.class);
        ModelRunRecordFinalizer finalizer = new ModelRunRecordFinalizer(
                verifier, store, Clock.fixed(NOW, ZoneOffset.UTC));

        assertThrows(IllegalStateException.class, () -> finalizer.finalizeRun(
                ModelAttemptTestFixture.GOAL_ID,
                ModelAttemptTestFixture.AGENT_RUN_ID,
                fixture.workItem(),
                fixture.preparation(),
                ModelAttemptTestFixture.requireReady(fixture),
                tool.request(),
                result));

        verifyNoInteractions(store);
    }

    @Test
    void exactV2ReplayPreservesThePublishedArtifact() throws Exception {
        ModelAttemptTestFixture.Fixture fixture = fixture();
        DeterministicFakeReturnedOutcomeTool tool = successfulTool(fixture);
        ToolResult result = tool.execute(
                tool.request(), fixture.preparation().executionPolicy());
        Path recordRoot = temporaryRoot.resolve("records");
        FileSystemRunRecordStore store = new FileSystemRunRecordStore(recordRoot);
        ModelRunRecordFinalizer finalizer = new ModelRunRecordFinalizer(
                request -> VerificationDecision.verified("exact response matched"),
                store,
                Clock.fixed(NOW, ZoneOffset.UTC));

        ModelRunRecordFinalizer.Published first = finalizer.finalizeRun(
                ModelAttemptTestFixture.GOAL_ID,
                ModelAttemptTestFixture.AGENT_RUN_ID,
                fixture.workItem(),
                fixture.preparation(),
                ModelAttemptTestFixture.requireReady(fixture),
                tool.request(),
                result);
        Path artifact = recordRoot.resolve(first.storedRecord().recordId() + ".run-record");
        byte[] firstBytes = Files.readAllBytes(artifact);
        var firstModified = Files.getLastModifiedTime(artifact);

        ModelRunRecordFinalizer.Published replay = finalizer.finalizeRun(
                ModelAttemptTestFixture.GOAL_ID,
                ModelAttemptTestFixture.AGENT_RUN_ID,
                fixture.workItem(),
                fixture.preparation(),
                ModelAttemptTestFixture.requireReady(fixture),
                tool.request(),
                result);

        assertEquals(first, replay);
        assertEquals(firstModified, Files.getLastModifiedTime(artifact));
        assertEquals(java.util.Arrays.toString(firstBytes),
                java.util.Arrays.toString(Files.readAllBytes(artifact)));
        assertEquals(first.record(),
                store.resolveModel(first.storedRecord().reference()).record());
    }

    private void assertNonVerifiedSuccess(VerificationDecision decision) throws Exception {
        ModelAttemptTestFixture.Fixture fixture = fixture();
        DeterministicFakeReturnedOutcomeTool tool = successfulTool(fixture);
        ToolResult result = tool.execute(
                tool.request(), fixture.preparation().executionPolicy());
        ModelRunRecordStore store = mock(ModelRunRecordStore.class);
        String recordId = AgentRunRecordIdentity.recordId(
                ModelAttemptTestFixture.GOAL_ID, ModelAttemptTestFixture.AGENT_RUN_ID);
        when(store.persistModel(eq(recordId), any())).thenReturn(stored(recordId));
        ModelRunRecordFinalizer finalizer = new ModelRunRecordFinalizer(
                request -> decision, store, Clock.fixed(NOW, ZoneOffset.UTC));

        ModelRunRecordFinalizer.Published published = finalizer.finalizeRun(
                ModelAttemptTestFixture.GOAL_ID,
                ModelAttemptTestFixture.AGENT_RUN_ID,
                fixture.workItem(),
                fixture.preparation(),
                ModelAttemptTestFixture.requireReady(fixture),
                tool.request(),
                result);

        assertEquals(decision, published.record().lifecycleRecord().verification());
        assertEquals(AgentLoopStopReason.AWAITING_VERIFICATION,
                published.record().lifecycleRecord().workerStopReason());
        assertEquals(AgentLoopStopReason.AWAITING_VERIFICATION,
                published.record().lifecycleRecord().finalStopReason());
    }

    private ModelAttemptTestFixture.Fixture fixture() {
        String prompt = "finalizer prompt";
        return ModelAttemptTestFixture.admitted(
                Path.of("."),
                prompt,
                ModelAttemptTestFixture.sha256(
                        ModelAttemptTestFixture.deterministicResponse(prompt)),
                mock(DeterministicFakeModelGateway.class));
    }

    private DeterministicFakeReturnedOutcomeTool successfulTool(
            ModelAttemptTestFixture.Fixture fixture) throws Exception {
        DeterministicFakeModelGateway gateway = fixture.ready().suitable().candidate().gateway();
        when(gateway.invoke(same(fixture.preparation().profiledRequest().request())))
                .thenReturn(new DeterministicFakeModelGateway().invoke(
                        fixture.preparation().profiledRequest().request()));
        var returned = new com.enhancer.model.DeterministicFakeExactRequestInvoker()
                .invoke(ModelAttemptTestFixture.requireReady(fixture));
        return new DeterministicFakeReturnedOutcomeTool(
                returned,
                ModelAttemptTestFixture.TARGET_PATH,
                mock(com.enhancer.tool.EvidenceRunNamespaceStore.class));
    }

    private StoredRunRecord stored(String recordId) {
        return new StoredRunRecord(
                recordId,
                "run-record/" + recordId,
                NOW,
                100,
                "d".repeat(64));
    }
}
