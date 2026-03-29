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

import java.io.InputStream;
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
    // ✅ SAVE DOCUMENT
    // =========================
    public void saveDocumentMetadata(MultipartFile file, String username) {

        try {
            System.out.println("🔥 Processing document...");

            String fileName = file.getOriginalFilename();

            boolean exists = documentRepository
                    .existsByFileNameAndUserUsername(fileName, username);

            if (exists) {
                System.out.println("⚠ File already exists");
                return;
            }

            // ✅ SAVE DOC META
            Document doc = new Document();
            doc.setFileName(fileName);
            doc.setFileSize(file.getSize());
            doc.setUserUsername(username);
            doc.setUploadedAt(LocalDateTime.now());

            Document savedDoc = documentRepository.save(doc);
            documentRepository.flush();

            System.out.println("📄 Doc ID: " + savedDoc.getId());

            // =========================
            // 🔥 EXTRACT TEXT (PDF / DOCX / TXT)
            // =========================
            String content = extractText(file);

            if (content == null || content.trim().isEmpty()) {
                throw new RuntimeException("Empty document");
            }

            System.out.println("📄 Content length: " + content.length());

            // =========================
            // 🔥 CLEAN TEXT
            // =========================
            content = content.replace("\r", " ").replace("\n", " ");

            // =========================
            // 🔥 SENTENCE SPLIT
            // =========================
            String[] sentences = content.split("(?<=[.!?])\\s+");

            int count = 0;

            StringBuilder chunkBuilder = new StringBuilder();
            int chunkSize = 0;

            // =========================
            // 🔥 SMART CHUNKING
            // =========================
            for (String sentence : sentences) {

                chunkBuilder.append(sentence).append(" ");
                chunkSize++;

                if (chunkSize >= 4) {

                    saveChunk(chunkBuilder.toString(), savedDoc.getId());
                    count++;

                    chunkBuilder = new StringBuilder();
                    chunkSize = 0;
                }
            }

            // remaining chunk
            if (!chunkBuilder.toString().isEmpty()) {
                saveChunk(chunkBuilder.toString(), savedDoc.getId());
                count++;
            }

            System.out.println("✅ Embeddings saved: " + count);

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("❌ Upload failed");
        }
    }

    // =========================
    // 🔥 SAVE CHUNK
    // =========================
    private void saveChunk(String chunk, Long docId) {

        try {
            chunk = chunk.trim();

            if (chunk.length() < 20) return;

            float[] vector = embeddingService.embed(chunk).vector();

            DocumentEmbedding de = new DocumentEmbedding();
            de.setChunkText(chunk);
            de.setEmbedding(convertToVectorString(vector));
            de.setDocumentId(docId);

            embeddingRepository.save(de);

        } catch (Exception e) {
            System.out.println("⚠ Failed chunk");
        }
    }

    // =========================
    // 🔥 EXTRACT TEXT
    // =========================
    private String extractText(MultipartFile file) {

        try {
            String name = file.getOriginalFilename().toLowerCase();

            InputStream input = file.getInputStream();

            if (name.endsWith(".pdf")) {
                PDDocument pdf = PDDocument.load(input);
                PDFTextStripper stripper = new PDFTextStripper();
                return stripper.getText(pdf);
            }

            if (name.endsWith(".docx")) {
                XWPFDocument doc = new XWPFDocument(input);
                XWPFWordExtractor extractor = new XWPFWordExtractor(doc);
                return extractor.getText();
            }

            // fallback (txt)
            return new String(file.getBytes(), StandardCharsets.UTF_8);

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // =========================
    // UTIL
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

    // =========================
// 🗑 DELETE DOCUMENT
// =========================
public void deleteDocument(Long documentId) {

    try {
        System.out.println("🗑 Deleting document: " + documentId);

        // 🔥 delete embeddings first
        embeddingRepository.deleteByDocumentId(documentId);

        // 🔥 then delete document
        documentRepository.deleteById(documentId);

        System.out.println("✅ Document deleted");

    } catch (Exception e) {
        e.printStackTrace();
        throw new RuntimeException("❌ Delete failed");
    }
}

// =========================
// 🛠 DEBUG DATA
// =========================
public String debugData() {

    long docCount = documentRepository.count();
    long embedCount = embeddingRepository.count();

    return "📄 Documents: " + docCount +
           " | 🔢 Embeddings: " + embedCount;
}

    public void clearAll() {
        embeddingRepository.deleteAll();
        documentRepository.deleteAll();
    }

    public List<Document> getDocumentsByUser(String username) {
        return documentRepository.findByUserUsername(username);
    }
}