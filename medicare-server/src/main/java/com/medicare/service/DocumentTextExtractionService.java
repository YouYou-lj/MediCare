package com.medicare.service;

import com.medicare.exception.BusinessException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFShape;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFTextShape;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

@Service
public class DocumentTextExtractionService {

    public String extractText(MultipartFile file) {
        String filename = file.getOriginalFilename() == null ? "document" : file.getOriginalFilename();
        String lowerName = filename.toLowerCase(Locale.ROOT);
        try {
            byte[] bytes = file.getBytes();
            if (lowerName.endsWith(".pdf")) {
                return extractPdf(bytes);
            }
            if (lowerName.endsWith(".docx")) {
                return extractDocx(bytes);
            }
            if (lowerName.endsWith(".doc")) {
                return extractDoc(bytes);
            }
            if (lowerName.endsWith(".pptx")) {
                return extractPptx(bytes);
            }
            if (lowerName.endsWith(".txt") || lowerName.endsWith(".md") || lowerName.endsWith(".text")) {
                return new String(bytes, StandardCharsets.UTF_8);
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(400, "文档解析失败：" + e.getMessage());
        }
        throw new BusinessException(400, "暂不支持该文件类型，请上传 pdf、doc、docx、txt、md 或 pptx 文件");
    }

    public String resolveSourceType(String filename) {
        String lowerName = filename == null ? "" : filename.toLowerCase(Locale.ROOT);
        if (lowerName.endsWith(".pdf")) return "pdf";
        if (lowerName.endsWith(".doc") || lowerName.endsWith(".docx")) return "word";
        if (lowerName.endsWith(".pptx")) return "powerpoint";
        if (lowerName.endsWith(".md")) return "markdown";
        return "text";
    }

    private String extractPdf(byte[] bytes) throws Exception {
        try (java.io.InputStream is = new java.io.ByteArrayInputStream(bytes);
             PDDocument document = PDDocument.load(is)) {
            return new PDFTextStripper().getText(document);
        }
    }

    private String extractDocx(byte[] bytes) throws Exception {
        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(bytes));
             XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {
            return extractor.getText();
        }
    }

    private String extractDoc(byte[] bytes) throws Exception {
        try (HWPFDocument document = new HWPFDocument(new ByteArrayInputStream(bytes));
             WordExtractor extractor = new WordExtractor(document)) {
            return extractor.getText();
        }
    }

    private String extractPptx(byte[] bytes) throws Exception {
        StringBuilder text = new StringBuilder();
        try (XMLSlideShow slides = new XMLSlideShow(new ByteArrayInputStream(bytes))) {
            for (XSLFSlide slide : slides.getSlides()) {
                for (XSLFShape shape : slide.getShapes()) {
                    if (shape instanceof XSLFTextShape textShape) {
                        text.append(textShape.getText()).append("\n");
                    }
                }
            }
        }
        return text.toString();
    }
}
