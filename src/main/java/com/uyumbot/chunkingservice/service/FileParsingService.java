package com.uyumbot.chunkingservice.service;

import com.uyumbot.chunkingservice.exception.ChunkingException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xslf.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;

/**
 * Parses uploaded files (DOCX, PDF, PPTX, XLSX) into the structured maps
 * expected by the format-specific chunkers.
 */
@Service
public class FileParsingService {

    /**
     * Detect format from filename extension and parse accordingly.
     *
     * @param file   the uploaded multipart file
     * @return a format-specific structure map ready for chunking
     */
    public Map<String, Object> parse(MultipartFile file) {
        String filename = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase();
        if (filename.endsWith(".docx")) return parseDocx(file);
        if (filename.endsWith(".pdf"))  return parsePdf(file);
        if (filename.endsWith(".pptx")) return parsePptx(file);
        if (filename.endsWith(".xlsx")) return parseXlsx(file);
        throw new ChunkingException("Unsupported file type: " + filename);
    }

    /**
     * Detect format string from filename extension.
     */
    public String detectFormat(MultipartFile file) {
        String filename = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase();
        if (filename.endsWith(".docx")) return "docx";
        if (filename.endsWith(".pdf"))  return "pdf";
        if (filename.endsWith(".pptx")) return "pptx";
        if (filename.endsWith(".xlsx")) return "xlsx";
        throw new ChunkingException("Unsupported file type: " + filename);
    }

    // -------------------------------------------------------------------------
    // DOCX
    // -------------------------------------------------------------------------

    /**
     * Parse a Word (.docx) file into the structure expected by {@code DocxChunker}.
     *
     * <pre>{@code
     * {
     *   "format": "docx",
     *   "doc_id": "<uuid>",
     *   "sections": [
     *     {
     *       "level": 1,
     *       "title": "...",
     *       "paragraphs": [{"style": "Normal", "text": "..."}],
     *       "tables": [{"rows": [[...]]}]
     *     }
     *   ]
     * }
     * }</pre>
     */
    Map<String, Object> parseDocx(MultipartFile file) {
        try (XWPFDocument doc = new XWPFDocument(file.getInputStream())) {
            List<Map<String, Object>> sections = new ArrayList<>();
            Map<String, Object> currentSection = null;

            for (IBodyElement element : doc.getBodyElements()) {
                if (element instanceof XWPFParagraph para) {
                    String style = para.getStyle() == null ? "Normal" : para.getStyle();
                    String text = para.getText().strip();

                    if (isHeading(style)) {
                        int level = headingLevel(style);
                        currentSection = new HashMap<>();
                        currentSection.put("level", level);
                        currentSection.put("title", text);
                        currentSection.put("paragraphs", new ArrayList<>());
                        currentSection.put("tables", new ArrayList<>());
                        sections.add(currentSection);
                    } else if (!text.isEmpty()) {
                        if (currentSection == null) {
                            currentSection = new HashMap<>();
                            currentSection.put("level", 0);
                            currentSection.put("title", "");
                            currentSection.put("paragraphs", new ArrayList<>());
                            currentSection.put("tables", new ArrayList<>());
                            sections.add(currentSection);
                        }
                        Map<String, Object> paraMap = new HashMap<>();
                        paraMap.put("style", style);
                        paraMap.put("text", text);
                        ((List<Map<String, Object>>) currentSection.get("paragraphs")).add(paraMap);
                    }
                } else if (element instanceof XWPFTable table) {
                    if (currentSection == null) {
                        currentSection = new HashMap<>();
                        currentSection.put("level", 0);
                        currentSection.put("title", "");
                        currentSection.put("paragraphs", new ArrayList<>());
                        currentSection.put("tables", new ArrayList<>());
                        sections.add(currentSection);
                    }
                    ((List<Map<String, Object>>) currentSection.get("tables")).add(convertDocxTable(table));
                }
            }

            Map<String, Object> structure = new HashMap<>();
            structure.put("format", "docx");
            structure.put("doc_id", UUID.randomUUID().toString());
            structure.put("sections", sections);
            return structure;
        } catch (IOException e) {
            throw new ChunkingException("Failed to parse DOCX file: " + e.getMessage(), e);
        }
    }

    private boolean isHeading(String style) {
        if (style == null) return false;
        String s = style.toLowerCase();
        return s.startsWith("heading") || s.equals("title") || s.equals("subtitle");
    }

    private int headingLevel(String style) {
        if (style == null) return 1;
        try {
            String digits = style.replaceAll("[^0-9]", "");
            return digits.isEmpty() ? 1 : Integer.parseInt(digits);
        } catch (NumberFormatException e) {
            return 1;
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> convertDocxTable(XWPFTable table) {
        List<List<String>> rows = new ArrayList<>();
        for (XWPFTableRow row : table.getRows()) {
            List<String> cells = new ArrayList<>();
            for (XWPFTableCell cell : row.getTableCells()) {
                cells.add(cell.getText().strip());
            }
            rows.add(cells);
        }
        Map<String, Object> tableMap = new HashMap<>();
        tableMap.put("rows", rows);
        return tableMap;
    }

    // -------------------------------------------------------------------------
    // PDF
    // -------------------------------------------------------------------------

    /**
     * Parse a PDF file into the structure expected by {@code PdfChunker}.
     *
     * <pre>{@code
     * {
     *   "format": "pdf",
     *   "doc_id": "<uuid>",
     *   "pages": [
     *     {
     *       "page_number": 1,
     *       "text": "...",
     *       "tables": []
     *     }
     *   ]
     * }
     * }</pre>
     */
    Map<String, Object> parsePdf(MultipartFile file) {
        try (PDDocument pdDoc = org.apache.pdfbox.Loader.loadPDF(file.getBytes())) {
            PDFTextStripper stripper = new PDFTextStripper();
            int pageCount = pdDoc.getNumberOfPages();
            List<Map<String, Object>> pages = new ArrayList<>();

            for (int i = 1; i <= pageCount; i++) {
                stripper.setStartPage(i);
                stripper.setEndPage(i);
                String text = stripper.getText(pdDoc).strip();

                Map<String, Object> page = new HashMap<>();
                page.put("page_number", i);
                page.put("text", text);
                page.put("tables", new ArrayList<>());
                pages.add(page);
            }

            Map<String, Object> structure = new HashMap<>();
            structure.put("format", "pdf");
            structure.put("doc_id", UUID.randomUUID().toString());
            structure.put("pages", pages);
            return structure;
        } catch (IOException e) {
            throw new ChunkingException("Failed to parse PDF file: " + e.getMessage(), e);
        }
    }

    // -------------------------------------------------------------------------
    // PPTX
    // -------------------------------------------------------------------------

    /**
     * Parse a PowerPoint (.pptx) file into the structure expected by {@code PptxChunker}.
     *
     * <pre>{@code
     * {
     *   "format": "pptx",
     *   "doc_id": "<uuid>",
     *   "slides": [
     *     {
     *       "slide_number": 1,
     *       "title": "...",
     *       "content": [{"type": "text", "text": "..."}],
     *       "tables": [{"rows": [[...]]}],
     *       "notes": "..."
     *     }
     *   ]
     * }
     * }</pre>
     */
    Map<String, Object> parsePptx(MultipartFile file) {
        try (XMLSlideShow pptx = new XMLSlideShow(file.getInputStream())) {
            List<Map<String, Object>> slides = new ArrayList<>();
            int slideNumber = 1;

            for (XSLFSlide slide : pptx.getSlides()) {
                String title = "";
                List<Map<String, Object>> content = new ArrayList<>();
                List<Map<String, Object>> tables = new ArrayList<>();

                for (XSLFShape shape : slide.getShapes()) {
                    if (shape instanceof XSLFTextShape textShape) {
                        String text = textShape.getText().strip();
                        if (text.isEmpty()) continue;

                        if (shape instanceof XSLFTextBox || textShape.getShapeName().toLowerCase().contains("title")) {
                            if (title.isEmpty()) {
                                title = text;
                            } else {
                                Map<String, Object> item = new HashMap<>();
                                item.put("type", "text");
                                item.put("text", text);
                                content.add(item);
                            }
                        } else {
                            Map<String, Object> item = new HashMap<>();
                            item.put("type", "text");
                            item.put("text", text);
                            content.add(item);
                        }
                    } else if (shape instanceof XSLFTable table) {
                        tables.add(convertPptxTable(table));
                    }
                }

                String notes = "";
                XSLFNotes notesSlide = slide.getNotes();
                if (notesSlide != null) {
                    StringBuilder notesSb = new StringBuilder();
                    for (XSLFShape shape : notesSlide.getShapes()) {
                        if (shape instanceof XSLFTextShape textShape) {
                            String t = textShape.getText().strip();
                            if (!t.isEmpty()) notesSb.append(t).append(" ");
                        }
                    }
                    notes = notesSb.toString().strip();
                }

                Map<String, Object> slideMap = new HashMap<>();
                slideMap.put("slide_number", slideNumber++);
                slideMap.put("title", title);
                slideMap.put("content", content);
                slideMap.put("tables", tables);
                slideMap.put("notes", notes);
                slides.add(slideMap);
            }

            Map<String, Object> structure = new HashMap<>();
            structure.put("format", "pptx");
            structure.put("doc_id", UUID.randomUUID().toString());
            structure.put("slides", slides);
            return structure;
        } catch (IOException e) {
            throw new ChunkingException("Failed to parse PPTX file: " + e.getMessage(), e);
        }
    }

    private Map<String, Object> convertPptxTable(XSLFTable table) {
        List<List<String>> rows = new ArrayList<>();
        for (XSLFTableRow row : table.getRows()) {
            List<String> cells = new ArrayList<>();
            for (XSLFTableCell cell : row.getCells()) {
                cells.add(cell.getText().strip());
            }
            rows.add(cells);
        }
        Map<String, Object> tableMap = new HashMap<>();
        tableMap.put("rows", rows);
        return tableMap;
    }

    // -------------------------------------------------------------------------
    // XLSX
    // -------------------------------------------------------------------------

    /**
     * Parse an Excel (.xlsx) file into the structure expected by {@code XlsxChunker}.
     *
     * <pre>{@code
     * {
     *   "format": "xlsx",
     *   "sheets": [
     *     {
     *       "name": "Sheet1",
     *       "data": [[...]],
     *       "formulas": [{"cell": "A1", "formula": "=SUM(...)"}],
     *       "charts": []
     *     }
     *   ]
     * }
     * }</pre>
     */
    Map<String, Object> parseXlsx(MultipartFile file) {
        try (XSSFWorkbook workbook = new XSSFWorkbook(file.getInputStream())) {
            List<Map<String, Object>> sheets = new ArrayList<>();
            DataFormatter formatter = new DataFormatter();

            for (int si = 0; si < workbook.getNumberOfSheets(); si++) {
                Sheet sheet = workbook.getSheetAt(si);
                String sheetName = workbook.getSheetName(si);

                List<List<Object>> data = new ArrayList<>();
                List<Map<String, Object>> formulas = new ArrayList<>();

                for (Row row : sheet) {
                    List<Object> rowData = new ArrayList<>();
                    for (Cell cell : row) {
                        if (cell.getCellType() == CellType.FORMULA) {
                            String formula = cell.getCellFormula();
                            Map<String, Object> formulaMap = new HashMap<>();
                            formulaMap.put("cell", cell.getAddress().formatAsString());
                            formulaMap.put("formula", "=" + formula);
                            formulas.add(formulaMap);
                            rowData.add(formatter.formatCellValue(cell));
                        } else {
                            rowData.add(formatter.formatCellValue(cell));
                        }
                    }
                    if (!rowData.isEmpty()) data.add(rowData);
                }

                Map<String, Object> sheetMap = new HashMap<>();
                sheetMap.put("name", sheetName);
                sheetMap.put("data", data);
                sheetMap.put("formulas", formulas);
                sheetMap.put("charts", new ArrayList<>());
                sheets.add(sheetMap);
            }

            Map<String, Object> structure = new HashMap<>();
            structure.put("format", "xlsx");
            structure.put("sheets", sheets);
            return structure;
        } catch (IOException e) {
            throw new ChunkingException("Failed to parse XLSX file: " + e.getMessage(), e);
        }
    }
}
