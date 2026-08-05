package com.enhancer.runtime;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Objects;

/** Acknowledges one exact resolved point without scanning, deleting, or applying its event. */
public final class FileSystemRuntimeEventPointAcknowledger {
    private final Path publicationRoot;
    private final FileSystemRuntimeEventPointReader reader;

    public FileSystemRuntimeEventPointAcknowledger(
            Path publicationRoot,
            RuntimeEventStore eventStore) {
        Objects.requireNonNull(publicationRoot, "publicationRoot must not be null");
        this.publicationRoot = publicationRoot.toAbsolutePath().normalize();
        this.reader = new FileSystemRuntimeEventPointReader(
                this.publicationRoot,
                Objects.requireNonNull(eventStore, "eventStore must not be null"));
    }

    public RuntimeEventPointAcknowledgement acknowledge(String publicationFile)
            throws IOException {
        String checkedFile =
                FileSystemRuntimeEventPointReader.canonicalPointFile(publicationFile);
        if (!Files.isDirectory(publicationRoot, LinkOption.NOFOLLOW_LINKS)) {
            throw new IOException(
                    "runtime event publication root must be an existing directory without symbolic links");
        }
        Path pending = publicationRoot.resolve(checkedFile).normalize();
        String acknowledgedFile = checkedFile.substring(
                        0,
                        checkedFile.length()
                                - FileSystemRuntimeEventPublisher.FILE_SUFFIX.length())
                + FileSystemRuntimeEventPublisher.ACKNOWLEDGED_FILE_SUFFIX;
        Path acknowledged = publicationRoot.resolve(acknowledgedFile).normalize();
        if (!pending.getParent().equals(publicationRoot)
                || !acknowledged.getParent().equals(publicationRoot)) {
            throw new IOException(
                    "runtime event publication point must remain under its explicit root");
        }
        boolean pendingExists = Files.exists(pending, LinkOption.NOFOLLOW_LINKS);
        boolean acknowledgedExists =
                Files.exists(acknowledged, LinkOption.NOFOLLOW_LINKS);
        if (pendingExists == acknowledgedExists) {
            throw new IOException(
                    "runtime event publication must resolve exactly one pending or acknowledged point");
        }

        Path retained = pendingExists ? pending : acknowledged;
        RuntimeEventPointResolution resolution =
                reader.resolveRetainedPoint(checkedFile, retained);
        if (acknowledgedExists) {
            return new RuntimeEventPointAcknowledgement(
                    RuntimeEventPointAcknowledgementStatus.ALREADY_ACKNOWLEDGED,
                    acknowledgedFile,
                    resolution);
        }
        if (Files.exists(acknowledged, LinkOption.NOFOLLOW_LINKS)) {
            throw new IOException(
                    "acknowledged runtime event point appeared before acknowledgement");
        }
        try {
            Files.move(
                    pending,
                    acknowledged,
                    StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException exception) {
            throw new IOException(
                    "runtime event acknowledgement requires atomic move support",
                    exception);
        }
        return new RuntimeEventPointAcknowledgement(
                RuntimeEventPointAcknowledgementStatus.ACKNOWLEDGED,
                acknowledgedFile,
                resolution);
    }
}
