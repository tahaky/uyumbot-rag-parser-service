package com.uyumbot.chunkingservice.service;

import com.uyumbot.chunkingservice.dto.ChunkRequest;
import com.uyumbot.chunkingservice.dto.ChunkResponse;
import com.uyumbot.chunkingservice.entity.Chunk;
import com.uyumbot.chunkingservice.entity.Document;
import com.uyumbot.chunkingservice.exception.ResourceNotFoundException;
import com.uyumbot.chunkingservice.repository.ChunkRepository;
import com.uyumbot.chunkingservice.repository.DocumentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ChunkService {

    private final ChunkRepository chunkRepository;
    private final DocumentRepository documentRepository;
    private final ChunkingService chunkingService;

    public ChunkService(ChunkRepository chunkRepository,
                        DocumentRepository documentRepository,
                        ChunkingService chunkingService) {
        this.chunkRepository = chunkRepository;
        this.documentRepository = documentRepository;
        this.chunkingService = chunkingService;
    }

    /** List all chunks for a document. */
    public List<ChunkResponse> listChunks(UUID documentId) {
        if (!documentRepository.existsById(documentId)) {
            throw new ResourceNotFoundException("Document", documentId);
        }
        return chunkRepository.findByDocumentId(documentId).stream()
                .map(chunkingService::toResponse)
                .collect(Collectors.toList());
    }

    /** Get a single chunk by ID. */
    public ChunkResponse getChunk(UUID chunkId) {
        Chunk chunk = chunkRepository.findById(chunkId)
                .orElseThrow(() -> new ResourceNotFoundException("Chunk", chunkId));
        return chunkingService.toResponse(chunk);
    }

    /**
     * Manually create a chunk for a document.
     * The hash is computed automatically from the text.
     */
    @Transactional
    public ChunkResponse createChunk(UUID documentId, ChunkRequest request) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document", documentId));

        String hash = md5(request.getText());
        String[] words = request.getText().trim().split("\\s+");

        Chunk chunk = Chunk.builder()
                .document(document)
                .stableId(request.getStableId())
                .text(request.getText())
                .chunkType(request.getChunkType())
                .hash(hash)
                .metadata(request.getMetadata())
                .wordCount(words.length)
                .charCount(request.getText().length())
                .build();

        return chunkingService.toResponse(chunkRepository.save(chunk));
    }

    /** Update a chunk's text and/or metadata. */
    @Transactional
    public ChunkResponse updateChunk(UUID chunkId, ChunkRequest request) {
        Chunk chunk = chunkRepository.findById(chunkId)
                .orElseThrow(() -> new ResourceNotFoundException("Chunk", chunkId));

        chunk.setStableId(request.getStableId());
        chunk.setText(request.getText());
        chunk.setChunkType(request.getChunkType());
        chunk.setHash(md5(request.getText()));
        chunk.setMetadata(request.getMetadata());
        chunk.setWordCount(request.getText().trim().split("\\s+").length);
        chunk.setCharCount(request.getText().length());

        return chunkingService.toResponse(chunkRepository.save(chunk));
    }

    /** Delete a chunk by ID. */
    @Transactional
    public void deleteChunk(UUID chunkId) {
        if (!chunkRepository.existsById(chunkId)) {
            throw new ResourceNotFoundException("Chunk", chunkId);
        }
        chunkRepository.deleteById(chunkId);
    }

    /** Delete all chunks for a document. */
    @Transactional
    public int deleteChunksByDocument(UUID documentId) {
        if (!documentRepository.existsById(documentId)) {
            throw new ResourceNotFoundException("Document", documentId);
        }
        return chunkRepository.deleteByDocumentId(documentId);
    }

    private String md5(String text) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(text.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("MD5 not available", e);
        }
    }
}
