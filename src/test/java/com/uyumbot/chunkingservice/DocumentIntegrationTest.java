package com.uyumbot.chunkingservice;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.uyumbot.chunkingservice.dto.DocumentRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DocumentIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void createAndChunkDocxDocument() throws Exception {
        // Create document with embedded structure
        DocumentRequest request = DocumentRequest.builder()
                .filename("test.docx")
                .format("docx")
                .structure(Map.of(
                        "doc_id", UUID.randomUUID().toString(),
                        "sections", List.of(
                                Map.of(
                                        "level", 1,
                                        "title", "Introduction",
                                        "paragraphs", List.of(
                                                Map.of("style", "Normal", "text", "This is intro content.")
                                        ),
                                        "tables", List.of()
                                )
                        )
                ))
                .build();

        MvcResult result = mockMvc.perform(post("/api/documents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.format").value("docx"))
                .andExpect(jsonPath("$.status").value("chunked"))
                .andReturn();

        String body = result.getResponse().getContentAsString();
        String id = objectMapper.readTree(body).get("id").asText();

        // Get document
        mockMvc.perform(get("/api/documents/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.chunkCount").value(1));

        // Get chunks
        mockMvc.perform(get("/api/documents/" + id + "/chunks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].chunkType").value("section"));

        // Delete document
        mockMvc.perform(delete("/api/documents/" + id))
                .andExpect(status().isNoContent());

        // Verify gone
        mockMvc.perform(get("/api/documents/" + id))
                .andExpect(status().isNotFound());
    }

    @Test
    void createDocumentWithoutStructure() throws Exception {
        DocumentRequest request = DocumentRequest.builder()
                .filename("pending.pdf")
                .format("pdf")
                .build();

        mockMvc.perform(post("/api/documents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("pending"))
                .andExpect(jsonPath("$.chunkCount").value(0));
    }

    @Test
    void chunkEndpointWorksAfterDocumentCreation() throws Exception {
        // Create document first
        DocumentRequest createRequest = DocumentRequest.builder()
                .filename("report.pdf")
                .format("pdf")
                .build();

        MvcResult createResult = mockMvc.perform(post("/api/documents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        String id = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asText();

        // Chunk it
        Map<String, Object> structure = Map.of(
                "doc_id", UUID.randomUUID().toString(),
                "pages", List.of(
                        Map.of("page_number", 1, "text", "Page one text.", "tables", List.of())
                )
        );

        mockMvc.perform(post("/api/documents/" + id + "/chunk")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(structure)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalChunks").value(1))
                .andExpect(jsonPath("$.pageChunks").value(1));
    }

    @Test
    void returns404ForUnknownDocument() throws Exception {
        mockMvc.perform(get("/api/documents/" + UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    void validationFailsOnMissingRequiredFields() throws Exception {
        mockMvc.perform(post("/api/documents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }
}
