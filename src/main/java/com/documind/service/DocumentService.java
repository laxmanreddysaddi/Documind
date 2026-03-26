package com.documind.service;

import com.documind.model.Document;
import com.documind.model.DocumentEmbedding;
import com.documind.repository.DocumentEmbeddingRepository;
import com.documind.repository.DocumentRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final DocumentEmbeddingRepository embeddingRepository;

    public DocumentService(
            DocumentRepository documentRepository,
            DocumentEmbeddingRepository embeddingRepository
    ) {
        this.documentRepository = documentRepository;
        this.embeddingRepository = embeddingRepository;
    }

    // 🚀 SAVE DOCUMENT + GENERATE EMBEDDINGS
    public void saveDocumentMetadata(MultipartFile file, String username) {

        try {
            // 1️⃣ Save document metadata
            Document doc = new Document();
            doc.setFileName(file.getOriginalFilename());
            doc.setFileSize(file.getSize());
            doc.setUserUsername(username);
            doc.setUploadedAt(LocalDateTime.now());

            documentRepository.save(doc);

            // 2️⃣ Read file content
            String content = new String(file.getBytes(), StandardCharsets.UTF_8);

            System.out.println("📄 Content preview: " +
                    content.substring(0, Math.min(200, content.length())));

            // 3️⃣ Split into chunks
            String[] chunks = content.split("\\. ");

            // 4️⃣ Generate SIMPLE embeddings (WORKING)
            for (String chunk : chunks) {

                if (chunk.trim().isEmpty()) continue;

                String vectorString = generateSimpleEmbedding(chunk);

                DocumentEmbedding de = new DocumentEmbedding();
                de.setChunkText(chunk);
                de.setEmbedding(vectorString);
                de.setDocumentId(doc.getId());

                embeddingRepository.save(de);

                System.out.println("✅ Saved chunk: " + chunk);
            }

            System.out.println("🔥 All embeddings saved successfully");

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("❌ File processing failed");
        }
    }

    // 🔥 SIMPLE EMBEDDING FUNCTION (NO API, NO ERROR)
    private String generateSimpleEmbedding(String text) {

        float[] vector = new float[10];

        for (int i = 0; i < text.length(); i++) {
            vector[i % 10] += text.charAt(i);
        }

        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < vector.length; i++) {
            sb.append(vector[i]);
            if (i < vector.length - 1) sb.append(",");
        }
        sb.append("]");

        return sb.toString();
    }

    // ✅ GET USER DOCUMENTS
    public List<Document> getDocumentsByUser(String username) {
        return documentRepository.findByUserUsername(username);
    }
}