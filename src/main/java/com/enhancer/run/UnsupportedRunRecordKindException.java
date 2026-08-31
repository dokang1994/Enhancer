package com.enhancer.run;

import java.io.IOException;
import java.util.Objects;

/** A known RunRecord payload was requested through the other type-level resolver. */
public final class UnsupportedRunRecordKindException extends IOException {
    private static final long serialVersionUID = 1L;

    private final String reference;
    private final RunRecordKind expectedKind;
    private final RunRecordKind actualKind;

    public UnsupportedRunRecordKindException(
            String reference,
            RunRecordKind expectedKind,
            RunRecordKind actualKind) {
        super(message(reference, expectedKind, actualKind));
        this.reference = requireReference(reference);
        this.expectedKind = Objects.requireNonNull(
                expectedKind, "expectedKind must not be null");
        this.actualKind = Objects.requireNonNull(
                actualKind, "actualKind must not be null");
        if (expectedKind == actualKind) {
            throw new IllegalArgumentException(
                    "expectedKind and actualKind must differ");
        }
    }

    public String reference() {
        return reference;
    }

    public RunRecordKind expectedKind() {
        return expectedKind;
    }

    public RunRecordKind actualKind() {
        return actualKind;
    }

    private static String message(
            String reference,
            RunRecordKind expectedKind,
            RunRecordKind actualKind) {
        return "RunRecord " + requireReference(reference)
                + " has kind " + Objects.requireNonNull(
                        actualKind, "actualKind must not be null")
                + ", not " + Objects.requireNonNull(
                        expectedKind, "expectedKind must not be null");
    }

    private static String requireReference(String value) {
        Objects.requireNonNull(value, "reference must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("reference must not be blank");
        }
        return value;
    }
}
