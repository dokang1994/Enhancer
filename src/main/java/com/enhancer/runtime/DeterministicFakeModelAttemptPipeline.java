package com.enhancer.runtime;

import com.enhancer.kernel.VerificationStatus;
import com.enhancer.model.DeterministicFakeExactRequestDecision;
import com.enhancer.model.DeterministicFakeExactRequestInvocationResult;
import com.enhancer.model.DeterministicFakeExactRequestInvoker;
import com.enhancer.model.DeterministicFakeExactRequestPreparation;
import com.enhancer.model.DeterministicFakeModelCandidate;
import com.enhancer.model.DeterministicFakeModelGateway;
import com.enhancer.model.DeterministicModelInvokeVerifier;
import com.enhancer.model.ModelCandidateSuitability;
import com.enhancer.model.ModelCandidateSuitabilityDecision;
import com.enhancer.model.ModelInvocationAdmissionDecision;
import com.enhancer.run.ModelRunRecordStore;
import com.enhancer.run.StoredRunRecord;
import com.enhancer.tool.CancellationToken;
import com.enhancer.tool.EvidenceRunNamespaceStore;
import com.enhancer.tool.ToolExecutor;
import com.enhancer.tool.ToolResult;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.util.Objects;
import java.util.Set;

/** Uncalled child-local RFC-0023 deterministic-fake attempt composition. */
final class DeterministicFakeModelAttemptPipeline {
    private final SchedulerModelInvocationPreparer preparer;
    private final DeterministicFakeModelGateway gateway;
    private final EvidenceRunNamespaceStore evidenceStore;
    private final ModelRunRecordFinalizer finalizer;

    DeterministicFakeModelAttemptPipeline(
            SchedulerModelInvocationPreparer preparer,
            DeterministicFakeModelGateway gateway,
            EvidenceRunNamespaceStore evidenceStore,
            ModelRunRecordStore recordStore,
            Clock clock) {
        this.preparer = Objects.requireNonNull(preparer, "preparer must not be null");
        this.gateway = Objects.requireNonNull(gateway, "gateway must not be null");
        this.evidenceStore = Objects.requireNonNull(
                evidenceStore, "evidenceStore must not be null");
        this.finalizer = new ModelRunRecordFinalizer(
                new DeterministicModelInvokeVerifier(evidenceStore),
                Objects.requireNonNull(recordStore, "recordStore must not be null"),
                Objects.requireNonNull(clock, "clock must not be null"));
    }

    Outcome execute(
            Path projectRoot,
            String goalId,
            String agentRunId,
            WorkItem workItem,
            SchedulerModelInvocationLimits limits,
            Set<String> deniedTools,
            long maximumReadBytes,
            Duration toolTimeout,
            CancellationToken cancellationToken) throws IOException {
        Objects.requireNonNull(projectRoot, "projectRoot must not be null");
        Objects.requireNonNull(workItem, "workItem must not be null");
        Objects.requireNonNull(limits, "limits must not be null");
        Objects.requireNonNull(deniedTools, "deniedTools must not be null");
        Objects.requireNonNull(toolTimeout, "toolTimeout must not be null");
        Objects.requireNonNull(cancellationToken, "cancellationToken must not be null");
        String evidenceRunId = AgentRunEvidenceIdentity.runId(goalId, agentRunId);

        SchedulerModelInvocationPreparation preparation;
        try {
            preparation = preparer.prepare(
                    projectRoot,
                    workItem,
                    evidenceRunId,
                    limits,
                    deniedTools,
                    maximumReadBytes,
                    toolTimeout,
                    cancellationToken);
        } catch (IOException | IllegalArgumentException exception) {
            return new Outcome.PreparationFailed();
        }

        if (preparation.admissionDecision()
                instanceof ModelInvocationAdmissionDecision.Rejected rejected) {
            return new Outcome.Refused(Stage.ADMISSION, rejected.reason());
        }
        ModelInvocationAdmissionDecision.Admitted admitted =
                (ModelInvocationAdmissionDecision.Admitted) preparation.admissionDecision();
        ModelCandidateSuitabilityDecision suitability = new ModelCandidateSuitability()
                .evaluate(admitted, DeterministicFakeModelCandidate.bind(gateway));
        if (suitability instanceof ModelCandidateSuitabilityDecision.Rejected rejected) {
            return new Outcome.Refused(Stage.CANDIDATE, rejected.reason());
        }
        DeterministicFakeExactRequestDecision requestDecision =
                new DeterministicFakeExactRequestPreparation().evaluate(
                        (ModelCandidateSuitabilityDecision.Suitable) suitability,
                        preparation.executionPolicy());
        if (requestDecision instanceof DeterministicFakeExactRequestDecision.Refused refused) {
            return new Outcome.Refused(Stage.EXACT_REQUEST, refused.reason());
        }
        DeterministicFakeExactRequestDecision.Ready ready =
                (DeterministicFakeExactRequestDecision.Ready) requestDecision;
        DeterministicFakeExactRequestInvocationResult invocation =
                new DeterministicFakeExactRequestInvoker().invoke(ready);
        if (invocation instanceof DeterministicFakeExactRequestInvocationResult.Refused refused) {
            return new Outcome.Refused(Stage.INVOCATION, refused.reason());
        }

        String targetPath = workItem.modelExecutionInput().orElseThrow().targetPath();
        DeterministicFakeReturnedOutcomeTool tool =
                new DeterministicFakeReturnedOutcomeTool(
                        invocation, targetPath, evidenceStore);
        ToolResult result;
        try (ToolExecutor executor = new ToolExecutor(java.util.List.of(tool))) {
            result = DeterministicFakeReturnedOutcomeTool.sanitize(
                    executor.execute(tool.request(), preparation.executionPolicy()));
        }
        ModelRunRecordFinalizer.Published published = finalizer.finalizeRun(
                goalId,
                agentRunId,
                workItem,
                preparation,
                ready,
                tool.request(),
                result);
        return new Outcome.Published(
                published.storedRecord(),
                published.record().lifecycleRecord().verification().status());
    }

    enum Stage {
        ADMISSION,
        CANDIDATE,
        EXACT_REQUEST,
        INVOCATION
    }

    sealed interface Outcome {
        record PreparationFailed() implements Outcome {}

        record Refused(Stage stage, Enum<?> reason) implements Outcome {
            public Refused {
                Objects.requireNonNull(stage, "stage must not be null");
                Objects.requireNonNull(reason, "reason must not be null");
            }
        }

        record Published(
                StoredRunRecord storedRecord,
                VerificationStatus verificationStatus) implements Outcome {
            public Published {
                Objects.requireNonNull(storedRecord, "storedRecord must not be null");
                Objects.requireNonNull(
                        verificationStatus, "verificationStatus must not be null");
            }
        }
    }
}
