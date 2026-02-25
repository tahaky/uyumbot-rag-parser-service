package com.uyumbot.chunkingservice.service.chunker;

import com.uyumbot.chunkingservice.exception.ChunkingException;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Factory that returns the appropriate {@link BaseChunker} for a given document format.
 */
@Component
public class ChunkerFactory {

    private final DocxChunker docxChunker;
    private final PdfChunker pdfChunker;
    private final PptxChunker pptxChunker;
    private final XlsxChunker xlsxChunker;

    public ChunkerFactory(DocxChunker docxChunker, PdfChunker pdfChunker,
                          PptxChunker pptxChunker, XlsxChunker xlsxChunker) {
        this.docxChunker = docxChunker;
        this.pdfChunker = pdfChunker;
        this.pptxChunker = pptxChunker;
        this.xlsxChunker = xlsxChunker;
    }

    /**
     * Return the chunker for the given format string.
     *
     * @param format one of {@code docx}, {@code pdf}, {@code pptx}, {@code xlsx}
     * @return matching chunker
     * @throws ChunkingException if the format is unsupported
     */
    public BaseChunker getChunker(String format) {
        if (format == null) throw new ChunkingException("Document format must not be null");
        return switch (format.toLowerCase()) {
            case "docx" -> docxChunker;
            case "pdf"  -> pdfChunker;
            case "pptx" -> pptxChunker;
            case "xlsx" -> xlsxChunker;
            default -> throw new ChunkingException("Unsupported document format: " + format);
        };
    }
}
