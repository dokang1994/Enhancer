package com.enhancer.runtime;

/**
 * External invocation port. Implementations own operation payload and credentials; neither
 * enters the durable ledger through this interface.
 */
public interface ExternalEffectAdapter {
    String adapterId();

    String operationSha256();

    ExternalEffectAdapterResult invoke(String idempotencyKey)
            throws ExternalEffectAdapterException;
}
