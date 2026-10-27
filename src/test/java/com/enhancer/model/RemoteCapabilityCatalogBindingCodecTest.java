package com.enhancer.model;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class RemoteCapabilityCatalogBindingCodecTest {
    private static final String DIGEST = "1".repeat(64);

    @TempDir
    Path temporaryRoot;

    @Test
    void fixedIdentitiesAndCanonicalBindingRoundTripExactly() {
        Path directory = temporaryRoot.toAbsolutePath().normalize()
                .resolve(RemoteCapabilityCatalogBindingCodec.CATALOG_DIRECTORY_NAME);
        RemoteCapabilityCatalogBinding binding =
                RemoteCapabilityCatalogBindingCodec.create(directory, DIGEST);
        String expected = "enhancer-installed-remote-capability-catalog-v1\n"
                + "catalogPath=" + binding.catalogPath() + "\n"
                + "catalogSha256=" + DIGEST + "\n";

        assertEquals("enhancer-remote-capability-catalog-binding-v1",
                RemoteCapabilityCatalogBindingCodec.BINDING_NAME);
        assertEquals("enhancer-remote-capability-catalogs-v1",
                RemoteCapabilityCatalogBindingCodec.CATALOG_DIRECTORY_NAME);
        assertEquals("enhancer-remote-capability-catalog-maintenance-v1.lock",
                RemoteCapabilityCatalogBindingCodec.LOCK_NAME);
        assertArrayEquals(expected.getBytes(StandardCharsets.UTF_8),
                binding.canonicalBytes());
        assertEquals(RemoteModelCapabilityCatalogTestFixture.sha256(
                binding.canonicalBytes()), binding.bindingSha256());

        RemoteCapabilityCatalogBindingCodec.Decoded decoded = assertInstanceOf(
                RemoteCapabilityCatalogBindingCodec.Decoded.class,
                RemoteCapabilityCatalogBindingCodec.decode(
                        directory, binding.canonicalBytes()));
        assertEquals(binding, decoded.binding());
    }

    @Test
    void bindingDecodeRejectsEveryNoncanonicalOrEscapingShape() {
        Path directory = temporaryRoot.toAbsolutePath().normalize()
                .resolve(RemoteCapabilityCatalogBindingCodec.CATALOG_DIRECTORY_NAME);
        byte[] canonical = RemoteCapabilityCatalogBindingCodec.create(
                directory, DIGEST).canonicalBytes();
        String text = new String(canonical, StandardCharsets.UTF_8);
        for (byte[] invalid : new byte[][] {
                new byte[0],
                ("\uFEFF" + text).getBytes(StandardCharsets.UTF_8),
                text.replace("\n", "\r\n").getBytes(StandardCharsets.UTF_8),
                text.substring(0, text.length() - 1).getBytes(StandardCharsets.UTF_8),
                (text + "extra=value\n").getBytes(StandardCharsets.UTF_8),
                text.replace("catalogSha256=" + DIGEST,
                        "catalogSha256=" + "A".repeat(64))
                        .getBytes(StandardCharsets.UTF_8),
                text.replace(DIGEST + ".conf", "other.conf")
                        .getBytes(StandardCharsets.UTF_8),
                new byte[4097]
        }) {
            assertInstanceOf(RemoteCapabilityCatalogBindingCodec.Refused.class,
                    RemoteCapabilityCatalogBindingCodec.decode(directory, invalid));
        }

        Path otherDirectory = temporaryRoot.toAbsolutePath().normalize()
                .resolve("other-directory");
        assertInstanceOf(RemoteCapabilityCatalogBindingCodec.Refused.class,
                RemoteCapabilityCatalogBindingCodec.decode(otherDirectory, canonical));
        assertThrows(IllegalArgumentException.class,
                () -> RemoteCapabilityCatalogBindingCodec.create(
                        Path.of("relative-directory"), DIGEST));
    }

    @Test
    void bindingDefensivelyCopiesBytesAndRedactsPathAndDigests() {
        Path directory = temporaryRoot.toAbsolutePath().normalize()
                .resolve(RemoteCapabilityCatalogBindingCodec.CATALOG_DIRECTORY_NAME);
        RemoteCapabilityCatalogBinding binding =
                RemoteCapabilityCatalogBindingCodec.create(directory, DIGEST);
        byte[] first = binding.canonicalBytes();
        first[0] ^= 1;
        assertFalse(java.util.Arrays.equals(first, binding.canonicalBytes()));
        assertFalse(binding.toString().contains(directory.toString()));
        assertFalse(binding.toString().contains(DIGEST));
        assertFalse(binding.toString().contains(binding.bindingSha256()));
    }
}
