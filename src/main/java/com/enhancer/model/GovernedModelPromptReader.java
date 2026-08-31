package com.enhancer.model;

import com.enhancer.io.BoundedFileOperations;
import com.enhancer.io.FileSizeLimitExceededException;
import com.enhancer.tool.ExecutionPolicy;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Objects;

/** Reads one governed model prompt file under the existing policy containment and byte bounds. */
public final class GovernedModelPromptReader {

    public String readFile(String promptPath, ExecutionPolicy policy) throws IOException {
        Objects.requireNonNull(promptPath, "promptPath must not be null");
        Objects.requireNonNull(policy, "policy must not be null");
        Path relativePath = Path.of(promptPath);
        if (relativePath.isAbsolute()) {
            throw new IllegalArgumentException(
                    "prompt-path must be relative to the project root");
        }

        Path normalizedRoot = policy.projectRoot();
        Path candidate = normalizedRoot.resolve(relativePath).normalize();
        if (!candidate.startsWith(normalizedRoot)) {
            throw new SecurityException("prompt-path resolves outside the project root");
        }

        Path realRoot;
        try {
            realRoot = normalizedRoot.toRealPath();
        } catch (NoSuchFileException exception) {
            throw new IOException("project root not found", exception);
        }
        if (!Files.isDirectory(realRoot)) {
            throw new IOException("project root must be a directory");
        }
        Path realFile;
        try {
            realFile = candidate.toRealPath();
        } catch (NoSuchFileException exception) {
            throw new IOException("prompt file not found: " + promptPath, exception);
        }
        if (!realFile.startsWith(realRoot)) {
            throw new SecurityException(
                    "prompt-path resolves outside the real project root");
        }
        if (!Files.isRegularFile(realFile)) {
            throw new IOException("prompt-path must identify a regular file");
        }
        if (Files.size(realFile) > policy.maxReadBytes()) {
            throw new IOException("prompt file size exceeds policy limit");
        }

        byte[] bytes;
        try {
            bytes = BoundedFileOperations.readAllBytes(realFile, policy.maxReadBytes());
        } catch (FileSizeLimitExceededException exception) {
            throw new IOException(
                    "prompt file size changed beyond policy limit while reading",
                    exception);
        }
        try {
            return StandardCharsets.UTF_8
                    .newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes))
                    .toString();
        } catch (CharacterCodingException exception) {
            throw new IOException("prompt file is not valid UTF-8", exception);
        }
    }
}
