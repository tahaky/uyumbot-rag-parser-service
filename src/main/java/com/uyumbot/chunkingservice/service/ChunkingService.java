package com.uyumbot.chunkingservice.service;

import com.uyumbot.chunkingservice.dto.ChunkResponse;
import com.uyumbot.chunkingservice.dto.ChunkSummary;
import com.uyumbot.chunkingservice.entity.Chunk;
import com.uyumbot.chunkingservice.entity.Document;
import com.uyumbot.chunkingservice.exception.ChunkingException;
import com.uyumbot.chunkingservice.exception.ResourceNotFoundException;
import com.uyumbot.chunkingservice.repository.ChunkRepository;
import com.uyumbot.chunkingservice.repository.DocumentRepository;
import com.uyumbot.chunkingservice.service.chunker.BaseChunker;
import com.uyumbot.chunkingservice.service.chunker.ChunkerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Orchestrates the chunking process: runs the appropriate chunker, then
 * upserts the resulting chunks into the database (insert new, update changed,
 * delete removed).
 */
@Service
public class ChunkingService {

    private final ChunkerFactory chunkerFactory;
    private final DocumentRepository documentRepository;
    private final ChunkRepository chunkRepository;

    public ChunkingService(ChunkerFactory chunkerFactory,
                           DocumentRepository documentRepository,
                           ChunkRepository chunkRepository) {
        this.chunkerFactory = chunkerFactory;
        this.documentRepository = documentRepository;
        this.chunkRepository = chunkRepository;
    }

    /**
     * Chunk the parsed structure of a document and persist the results.
     *
     * @param documentId  ID of the already-created document
     * @param structure   format-specific parsed document structure
     * @return summary with all resulting chunks
     */
    @Transactional
    public ChunkSummary chunkDocument(UUID documentId, Map<String, Object> structure) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document", documentId));

        if (structure == null || structure.isEmpty()) {
            throw new ChunkingException("Document structure must not be empty");
        }

        // Run format-specific chunker
        BaseChunker chunker = chunkerFactory.getChunker(document.getFormat());
        List<Map<String, Object>> rawChunks;
        try {
            rawChunks = chunker.chunk(structure);
        } catch (ChunkingException ce) {
            throw ce;
        } catch (Exception e) {
            throw new ChunkingException("Chunking failed for document " + documentId + ": " + e.getMessage(), e);
        }

        // Upsert chunks
        List<Chunk> savedChunks = upsertChunks(document, rawChunks);

        // Update document status
        document.setStatus("chunked");
        documentRepository.save(document);

        return buildSummary(document.getId(), savedChunks);
    }

    @SuppressWarnings("unchecked")
    private List<Chunk> upsertChunks(Document document, List<Map<String, Object>> rawChunks) {
        // Fetch existing chunks for this document, keyed by stableId
        List<Chunk> existingChunks = chunkRepository.findByDocumentId(document.getId());
        Map<String, Chunk> existingByStableId = existingChunks.stream()
                .collect(Collectors.toMap(Chunk::getStableId, c -> c));

        Set<String> newStableIds = new HashSet<>();
        List<Chunk> result = new ArrayList<>();

        for (Map<String, Object> raw : rawChunks) {
            String stableId = (String) raw.get("stable_id");
            String text     = (String) raw.get("text");
            String type     = (String) raw.get("type");
            String hash     = (String) raw.get("hash");
            Map<String, Object> meta = (Map<String, Object>) raw.getOrDefault("metadata", Map.of());
            int wordCount   = toInt(raw.get("word_count"));
            int charCount   = toInt(raw.get("char_count"));

            newStableIds.add(stableId);
            Chunk existing = existingByStableId.get(stableId);

            if (existing != null) {
                if (!existing.getHash().equals(hash)) {
                    existing.setText(text);
                    existing.setHash(hash);
                    existing.setMetadata(meta);
                    existing.setWordCount(wordCount);
                    existing.setCharCount(charCount);
                    result.add(chunkRepository.save(existing));
                } else {
                    result.add(existing);
                }
            } else {
                Chunk newChunk = Chunk.builder()
                        .document(document)
                        .stableId(stableId)
                        .text(text)
                        .chunkType(type)
                        .hash(hash)
                        .metadata(meta)
                        .wordCount(wordCount)
                        .charCount(charCount)
                        .build();
                result.add(chunkRepository.save(newChunk));
            }
        }

        // Delete removed chunks
        for (Chunk old : existingChunks) {
            if (!newStableIds.contains(old.getStableId())) {
                chunkRepository.delete(old);
            }
        }

        return result;
    }

    private ChunkSummary buildSummary(UUID documentId, List<Chunk> chunks) {
        List<ChunkResponse> responses = chunks.stream().map(this::toResponse).collect(Collectors.toList());
        long sections   = chunks.stream().filter(c -> c.getChunkType().startsWith("section")).count();
        long pages      = chunks.stream().filter(c -> "page".equals(c.getChunkType())).count();
        long slides     = chunks.stream().filter(c -> "slide".equals(c.getChunkType())).count();
        long sheets     = chunks.stream().filter(c -> "sheet".equals(c.getChunkType())).count();
        long tables     = chunks.stream().filter(c -> "table".equals(c.getChunkType())).count();
        long totalWords = chunks.stream().mapToLong(c -> c.getWordCount() == null ? 0 : c.getWordCount()).sum();
        long totalChars = chunks.stream().mapToLong(c -> c.getCharCount() == null ? 0 : c.getCharCount()).sum();

        return ChunkSummary.builder()
                .documentId(documentId)
                .totalChunks(chunks.size())
                .sectionChunks(sections)
                .pageChunks(pages)
                .slideChunks(slides)
                .sheetChunks(sheets)
                .tableChunks(tables)
                .totalWords(totalWords)
                .totalCharacters(totalChars)
                .chunks(responses)
                .build();
    }

    public ChunkResponse toResponse(Chunk chunk) {
        return ChunkResponse.builder()
                .chunkId(chunk.getChunkId())
                .documentId(chunk.getDocument().getId())
                .stableId(chunk.getStableId())
                .text(chunk.getText())
                .chunkType(chunk.getChunkType())
                .hash(chunk.getHash())
                .metadata(chunk.getMetadata())
                .wordCount(chunk.getWordCount())
                .charCount(chunk.getCharCount())
                .createdAt(chunk.getCreatedAt())
                .updatedAt(chunk.getUpdatedAt())
                .build();
    }

    private int toInt(Object o) {
        if (o instanceof Number n) return n.intValue();
        return 0;
    }
}
