package com.resumepilot.service;

import com.lowagie.text.Document;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;
import com.resumepilot.entity.DownloadRecord;
import com.resumepilot.entity.Resume;
import com.resumepilot.entity.ResumeVersion;
import com.resumepilot.entity.User;
import com.resumepilot.exception.BadRequestException;
import com.resumepilot.exception.ResourceNotFoundException;
import com.resumepilot.repository.DownloadRecordRepository;
import com.resumepilot.repository.ResumeVersionRepository;
import com.resumepilot.service.ai.AiOptimizationResult;
import lombok.RequiredArgsConstructor;
import org.apache.poi.xwpf.usermodel.*;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Generates professionally formatted PDF (OpenPDF) and DOCX (Apache POI)
 * downloads from resume content, and records every download for analytics.
 */
@Service
@RequiredArgsConstructor
public class DownloadService {

    private static final Pattern SECTION_HEADER = Pattern.compile(
            "(?i)^\\s*(summary|objective|experience|education|skills|projects|achievements|"
                    + "certifications|languages|interests|contact)\\s*[:\\n]");

    private final ResumeService resumeService;
    private final ResumeVersionRepository versionRepository;
    private final DownloadRecordRepository downloadRepository;

    /**
     * Downloads a resume (latest content) or a specific version in PDF or DOCX.
     *
     * @return ResponseEntity with the generated file as attachment
     */
    @Transactional
    public ResponseEntity<Resource> download(User user, Long resumeId, Long versionId,
                                             String format, String fileName) {
        Resume resume = resumeService.getOwned(resumeId, user.getId());
        String content;
        Integer atsScore;

        if (versionId != null) {
            ResumeVersion version = versionRepository.findByIdAndResumeId(versionId, resumeId)
                    .orElseThrow(() -> ResourceNotFoundException.of("Version", versionId));
            content = version.getContent();
            atsScore = version.getAtsScore();
        } else {
            content = resume.getContent();
            atsScore = resume.getAtsScore();
        }

        String outputName = sanitizeFileName(fileName != null ? fileName : resume.getName());
        String extension = format.toLowerCase();
        byte[] bytes;

        switch (extension) {
            case "pdf" -> bytes = generatePdf(outputName, content, atsScore);
            case "docx" -> bytes = generateDocx(outputName, content, atsScore);
            default -> throw new BadRequestException("Unsupported format: " + format + " (use pdf or docx)");
        }

        recordDownload(user, resume, versionId, extension, outputName + "." + extension);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + outputName + "." + extension + "\"")
                .contentType(extension.equals("pdf")
                        ? MediaType.APPLICATION_PDF
                        : MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
                .contentLength(bytes.length)
                .body(new ByteArrayResource(bytes));
    }

    /** Generates a plain-text .txt export of history (also used by export-history). */
    public byte[] exportHistoryAsCsv(List<String[]> rows) {
        StringBuilder sb = new StringBuilder("Date,Resume,Job Title,ATS Score,Keyword Match,Provider,Status\n");
        for (String[] row : rows) {
            sb.append(String.join(",", escapeCsv(row))).append("\n");
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    // ------------------------------------------------------------------
    // PDF generation (OpenPDF)
    // ------------------------------------------------------------------

    private byte[] generatePdf(String title, String content, Integer atsScore) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4, 54, 54, 54, 54);
            PdfWriter.getInstance(document, out);
            document.open();

            Font nameFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 22);
            Font sectionFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13);
            Font bodyFont = FontFactory.getFont(FontFactory.HELVETICA, 10.5f);

            Paragraph titleParagraph = new Paragraph(title, nameFont);
            titleParagraph.setSpacingAfter(14);
            document.add(titleParagraph);

            for (String line : content.split("\n")) {
                String trimmed = line.trim();
                if (trimmed.isEmpty()) {
                    document.add(new Paragraph(" ", bodyFont));
                } else if (SECTION_HEADER.matcher(trimmed).find()) {
                    Paragraph header = new Paragraph(trimmed.toUpperCase(), sectionFont);
                    header.setSpacingBefore(12);
                    header.setSpacingAfter(6);
                    document.add(header);
                } else {
                    Paragraph p = new Paragraph(trimmed, bodyFont);
                    p.setSpacingAfter(3);
                    document.add(p);
                }
            }

            if (atsScore != null) {
                Paragraph footer = new Paragraph(
                        "ATS Score: " + atsScore + "/100 (generated by AI Resume Optimizer)",
                        FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 8));
                footer.setSpacingBefore(18);
                document.add(footer);
            }

            document.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate PDF", e);
        }
    }

    // ------------------------------------------------------------------
    // DOCX generation (Apache POI)
    // ------------------------------------------------------------------

    private byte[] generateDocx(String title, String content, Integer atsScore) {
        try (XWPFDocument doc = new XWPFDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            // Title
            XWPFParagraph titleP = doc.createParagraph();
            titleP.setAlignment(ParagraphAlignment.CENTER);
            XWPFRun titleRun = titleP.createRun();
            titleRun.setText(title);
            titleRun.setBold(true);
            titleRun.setFontSize(24);
            titleRun.setFontFamily("Calibri");

            for (String line : content.split("\n")) {
                String trimmed = line.trim();
                if (trimmed.isEmpty()) {
                    continue;
                }
                XWPFParagraph paragraph = doc.createParagraph();
                paragraph.setSpacingAfter(80);
                XWPFRun run = paragraph.createRun();
                run.setFontFamily("Calibri");
                run.setFontSize(10.5f);

                if (SECTION_HEADER.matcher(trimmed).find()) {
                    paragraph.setSpacingBefore(160);
                    run.setText(trimmed.toUpperCase());
                    run.setBold(true);
                    run.setFontSize(13);
                    paragraph.setBorderBottom(Borders.SINGLE);
                } else {
                    run.setText(trimmed);
                }
            }

            if (atsScore != null) {
                XWPFParagraph footer = doc.createParagraph();
                XWPFRun run = footer.createRun();
                run.setItalic(true);
                run.setFontSize(8);
                run.setText("ATS Score: " + atsScore + "/100 (generated by AI Resume Optimizer)");
            }

            doc.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate DOCX", e);
        }
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private void recordDownload(User user, Resume resume, Long versionId,
                                String format, String fileName) {
        DownloadRecord record = new DownloadRecord();
        record.setUser(user);
        record.setResume(resume);
        if (versionId != null) {
            versionRepository.findByIdAndResumeId(versionId, resume.getId())
                    .ifPresent(record::setVersion);
        }
        record.setFormat(format.toUpperCase());
        record.setFileName(fileName);
        downloadRepository.save(record);
    }

    private String sanitizeFileName(String name) {
        return name.replaceAll("[^a-zA-Z0-9 _\\-]", "").trim().replaceAll("\\s+", "_");
    }

    private String[] escapeCsv(String[] row) {
        List<String> escaped = new ArrayList<>();
        for (String cell : row) {
            if (cell == null) {
                escaped.add("");
            } else if (cell.contains(",") || cell.contains("\"")) {
                escaped.add("\"" + cell.replace("\"", "\"\"") + "\"");
            } else {
                escaped.add(cell);
            }
        }
        return escaped.toArray(String[]::new);
    }
}
