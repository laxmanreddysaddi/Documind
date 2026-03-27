package com.documind.service;

import com.documind.model.Document;
import com.documind.model.DocumentEmbedding;
import com.documind.repository.DocumentEmbeddingRepository;
import com.documind.repository.DocumentRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List; // ✅ FIXED

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

            // 1️⃣ Save document
            Document doc = new Document();
            doc.setFileName(file.getOriginalFilename());
            doc.setFileSize(file.getSize());
            doc.setUserUsername(username);
            doc.setUploadedAt(LocalDateTime.now());

            documentRepository.save(doc);

            // 2️⃣ Read content
            String content = new String(file.getBytes(), StandardCharsets.UTF_8);

            // 3️⃣ Split into chunks
            String[] chunks = content.split("\\. ");

            int count = 0;

            for (String chunk : chunks) {

                if (chunk.trim().isEmpty()) continue;

                float[] vector = embeddingService.embed(chunk).vector();
                String vectorString = convertToVectorString(vector);

                DocumentEmbedding de = new DocumentEmbedding();
                de.setChunkText(chunk);
                de.setEmbedding(vectorString);
                de.setDocumentId(doc.getId());

                embeddingRepository.save(de);
                count++;
            }

            System.out.println("✅ Saved embeddings: " + count);

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("❌ File processing failed");
        }
    }

    // ✅ FIXED METHOD
    public List<Document> getDocumentsByUser(String username) {
        return documentRepository.findByUserUsername(username);
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
}