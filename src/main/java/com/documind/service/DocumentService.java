package com.documind.service;

import com.documind.model.Document;
import com.documind.model.DocumentEmbedding;
import com.documind.repository.DocumentEmbeddingRepository;
import com.documind.repository.DocumentRepository;

import jakarta.transaction.Transactional;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
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

    @Transactional
   public void saveDocumentMetadata(MultipartFile file, String username) {

    try {
        String fileName = file.getOriginalFilename();

        // ✅ CHECK DUPLICATE
        boolean exists = documentRepository
                .existsByFileNameAndUserUsername(fileName, username);

        if (exists) {
            System.out.println("⚠ File already exists. Skipping embeddings.");
            return;
        }

        // ✅ SAVE DOCUMENT
        Document doc = new Document();
        doc.setFileName(fileName);
        doc.setFileSize(file.getSize());
        doc.setUserUsername(username);
        doc.setUploadedAt(LocalDateTime.now());

        Document savedDoc = documentRepository.save(doc);

        // ✅ READ CONTENT
        String content = new String(file.getBytes(), StandardCharsets.UTF_8);

        String[] chunks = content.split("\\. ");

        for (String chunk : chunks) {

            if (chunk.trim().isEmpty()) continue;

            float[] vector = embeddingService.embed(chunk).vector();

            DocumentEmbedding de = new DocumentEmbedding();
            de.setChunkText(chunk);
            de.setEmbedding(convertToVectorString(vector));
            de.setDocumentId(savedDoc.getId());

            embeddingRepository.save(de);
        }

        System.out.println("✅ Embeddings saved");

    } catch (Exception e) {
        e.printStackTrace();
        throw new RuntimeException("Upload failed");
    }
}
    private String extractText(MultipartFile file) throws Exception {

        String fileName = file.getOriginalFilename().toLowerCase();

        if (fileName.endsWith(".txt")) {
            return new String(file.getBytes(), StandardCharsets.UTF_8);
        }

        else if (fileName.endsWith(".pdf")) {
            PDDocument pdf = PDDocument.load(file.getInputStream());
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(pdf);
            pdf.close();
            return text;
        }

        else if (fileName.endsWith(".docx")) {
            XWPFDocument docx = new XWPFDocument(file.getInputStream());
            XWPFWordExtractor extractor = new XWPFWordExtractor(docx);
            String text = extractor.getText();
            docx.close();
            return text;
        }

        else {
            throw new RuntimeException("Unsupported file");
        }
    }

    private String cleanText(String text) {
        return text.replaceAll("\\u0000", "")
                   .replaceAll("[^\\x00-\\x7F]", " ")
                   .replaceAll("\\s+", " ")
                   .trim();
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

    public String debugData() {
    long docCount = documentRepository.count();
    long embedCount = embeddingRepository.count();

    return "📊 Documents: " + docCount +
           " | Embeddings: " + embedCount;
}
    public List<Document> getDocumentsByUser(String username) {
        return documentRepository.findByUserUsername(username);
    }

    public void deleteDocument(Long docId) {

    // delete embeddings first
    embeddingRepository.deleteByDocumentId(docId);

    // delete document
    documentRepository.deleteById(docId);
}

    public void clearAll() {
        embeddingRepository.deleteAll();
        documentRepository.deleteAll();
    }
}