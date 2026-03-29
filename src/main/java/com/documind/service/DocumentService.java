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
    // ✅ UPLOAD + EMBEDDINGS
    // =========================
   public void saveDocumentMetadata(MultipartFile file, String username) {

    try {
        System.out.println("🔥 Processing document...");

        String fileName = file.getOriginalFilename();

        // ✅ Duplicate check
        boolean exists = documentRepository
                .existsByFileNameAndUserUsername(fileName, username);

        if (exists) {
            System.out.println("⚠ File already exists");
            return;
        }

        // ✅ Save document
        Document doc = new Document();
        doc.setFileName(fileName);
        doc.setFileSize(file.getSize());
        doc.setUserUsername(username);
        doc.setUploadedAt(LocalDateTime.now());

        Document savedDoc = documentRepository.save(doc);

        // ================= READ FILE CONTENT =================
        String content = "";

        if (fileName.endsWith(".txt")) {
            content = new String(file.getBytes(), StandardCharsets.UTF_8);

        } else if (fileName.endsWith(".pdf")) {

            PDDocument pdf = PDDocument.load(file.getInputStream());
            PDFTextStripper stripper = new PDFTextStripper();
            content = stripper.getText(pdf);
            pdf.close();

        } else if (fileName.endsWith(".docx")) {

            XWPFDocument docx = new XWPFDocument(file.getInputStream());
            XWPFWordExtractor extractor = new XWPFWordExtractor(docx);
            content = extractor.getText();
            docx.close();
        }

        if (content.isEmpty()) {
            throw new RuntimeException("❌ No content extracted");
        }

        // ================= CHUNKING =================
        String[] chunks = content.split("\\. ");

        int count = 0;

        for (String chunk : chunks) {

            if (chunk.trim().length() < 20) continue;

            float[] vector = embeddingService.embed(chunk).vector();

            DocumentEmbedding de = new DocumentEmbedding();
            de.setChunkText(chunk);
            de.setEmbedding(convertToVectorString(vector));
            de.setDocumentId(savedDoc.getId());

            embeddingRepository.save(de);
            count++;
        }

        System.out.println("✅ Embeddings saved: " + count);

    } catch (Exception e) {
        e.printStackTrace();
        throw new RuntimeException("❌ Upload failed: " + e.getMessage());
    }
}
    // =========================
    // 🗑 DELETE DOCUMENT
    // =========================
    public void deleteDocument(Long documentId) {

        try {
            // 🔥 FIRST delete embeddings
            embeddingRepository.deleteByDocumentId(documentId);

            // 🔥 THEN delete document
            documentRepository.deleteById(documentId);

            System.out.println("✅ Deleted doc: " + documentId);

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("❌ Delete failed");
        }
    }

    // =========================
    // 📂 HISTORY
    // =========================
    public List<Document> getDocumentsByUser(String username) {
        return documentRepository.findByUserUsername(username);
    }

    // =========================
    // 🧹 CLEAR ALL
    // =========================
    public void clearAll() {
        embeddingRepository.deleteAll();
        documentRepository.deleteAll();
    }

    // =========================
    // 🛠 DEBUG
    // =========================
    public String debugData() {
        return "Docs: " + documentRepository.count() +
               " | Embeddings: " + embeddingRepository.count();
    }

    // =========================
    // 🔄 VECTOR STRING
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