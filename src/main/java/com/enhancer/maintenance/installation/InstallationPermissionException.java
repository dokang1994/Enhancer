package com.enhancer.maintenance.installation;

import java.util.Objects;

/** Bounded adapter refusal with no platform dump, candidate bytes, or credential. */
public final class InstallationPermissionException extends Exception {
    private static final long serialVersionUID = 1L;
    private static final int MAXIMUM_DETAIL_CHARACTERS = 512;
    private final InstallationPermissionFailureReason reason;

    public InstallationPermissionException(
            InstallationPermissionFailureReason reason, String detail) {
        super(detail(detail));
        this.reason = Objects.requireNonNull(reason, "reason must not be null");
    }

    public InstallationPermissionFailureReason reason() {
        return reason;
    }

    private static String detail(String value) {
        String checked = Objects.requireNonNull(value, "detail must not be null");
        if (checked.isBlank() || checked.length() > MAXIMUM_DETAIL_CHARACTERS) {
            throw new IllegalArgumentException("detail is outside supported bounds");
        }
        for (int index = 0; index < checked.length(); index++) {
            if (Character.isISOControl(checked.charAt(index))) {
                throw new IllegalArgumentException("detail must not contain controls");
            }
        }
        return checked;
    }
}
