package com.enhancer.runtime;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;

/** Reads one untrusted detached cancellation proof without following links. */
public final class CancellationProofFileReader {
    public static final int MAX_PROOF_BYTES = DetachedSignedCancellationGrant.MAX_PROOF_BYTES;

    public byte[] read(Path proofFile) throws IOException {
        Path checked = Objects.requireNonNull(proofFile, "proofFile must not be null");
        if (!checked.isAbsolute() || !checked.equals(checked.normalize())) {
            throw new IllegalArgumentException(
                    "proofFile must be an absolute normalized path");
        }
        InstalledCancellationTrustMetadataLoader.requireExactRegularFile(
                checked, "cancellation proof");
        return InstalledCancellationTrustMetadataLoader.readBoundedSnapshot(
                checked, MAX_PROOF_BYTES, "cancellation proof");
    }
}
