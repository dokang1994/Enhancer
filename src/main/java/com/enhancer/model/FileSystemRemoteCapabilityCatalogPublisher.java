package com.enhancer.model;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Objects;

/** Test-composed filesystem implementation of the bounded RFC-0032 publisher. */
final class FileSystemRemoteCapabilityCatalogPublisher
        implements RemoteCapabilityCatalogPublisher {
    private static final long MAX_CATALOG_BYTES = 4L * 1024 * 1024;
    private static final long MAX_BINDING_BYTES = 4L * 1024;

    private final String installationIdentity;
    private final Path root;
    private final Path catalogs;
    private final Path bindingPath;
    private final Path lockPath;
    private final RemoteCapabilityCatalogCandidateLocator locator;
    private final RemoteCapabilityCatalogEvidenceVerifier verifier;
    private final RemoteCapabilityCatalogPublicationFaultInjector faults;

    FileSystemRemoteCapabilityCatalogPublisher(
            String installationIdentity,
            Path installationRoot,
            RemoteCapabilityCatalogCandidateLocator locator,
            RemoteCapabilityCatalogEvidenceVerifier verifier,
            RemoteCapabilityCatalogPublicationFaultInjector faults) {
        this.installationIdentity = RemoteCapabilityCatalogPublicationValues.identity(
                installationIdentity, "installationIdentity");
        root = Objects.requireNonNull(
                installationRoot, "installationRoot must not be null");
        if (!root.isAbsolute() || !root.equals(root.normalize())) {
            throw new IllegalArgumentException(
                    "installationRoot must be absolute and normalized");
        }
        catalogs = root.resolve(
                RemoteCapabilityCatalogBindingCodec.CATALOG_DIRECTORY_NAME);
        bindingPath = root.resolve(RemoteCapabilityCatalogBindingCodec.BINDING_NAME);
        lockPath = root.resolve(RemoteCapabilityCatalogBindingCodec.LOCK_NAME);
        this.locator = Objects.requireNonNull(locator, "locator must not be null");
        this.verifier = Objects.requireNonNull(verifier, "verifier must not be null");
        this.faults = Objects.requireNonNull(faults, "faults must not be null");
    }

    @Override
    public RemoteCapabilityCatalogPublicationResult publish(
            RemoteCapabilityCatalogPublicationIntent intent) {
        Objects.requireNonNull(intent, "intent must not be null");
        if (!installationIdentity.equals(intent.installationIdentity()) || !safeRoot()) {
            return refused(intent, RemoteCapabilityCatalogPublicationRefusalReason
                    .INSTALLATION_ROOT_UNAVAILABLE_OR_UNSAFE);
        }
        try (FileChannel channel = FileChannel.open(
                lockPath,
                StandardOpenOption.CREATE,
                StandardOpenOption.WRITE,
                LinkOption.NOFOLLOW_LINKS)) {
            FileLock attempted;
            try {
                attempted = channel.tryLock();
            } catch (OverlappingFileLockException exception) {
                attempted = null;
            }
            if (attempted == null) {
                return refused(intent, RemoteCapabilityCatalogPublicationRefusalReason
                        .MAINTENANCE_LOCK_UNAVAILABLE_OR_CONTENDED);
            }
            try (FileLock acquired = attempted) {
                if (!acquired.isValid()) {
                    return refused(intent, RemoteCapabilityCatalogPublicationRefusalReason
                            .MAINTENANCE_LOCK_UNAVAILABLE_OR_CONTENDED);
                }
                return publishLocked(intent);
            }
        } catch (IOException | SecurityException exception) {
            return refused(intent, RemoteCapabilityCatalogPublicationRefusalReason
                    .MAINTENANCE_LOCK_UNAVAILABLE_OR_CONTENDED);
        }
    }

    private RemoteCapabilityCatalogPublicationResult publishLocked(
            RemoteCapabilityCatalogPublicationIntent intent) {
        CurrentAuthority initial = readCurrentAuthority();
        RemoteCapabilityCatalogPublicationResult currentRefusal =
                classifyInitialCurrent(intent, initial);
        if (currentRefusal != null) {
            return currentRefusal;
        }

        RemoteCapabilityCatalogCandidateSnapshot candidate;
        try {
            candidate = readCandidate(locator.locate(intent.candidateInputIdentity()));
        } catch (RemoteCapabilityCatalogCandidateSnapshot.Exception exception) {
            if (existingInstall(initial, intent)) {
                return refused(intent, RemoteCapabilityCatalogPublicationRefusalReason
                        .CURRENT_BINDING_ALREADY_EXISTS);
            }
            return refused(intent,
                    exception.failure()
                                    == RemoteCapabilityCatalogCandidateSnapshot.Failure
                                            .OUTSIDE_SIZE_BOUND
                            ? RemoteCapabilityCatalogPublicationRefusalReason
                                    .CANDIDATE_OUTSIDE_SIZE_BOUND
                            : RemoteCapabilityCatalogPublicationRefusalReason
                                    .CATALOG_INVALID);
        } catch (CandidateAcquisitionException exception) {
            if (existingInstall(initial, intent)) {
                return refused(intent, RemoteCapabilityCatalogPublicationRefusalReason
                        .CURRENT_BINDING_ALREADY_EXISTS);
            }
            return refused(intent,
                    exception.failure == CandidateAcquisitionFailure.CHANGED
                            ? RemoteCapabilityCatalogPublicationRefusalReason
                                    .CANDIDATE_CHANGED_DURING_READ
                            : RemoteCapabilityCatalogPublicationRefusalReason
                                    .CANDIDATE_UNAVAILABLE_OR_UNSAFE);
        } catch (RuntimeException exception) {
            if (existingInstall(initial, intent)) {
                return refused(intent, RemoteCapabilityCatalogPublicationRefusalReason
                        .CURRENT_BINDING_ALREADY_EXISTS);
            }
            return refused(intent, RemoteCapabilityCatalogPublicationRefusalReason
                    .CANDIDATE_UNAVAILABLE_OR_UNSAFE);
        }

        RemoteCapabilityCatalogBinding requested =
                RemoteCapabilityCatalogBindingCodec.create(catalogs, candidate.sha256());
        boolean exactReplay = initial.binding != null
                && initial.binding.equals(requested);
        if (existingInstall(initial, intent) && !exactReplay) {
            return refused(intent, RemoteCapabilityCatalogPublicationRefusalReason
                    .CURRENT_BINDING_ALREADY_EXISTS);
        }
        RemoteCapabilityCatalogEvidenceDecision evidenceDecision;
        try {
            evidenceDecision = verifier.verify(
                    new RemoteCapabilityCatalogEvidenceVerificationRequest(
                            installationIdentity,
                            intent.candidateInputIdentity(),
                            intent.evidenceIdentity(),
                            candidate));
        } catch (RuntimeException exception) {
            evidenceDecision = RemoteCapabilityCatalogEvidenceDecision.REFUSED;
        }
        if (evidenceDecision != RemoteCapabilityCatalogEvidenceDecision.APPROVED) {
            return refused(intent, RemoteCapabilityCatalogPublicationRefusalReason
                    .EVIDENCE_NOT_APPROVED_OR_MISMATCHED);
        }
        if (exactReplay) {
            return success(
                    intent,
                    RemoteCapabilityCatalogPublicationStatus.EXACT_REPLAY,
                    candidate,
                    requested);
        }
        if (intent.operation() == RemoteCapabilityCatalogPublicationOperation.ROTATE
                && !sameDigest(
                        initial.binding.bindingSha256(),
                        intent.expectedCurrentBindingSha256().orElseThrow())) {
            return refused(intent, RemoteCapabilityCatalogPublicationRefusalReason
                    .EXPECTED_CURRENT_MISMATCH);
        }

        try {
            publishCatalog(candidate, requested.catalogPath());
        } catch (ImmutableCatalogConflictException exception) {
            return refused(intent, RemoteCapabilityCatalogPublicationRefusalReason
                    .IMMUTABLE_CATALOG_CONFLICT);
        } catch (IOException | SecurityException exception) {
            return refused(intent, RemoteCapabilityCatalogPublicationRefusalReason
                    .ATOMIC_PUBLICATION_UNSUPPORTED_OR_FAILED);
        }
        try {
            faults.check(RemoteCapabilityCatalogPublicationFaultInjector.Point
                    .AFTER_CATALOG_PUBLICATION);
        } catch (RemoteCapabilityCatalogPublicationFaultInjector.Fault exception) {
            return refused(intent, RemoteCapabilityCatalogPublicationRefusalReason
                    .ATOMIC_PUBLICATION_UNSUPPORTED_OR_FAILED);
        }

        RemoteCapabilityCatalogPublicationResult preSwitchRefusal =
                classifyPreSwitch(intent, initial);
        if (preSwitchRefusal != null) {
            return preSwitchRefusal;
        }

        try {
            publishBinding(intent.operation(), requested);
        } catch (IOException | SecurityException exception) {
            CurrentAuthority observed = readCurrentAuthority();
            return refused(intent,
                    observed.binding != null && observed.binding.equals(requested)
                            ? RemoteCapabilityCatalogPublicationRefusalReason
                                    .POST_PUBLICATION_STATE_UNKNOWN
                            : RemoteCapabilityCatalogPublicationRefusalReason
                                    .ATOMIC_PUBLICATION_UNSUPPORTED_OR_FAILED);
        }
        try {
            faults.check(RemoteCapabilityCatalogPublicationFaultInjector.Point
                    .AFTER_BINDING_PUBLICATION);
        } catch (RemoteCapabilityCatalogPublicationFaultInjector.Fault exception) {
            return refused(intent, RemoteCapabilityCatalogPublicationRefusalReason
                    .POST_PUBLICATION_STATE_UNKNOWN);
        }

        CurrentAuthority verified = readCurrentAuthority();
        if (verified.binding == null || !verified.binding.equals(requested)) {
            return refused(intent, RemoteCapabilityCatalogPublicationRefusalReason
                    .POST_PUBLICATION_STATE_UNKNOWN);
        }
        return success(
                intent,
                intent.operation() == RemoteCapabilityCatalogPublicationOperation.INSTALL
                        ? RemoteCapabilityCatalogPublicationStatus.INSTALLED
                        : RemoteCapabilityCatalogPublicationStatus.ROTATED,
                candidate,
                requested);
    }

    private RemoteCapabilityCatalogPublicationResult classifyInitialCurrent(
            RemoteCapabilityCatalogPublicationIntent intent,
            CurrentAuthority current) {
        if (intent.operation() == RemoteCapabilityCatalogPublicationOperation.ROTATE
                && current.state == CurrentAuthorityState.ABSENT) {
            return refused(intent, RemoteCapabilityCatalogPublicationRefusalReason
                    .CURRENT_BINDING_REQUIRED);
        }
        if (current.state == CurrentAuthorityState.INVALID) {
            return refused(intent, RemoteCapabilityCatalogPublicationRefusalReason
                    .CURRENT_BINDING_INVALID_OR_UNSAFE);
        }
        return null;
    }

    private RemoteCapabilityCatalogPublicationResult classifyPreSwitch(
            RemoteCapabilityCatalogPublicationIntent intent,
            CurrentAuthority initial) {
        CurrentAuthority fresh = readCurrentAuthority();
        if (intent.operation() == RemoteCapabilityCatalogPublicationOperation.INSTALL) {
            if (fresh.state == CurrentAuthorityState.INVALID) {
                return refused(intent, RemoteCapabilityCatalogPublicationRefusalReason
                        .CURRENT_BINDING_INVALID_OR_UNSAFE);
            }
            if (fresh.state != CurrentAuthorityState.ABSENT) {
                return refused(intent, RemoteCapabilityCatalogPublicationRefusalReason
                        .CURRENT_BINDING_ALREADY_EXISTS);
            }
            return null;
        }
        if (fresh.state != CurrentAuthorityState.VALID) {
            return refused(intent, RemoteCapabilityCatalogPublicationRefusalReason
                    .CURRENT_BINDING_INVALID_OR_UNSAFE);
        }
        String expected = intent.expectedCurrentBindingSha256().orElseThrow();
        if (!sameDigest(initial.binding.bindingSha256(), fresh.binding.bindingSha256())
                || !sameDigest(expected, fresh.binding.bindingSha256())) {
            return refused(intent, RemoteCapabilityCatalogPublicationRefusalReason
                    .EXPECTED_CURRENT_MISMATCH);
        }
        return null;
    }

    private boolean safeRoot() {
        try {
            return Files.isDirectory(root, LinkOption.NOFOLLOW_LINKS)
                    && root.toRealPath().equals(root)
                    && Files.isDirectory(catalogs, LinkOption.NOFOLLOW_LINKS)
                    && catalogs.toRealPath().equals(catalogs);
        } catch (IOException | SecurityException exception) {
            return false;
        }
    }

    private RemoteCapabilityCatalogCandidateSnapshot readCandidate(Path path)
            throws CandidateAcquisitionException,
            RemoteCapabilityCatalogCandidateSnapshot.Exception {
        if (!safeCandidatePath(path)) {
            throw new CandidateAcquisitionException(
                    CandidateAcquisitionFailure.UNAVAILABLE);
        }
        BasicFileAttributes before;
        try {
            before = Files.readAttributes(
                    path, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
        } catch (IOException | SecurityException exception) {
            throw new CandidateAcquisitionException(
                    CandidateAcquisitionFailure.UNAVAILABLE);
        }
        if (before.size() < 1 || before.size() > MAX_CATALOG_BYTES) {
            throw new RemoteCapabilityCatalogCandidateSnapshot.Exception(
                    RemoteCapabilityCatalogCandidateSnapshot.Failure.OUTSIDE_SIZE_BOUND);
        }
        byte[] bytes;
        try (FileChannel channel = FileChannel.open(
                path, StandardOpenOption.READ, LinkOption.NOFOLLOW_LINKS)) {
            if (channel.size() != before.size()) {
                throw new CandidateAcquisitionException(
                        CandidateAcquisitionFailure.CHANGED);
            }
            try {
                bytes = PinnedFileRemoteModelCandidateCapabilityFactSource
                        .readDeclaredBytes(channel, before.size());
            } catch (IOException exception) {
                throw new CandidateAcquisitionException(
                        CandidateAcquisitionFailure.CHANGED);
            }
            try {
                faults.check(RemoteCapabilityCatalogPublicationFaultInjector.Point
                        .AFTER_CANDIDATE_SNAPSHOT_READ);
            } catch (RemoteCapabilityCatalogPublicationFaultInjector.Fault exception) {
                throw new CandidateAcquisitionException(
                        CandidateAcquisitionFailure.UNAVAILABLE);
            }
        } catch (CandidateAcquisitionException exception) {
            throw exception;
        } catch (IOException | SecurityException exception) {
            throw new CandidateAcquisitionException(
                    CandidateAcquisitionFailure.UNAVAILABLE);
        }
        BasicFileAttributes after;
        try {
            after = Files.readAttributes(
                    path, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
        } catch (IOException | SecurityException exception) {
            throw new CandidateAcquisitionException(CandidateAcquisitionFailure.CHANGED);
        }
        if (!sameIdentity(before, after) || !safeCandidatePath(path)) {
            throw new CandidateAcquisitionException(CandidateAcquisitionFailure.CHANGED);
        }
        return RemoteCapabilityCatalogCandidateSnapshot.capture(bytes);
    }

    private boolean safeCandidatePath(Path path) {
        try {
            return path != null
                    && path.isAbsolute()
                    && path.equals(path.normalize())
                    && !Files.isSymbolicLink(path)
                    && Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)
                    && path.toRealPath().equals(path);
        } catch (IOException | SecurityException exception) {
            return false;
        }
    }

    private CurrentAuthority readCurrentAuthority() {
        try {
            if (Files.notExists(bindingPath, LinkOption.NOFOLLOW_LINKS)) {
                return CurrentAuthority.absent();
            }
            if (!Files.exists(bindingPath, LinkOption.NOFOLLOW_LINKS)
                    || Files.isSymbolicLink(bindingPath)
                    || !Files.isRegularFile(bindingPath, LinkOption.NOFOLLOW_LINKS)
                    || !bindingPath.toRealPath().equals(bindingPath)) {
                return CurrentAuthority.invalid();
            }
            byte[] bytes = readStableFile(bindingPath, MAX_BINDING_BYTES);
            RemoteCapabilityCatalogBindingCodec.DecodeResult decoded =
                    RemoteCapabilityCatalogBindingCodec.decode(catalogs, bytes);
            if (!(decoded instanceof RemoteCapabilityCatalogBindingCodec.Decoded valid)
                    || !validCatalogArtifact(
                            valid.binding().catalogPath(),
                            valid.binding().catalogSha256(),
                            null)) {
                return CurrentAuthority.invalid();
            }
            return CurrentAuthority.valid(valid.binding());
        } catch (IOException | SecurityException exception) {
            return CurrentAuthority.invalid();
        }
    }

    private void publishCatalog(
            RemoteCapabilityCatalogCandidateSnapshot candidate,
            Path target) throws IOException, ImmutableCatalogConflictException {
        if (Files.exists(target, LinkOption.NOFOLLOW_LINKS)) {
            requireMatchingCatalog(target, candidate);
            return;
        }
        Path temporary = Files.createTempFile(catalogs, ".catalog-", ".candidate");
        writeForced(temporary, candidate.bytes());
        if (!validCatalogArtifact(
                temporary, candidate.sha256(), candidate.bytes(), false)) {
            throw new IOException("catalog candidate verification failed");
        }
        try {
            Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE);
        } catch (FileAlreadyExistsException exception) {
            requireMatchingCatalog(target, candidate);
            return;
        }
        requireMatchingCatalog(target, candidate);
    }

    private void requireMatchingCatalog(
            Path target,
            RemoteCapabilityCatalogCandidateSnapshot candidate)
            throws ImmutableCatalogConflictException {
        if (!validCatalogArtifact(
                target, candidate.sha256(), candidate.bytes())) {
            throw new ImmutableCatalogConflictException();
        }
    }

    private boolean validCatalogArtifact(
            Path path,
            String expectedDigest,
            byte[] expectedBytes) {
        return validCatalogArtifact(path, expectedDigest, expectedBytes, true);
    }

    private boolean validCatalogArtifact(
            Path path,
            String expectedDigest,
            byte[] expectedBytes,
            boolean requireFinalPath) {
        try {
            if (path == null
                    || !path.isAbsolute()
                    || !path.equals(path.normalize())
                    || !catalogs.equals(path.getParent())
                    || Files.isSymbolicLink(path)
                    || !Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)
                    || !path.toRealPath().equals(path)) {
                return false;
            }
            if (requireFinalPath
                    && !RemoteCapabilityCatalogBindingCodec
                            .create(catalogs, expectedDigest)
                            .catalogPath()
                            .equals(path)) {
                return false;
            }
            byte[] bytes = readStableFile(path, MAX_CATALOG_BYTES);
            RemoteCapabilityCatalogCandidateSnapshot snapshot =
                    RemoteCapabilityCatalogCandidateSnapshot.capture(bytes);
            return sameDigest(expectedDigest, snapshot.sha256())
                    && (expectedBytes == null || Arrays.equals(expectedBytes, bytes));
        } catch (IOException
                | RemoteCapabilityCatalogCandidateSnapshot.Exception
                | RuntimeException exception) {
            return false;
        }
    }

    private void publishBinding(
            RemoteCapabilityCatalogPublicationOperation operation,
            RemoteCapabilityCatalogBinding binding) throws IOException {
        Path temporary = Files.createTempFile(root, ".binding-", ".candidate");
        writeForced(temporary, binding.canonicalBytes());
        byte[] verifiedBytes = readStableFile(temporary, MAX_BINDING_BYTES);
        RemoteCapabilityCatalogBindingCodec.DecodeResult decoded =
                RemoteCapabilityCatalogBindingCodec.decode(catalogs, verifiedBytes);
        if (!(decoded instanceof RemoteCapabilityCatalogBindingCodec.Decoded valid)
                || !valid.binding().equals(binding)) {
            throw new IOException("binding candidate verification failed");
        }
        if (operation == RemoteCapabilityCatalogPublicationOperation.ROTATE) {
            Files.move(
                    temporary,
                    bindingPath,
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING);
        } else {
            Files.move(temporary, bindingPath, StandardCopyOption.ATOMIC_MOVE);
        }
    }

    private static byte[] readStableFile(Path path, long maximumBytes)
            throws IOException {
        BasicFileAttributes before = Files.readAttributes(
                path, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
        if (before.size() < 1 || before.size() > maximumBytes) {
            throw new IOException("file outside bounded size");
        }
        byte[] bytes;
        try (FileChannel channel = FileChannel.open(
                path, StandardOpenOption.READ, LinkOption.NOFOLLOW_LINKS)) {
            if (channel.size() != before.size()) {
                throw new IOException("file changed before read");
            }
            bytes = PinnedFileRemoteModelCandidateCapabilityFactSource
                    .readDeclaredBytes(channel, before.size());
        }
        BasicFileAttributes after = Files.readAttributes(
                path, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
        if (!sameIdentity(before, after)
                || Files.isSymbolicLink(path)
                || !path.toRealPath().equals(path)) {
            throw new IOException("file changed during read");
        }
        return bytes;
    }

    private static boolean sameIdentity(
            BasicFileAttributes before,
            BasicFileAttributes after) {
        return before.size() == after.size()
                && Objects.equals(before.fileKey(), after.fileKey())
                && before.creationTime().equals(after.creationTime())
                && before.lastModifiedTime().equals(after.lastModifiedTime());
    }

    private static void writeForced(Path path, byte[] bytes) throws IOException {
        try (FileChannel channel = FileChannel.open(
                path,
                StandardOpenOption.WRITE,
                StandardOpenOption.TRUNCATE_EXISTING,
                LinkOption.NOFOLLOW_LINKS)) {
            ByteBuffer buffer = ByteBuffer.wrap(bytes);
            while (buffer.hasRemaining()) {
                channel.write(buffer);
            }
            channel.force(true);
        }
    }

    private static boolean sameDigest(String left, String right) {
        return MessageDigest.isEqual(
                left.getBytes(StandardCharsets.US_ASCII),
                right.getBytes(StandardCharsets.US_ASCII));
    }

    private static boolean existingInstall(
            CurrentAuthority current,
            RemoteCapabilityCatalogPublicationIntent intent) {
        return intent.operation() == RemoteCapabilityCatalogPublicationOperation.INSTALL
                && current.state == CurrentAuthorityState.VALID;
    }

    private RemoteCapabilityCatalogPublicationResult refused(
            RemoteCapabilityCatalogPublicationIntent intent,
            RemoteCapabilityCatalogPublicationRefusalReason reason) {
        return new RemoteCapabilityCatalogPublicationResult.Refused(
                intent.operation(),
                reason,
                installationIdentity,
                intent.evidenceIdentity());
    }

    private RemoteCapabilityCatalogPublicationResult success(
            RemoteCapabilityCatalogPublicationIntent intent,
            RemoteCapabilityCatalogPublicationStatus status,
            RemoteCapabilityCatalogCandidateSnapshot candidate,
            RemoteCapabilityCatalogBinding binding) {
        return new RemoteCapabilityCatalogPublicationResult.Succeeded(
                intent.operation(),
                status,
                installationIdentity,
                intent.evidenceIdentity(),
                candidate.catalogIdentity(),
                candidate.sha256(),
                binding.bindingSha256());
    }

    private enum CurrentAuthorityState {
        ABSENT,
        VALID,
        INVALID
    }

    private static final class CurrentAuthority {
        private final CurrentAuthorityState state;
        private final RemoteCapabilityCatalogBinding binding;

        private CurrentAuthority(
                CurrentAuthorityState state,
                RemoteCapabilityCatalogBinding binding) {
            this.state = state;
            this.binding = binding;
        }

        private static CurrentAuthority absent() {
            return new CurrentAuthority(CurrentAuthorityState.ABSENT, null);
        }

        private static CurrentAuthority valid(
                RemoteCapabilityCatalogBinding binding) {
            return new CurrentAuthority(
                    CurrentAuthorityState.VALID,
                    Objects.requireNonNull(binding, "binding must not be null"));
        }

        private static CurrentAuthority invalid() {
            return new CurrentAuthority(CurrentAuthorityState.INVALID, null);
        }
    }

    private enum CandidateAcquisitionFailure {
        UNAVAILABLE,
        CHANGED
    }

    private static final class CandidateAcquisitionException extends Exception {
        private static final long serialVersionUID = 1L;
        private final CandidateAcquisitionFailure failure;

        private CandidateAcquisitionException(CandidateAcquisitionFailure failure) {
            this.failure = Objects.requireNonNull(failure, "failure must not be null");
        }
    }

    private static final class ImmutableCatalogConflictException extends Exception {
        private static final long serialVersionUID = 1L;
    }
}
