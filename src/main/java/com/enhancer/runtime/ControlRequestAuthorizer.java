package com.enhancer.runtime;

import com.enhancer.bus.MessageEnvelope;
import java.io.IOException;

/** Trusted Gate 12 port; envelope metadata alone is never authorization. */
@FunctionalInterface
public interface ControlRequestAuthorizer {
    ControlAuthorizationDecision authorize(MessageEnvelope retainedRequest)
            throws IOException;
}
