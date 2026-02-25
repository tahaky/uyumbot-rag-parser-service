package com.uyumbot.chunkingservice.service.chunker;

import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Section-based chunker for Word (DOCX) documents.
 *
 * <p>Creates hierarchical chunks based on document structure.
 * Stable IDs are derived from section titles so that adding a paragraph
 * in one section does not change the stable IDs of other sections
 * (no butterfly effect).
 *
 * <p>Expected input structure:
 * <pre>{@code
 * {
 *   "format": "docx",
 *   "doc_id": "<uuid>",
 *   "sections": [
 *     {
 *       "level": 1,
 *       "title": "Introduction",
 *       "paragraphs": [{"style": "Normal", "text": "..."}],
 *       "tables": [{"rows": [[...], ...]}]
 *     }
 *   ]
 * }
 * }</pre>
 */
@Component
public class DocxChunker extends BaseChunker {

    public DocxChunker() {
        super(800);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> chunk(Map<String, Object> parsedStructure) {
        List<Map<String, Object>> chunks = new ArrayList<>();
        List<Map<String, Object>> sections = (List<Map<String, Object>>) parsedStructure.getOrDefault("sections", List.of());

        String docId = resolveDocId(parsedStructure, sections);

        Set<String> seenPaths = new HashSet<>();
        Map<String, Integer> sectionCounter = new HashMap<>();

        for (Map<String, Object> section : sections) {
            int level = toInt(section.get("level"), 0);
            String title = toString(section.get("title")).strip();
            if (title.isEmpty()) continue;

            String normalized = normalizeHeading(title);
            String sectionPath;
            if (sectionCounter.containsKey(normalized)) {
                int count = sectionCounter.get(normalized) + 1;
                sectionCounter.put(normalized, count);
                sectionPath = docId + "/" + normalized + "_" + count;
            } else {
                sectionCounter.put(normalized, 0);
                sectionPath = docId + "/" + normalized;
            }

            // Ensure uniqueness
            if (seenPaths.contains(sectionPath)) {
                int counter = 1;
                while (seenPaths.contains(sectionPath + "_" + counter)) counter++;
                sectionPath = sectionPath + "_" + counter;
            }
            seenPaths.add(sectionPath);

            // Tables within this section
            List<Map<String, Object>> tables = (List<Map<String, Object>>) section.getOrDefault("tables", List.of());
            for (Map<String, Object> table : tables) {
                Map<String, Object> tableChunk = createTableChunk(table, sectionPath, docId);
                if (tableChunk != null) chunks.add(tableChunk);
            }

            // Build section text
            List<String> contentParts = new ArrayList<>();
            contentParts.add(title);
            List<Map<String, Object>> paragraphs = (List<Map<String, Object>>) section.getOrDefault("paragraphs", List.of());
            for (Map<String, Object> para : paragraphs) {
                if (toString(para.get("style")).contains("Heading")) continue;
                String paraText = toString(para.get("text")).strip();
                if (!paraText.isEmpty()) contentParts.add(paraText);
            }

            if (contentParts.size() <= 1) continue; // only title, no real content

            String fullText = String.join("\n\n", contentParts);
            if (fullText.length() > maxChunkSize) {
                chunks.addAll(splitLargeSection(title, paragraphs, sectionPath, level));
            } else {
                Map<String, Object> meta = new HashMap<>();
                meta.put("heading", title);
                meta.put("level", level);
                chunks.add(createChunk(sectionPath, fullText, "section", meta));
            }
        }
        return chunks;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> splitLargeSection(
            String title, List<Map<String, Object>> paragraphs, String path, int level) {

        List<Map<String, Object>> result = new ArrayList<>();
        if (paragraphs.isEmpty()) {
            Map<String, Object> meta = new HashMap<>();
            meta.put("heading", title);
            meta.put("level", level);
            meta.put("is_oversized", true);
            result.add(createChunk(path, title, "section", meta));
            return result;
        }

        List<String> currentParts = new ArrayList<>();
        currentParts.add(title);
        int currentSize = title.length();
        int partIndex = 0;

        for (Map<String, Object> para : paragraphs) {
            if (toString(para.get("style")).contains("Heading")) continue;
            String paraText = toString(para.get("text")).strip();
            if (paraText.isEmpty()) continue;

            if (currentSize + paraText.length() + 2 > maxChunkSize && currentParts.size() > 1) {
                String chunkText = String.join("\n\n", currentParts);
                Map<String, Object> meta = new HashMap<>();
                meta.put("heading", title);
                meta.put("level", level);
                meta.put("parent_section", path);
                meta.put("part_index", partIndex);
                meta.put("is_partial", true);
                result.add(createChunk(path + "_part" + partIndex, chunkText, "section_part", meta));
                currentParts = new ArrayList<>(List.of(title, paraText));
                currentSize = title.length() + paraText.length() + 2;
                partIndex++;
            } else {
                currentParts.add(paraText);
                currentSize += paraText.length() + 2;
            }
        }

        if (!currentParts.isEmpty()) {
            String chunkText = String.join("\n\n", currentParts);
            String stableId = partIndex > 0 ? path + "_part" + partIndex : path;
            String chunkType = partIndex > 0 ? "section_part" : "section";
            Map<String, Object> meta = new HashMap<>();
            meta.put("heading", title);
            meta.put("level", level);
            if (partIndex > 0) {
                meta.put("parent_section", path);
                meta.put("part_index", partIndex);
                meta.put("is_partial", true);
            }
            result.add(createChunk(stableId, chunkText, chunkType, meta));
        }
        return result;
    }

    private Map<String, Object> createTableChunk(Map<String, Object> table, String parentSection, String docId) {
        String tableText = formatTable(table);
        if (tableText.isEmpty()) return null;

        String tableHash = md5(tableText).substring(0, 12);
        String stableId = parentSection != null
                ? parentSection + "/table_" + tableHash
                : docId + "/table_" + tableHash;

        @SuppressWarnings("unchecked")
        List<Object> rows = (List<Object>) table.getOrDefault("rows", List.of());
        int cols = rows.isEmpty() ? 0 : (rows.get(0) instanceof List<?> l ? l.size() : 1);

        Map<String, Object> meta = new HashMap<>();
        meta.put("rows", rows.size());
        meta.put("columns", cols);
        meta.put("is_table", true);
        meta.put("parent_section", parentSection);
        return createChunk(stableId, tableText, "table", meta);
    }

    @SuppressWarnings("unchecked")
    private String resolveDocId(Map<String, Object> structure, List<Map<String, Object>> sections) {
        Object docId = structure.get("doc_id");
        if (docId != null && !docId.toString().isBlank()) return docId.toString();
        return md5(sections.toString()).substring(0, 8);
    }

    private String toString(Object o) {
        return o == null ? "" : o.toString();
    }

    private int toInt(Object o, int defaultVal) {
        if (o instanceof Number n) return n.intValue();
        return defaultVal;
    }
}
