package com.enhancer.runtime;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Objects;
import java.util.regex.Pattern;

/** Resolves one explicitly named publication point without scanning or mutating either store. */
public final class FileSystemRuntimeEventPointReader {
    private static final Pattern CANONICAL_POINT_FILE = Pattern.compile(
            "[0-9a-f]{64}\\Q"
                    + FileSystemRuntimeEventPublisher.FILE_SUFFIX
                    + "\\E");

    private final Path publicationRoot;
    private final RuntimeEventStore eventStore;

    public FileSystemRuntimeEventPointReader(
            Path publicationRoot,
            RuntimeEventStore eventStore) {
        Objects.requireNonNull(publicationRoot, "publicationRoot must not be null");
        this.publicationRoot = publicationRoot.toAbsolutePath().normalize();
        this.eventStore = Objects.requireNonNull(
                eventStore, "eventStore must not be null");
    }

    public RuntimeEventPointResolution resolve(String publicationFile)
            throws IOException {
        String checkedFile = canonicalPointFile(publicationFile);
        return resolveRetainedPoint(
                checkedFile,
                publicationRoot.resolve(checkedFile));
    }

    RuntimeEventPointResolution resolveRetainedPoint(
            String publicationFile,
            Path retainedPoint) throws IOException {
        String checkedFile = canonicalPointFile(publicationFile);
        Path point = Objects.requireNonNull(
                        retainedPoint, "retainedPoint must not be null")
                .toAbsolutePath()
                .normalize();
        if (!Files.isDirectory(publicationRoot, LinkOption.NOFOLLOW_LINKS)) {
            throw new IOException(
                    "runtime event publication root must be an existing directory without symbolic links");
        }
        if (!point.getParent().equals(publicationRoot)
                || !Files.isRegularFile(point, LinkOption.NOFOLLOW_LINKS)) {
            throw new IOException(
                    "runtime event publication point must be a regular same-root file without symbolic links");
        }

        RuntimeEventPublicationReference reference =
                FileSystemRuntimeEventPublisher.readAcceptedPoint(point);
        if (!FileSystemRuntimeEventPublisher.pointName(reference)
                .equals(checkedFile)) {
            throw new IOException(
                    "runtime event publication filename does not match its reference");
        }
        String retainedFile = point.getFileName().toString();
        if (!retainedFile.equals(checkedFile)
                && !retainedFile.equals(
                        FileSystemRuntimeEventPublisher.acknowledgedPointName(reference))) {
            throw new IOException(
                    "runtime event publication retained filename is invalid");
        }
        ReferenceIdentity identity = referenceIdentity(reference);
        RuntimeEventStream stream = eventStore.resolve(identity.goalId());
        RuntimeEvent event = stream.events().stream()
                .filter(candidate -> candidate.eventId().equals(identity.eventId()))
                .findFirst()
                .orElseThrow(() -> new IOException(
                        "runtime event publication reference does not resolve to an event"));
        if (!RuntimeEventPublicationReference.from(event).equals(reference)) {
            throw new IOException(
                    "runtime event publication reference does not match the resolved event");
        }
        return new RuntimeEventPointResolution(
                reference,
                event,
                stream.revision());
    }

    static String canonicalPointFile(String publicationFile) {
        String checkedFile = Objects.requireNonNull(
                publicationFile, "publicationFile must not be null");
        if (!CANONICAL_POINT_FILE.matcher(checkedFile).matches()) {
            throw new IllegalArgumentException(
                    "publicationFile must be one canonical runtime-event reference point");
        }
        return checkedFile;
    }

    private ReferenceIdentity referenceIdentity(
            RuntimeEventPublicationReference reference) throws IOException {
        String[] segments = reference.reference().split("/", -1);
        if (segments.length != 3 || !segments[0].equals("runtime-event")) {
            throw new IOException(
                    "runtime event publication reference grammar is invalid");
        }
        try {
            return new ReferenceIdentity(
                    RuntimeIdentity.canonicalUuid(segments[1], "goalId"),
                    RuntimeIdentity.canonicalUuid(segments[2], "eventId"));
        } catch (IllegalArgumentException exception) {
            throw new IOException(
                    "runtime event publication reference identities are invalid",
                    exception);
        }
    }

    private record ReferenceIdentity(String goalId, String eventId) {
    }
}
