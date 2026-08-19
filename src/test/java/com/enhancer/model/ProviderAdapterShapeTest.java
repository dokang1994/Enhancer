package com.enhancer.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Modifier;
import org.junit.jupiter.api.Test;

/**
 * Proves the provider adapter shape exists inside its package boundary without ever
 * invoking it: RFC-0013 requires the slice to compile and bound one concrete adapter
 * shape while the deterministic fake remains the only executed gateway.
 */
class ProviderAdapterShapeTest {

    @Test
    void adapterShapeCompilesPackagePrivateBehindTheGatewayPort() throws Exception {
        Class<?> adapter = Class.forName(
                "com.enhancer.model.HttpMessageApiModelProviderAdapter");

        assertFalse(
                Modifier.isPublic(adapter.getModifiers()),
                "the provider adapter shape must stay package-private");
        assertTrue(
                ModelGateway.class.isAssignableFrom(adapter),
                "the provider adapter shape must implement the gateway port");
        assertTrue(
                Modifier.isFinal(adapter.getModifiers()),
                "the provider adapter shape must not be extensible");
    }

    @Test
    void credentialSupplierPortHasNoDefaultProvider() {
        assertTrue(ModelCredentialSupplier.class.isInterface());
        assertEquals(
                0,
                ModelCredentialSupplier.class.getFields().length,
                "the credential supplier port must not carry a default credential");
        long defaultOrStaticMethods = java.util.Arrays
                .stream(ModelCredentialSupplier.class.getMethods())
                .filter(method -> method.isDefault()
                        || Modifier.isStatic(method.getModifiers()))
                .count();
        assertEquals(
                0,
                defaultOrStaticMethods,
                "the credential supplier port must not supply a default provider");
    }
}
