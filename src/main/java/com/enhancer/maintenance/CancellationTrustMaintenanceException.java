package com.enhancer.maintenance;

import java.io.IOException;
import java.util.Objects;

/** Typed checked failure from the installed-trust maintenance state machine. */
public final class CancellationTrustMaintenanceException extends IOException {
    private static final long serialVersionUID = 1L;
    private static final int MAX_DETAIL_CHARACTERS = 256;

    private final CancellationTrustMaintenanceFailureReason reason;

    public CancellationTrustMaintenanceException(
            CancellationTrustMaintenanceFailureReason reason,
            String detail) {
        this(reason, detail, null);
    }

    public CancellationTrustMaintenanceException(
            CancellationTrustMaintenanceFailureReason reason,
            String detail,
            Throwable cause) {
        super(bounded(detail), cause);
        this.reason = Objects.requireNonNull(reason, "reason must not be null");
    }

    public CancellationTrustMaintenanceFailureReason reason() {
        return reason;
    }

    public CancellationTrustMaintenanceFailureCategory category() {
        return reason.category();
    }

    private static String bounded(String detail) {
        String checked = Objects.requireNonNull(detail, "detail must not be null");
        return checked.length() <= MAX_DETAIL_CHARACTERS
                ? checked
                : checked.substring(0, MAX_DETAIL_CHARACTERS);
    }
}
