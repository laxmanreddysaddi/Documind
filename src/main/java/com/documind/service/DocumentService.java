package com.documind.service;

import com.documind.model.Document;
import com.documind.model.DocumentEmbedding;
import com.documind.repository.DocumentEmbeddingRepository;
import com.documind.repository.DocumentRepository;

import jakarta.transaction.Transactional;
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

    // 🔥 FINAL FIXED METHOD
    @Transactional
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
            documentRepository.flush();

            System.out.println("📄 Doc ID: " + savedDoc.getId());

            // ✅ Read file content
            String content = new String(file.getBytes(), StandardCharsets.UTF_8);
            System.out.println("📄 Content length: " + content.length());

            // ✅ Split into chunks
            String[] chunks = content.split("\\. ");

            int MAX_CHUNKS = 20; // 🔥 IMPORTANT (prevents crash)

            List<DocumentEmbedding> embeddingList = new ArrayList<>();

            for (int i = 0; i < Math.min(chunks.length, MAX_CHUNKS); i++) {

                String chunk = chunks[i];

                if (chunk.trim().isEmpty()) continue;

                float[] vector = embeddingService.embed(chunk).vector();
                String vectorString = convertToVectorString(vector);

                DocumentEmbedding de = new DocumentEmbedding();
                de.setChunkText(chunk);
                de.setEmbedding(vectorString);
                de.setDocumentId(savedDoc.getId());

                embeddingList.add(de);
            }

            // ✅ Save all at once (VERY IMPORTANT)
            embeddingRepository.saveAll(embeddingList);

            System.out.println("✅ Saved embeddings: " + embeddingList.size());

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("❌ File processing failed");
        }
    }

    public boolean isFileAlreadyExists(String fileName, String username) {
        return documentRepository
                .existsByFileNameAndUserUsername(fileName, username);
    }

    public List<Document> getDocumentsByUser(String username) {
        return documentRepository.findByUserUsername(username);
    }

    public String debugData() {
        return "Documents: " + documentRepository.count() +
               " | Embeddings: " + embeddingRepository.count();
    }

    public void clearAll() {
    embeddingRepository.deleteAll();
    documentRepository.deleteAll();
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