package com.uyumbot.chunkingservice.controller;

import com.uyumbot.chunkingservice.dto.ChunkSummary;
import com.uyumbot.chunkingservice.dto.DocumentRequest;
import com.uyumbot.chunkingservice.dto.DocumentResponse;
import com.uyumbot.chunkingservice.service.DocumentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST controller for Document CRUD and chunking operations.
 *
 * <pre>
 * GET    /api/documents              - list all documents
 * POST   /api/documents              - create document (+ optional auto-chunk)
 * GET    /api/documents/{id}         - get document
 * PUT    /api/documents/{id}         - update document (+ optional re-chunk)
 * DELETE /api/documents/{id}         - delete document and its chunks
 * POST   /api/documents/{id}/chunk   - trigger chunking with provided structure
 * </pre>
 */
@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @GetMapping
    public ResponseEntity<List<DocumentResponse>> listDocuments() {
        return ResponseEntity.ok(documentService.listDocuments());
    }

    @PostMapping
    public ResponseEntity<DocumentResponse> createDocument(@Valid @RequestBody DocumentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(documentService.createDocument(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<DocumentResponse> getDocument(@PathVariable UUID id) {
        return ResponseEntity.ok(documentService.getDocument(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<DocumentResponse> updateDocument(
            @PathVariable UUID id,
            @Valid @RequestBody DocumentRequest request) {
        return ResponseEntity.ok(documentService.updateDocument(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDocument(@PathVariable UUID id) {
        documentService.deleteDocument(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Trigger (re-)chunking for a document.
     * The request body is a format-specific parsed structure map.
     */
    @PostMapping("/{id}/chunk")
    public ResponseEntity<ChunkSummary> chunkDocument(
            @PathVariable UUID id,
            @RequestBody Map<String, Object> structure) {
        return ResponseEntity.ok(documentService.chunkDocument(id, structure));
    }
}
