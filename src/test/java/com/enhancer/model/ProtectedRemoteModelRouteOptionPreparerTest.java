package com.enhancer.model;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Path;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class ProtectedRemoteModelRouteOptionPreparerTest {

    @Test
    void publicConstructionAcceptsOnlyOneProtectedFixedBindingPath() {
        Path fixedBinding = Path.of(
                        "build",
                        "missing-rfc0034-installation",
                        RemoteCapabilityCatalogBindingCodec.BINDING_NAME)
                .toAbsolutePath()
                .normalize();

        assertDoesNotThrow(
                () -> new ProtectedRemoteModelRouteOptionPreparer(fixedBinding));
        assertThrows(NullPointerException.class,
                () -> new ProtectedRemoteModelRouteOptionPreparer(null));
        assertThrows(IllegalArgumentException.class,
                () -> new ProtectedRemoteModelRouteOptionPreparer(
                        Path.of(RemoteCapabilityCatalogBindingCodec.BINDING_NAME)));
        assertThrows(IllegalArgumentException.class,
                () -> new ProtectedRemoteModelRouteOptionPreparer(
                        fixedBinding.resolveSibling("request-selected.binding")));
    }

    @Test
    void nullProgrammingInputsFailBeforeAnyDelegation() {
        ProtectedRemoteModelRouteOptionPreparationTestFixture fixture =
                new ProtectedRemoteModelRouteOptionPreparationTestFixture();
        AtomicInteger acquisitions = new AtomicInteger();
        AtomicInteger evaluations = new AtomicInteger();
        ProtectedRemoteModelRouteOptionPreparer preparer =
                new ProtectedRemoteModelRouteOptionPreparer(
                        (candidate, destination, time) -> {
                            acquisitions.incrementAndGet();
                            return acquired(fixture);
                        },
                        (request, eligibility, facts, time) -> {
                            evaluations.incrementAndGet();
                            return suitable(fixture);
                        });

        assertThrows(NullPointerException.class,
                () -> preparer.prepare(
                        null, fixture.eligibility,
                        ProtectedRemoteModelRouteOptionPreparationTestFixture
                                .EVALUATION_TIME));
        assertThrows(NullPointerException.class,
                () -> preparer.prepare(
                        fixture.profiledRequest, null,
                        ProtectedRemoteModelRouteOptionPreparationTestFixture
                                .EVALUATION_TIME));
        assertThrows(NullPointerException.class,
                () -> preparer.prepare(
                        fixture.profiledRequest, fixture.eligibility, null));

        assertEquals(0, acquisitions.get());
        assertEquals(0, evaluations.get());
    }

    @Test
    void everyAcquisitionRefusalIsRetainedExactlyAndSuppressesSuitability() {
        ProtectedRemoteModelRouteOptionPreparationTestFixture fixture =
                new ProtectedRemoteModelRouteOptionPreparationTestFixture();
        for (ProtectedRemoteCapabilityCatalogFactSourceRefusalReason reason
                : ProtectedRemoteCapabilityCatalogFactSourceRefusalReason.values()) {
            Optional<RemoteModelCandidateCapabilityFactSourceRefusalReason> sourceReason =
                    reason == ProtectedRemoteCapabilityCatalogFactSourceRefusalReason
                            .SOURCE_REFUSED
                    ? Optional.of(
                            RemoteModelCandidateCapabilityFactSourceRefusalReason
                                    .RECORD_NOT_FOUND)
                    : Optional.empty();
            ProtectedRemoteCapabilityCatalogFactSourceResult.Refused refusal =
                    new ProtectedRemoteCapabilityCatalogFactSourceResult.Refused(
                            reason, sourceReason);
            AtomicInteger acquisitions = new AtomicInteger();
            AtomicInteger evaluations = new AtomicInteger();
            ProtectedRemoteModelRouteOptionPreparer preparer =
                    new ProtectedRemoteModelRouteOptionPreparer(
                            (candidate, destination, time) -> {
                                acquisitions.incrementAndGet();
                                assertSame(fixture.eligibility.proposal()
                                        .candidateFacts().candidateIdentity(), candidate);
                                assertSame(fixture.eligibility.proposal()
                                        .destinationPolicyFacts()
                                        .destinationIdentity(), destination);
                                assertSame(
                                        ProtectedRemoteModelRouteOptionPreparationTestFixture
                                                .EVALUATION_TIME,
                                        time);
                                return refusal;
                            },
                            (request, eligibility, facts, time) -> {
                                evaluations.incrementAndGet();
                                return suitable(fixture);
                            });

            ProtectedRemoteModelRouteOptionPreparationResult.AcquisitionRefused result =
                    assertInstanceOf(
                            ProtectedRemoteModelRouteOptionPreparationResult
                                    .AcquisitionRefused.class,
                            prepare(preparer, fixture));

            assertSame(refusal, result.refusal());
            assertEquals(1, acquisitions.get());
            assertEquals(0, evaluations.get());
        }
    }

    @Test
    void everySuitabilityRefusalRetainsDigestAndExactInputs() {
        ProtectedRemoteModelRouteOptionPreparationTestFixture fixture =
                new ProtectedRemoteModelRouteOptionPreparationTestFixture();
        for (RemoteModelCandidateSuitabilityRefusalReason reason
                : RemoteModelCandidateSuitabilityRefusalReason.values()) {
            AtomicInteger acquisitions = new AtomicInteger();
            AtomicInteger evaluations = new AtomicInteger();
            AtomicReference<ProfiledModelRequest> observedRequest =
                    new AtomicReference<>();
            AtomicReference<ModelOutboundRouteAdmissionDecision.Eligible>
                    observedEligibility = new AtomicReference<>();
            AtomicReference<RemoteModelCandidateCapabilityFacts> observedFacts =
                    new AtomicReference<>();
            AtomicReference<Instant> observedTime = new AtomicReference<>();
            ProtectedRemoteModelRouteOptionPreparer preparer =
                    new ProtectedRemoteModelRouteOptionPreparer(
                            (candidate, destination, time) -> {
                                acquisitions.incrementAndGet();
                                return acquired(fixture);
                            },
                            (request, eligibility, facts, time) -> {
                                evaluations.incrementAndGet();
                                observedRequest.set(request);
                                observedEligibility.set(eligibility);
                                observedFacts.set(facts);
                                observedTime.set(time);
                                return new RemoteModelCandidateSuitabilityDecision
                                        .Refused(reason);
                            });

            ProtectedRemoteModelRouteOptionPreparationResult.SuitabilityRefused result =
                    assertInstanceOf(
                            ProtectedRemoteModelRouteOptionPreparationResult
                                    .SuitabilityRefused.class,
                            prepare(preparer, fixture));

            assertSame(reason, result.reason());
            assertEquals(
                    ProtectedRemoteModelRouteOptionPreparationTestFixture.BINDING_SHA_256,
                    result.bindingSha256());
            assertEquals(1, acquisitions.get());
            assertEquals(1, evaluations.get());
            assertSame(fixture.profiledRequest, observedRequest.get());
            assertSame(fixture.eligibility, observedEligibility.get());
            assertSame(fixture.capabilityFacts, observedFacts.get());
            assertSame(
                    ProtectedRemoteModelRouteOptionPreparationTestFixture.EVALUATION_TIME,
                    observedTime.get());
        }
    }

    @Test
    void suitableAcquisitionConstructsOneExactFreshOption() {
        ProtectedRemoteModelRouteOptionPreparationTestFixture fixture =
                new ProtectedRemoteModelRouteOptionPreparationTestFixture();
        AtomicInteger acquisitions = new AtomicInteger();
        AtomicInteger evaluations = new AtomicInteger();
        ProtectedRemoteModelRouteOptionPreparer preparer =
                new ProtectedRemoteModelRouteOptionPreparer(
                        (candidate, destination, time) -> {
                            acquisitions.incrementAndGet();
                            return acquired(fixture);
                        },
                        (request, eligibility, facts, time) -> {
                            evaluations.incrementAndGet();
                            assertSame(fixture.profiledRequest, request);
                            assertSame(fixture.eligibility, eligibility);
                            assertSame(fixture.capabilityFacts, facts);
                            assertSame(
                                    ProtectedRemoteModelRouteOptionPreparationTestFixture
                                            .EVALUATION_TIME,
                                    time);
                            return suitable(fixture);
                        });

        ProtectedRemoteModelRouteOptionPreparationResult.Prepared result =
                assertInstanceOf(
                        ProtectedRemoteModelRouteOptionPreparationResult.Prepared.class,
                        prepare(preparer, fixture));

        assertSame(fixture.profiledRequest, result.option().profiledRequest());
        assertSame(fixture.eligibility, result.option().eligibility());
        assertSame(fixture.suitability, result.option().suitability());
        assertEquals(
                ProtectedRemoteModelRouteOptionPreparationTestFixture.BINDING_SHA_256,
                result.bindingSha256());
        assertEquals(1, acquisitions.get());
        assertEquals(1, evaluations.get());
    }

    @Test
    void repeatedCallsAlwaysReacquireReevaluateAndCreateFreshOptions() {
        ProtectedRemoteModelRouteOptionPreparationTestFixture fixture =
                new ProtectedRemoteModelRouteOptionPreparationTestFixture();
        AtomicInteger acquisitions = new AtomicInteger();
        AtomicInteger evaluations = new AtomicInteger();
        ProtectedRemoteModelRouteOptionPreparer preparer =
                new ProtectedRemoteModelRouteOptionPreparer(
                        (candidate, destination, time) -> {
                            acquisitions.incrementAndGet();
                            return acquired(fixture);
                        },
                        (request, eligibility, facts, time) -> {
                            evaluations.incrementAndGet();
                            return suitable(fixture);
                        });

        ProtectedRemoteModelRouteOptionPreparationResult.Prepared first =
                assertInstanceOf(
                        ProtectedRemoteModelRouteOptionPreparationResult.Prepared.class,
                        prepare(preparer, fixture));
        ProtectedRemoteModelRouteOptionPreparationResult.Prepared second =
                assertInstanceOf(
                        ProtectedRemoteModelRouteOptionPreparationResult.Prepared.class,
                        prepare(preparer, fixture));

        assertEquals(2, acquisitions.get());
        assertEquals(2, evaluations.get());
        assertNotSame(first.option(), second.option());
        assertSame(first.option().suitability(), second.option().suitability());
    }

    @Test
    void unexpectedExceptionsPropagateWithoutResultWrapping() {
        ProtectedRemoteModelRouteOptionPreparationTestFixture fixture =
                new ProtectedRemoteModelRouteOptionPreparationTestFixture();
        IllegalStateException acquisitionFailure =
                new IllegalStateException("acquisition failure sentinel");
        AtomicInteger acquisitionSuppressedEvaluations = new AtomicInteger();
        ProtectedRemoteModelRouteOptionPreparer acquisitionFailurePreparer =
                new ProtectedRemoteModelRouteOptionPreparer(
                        (candidate, destination, time) -> {
                            throw acquisitionFailure;
                        },
                        (request, eligibility, facts, time) -> {
                            acquisitionSuppressedEvaluations.incrementAndGet();
                            return suitable(fixture);
                        });
        assertSame(
                acquisitionFailure,
                assertThrows(
                        IllegalStateException.class,
                        () -> prepare(acquisitionFailurePreparer, fixture)));
        assertEquals(0, acquisitionSuppressedEvaluations.get());

        IllegalStateException evaluationFailure =
                new IllegalStateException("evaluation failure sentinel");
        ProtectedRemoteModelRouteOptionPreparer evaluationFailurePreparer =
                new ProtectedRemoteModelRouteOptionPreparer(
                        (candidate, destination, time) -> acquired(fixture),
                        (request, eligibility, facts, time) -> {
                            throw evaluationFailure;
                        });
        assertSame(
                evaluationFailure,
                assertThrows(
                        IllegalStateException.class,
                        () -> prepare(evaluationFailurePreparer, fixture)));
    }

    private static ProtectedRemoteModelRouteOptionPreparationResult prepare(
            ProtectedRemoteModelRouteOptionPreparer preparer,
            ProtectedRemoteModelRouteOptionPreparationTestFixture fixture) {
        return preparer.prepare(
                fixture.profiledRequest,
                fixture.eligibility,
                ProtectedRemoteModelRouteOptionPreparationTestFixture.EVALUATION_TIME);
    }

    private static ProtectedRemoteCapabilityCatalogFactSourceResult.Acquired acquired(
            ProtectedRemoteModelRouteOptionPreparationTestFixture fixture) {
        return new ProtectedRemoteCapabilityCatalogFactSourceResult.Acquired(
                fixture.capabilityFacts,
                ProtectedRemoteModelRouteOptionPreparationTestFixture.BINDING_SHA_256);
    }

    private static RemoteModelCandidateSuitabilityDecision.Suitable suitable(
            ProtectedRemoteModelRouteOptionPreparationTestFixture fixture) {
        return new RemoteModelCandidateSuitabilityDecision.Suitable(
                fixture.suitability);
    }
}
