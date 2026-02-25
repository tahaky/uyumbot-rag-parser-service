package com.uyumbot.chunkingservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChunkSummary {

    private UUID documentId;
    private int totalChunks;
    private long sectionChunks;
    private long pageChunks;
    private long slideChunks;
    private long sheetChunks;
    private long tableChunks;
    private long totalWords;
    private long totalCharacters;
    private List<ChunkResponse> chunks;
}
