package com.enhancer.maintenance.installation;

import java.util.Objects;

/** Bounded typed refusal from a pure installation record byte format. */
public final class InstallationRecordFormatException extends Exception {
    private static final long serialVersionUID = 1L;
    private final Reason reason;

    public InstallationRecordFormatException(Reason reason, String detail) {
        super(InstallationPrincipal.boundedText(detail, "detail"));
        this.reason = Objects.requireNonNull(reason, "reason must not be null");
    }

    public Reason reason() {
        return reason;
    }

    public enum Reason {
        UNSUPPORTED_SCHEMA,
        CORRUPT_RECORD,
        SIZE_LIMIT_EXCEEDED,
        FOREIGN_RECORD,
        NON_CANONICAL_RECORD
    }
}
