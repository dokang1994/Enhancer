package com.enhancer.runtime;

import java.util.Objects;
import java.util.UUID;

final class RuntimeIdentity {
    private RuntimeIdentity() {
    }

    static String canonicalUuid(String value, String field) {
        Objects.requireNonNull(value, field + " must not be null");
        try {
            if (!UUID.fromString(value).toString().equals(value)) {
                throw new IllegalArgumentException(
                        field + " must be a canonical UUID");
            }
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(
                    field + " must be a canonical UUID",
                    exception);
        }
        return value;
    }
}
