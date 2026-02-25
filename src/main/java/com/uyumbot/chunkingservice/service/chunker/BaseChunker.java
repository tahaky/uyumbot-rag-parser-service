package com.uyumbot.chunkingservice.service.chunker;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Abstract base chunker that all format-specific chunkers extend.
 * Provides stable chunk ID generation and common chunk creation utilities.
 */
public abstract class BaseChunker {

    protected final int maxChunkSize;

    protected BaseChunker(int maxChunkSize) {
        this.maxChunkSize = maxChunkSize;
    }

    protected BaseChunker() {
        this.maxChunkSize = 800;
    }

    /**
     * Chunk a parsed document structure into a list of chunk maps.
     *
     * @param parsedStructure format-specific parsed document map
     * @return list of chunk maps containing stable_id, text, type, hash, metadata, word_count, char_count
     */
    public abstract List<Map<String, Object>> chunk(Map<String, Object> parsedStructure);

    /**
     * Build a standardised chunk map.
     */
    protected Map<String, Object> createChunk(String stableId, String text, String chunkType, Map<String, Object> metadata) {
        Map<String, Object> chunk = new HashMap<>();
        chunk.put("stable_id", stableId);
        chunk.put("text", text);
        chunk.put("type", chunkType);
        chunk.put("hash", md5(text));
        chunk.put("metadata", metadata != null ? metadata : new HashMap<>());
        chunk.put("word_count", countWords(text));
        chunk.put("char_count", text.length());
        return chunk;
    }

    /** MD5 hex of the given text (same algorithm as the Python service). */
    protected String md5(String text) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(text.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("MD5 not available", e);
        }
    }

    /** Count words by splitting on whitespace. */
    protected int countWords(String text) {
        if (text == null || text.isBlank()) return 0;
        return text.trim().split("\\s+").length;
    }

    /**
     * Normalise a heading string for use as a stable path segment.
     * Mirrors the Python {@code normalize_heading} utility.
     */
    protected String normalizeHeading(String heading) {
        if (heading == null) return "";
        return heading.trim()
                .toLowerCase()
                .replaceAll("[^a-z0-9\\s_-]", "")
                .replaceAll("\\s+", "_")
                .replaceAll("_+", "_")
                .replaceAll("^_|_$", "");
    }

    /** Format a table structure (list of rows) as a Markdown table string. */
    protected String formatTable(Map<String, Object> table) {
        @SuppressWarnings("unchecked")
        List<Object> rows = (List<Object>) table.get("rows");
        if (rows == null || rows.isEmpty()) return "";

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < rows.size(); i++) {
            Object row = rows.get(i);
            if (row instanceof List<?> cells) {
                sb.append("| ").append(cells.stream().map(Object::toString)
                        .collect(java.util.stream.Collectors.joining(" | "))).append(" |");
            } else {
                sb.append("| ").append(row).append(" |");
            }
            sb.append("\n");
            if (i == 0) {
                // separator row
                int cols = (row instanceof List<?> l) ? l.size() : 1;
                sb.append("|").append("---|".repeat(cols)).append("\n");
            }
        }
        return sb.toString().trim();
    }
}
