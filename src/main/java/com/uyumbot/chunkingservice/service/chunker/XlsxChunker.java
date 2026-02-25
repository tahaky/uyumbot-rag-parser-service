package com.uyumbot.chunkingservice.service.chunker;

import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Sheet-based chunker for Excel (XLSX) files.
 *
 * <p>Each sheet becomes one chunk. Stable IDs take the form {@code sheet_<normalized_name>}.
 *
 * <p>Expected input structure:
 * <pre>{@code
 * {
 *   "format": "xlsx",
 *   "sheets": [
 *     {
 *       "name": "Sales Q4",
 *       "data": [[...], [...]],
 *       "formulas": [{"cell": "A1", "formula": "=SUM(...)"}],
 *       "charts": [...]
 *     }
 *   ]
 * }
 * }</pre>
 */
@Component
public class XlsxChunker extends BaseChunker {

    public XlsxChunker() {
        super(800);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> chunk(Map<String, Object> parsedStructure) {
        List<Map<String, Object>> chunks = new ArrayList<>();
        List<Map<String, Object>> sheets = (List<Map<String, Object>>) parsedStructure.getOrDefault("sheets", List.of());

        for (Map<String, Object> sheet : sheets) {
            String sheetName = toString(sheet.get("name"), "untitled");

            List<String> textParts = new ArrayList<>();
            textParts.add("Sheet: " + sheetName + "\n");

            List<List<Object>> data = (List<List<Object>>) sheet.getOrDefault("data", List.of());
            for (List<Object> row : data) {
                String rowText = row.stream().map(Object::toString).collect(Collectors.joining("\t"));
                textParts.add(rowText);
            }

            List<Map<String, Object>> formulas = (List<Map<String, Object>>) sheet.getOrDefault("formulas", List.of());
            if (!formulas.isEmpty()) {
                textParts.add("\nFormulas:");
                for (Map<String, Object> formula : formulas) {
                    textParts.add(toString(formula.get("cell"), "") + ": " + toString(formula.get("formula"), ""));
                }
            }

            String sheetText = String.join("\n", textParts);
            String normalizedName = normalizeSheetName(sheetName);
            String stableId = "sheet_" + normalizedName;

            Map<String, Object> meta = new HashMap<>();
            meta.put("sheet_name", sheetName);
            meta.put("rows", data.size());
            meta.put("has_formulas", !formulas.isEmpty());
            meta.put("has_charts", !((List<?>) sheet.getOrDefault("charts", List.of())).isEmpty());

            chunks.add(createChunk(stableId, sheetText, "sheet", meta));
        }
        return chunks;
    }

    private String normalizeSheetName(String name) {
        if (name == null) return "untitled";
        String normalized = name.trim().toLowerCase()
                .replace(" ", "_")
                .replaceAll("[^\\w_]", "")
                .replaceAll("_+", "_")
                .replaceAll("^_|_$", "");
        if (normalized.length() > 100) normalized = normalized.substring(0, 100).replaceAll("_$", "");
        return normalized.isEmpty() ? "untitled" : normalized;
    }

    private String toString(Object o, String defaultVal) {
        return o == null ? defaultVal : o.toString();
    }
}
