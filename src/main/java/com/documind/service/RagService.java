package com.documind.service;

import com.documind.model.Document;
import com.documind.model.DocumentEmbedding;
import com.documind.repository.DocumentEmbeddingRepository;
import com.documind.repository.DocumentRepository;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RagService {

    private final HuggingFaceEmbeddingService embeddingService;
    private final DocumentEmbeddingRepository embeddingRepository;
    private final DocumentRepository documentRepository;
    private final ChatLanguageModel chatModel;

    public RagService(
            HuggingFaceEmbeddingService embeddingService,
            DocumentEmbeddingRepository embeddingRepository,
            DocumentRepository documentRepository,
            ChatLanguageModel chatModel
    ) {
        this.embeddingService = embeddingService;
        this.embeddingRepository = embeddingRepository;
        this.documentRepository = documentRepository;
        this.chatModel = chatModel;
    }

    public String ask(String question, String username) {

        try {
            System.out.println("🔥 ===== RAG STARTED =====");
            System.out.println("❓ Question: " + question);

            // =========================
            // ✅ STEP 1: Query Embedding
            // =========================
            float[] queryVector = embeddingService.embed(question).vector();

            // =========================
            // ✅ STEP 2: Get User Docs
            // =========================
            List<Document> docs = documentRepository.findByUserUsername(username);

            if (docs.isEmpty()) {
                return "⚠ Please upload a document first.";
            }

            List<Long> docIds = docs.stream().map(Document::getId).toList();
            System.out.println("📄 User Doc IDs: " + docIds);

            // =========================
            // ✅ STEP 3: Fetch Embeddings
            // =========================
            List<DocumentEmbedding> embeddings =
                    embeddingRepository.findByDocumentIds(docIds);

            System.out.println("📊 Embeddings fetched: " + embeddings.size());

            if (embeddings.isEmpty()) {
                return "⚠ No embeddings found.";
            }

            // =========================
            // ✅ STEP 4: Rank by Similarity
            // =========================
            List<String> topChunks = embeddings.stream()
                    .sorted((a, b) -> Float.compare(
                            cosineSimilarity(queryVector, stringToVector(b.getEmbedding())),
                            cosineSimilarity(queryVector, stringToVector(a.getEmbedding()))
                    ))
                    .limit(5) // 🔥 increased context
                    .map(DocumentEmbedding::getChunkText)
                    .toList();

            // =========================
            // ✅ STEP 5: Build Context
            // =========================
            StringBuilder context = new StringBuilder();

            for (String chunk : topChunks) {
                context.append(chunk).append("\n\n");
            }

            System.out.println("📚 Context size: " + context.length());

            // =========================
            // ✅ STEP 6: STRICT PROMPT
            // =========================
            String prompt =
                    "You are DocuMind AI.\n\n" +

                    "STRICT RULES:\n" +
                    "1. Answer ONLY using the given context.\n" +
                    "2. Do NOT use your own knowledge.\n" +
                    "3. If answer is not present, say exactly: Not found in document\n" +
                    "4. Do NOT guess.\n" +
                    "5. Keep answer short.\n\n" +

                    "CONTEXT:\n" + context + "\n\n" +

                    "QUESTION:\n" + question + "\n\n" +

                    "FINAL ANSWER:";

            // =========================
            // ✅ STEP 7: LLM CALL
            // =========================
            String response = chatModel.generate(prompt);

            if (response == null || response.trim().isEmpty()) {
                return "Not found in document";
            }

            return response;

        } catch (Exception e) {
            e.printStackTrace();
            return "❌ Error: " + e.getMessage();
        }
    }

    // =========================
    // ✅ COSINE SIMILARITY
    // =========================
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

    // =========================
    // ✅ STRING → VECTOR
    // =========================
    private float[] stringToVector(String str) {

        str = str.replace("[", "").replace("]", "");
        String[] parts = str.split(",");

        float[] vector = new float[parts.length];

        for (int i = 0; i < parts.length; i++) {
            try {
                vector[i] = Float.parseFloat(parts[i]);
            } catch (Exception e) {
                vector[i] = 0;
            }
        }

        return vector;
    }
}