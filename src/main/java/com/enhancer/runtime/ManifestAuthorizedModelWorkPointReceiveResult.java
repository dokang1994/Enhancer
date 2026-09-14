package com.enhancer.runtime;

import java.util.Objects;

/** Bounded result after durable typed admission and transport-point acknowledgement. */
public record ManifestAuthorizedModelWorkPointReceiveResult(
        DurableWorkMessageReceiveResult admission,
        String spoolStatus,
        String submissionId,
        String messageId,
        String acknowledgedFile) {
    public ManifestAuthorizedModelWorkPointReceiveResult {
        Objects.requireNonNull(admission, "admission must not be null");
        spoolStatus = required(spoolStatus, "spoolStatus");
        submissionId = required(submissionId, "submissionId");
        messageId = required(messageId, "messageId");
        acknowledgedFile = required(acknowledgedFile, "acknowledgedFile");
    }

    private static String required(String value, String name) {
        Objects.requireNonNull(value, name + " must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }
}
