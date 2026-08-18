package com.enhancer.maintenance.installation;

import com.enhancer.maintenance.installation.InstallationTransactionStoreException.Reason;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Bounded local-filesystem cursor store for cooperating processes. The caller must
 * provision and protect the storage root; this adapter grants no installation or
 * permission authority and supplies no rollback or directory-durability guarantee.
 */
public final class FileSystemInstallationTransactionStore
        implements InstallationTransactionStore {
    private static final int MAX_ENVELOPE_BYTES =
            InstallationIntegrityEnvelope.HEADER_BYTES
                    + InstallationTransactionFileFormat.MAX_BODY_BYTES;

    private final Path storageRoot;

    public FileSystemInstallationTransactionStore(Path storageRoot)
            throws InstallationTransactionStoreException {
        Path checked = Objects.requireNonNull(
                storageRoot, "storageRoot must not be null");
        if (!checked.isAbsolute() || !checked.equals(checked.normalize())) {
            throw failure(Reason.STORE_UNAVAILABLE,
                    "transaction storage root must be absolute and normalized");
        }
        this.storageRoot = checked;
        requireStableRoot();
    }

    @Override
    public Mutation create(InstallationTransactionState initial)
            throws InstallationTransactionStoreException {
        InstallationTransactionState checked = Objects.requireNonNull(
                initial, "initial must not be null");
        UUID transactionId = checked.plan().transactionId();
        try (HeldLock held = acquire(transactionId)) {
            held.requireValid();
            Optional<StoredState> existing = readIfPresent(transactionId);
            if (existing.isPresent()) {
                InstallationTransactionState state = existing.orElseThrow().state();
                if (state.equals(checked)) {
                    return new Mutation(state, MutationDisposition.EXACT_REPLAY);
                }
                throw failure(Reason.TRANSACTION_CONFLICT,
                        "transaction identity is already bound to different state");
            }
            MutationDisposition disposition = publishCreate(transactionId, checked);
            InstallationTransactionState published = resolveStored(transactionId).state();
            if (!published.equals(checked)) {
                throw failure(Reason.REQUIRES_RECONCILIATION,
                        "published transaction state could not be established");
            }
            return new Mutation(published, disposition);
        }
    }

    @Override
    public InstallationTransactionState resolve(UUID transactionId)
            throws InstallationTransactionStoreException {
        UUID checked = Objects.requireNonNull(
                transactionId, "transactionId must not be null");
        return resolveStored(checked).state();
    }

    @Override
    public Mutation compareAndExchange(
            UUID transactionId,
            long expectedRevision,
            InstallationTransactionState replacement)
            throws InstallationTransactionStoreException {
        UUID checkedId = Objects.requireNonNull(
                transactionId, "transactionId must not be null");
        InstallationTransactionState checkedReplacement = Objects.requireNonNull(
                replacement, "replacement must not be null");
        if (!checkedId.equals(checkedReplacement.plan().transactionId())) {
            throw failure(Reason.INVALID_TRANSITION,
                    "replacement transaction identity does not match the point");
        }
        try (HeldLock held = acquire(checkedId)) {
            held.requireValid();
            StoredState current = resolveStored(checkedId);
            if (current.state().equals(checkedReplacement)) {
                return new Mutation(current.state(), MutationDisposition.EXACT_REPLAY);
            }
            if (current.state().revision() != expectedRevision) {
                throw failure(Reason.REVISION_CONFLICT,
                        "transaction revision does not match the expected revision");
            }
            if (!current.state().isImmediateSuccessor(checkedReplacement)) {
                throw failure(Reason.INVALID_TRANSITION,
                        "replacement is not the immediate transaction successor");
            }
            publishReplacement(current, checkedReplacement);
            InstallationTransactionState published = resolveStored(checkedId).state();
            if (!published.equals(checkedReplacement)) {
                throw failure(Reason.REQUIRES_RECONCILIATION,
                        "replacement publication could not be established");
            }
            return new Mutation(published, MutationDisposition.ADVANCED);
        }
    }

    private MutationDisposition publishCreate(
            UUID transactionId,
            InstallationTransactionState state)
            throws InstallationTransactionStoreException {
        Path target = transactionPath(transactionId);
        Path candidate = writeCandidate(transactionId, state);
        try {
            if (Files.exists(target, LinkOption.NOFOLLOW_LINKS)) {
                return classifyCreateCollision(transactionId, state);
            }
            try {
                Files.move(candidate, target, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException exception) {
                throw failure(Reason.STORE_UNAVAILABLE,
                        "transaction creation requires atomic move support");
            } catch (FileAlreadyExistsException collision) {
                return classifyCreateCollision(transactionId, state);
            } catch (IOException uncertain) {
                classifyUncertainPublication(transactionId, state, false);
                return MutationDisposition.EXACT_REPLAY;
            }
            return MutationDisposition.CREATED;
        } finally {
            deleteCandidate(candidate);
        }
    }

    private void publishReplacement(
            StoredState current,
            InstallationTransactionState replacement)
            throws InstallationTransactionStoreException {
        UUID transactionId = replacement.plan().transactionId();
        Path candidate = writeCandidate(transactionId, replacement);
        try {
            StoredState reread = resolveStored(transactionId);
            if (!MessageDigest.isEqual(current.bytes(), reread.bytes())) {
                throw failure(Reason.REVISION_CONFLICT,
                        "transaction bytes changed before publication");
            }
            try {
                Files.move(
                        candidate,
                        transactionPath(transactionId),
                        StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException exception) {
                throw failure(Reason.STORE_UNAVAILABLE,
                        "transaction replacement requires atomic move support");
            } catch (IOException uncertain) {
                classifyUncertainPublication(transactionId, replacement, true);
            }
        } finally {
            deleteCandidate(candidate);
        }
    }

    private Path writeCandidate(UUID transactionId, InstallationTransactionState state)
            throws InstallationTransactionStoreException {
        requireStableRoot();
        byte[] encoded = encode(state);
        Path candidate;
        try {
            candidate = Files.createTempFile(
                    storageRoot,
                    "." + transactionId + ".installation-transaction-",
                    ".tmp");
            try (FileChannel channel = FileChannel.open(
                    candidate,
                    StandardOpenOption.WRITE,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    LinkOption.NOFOLLOW_LINKS)) {
                ByteBuffer remaining = ByteBuffer.wrap(encoded);
                while (remaining.hasRemaining()) {
                    channel.write(remaining);
                }
                channel.force(true);
            }
        } catch (IOException exception) {
            throw failure(Reason.STORE_UNAVAILABLE,
                    "transaction candidate could not be written");
        }
        try {
            StoredState validated = readExactFile(candidate, transactionId, false);
            if (!validated.state().equals(state)
                    || !MessageDigest.isEqual(encoded, validated.bytes())) {
                throw failure(Reason.CORRUPT_STATE,
                        "transaction candidate did not validate exactly");
            }
            return candidate;
        } catch (InstallationTransactionStoreException exception) {
            deleteCandidate(candidate);
            throw exception;
        }
    }

    private MutationDisposition classifyCreateCollision(
            UUID transactionId,
            InstallationTransactionState requested)
            throws InstallationTransactionStoreException {
        Optional<StoredState> existing = readIfPresent(transactionId);
        if (existing.isPresent() && existing.orElseThrow().state().equals(requested)) {
            return MutationDisposition.EXACT_REPLAY;
        }
        throw failure(Reason.TRANSACTION_CONFLICT,
                "transaction identity appeared with different state");
    }

    private void classifyUncertainPublication(
            UUID transactionId,
            InstallationTransactionState requested,
            boolean replacement)
            throws InstallationTransactionStoreException {
        try {
            Optional<StoredState> observed = readIfPresent(transactionId);
            if (observed.isPresent()
                    && observed.orElseThrow().state().equals(requested)) {
                return;
            }
            if (!replacement && observed.isPresent()) {
                throw failure(Reason.TRANSACTION_CONFLICT,
                        "transaction creation raced with different state");
            }
            throw failure(Reason.REQUIRES_RECONCILIATION,
                    "transaction publication outcome is uncertain");
        } catch (InstallationTransactionStoreException failure) {
            if (failure.reason() == Reason.TRANSACTION_CONFLICT
                    || failure.reason() == Reason.REQUIRES_RECONCILIATION) {
                throw failure;
            }
            throw failure(Reason.REQUIRES_RECONCILIATION,
                    "transaction publication outcome cannot be resolved");
        }
    }

    private StoredState resolveStored(UUID transactionId)
            throws InstallationTransactionStoreException {
        return readIfPresent(transactionId).orElseThrow(() -> failure(
                Reason.NOT_FOUND, "transaction state was not found"));
    }

    private Optional<StoredState> readIfPresent(UUID transactionId)
            throws InstallationTransactionStoreException {
        requireStableRoot();
        Path artifact = transactionPath(transactionId);
        if (!Files.exists(artifact, LinkOption.NOFOLLOW_LINKS)) {
            return Optional.empty();
        }
        return Optional.of(readExactFile(artifact, transactionId, true));
    }

    private StoredState readExactFile(
            Path artifact,
            UUID expectedTransactionId,
            boolean requireCanonicalLeaf)
            throws InstallationTransactionStoreException {
        if (!Files.isRegularFile(artifact, LinkOption.NOFOLLOW_LINKS)
                || Files.isSymbolicLink(artifact)) {
            throw failure(Reason.CORRUPT_STATE,
                    "transaction artifact must be a non-symbolic regular file");
        }
        long declaredSize;
        try {
            declaredSize = Files.size(artifact);
        } catch (IOException exception) {
            throw failure(Reason.STORE_UNAVAILABLE,
                    "transaction artifact size could not be read");
        }
        if (declaredSize > MAX_ENVELOPE_BYTES) {
            throw failure(Reason.CAPACITY_EXCEEDED,
                    "transaction artifact exceeds the supported bound");
        }
        byte[] bytes;
        try (FileChannel channel = FileChannel.open(
                artifact, StandardOpenOption.READ, LinkOption.NOFOLLOW_LINKS)) {
            ByteBuffer bounded = ByteBuffer.allocate(MAX_ENVELOPE_BYTES + 1);
            while (bounded.hasRemaining() && channel.read(bounded) >= 0) {
                // Continue until EOF or the one-byte overflow sentinel is filled.
            }
            if (bounded.position() > MAX_ENVELOPE_BYTES) {
                throw failure(Reason.CAPACITY_EXCEEDED,
                        "transaction artifact grew outside the supported bound");
            }
            bytes = Arrays.copyOf(bounded.array(), bounded.position());
        } catch (NoSuchFileException missing) {
            throw failure(Reason.NOT_FOUND, "transaction state disappeared");
        } catch (InstallationTransactionStoreException exception) {
            throw exception;
        } catch (IOException exception) {
            throw failure(Reason.STORE_UNAVAILABLE,
                    "transaction artifact could not be read");
        }
        requireStableRoot();
        InstallationTransactionState state = decode(bytes);
        if (!state.plan().transactionId().equals(expectedTransactionId)
                || (requireCanonicalLeaf
                    && !artifact.equals(transactionPath(expectedTransactionId)))) {
            throw failure(Reason.CORRUPT_STATE,
                    "transaction artifact does not match its exact point");
        }
        return new StoredState(state, bytes);
    }

    private HeldLock acquire(UUID transactionId)
            throws InstallationTransactionStoreException {
        requireStableRoot();
        Path path = lockPath(transactionId);
        if (Files.exists(path, LinkOption.NOFOLLOW_LINKS)
                && (!Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)
                    || Files.isSymbolicLink(path))) {
            throw failure(Reason.STORE_UNAVAILABLE,
                    "transaction lock must be a non-symbolic regular file");
        }
        final FileChannel channel;
        try {
            channel = FileChannel.open(
                    path,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.WRITE,
                    LinkOption.NOFOLLOW_LINKS);
        } catch (IOException exception) {
            throw failure(Reason.STORE_UNAVAILABLE,
                    "transaction lock could not be opened");
        }
        try {
            FileLock lock;
            try {
                lock = channel.tryLock();
            } catch (OverlappingFileLockException contended) {
                throw failure(Reason.LOCK_CONTENDED,
                        "transaction lock is contended");
            }
            if (lock == null) {
                throw failure(Reason.LOCK_CONTENDED,
                        "transaction lock is contended");
            }
            requireStableRoot();
            if (!Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)
                    || Files.isSymbolicLink(path)) {
                lock.close();
                throw failure(Reason.STORE_UNAVAILABLE,
                        "transaction lock changed type");
            }
            return new HeldLock(channel, lock);
        } catch (InstallationTransactionStoreException exception) {
            closeChannel(channel);
            throw exception;
        } catch (IOException exception) {
            closeChannel(channel);
            throw failure(Reason.STORE_UNAVAILABLE,
                    "transaction lock could not be validated");
        }
    }

    private void requireStableRoot() throws InstallationTransactionStoreException {
        try {
            if (Files.isSymbolicLink(storageRoot)
                    || !Files.isDirectory(storageRoot, LinkOption.NOFOLLOW_LINKS)
                    || !storageRoot.equals(storageRoot.toRealPath(LinkOption.NOFOLLOW_LINKS))) {
                throw failure(Reason.STORE_UNAVAILABLE,
                        "transaction storage root is unavailable or changed");
            }
        } catch (IOException exception) {
            throw failure(Reason.STORE_UNAVAILABLE,
                    "transaction storage root could not be resolved");
        }
    }

    private byte[] encode(InstallationTransactionState state)
            throws InstallationTransactionStoreException {
        try {
            return InstallationTransactionFileFormat.encode(state);
        } catch (InstallationRecordFormatException exception) {
            throw mapFormat(exception);
        }
    }

    private InstallationTransactionState decode(byte[] bytes)
            throws InstallationTransactionStoreException {
        try {
            return InstallationTransactionFileFormat.decode(bytes);
        } catch (InstallationRecordFormatException exception) {
            throw mapFormat(exception);
        }
    }

    private InstallationTransactionStoreException mapFormat(
            InstallationRecordFormatException exception) {
        return switch (exception.reason()) {
            case UNSUPPORTED_SCHEMA -> failure(
                    Reason.UNSUPPORTED_SCHEMA, "transaction schema is unsupported");
            case SIZE_LIMIT_EXCEEDED -> failure(
                    Reason.CAPACITY_EXCEEDED, "transaction record exceeds a format bound");
            case CORRUPT_RECORD, FOREIGN_RECORD, NON_CANONICAL_RECORD -> failure(
                    Reason.CORRUPT_STATE, "transaction record failed validation");
        };
    }

    private Path transactionPath(UUID transactionId) {
        return storageRoot.resolve(InstallationRecordFileNames.transaction(transactionId));
    }

    private Path lockPath(UUID transactionId) {
        return storageRoot.resolve(InstallationRecordFileNames.transactionLock(transactionId));
    }

    private void deleteCandidate(Path candidate)
            throws InstallationTransactionStoreException {
        try {
            Files.deleteIfExists(candidate);
        } catch (IOException exception) {
            throw failure(Reason.STORE_UNAVAILABLE,
                    "transaction candidate could not be removed");
        }
    }

    private static void closeChannel(FileChannel channel) {
        try {
            channel.close();
        } catch (IOException ignored) {
            // The typed acquisition failure remains primary.
        }
    }

    private static InstallationTransactionStoreException failure(
            Reason reason,
            String detail) {
        return new InstallationTransactionStoreException(reason, detail);
    }

    private record StoredState(InstallationTransactionState state, byte[] bytes) {
        private StoredState {
            state = Objects.requireNonNull(state, "state must not be null");
            bytes = Objects.requireNonNull(bytes, "bytes must not be null").clone();
        }

        @Override
        public byte[] bytes() {
            return bytes.clone();
        }
    }

    private record HeldLock(FileChannel channel, FileLock lock) implements AutoCloseable {
        private HeldLock {
            channel = Objects.requireNonNull(channel, "channel must not be null");
            lock = Objects.requireNonNull(lock, "lock must not be null");
        }

        private void requireValid() throws InstallationTransactionStoreException {
            if (!channel.isOpen() || !lock.isValid()) {
                throw failure(Reason.STORE_UNAVAILABLE,
                        "transaction lock is no longer valid");
            }
        }

        @Override
        public void close() throws InstallationTransactionStoreException {
            try {
                lock.close();
                channel.close();
            } catch (IOException exception) {
                throw failure(Reason.STORE_UNAVAILABLE,
                        "transaction lock could not be released");
            }
        }
    }
}
