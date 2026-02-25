package com.uyumbot.chunkingservice.controller;

import com.uyumbot.chunkingservice.dto.ChunkRequest;
import com.uyumbot.chunkingservice.dto.ChunkResponse;
import com.uyumbot.chunkingservice.service.ChunkService;
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
@RestController
public class ChunkController {

    private final ChunkService chunkService;

    public ChunkController(ChunkService chunkService) {
        this.chunkService = chunkService;
    }

    @GetMapping("/api/documents/{docId}/chunks")
    public ResponseEntity<List<ChunkResponse>> listChunks(@PathVariable UUID docId) {
        return ResponseEntity.ok(chunkService.listChunks(docId));
    }

    @PostMapping("/api/documents/{docId}/chunks")
    public ResponseEntity<ChunkResponse> createChunk(
            @PathVariable UUID docId,
            @Valid @RequestBody ChunkRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(chunkService.createChunk(docId, request));
    }

    @DeleteMapping("/api/documents/{docId}/chunks")
    public ResponseEntity<Map<String, Integer>> deleteChunksByDocument(@PathVariable UUID docId) {
        int deleted = chunkService.deleteChunksByDocument(docId);
        return ResponseEntity.ok(Map.of("deleted", deleted));
    }

    @GetMapping("/api/chunks/{id}")
    public ResponseEntity<ChunkResponse> getChunk(@PathVariable UUID id) {
        return ResponseEntity.ok(chunkService.getChunk(id));
    }

    @PutMapping("/api/chunks/{id}")
    public ResponseEntity<ChunkResponse> updateChunk(
            @PathVariable UUID id,
            @Valid @RequestBody ChunkRequest request) {
        return ResponseEntity.ok(chunkService.updateChunk(id, request));
    }

    @DeleteMapping("/api/chunks/{id}")
    public ResponseEntity<Void> deleteChunk(@PathVariable UUID id) {
        chunkService.deleteChunk(id);
        return ResponseEntity.noContent().build();
    }
}
