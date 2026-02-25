package com.uyumbot.chunkingservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChunkRequest {

    @NotBlank(message = "stableId is required")
    private String stableId;

    @NotBlank(message = "text is required")
    private String text;

    @NotBlank(message = "chunkType is required")
    private String chunkType;

    private Map<String, Object> metadata;
}
