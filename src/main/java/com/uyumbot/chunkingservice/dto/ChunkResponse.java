package com.uyumbot.chunkingservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChunkResponse {

    private UUID chunkId;
    private UUID documentId;
    private String stableId;
    private String text;
    private String chunkType;
    private String hash;
    private Map<String, Object> metadata;
    private Integer wordCount;
    private Integer charCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
