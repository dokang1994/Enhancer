package com.enhancer.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.enhancer.kernel.VerificationStatus;
import com.enhancer.model.DeterministicFakeModelGateway;
import com.enhancer.model.ModelFailureCode;
import com.enhancer.model.ModelInvocationAdmissionDecision;
import com.enhancer.model.ModelInvocationRejectionReason;
import com.enhancer.model.ModelResponse;
import com.enhancer.model.ModelTokenBudget;
import com.enhancer.run.ModelRunRecord;
import com.enhancer.run.ModelRunRecordStore;
import com.enhancer.run.ResolvedModelRunRecord;
import com.enhancer.run.StoredRunRecord;
import com.enhancer.tool.CancellationToken;
import com.enhancer.tool.EvidenceRunNamespaceStore;
import com.enhancer.tool.EvidenceStoragePolicy;
import com.enhancer.tool.ResolvedEvidence;
import com.enhancer.tool.StoredEvidence;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;

class DeterministicFakeModelAttemptPipelineTest {
    @TempDir
    Path projectRoot;

    @Test
    void preservesTheExactPreparedChainAndPublishesOneVerifiedV2() throws Exception {
        String prompt = "pipeline success";
        String expectedText = ModelAttemptTestFixture.deterministicResponse(prompt);
        DeterministicFakeModelGateway gateway = mock(DeterministicFakeModelGateway.class);
        ModelAttemptTestFixture.Fixture fixture = ModelAttemptTestFixture.admitted(
                projectRoot,
                prompt,
                ModelAttemptTestFixture.sha256(expectedText),
                gateway);
        SchedulerModelInvocationPreparer preparer = mock(SchedulerModelInvocationPreparer.class);
        when(preparer.prepare(any(), same(fixture.workItem()), any(), any(), any(),
                anyLong(), any(), any())).thenReturn(fixture.preparation());
        ModelResponse response = new DeterministicFakeModelGateway()
                .invoke(fixture.preparation().profiledRequest().request());
        when(gateway.invoke(same(fixture.preparation().profiledRequest().request())))
                .thenReturn(response);
        EvidenceRunNamespaceStore evidenceStore = mock(EvidenceRunNamespaceStore.class);
        ModelRunRecordStore recordStore = mock(ModelRunRecordStore.class);
        String recordId = AgentRunRecordIdentity.recordId(
                ModelAttemptTestFixture.GOAL_ID, ModelAttemptTestFixture.AGENT_RUN_ID);
        StoredRunRecord stored = new StoredRunRecord(
                recordId, "run-record/" + recordId, Instant.EPOCH, 1, "e".repeat(64));
        when(recordStore.persistModel(eq(recordId), any())).thenReturn(stored);
        DeterministicFakeModelAttemptPipeline pipeline = pipeline(
                preparer, gateway, evidenceStore, recordStore);

        DeterministicFakeModelAttemptPipeline.Outcome.Published result = assertInstanceOf(
                DeterministicFakeModelAttemptPipeline.Outcome.Published.class,
                execute(pipeline, fixture.workItem()));

        verify(preparer).prepare(
                eq(projectRoot),
                same(fixture.workItem()),
                eq(AgentRunEvidenceIdentity.runId(
                        ModelAttemptTestFixture.GOAL_ID,
                        ModelAttemptTestFixture.AGENT_RUN_ID)),
                any(), eq(Set.of()),
                eq(EvidenceStoragePolicy.MAX_SUPPORTED_CONTENT_BYTES), any(), any());
        verify(gateway).invoke(same(fixture.preparation().profiledRequest().request()));
        ArgumentCaptor<ModelRunRecord> record = ArgumentCaptor.forClass(ModelRunRecord.class);
        verify(recordStore).persistModel(eq(recordId), record.capture());
        assertSame(stored, result.storedRecord());
        assertEquals(VerificationStatus.VERIFIED, result.verificationStatus());
        assertSame(fixture.preparation().profiledRequest().request(),
                record.getValue().modelRequest());
        assertEquals(AgentRunEvidenceIdentity.runId(
                        ModelAttemptTestFixture.GOAL_ID,
                        ModelAttemptTestFixture.AGENT_RUN_ID),
                record.getValue().lifecycleRecord().toolRequest().correlationId());
        assertEquals(ModelAttemptTestFixture.LOGICAL_RUN_ID,
                record.getValue().lifecycleRecord().logicalRunId());
        verifyNoInteractions(evidenceStore);
    }

    @Test
    void everyPreCallRefusalLeavesEvidenceAndRecordStoresUntouched() throws Exception {
        assertRefused(admissionRejected(),
                DeterministicFakeModelAttemptPipeline.Stage.ADMISSION);
        assertRefused(candidateRejected(),
                DeterministicFakeModelAttemptPipeline.Stage.CANDIDATE);
        assertRefused(requestRejected(),
                DeterministicFakeModelAttemptPipeline.Stage.EXACT_REQUEST);
        assertRefused(invocationRejected(),
                DeterministicFakeModelAttemptPipeline.Stage.INVOCATION);
    }

    @Test
    void preparationIoFailureIsAClosedRedactedZeroWriteOutcome() throws Exception {
        SchedulerModelInvocationPreparer preparer = mock(SchedulerModelInvocationPreparer.class);
        ModelAttemptTestFixture.Fixture fixture = normal("preparation");
        when(preparer.prepare(any(), any(), any(), any(), any(), anyLong(), any(), any()))
                .thenThrow(new IOException("secret prompt path"));
        DeterministicFakeModelGateway gateway = mock(DeterministicFakeModelGateway.class);
        EvidenceRunNamespaceStore evidenceStore = mock(EvidenceRunNamespaceStore.class);
        ModelRunRecordStore recordStore = mock(ModelRunRecordStore.class);

        Object result = execute(
                pipeline(preparer, gateway, evidenceStore, recordStore), fixture.workItem());

        assertInstanceOf(
                DeterministicFakeModelAttemptPipeline.Outcome.PreparationFailed.class,
                result);
        assertFalse(result.toString().contains("secret"));
        verifyNoInteractions(gateway, evidenceStore, recordStore);
    }

    @Test
    void gatewayFailureIsCodeOnlyAndPublishesFailedV2WithoutEvidenceStorage()
            throws Exception {
        ModelAttemptTestFixture.Fixture fixture = normal("gateway failure");
        SchedulerModelInvocationPreparer preparer = prepared(fixture);
        DeterministicFakeModelGateway gateway = fixture.ready().suitable().candidate().gateway();
        doThrow(new com.enhancer.model.ModelGatewayException(
                ModelFailureCode.PROVIDER_UNAVAILABLE, "secret provider endpoint"))
                .when(gateway)
                .invoke(same(fixture.preparation().profiledRequest().request()));
        EvidenceRunNamespaceStore evidenceStore = mock(EvidenceRunNamespaceStore.class);
        ModelRunRecordStore recordStore = mock(ModelRunRecordStore.class);
        String recordId = AgentRunRecordIdentity.recordId(
                ModelAttemptTestFixture.GOAL_ID, ModelAttemptTestFixture.AGENT_RUN_ID);
        when(recordStore.persistModel(eq(recordId), any())).thenReturn(
                new StoredRunRecord(recordId, "run-record/" + recordId,
                        Instant.EPOCH, 1, "f".repeat(64)));

        DeterministicFakeModelAttemptPipeline.Outcome.Published result = assertInstanceOf(
                DeterministicFakeModelAttemptPipeline.Outcome.Published.class,
                execute(pipeline(preparer, gateway, evidenceStore, recordStore),
                        fixture.workItem()));

        assertEquals(VerificationStatus.NOT_PERFORMED, result.verificationStatus());
        ArgumentCaptor<ModelRunRecord> record = ArgumentCaptor.forClass(ModelRunRecord.class);
        verify(recordStore).persistModel(eq(recordId), record.capture());
        assertEquals("tool-failure-code=TEMPORARY_FAILURE",
                record.getValue().lifecycleRecord().toolResult().evidence().outputTail());
        assertFalse(record.getValue().toString().contains("secret"));
        verifyNoInteractions(evidenceStore);
    }

    @Test
    void evidenceIoFailureIsSanitizedBeforeFailedV2Publication() throws Exception {
        String prompt = "sensitive prompt ".repeat(400);
        String expectedText = ModelAttemptTestFixture.deterministicResponse(prompt);
        DeterministicFakeModelGateway gateway = mock(DeterministicFakeModelGateway.class);
        ModelAttemptTestFixture.Fixture fixture = ModelAttemptTestFixture.admitted(
                projectRoot,
                prompt,
                ModelAttemptTestFixture.sha256(expectedText),
                gateway);
        SchedulerModelInvocationPreparer preparer = prepared(fixture);
        when(gateway.invoke(same(fixture.preparation().profiledRequest().request())))
                .thenReturn(new DeterministicFakeModelGateway()
                        .invoke(fixture.preparation().profiledRequest().request()));
        EvidenceRunNamespaceStore evidenceStore = mock(EvidenceRunNamespaceStore.class);
        doThrow(new IOException("secret evidence path"))
                .when(evidenceStore)
                .ensureRun(any());
        ModelRunRecordStore recordStore = mock(ModelRunRecordStore.class);
        String recordId = AgentRunRecordIdentity.recordId(
                ModelAttemptTestFixture.GOAL_ID, ModelAttemptTestFixture.AGENT_RUN_ID);
        when(recordStore.persistModel(eq(recordId), any())).thenReturn(
                new StoredRunRecord(recordId, "run-record/" + recordId,
                        Instant.EPOCH, 1, "1".repeat(64)));

        DeterministicFakeModelAttemptPipeline.Outcome.Published result = assertInstanceOf(
                DeterministicFakeModelAttemptPipeline.Outcome.Published.class,
                execute(pipeline(preparer, gateway, evidenceStore, recordStore),
                        fixture.workItem()));

        assertEquals(VerificationStatus.NOT_PERFORMED, result.verificationStatus());
        ArgumentCaptor<ModelRunRecord> record = ArgumentCaptor.forClass(ModelRunRecord.class);
        verify(recordStore).persistModel(eq(recordId), record.capture());
        assertEquals("tool-failure-code=EXECUTION_FAILED",
                record.getValue().lifecycleRecord().toolResult().evidence().outputTail());
        assertFalse(record.getValue().toString().contains("secret evidence path"));
        verify(evidenceStore).ensureRun(eq(AgentRunEvidenceIdentity.runId(
                ModelAttemptTestFixture.GOAL_ID,
                ModelAttemptTestFixture.AGENT_RUN_ID)));
        verify(evidenceStore, never()).persist(any(), any());
        verify(evidenceStore, never()).resolve(any());
    }

    @Test
    void longSuccessOrdersEvidenceBeforeVerificationAndV2Publication() throws Exception {
        String prompt = "ordered long prompt ".repeat(300);
        String expectedText = ModelAttemptTestFixture.deterministicResponse(prompt);
        DeterministicFakeModelGateway gateway = mock(DeterministicFakeModelGateway.class);
        ModelAttemptTestFixture.Fixture fixture = ModelAttemptTestFixture.admitted(
                projectRoot,
                prompt,
                ModelAttemptTestFixture.sha256(expectedText),
                gateway);
        SchedulerModelInvocationPreparer preparer = prepared(fixture);
        when(gateway.invoke(same(fixture.preparation().profiledRequest().request())))
                .thenReturn(new DeterministicFakeModelGateway()
                        .invoke(fixture.preparation().profiledRequest().request()));
        List<String> events = new ArrayList<>();
        OrderedEvidenceStore evidenceStore = new OrderedEvidenceStore(events);
        String recordId = AgentRunRecordIdentity.recordId(
                ModelAttemptTestFixture.GOAL_ID, ModelAttemptTestFixture.AGENT_RUN_ID);
        ModelRunRecordStore recordStore = new ModelRunRecordStore() {
            @Override
            public StoredRunRecord persistModel(ModelRunRecord record) {
                throw new AssertionError("random v2 identity must not be used");
            }

            @Override
            public StoredRunRecord persistModel(String actualRecordId, ModelRunRecord record) {
                assertEquals(recordId, actualRecordId);
                events.add("persist-model");
                return new StoredRunRecord(
                        recordId,
                        "run-record/" + recordId,
                        Instant.EPOCH,
                        1,
                        "2".repeat(64));
            }

            @Override
            public ResolvedModelRunRecord resolveModel(String reference) {
                throw new AssertionError("pipeline must not own point recovery");
            }
        };

        DeterministicFakeModelAttemptPipeline.Outcome.Published result = assertInstanceOf(
                DeterministicFakeModelAttemptPipeline.Outcome.Published.class,
                execute(pipeline(preparer, gateway, evidenceStore, recordStore),
                        fixture.workItem()));

        assertEquals(VerificationStatus.VERIFIED, result.verificationStatus());
        assertEquals(
                List.of("ensure", "persist-evidence", "resolve-evidence", "persist-model"),
                events);
    }

    private void assertRefused(
            ModelAttemptTestFixture.Fixture fixture,
            DeterministicFakeModelAttemptPipeline.Stage expectedStage) throws Exception {
        SchedulerModelInvocationPreparer preparer = prepared(fixture);
        DeterministicFakeModelGateway gateway = mock(DeterministicFakeModelGateway.class);
        EvidenceRunNamespaceStore evidenceStore = mock(EvidenceRunNamespaceStore.class);
        ModelRunRecordStore recordStore = mock(ModelRunRecordStore.class);
        DeterministicFakeModelAttemptPipeline.Outcome.Refused result = assertInstanceOf(
                DeterministicFakeModelAttemptPipeline.Outcome.Refused.class,
                execute(pipeline(preparer, gateway, evidenceStore, recordStore),
                        fixture.workItem()));

        assertEquals(expectedStage, result.stage());
        verifyNoInteractions(evidenceStore, recordStore);
        verify(gateway, never()).invoke(any());
    }

    private ModelAttemptTestFixture.Fixture admissionRejected() {
        ModelAttemptTestFixture.Fixture normal = normal("admission");
        return new ModelAttemptTestFixture.Fixture(
                normal.workItem(),
                new SchedulerModelInvocationPreparation(
                        normal.preparation().approvedTask(),
                        normal.preparation().executionPolicy(),
                        normal.preparation().profiledRequest(),
                        new ModelInvocationAdmissionDecision.Rejected(
                                ModelInvocationRejectionReason.TASK_TOOL_NOT_ALLOWED)),
                null);
    }

    private ModelAttemptTestFixture.Fixture candidateRejected() {
        return ModelAttemptTestFixture.admitted(
                projectRoot,
                "candidate",
                "c".repeat(64),
                mock(DeterministicFakeModelGateway.class),
                new ModelTokenBudget(20_000, 20_000, 40_000),
                "different-model",
                CancellationToken.none());
    }

    private ModelAttemptTestFixture.Fixture requestRejected() {
        return ModelAttemptTestFixture.admitted(
                projectRoot,
                "request-too-long",
                "c".repeat(64),
                mock(DeterministicFakeModelGateway.class),
                new ModelTokenBudget(1, 20_000, 20_001),
                "deterministic-fake",
                CancellationToken.none());
    }

    private ModelAttemptTestFixture.Fixture invocationRejected() {
        return ModelAttemptTestFixture.admitted(
                projectRoot,
                "cancelled",
                "c".repeat(64),
                mock(DeterministicFakeModelGateway.class),
                new ModelTokenBudget(20_000, 20_000, 40_000),
                "deterministic-fake",
                () -> true);
    }

    private ModelAttemptTestFixture.Fixture normal(String prompt) {
        return ModelAttemptTestFixture.admitted(
                projectRoot,
                prompt,
                ModelAttemptTestFixture.sha256(
                        ModelAttemptTestFixture.deterministicResponse(prompt)),
                mock(DeterministicFakeModelGateway.class));
    }

    private SchedulerModelInvocationPreparer prepared(
            ModelAttemptTestFixture.Fixture fixture) throws Exception {
        SchedulerModelInvocationPreparer preparer = mock(SchedulerModelInvocationPreparer.class);
        when(preparer.prepare(any(), any(), any(), any(), any(), anyLong(), any(), any()))
                .thenReturn(fixture.preparation());
        return preparer;
    }

    private DeterministicFakeModelAttemptPipeline pipeline(
            SchedulerModelInvocationPreparer preparer,
            DeterministicFakeModelGateway gateway,
            EvidenceRunNamespaceStore evidenceStore,
            ModelRunRecordStore recordStore) {
        return new DeterministicFakeModelAttemptPipeline(
                preparer,
                gateway,
                evidenceStore,
                recordStore,
                Clock.fixed(Instant.parse("2026-09-03T12:13:14.567890Z"), ZoneOffset.UTC));
    }

    private DeterministicFakeModelAttemptPipeline.Outcome execute(
            DeterministicFakeModelAttemptPipeline pipeline,
            WorkItem workItem) throws Exception {
        return pipeline.execute(
                projectRoot,
                ModelAttemptTestFixture.GOAL_ID,
                ModelAttemptTestFixture.AGENT_RUN_ID,
                workItem,
                new SchedulerModelInvocationLimits(Duration.ofSeconds(1), 20_000),
                Set.of(),
                EvidenceStoragePolicy.MAX_SUPPORTED_CONTENT_BYTES,
                Duration.ofSeconds(2),
                CancellationToken.none());
    }

    private static final class OrderedEvidenceStore implements EvidenceRunNamespaceStore {
        private static final String EVIDENCE_ID =
                "00000000-0000-0000-0000-00000000e001";

        private final List<String> events;
        private StoredEvidence stored;
        private String content;

        private OrderedEvidenceStore(List<String> events) {
            this.events = events;
        }

        @Override
        public void ensureRun(String runId) {
            assertEquals(AgentRunEvidenceIdentity.runId(
                    ModelAttemptTestFixture.GOAL_ID,
                    ModelAttemptTestFixture.AGENT_RUN_ID), runId);
            events.add("ensure");
        }

        @Override
        public String createRun() {
            throw new AssertionError("random evidence identity must not be used");
        }

        @Override
        public StoredEvidence persist(String runId, String value) {
            events.add("persist-evidence");
            content = value;
            stored = new StoredEvidence(
                    runId,
                    EVIDENCE_ID,
                    "evidence/" + runId + "/" + EVIDENCE_ID,
                    Instant.EPOCH,
                    value.getBytes(StandardCharsets.UTF_8).length,
                    ModelAttemptTestFixture.sha256(value));
            return stored;
        }

        @Override
        public ResolvedEvidence resolve(String reference) {
            assertEquals(stored.reference(), reference);
            events.add("resolve-evidence");
            return new ResolvedEvidence(stored, content);
        }

        @Override
        public EvidenceStoragePolicy storagePolicy() {
            return new EvidenceStoragePolicy(
                    EvidenceStoragePolicy.MAX_SUPPORTED_CONTENT_BYTES);
        }
    }
}
