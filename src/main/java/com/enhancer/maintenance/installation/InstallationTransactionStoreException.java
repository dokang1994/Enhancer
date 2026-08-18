package com.enhancer.maintenance.installation;

import java.util.Objects;

/** Bounded typed transaction-store refusal, separate from permission-adapter failures. */
public final class InstallationTransactionStoreException extends Exception {
    private static final long serialVersionUID = 1L;
    private final Reason reason;

    public InstallationTransactionStoreException(Reason reason, String detail) {
        super(InstallationPrincipal.boundedText(detail, "detail"));
        this.reason = Objects.requireNonNull(reason, "reason must not be null");
    }

    public Reason reason() {
        return reason;
    }

    public enum Reason {
        NOT_FOUND,
        TRANSACTION_CONFLICT,
        REVISION_CONFLICT,
        INVALID_TRANSITION,
        LOCK_CONTENDED,
        UNSUPPORTED_SCHEMA,
        CORRUPT_STATE,
        CAPACITY_EXCEEDED,
        STORE_UNAVAILABLE,
        REQUIRES_RECONCILIATION
    }
}
