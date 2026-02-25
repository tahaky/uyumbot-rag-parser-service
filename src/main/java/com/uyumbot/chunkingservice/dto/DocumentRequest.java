package com.uyumbot.chunkingservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Request body for creating or updating a document.
 * The {@code structure} field contains format-specific parsed content
 * that will be chunked and stored.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentRequest {

    @NotBlank(message = "filename is required")
    private String filename;

    @NotBlank(message = "format is required")
    @Pattern(regexp = "docx|pdf|pptx|xlsx", message = "format must be one of: docx, pdf, pptx, xlsx")
    private String format;

    /**
     * Parsed document structure.
     * When provided, chunking is performed automatically on create/update.
     *
     * DOCX: {"doc_id": "...", "sections": [...]}
     * PDF:  {"doc_id": "...", "pages": [...]}
     * PPTX: {"doc_id": "...", "slides": [...]}
     * XLSX: {"sheets": [...]}
     */
    private Map<String, Object> structure;
}
