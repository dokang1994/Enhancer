package com.enhancer.runtime;

import com.enhancer.run.ModelRunRecordStore;
import com.enhancer.run.ResolvedModelRunRecord;
import com.enhancer.run.ResolvedRunRecord;
import com.enhancer.run.RunRecord;
import com.enhancer.run.RunRecordStore;
import com.enhancer.run.StoredRunRecord;
import com.enhancer.tool.EvidenceStore;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;

/** Resolves and fully binds one checkpointed record through its payload-kind port. */
final class AgentRunRecordResolver {
    private final RunRecordStore legacyStore;
    private final Optional<ModelContext> modelContext;

    AgentRunRecordResolver(RunRecordStore legacyStore) {
        this.legacyStore = Objects.requireNonNull(
                legacyStore, "legacyStore must not be null");
        this.modelContext = Optional.empty();
    }

    AgentRunRecordResolver(
            RunRecordStore legacyStore,
            ModelRunRecordStore modelStore,
            EvidenceStore evidenceStore,
            Path projectRoot,
            ModelProcessExecutionConfiguration configuration) {
        this.legacyStore = Objects.requireNonNull(
                legacyStore, "legacyStore must not be null");
        this.modelContext = Optional.of(new ModelContext(
                modelStore,
                new ModelRunRecordBindingValidator(evidenceStore),
                Objects.requireNonNull(projectRoot, "projectRoot must not be null")
                        .toAbsolutePath().normalize(),
                configuration));
    }

    Resolved resolve(
            String goalId,
            String agentRunId,
            WorkItem workItem,
            String reference) throws IOException {
        Objects.requireNonNull(workItem, "workItem must not be null");
        Objects.requireNonNull(reference, "reference must not be null");
        if (!workItem.isModelWork()) {
            ResolvedRunRecord legacy = legacyStore.resolve(reference);
            DurableAgentRunFinalizer.requireBinding(legacy.record(), workItem);
            return new Resolved(legacy.metadata(), legacy.record());
        }

        ModelContext context = modelContext.orElseThrow(() ->
                new IOException("typed ModelWork record resolution is unavailable"));
        String expectedReference = AgentRunRecordIdentity.reference(goalId, agentRunId);
        if (!expectedReference.equals(reference)) {
            throw new IOException(
                    "typed ModelWork reference does not match the deterministic AgentRun identity");
        }
        ResolvedModelRunRecord model = context.modelStore().resolveModel(reference);
        context.validator().requireBinding(
                model,
                goalId,
                agentRunId,
                workItem,
                context.projectRoot(),
                context.configuration());
        return new Resolved(model.metadata(), model.record().lifecycleRecord());
    }

    record Resolved(StoredRunRecord metadata, RunRecord record) {
        Resolved {
            Objects.requireNonNull(metadata, "metadata must not be null");
            Objects.requireNonNull(record, "record must not be null");
        }
    }

    private record ModelContext(
            ModelRunRecordStore modelStore,
            ModelRunRecordBindingValidator validator,
            Path projectRoot,
            ModelProcessExecutionConfiguration configuration) {
        private ModelContext {
            Objects.requireNonNull(modelStore, "modelStore must not be null");
            Objects.requireNonNull(validator, "validator must not be null");
            Objects.requireNonNull(projectRoot, "projectRoot must not be null");
            Objects.requireNonNull(configuration, "configuration must not be null");
        }
    }
}
