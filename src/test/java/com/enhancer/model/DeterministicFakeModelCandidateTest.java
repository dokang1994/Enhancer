package com.enhancer.model;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

class DeterministicFakeModelCandidateTest {

    @Test
    void bindsTheExactFakeGatewayAndSuppliesFixedRepositoryFacts() {
        DeterministicFakeModelGateway gateway = new DeterministicFakeModelGateway();

        DeterministicFakeModelCandidate candidate =
                DeterministicFakeModelCandidate.bind(gateway);

        assertSame(gateway, candidate.gateway());
        assertEquals("deterministic-fake-v2", candidate.candidateId());
        assertEquals("deterministic-fake", candidate.modelClass());
        assertEquals("deterministic-echo", candidate.requiredCapability());
        assertEquals(
                ModelReasoningRequirement.MINIMAL,
                candidate.maximumReasoningRequirement());
        assertEquals("CLOSED_IN_PROCESS_FAKE", candidate.localityProvenance());
        assertEquals(
                "deterministic-unicode-scalar-v1",
                candidate.tokenSemanticsId());
        assertTrue(candidate.tokenSemanticsAvailable());
        assertEquals(524_288, candidate.maximumContextTokens());
        assertEquals(262_144, candidate.maximumInputTokens());
        assertEquals(262_144, candidate.maximumOutputTokens());
        assertEquals(524_130, candidate.maximumTotalTokens());
        assertFalse(candidate.hasProviderCharge());
        assertEquals(
                ModelDataClassification.PUBLIC,
                candidate.maximumDataClassification());
    }

    @Test
    void rejectsNullGateway() {
        assertThrows(
                NullPointerException.class,
                () -> DeterministicFakeModelCandidate.bind(null));
    }

    @Test
    void remainsAnOpaqueExactFakeBinding() {
        Class<DeterministicFakeModelCandidate> type =
                DeterministicFakeModelCandidate.class;
        assertTrue(Modifier.isPublic(type.getModifiers()));
        assertTrue(Modifier.isFinal(type.getModifiers()));
        assertFalse(type.isRecord());
        assertEquals(Object.class, type.getSuperclass());
        assertArrayEquals(new Class<?>[0], type.getInterfaces());

        Field[] fields = Arrays.stream(type.getDeclaredFields())
                .filter(field -> !Modifier.isStatic(field.getModifiers()))
                .toArray(Field[]::new);
        assertEquals(1, fields.length);
        assertEquals(DeterministicFakeModelGateway.class, fields[0].getType());
        assertTrue(Modifier.isPrivate(fields[0].getModifiers()));
        assertTrue(Modifier.isFinal(fields[0].getModifiers()));

        Constructor<?>[] constructors = type.getDeclaredConstructors();
        assertEquals(1, constructors.length);
        assertTrue(Modifier.isPrivate(constructors[0].getModifiers()));
        assertArrayEquals(
                new Class<?>[] {DeterministicFakeModelGateway.class},
                constructors[0].getParameterTypes());

        Method[] factories = Arrays.stream(type.getDeclaredMethods())
                .filter(method -> Modifier.isPublic(method.getModifiers()))
                .filter(method -> Modifier.isStatic(method.getModifiers()))
                .toArray(Method[]::new);
        assertEquals(1, factories.length);
        assertEquals("bind", factories[0].getName());
        assertEquals(type, factories[0].getReturnType());
        assertArrayEquals(
                new Class<?>[] {DeterministicFakeModelGateway.class},
                factories[0].getParameterTypes());

        assertTrue(Modifier.isFinal(DeterministicFakeModelGateway.class.getModifiers()));
    }
}
