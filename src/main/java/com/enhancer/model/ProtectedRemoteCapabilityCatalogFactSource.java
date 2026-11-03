package com.enhancer.model;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Acquires RFC-0031 facts through one protected RFC-0032 fixed binding.
 *
 * <p>The binding path is protected construction input. Each call samples the binding
 * before and after exactly one delegated RFC-0031 acquisition.
 */
public final class ProtectedRemoteCapabilityCatalogFactSource {
    private static final long MAX_BINDING_BYTES = 4L * 1024;
    private static final Pattern LABEL =
            Pattern.compile("[a-z0-9]+(?:-[a-z0-9]+)*");

    private final Path bindingPath;
    private final Delegate delegate;

    /** Creates a read-only source from one protected absolute fixed binding path. */
    public ProtectedRemoteCapabilityCatalogFactSource(Path bindingPath) {
        this(bindingPath, (catalogPath, catalogSha256, candidateIdentity,
                destinationIdentity, evaluationTime) ->
                new PinnedFileRemoteModelCandidateCapabilityFactSource(
                        catalogPath, catalogSha256)
                        .acquire(candidateIdentity, destinationIdentity, evaluationTime));
    }

    ProtectedRemoteCapabilityCatalogFactSource(
            Path bindingPath,
            Delegate delegate) {
        Path checked = Objects.requireNonNull(
                bindingPath, "bindingPath must not be null");
        if (!checked.isAbsolute() || !checked.equals(checked.normalize())
                || checked.getParent() == null
                || !RemoteCapabilityCatalogBindingCodec.BINDING_NAME.equals(
                        checked.getFileName().toString())) {
            throw new IllegalArgumentException(
                    "bindingPath must be an absolute normalized fixed binding path");
        }
        this.bindingPath = checked;
        this.delegate = Objects.requireNonNull(delegate, "delegate must not be null");
    }

    /** Performs one stable sequential binding observation around one RFC-0031 call. */
    public ProtectedRemoteCapabilityCatalogFactSourceResult acquire(
            String candidateIdentity,
            String destinationIdentity,
            Instant evaluationTime) {
        programmingLabel(candidateIdentity, "candidateIdentity");
        programmingLabel(destinationIdentity, "destinationIdentity");
        Objects.requireNonNull(evaluationTime, "evaluationTime must not be null");

        BindingSample first = sampleBinding();
        if (first.refusal != null) {
            return refused(first.refusal);
        }

        RemoteModelCandidateCapabilityFactSourceResult delegated = delegate.acquire(
                first.binding.catalogPath(),
                first.binding.catalogSha256(),
                candidateIdentity,
                destinationIdentity,
                evaluationTime);

        BindingSample second = sampleBinding();
        if (second.refusal != null || !first.binding.equals(second.binding)) {
            return refused(ProtectedRemoteCapabilityCatalogFactSourceRefusalReason
                    .BINDING_CHANGED_DURING_ACQUISITION);
        }
        if (delegated instanceof RemoteModelCandidateCapabilityFactSourceResult
                .Acquired acquired) {
            return new ProtectedRemoteCapabilityCatalogFactSourceResult.Acquired(
                    acquired.facts(), first.binding.bindingSha256());
        }
        RemoteModelCandidateCapabilityFactSourceResult.Refused sourceRefused =
                (RemoteModelCandidateCapabilityFactSourceResult.Refused) delegated;
        return new ProtectedRemoteCapabilityCatalogFactSourceResult.Refused(
                ProtectedRemoteCapabilityCatalogFactSourceRefusalReason.SOURCE_REFUSED,
                Optional.of(sourceRefused.reason()));
    }

    private BindingSample sampleBinding() {
        Path installationRoot = bindingPath.getParent();
        Path catalogDirectory = installationRoot.resolve(
                RemoteCapabilityCatalogBindingCodec.CATALOG_DIRECTORY_NAME);
        if (!safeDirectory(installationRoot) || !safeDirectory(catalogDirectory)) {
            return BindingSample.refused(
                    ProtectedRemoteCapabilityCatalogFactSourceRefusalReason
                            .INSTALLATION_ROOT_UNAVAILABLE_OR_UNSAFE);
        }
        if (!safeBindingFile()) {
            return BindingSample.refused(
                    ProtectedRemoteCapabilityCatalogFactSourceRefusalReason
                            .BINDING_UNAVAILABLE_OR_UNSAFE);
        }

        BasicFileAttributes before;
        try {
            before = Files.readAttributes(
                    bindingPath,
                    BasicFileAttributes.class,
                    LinkOption.NOFOLLOW_LINKS);
        } catch (IOException | SecurityException exception) {
            return BindingSample.refused(
                    ProtectedRemoteCapabilityCatalogFactSourceRefusalReason
                            .BINDING_UNAVAILABLE_OR_UNSAFE);
        }
        if (before.size() < 1 || before.size() > MAX_BINDING_BYTES) {
            return BindingSample.refused(
                    ProtectedRemoteCapabilityCatalogFactSourceRefusalReason
                            .BINDING_OUTSIDE_SIZE_BOUND);
        }

        byte[] bytes;
        try (FileChannel channel = FileChannel.open(
                bindingPath,
                StandardOpenOption.READ,
                LinkOption.NOFOLLOW_LINKS)) {
            if (channel.size() != before.size()) {
                return BindingSample.refused(
                        ProtectedRemoteCapabilityCatalogFactSourceRefusalReason
                                .BINDING_CHANGED_DURING_READ);
            }
            bytes = PinnedFileRemoteModelCandidateCapabilityFactSource
                    .readDeclaredBytes(channel, before.size());
        } catch (IOException | SecurityException exception) {
            return BindingSample.refused(
                    ProtectedRemoteCapabilityCatalogFactSourceRefusalReason
                            .BINDING_CHANGED_DURING_READ);
        }

        BasicFileAttributes after;
        try {
            after = Files.readAttributes(
                    bindingPath,
                    BasicFileAttributes.class,
                    LinkOption.NOFOLLOW_LINKS);
        } catch (IOException | SecurityException exception) {
            return BindingSample.refused(
                    ProtectedRemoteCapabilityCatalogFactSourceRefusalReason
                            .BINDING_CHANGED_DURING_READ);
        }
        if (!sameIdentity(before, after)
                || !safeDirectory(installationRoot)
                || !safeDirectory(catalogDirectory)
                || !safeBindingFile()) {
            return BindingSample.refused(
                    ProtectedRemoteCapabilityCatalogFactSourceRefusalReason
                            .BINDING_CHANGED_DURING_READ);
        }

        RemoteCapabilityCatalogBindingCodec.DecodeResult decoded =
                RemoteCapabilityCatalogBindingCodec.decode(catalogDirectory, bytes);
        if (!(decoded instanceof RemoteCapabilityCatalogBindingCodec.Decoded valid)) {
            return BindingSample.refused(
                    ProtectedRemoteCapabilityCatalogFactSourceRefusalReason
                            .BINDING_INVALID);
        }
        return BindingSample.acquired(valid.binding());
    }

    private boolean safeBindingFile() {
        try {
            return !Files.isSymbolicLink(bindingPath)
                    && Files.isRegularFile(bindingPath, LinkOption.NOFOLLOW_LINKS)
                    && bindingPath.toRealPath().equals(bindingPath);
        } catch (IOException | SecurityException exception) {
            return false;
        }
    }

    private static boolean safeDirectory(Path directory) {
        try {
            return !Files.isSymbolicLink(directory)
                    && Files.isDirectory(directory, LinkOption.NOFOLLOW_LINKS)
                    && directory.toRealPath().equals(directory);
        } catch (IOException | SecurityException exception) {
            return false;
        }
    }

    private static boolean sameIdentity(
            BasicFileAttributes before,
            BasicFileAttributes after) {
        return before.size() == after.size()
                && Objects.equals(before.fileKey(), after.fileKey())
                && before.creationTime().equals(after.creationTime())
                && before.lastModifiedTime().equals(after.lastModifiedTime());
    }

    private static void programmingLabel(String value, String name) {
        Objects.requireNonNull(value, name + " must not be null");
        if (value.length() > 256 || !LABEL.matcher(value).matches()) {
            throw new IllegalArgumentException(
                    name + " must be a bounded stable lowercase label");
        }
    }

    private static ProtectedRemoteCapabilityCatalogFactSourceResult refused(
            ProtectedRemoteCapabilityCatalogFactSourceRefusalReason reason) {
        return new ProtectedRemoteCapabilityCatalogFactSourceResult.Refused(
                reason, Optional.empty());
    }

    @FunctionalInterface
    interface Delegate {
        RemoteModelCandidateCapabilityFactSourceResult acquire(
                Path catalogPath,
                String catalogSha256,
                String candidateIdentity,
                String destinationIdentity,
                Instant evaluationTime);
    }

    private static final class BindingSample {
        private final RemoteCapabilityCatalogBinding binding;
        private final ProtectedRemoteCapabilityCatalogFactSourceRefusalReason refusal;

        private BindingSample(
                RemoteCapabilityCatalogBinding binding,
                ProtectedRemoteCapabilityCatalogFactSourceRefusalReason refusal) {
            this.binding = binding;
            this.refusal = refusal;
        }

        private static BindingSample acquired(
                RemoteCapabilityCatalogBinding binding) {
            return new BindingSample(
                    Objects.requireNonNull(binding, "binding must not be null"), null);
        }

        private static BindingSample refused(
                ProtectedRemoteCapabilityCatalogFactSourceRefusalReason reason) {
            return new BindingSample(
                    null, Objects.requireNonNull(reason, "reason must not be null"));
        }
    }
}
