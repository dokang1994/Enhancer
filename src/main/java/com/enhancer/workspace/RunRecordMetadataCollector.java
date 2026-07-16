package com.enhancer.workspace;

import com.enhancer.text.UnicodeText;
import com.enhancer.run.CorruptedRunRecordException;
import com.enhancer.run.MissingRunRecordException;
import com.enhancer.run.ResolvedRunRecord;
import com.enhancer.run.RunRecordStore;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Read-only Workspace source adapter over the RunRecord store. It observes stored run records
 * as bounded metadata: the durable reference, the envelope digest, and the stored time. A record
 * that fails integrity resolution is surfaced as an explicit Unavailable observation instead of
 * being silently skipped.
 */
public final class RunRecordMetadataCollector {
    public static final int MAX_OBSERVED_RECORDS = 256;
    private static final String PROVENANCE = "run-record-store";
    private static final int MAX_REASON_CHARACTERS =
            WorkspaceSourceObservation.MAX_REASON_CHARACTERS;

    public List<WorkspaceSourceObservation> observe(RunRecordStore store, Instant observedAt)
            throws IOException {
        Objects.requireNonNull(store, "store must not be null");
        Objects.requireNonNull(observedAt, "observedAt must not be null");

        List<WorkspaceSourceObservation> observations = new ArrayList<>();
        for (String reference : store.recentReferences(MAX_OBSERVED_RECORDS)) {
            observations.add(observe(store, reference, observedAt));
        }
        return List.copyOf(observations);
    }

    private WorkspaceSourceObservation observe(
            RunRecordStore store,
            String reference,
            Instant observedAt) throws IOException {
        ResolvedRunRecord resolved;
        try {
            resolved = store.resolve(reference);
        } catch (CorruptedRunRecordException | MissingRunRecordException exception) {
            return new WorkspaceSourceObservation(
                    WorkspaceSourceKind.RUN_RECORD,
                    reference,
                    PROVENANCE,
                    observedAt,
                    Optional.empty(),
                    WorkspaceSourceState.UNAVAILABLE,
                    Optional.empty(),
                    Optional.of(boundedReason(exception)));
        }

        Instant storedAt = resolved.metadata().storedAt();
        return new WorkspaceSourceObservation(
                WorkspaceSourceKind.RUN_RECORD,
                reference,
                PROVENANCE,
                observedAt,
                storedAt.isAfter(observedAt) ? Optional.empty() : Optional.of(storedAt),
                WorkspaceSourceState.AVAILABLE,
                Optional.of(resolved.metadata().sha256()),
                Optional.empty());
    }

    private static String boundedReason(IOException exception) {
        String message = exception.getMessage();
        String reason = message == null || message.isBlank()
                ? exception.getClass().getSimpleName()
                : message;
        return UnicodeText.prefix(reason, MAX_REASON_CHARACTERS);
    }
}
