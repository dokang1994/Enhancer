package com.enhancer.model;

import java.util.Objects;
import java.util.regex.Pattern;

/** One immutable bounded model response carried past the gateway boundary. */
public record ModelResponse(
        String text,
        String modelClass,
        ModelUsage usage) {

    public static final int MAX_TEXT_CHARACTERS = 262_144;

    private static final Pattern MODEL_CLASS =
            Pattern.compile("[a-z0-9]+(?:-[a-z0-9]+)*");

    public ModelResponse {
        Objects.requireNonNull(text, "text must not be null");
        Objects.requireNonNull(modelClass, "modelClass must not be null");
        Objects.requireNonNull(usage, "usage must not be null");

        if (text.length() > MAX_TEXT_CHARACTERS) {
            throw new IllegalArgumentException(
                    "text exceeds " + MAX_TEXT_CHARACTERS + " characters");
        }
        if (modelClass.length() > ModelRequest.MAX_MODEL_CLASS_CHARACTERS
                || !MODEL_CLASS.matcher(modelClass).matches()) {
            throw new IllegalArgumentException(
                    "modelClass must be a stable lowercase hyphenated label of at most "
                            + ModelRequest.MAX_MODEL_CLASS_CHARACTERS + " characters");
        }
    }
}
