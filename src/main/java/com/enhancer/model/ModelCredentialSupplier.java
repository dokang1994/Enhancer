package com.enhancer.model;

/**
 * The injected credential boundary for provider adapters.
 *
 * <p>There is no default provider: the deterministic fake requires no credential,
 * and only an explicitly injected supplier can hand a provider adapter its secret.
 * The supplied value must never be logged, displayed, persisted, or copied into
 * request or response evidence.
 */
@FunctionalInterface
public interface ModelCredentialSupplier {
    String credential() throws ModelGatewayException;
}
