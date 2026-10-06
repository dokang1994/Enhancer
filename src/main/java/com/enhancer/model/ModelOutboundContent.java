package com.enhancer.model;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;

/** Exact bounded prompt projection that may become model-visible. */
public final class ModelOutboundContent {

    public static final String SCHEMA_VERSION = "model-outbound-content-v1";

    private final String sourcePromptDigest;
    private final String outboundPrompt;
    private final String outboundPromptDigest;
    private final ModelOutboundContentTransformation transformation;

    private ModelOutboundContent(
            String sourcePrompt,
            String outboundPrompt,
            ModelOutboundContentTransformation transformation) {
        validatePrompt(sourcePrompt, "sourcePrompt");
        validatePrompt(outboundPrompt, "outboundPrompt");
        this.transformation = Objects.requireNonNull(
                transformation, "transformation must not be null");
        if (transformation == ModelOutboundContentTransformation.UNCHANGED
                && !sourcePrompt.equals(outboundPrompt)) {
            throw new IllegalArgumentException(
                    "UNCHANGED content must exactly equal the source prompt");
        }
        if (transformation == ModelOutboundContentTransformation.REDACTED
                && sourcePrompt.equals(outboundPrompt)) {
            throw new IllegalArgumentException(
                    "REDACTED content must differ from the source prompt");
        }
        this.sourcePromptDigest = digestOf(sourcePrompt);
        this.outboundPrompt = outboundPrompt;
        this.outboundPromptDigest = digestOf(outboundPrompt);
    }

    public static ModelOutboundContent unchanged(String sourcePrompt) {
        return new ModelOutboundContent(
                sourcePrompt,
                sourcePrompt,
                ModelOutboundContentTransformation.UNCHANGED);
    }

    public static ModelOutboundContent redacted(String sourcePrompt, String outboundPrompt) {
        return new ModelOutboundContent(
                sourcePrompt,
                outboundPrompt,
                ModelOutboundContentTransformation.REDACTED);
    }

    private static void validatePrompt(String prompt, String fieldName) {
        Objects.requireNonNull(prompt, fieldName + " must not be null");
        if (prompt.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        if (prompt.length() > ModelRequest.MAX_PROMPT_CHARACTERS) {
            throw new IllegalArgumentException(fieldName + " exceeds the supported bound");
        }
    }

    static String digestOf(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    public String schemaVersion() {
        return SCHEMA_VERSION;
    }

    public String sourcePromptDigest() {
        return sourcePromptDigest;
    }

    public String outboundPrompt() {
        return outboundPrompt;
    }

    public String outboundPromptDigest() {
        return outboundPromptDigest;
    }

    public ModelOutboundContentTransformation transformation() {
        return transformation;
    }

    @Override
    public String toString() {
        return "ModelOutboundContent[sourcePromptDigest=" + sourcePromptDigest
                + ", outboundPromptDigest=" + outboundPromptDigest
                + ", transformation=" + transformation + "]";
    }
}
