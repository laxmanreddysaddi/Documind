package com.documind.service;

import com.documind.model.Document;
import com.documind.model.DocumentEmbedding;
import com.documind.repository.DocumentEmbeddingRepository;
import com.documind.repository.DocumentRepository;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RagService {

    private final DocumentEmbeddingRepository embeddingRepository;
    private final DocumentRepository documentRepository;
    private final HuggingFaceEmbeddingService embeddingService;

    public RagService(
            DocumentEmbeddingRepository embeddingRepository,
            DocumentRepository documentRepository,
            HuggingFaceEmbeddingService embeddingService
    ) {
        this.embeddingRepository = embeddingRepository;
        this.documentRepository = documentRepository;
        this.embeddingService = embeddingService;
    }

    public String ask(String question, String username) {

        try {
            System.out.println("🔥 RAG STARTED");

            // ✅ 1. Convert question → vector
            float[] queryVector = embeddingService.embed(question).vector();

            // ✅ 2. Get user documents
            List<Document> docs = documentRepository.findByUserUsername(username);

            if (docs.isEmpty()) {
                return "⚠ Please upload a document first.";
            }

            List<Long> docIds = docs.stream().map(Document::getId).toList();

            // ✅ 3. Fetch embeddings
            List<DocumentEmbedding> embeddings =
                    embeddingRepository.findByDocumentIds(docIds);

            if (embeddings.isEmpty()) {
                return "⚠ No data found in document.";
            }

            // ✅ 4. Sort by similarity (IMPORTANT)
            List<String> topChunks = embeddings.stream()
                    .sorted((a, b) -> Float.compare(
                            cosineSimilarity(queryVector, stringToVector(b.getEmbedding())),
                            cosineSimilarity(queryVector, stringToVector(a.getEmbedding()))
                    ))
                    .limit(3)
                    .map(DocumentEmbedding::getChunkText)
                    .toList();

            // ✅ 5. Build answer
            StringBuilder answer = new StringBuilder("📄 Answer:\n\n");

            for (String chunk : topChunks) {
                answer.append("- ").append(chunk).append("\n\n");
            }

            return answer.toString();

        } catch (Exception e) {
            e.printStackTrace();
            return "❌ Error: " + e.getMessage();
        }
    }

    // ===============================
    // ✅ COSINE SIMILARITY
    // ===============================
    private float cosineSimilarity(float[] a, float[] b) {

        int length = Math.min(a.length, b.length);

        float dot = 0, normA = 0, normB = 0;

        for (int i = 0; i < length; i++) {
            dot += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }

        if (normA == 0 || normB == 0) return 0;

        return (float) (dot / (Math.sqrt(normA) * Math.sqrt(normB)));
    }

    // ===============================
    // ✅ STRING → VECTOR
    // ===============================
    private float[] stringToVector(String str) {

        str = str.replace("[", "").replace("]", "");
        String[] parts = str.split(",");

        float[] vector = new float[parts.length];

        for (int i = 0; i < parts.length; i++) {
            vector[i] = Float.parseFloat(parts[i]);
        }

        return vector;
    }
}