package com.enhancer.runtime;

import com.enhancer.tool.EvidenceStore;
import com.enhancer.tool.ResolvedEvidence;
import com.enhancer.tool.StoredEvidence;
import java.io.IOException;
import java.util.Objects;

/**
 * Application boundary for prepare-before-invoke and evidence-before-terminal ordering.
 * Existing prepared intent is never interpreted as permission to invoke again.
 */
public final class DurableExternalEffectExecutor {
    private final DurableExternalEffectLedger ledger;
    private final EvidenceStore evidenceStore;

    public DurableExternalEffectExecutor(
            DurableExternalEffectLedger ledger,
            EvidenceStore evidenceStore) {
        this.ledger = Objects.requireNonNull(
                ledger, "ledger must not be null");
        this.evidenceStore = Objects.requireNonNull(
                evidenceStore, "evidenceStore must not be null");
    }

    public ExternalEffectExecutionResult execute(
            ExternalEffectRequest request,
            ExternalEffectAdapter adapter,
            String ownerId,
            long fenceToken) throws IOException, ExternalEffectAdapterException {
        Objects.requireNonNull(request, "request must not be null");
        Objects.requireNonNull(adapter, "adapter must not be null");
        requireAdapterBinding(request, adapter);

        ExternalEffectRecord existing = ledger.records().stream()
                .filter(record -> record.request().idempotencyKey()
                        .equals(request.idempotencyKey()))
                .findFirst()
                .orElse(null);
        if (existing != null) {
            if (!existing.request().equals(request)) {
                throw new IllegalArgumentException(
                        "idempotency key is already bound to a different effect");
            }
            if (existing.status() == ExternalEffectStatus.PREPARED) {
                throw new PreparedExternalEffectRequiresRecoveryException(
                        request.idempotencyKey());
            }
            return resolvedResult(existing, false);
        }

        ledger.prepare(request, ownerId, fenceToken);
        ExternalEffectAdapterResult adapterResult = Objects.requireNonNull(
                adapter.invoke(request.idempotencyKey()),
                "adapter result must not be null");
        String evidenceRunId = evidenceStore.createRun();
        StoredEvidence stored = evidenceStore.persist(
                evidenceRunId, adapterResult.evidenceContent());
        ExternalEffectOutcomeEvidence binding =
                new ExternalEffectOutcomeEvidence(
                        stored.reference(), stored.sha256());
        ExternalEffectRecord terminal = ledger.recordOutcome(
                request.idempotencyKey(),
                adapterResult.status(),
                binding,
                ownerId,
                fenceToken);
        return resolvedResult(terminal, true);
    }

    private ExternalEffectExecutionResult resolvedResult(
            ExternalEffectRecord record,
            boolean adapterInvoked) throws IOException {
        ExternalEffectOutcomeEvidence binding = record.outcomeEvidence()
                .orElseThrow();
        ResolvedEvidence resolved = evidenceStore.resolve(binding.reference());
        if (!binding.sha256().equals(resolved.metadata().sha256())) {
            throw new IOException(
                    "terminal external effect evidence digest does not match");
        }
        return new ExternalEffectExecutionResult(
                record, resolved, adapterInvoked);
    }

    private static void requireAdapterBinding(
            ExternalEffectRequest request,
            ExternalEffectAdapter adapter) {
        if (!request.adapterId().equals(adapter.adapterId())) {
            throw new IllegalArgumentException(
                    "external effect adapter identity does not match request");
        }
        if (!request.operationSha256().equals(adapter.operationSha256())) {
            throw new IllegalArgumentException(
                    "external effect semantic digest does not match adapter operation");
        }
    }
}
