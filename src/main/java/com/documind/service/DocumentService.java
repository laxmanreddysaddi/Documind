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

    // =========================
    // ✅ MAIN UPLOAD METHOD (FIXED)
    // =========================
    public void saveDocumentMetadata(MultipartFile file, String username) {

        try {
            System.out.println("🔥 Processing document...");

            String fileName = file.getOriginalFilename();

            if (fileName == null) {
                throw new RuntimeException("❌ Invalid file name");
            }

            // ✅ Save document first
            Document doc = new Document();
            doc.setFileName(fileName);
            doc.setFileSize(file.getSize());
            doc.setUserUsername(username);
            doc.setUploadedAt(LocalDateTime.now());

            Document savedDoc = documentRepository.save(doc);

            System.out.println("📄 Saved Doc ID: " + savedDoc.getId());

            // ================= READ FILE =================
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

            } else {
                throw new RuntimeException("❌ Unsupported file type");
            }

            if (content == null || content.trim().isEmpty()) {
                System.out.println("⚠ No content extracted");
                return;
            }

            System.out.println("📄 Content length: " + content.length());

            // ================= CHUNKING =================
            String[] chunks = content.split("\\. ");
            // ================= CHUNKING (FIXED - SMART) =================
int chunkSize = 500; // 🔥 BEST SIZE
int overlap = 100;   // 🔥 CONTEXT PRESERVATION

int success = 0;
int failed = 0;

for (int i = 0; i < content.length(); i += (chunkSize - overlap)) {

    int end = Math.min(content.length(), i + chunkSize);
    String chunk = content.substring(i, end).trim();

    // ❌ Skip very small chunks
    if (chunk.length() < 50) continue;

    try {

        float[] vector = embeddingService.embed(chunk).vector();

        if (vector == null || vector.length == 0) {
            failed++;
            continue;
        }

        DocumentEmbedding de = new DocumentEmbedding();
        de.setChunkText(chunk);
        de.setEmbedding(convertToVectorString(vector));
        de.setDocumentId(savedDoc.getId());

        embeddingRepository.save(de);
        success++;

    } catch (Exception e) {
        failed++;
        System.out.println("⚠ Skipped chunk");
    }
}

System.out.println("✅ Embeddings saved: " + success);
System.out.println("⚠ Failed chunks: " + failed);

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("❌ Upload failed: " + e.getMessage());
        }
    }

    // =========================
    // ✅ GET USER DOCUMENTS
    // =========================
    public java.util.List<Document> getDocumentsByUser(String username) {
        return documentRepository.findByUserUsername(username);
    }

    // =========================
    // ✅ DELETE DOCUMENT
    // =========================
    public void deleteDocument(Long id) {

        try {
            embeddingRepository.deleteByDocumentId(id);
            documentRepository.deleteById(id);

            System.out.println("🗑 Document deleted: " + id);

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("❌ Delete failed");
        }
    }

    // =========================
    // ✅ CLEAR ALL DATA
    // =========================
    public void clearAll() {
        embeddingRepository.deleteAll();
        documentRepository.deleteAll();
    }

    // =========================
    // ✅ DEBUG
    // =========================
    public String debugData() {
        return "Documents: " + documentRepository.count() +
               " | Embeddings: " + embeddingRepository.count();
    }

    // =========================
    // 🔥 VECTOR CONVERTER
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