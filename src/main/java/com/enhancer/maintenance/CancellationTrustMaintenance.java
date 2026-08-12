package com.enhancer.maintenance;

import com.enhancer.runtime.InstalledCancellationTrustMetadata;
import com.enhancer.runtime.InstalledCancellationTrustMetadataLoader;
import com.enhancer.runtime.PinnedFileCancellationGrantTrustPolicyLoader;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

/** Unexposed state machine for separately authorized installed-trust maintenance. */
public final class CancellationTrustMaintenance {
    static final String TRUST_DIRECTORY_NAME =
            "enhancer-cancellation-trust-policies-v1";
    static final String LOCK_FILE_NAME =
            "enhancer-cancellation-trust-maintenance-v1.lock";
    static final String POLICY_FILE_PREFIX = "enhancer-cancellation-trust-policy-";
    static final String POLICY_FILE_SUFFIX = ".conf";

    private static final Pattern SHA256 = Pattern.compile("[0-9a-f]{64}");
    private static final CancellationTrustMaintenanceFaultInjector NO_FAULT = phase -> { };
    private final CancellationTrustMaintenanceFaultInjector faults;

    public CancellationTrustMaintenance() {
        this(NO_FAULT);
    }

    CancellationTrustMaintenance(CancellationTrustMaintenanceFaultInjector faults) {
        this.faults = Objects.requireNonNull(faults, "faults must not be null");
    }

    public CancellationTrustMaintenanceResult install(Path applicationJar, Path candidate)
            throws IOException {
        final Layout layout;
        try {
            layout = Layout.resolve(applicationJar);
        } catch (IOException | IllegalArgumentException exception) {
            throw failure(
                    CancellationTrustMaintenanceFailureReason.INVALID_INSTALLATION,
                    "invalid installed application layout",
                    exception);
        }
        try (HeldLock lock = acquire(layout.lockFile())) {
            lock.requireValid();
            faults.after(CancellationTrustMaintenancePhase.AFTER_LOCK_ACQUIRED);
            if (Files.exists(layout.metadataFile(), LinkOption.NOFOLLOW_LINKS)) {
                throw failure(
                        CancellationTrustMaintenanceFailureReason.EXISTING_BINDING,
                        "installed cancellation trust metadata already exists");
            }
            Prepared prepared = prepare(layout, candidate);
            Path policy = publishPolicy(layout, prepared.snapshot());
            byte[] metadata = metadata(policy, prepared.snapshot().sha256());
            Path metadataCandidate = writeMetadataCandidate(layout, metadata);
            if (Files.exists(layout.metadataFile(), LinkOption.NOFOLLOW_LINKS)) {
                throw failure(
                        CancellationTrustMaintenanceFailureReason.EXISTING_BINDING,
                        "installed cancellation trust metadata appeared");
            }
            faults.after(CancellationTrustMaintenancePhase.BEFORE_METADATA_SWITCH);
            layout.requireExact();
            exactPolicy(policy, prepared.snapshot());
            atomicMove(metadataCandidate, layout.metadataFile(), false);
            faults.after(CancellationTrustMaintenancePhase.AFTER_METADATA_SWITCH);
            return verify(layout, CancellationTrustMaintenanceStatus.INSTALLED,
                    prepared.snapshot().sha256(), metadata);
        }
    }

    public CancellationTrustMaintenanceResult rotate(
            Path applicationJar, Path candidate, String expectedCurrentSha256)
            throws IOException {
        final Layout layout;
        final byte[] expected;
        try {
            layout = Layout.resolve(applicationJar);
            expected = digest(expectedCurrentSha256);
        } catch (IOException | IllegalArgumentException exception) {
            throw failure(
                    CancellationTrustMaintenanceFailureReason.INVALID_INSTALLATION,
                    "invalid installed application layout or expected digest",
                    exception);
        }
        try (HeldLock lock = acquire(layout.lockFile())) {
            lock.requireValid();
            faults.after(CancellationTrustMaintenancePhase.AFTER_LOCK_ACQUIRED);
            InstalledCancellationTrustMetadataLoader.CanonicalSnapshot current =
                    loadCurrent(layout);
            faults.after(CancellationTrustMaintenancePhase.AFTER_CURRENT_VALIDATED);
            Prepared prepared = prepare(layout, candidate);
            Path policy = layout.policyFile(prepared.snapshot().sha256());
            byte[] requested = metadata(policy, prepared.snapshot().sha256());
            if (MessageDigest.isEqual(current.bytes(), requested)) {
                InstalledCancellationTrustMetadataLoader.CanonicalSnapshot replay =
                        productionLoad(layout);
                if (!MessageDigest.isEqual(replay.bytes(), requested)) {
                    throw failure(
                            CancellationTrustMaintenanceFailureReason
                                    .POST_SWITCH_VERIFICATION_FAILED,
                            "installed metadata drifted during replay");
                }
                return result(CancellationTrustMaintenanceStatus.EXACT_REPLAY,
                        replay.metadata(), replay.sha256());
            }
            requireDigest(expected, current.sha256(), "installed metadata is stale");
            publishPolicy(layout, prepared.snapshot());
            Path metadataCandidate = writeMetadataCandidate(layout, requested);
            faults.after(CancellationTrustMaintenancePhase.BEFORE_FINAL_CAS);
            InstalledCancellationTrustMetadataLoader.CanonicalSnapshot finalCurrent =
                    productionLoad(layout);
            requireDigest(expected, finalCurrent.sha256(),
                    "installed metadata changed before rotation");
            faults.after(CancellationTrustMaintenancePhase.BEFORE_METADATA_SWITCH);
            layout.requireExact();
            exactPolicy(policy, prepared.snapshot());
            atomicMove(metadataCandidate, layout.metadataFile(), true);
            faults.after(CancellationTrustMaintenancePhase.AFTER_METADATA_SWITCH);
            return verify(layout, CancellationTrustMaintenanceStatus.ROTATED,
                    prepared.snapshot().sha256(), requested);
        }
    }

    private Prepared prepare(Layout layout, Path candidate) throws IOException {
        final PinnedFileCancellationGrantTrustPolicyLoader.CanonicalSnapshot snapshot;
        try {
            Path exact = exactFile(candidate, "candidatePolicy");
            snapshot = PinnedFileCancellationGrantTrustPolicyLoader
                    .readCanonicalSnapshot(exact);
        } catch (IOException | IllegalArgumentException exception) {
            throw failure(
                    CancellationTrustMaintenanceFailureReason.INVALID_CANDIDATE_POLICY,
                    "invalid cancellation trust policy candidate",
                    exception);
        }
        Path policy = layout.policyFile(snapshot.sha256());
        InstalledCancellationTrustMetadata parsed =
                InstalledCancellationTrustMetadataLoader.parseCanonical(
                        metadata(policy, snapshot.sha256()));
        if (!parsed.policyFile().equals(policy)
                || !parsed.expectedSha256().equals(snapshot.sha256())) {
            throw failure(
                    CancellationTrustMaintenanceFailureReason.INVALID_CANDIDATE_POLICY,
                    "constructed metadata changed");
        }
        faults.after(CancellationTrustMaintenancePhase.AFTER_CANDIDATE_VALIDATED);
        return new Prepared(snapshot);
    }

    private Path publishPolicy(
            Layout layout,
            PinnedFileCancellationGrantTrustPolicyLoader.CanonicalSnapshot snapshot)
            throws IOException {
        Path target = layout.policyFile(snapshot.sha256());
        try {
            return publishPolicyUnchecked(layout, snapshot, target);
        } catch (CancellationTrustMaintenanceException exception) {
            throw exception;
        } catch (IOException exception) {
            throw failure(
                    CancellationTrustMaintenanceFailureReason.PUBLICATION_FAILED,
                    "policy publication failed",
                    exception);
        }
    }

    private Path publishPolicyUnchecked(
            Layout layout,
            PinnedFileCancellationGrantTrustPolicyLoader.CanonicalSnapshot snapshot,
            Path target) throws IOException {
        if (Files.exists(target, LinkOption.NOFOLLOW_LINKS)) {
            exactPolicy(target, snapshot);
            faults.after(CancellationTrustMaintenancePhase.AFTER_POLICY_PUBLISHED);
            return target;
        }
        Path candidate = unique(layout.trustDirectory(), ".policy-candidate-");
        writeForce(candidate, snapshot.bytes());
        PinnedFileCancellationGrantTrustPolicyLoader.CanonicalSnapshot reread =
                PinnedFileCancellationGrantTrustPolicyLoader.readCanonicalSnapshot(candidate);
        if (!reread.sha256().equals(snapshot.sha256())
                || !MessageDigest.isEqual(reread.bytes(), snapshot.bytes())) {
            throw new IOException("policy candidate changed");
        }
        faults.after(CancellationTrustMaintenancePhase.AFTER_POLICY_CANDIDATE_FORCED);
        if (Files.exists(target, LinkOption.NOFOLLOW_LINKS)) {
            exactPolicy(target, snapshot);
        } else {
            try {
                atomicMove(candidate, target, false);
            } catch (FileAlreadyExistsException exception) {
                exactPolicy(target, snapshot);
            }
        }
        exactPolicy(target, snapshot);
        faults.after(CancellationTrustMaintenancePhase.AFTER_POLICY_PUBLISHED);
        return target;
    }

    private Path writeMetadataCandidate(Layout layout, byte[] bytes) throws IOException {
        try {
            Path candidate = unique(layout.installationDirectory(), ".metadata-candidate-");
            writeForce(candidate, bytes);
            byte[] reread = readBounded(candidate,
                    InstalledCancellationTrustMetadataLoader.MAX_METADATA_BYTES,
                    "metadata candidate");
            InstalledCancellationTrustMetadata parsed =
                    InstalledCancellationTrustMetadataLoader.parseCanonical(reread);
            if (!MessageDigest.isEqual(bytes, reread)
                    || !parsed.policyFile().equals(
                            layout.policyFile(parsed.expectedSha256()))) {
                throw new IOException("metadata candidate changed");
            }
            new PinnedFileCancellationGrantTrustPolicyLoader(
                    parsed.policyFile(), parsed.expectedSha256()).load();
            faults.after(
                    CancellationTrustMaintenancePhase.AFTER_METADATA_CANDIDATE_VALIDATED);
            return candidate;
        } catch (CancellationTrustMaintenanceException exception) {
            throw exception;
        } catch (IOException exception) {
            throw failure(
                    CancellationTrustMaintenanceFailureReason.CANDIDATE_WRITE_FAILED,
                    "metadata candidate persistence failed",
                    exception);
        }
    }

    private CancellationTrustMaintenanceResult verify(
            Layout layout,
            CancellationTrustMaintenanceStatus status,
            String policySha256,
            byte[] expectedMetadata) throws IOException {
        InstalledCancellationTrustMetadataLoader.CanonicalSnapshot installed =
                productionLoad(layout);
        if (!installed.metadata().expectedSha256().equals(policySha256)
                || !MessageDigest.isEqual(installed.bytes(), expectedMetadata)) {
            throw failure(
                    CancellationTrustMaintenanceFailureReason.POST_SWITCH_VERIFICATION_FAILED,
                    "published binding is not exact");
        }
        faults.after(CancellationTrustMaintenancePhase.AFTER_INSTALLED_VERIFIED);
        return result(status, installed.metadata(), installed.sha256());
    }

    private static InstalledCancellationTrustMetadataLoader.CanonicalSnapshot
            productionLoad(Layout layout) throws IOException {
        InstalledCancellationTrustMetadataLoader.CanonicalSnapshot snapshot =
                new InstalledCancellationTrustMetadataLoader(
                        layout.applicationJar()).loadCanonicalSnapshot();
        new PinnedFileCancellationGrantTrustPolicyLoader(
                snapshot.metadata().policyFile(),
                snapshot.metadata().expectedSha256()).load();
        return snapshot;
    }

    private static CancellationTrustMaintenanceResult result(
            CancellationTrustMaintenanceStatus status,
            InstalledCancellationTrustMetadata metadata,
            String metadataSha256) {
        return new CancellationTrustMaintenanceResult(status, metadata.policyFile(),
                metadata.expectedSha256(), metadataSha256);
    }

    private HeldLock acquire(Path path) throws IOException {
        try {
            if (Files.exists(path, LinkOption.NOFOLLOW_LINKS)) {
                requireFile(path, "maintenance lock");
            }
        } catch (IOException exception) {
            throw failure(
                    CancellationTrustMaintenanceFailureReason.LOCK_FAILED,
                    "maintenance lock path is invalid",
                    exception);
        }
        final FileChannel channel;
        try {
            channel = FileChannel.open(path, StandardOpenOption.CREATE,
                    StandardOpenOption.WRITE, LinkOption.NOFOLLOW_LINKS);
        } catch (IOException exception) {
            throw failure(
                    CancellationTrustMaintenanceFailureReason.LOCK_FAILED,
                    "maintenance lock could not be opened",
                    exception);
        }
        try {
            FileLock lock;
            try {
                lock = channel.tryLock();
            } catch (OverlappingFileLockException exception) {
                throw failure(
                        CancellationTrustMaintenanceFailureReason.LOCK_CONTENDED,
                        "maintenance lock is contended",
                        exception);
            }
            if (lock == null) {
                throw failure(
                        CancellationTrustMaintenanceFailureReason.LOCK_CONTENDED,
                        "maintenance lock is contended");
            }
            requireFile(path, "maintenance lock");
            return new HeldLock(channel, lock);
        } catch (IOException | RuntimeException exception) {
            channel.close();
            throw exception;
        }
    }

    private static void exactPolicy(
            Path path,
            PinnedFileCancellationGrantTrustPolicyLoader.CanonicalSnapshot expected)
            throws IOException {
        PinnedFileCancellationGrantTrustPolicyLoader.CanonicalSnapshot actual =
                PinnedFileCancellationGrantTrustPolicyLoader.readCanonicalSnapshot(path);
        if (!actual.sha256().equals(expected.sha256())
                || !MessageDigest.isEqual(actual.bytes(), expected.bytes())) {
            throw failure(
                    CancellationTrustMaintenanceFailureReason.POLICY_COLLISION,
                    "content-addressed policy is corrupt");
        }
    }

    private static Path unique(Path directory, String prefix) throws IOException {
        requireDirectory(directory, "candidate directory");
        return directory.resolve(prefix + UUID.randomUUID() + ".tmp");
    }

    private static void writeForce(Path path, byte[] bytes) throws IOException {
        try (FileChannel channel = FileChannel.open(path, StandardOpenOption.CREATE_NEW,
                StandardOpenOption.WRITE, LinkOption.NOFOLLOW_LINKS)) {
            ByteBuffer source = ByteBuffer.wrap(bytes);
            while (source.hasRemaining()) {
                channel.write(source);
            }
            channel.force(true);
        }
    }

    private static void atomicMove(Path source, Path target, boolean replace)
            throws IOException {
        try {
            if (replace) {
                Files.move(source, target, StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING);
            } else {
                Files.move(source, target, StandardCopyOption.ATOMIC_MOVE);
            }
        } catch (AtomicMoveNotSupportedException exception) {
            throw failure(
                    CancellationTrustMaintenanceFailureReason.PUBLICATION_FAILED,
                    "required atomic maintenance move is unsupported",
                    exception);
        }
    }

    private static byte[] metadata(Path policy, String sha256) {
        return InstalledCancellationTrustMetadataLoader.encodeCanonical(
                new InstalledCancellationTrustMetadata(policy, sha256));
    }

    private static byte[] readBounded(Path path, int maximum, String label)
            throws IOException {
        requireFile(path, label);
        try (FileChannel channel = FileChannel.open(
                path, StandardOpenOption.READ, LinkOption.NOFOLLOW_LINKS)) {
            long size = channel.size();
            if (size <= 0 || size > maximum) {
                throw new IOException(label + " is outside supported size bounds");
            }
            byte[] bytes = new byte[(int) size];
            ByteBuffer target = ByteBuffer.wrap(bytes);
            while (target.hasRemaining()) {
                if (channel.read(target) < 0) {
                    throw new IOException(label + " changed while reading");
                }
            }
            if (channel.read(ByteBuffer.allocate(1)) >= 0 || channel.size() != size) {
                throw new IOException(label + " changed while reading");
            }
            return bytes;
        }
    }

    private static Path exactFile(Path path, String label) throws IOException {
        Path checked = Objects.requireNonNull(path, label + " must not be null");
        if (!checked.isAbsolute() || !checked.equals(checked.normalize())) {
            throw new IllegalArgumentException(label + " must be absolute and normalized");
        }
        requireFile(checked, label);
        return checked;
    }

    private static void requireFile(Path path, String label) throws IOException {
        Path parent = path.getParent();
        if (parent == null) {
            throw new IOException(label + " must have a parent");
        }
        requireDirectory(parent, label + " parent");
        if (!Files.exists(path, LinkOption.NOFOLLOW_LINKS)
                || Files.isSymbolicLink(path)
                || !Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)
                || !path.toRealPath().equals(path)) {
            throw new IOException(label + " must be an exact real regular file");
        }
    }

    private static void requireDirectory(Path path, String label) throws IOException {
        if (!path.isAbsolute() || !path.equals(path.normalize())
                || !Files.exists(path, LinkOption.NOFOLLOW_LINKS)
                || Files.isSymbolicLink(path)
                || !Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)
                || !path.toRealPath().equals(path)) {
            throw new IOException(label + " must be an exact real directory");
        }
    }

    private static byte[] digest(String value) {
        Objects.requireNonNull(value, "expectedCurrentSha256 must not be null");
        if (!SHA256.matcher(value).matches()) {
            throw new IllegalArgumentException(
                    "expectedCurrentSha256 must be lowercase SHA-256");
        }
        return HexFormat.of().parseHex(value);
    }

    private static void requireDigest(byte[] expected, String actual, String message)
            throws IOException {
        if (!MessageDigest.isEqual(expected, HexFormat.of().parseHex(actual))) {
            throw failure(
                    CancellationTrustMaintenanceFailureReason.STALE_CURRENT_METADATA,
                    message);
        }
    }

    private static InstalledCancellationTrustMetadataLoader.CanonicalSnapshot
            loadCurrent(Layout layout) throws IOException {
        try {
            return productionLoad(layout);
        } catch (IOException | IllegalArgumentException exception) {
            throw failure(
                    CancellationTrustMaintenanceFailureReason.INVALID_CURRENT_BINDING,
                    "installed cancellation trust binding is invalid",
                    exception);
        }
    }

    private static CancellationTrustMaintenanceException failure(
            CancellationTrustMaintenanceFailureReason reason,
            String detail) {
        return new CancellationTrustMaintenanceException(reason, detail);
    }

    private static CancellationTrustMaintenanceException failure(
            CancellationTrustMaintenanceFailureReason reason,
            String detail,
            Throwable cause) {
        return new CancellationTrustMaintenanceException(reason, detail, cause);
    }

    private record Layout(
            Path applicationJar,
            Path installationDirectory,
            Path trustDirectory,
            Path metadataFile,
            Path lockFile) {
        private static Layout resolve(Path applicationJar) throws IOException {
            Path app = exactFile(applicationJar, "applicationJar");
            if (!app.getFileName().toString().endsWith(".jar")) {
                throw new IllegalArgumentException("applicationJar must have a .jar name");
            }
            Path directory = app.getParent();
            Path trust = directory.resolve(TRUST_DIRECTORY_NAME);
            requireDirectory(trust, "cancellation trust directory");
            return new Layout(app, directory, trust,
                    app.resolveSibling(InstalledCancellationTrustMetadataLoader.METADATA_FILE_NAME),
                    app.resolveSibling(LOCK_FILE_NAME));
        }

        private Path policyFile(String sha256) {
            return trustDirectory.resolve(POLICY_FILE_PREFIX + sha256 + POLICY_FILE_SUFFIX);
        }

        private void requireExact() throws IOException {
            exactFile(applicationJar, "applicationJar");
            requireDirectory(installationDirectory, "installation directory");
            requireDirectory(trustDirectory, "cancellation trust directory");
            requireFile(lockFile, "maintenance lock");
        }
    }

    private record Prepared(
            PinnedFileCancellationGrantTrustPolicyLoader.CanonicalSnapshot snapshot) { }

    private record HeldLock(FileChannel channel, FileLock lock) implements AutoCloseable {
        private void requireValid() throws IOException {
            if (!lock.isValid()) {
                throw new IOException("maintenance lock is invalid");
            }
        }

        @Override
        public void close() throws IOException {
            try {
                lock.release();
            } finally {
                channel.close();
            }
        }
    }
}
