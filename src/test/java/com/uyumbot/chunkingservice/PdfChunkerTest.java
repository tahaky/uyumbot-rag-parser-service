package com.uyumbot.chunkingservice.service.chunker;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class PdfChunkerTest {

    private final PdfChunker chunker = new PdfChunker();

    @Test
    void chunkCreatesPageChunks() {
        Map<String, Object> structure = Map.of(
                "format", "pdf",
                "doc_id", "pdf-doc",
                "pages", List.of(
                        Map.of("page_number", 1, "text", "Page one content.", "tables", List.of()),
                        Map.of("page_number", 2, "text", "Page two content.", "tables", List.of())
                )
        );

        List<Map<String, Object>> chunks = chunker.chunk(structure);

        assertThat(chunks).hasSize(2);
        assertThat(chunks.get(0).get("stable_id")).isEqualTo("pdf-doc/page_1");
        assertThat(chunks.get(1).get("stable_id")).isEqualTo("pdf-doc/page_2");
        assertThat(chunks.get(0).get("type")).isEqualTo("page");
    }

    @Test
    void chunkSkipsEmptyPages() {
        Map<String, Object> structure = Map.of(
                "format", "pdf",
                "doc_id", "pdf-doc2",
                "pages", List.of(
                        Map.of("page_number", 1, "text", "", "tables", List.of()),
                        Map.of("page_number", 2, "text", "Real content here.", "tables", List.of())
                )
        );

        List<Map<String, Object>> chunks = chunker.chunk(structure);
        assertThat(chunks).hasSize(1);
        assertThat(chunks.get(0).get("stable_id")).isEqualTo("pdf-doc2/page_2");
    }
}
