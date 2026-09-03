package com.enhancer.runtime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.enhancer.model.DeterministicFakeModelGateway;
import com.enhancer.run.ModelRunRecord;
import com.enhancer.run.ModelRunRecordStore;
import com.enhancer.run.ResolvedModelRunRecord;
import com.enhancer.run.StoredRunRecord;
import com.enhancer.tool.CancellationToken;
import com.enhancer.tool.EvidenceRunNamespaceStore;
import com.enhancer.tool.EvidenceStoragePolicy;
import com.enhancer.tool.FileSystemEvidenceStore;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Set;

final class ModelProcessValidationTestFixture {
    static final SchedulerModelInvocationLimits LIMITS =
            new SchedulerModelInvocationLimits(Duration.ofSeconds(1), 20_000);
    static final long MAXIMUM_READ_BYTES = 64 * 1024;
    static final Duration TOOL_TIMEOUT = Duration.ofSeconds(2);

    private ModelProcessValidationTestFixture() {}

    static Prepared valid(Path projectRoot) throws Exception {
        return valid(
                projectRoot,
                "process validation",
                mock(EvidenceRunNamespaceStore.class));
    }

    static Prepared validWithLongEvidence(Path projectRoot, Path evidenceRoot)
            throws Exception {
        return valid(
                projectRoot,
                "long process validation prompt ".repeat(300),
                new FileSystemEvidenceStore(
                        evidenceRoot,
                        new EvidenceStoragePolicy(
                                EvidenceStoragePolicy.MAX_SUPPORTED_CONTENT_BYTES)));
    }

    private static Prepared valid(
            Path projectRoot,
            String prompt,
            EvidenceRunNamespaceStore evidenceStore) throws Exception {
        DeterministicFakeModelGateway gateway = new DeterministicFakeModelGateway();
        ModelAttemptTestFixture.Fixture fixture = ModelAttemptTestFixture.admitted(
                projectRoot,
                prompt,
                ModelAttemptTestFixture.sha256(
                        ModelAttemptTestFixture.deterministicResponse(prompt)),
                gateway);
        SchedulerModelInvocationPreparer preparer =
                mock(SchedulerModelInvocationPreparer.class);
        when(preparer.prepare(
                any(), any(), any(), any(), any(), anyLong(), any(), any()))
                .thenReturn(fixture.preparation());
        CapturingStore recordStore = new CapturingStore();
        DeterministicFakeModelAttemptPipeline pipeline =
                new DeterministicFakeModelAttemptPipeline(
                        preparer,
                        gateway,
                        evidenceStore,
                        recordStore,
                        Clock.fixed(
                                Instant.parse("2026-09-03T12:13:14.567890Z"),
                                ZoneOffset.UTC));

        pipeline.execute(
                projectRoot,
                ModelAttemptTestFixture.GOAL_ID,
                ModelAttemptTestFixture.AGENT_RUN_ID,
                fixture.workItem(),
                LIMITS,
                Set.of(),
                MAXIMUM_READ_BYTES,
                TOOL_TIMEOUT,
                CancellationToken.none());

        return new Prepared(
                fixture.workItem(),
                recordStore.resolved(),
                evidenceStore,
                new ModelProcessExecutionConfiguration(
                        LIMITS, Set.of(), MAXIMUM_READ_BYTES, TOOL_TIMEOUT));
    }

    record Prepared(
            WorkItem workItem,
            ResolvedModelRunRecord resolved,
            EvidenceRunNamespaceStore evidenceStore,
            ModelProcessExecutionConfiguration configuration) {}

    private static final class CapturingStore implements ModelRunRecordStore {
        private ModelRunRecord record;
        private StoredRunRecord metadata;

        @Override
        public StoredRunRecord persistModel(ModelRunRecord ignored) {
            throw new AssertionError("the deterministic record identity is required");
        }

        @Override
        public StoredRunRecord persistModel(String recordId, ModelRunRecord value) {
            record = value;
            metadata = new StoredRunRecord(
                    recordId,
                    "run-record/" + recordId,
                    Instant.parse("2026-09-03T12:13:14.568Z"),
                    1,
                    "d".repeat(64));
            return metadata;
        }

        @Override
        public ResolvedModelRunRecord resolveModel(String reference) {
            throw new AssertionError("the attempt pipeline does not own recovery");
        }

        ResolvedModelRunRecord resolved() {
            return new ResolvedModelRunRecord(metadata, record);
        }
    }
}
