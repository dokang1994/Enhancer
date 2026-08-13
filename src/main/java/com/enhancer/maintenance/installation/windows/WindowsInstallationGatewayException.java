package com.enhancer.maintenance.installation.windows;

import java.util.Objects;

/** Bounded gateway refusal; platform dumps and native error text are not propagated. */
public final class WindowsInstallationGatewayException extends Exception {
    private static final long serialVersionUID = 1L;
    private final WindowsInstallationGatewayFailureReason reason;

    public WindowsInstallationGatewayException(
            WindowsInstallationGatewayFailureReason reason, String detail) {
        super(detail(detail));
        this.reason = Objects.requireNonNull(reason, "reason must not be null");
    }

    public WindowsInstallationGatewayFailureReason reason() {
        return reason;
    }

    private static String detail(String value) {
        String checked = Objects.requireNonNull(value, "detail must not be null");
        if (checked.isBlank() || checked.length() > 256) {
            throw new IllegalArgumentException("detail is outside supported bounds");
        }
        return checked;
    }
}
