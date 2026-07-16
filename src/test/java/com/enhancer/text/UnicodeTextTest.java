package com.enhancer.text;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class UnicodeTextTest {
    @Test
    void truncatesPrefixesAndSuffixesWithoutSplittingSurrogatePairs() {
        String prefixBoundary = "x".repeat(4) + "\uD83D\uDE80" + "tail";
        String suffixBoundary = "head" + "\uD83D\uDE80" + "x".repeat(4);

        assertEquals("xxxx", UnicodeText.prefix(prefixBoundary, 5));
        assertEquals("xxxx", UnicodeText.suffix(suffixBoundary, 5));
        assertEquals(prefixBoundary, UnicodeText.prefix(prefixBoundary, 20));
        assertEquals(suffixBoundary, UnicodeText.suffix(suffixBoundary, 20));
    }
}
