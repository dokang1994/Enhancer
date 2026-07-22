package com.enhancer.runtime;

import com.enhancer.bus.MessageEnvelope;
import java.io.IOException;
import java.time.Instant;

/**
 * Builds the first-use work message for a generated-input submission.
 *
 * <p>It is invoked only on first use, after {@link GeneratedInputSubmissionService} has resolved
 * that no manifest exists and captured the occurrence time. The caller (for example the CLI)
 * captures the governed repository snapshot here, so the clock and repository context are consulted
 * only when the manifest does not already exist. The returned envelope must carry the supplied
 * identities and occurrence time and a {@code WorkPayload} consistent with the request.
 */
@FunctionalInterface
public interface SubmissionEnvelopeFactory {
    MessageEnvelope create(GeneratedSubmissionIdentities identities, Instant occurredAt)
            throws IOException;
}
