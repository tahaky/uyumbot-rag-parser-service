package com.uyumbot.chunkingservice.service.chunker;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class XlsxChunkerTest {

    private final XlsxChunker chunker = new XlsxChunker();

    @Test
    void chunkCreatesSheetChunks() {
        Map<String, Object> structure = Map.of(
                "format", "xlsx",
                "sheets", List.of(
                        Map.of(
                                "name", "Sales Q4",
                                "data", List.of(
                                        List.of("Month", "Revenue"),
                                        List.of("Oct", "1000"),
                                        List.of("Nov", "1200")
                                ),
                                "formulas", List.of(),
                                "charts", List.of()
                        )
                )
        );

        List<Map<String, Object>> chunks = chunker.chunk(structure);

        assertThat(chunks).hasSize(1);
        assertThat(chunks.get(0).get("stable_id")).isEqualTo("sheet_sales_q4");
        assertThat(chunks.get(0).get("type")).isEqualTo("sheet");
        assertThat(chunks.get(0).get("text").toString()).contains("Sales Q4");
        assertThat(chunks.get(0).get("text").toString()).contains("Month");
    }
}
