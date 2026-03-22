package com.documind.service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.File;

@Service
public class PdfExtractorService {
    private static final Logger log =
            LoggerFactory.getLogger(PdfExtractorService.class);
    public String extractText(String path) {
        log.info("Extracting text from PDF");
        try (PDDocument document = PDDocument.load(new File(path))) {

            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);

        } catch (Exception e) {
            throw new RuntimeException("Failed to read PDF: " + e.getMessage());
        }
    }
}
