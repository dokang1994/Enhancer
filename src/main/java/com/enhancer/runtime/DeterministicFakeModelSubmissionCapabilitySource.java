package com.enhancer.runtime;

/** Closed repository-owned capability projection for deterministic-fake submission. */
final class DeterministicFakeModelSubmissionCapabilitySource {

    private static final String REQUIRED_CAPABILITY = "deterministic-echo";

    private DeterministicFakeModelSubmissionCapabilitySource() {}

    static String requiredCapability() {
        return REQUIRED_CAPABILITY;
    }
}
