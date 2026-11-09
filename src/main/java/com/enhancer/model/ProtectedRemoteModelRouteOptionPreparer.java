package com.enhancer.model;

import java.nio.file.Path;
import java.time.Instant;
import java.util.Objects;

/** Prepares one invocation-local remote route option through RFC-0033 and RFC-0030. */
public final class ProtectedRemoteModelRouteOptionPreparer {
    private final FactAcquirer factAcquirer;
    private final SuitabilityEvaluation suitabilityEvaluation;

    /** Binds preparation to one protected absolute normalized fixed binding path. */
    public ProtectedRemoteModelRouteOptionPreparer(Path bindingPath) {
        this.factAcquirer = new ProtectedRemoteCapabilityCatalogFactSource(
                bindingPath)::acquire;
        this.suitabilityEvaluation = new RemoteModelCandidateSuitabilityEvaluator()
                ::evaluate;
    }

    ProtectedRemoteModelRouteOptionPreparer(
            FactAcquirer factAcquirer,
            SuitabilityEvaluation suitabilityEvaluation) {
        this.factAcquirer = Objects.requireNonNull(
                factAcquirer, "factAcquirer must not be null");
        this.suitabilityEvaluation = Objects.requireNonNull(
                suitabilityEvaluation, "suitabilityEvaluation must not be null");
    }

    /** Acquires, evaluates, and prepares at most one exact route option. */
    public ProtectedRemoteModelRouteOptionPreparationResult prepare(
            ProfiledModelRequest profiledRequest,
            ModelOutboundRouteAdmissionDecision.Eligible eligibility,
            Instant evaluationTime) {
        Objects.requireNonNull(profiledRequest, "profiledRequest must not be null");
        Objects.requireNonNull(eligibility, "eligibility must not be null");
        Objects.requireNonNull(evaluationTime, "evaluationTime must not be null");

        String candidateIdentity = eligibility.proposal()
                .candidateFacts().candidateIdentity();
        String destinationIdentity = eligibility.proposal()
                .destinationPolicyFacts().destinationIdentity();
        ProtectedRemoteCapabilityCatalogFactSourceResult acquisition =
                factAcquirer.acquire(
                        candidateIdentity, destinationIdentity, evaluationTime);
        if (acquisition instanceof ProtectedRemoteCapabilityCatalogFactSourceResult
                .Refused refused) {
            return new ProtectedRemoteModelRouteOptionPreparationResult
                    .AcquisitionRefused(refused);
        }

        ProtectedRemoteCapabilityCatalogFactSourceResult.Acquired acquired =
                (ProtectedRemoteCapabilityCatalogFactSourceResult.Acquired) acquisition;
        RemoteModelCandidateSuitabilityDecision suitability =
                suitabilityEvaluation.evaluate(
                        profiledRequest,
                        eligibility,
                        acquired.facts(),
                        evaluationTime);
        if (suitability instanceof RemoteModelCandidateSuitabilityDecision
                .Refused refused) {
            return new ProtectedRemoteModelRouteOptionPreparationResult
                    .SuitabilityRefused(
                            refused.reason(), acquired.bindingSha256());
        }

        RemoteModelCandidateSuitabilityDecision.Suitable suitable =
                (RemoteModelCandidateSuitabilityDecision.Suitable) suitability;
        ModelRouteOption option = new ModelRouteOption(
                profiledRequest, eligibility, suitable.suitability());
        return new ProtectedRemoteModelRouteOptionPreparationResult.Prepared(
                option, acquired.bindingSha256());
    }

    @FunctionalInterface
    interface FactAcquirer {
        ProtectedRemoteCapabilityCatalogFactSourceResult acquire(
                String candidateIdentity,
                String destinationIdentity,
                Instant evaluationTime);
    }

    @FunctionalInterface
    interface SuitabilityEvaluation {
        RemoteModelCandidateSuitabilityDecision evaluate(
                ProfiledModelRequest profiledRequest,
                ModelOutboundRouteAdmissionDecision.Eligible eligibility,
                RemoteModelCandidateCapabilityFacts capabilityFacts,
                Instant evaluationTime);
    }
}
