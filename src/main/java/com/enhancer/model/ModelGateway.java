package com.enhancer.model;

/**
 * The provider-neutral model invocation port.
 *
 * <p>Implementations map one bounded {@link ModelRequest} to one bounded
 * {@link ModelResponse} or fail with one typed {@link ModelGatewayException}.
 * Provider wire formats never leak through this port.
 */
public interface ModelGateway {
    ModelResponse invoke(ModelRequest request) throws ModelGatewayException;
}
