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
import java.util.List;

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

    // =========================
    // ✅ MAIN METHOD (FIXED)
    // =========================
    public void saveDocumentMetadata(MultipartFile file, String username) {

        try {
            System.out.println("🔥 Processing document...");

            String fileName = file.getOriginalFilename();

            // ✅ DUPLICATE CHECK
            if (documentRepository.existsByFileNameAndUserUsername(fileName, username)) {
                System.out.println("⚠ File already exists");
                return;
            }

            // ✅ SAVE DOCUMENT FIRST (CRITICAL)
            Document doc = new Document();
            doc.setFileName(fileName);
            doc.setFileSize(file.getSize());
            doc.setUserUsername(username);
            doc.setUploadedAt(LocalDateTime.now());

            documentRepository.saveAndFlush(doc); // 🔥 IMPORTANT

            Long docId = doc.getId(); // ✅ GUARANTEED ID

            if (docId == null) {
                throw new RuntimeException("❌ Document ID is NULL");
            }

            System.out.println("📄 Doc ID: " + docId);

            // =========================
            // ✅ EXTRACT TEXT (TXT / PDF / DOCX)
            // =========================
            String content = "";

            String lower = fileName.toLowerCase();

            if (lower.endsWith(".txt")) {

                content = new String(file.getBytes(), StandardCharsets.UTF_8);

            } else if (lower.endsWith(".pdf")) {

                try (PDDocument pdf = PDDocument.load(file.getInputStream())) {
                    PDFTextStripper stripper = new PDFTextStripper();
                    content = stripper.getText(pdf);
                }

            } else if (lower.endsWith(".docx")) {

                try (XWPFDocument docx = new XWPFDocument(file.getInputStream())) {
                    XWPFWordExtractor extractor = new XWPFWordExtractor(docx);
                    content = extractor.getText();
                }
            }

            System.out.println("📄 Content length: " + content.length());

            if (content == null || content.trim().isEmpty()) {
                throw new RuntimeException("❌ Empty content extracted");
            }

            // =========================
            // ✅ CHUNKING (FIXED)
            // =========================
            String[] chunks = content.split("\\. ");

            int count = 0;

            for (String chunk : chunks) {

                if (chunk == null || chunk.trim().isEmpty()) continue;

                float[] vector;

                try {
                    vector = embeddingService.embed(chunk).vector();
                } catch (Exception e) {
                    System.out.println("❌ Embedding failed for chunk");
                    continue;
                }

                if (vector == null || vector.length == 0) continue;

                DocumentEmbedding de = new DocumentEmbedding();
                de.setChunkText(chunk.trim());
                de.setEmbedding(convertToVectorString(vector));
                de.setDocumentId(docId); // 🔥 FIXED LINK

                embeddingRepository.save(de);
                count++;
            }

            embeddingRepository.flush(); // 🔥 IMPORTANT

            System.out.println("✅ Saved embeddings: " + count);

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("❌ File processing failed: " + e.getMessage());
        }
    }

    // =========================
    // ✅ HISTORY
    // =========================
    public List<Document> getDocumentsByUser(String username) {
        return documentRepository.findByUserUsername(username);
    }

    // =========================
    // ✅ DUPLICATE CHECK
    // =========================
    public boolean isFileAlreadyExists(String fileName, String username) {
        return documentRepository.existsByFileNameAndUserUsername(fileName, username);
    }

    // =========================
    // ✅ DEBUG
    // =========================
    public String debugData() {
        return "Documents: " + documentRepository.count()
                + " | Embeddings: " + embeddingRepository.count();
    }

    // =========================
    // ✅ CLEAR DB (USE ONCE)
    // =========================
    public void clearAll() {
        embeddingRepository.deleteAll();
        documentRepository.deleteAll();
    }

    // =========================
    // ✅ VECTOR FORMAT
    // =========================
    private String convertToVectorString(float[] vector) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < vector.length; i++) {
            sb.append(vector[i]);
            if (i < vector.length - 1) sb.append(",");
        }
        sb.append("]");
        return sb.toString();
    }
}