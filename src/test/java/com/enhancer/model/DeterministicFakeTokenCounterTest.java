package com.enhancer.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class DeterministicFakeTokenCounterTest {

    private final DeterministicFakeTokenCounter counter =
            new DeterministicFakeTokenCounter();

    @Test
    void countsExactlyTheSuppliedWellFormedUnicodeScalars() {
        assertEquals(0, counter.count(""));
        assertEquals(1, counter.count("\n"));
        assertEquals(2, counter.count("\r\n"));
        assertEquals(1, counter.count("A"));
        assertEquals(1, counter.count("\uFEFF"));
        assertEquals(1, counter.count("\u00E9"));
        assertEquals(2, counter.count("e\u0301"));
        assertEquals(1, counter.count("\u0301"));
        assertEquals(1, counter.count("\uD83D\uDE00"));
        assertEquals(3, counter.count("A\uD83D\uDE00B"));
        assertEquals(2, counter.count("\uD83D\uDE00\uD83D\uDE80"));
    }

    @Test
    void rejectsNullAndEveryMalformedSurrogatePositionWithoutEchoingInput() {
        assertThrows(NullPointerException.class, () -> counter.count(null));

        List<String> malformed = List.of(
                "\uD800",
                "\uDC00",
                "\uD800A",
                "\uD800\uD800",
                "\uDC00\uDC00",
                "\uDC00\uD800",
                "A\uDC00",
                "\uD800Avalid",
                "valid\uD800A",
                "prefix\uDC00suffix");
        for (String value : malformed) {
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> counter.count(value));
            assertFalse(exception.getMessage().contains(value));
        }
    }

    @Test
    void checkedResponseAlgebraMatchesTheExactFakeAcrossDigitBoundaries()
            throws Exception {
        DeterministicFakeModelGateway gateway = new DeterministicFakeModelGateway();
        for (int length : new int[] {9, 10, 99, 100, 999, 1_000, 9_999,
                10_000, 99_999, 100_000}) {
            String prompt = "a".repeat(length);
            ModelResponse response = gateway.invoke(request(prompt));

            assertEquals(
                    response.text().length(),
                    DeterministicFakeTokenCounter.responseUtf16Length(prompt.length()));
            assertEquals(
                    counter.count(response.text()),
                    DeterministicFakeTokenCounter.responseTokenCount(
                            prompt.length(), counter.count(prompt)));
        }

        String supplementaryPrompt = "A\uD83D\uDE00B";
        ModelResponse supplementaryResponse = gateway.invoke(request(supplementaryPrompt));
        assertEquals(
                supplementaryResponse.text().length(),
                DeterministicFakeTokenCounter.responseUtf16Length(
                        supplementaryPrompt.length()));
        assertEquals(
                counter.count(supplementaryResponse.text()),
                DeterministicFakeTokenCounter.responseTokenCount(
                        supplementaryPrompt.length(),
                        counter.count(supplementaryPrompt)));
        assertFalse(supplementaryResponse.usage().inputUnits()
                == counter.count(supplementaryPrompt));
    }

    @Test
    void provesTheTightSuccessfulAndRefusedAsciiBoundaries() throws Exception {
        String largestSuccessfulPrompt = "a".repeat(261_986);
        long successfulOutput = DeterministicFakeTokenCounter.responseTokenCount(
                largestSuccessfulPrompt.length(),
                counter.count(largestSuccessfulPrompt));

        assertEquals(262_144, successfulOutput);
        assertEquals(
                524_130,
                Math.addExact(counter.count(largestSuccessfulPrompt), successfulOutput));
        assertEquals(
                262_144,
                new DeterministicFakeModelGateway()
                        .invoke(request(largestSuccessfulPrompt))
                        .text()
                        .length());

        String refusedPrompt = "a".repeat(261_987);
        assertEquals(
                262_145,
                DeterministicFakeTokenCounter.responseUtf16Length(refusedPrompt.length()));
        ModelGatewayException exception = assertThrows(
                ModelGatewayException.class,
                () -> new DeterministicFakeModelGateway().invoke(request(refusedPrompt)));
        assertEquals(ModelFailureCode.BUDGET_EXCEEDED, exception.code());
    }

    @Test
    void responseDerivationRejectsInvalidCountsAndOverflows() {
        assertThrows(
                IllegalArgumentException.class,
                () -> DeterministicFakeTokenCounter.responseUtf16Length(-1));
        assertThrows(
                IllegalArgumentException.class,
                () -> DeterministicFakeTokenCounter.responseTokenCount(0, -1));
        assertThrows(
                ArithmeticException.class,
                () -> DeterministicFakeTokenCounter.responseUtf16Length(Long.MAX_VALUE));
        assertThrows(
                ArithmeticException.class,
                () -> DeterministicFakeTokenCounter.responseTokenCount(0, Long.MAX_VALUE));
    }

    @Test
    void exposesOneFieldFreeCounterAndPackagePrivateCheckedAlgebra() throws Exception {
        Class<DeterministicFakeTokenCounter> type = DeterministicFakeTokenCounter.class;
        assertTrue(Modifier.isPublic(type.getModifiers()));
        assertTrue(Modifier.isFinal(type.getModifiers()));
        Field[] instanceFields = Arrays.stream(type.getDeclaredFields())
                .filter(field -> !Modifier.isStatic(field.getModifiers()))
                .toArray(Field[]::new);
        assertEquals(0, instanceFields.length);

        Method[] publicMethods = Arrays.stream(type.getDeclaredMethods())
                .filter(method -> Modifier.isPublic(method.getModifiers()))
                .toArray(Method[]::new);
        assertEquals(1, publicMethods.length);
        Method count = type.getDeclaredMethod("count", String.class);
        assertEquals(long.class, count.getReturnType());
        assertFalse(Modifier.isStatic(count.getModifiers()));

        assertFalse(Modifier.isPublic(type.getDeclaredMethod(
                "responseUtf16Length", long.class).getModifiers()));
        assertFalse(Modifier.isPublic(type.getDeclaredMethod(
                "responseTokenCount", long.class, long.class).getModifiers()));
    }

    private static ModelRequest request(String prompt) {
        return new ModelRequest(
                "token-counter-test",
                prompt,
                "deterministic-fake",
                Duration.ofSeconds(2),
                ModelRequest.MAX_RESPONSE_LENGTH);
    }
}
