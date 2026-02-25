package com.uyumbot.chunkingservice.controller;

import com.uyumbot.chunkingservice.dto.ChunkRequest;
import com.uyumbot.chunkingservice.dto.ChunkResponse;
import com.uyumbot.chunkingservice.service.ChunkService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST controller for Chunk CRUD operations.
 *
 * <pre>
 * GET    /api/documents/{docId}/chunks           - list chunks for a document
 * POST   /api/documents/{docId}/chunks           - manually add a chunk
 * DELETE /api/documents/{docId}/chunks           - delete all chunks for document
 * GET    /api/chunks/{id}                        - get a single chunk
 * PUT    /api/chunks/{id}                        - update a chunk
 * DELETE /api/chunks/{id}                        - delete a chunk
 * </pre>
 */
@Tag(name = "Chunks", description = "Chunk CRUD operations")
@RestController
public class ChunkController {

    private final ChunkService chunkService;

    public ChunkController(ChunkService chunkService) {
        this.chunkService = chunkService;
    }

    @Operation(summary = "List chunks for a document")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Chunks retrieved"),
            @ApiResponse(responseCode = "404", description = "Document not found")
    })
    @GetMapping("/api/documents/{docId}/chunks")
    public ResponseEntity<List<ChunkResponse>> listChunks(
            @Parameter(description = "Document UUID") @PathVariable UUID docId) {
        return ResponseEntity.ok(chunkService.listChunks(docId));
    }

    @Operation(summary = "Manually add a chunk to a document")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Chunk created"),
            @ApiResponse(responseCode = "400", description = "Invalid request body"),
            @ApiResponse(responseCode = "404", description = "Document not found")
    })
    @PostMapping("/api/documents/{docId}/chunks")
    public ResponseEntity<ChunkResponse> createChunk(
            @Parameter(description = "Document UUID") @PathVariable UUID docId,
            @Valid @RequestBody ChunkRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(chunkService.createChunk(docId, request));
    }

    @Operation(summary = "Delete all chunks for a document")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Chunks deleted"),
            @ApiResponse(responseCode = "404", description = "Document not found")
    })
    @DeleteMapping("/api/documents/{docId}/chunks")
    public ResponseEntity<Map<String, Integer>> deleteChunksByDocument(
            @Parameter(description = "Document UUID") @PathVariable UUID docId) {
        int deleted = chunkService.deleteChunksByDocument(docId);
        return ResponseEntity.ok(Map.of("deleted", deleted));
    }

    @Operation(summary = "Get a single chunk by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Chunk found"),
            @ApiResponse(responseCode = "404", description = "Chunk not found")
    })
    @GetMapping("/api/chunks/{id}")
    public ResponseEntity<ChunkResponse> getChunk(
            @Parameter(description = "Chunk UUID") @PathVariable UUID id) {
        return ResponseEntity.ok(chunkService.getChunk(id));
    }

    @Operation(summary = "Update a chunk")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Chunk updated"),
            @ApiResponse(responseCode = "400", description = "Invalid request body"),
            @ApiResponse(responseCode = "404", description = "Chunk not found")
    })
    @PutMapping("/api/chunks/{id}")
    public ResponseEntity<ChunkResponse> updateChunk(
            @Parameter(description = "Chunk UUID") @PathVariable UUID id,
            @Valid @RequestBody ChunkRequest request) {
        return ResponseEntity.ok(chunkService.updateChunk(id, request));
    }

    @Operation(summary = "Delete a chunk by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Chunk deleted"),
            @ApiResponse(responseCode = "404", description = "Chunk not found")
    })
    @DeleteMapping("/api/chunks/{id}")
    public ResponseEntity<Void> deleteChunk(
            @Parameter(description = "Chunk UUID") @PathVariable UUID id) {
        chunkService.deleteChunk(id);
        return ResponseEntity.noContent().build();
    }
}
