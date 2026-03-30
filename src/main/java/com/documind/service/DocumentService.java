package com.documind.service;

import com.documind.model.Document;
import com.documind.model.DocumentEmbedding;
import com.documind.repository.DocumentEmbeddingRepository;
import com.documind.repository.DocumentRepository;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
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

            // 🔥 FILE SIZE LIMIT
            if (file.getSize() > 5 * 1024 * 1024) {
                throw new RuntimeException("❌ File too large (max 5MB)");
            }

            String fileName = file.getOriginalFilename();

            boolean exists = documentRepository
                    .existsByFileNameAndUserUsername(fileName, username);

            if (exists) {
                System.out.println("⚠ File already exists");
                return;
            }

            // ✅ SAVE META
            Document doc = new Document();
            doc.setFileName(fileName);
            doc.setFileSize(file.getSize());
            doc.setUserUsername(username);
            doc.setUploadedAt(LocalDateTime.now());

            Document savedDoc = documentRepository.save(doc);

            System.out.println("📄 Doc ID: " + savedDoc.getId());

            // =========================
            // 🔥 EXTRACT TEXT
            // =========================
            String content = extractText(file);

            if (content == null || content.trim().isEmpty()) {
                throw new RuntimeException("Empty document");
            }

            // =========================
            // 🔥 CLEAN TEXT
            // =========================
            content = content
                    .replaceAll("\\s+", " ")
                    .replaceAll("[^a-zA-Z0-9.,!? ]", " ")
                    .trim();

            System.out.println("📄 Clean length: " + content.length());

            // =========================
            // 🔥 SPLIT SENTENCES
            // =========================
            String[] sentences = content.split("(?<=[.!?])\\s+");

            int count = 0;
            int maxChunks = 50; // 🔥 LIMIT

            StringBuilder chunkBuilder = new StringBuilder();
            int chunkSize = 0;

            for (String sentence : sentences) {

                if (count >= maxChunks) break;

                chunkBuilder.append(sentence).append(" ");
                chunkSize++;

                if (chunkSize >= 3) {

                    saveChunk(chunkBuilder.toString(), savedDoc.getId());
                    count++;

                    chunkBuilder = new StringBuilder();
                    chunkSize = 0;
                }
            }

            if (!chunkBuilder.toString().isEmpty() && count < maxChunks) {
                saveChunk(chunkBuilder.toString(), savedDoc.getId());
                count++;
            }

            System.out.println("✅ Total chunks: " + count);

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

            // 🔥 IGNORE BAD CHUNKS
            if (chunk.length() < 50) return;

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
    // 🔥 SAFE TEXT EXTRACTION
    // =========================
    private String extractText(MultipartFile file) {

        try {
            String name = file.getOriginalFilename().toLowerCase();
            InputStream input = file.getInputStream();

            // ===== PDF =====
            if (name.endsWith(".pdf")) {

                try (PDDocument pdf = PDDocument.load(input)) {

                    PDFTextStripper stripper = new PDFTextStripper();

                    // 🔥 LIMIT PAGES
                    stripper.setStartPage(1);
                    stripper.setEndPage(Math.min(pdf.getNumberOfPages(), 20));

                    return stripper.getText(pdf);
                }
            }

            // ===== DOCX =====
            if (name.endsWith(".docx")) {

                try (XWPFDocument doc = new XWPFDocument(input)) {

                    StringBuilder text = new StringBuilder();
                    int count = 0;

                    for (var para : doc.getParagraphs()) {

                        text.append(para.getText()).append("\n");
                        count++;

                        // 🔥 LIMIT PARAGRAPHS
                        if (count > 300) break;
                    }

                    return text.toString();
                }
            }

            // ===== TXT =====
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
    // 🗑 DELETE
    // =========================
    public void deleteDocument(Long documentId) {

        embeddingRepository.deleteByDocumentId(documentId);
        documentRepository.deleteById(documentId);

        System.out.println("✅ Deleted doc: " + documentId);
    }

    // =========================
    // 🛠 DEBUG
    // =========================
    public String debugData() {
        return "Docs: " + documentRepository.count() +
               " | Embeddings: " + embeddingRepository.count();
    }

    public void clearAll() {
        embeddingRepository.deleteAll();
        documentRepository.deleteAll();
    }

    public List<Document> getDocumentsByUser(String username) {
        return documentRepository.findByUserUsername(username);
    }
}