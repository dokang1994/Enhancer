package com.enhancer.maintenance.installation;

import java.util.UUID;
import java.util.Objects;

/** Platform-neutral point store port; it defines no persistence implementation. */
public interface InstallationTransactionStore {
    /** Creates once; exact replay returns the unchanged state and changed reuse conflicts. */
    Mutation create(InstallationTransactionState initial)
            throws InstallationTransactionStoreException;

    /** Resolves exactly one transaction without listing, scanning, or creating it. */
    InstallationTransactionState resolve(UUID transactionId)
            throws InstallationTransactionStoreException;

    /**
     * Persists exactly one valid state transition when the current revision matches.
     * Exact replacement replay returns the unchanged state without another mutation.
     */
    Mutation compareAndExchange(
            UUID transactionId,
            long expectedRevision,
            InstallationTransactionState replacement)
            throws InstallationTransactionStoreException;

    /** Exact write outcome; only fresh mutation grants phase-invocation ownership. */
    record Mutation(
            InstallationTransactionState state,
            MutationDisposition disposition) {
        public Mutation {
            state = Objects.requireNonNull(state, "state must not be null");
            disposition = Objects.requireNonNull(
                    disposition, "disposition must not be null");
        }
    }

    enum MutationDisposition {
        CREATED,
        ADVANCED,
        EXACT_REPLAY
    }
}
