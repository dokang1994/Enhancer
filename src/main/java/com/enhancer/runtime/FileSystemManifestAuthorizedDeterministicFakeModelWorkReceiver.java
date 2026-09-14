package com.enhancer.runtime;

import com.enhancer.bus.FileSpoolMessageTransport;
import com.enhancer.bus.TransportMessage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Objects;

/** Point-resolved filesystem composition for manifest-authorized typed ModelWork receive. */
public final class FileSystemManifestAuthorizedDeterministicFakeModelWorkReceiver {
    private static final String ACKNOWLEDGED_SUFFIX = ".received";

    private final Path spoolRoot;
    private final SubmissionManifestStore manifestStore;
    private final SchedulerQueueStore queueStore;
    private final Acknowledger acknowledger;

    public FileSystemManifestAuthorizedDeterministicFakeModelWorkReceiver(
            Path transportSpoolRoot,
            Path submissionRoot,
            Path queueRoot) {
        this(
                transportSpoolRoot,
                new FileSystemSubmissionManifestStore(submissionRoot),
                new FileSystemSchedulerQueueStore(queueRoot),
                FileSystemManifestAuthorizedDeterministicFakeModelWorkReceiver
                        ::moveAtomically);
    }

    FileSystemManifestAuthorizedDeterministicFakeModelWorkReceiver(
            Path transportSpoolRoot,
            SubmissionManifestStore manifestStore,
            SchedulerQueueStore queueStore,
            Acknowledger acknowledger) {
        this.spoolRoot = Objects.requireNonNull(
                transportSpoolRoot, "transportSpoolRoot must not be null")
                .toAbsolutePath().normalize();
        this.manifestStore = Objects.requireNonNull(
                manifestStore, "manifestStore must not be null");
        this.queueStore = Objects.requireNonNull(queueStore, "queueStore must not be null");
        this.acknowledger = Objects.requireNonNull(
                acknowledger, "acknowledger must not be null");
    }

    public ManifestAuthorizedModelWorkPointReceiveResult receive(String messageFile)
            throws IOException {
        String canonicalFile = canonicalTransportFile(messageFile);
        Point point = resolvePoint(canonicalFile);
        TransportMessage message = FileSpoolMessageTransport.read(point.messagePath());
        DurableWorkMessageReceiveResult admission =
                new ManifestAuthorizedDeterministicFakeModelWorkReceiver(
                        manifestStore, queueStore).receive(message);
        String spoolStatus = acknowledge(point);
        return new ManifestAuthorizedModelWorkPointReceiveResult(
                admission,
                spoolStatus,
                message.envelope().messageId(),
                message.envelope().messageId(),
                point.acknowledgedPath().getFileName().toString());
    }

    private Point resolvePoint(String messageFile) throws IOException {
        requireSafeRoot();
        Path pending = spoolRoot.resolve(messageFile).normalize();
        Path acknowledged = spoolRoot.resolve(
                messageFile.substring(0,
                        messageFile.length() - FileSpoolMessageTransport.FILE_SUFFIX.length())
                        + ACKNOWLEDGED_SUFFIX).normalize();
        require(pending.getParent().equals(spoolRoot)
                        && acknowledged.getParent().equals(spoolRoot),
                "messageFile must name a same-root transport point");
        boolean pendingExists = Files.exists(pending, LinkOption.NOFOLLOW_LINKS);
        boolean acknowledgedExists = Files.exists(
                acknowledged, LinkOption.NOFOLLOW_LINKS);
        require(pendingExists != acknowledgedExists,
                "messageFile must resolve exactly one pending or acknowledged point");
        Path selected = pendingExists ? pending : acknowledged;
        require(!Files.isSymbolicLink(selected)
                        && Files.isRegularFile(selected, LinkOption.NOFOLLOW_LINKS),
                "messageFile must resolve to a regular non-symbolic point");
        require(selected.toRealPath().getParent().equals(spoolRoot),
                "messageFile must remain inside the real spool root");
        return new Point(selected, acknowledged, acknowledgedExists);
    }

    private void requireSafeRoot() throws IOException {
        require(!Files.isSymbolicLink(spoolRoot)
                        && Files.isDirectory(spoolRoot, LinkOption.NOFOLLOW_LINKS),
                "transport spool root must be a regular directory");
        require(spoolRoot.toRealPath().equals(spoolRoot),
                "transport spool root must not use link or reparse indirection");
    }

    private String acknowledge(Point point) throws IOException {
        if (point.acknowledged()) {
            return "ALREADY_ACKNOWLEDGED";
        }
        if (Files.exists(point.acknowledgedPath(), LinkOption.NOFOLLOW_LINKS)) {
            throw new IOException("acknowledged point appeared before acknowledgement");
        }
        acknowledger.acknowledge(point.messagePath(), point.acknowledgedPath());
        return "ACKNOWLEDGED";
    }

    private static void moveAtomically(Path pending, Path acknowledged)
            throws IOException {
        Files.move(pending, acknowledged, StandardCopyOption.ATOMIC_MOVE);
    }

    private static String canonicalTransportFile(String value) {
        Objects.requireNonNull(value, "messageFile must not be null");
        String suffix = FileSpoolMessageTransport.FILE_SUFFIX;
        require(value.endsWith(suffix),
                "messageFile must be a canonical UUID transport filename");
        String identity = value.substring(0, value.length() - suffix.length());
        require(identity.equals(java.util.UUID.fromString(identity).toString()),
                "messageFile must be a canonical UUID transport filename");
        return value;
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalArgumentException(message);
        }
    }

    private record Point(
            Path messagePath,
            Path acknowledgedPath,
            boolean acknowledged) {
    }

    @FunctionalInterface
    interface Acknowledger {
        void acknowledge(Path pending, Path acknowledged) throws IOException;
    }
}
