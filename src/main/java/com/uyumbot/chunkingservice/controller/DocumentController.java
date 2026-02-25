package com.uyumbot.chunkingservice.controller;

import com.uyumbot.chunkingservice.dto.ChunkSummary;
import com.uyumbot.chunkingservice.dto.DocumentRequest;
import com.uyumbot.chunkingservice.dto.DocumentResponse;
import com.uyumbot.chunkingservice.service.DocumentService;
import com.uyumbot.chunkingservice.service.FileParsingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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
 * POST   /api/documents/upload       - upload file, parse and auto-chunk
 * </pre>
 */
@Tag(name = "Documents", description = "Document CRUD and chunking operations")
@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private final DocumentService documentService;
    private final FileParsingService fileParsingService;

    public DocumentController(DocumentService documentService, FileParsingService fileParsingService) {
        this.documentService = documentService;
        this.fileParsingService = fileParsingService;
    }

    @Operation(summary = "List all documents")
    @GetMapping
    public ResponseEntity<List<DocumentResponse>> listDocuments() {
        return ResponseEntity.ok(documentService.listDocuments());
    }

    @Operation(summary = "Create a new document", description = "Creates a document and optionally auto-chunks its content")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Document created"),
            @ApiResponse(responseCode = "400", description = "Invalid request body")
    })
    @PostMapping
    public ResponseEntity<DocumentResponse> createDocument(@Valid @RequestBody DocumentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(documentService.createDocument(request));
    }

    @Operation(summary = "Get a document by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Document found"),
            @ApiResponse(responseCode = "404", description = "Document not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<DocumentResponse> getDocument(
            @Parameter(description = "Document UUID") @PathVariable UUID id) {
        return ResponseEntity.ok(documentService.getDocument(id));
    }

    @Operation(summary = "Update a document", description = "Updates document metadata and optionally re-chunks its content")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Document updated"),
            @ApiResponse(responseCode = "404", description = "Document not found"),
            @ApiResponse(responseCode = "400", description = "Invalid request body")
    })
    @PutMapping("/{id}")
    public ResponseEntity<DocumentResponse> updateDocument(
            @Parameter(description = "Document UUID") @PathVariable UUID id,
            @Valid @RequestBody DocumentRequest request) {
        return ResponseEntity.ok(documentService.updateDocument(id, request));
    }

    @Operation(summary = "Delete a document and its chunks")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Document deleted"),
            @ApiResponse(responseCode = "404", description = "Document not found")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDocument(
            @Parameter(description = "Document UUID") @PathVariable UUID id) {
        documentService.deleteDocument(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Trigger chunking for a document",
            description = "Triggers (re-)chunking for a document using the provided format-specific parsed structure map")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Chunking completed"),
            @ApiResponse(responseCode = "404", description = "Document not found")
    })
    @PostMapping("/{id}/chunk")
    public ResponseEntity<ChunkSummary> chunkDocument(
            @Parameter(description = "Document UUID") @PathVariable UUID id,
            @RequestBody Map<String, Object> structure) {
        return ResponseEntity.ok(documentService.chunkDocument(id, structure));
    }

    @Operation(summary = "Upload and parse a document file",
            description = "Accepts a multipart file (docx, pdf, pptx, xlsx), parses it and auto-chunks its content")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Document uploaded, parsed and chunked"),
            @ApiResponse(responseCode = "400", description = "Unsupported file type or parse error")
    })
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DocumentResponse> uploadDocument(
            @Parameter(description = "File to upload (docx, pdf, pptx, xlsx)")
            @RequestParam("file") MultipartFile file) {
        String format = fileParsingService.detectFormat(file);
        Map<String, Object> structure = fileParsingService.parse(file);
        DocumentRequest request = DocumentRequest.builder()
                .filename(file.getOriginalFilename())
                .format(format)
                .structure(structure)
                .build();
        return ResponseEntity.status(HttpStatus.CREATED).body(documentService.createDocument(request));
    }
}
