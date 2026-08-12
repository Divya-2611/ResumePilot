package com.resumepilot.service;

import com.resumepilot.exception.BadRequestException;
import com.resumepilot.exception.FileStorageException;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Extracts plain text from uploaded resumes and job descriptions.
 * Supports PDF (PDFBox) and DOCX (Apache POI).
 */
@Slf4j
@Service
public class ResumeParserService {

    /**
     * Extracts text from a PDF or DOCX file.
     *
     * @param file the uploaded file
     * @return extracted plain text
     */
    public String extractText(MultipartFile file) {
        String extension = extensionOf(file.getOriginalFilename());
        try (InputStream in = file.getInputStream()) {
            return switch (extension) {
                case "pdf" -> extractPdf(in);
                case "docx" -> extractDocx(in);
                default -> throw new BadRequestException(
                        "Unsupported format '" + extension + "'. Supported: PDF, DOCX");
            };
        } catch (IOException e) {
            throw new FileStorageException("Could not read uploaded file", e);
        }
    }

    private String extractPdf(InputStream in) {
        try (PDDocument document = PDDocument.load(in)) {
            if (document.isEncrypted()) {
                throw new BadRequestException("Encrypted PDFs are not supported");
            }
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        } catch (BadRequestException e) {
            throw e;
        } catch (IOException e) {
            throw new FileStorageException("Failed to parse PDF", e);
        }
    }

    private String extractDocx(InputStream in) {
        try (XWPFDocument document = new XWPFDocument(in)) {
            List<String> parts = new ArrayList<>();
            for (XWPFParagraph paragraph : document.getParagraphs()) {
                if (!paragraph.getText().isBlank()) {
                    parts.add(paragraph.getText());
                }
            }
            for (XWPFTable table : document.getTables()) {
                for (var row : table.getRows()) {
                    for (var cell : row.getTableCells()) {
                        String text = cell.getText().trim();
                        if (!text.isBlank()) {
                            parts.add(text);
                        }
                    }
                }
            }
            return String.join("\n", parts);
        } catch (BadRequestException e) {
            throw e;
        } catch (IOException e) {
            throw new FileStorageException("Failed to parse DOCX", e);
        }
    }

    private String extensionOf(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }
}
