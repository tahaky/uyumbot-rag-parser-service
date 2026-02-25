package com.uyumbot.chunkingservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(
    name = "chunks",
    indexes = {
        @Index(name = "idx_chunks_document_id", columnList = "document_id"),
        @Index(name = "idx_chunks_stable_id", columnList = "stable_id"),
        @Index(name = "idx_chunks_hash", columnList = "hash"),
        @Index(name = "idx_chunks_type", columnList = "chunk_type")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_chunks_doc_stable", columnNames = {"document_id", "stable_id"})
    }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Chunk {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "chunk_id", updatable = false, nullable = false)
    private UUID chunkId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false)
    private Document document;

    @Column(name = "stable_id", length = 255, nullable = false)
    private String stableId;

    @Column(name = "text", columnDefinition = "TEXT", nullable = false)
    private String text;

    @Column(name = "chunk_type", length = 50, nullable = false)
    private String chunkType;

    @Column(name = "hash", length = 64, nullable = false)
    private String hash;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata")
    private Map<String, Object> metadata;

    @Column(name = "word_count")
    private Integer wordCount;

    @Column(name = "char_count")
    private Integer charCount;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
