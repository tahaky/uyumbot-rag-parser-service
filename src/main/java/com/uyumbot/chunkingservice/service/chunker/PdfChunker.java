package com.uyumbot.chunkingservice.service.chunker;

import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Page-based chunker for PDF documents.
 *
 * <p>Each page becomes one chunk. Stable IDs take the form {@code <docId>/page_<n>}.
 *
 * <p>Expected input structure:
 * <pre>{@code
 * {
 *   "format": "pdf",
 *   "doc_id": "<uuid>",
 *   "pages": [
 *     {
 *       "page_number": 1,
 *       "text": "...",
 *       "tables": [{"rows": [[...]]}]
 *     }
 *   ]
 * }
 * }</pre>
 */
@Component
public class PdfChunker extends BaseChunker {

    public PdfChunker() {
        super(800);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> chunk(Map<String, Object> parsedStructure) {
        List<Map<String, Object>> chunks = new ArrayList<>();
        List<Map<String, Object>> pages = (List<Map<String, Object>>) parsedStructure.getOrDefault("pages", List.of());

        String docId = resolveDocId(parsedStructure, pages);

        for (Map<String, Object> page : pages) {
            int pageNumber = toInt(page.get("page_number"), 0);

            List<String> textParts = new ArrayList<>();
            String pageText = toString(page.get("text")).strip();
            if (!pageText.isEmpty()) textParts.add(pageText);

            List<Map<String, Object>> tables = (List<Map<String, Object>>) page.getOrDefault("tables", List.of());
            for (Map<String, Object> table : tables) {
                String tableText = formatTable(table);
                if (!tableText.isEmpty()) textParts.add(tableText);
            }

            if (textParts.isEmpty()) continue;

            String fullText = String.join("\n\n", textParts);
            String stableId = docId + "/page_" + pageNumber;

            Map<String, Object> meta = new HashMap<>();
            meta.put("page_number", pageNumber);
            meta.put("has_tables", !tables.isEmpty());
            meta.put("char_count", fullText.length());

            chunks.add(createChunk(stableId, fullText, "page", meta));
        }
        return chunks;
    }

    private String resolveDocId(Map<String, Object> structure, List<Map<String, Object>> pages) {
        Object docId = structure.get("doc_id");
        if (docId != null && !docId.toString().isBlank()) return docId.toString();
        return md5(pages.toString()).substring(0, 8);
    }

    private String toString(Object o) {
        return o == null ? "" : o.toString();
    }

    private int toInt(Object o, int defaultVal) {
        if (o instanceof Number n) return n.intValue();
        return defaultVal;
    }
}
