package com.uyumbot.chunkingservice.service;

import com.uyumbot.chunkingservice.dto.ChunkSummary;
import com.uyumbot.chunkingservice.dto.DocumentRequest;
import com.uyumbot.chunkingservice.dto.DocumentResponse;
import com.uyumbot.chunkingservice.entity.Document;
import com.uyumbot.chunkingservice.exception.ResourceNotFoundException;
import com.uyumbot.chunkingservice.repository.ChunkRepository;
import com.uyumbot.chunkingservice.repository.DocumentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final ChunkRepository chunkRepository;
    private final ChunkingService chunkingService;

    public DocumentService(DocumentRepository documentRepository,
                           ChunkRepository chunkRepository,
                           ChunkingService chunkingService) {
        this.documentRepository = documentRepository;
        this.chunkRepository = chunkRepository;
        this.chunkingService = chunkingService;
    }

    /** List all documents. */
    public List<DocumentResponse> listDocuments() {
        return documentRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /** Get a single document by ID. */
    public DocumentResponse getDocument(UUID id) {
        Document doc = documentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Document", id));
        return toResponse(doc);
    }

    /**
     * Create a new document.
     * If {@code request.structure} is provided, chunking is performed immediately.
     */
    @Transactional
    public DocumentResponse createDocument(DocumentRequest request) {
        Document document = Document.builder()
                .filename(request.getFilename())
                .format(request.getFormat())
                .status(request.getStructure() != null ? "chunking" : "pending")
                .build();
        document = documentRepository.save(document);

        if (request.getStructure() != null && !request.getStructure().isEmpty()) {
            chunkingService.chunkDocument(document.getId(), request.getStructure());
            // Reload to pick up updated status
            document = documentRepository.findById(document.getId()).orElse(document);
        }

        return toResponse(document);
    }

    /**
     * Update a document's metadata (filename, format).
     * If {@code request.structure} is provided, re-chunking is performed.
     */
    @Transactional
    public DocumentResponse updateDocument(UUID id, DocumentRequest request) {
        Document document = documentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Document", id));

        document.setFilename(request.getFilename());
        document.setFormat(request.getFormat());
        document = documentRepository.save(document);

        if (request.getStructure() != null && !request.getStructure().isEmpty()) {
            chunkingService.chunkDocument(document.getId(), request.getStructure());
            document = documentRepository.findById(document.getId()).orElse(document);
        }

        return toResponse(document);
    }

    /** Delete a document and all its chunks (cascade). */
    @Transactional
    public void deleteDocument(UUID id) {
        if (!documentRepository.existsById(id)) {
            throw new ResourceNotFoundException("Document", id);
        }
        documentRepository.deleteById(id);
    }

    /** Trigger (re-)chunking for a document given a parsed structure. */
    @Transactional
    public ChunkSummary chunkDocument(UUID id, java.util.Map<String, Object> structure) {
        // Ensure document exists
        if (!documentRepository.existsById(id)) {
            throw new ResourceNotFoundException("Document", id);
        }
        return chunkingService.chunkDocument(id, structure);
    }

    private DocumentResponse toResponse(Document doc) {
        long chunkCount = chunkRepository.countByDocumentId(doc.getId());
        return DocumentResponse.builder()
                .id(doc.getId())
                .filename(doc.getFilename())
                .format(doc.getFormat())
                .status(doc.getStatus())
                .chunkCount(chunkCount)
                .createdAt(doc.getCreatedAt())
                .updatedAt(doc.getUpdatedAt())
                .build();
    }
}
