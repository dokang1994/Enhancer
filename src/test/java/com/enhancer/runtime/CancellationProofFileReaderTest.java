package com.enhancer.runtime;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CancellationProofFileReaderTest {
    @TempDir
    Path temporaryRoot;

    @Test
    void readsOneExactBoundedProofSnapshot() throws Exception {
        byte[] proof = new byte[] {1, 2, 3, 4};
        Path proofFile = temporaryRoot.resolve("proof.bin").toAbsolutePath().normalize();
        Files.write(proofFile, proof);

        assertArrayEquals(proof, new CancellationProofFileReader().read(proofFile));
    }

    @Test
    void rejectsRelativeMissingEmptyAndOversizedProofs() throws Exception {
        CancellationProofFileReader reader = new CancellationProofFileReader();
        assertThrows(IllegalArgumentException.class,
                () -> reader.read(Path.of("proof.bin")));
        assertThrows(IOException.class, () -> reader.read(
                temporaryRoot.resolve("missing.bin").toAbsolutePath().normalize()));
        Path proofFile = temporaryRoot.resolve("proof.bin").toAbsolutePath().normalize();
        Files.write(proofFile, new byte[0]);
        assertThrows(IOException.class, () -> reader.read(proofFile));
        Files.write(proofFile, new byte[CancellationProofFileReader.MAX_PROOF_BYTES + 1]);
        assertThrows(IOException.class, () -> reader.read(proofFile));
    }
}
