package com.enhancer.verification;

@FunctionalInterface
public interface IndependentVerifier {
    VerificationDecision verify(VerificationRequest request);
}
