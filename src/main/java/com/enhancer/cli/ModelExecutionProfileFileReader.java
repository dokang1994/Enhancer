package com.enhancer.cli;

import com.enhancer.io.BoundedFileOperations;
import com.enhancer.io.FileSizeLimitExceededException;
import com.enhancer.model.ModelCostBudget;
import com.enhancer.model.ModelDataClassification;
import com.enhancer.model.ModelExecutionProfile;
import com.enhancer.model.ModelLocalityRequirement;
import com.enhancer.model.ModelReasoningRequirement;
import com.enhancer.model.ModelTokenBudget;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Objects;
import java.util.regex.Pattern;

/** Reads one complete interface-owned model execution profile without ambient lookup. */
final class ModelExecutionProfileFileReader {
    static final int MAX_PROFILE_BYTES = 4096;

    private static final Pattern POSITIVE_DECIMAL = Pattern.compile("[1-9][0-9]*");
    private static final Pattern NON_NEGATIVE_DECIMAL = Pattern.compile("0|[1-9][0-9]*");
    private static final String[] KEYS = {
            "schemaVersion",
            "requiredCapability",
            "modelClass",
            "localityRequirement",
            "reasoningRequirement",
            "minimumContextTokens",
            "tokenBudget.maxInputTokens",
            "tokenBudget.maxOutputTokens",
            "tokenBudget.maxTotalTokens",
            "costBudget.currencyCode",
            "costBudget.maxMicrounits",
            "maximumInvocationTimeMillis",
            "dataClassification"
    };

    ModelExecutionProfile read(Path projectRoot, Path profilePath) throws IOException {
        Path requestedRoot = Objects.requireNonNull(
                projectRoot, "projectRoot must not be null");
        Path requestedProfile = Objects.requireNonNull(
                profilePath, "profilePath must not be null");
        if (requestedProfile.toString().isBlank()
                || requestedProfile.isAbsolute()
                || requestedProfile.getRoot() != null) {
            throw new IllegalArgumentException(
                    "model execution profile path must be project-relative");
        }
        for (Path element : requestedProfile) {
            if (element.toString().equals("..")) {
                throw new IllegalArgumentException(
                        "model execution profile path must not contain traversal");
            }
        }

        Path realRoot = requestedRoot.toRealPath();
        if (!Files.isDirectory(realRoot, LinkOption.NOFOLLOW_LINKS)) {
            throw new IOException("project root must be a directory");
        }
        Path candidate = realRoot.resolve(requestedProfile).normalize();
        if (!candidate.startsWith(realRoot)) {
            throw new IllegalArgumentException(
                    "model execution profile path resolves outside the project root");
        }
        if (Files.isSymbolicLink(candidate)) {
            throw new IOException("model execution profile must not be a symbolic link");
        }

        Path realProfile = candidate.toRealPath();
        if (!realProfile.startsWith(realRoot)) {
            throw new IllegalArgumentException(
                    "model execution profile resolves outside the real project root");
        }
        if (!Files.isRegularFile(realProfile, LinkOption.NOFOLLOW_LINKS)) {
            throw new IOException("model execution profile must be a regular file");
        }

        byte[] bytes;
        try {
            bytes = BoundedFileOperations.readAllBytesNoFollow(
                    candidate, MAX_PROFILE_BYTES);
        } catch (FileSizeLimitExceededException exception) {
            throw new IOException("model execution profile exceeds the byte limit", exception);
        }
        return parse(decode(bytes));
    }

    private static String decode(byte[] bytes) throws IOException {
        try {
            return StandardCharsets.UTF_8
                    .newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes))
                    .toString();
        } catch (CharacterCodingException exception) {
            throw new IOException("model execution profile is not valid UTF-8", exception);
        }
    }

    private static ModelExecutionProfile parse(String document) {
        if (document.indexOf('\r') >= 0 || document.indexOf('\0') >= 0) {
            throw invalidDocument();
        }
        String[] lines = document.split("\n", -1);
        if (lines.length != KEYS.length + 1 || !lines[KEYS.length].isEmpty()) {
            throw invalidDocument();
        }

        String[] values = new String[KEYS.length];
        for (int index = 0; index < KEYS.length; index++) {
            String prefix = KEYS[index] + "=";
            String line = lines[index];
            if (!line.startsWith(prefix)
                    || line.indexOf('=', prefix.length()) >= 0) {
                throw invalidDocument();
            }
            String value = line.substring(prefix.length());
            if (value.isEmpty()) {
                throw invalidDocument();
            }
            values[index] = value;
        }

        try {
            long minimumContextTokens = positiveLong(values[5], KEYS[5]);
            ModelTokenBudget tokenBudget = new ModelTokenBudget(
                    positiveLong(values[6], KEYS[6]),
                    positiveLong(values[7], KEYS[7]),
                    positiveLong(values[8], KEYS[8]));
            ModelCostBudget costBudget = new ModelCostBudget(
                    values[9], nonNegativeLong(values[10], KEYS[10]));
            Duration maximumInvocationTime = Duration.ofMillis(
                    positiveLong(values[11], KEYS[11]));
            return new ModelExecutionProfile(
                    values[0],
                    values[1],
                    values[2],
                    exactEnum(ModelLocalityRequirement.class, values[3], KEYS[3]),
                    exactEnum(ModelReasoningRequirement.class, values[4], KEYS[4]),
                    minimumContextTokens,
                    tokenBudget,
                    costBudget,
                    maximumInvocationTime,
                    exactEnum(ModelDataClassification.class, values[12], KEYS[12]));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(
                    "model execution profile values are invalid", exception);
        }
    }

    private static long positiveLong(String value, String fieldName) {
        if (!POSITIVE_DECIMAL.matcher(value).matches()) {
            throw invalidField(fieldName);
        }
        return parseLong(value, fieldName);
    }

    private static long nonNegativeLong(String value, String fieldName) {
        if (!NON_NEGATIVE_DECIMAL.matcher(value).matches()) {
            throw invalidField(fieldName);
        }
        return parseLong(value, fieldName);
    }

    private static long parseLong(String value, String fieldName) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException exception) {
            throw invalidField(fieldName, exception);
        }
    }

    private static <T extends Enum<T>> T exactEnum(
            Class<T> type,
            String value,
            String fieldName) {
        try {
            return Enum.valueOf(type, value);
        } catch (IllegalArgumentException exception) {
            throw invalidField(fieldName, exception);
        }
    }

    private static IllegalArgumentException invalidDocument() {
        return new IllegalArgumentException(
                "model execution profile document is noncanonical");
    }

    private static IllegalArgumentException invalidField(String fieldName) {
        return new IllegalArgumentException(
                "model execution profile field is invalid: " + fieldName);
    }

    private static IllegalArgumentException invalidField(
            String fieldName,
            RuntimeException cause) {
        return new IllegalArgumentException(
                "model execution profile field is invalid: " + fieldName,
                cause);
    }
}
