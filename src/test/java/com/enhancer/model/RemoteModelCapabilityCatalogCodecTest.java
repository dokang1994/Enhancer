package com.enhancer.model;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class RemoteModelCapabilityCatalogCodecTest {

    @Test
    void validCatalogsDecodeAndReencodeToTheExactCanonicalBytes() {
        for (byte[] bytes : new byte[][] {
                RemoteModelCapabilityCatalogTestFixture.zeroRecords(),
                RemoteModelCapabilityCatalogTestFixture.oneRecord(),
                RemoteModelCapabilityCatalogTestFixture.maximumRecords()
        }) {
            RemoteModelCapabilityCatalogCodec.Decoded decoded = assertInstanceOf(
                    RemoteModelCapabilityCatalogCodec.Decoded.class,
                    RemoteModelCapabilityCatalogCodec.decode(bytes));
            assertArrayEquals(bytes,
                    RemoteModelCapabilityCatalogCodec.encode(decoded.catalog()));
            assertEquals("local-capability-catalog", decoded.catalog().identity());
            assertThrows(UnsupportedOperationException.class,
                    () -> decoded.catalog().records().clear());
        }
    }

    @Test
    void closedFailuresPreserveThePinnedSourceDistinctions() {
        byte[] one = RemoteModelCapabilityCatalogTestFixture.oneRecord();
        assertFailure(new byte[] {(byte) 0xc3, (byte) 0x28},
                RemoteModelCapabilityCatalogCodec.Failure.INVALID_UTF8);
        assertFailure(RemoteModelCapabilityCatalogTestFixture.replace(one,
                        "capability-catalog-v1", "capability-catalog-v2"),
                RemoteModelCapabilityCatalogCodec.Failure.SCHEMA_UNSUPPORTED);
        assertFailure(new String(one, StandardCharsets.UTF_8)
                        .replace("\n", "\r\n").getBytes(StandardCharsets.UTF_8),
                RemoteModelCapabilityCatalogCodec.Failure.NOT_CANONICAL);
        assertFailure(RemoteModelCapabilityCatalogTestFixture.replace(one,
                        "maximumContextTokens=1000", "maximumContextTokens=0"),
                RemoteModelCapabilityCatalogCodec.Failure.CONTENT_INVALID);
    }

    @Test
    void duplicateUnsortedAndInvalidUnselectedRecordsFailWholeCatalog() {
        assertFailure(RemoteModelCapabilityCatalogTestFixture.twoRecords(
                        "candidate-b", "destination-a",
                        "candidate-a", "destination-a"),
                RemoteModelCapabilityCatalogCodec.Failure.CONTENT_INVALID);
        assertFailure(RemoteModelCapabilityCatalogTestFixture.twoRecords(
                        "candidate-a", "destination-a",
                        "candidate-a", "destination-a"),
                RemoteModelCapabilityCatalogCodec.Failure.CONTENT_INVALID);
        byte[] two = RemoteModelCapabilityCatalogTestFixture.twoRecords(
                "candidate-a", "destination-a", "candidate-b", "destination-a");
        assertFailure(RemoteModelCapabilityCatalogTestFixture.replace(two,
                        "record.1.maximumContextTokens=1000",
                        "record.1.maximumContextTokens=0"),
                RemoteModelCapabilityCatalogCodec.Failure.CONTENT_INVALID);
    }

    private static void assertFailure(
            byte[] bytes, RemoteModelCapabilityCatalogCodec.Failure expected) {
        RemoteModelCapabilityCatalogCodec.Refused refused = assertInstanceOf(
                RemoteModelCapabilityCatalogCodec.Refused.class,
                RemoteModelCapabilityCatalogCodec.decode(bytes));
        assertEquals(expected, refused.failure());
    }
}
