package com.enhancer.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

class DeterministicFakeModelSubmissionCapabilitySourceTest {

    @Test
    void suppliesOnlyTheFixedDeterministicEchoWithoutInput() {
        assertEquals(
                "deterministic-echo",
                DeterministicFakeModelSubmissionCapabilitySource.requiredCapability());
    }

    @Test
    void constructionAndShapeRemainClosed() throws Exception {
        Class<?> type = DeterministicFakeModelSubmissionCapabilitySource.class;
        assertTrue(Modifier.isFinal(type.getModifiers()));
        assertFalse(Modifier.isPublic(type.getModifiers()));
        assertFalse(type.isRecord());
        assertFalse(type.isInterface());
        assertEquals(Object.class, type.getSuperclass());
        assertEquals(0, type.getInterfaces().length);
        assertTrue(Arrays.stream(type.getDeclaredFields())
                .allMatch(field -> Modifier.isStatic(field.getModifiers())));

        Constructor<?>[] constructors = type.getDeclaredConstructors();
        assertEquals(1, constructors.length);
        assertEquals(0, constructors[0].getParameterCount());
        assertTrue(Modifier.isPrivate(constructors[0].getModifiers()));

        Method source = type.getDeclaredMethod("requiredCapability");
        assertEquals(String.class, source.getReturnType());
        assertEquals(0, source.getParameterCount());
        assertTrue(Modifier.isStatic(source.getModifiers()));
        assertFalse(Modifier.isPublic(source.getModifiers()));
    }
}
