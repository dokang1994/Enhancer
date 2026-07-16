package com.enhancer.verification;

import com.enhancer.kernel.VerificationDecision;

@FunctionalInterface
public interface IndependentVerifier {
    VerificationDecision verify(VerificationRequest request);
}
