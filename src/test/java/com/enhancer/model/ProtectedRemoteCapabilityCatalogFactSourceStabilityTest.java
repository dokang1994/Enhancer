package com.enhancer.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ProtectedRemoteCapabilityCatalogFactSourceStabilityTest {
    private static final Instant EVALUATION_TIME =
            Instant.parse("2026-10-23T00:01:00Z");
    private static final String FIRST_DIGEST = "1".repeat(64);
    private static final String SECOND_DIGEST = "2".repeat(64);

    @TempDir
    Path temporaryRoot;

    @Test
    void defaultDelegationCreatesFreshRfc0031SourceAndReturnsExactFacts()
            throws IOException {
        byte[] catalog = RemoteModelCapabilityCatalogTestFixture.oneRecord();
        String digest = RemoteModelCapabilityCatalogTestFixture.sha256(catalog);
        RemoteCapabilityCatalogBinding binding = installBinding(
                temporaryRoot, digest);
        Files.write(binding.catalogPath(), catalog);

        ProtectedRemoteCapabilityCatalogFactSourceResult.Acquired acquired =
                assertInstanceOf(
                        ProtectedRemoteCapabilityCatalogFactSourceResult.Acquired.class,
                        new ProtectedRemoteCapabilityCatalogFactSource(
                                bindingPath(temporaryRoot))
                                .acquire(
                                        "candidate-a",
                                        "destination-a",
                                        EVALUATION_TIME));

        assertEquals(binding.bindingSha256(), acquired.bindingSha256());
        assertEquals("candidate-a", acquired.facts().candidateIdentity());
        assertEquals("destination-a", acquired.facts().destinationIdentity());
        assertEquals(digest, acquired.facts().sourceRevision());
    }

    @Test
    void everyDelegatedRefusalIsRetainedOnlyAfterStableSecondSample()
            throws IOException {
        installBinding(temporaryRoot, FIRST_DIGEST);
        for (RemoteModelCandidateCapabilityFactSourceRefusalReason sourceReason
                : RemoteModelCandidateCapabilityFactSourceRefusalReason.values()) {
            AtomicInteger calls = new AtomicInteger();
            ProtectedRemoteCapabilityCatalogFactSource source =
                    new ProtectedRemoteCapabilityCatalogFactSource(
                            bindingPath(temporaryRoot),
                            (path, pin, candidate, destination, time) -> {
                                calls.incrementAndGet();
                                return new RemoteModelCandidateCapabilityFactSourceResult
                                        .Refused(sourceReason);
                            });

            ProtectedRemoteCapabilityCatalogFactSourceResult.Refused refused =
                    assertInstanceOf(
                            ProtectedRemoteCapabilityCatalogFactSourceResult.Refused.class,
                            source.acquire(
                                    "candidate-a",
                                    "destination-a",
                                    EVALUATION_TIME));
            assertEquals(1, calls.get());
            assertEquals(
                    ProtectedRemoteCapabilityCatalogFactSourceRefusalReason
                            .SOURCE_REFUSED,
                    refused.reason());
            assertEquals(Optional.of(sourceReason), refused.sourceReason());
        }
    }

    @Test
    void rotationDuringDelegationHidesEarlierAcquiredFacts() throws IOException {
        installBinding(temporaryRoot, FIRST_DIGEST);
        RemoteModelCandidateCapabilityFacts delegatedFacts =
                new PinnedFileRemoteModelCandidateCapabilityFactSourceTestFacts().facts();
        AtomicInteger calls = new AtomicInteger();
        ProtectedRemoteCapabilityCatalogFactSource source =
                new ProtectedRemoteCapabilityCatalogFactSource(
                        bindingPath(temporaryRoot),
                        (path, pin, candidate, destination, time) -> {
                            calls.incrementAndGet();
                            replaceBindingUnchecked(temporaryRoot, SECOND_DIGEST);
                            return new RemoteModelCandidateCapabilityFactSourceResult
                                    .Acquired(delegatedFacts);
                        });

        ProtectedRemoteCapabilityCatalogFactSourceResult.Refused refused =
                assertInstanceOf(
                        ProtectedRemoteCapabilityCatalogFactSourceResult.Refused.class,
                        source.acquire(
                                "candidate-a", "destination-a", EVALUATION_TIME));

        assertEquals(1, calls.get());
        assertEquals(
                ProtectedRemoteCapabilityCatalogFactSourceRefusalReason
                        .BINDING_CHANGED_DURING_ACQUISITION,
                refused.reason());
        assertEquals(Optional.empty(), refused.sourceReason());
    }

    @Test
    void secondSampleFailureOrInvalidityHidesEarlierSourceRefusal()
            throws IOException {
        Path missingCase = Files.createDirectory(temporaryRoot.resolve("missing-case"));
        installBinding(missingCase, FIRST_DIGEST);
        ProtectedRemoteCapabilityCatalogFactSource missingSource =
                new ProtectedRemoteCapabilityCatalogFactSource(
                        bindingPath(missingCase),
                        (path, pin, candidate, destination, time) -> {
                            deleteUnchecked(bindingPath(missingCase));
                            return new RemoteModelCandidateCapabilityFactSourceResult
                                    .Refused(RemoteModelCandidateCapabilityFactSourceRefusalReason
                                            .RECORD_NOT_FOUND);
                        });
        assertAcquisitionChanged(missingSource);

        Path invalidCase = Files.createDirectory(temporaryRoot.resolve("invalid-case"));
        installBinding(invalidCase, FIRST_DIGEST);
        ProtectedRemoteCapabilityCatalogFactSource invalidSource =
                new ProtectedRemoteCapabilityCatalogFactSource(
                        bindingPath(invalidCase),
                        (path, pin, candidate, destination, time) -> {
                            writeStringUnchecked(
                                    bindingPath(invalidCase), "not-canonical\n");
                            return new RemoteModelCandidateCapabilityFactSourceResult
                                    .Refused(RemoteModelCandidateCapabilityFactSourceRefusalReason
                                            .RECORD_NOT_FOUND);
                        });
        assertAcquisitionChanged(invalidSource);
    }

    @Test
    void replacementDuringFirstReadIsChangedDuringReadAndSkipsDelegation()
            throws IOException {
        installBinding(temporaryRoot, FIRST_DIGEST);
        AtomicInteger calls = new AtomicInteger();
        ProtectedRemoteCapabilityCatalogFactSource source =
                new ProtectedRemoteCapabilityCatalogFactSource(
                        bindingPath(temporaryRoot),
                        (path, pin, candidate, destination, time) -> {
                            calls.incrementAndGet();
                            return new RemoteModelCandidateCapabilityFactSourceResult
                                    .Refused(RemoteModelCandidateCapabilityFactSourceRefusalReason
                                            .RECORD_NOT_FOUND);
                        },
                        (path, sampleOrdinal) -> {
                            if (sampleOrdinal == 1) {
                                replaceBindingBytes(
                                        temporaryRoot, new byte[] {1});
                            }
                        });

        ProtectedRemoteCapabilityCatalogFactSourceResult.Refused refused =
                assertInstanceOf(
                        ProtectedRemoteCapabilityCatalogFactSourceResult.Refused.class,
                        source.acquire(
                                "candidate-a", "destination-a", EVALUATION_TIME));
        assertEquals(0, calls.get());
        assertEquals(
                ProtectedRemoteCapabilityCatalogFactSourceRefusalReason
                        .BINDING_CHANGED_DURING_READ,
                refused.reason());
    }

    @Test
    void replacementDuringSecondReadCollapsesToAcquisitionChange()
            throws IOException {
        installBinding(temporaryRoot, FIRST_DIGEST);
        AtomicInteger calls = new AtomicInteger();
        ProtectedRemoteCapabilityCatalogFactSource source =
                new ProtectedRemoteCapabilityCatalogFactSource(
                        bindingPath(temporaryRoot),
                        (path, pin, candidate, destination, time) -> {
                            calls.incrementAndGet();
                            return new RemoteModelCandidateCapabilityFactSourceResult
                                    .Refused(RemoteModelCandidateCapabilityFactSourceRefusalReason
                                            .RECORD_NOT_FOUND);
                        },
                        (path, sampleOrdinal) -> {
                            if (sampleOrdinal == 2) {
                                replaceBindingBytes(
                                        temporaryRoot, new byte[] {1});
                            }
                        });

        ProtectedRemoteCapabilityCatalogFactSourceResult.Refused refused =
                assertInstanceOf(
                        ProtectedRemoteCapabilityCatalogFactSourceResult.Refused.class,
                        source.acquire(
                                "candidate-a", "destination-a", EVALUATION_TIME));
        assertEquals(1, calls.get());
        assertEquals(
                ProtectedRemoteCapabilityCatalogFactSourceRefusalReason
                        .BINDING_CHANGED_DURING_ACQUISITION,
                refused.reason());
        assertEquals(Optional.empty(), refused.sourceReason());
    }

    @Test
    void repeatedCallsResampleBindingAndNeverCacheSelectedSource() throws IOException {
        RemoteCapabilityCatalogBinding first = installBinding(
                temporaryRoot, FIRST_DIGEST);
        RemoteModelCandidateCapabilityFacts facts =
                new PinnedFileRemoteModelCandidateCapabilityFactSourceTestFacts().facts();
        List<Path> delegatedPaths = new ArrayList<>();
        List<String> delegatedPins = new ArrayList<>();
        ProtectedRemoteCapabilityCatalogFactSource source =
                new ProtectedRemoteCapabilityCatalogFactSource(
                        bindingPath(temporaryRoot),
                        (path, pin, candidate, destination, time) -> {
                            delegatedPaths.add(path);
                            delegatedPins.add(pin);
                            return new RemoteModelCandidateCapabilityFactSourceResult
                                    .Acquired(facts);
                        });

        ProtectedRemoteCapabilityCatalogFactSourceResult.Acquired firstResult =
                assertInstanceOf(
                        ProtectedRemoteCapabilityCatalogFactSourceResult.Acquired.class,
                        source.acquire(
                                "candidate-a", "destination-a", EVALUATION_TIME));
        RemoteCapabilityCatalogBinding second =
                replaceBinding(temporaryRoot, SECOND_DIGEST);
        ProtectedRemoteCapabilityCatalogFactSourceResult.Acquired secondResult =
                assertInstanceOf(
                        ProtectedRemoteCapabilityCatalogFactSourceResult.Acquired.class,
                        source.acquire(
                                "candidate-a", "destination-a", EVALUATION_TIME));

        assertSame(facts, firstResult.facts());
        assertSame(facts, secondResult.facts());
        assertEquals(List.of(first.catalogPath(), second.catalogPath()), delegatedPaths);
        assertEquals(List.of(FIRST_DIGEST, SECOND_DIGEST), delegatedPins);
        assertEquals(first.bindingSha256(), firstResult.bindingSha256());
        assertEquals(second.bindingSha256(), secondResult.bindingSha256());
        assertTrue(!firstResult.bindingSha256().equals(secondResult.bindingSha256()));
    }

    private void assertAcquisitionChanged(
            ProtectedRemoteCapabilityCatalogFactSource source) {
        ProtectedRemoteCapabilityCatalogFactSourceResult.Refused refused =
                assertInstanceOf(
                        ProtectedRemoteCapabilityCatalogFactSourceResult.Refused.class,
                        source.acquire(
                                "candidate-a", "destination-a", EVALUATION_TIME));
        assertEquals(
                ProtectedRemoteCapabilityCatalogFactSourceRefusalReason
                        .BINDING_CHANGED_DURING_ACQUISITION,
                refused.reason());
        assertEquals(Optional.empty(), refused.sourceReason());
    }

    private static RemoteCapabilityCatalogBinding installBinding(
            Path root, String digest) throws IOException {
        Files.createDirectories(catalogDirectory(root));
        RemoteCapabilityCatalogBinding binding =
                RemoteCapabilityCatalogBindingCodec.create(
                        catalogDirectory(root), digest);
        Files.write(bindingPath(root), binding.canonicalBytes());
        return binding;
    }

    private static RemoteCapabilityCatalogBinding replaceBinding(
            Path root, String digest) throws IOException {
        RemoteCapabilityCatalogBinding binding =
                RemoteCapabilityCatalogBindingCodec.create(
                        catalogDirectory(root), digest);
        replaceBindingBytes(root, binding.canonicalBytes());
        return binding;
    }

    private static void replaceBindingBytes(
            Path root, byte[] bytes) throws IOException {
        Path replacement = root.resolve("binding-replacement");
        Files.write(replacement, bytes);
        Files.move(
                replacement,
                bindingPath(root),
                StandardCopyOption.REPLACE_EXISTING);
    }

    private static RemoteCapabilityCatalogBinding replaceBindingUnchecked(
            Path root, String digest) {
        try {
            return replaceBinding(root, digest);
        } catch (IOException exception) {
            throw new AssertionError(exception);
        }
    }

    private static void deleteUnchecked(Path path) {
        try {
            Files.delete(path);
        } catch (IOException exception) {
            throw new AssertionError(exception);
        }
    }

    private static void writeStringUnchecked(Path path, String value) {
        try {
            Files.writeString(path, value);
        } catch (IOException exception) {
            throw new AssertionError(exception);
        }
    }

    private static Path bindingPath(Path root) {
        return root.toAbsolutePath().normalize()
                .resolve(RemoteCapabilityCatalogBindingCodec.BINDING_NAME);
    }

    private static Path catalogDirectory(Path root) {
        return root.toAbsolutePath().normalize()
                .resolve(RemoteCapabilityCatalogBindingCodec.CATALOG_DIRECTORY_NAME);
    }
}
