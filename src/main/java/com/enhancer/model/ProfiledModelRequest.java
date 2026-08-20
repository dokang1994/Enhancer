package com.enhancer.model;

import java.util.Objects;

/**
 * One immutable aligned pair of a model request and its complete execution profile.
 *
 * <p>This value proves only intrinsic model-class and invocation-time alignment. It
 * performs no policy evaluation and grants no task, Tool, provider, network,
 * transmission, credential, or spend authority.
 */
public record ProfiledModelRequest(
        ModelRequest request,
        ModelExecutionProfile executionProfile) {

    public ProfiledModelRequest {
        Objects.requireNonNull(request, "request must not be null");
        Objects.requireNonNull(executionProfile, "executionProfile must not be null");

        if (!request.modelClass().equals(executionProfile.modelClass())) {
            throw new IllegalArgumentException(
                    "request and executionProfile modelClass values must match");
        }
        if (executionProfile.maximumInvocationTime().compareTo(request.timeout()) > 0) {
            throw new IllegalArgumentException(
                    "executionProfile maximumInvocationTime must not exceed request timeout");
        }
    }
}
