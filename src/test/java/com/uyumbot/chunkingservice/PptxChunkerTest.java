package com.uyumbot.chunkingservice.service.chunker;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class PptxChunkerTest {

    private final PptxChunker chunker = new PptxChunker();

    @Test
    void chunkCreatesSlideChunks() {
        Map<String, Object> structure = Map.of(
                "format", "pptx",
                "doc_id", "pptx-doc",
                "slides", List.of(
                        Map.of(
                                "slide_number", 1,
                                "title", "Overview",
                                "content", List.of(Map.of("type", "text", "text", "Bullet point")),
                                "tables", List.of(),
                                "notes", "Speaker notes here"
                        )
                )
        );

        List<Map<String, Object>> chunks = chunker.chunk(structure);

        assertThat(chunks).hasSize(1);
        assertThat(chunks.get(0).get("stable_id")).isEqualTo("pptx-doc/slide_1");
        assertThat(chunks.get(0).get("type")).isEqualTo("slide");
        assertThat(chunks.get(0).get("text").toString()).contains("Overview");
        assertThat(chunks.get(0).get("text").toString()).contains("Speaker notes here");
    }
}
