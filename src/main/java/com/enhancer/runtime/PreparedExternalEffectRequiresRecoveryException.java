package com.enhancer.runtime;

/** Existing prepared intent is ambiguous and cannot authorize automatic invocation. */
public final class PreparedExternalEffectRequiresRecoveryException
        extends IllegalStateException {
    private static final long serialVersionUID = 1L;

    public PreparedExternalEffectRequiresRecoveryException(
            String idempotencyKey) {
        super("external effect requires explicit recovery: " + idempotencyKey);
    }
}
