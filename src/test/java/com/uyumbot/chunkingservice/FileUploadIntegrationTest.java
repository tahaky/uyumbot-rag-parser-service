package com.uyumbot.chunkingservice;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FileUploadIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void uploadUnsupportedFileTypeReturnsBadRequest() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.txt", "text/plain", "hello".getBytes());

        mockMvc.perform(multipart("/api/documents/upload").file(file))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void uploadDocxFileCreatesAndChunksDocument() throws Exception {
        // Minimal valid DOCX bytes (created via POI in memory)
        byte[] docxBytes = buildMinimalDocx();

        MockMultipartFile file = new MockMultipartFile(
                "file", "test.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                docxBytes);

        mockMvc.perform(multipart("/api/documents/upload").file(file))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.filename").value("test.docx"))
                .andExpect(jsonPath("$.format").value("docx"))
                .andExpect(jsonPath("$.id").exists());
    }

    @Test
    void uploadXlsxFileCreatesDocument() throws Exception {
        byte[] xlsxBytes = buildMinimalXlsx();

        MockMultipartFile file = new MockMultipartFile(
                "file", "data.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                xlsxBytes);

        mockMvc.perform(multipart("/api/documents/upload").file(file))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.filename").value("data.xlsx"))
                .andExpect(jsonPath("$.format").value("xlsx"))
                .andExpect(jsonPath("$.id").exists());
    }

    // -------------------------------------------------------------------------
    // Helpers – build minimal in-memory Office documents
    // -------------------------------------------------------------------------

    private byte[] buildMinimalDocx() throws Exception {
        try (org.apache.poi.xwpf.usermodel.XWPFDocument doc =
                     new org.apache.poi.xwpf.usermodel.XWPFDocument()) {
            org.apache.poi.xwpf.usermodel.XWPFParagraph heading = doc.createParagraph();
            heading.setStyle("Heading1");
            org.apache.poi.xwpf.usermodel.XWPFRun run = heading.createRun();
            run.setText("Introduction");

            org.apache.poi.xwpf.usermodel.XWPFParagraph body = doc.createParagraph();
            org.apache.poi.xwpf.usermodel.XWPFRun bodyRun = body.createRun();
            bodyRun.setText("This is the introduction paragraph.");

            java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
            doc.write(out);
            return out.toByteArray();
        }
    }

    private byte[] buildMinimalXlsx() throws Exception {
        try (org.apache.poi.xssf.usermodel.XSSFWorkbook wb =
                     new org.apache.poi.xssf.usermodel.XSSFWorkbook()) {
            org.apache.poi.ss.usermodel.Sheet sheet = wb.createSheet("Sheet1");
            org.apache.poi.ss.usermodel.Row row = sheet.createRow(0);
            row.createCell(0).setCellValue("Name");
            row.createCell(1).setCellValue("Value");
            org.apache.poi.ss.usermodel.Row row2 = sheet.createRow(1);
            row2.createCell(0).setCellValue("Alpha");
            row2.createCell(1).setCellValue(42);

            java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
            wb.write(out);
            return out.toByteArray();
        }
    }
}
