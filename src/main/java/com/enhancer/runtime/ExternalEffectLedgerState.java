package com.enhancer.runtime;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/** Immutable schema-v2 current state for one Goal's bounded external-effect ledger. */
public final class ExternalEffectLedgerState {
    public static final int CURRENT_SCHEMA_VERSION = 2;
    public static final int MAX_EFFECTS = 256;

    private final int schemaVersion;
    private final String goalId;
    private final long revision;
    private final List<ExternalEffectRecord> records;

    ExternalEffectLedgerState(
            int schemaVersion,
            String goalId,
            long revision,
            List<ExternalEffectRecord> records) {
        if (schemaVersion != CURRENT_SCHEMA_VERSION) {
            throw new IllegalArgumentException(
                    "External effect ledger schema version is unsupported");
        }
        this.schemaVersion = schemaVersion;
        this.goalId = RuntimeIdentity.canonicalUuid(goalId, "goalId");
        if (revision < 0) {
            throw new IllegalArgumentException("revision must not be negative");
        }
        this.revision = revision;
        Objects.requireNonNull(records, "records must not be null");
        if (records.size() > MAX_EFFECTS) {
            throw new IllegalArgumentException(
                    "external effect ledger exceeds its capacity");
        }
        List<ExternalEffectRecord> copy = new ArrayList<>();
        Set<String> keys = new LinkedHashSet<>();
        for (ExternalEffectRecord record : records) {
            ExternalEffectRecord checked = Objects.requireNonNull(
                    record, "records must not contain null");
            if (!this.goalId.equals(checked.request().goalId())) {
                throw new IllegalArgumentException(
                        "external effect Goal does not match ledger Goal");
            }
            if (!keys.add(checked.request().idempotencyKey())) {
                throw new IllegalArgumentException(
                        "external effect idempotency keys must be unique");
            }
            copy.add(checked);
        }
        this.records = List.copyOf(copy);
    }

    public static ExternalEffectLedgerState initial(String goalId) {
        return new ExternalEffectLedgerState(
                CURRENT_SCHEMA_VERSION, goalId, 0, List.of());
    }

    public int schemaVersion() {
        return schemaVersion;
    }

    public String goalId() {
        return goalId;
    }

    public long revision() {
        return revision;
    }

    public List<ExternalEffectRecord> records() {
        return records;
    }

    Optional<ExternalEffectRecord> find(String idempotencyKey) {
        Objects.requireNonNull(
                idempotencyKey, "idempotencyKey must not be null");
        return records.stream()
                .filter(record -> record.request().idempotencyKey()
                        .equals(idempotencyKey))
                .findFirst();
    }

    ExternalEffectLedgerState prepare(ExternalEffectRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        if (!goalId.equals(request.goalId())) {
            throw new IllegalArgumentException(
                    "external effect Goal does not match ledger Goal");
        }
        if (records.size() >= MAX_EFFECTS) {
            throw new IllegalStateException(
                    "external effect ledger is at capacity");
        }
        List<ExternalEffectRecord> next = new ArrayList<>(records);
        next.add(new ExternalEffectRecord(
                request, ExternalEffectStatus.PREPARED));
        return new ExternalEffectLedgerState(
                schemaVersion, goalId, revision + 1, next);
    }

    ExternalEffectLedgerState recordOutcome(
            String idempotencyKey,
            ExternalEffectStatus outcome,
            ExternalEffectOutcomeEvidence evidence) {
        Objects.requireNonNull(evidence, "evidence must not be null");
        if (!outcome.isTerminal()) {
            throw new IllegalArgumentException(
                    "external effect outcome must be terminal");
        }
        List<ExternalEffectRecord> next = new ArrayList<>(records);
        for (int index = 0; index < next.size(); index++) {
            ExternalEffectRecord current = next.get(index);
            if (current.request().idempotencyKey().equals(idempotencyKey)) {
                next.set(index, current.terminate(outcome, evidence));
                return new ExternalEffectLedgerState(
                        schemaVersion, goalId, revision + 1, next);
            }
        }
        throw new IllegalArgumentException(
                "external effect is not prepared: " + idempotencyKey);
    }

    boolean isValidSuccessorOf(ExternalEffectLedgerState current) {
        Objects.requireNonNull(current, "current must not be null");
        if (!goalId.equals(current.goalId)
                || revision != current.revision + 1) {
            return false;
        }
        if (records.size() == current.records.size() + 1) {
            if (!records.subList(0, current.records.size())
                    .equals(current.records)) {
                return false;
            }
            return records.get(records.size() - 1).status()
                    == ExternalEffectStatus.PREPARED;
        }
        if (records.size() != current.records.size()) {
            return false;
        }
        int changes = 0;
        for (int index = 0; index < records.size(); index++) {
            ExternalEffectRecord before = current.records.get(index);
            ExternalEffectRecord after = records.get(index);
            if (before.equals(after)) {
                continue;
            }
            changes++;
            if (!before.request().equals(after.request())
                    || before.status() != ExternalEffectStatus.PREPARED
                    || !after.status().isTerminal()) {
                return false;
            }
        }
        return changes == 1;
    }
}
