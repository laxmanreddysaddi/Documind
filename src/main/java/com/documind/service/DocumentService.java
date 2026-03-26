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

    // ✅ SAVE DOCUMENT + EMBEDDINGS
    public void saveDocumentMetadata(MultipartFile file, String username) {

        try {
            Document doc = new Document();
            doc.setFileName(file.getOriginalFilename());
            doc.setFileSize(file.getSize());
            doc.setUserUsername(username);
            doc.setUploadedAt(LocalDateTime.now());

            documentRepository.save(doc);

            String content = new String(file.getBytes(), StandardCharsets.UTF_8);

            System.out.println("📄 Content preview: " +
                    content.substring(0, Math.min(200, content.length())));

            String[] chunks = content.split("\\. ");

            for (String chunk : chunks) {

                if (chunk.trim().isEmpty()) continue;

                var embedding = embeddingService.embed(chunk);
                String vectorString = convertToVectorString(embedding.vector());

                DocumentEmbedding de = new DocumentEmbedding();
                de.setChunkText(chunk);
                de.setEmbedding(vectorString);
                de.setDocumentId(doc.getId());

                embeddingRepository.save(de);
            }

            System.out.println("✅ Embeddings saved");

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("❌ File processing failed");
        }
    }

    // ✅ ADD THIS (FIXES YOUR ERROR)
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