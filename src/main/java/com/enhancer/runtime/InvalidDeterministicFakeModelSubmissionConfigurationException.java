package com.enhancer.runtime;

import java.io.IOException;

/** Distinguishes first-use repository configuration I/O from durable-store I/O. */
final class InvalidDeterministicFakeModelSubmissionConfigurationException
        extends IOException {
    private static final long serialVersionUID = 1L;

    InvalidDeterministicFakeModelSubmissionConfigurationException(IOException cause) {
        super("deterministic-fake model submission project configuration is invalid", cause);
    }
}
