package com.uyumbot.chunkingservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentResponse {

    private UUID id;
    private String filename;
    private String format;
    private String status;
    private long chunkCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
