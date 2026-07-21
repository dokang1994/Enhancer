package com.enhancer.session;

import com.enhancer.io.BoundedFileOperations;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.HexFormat;

final class DevelopmentTaskContractReader {
    private static final long MAX_DOCUMENT_BYTES = 1024 * 1024;
    private static final List<String> CONTRACT_SECTIONS = List.of(
            "Task ID",
            "Task",
            "Justified By",
            "Acceptance Criteria",
            "Out Of Scope",
            "Approval");

    DevelopmentTaskContract read(Path projectRoot) throws IOException {
        Objects.requireNonNull(projectRoot, "projectRoot must not be null");
        Path realRoot = projectRoot.toRealPath();
        Path document = realRoot.resolve("CURRENT_TASK.md");
        if (!Files.isRegularFile(document, LinkOption.NOFOLLOW_LINKS)) {
            throw new IOException("CURRENT_TASK.md must be a regular file");
        }
        String content = decode(BoundedFileOperations.readAllBytes(
                document,
                MAX_DOCUMENT_BYTES));
        Map<String, String> sections = sections(content);
        String taskId = singleLine(required(sections, "Task ID"), "Task ID");
        String status = singleLine(required(sections, "Status"), "Status");
        StringBuilder canonical = new StringBuilder();
        for (String section : CONTRACT_SECTIONS) {
            canonical.append("## ")
                    .append(section)
                    .append('\n')
                    .append(required(sections, section))
                    .append('\n');
        }
        return new DevelopmentTaskContract(
                taskId,
                status,
                HexFormat.of().formatHex(sha256(canonical.toString()
                        .getBytes(StandardCharsets.UTF_8))));
    }

    private Map<String, String> sections(String content) throws IOException {
        Map<String, StringBuilder> builders = new LinkedHashMap<>();
        String current = null;
        for (String line : content.split("\\R", -1)) {
            if (line.startsWith("## ")) {
                current = line.substring(3).strip();
                if (current.isEmpty() || builders.putIfAbsent(
                        current, new StringBuilder()) != null) {
                    throw new IOException("CURRENT_TASK.md contains an invalid section heading");
                }
            } else if (current != null) {
                builders.get(current).append(line).append('\n');
            }
        }
        Map<String, String> result = new LinkedHashMap<>();
        for (Map.Entry<String, StringBuilder> entry : builders.entrySet()) {
            result.put(entry.getKey(), entry.getValue().toString().strip());
        }
        Set<String> required = Set.of(
                "Status", "Task ID", "Task", "Justified By",
                "Acceptance Criteria", "Out Of Scope", "Approval");
        if (!result.keySet().containsAll(required)) {
            throw new IOException("CURRENT_TASK.md is missing a checkpoint contract section");
        }
        return Map.copyOf(result);
    }

    private String required(Map<String, String> sections, String name) throws IOException {
        String value = sections.get(name);
        if (value == null || value.isBlank()) {
            throw new IOException("CURRENT_TASK.md section is empty: " + name);
        }
        return value;
    }

    private String singleLine(String value, String name) throws IOException {
        if (value.lines().count() != 1) {
            throw new IOException("CURRENT_TASK.md section must contain one line: " + name);
        }
        return value;
    }

    private String decode(byte[] bytes) throws IOException {
        try {
            CharBuffer decoded = StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes));
            return decoded.toString();
        } catch (CharacterCodingException exception) {
            throw new IOException("CURRENT_TASK.md is not valid UTF-8", exception);
        }
    }

    private byte[] sha256(byte[] bytes) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(bytes);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
