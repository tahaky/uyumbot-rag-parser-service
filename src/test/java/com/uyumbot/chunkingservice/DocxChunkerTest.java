package com.uyumbot.chunkingservice.service.chunker;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DocxChunkerTest {

    private final DocxChunker chunker = new DocxChunker();

    @Test
    void chunkReturnsExpectedSectionChunk() {
        Map<String, Object> structure = Map.of(
                "format", "docx",
                "doc_id", "test-doc",
                "sections", List.of(
                        Map.of(
                                "level", 1,
                                "title", "Introduction",
                                "paragraphs", List.of(
                                        Map.of("style", "Normal", "text", "This is the introduction text.")
                                ),
                                "tables", List.of()
                        )
                )
        );

        List<Map<String, Object>> chunks = chunker.chunk(structure);

        assertThat(chunks).hasSize(1);
        Map<String, Object> chunk = chunks.get(0);
        assertThat(chunk.get("stable_id")).isEqualTo("test-doc/introduction");
        assertThat(chunk.get("type")).isEqualTo("section");
        assertThat(chunk.get("text").toString()).contains("Introduction");
        assertThat(chunk.get("text").toString()).contains("This is the introduction text.");
        assertThat(chunk.get("hash")).isNotNull();
        assertThat(chunk.get("word_count")).isNotNull();
    }

    @Test
    void chunkHandlesTableInSection() {
        Map<String, Object> structure = Map.of(
                "format", "docx",
                "doc_id", "doc1",
                "sections", List.of(
                        Map.of(
                                "level", 1,
                                "title", "Data",
                                "paragraphs", List.of(
                                        Map.of("style", "Normal", "text", "Some data.")
                                ),
                                "tables", List.of(
                                        Map.of("rows", List.of(
                                                List.of("Col1", "Col2"),
                                                List.of("A", "B")
                                        ))
                                )
                        )
                )
        );

        List<Map<String, Object>> chunks = chunker.chunk(structure);

        assertThat(chunks).hasSizeGreaterThanOrEqualTo(2);
        boolean hasTable = chunks.stream().anyMatch(c -> "table".equals(c.get("type")));
        assertThat(hasTable).isTrue();
    }

    @Test
    void chunkProducesUniqueStableIds() {
        Map<String, Object> structure = Map.of(
                "format", "docx",
                "doc_id", "doc2",
                "sections", List.of(
                        Map.of("level", 1, "title", "Section", "paragraphs",
                                List.of(Map.of("style", "Normal", "text", "First")), "tables", List.of()),
                        Map.of("level", 1, "title", "Section", "paragraphs",
                                List.of(Map.of("style", "Normal", "text", "Second")), "tables", List.of())
                )
        );

        List<Map<String, Object>> chunks = chunker.chunk(structure);
        List<String> stableIds = chunks.stream().map(c -> (String) c.get("stable_id")).toList();
        assertThat(stableIds).doesNotHaveDuplicates();
    }
}
