package com.enhancer.runtime;

import java.io.IOException;
import java.util.Optional;

/** Deterministic point persistence for accepted signed cancellation verification audit. */
public interface CancellationAuthorizationAuditStore {
    CancellationAuthorizationAuditRecord persist(
            CancellationAuthorizationAuditRecord audit) throws IOException;

    Optional<CancellationAuthorizationAuditRecord> find(String authorizationId)
            throws IOException;

    CancellationAuthorizationAuditRecord resolve(String reference) throws IOException;
}
