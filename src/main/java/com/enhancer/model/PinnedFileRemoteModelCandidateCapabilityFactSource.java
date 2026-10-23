package com.enhancer.model;

import java.nio.file.Path;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Objects;
import java.util.regex.Pattern;

/** Acquires remote-model capability facts from one locally pinned catalog file. */
public final class PinnedFileRemoteModelCandidateCapabilityFactSource {

    private static final int MAX_IDENTITY_CHARACTERS = 256;
    private static final Pattern STABLE_LABEL =
            Pattern.compile("[a-z0-9]+(?:-[a-z0-9]+)*");
    private static final Pattern SHA_256 = Pattern.compile("[0-9a-f]{64}");

    private final Path catalogFile;
    private final byte[] expectedSha256;

    public PinnedFileRemoteModelCandidateCapabilityFactSource(
            Path catalogFile,
            String expectedSha256) {
        Path checkedCatalogFile = Objects.requireNonNull(
                catalogFile, "catalogFile must not be null");
        if (!checkedCatalogFile.isAbsolute()
                || !checkedCatalogFile.equals(checkedCatalogFile.normalize())) {
            throw new IllegalArgumentException(
                    "catalogFile must be absolute and normalized");
        }
        String checkedDigest = Objects.requireNonNull(
                expectedSha256, "expectedSha256 must not be null");
        if (!SHA_256.matcher(checkedDigest).matches()) {
            throw new IllegalArgumentException(
                    "expectedSha256 must be a lowercase SHA-256 digest");
        }
        this.catalogFile = checkedCatalogFile;
        this.expectedSha256 = HexFormat.of().parseHex(checkedDigest);
    }

    public RemoteModelCandidateCapabilityFactSourceResult acquire(
            String candidateIdentity,
            String destinationIdentity,
            Instant evaluationTime) {
        stableLabel(candidateIdentity, "candidateIdentity");
        stableLabel(destinationIdentity, "destinationIdentity");
        Objects.requireNonNull(evaluationTime, "evaluationTime must not be null");

        return new RemoteModelCandidateCapabilityFactSourceResult.Refused(
                RemoteModelCandidateCapabilityFactSourceRefusalReason
                        .CATALOG_UNAVAILABLE_OR_UNSAFE);
    }

    private static void stableLabel(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        if (value.length() > MAX_IDENTITY_CHARACTERS
                || !STABLE_LABEL.matcher(value).matches()) {
            throw new IllegalArgumentException(
                    fieldName + " must be a bounded stable lowercase label");
        }
    }
}
