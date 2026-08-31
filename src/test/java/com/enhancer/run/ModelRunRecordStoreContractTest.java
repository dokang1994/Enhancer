package com.enhancer.run;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Comparator;
import org.junit.jupiter.api.Test;

class ModelRunRecordStoreContractTest {

    @Test
    void modelPortUsesDistinctNamesAndLeavesLegacyPortShapeUnchanged() {
        assertArrayEquals(
                new String[] {
                    "persistModel(ModelRunRecord)",
                    "persistModel(String,ModelRunRecord)",
                    "resolveModel(String)"
                },
                signatures(ModelRunRecordStore.class));
        assertArrayEquals(
                new String[] {
                    "persist(RunRecord)",
                    "persist(String,RunRecord)",
                    "recentReferences(int)",
                    "references()",
                    "resolve(String)"
                },
                signatures(RunRecordStore.class));
    }

    @Test
    void defaultCallerSuppliedModelIdentityFailsClosed() {
        ModelRunRecordStore store = new ModelRunRecordStore() {
            @Override
            public StoredRunRecord persistModel(ModelRunRecord record) {
                throw new UnsupportedOperationException();
            }

            @Override
            public ResolvedModelRunRecord resolveModel(String reference) {
                throw new UnsupportedOperationException();
            }
        };

        IOException failure = assertThrows(
                IOException.class,
                () -> store.persistModel(
                        "11111111-1111-1111-1111-111111111111", null));
        assertEquals(
                "this Model RunRecord store does not support caller-supplied identities",
                failure.getMessage());
    }

    private static String[] signatures(Class<?> type) {
        return Arrays.stream(type.getDeclaredMethods())
                .filter(method -> !method.isSynthetic())
                .sorted(Comparator.comparing(ModelRunRecordStoreContractTest::signature))
                .map(ModelRunRecordStoreContractTest::signature)
                .toArray(String[]::new);
    }

    private static String signature(Method method) {
        return method.getName() + "(" + Arrays.stream(method.getParameterTypes())
                .map(Class::getSimpleName)
                .reduce((left, right) -> left + "," + right)
                .orElse("") + ")";
    }
}
