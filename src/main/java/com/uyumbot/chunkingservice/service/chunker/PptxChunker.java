package com.uyumbot.chunkingservice.service.chunker;

import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Slide-based chunker for PowerPoint (PPTX) presentations.
 *
 * <p>Each slide becomes one chunk. Stable IDs take the form {@code <docId>/slide_<n>}.
 *
 * <p>Expected input structure:
 * <pre>{@code
 * {
 *   "format": "pptx",
 *   "doc_id": "<uuid>",
 *   "slides": [
 *     {
 *       "slide_number": 1,
 *       "title": "...",
 *       "content": [{"type": "text", "text": "..."}],
 *       "tables": [{"rows": [[...]]}],
 *       "notes": "..."
 *     }
 *   ]
 * }
 * }</pre>
 */
@Component
public class PptxChunker extends BaseChunker {

    public PptxChunker() {
        super(800);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> chunk(Map<String, Object> parsedStructure) {
        List<Map<String, Object>> chunks = new ArrayList<>();
        List<Map<String, Object>> slides = (List<Map<String, Object>>) parsedStructure.getOrDefault("slides", List.of());

        String docId = resolveDocId(parsedStructure, slides);

        for (Map<String, Object> slide : slides) {
            int slideNumber = toInt(slide.get("slide_number"), 0);

            List<String> textParts = new ArrayList<>();

            String title = toString(slide.get("title")).strip();
            if (!title.isEmpty()) textParts.add("# " + title);

            List<Map<String, Object>> content = (List<Map<String, Object>>) slide.getOrDefault("content", List.of());
            for (Map<String, Object> item : content) {
                if ("text".equals(item.get("type"))) {
                    String text = toString(item.get("text")).strip();
                    if (!text.isEmpty()) textParts.add(text);
                }
            }

            List<Map<String, Object>> tables = (List<Map<String, Object>>) slide.getOrDefault("tables", List.of());
            for (Map<String, Object> table : tables) {
                String tableText = formatTable(table);
                if (!tableText.isEmpty()) textParts.add(tableText);
            }

            String notes = toString(slide.get("notes")).strip();
            if (!notes.isEmpty()) textParts.add("\n[Notes: " + notes + "]");

            if (textParts.isEmpty()) continue;

            String slideText = String.join("\n\n", textParts);
            String stableId = docId + "/slide_" + slideNumber;

            Map<String, Object> meta = new HashMap<>();
            meta.put("slide_number", slideNumber);
            meta.put("title", title.isEmpty() ? null : title);
            meta.put("has_notes", !notes.isEmpty());
            meta.put("has_tables", !tables.isEmpty());
            meta.put("content_items", content.size());

            chunks.add(createChunk(stableId, slideText, "slide", meta));
        }
        return chunks;
    }

    private String resolveDocId(Map<String, Object> structure, List<Map<String, Object>> slides) {
        Object docId = structure.get("doc_id");
        if (docId != null && !docId.toString().isBlank()) return docId.toString();
        return md5(slides.toString()).substring(0, 8);
    }

    private String toString(Object o) {
        return o == null ? "" : o.toString();
    }

    private int toInt(Object o, int defaultVal) {
        if (o instanceof Number n) return n.intValue();
        return defaultVal;
    }
}
