package com.uyumbot.chunkingservice.repository;

import com.uyumbot.chunkingservice.entity.Chunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ChunkRepository extends JpaRepository<Chunk, UUID> {

    List<Chunk> findByDocumentId(UUID documentId);

    Optional<Chunk> findByDocumentIdAndStableId(UUID documentId, String stableId);

    @Modifying
    @Query("DELETE FROM Chunk c WHERE c.document.id = :documentId")
    int deleteByDocumentId(UUID documentId);

    long countByDocumentId(UUID documentId);
}
