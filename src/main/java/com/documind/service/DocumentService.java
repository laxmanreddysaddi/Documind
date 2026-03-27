package com.documind.service;

import com.documind.model.Document;
import com.documind.model.DocumentEmbedding;
import com.documind.repository.DocumentEmbeddingRepository;
import com.documind.repository.DocumentRepository;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

@Service
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final DocumentEmbeddingRepository embeddingRepository;
    private final HuggingFaceEmbeddingService embeddingService;

    public DocumentService(
            DocumentRepository documentRepository,
            DocumentEmbeddingRepository embeddingRepository,
            HuggingFaceEmbeddingService embeddingService
    ) {
        this.documentRepository = documentRepository;
        this.embeddingRepository = embeddingRepository;
        this.embeddingService = embeddingService;
    }

    public void saveDocumentMetadata(MultipartFile file, String username) {

        try {
            System.out.println("🔥 Processing document...");

            // ✅ SAVE DOCUMENT (IMPORTANT FIX)
            Document doc = new Document();
            doc.setFileName(file.getOriginalFilename());
            doc.setFileSize(file.getSize());
            doc.setUserUsername(username);
            doc.setUploadedAt(LocalDateTime.now());

            Document savedDoc = documentRepository.save(doc);

            // ✅ READ CONTENT
            String content;
String fileName = file.getOriginalFilename();

if (fileName.endsWith(".txt")) {

    content = new String(file.getBytes(), StandardCharsets.UTF_8);

} else if (fileName.endsWith(".pdf")) {

    try (PDDocument pdf = PDDocument.load(file.getInputStream())) {
        PDFTextStripper stripper = new PDFTextStripper();
        content = stripper.getText(pdf);
    }

} else if (fileName.endsWith(".docx")) {

    try (XWPFDocument docx = new XWPFDocument(file.getInputStream())) {
        XWPFWordExtractor extractor = new XWPFWordExtractor(docx);
        content = extractor.getText();
    }

} else {
    throw new RuntimeException("Unsupported file type");
}

            System.out.println("📄 Content length: " + content.length());

            // ✅ SPLIT
            String[] chunks = content.split("\\. ");

            int count = 0;

            for (String chunk : chunks) {

                if (chunk.trim().isEmpty()) continue;

                float[] vector = embeddingService.embed(chunk).vector();
                String vectorString = convertToVectorString(vector);

                DocumentEmbedding de = new DocumentEmbedding();
                de.setChunkText(chunk);
                de.setEmbedding(vectorString);

                // 🔥 IMPORTANT FIX
                de.setDocumentId(savedDoc.getId());

                embeddingRepository.save(de);
                count++;
            }

            System.out.println("✅ Saved embeddings: " + count);

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("❌ File processing failed");
        }
    }

    private String convertToVectorString(float[] vector) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < vector.length; i++) {
            sb.append(vector[i]);
            if (i < vector.length - 1) sb.append(",");
        }
        sb.append("]");
        return sb.toString();
    }

    // ✅ REQUIRED FOR HISTORY
    public java.util.List<Document> getDocumentsByUser(String username) {
        return documentRepository.findByUserUsername(username);
    }
}