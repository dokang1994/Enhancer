package com.enhancer.model;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Objects;
import java.util.regex.Pattern;

/** Acquires remote-model capability facts from one locally pinned catalog file. */
public final class PinnedFileRemoteModelCandidateCapabilityFactSource {
    private static final int MAX_BYTES = 4 * 1024 * 1024;
    private static final Pattern LABEL = Pattern.compile("[a-z0-9]+(?:-[a-z0-9]+)*");
    private static final Pattern DIGEST = Pattern.compile("[0-9a-f]{64}");

    private final Path catalogFile;
    private final byte[] expectedDigest;

    public PinnedFileRemoteModelCandidateCapabilityFactSource(
            Path catalogFile, String expectedSha256) {
        Path checked = Objects.requireNonNull(catalogFile, "catalogFile must not be null");
        if (!checked.isAbsolute() || !checked.equals(checked.normalize())) {
            throw new IllegalArgumentException("catalogFile must be absolute and normalized");
        }
        Objects.requireNonNull(expectedSha256, "expectedSha256 must not be null");
        if (!DIGEST.matcher(expectedSha256).matches()) {
            throw new IllegalArgumentException(
                    "expectedSha256 must be a lowercase SHA-256 digest");
        }
        this.catalogFile = checked;
        this.expectedDigest = HexFormat.of().parseHex(expectedSha256);
    }

    public RemoteModelCandidateCapabilityFactSourceResult acquire(
            String candidateIdentity, String destinationIdentity, Instant evaluationTime) {
        programmingLabel(candidateIdentity, "candidateIdentity");
        programmingLabel(destinationIdentity, "destinationIdentity");
        Objects.requireNonNull(evaluationTime, "evaluationTime must not be null");

        BasicFileAttributes before;
        try {
            Path parent = catalogFile.getParent();
            if (parent == null
                    || !Files.isDirectory(parent, LinkOption.NOFOLLOW_LINKS)
                    || !parent.toRealPath().equals(parent)
                    || Files.isSymbolicLink(catalogFile)
                    || !Files.isRegularFile(catalogFile, LinkOption.NOFOLLOW_LINKS)
                    || !catalogFile.toRealPath().equals(catalogFile)) {
                return refusal(Reason.UNSAFE);
            }
            before = Files.readAttributes(catalogFile, BasicFileAttributes.class,
                    LinkOption.NOFOLLOW_LINKS);
        } catch (IOException | SecurityException exception) {
            return refusal(Reason.UNSAFE);
        }
        if (before.size() < 1 || before.size() > MAX_BYTES) {
            return refusal(Reason.SIZE);
        }

        byte[] bytes;
        try (FileChannel channel = FileChannel.open(catalogFile,
                StandardOpenOption.READ, LinkOption.NOFOLLOW_LINKS)) {
            long size = channel.size();
            if (size < 1 || size > MAX_BYTES) {
                return refusal(Reason.SIZE);
            }
            if (size != before.size()) {
                return refusal(Reason.CHANGED);
            }
            bytes = readDeclaredBytes(channel, size);
            BasicFileAttributes after = Files.readAttributes(catalogFile,
                    BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
            if (before.size() != after.size()
                    || !Objects.equals(before.fileKey(), after.fileKey())
                    || !before.creationTime().equals(after.creationTime())
                    || !before.lastModifiedTime().equals(after.lastModifiedTime())
                    || Files.isSymbolicLink(catalogFile)
                    || !catalogFile.toRealPath().equals(catalogFile)) {
                return refusal(Reason.CHANGED);
            }
        } catch (IOException | SecurityException exception) {
            return refusal(Reason.CHANGED);
        }
        if (!MessageDigest.isEqual(expectedDigest, sha256(bytes))) {
            return refusal(Reason.PIN);
        }

        RemoteModelCapabilityCatalogCodec.DecodeResult decoded =
                RemoteModelCapabilityCatalogCodec.decode(bytes);
        if (decoded instanceof RemoteModelCapabilityCatalogCodec.Refused refused) {
            return new RemoteModelCandidateCapabilityFactSourceResult.Refused(
                    codecRefusal(refused.failure()));
        }
        RemoteModelCapabilityCatalogCodec.Catalog catalog =
                ((RemoteModelCapabilityCatalogCodec.Decoded) decoded).catalog();
        RemoteModelCapabilityCatalogCodec.CapabilityRecord selected =
                catalog.records().stream()
                .filter(record -> record.candidate().equals(candidateIdentity)
                        && record.destination().equals(destinationIdentity))
                .findFirst().orElse(null);
        if (selected == null) {
            return new RemoteModelCandidateCapabilityFactSourceResult.Refused(
                    RemoteModelCandidateCapabilityFactSourceRefusalReason.RECORD_NOT_FOUND);
        }
        if (evaluationTime.isBefore(selected.observed())
                || !evaluationTime.isBefore(selected.expires())) {
            return new RemoteModelCandidateCapabilityFactSourceResult.Refused(
                    RemoteModelCandidateCapabilityFactSourceRefusalReason.RECORD_STALE);
        }
        RemoteModelCandidateCapabilityFacts facts =
                RemoteModelCandidateCapabilityFacts.create(
                        RemoteModelCandidateCapabilityFacts.SCHEMA_VERSION,
                        catalog.identity(),
                        HexFormat.of().formatHex(expectedDigest),
                        selected.evidence(),
                        selected.observed(),
                        selected.expires(),
                        selected.candidate(),
                        selected.destination(),
                        selected.modelClass(),
                        selected.capability(),
                        "profile-token-budget-v1",
                        selected.schedule(),
                        new ModelTokenBudget(selected.basisInput(), selected.basisOutput(),
                                selected.basisTotal()),
                        ModelReasoningRequirement.valueOf(selected.reasoning()),
                        selected.context(),
                        new ModelTokenBudget(selected.maximumInput(),
                                selected.maximumOutput(), selected.maximumTotal()),
                        Duration.ofMillis(selected.millis()),
                        selected.currency(),
                        selected.cost(),
                        ModelDataClassification.valueOf(selected.classification()));
        return new RemoteModelCandidateCapabilityFactSourceResult.Acquired(facts);
    }

    static byte[] readDeclaredBytes(SeekableByteChannel channel, long size)
            throws IOException {
        if (size < 0 || size > Integer.MAX_VALUE) {
            throw new IOException("catalog changed during read");
        }
        byte[] bytes = new byte[(int) size];
        ByteBuffer target = ByteBuffer.wrap(bytes);
        while (target.hasRemaining()) {
            if (channel.read(target) <= 0) {
                throw new IOException("catalog changed during read");
            }
        }
        if (channel.read(ByteBuffer.allocate(1)) >= 0 || channel.size() != size) {
            throw new IOException("catalog changed during read");
        }
        return bytes;
    }

    private static void programmingLabel(String value, String name) {
        Objects.requireNonNull(value, name + " must not be null");
        if (value.length() > 256 || !LABEL.matcher(value).matches()) {
            throw new IllegalArgumentException(
                    name + " must be a bounded stable lowercase label");
        }
    }

    private static byte[] sha256(byte[] bytes) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(bytes);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 unavailable", exception);
        }
    }

    private static RemoteModelCandidateCapabilityFactSourceResult refusal(Reason reason) {
        return new RemoteModelCandidateCapabilityFactSourceResult.Refused(reason.value);
    }

    private static RemoteModelCandidateCapabilityFactSourceRefusalReason codecRefusal(
            RemoteModelCapabilityCatalogCodec.Failure failure) {
        return switch (failure) {
            case INVALID_UTF8 -> RemoteModelCandidateCapabilityFactSourceRefusalReason
                    .CATALOG_INVALID_UTF8;
            case SCHEMA_UNSUPPORTED ->
                    RemoteModelCandidateCapabilityFactSourceRefusalReason
                            .CATALOG_SCHEMA_UNSUPPORTED;
            case NOT_CANONICAL -> RemoteModelCandidateCapabilityFactSourceRefusalReason
                    .CATALOG_NOT_CANONICAL;
            case CONTENT_INVALID -> RemoteModelCandidateCapabilityFactSourceRefusalReason
                    .CATALOG_CONTENT_INVALID;
        };
    }

    private enum Reason {
        UNSAFE(RemoteModelCandidateCapabilityFactSourceRefusalReason
                .CATALOG_UNAVAILABLE_OR_UNSAFE),
        SIZE(RemoteModelCandidateCapabilityFactSourceRefusalReason
                .CATALOG_OUTSIDE_SIZE_BOUND),
        CHANGED(RemoteModelCandidateCapabilityFactSourceRefusalReason
                .CATALOG_CHANGED_DURING_READ),
        PIN(RemoteModelCandidateCapabilityFactSourceRefusalReason.CATALOG_PIN_MISMATCH);

        private final RemoteModelCandidateCapabilityFactSourceRefusalReason value;

        Reason(RemoteModelCandidateCapabilityFactSourceRefusalReason value) {
            this.value = value;
        }
    }
}
