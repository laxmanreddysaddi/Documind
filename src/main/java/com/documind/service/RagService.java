package com.documind.service;

import com.documind.model.DocumentEmbedding;
import com.documind.repository.DocumentEmbeddingRepository;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RagService {

    private final HuggingFaceEmbeddingService embeddingService;
    private final DocumentEmbeddingRepository embeddingRepository;
    private final ChatLanguageModel chatModel;

    public RagService(
            HuggingFaceEmbeddingService embeddingService,
            DocumentEmbeddingRepository embeddingRepository,
            ChatLanguageModel chatModel
    ) {
        this.embeddingService = embeddingService;
        this.embeddingRepository = embeddingRepository;
        this.chatModel = chatModel;
    }

    // =========================
    // 🔥 STRICT RAG METHOD
    // =========================
    public String ask(String question, String username, Long documentId) {

        try {
            System.out.println("🔥 ===== RAG STARTED =====");
            System.out.println("❓ Question: " + question);
            System.out.println("📄 Document ID: " + documentId);

            // =========================
            // ✅ 1. EMBEDDING
            // =========================
            float[] queryVector = embeddingService.embed(question).vector();

            // =========================
            // ✅ 2. FETCH ONLY THIS DOC
            // =========================
            List<DocumentEmbedding> embeddings =
                    embeddingRepository.findByDocumentId(documentId);

            System.out.println("📊 Embeddings: " + embeddings.size());

            if (embeddings.isEmpty()) {
                return "❌ No embeddings found for this document";
            }

            // =========================
            // ✅ 3. SIMILARITY + FILTER
            // =========================
            List<String> topChunks = embeddings.stream()
                    .map(e -> {
                        float score = cosineSimilarity(
                                queryVector,
                                stringToVector(e.getEmbedding())
                        );
                        return new Object[]{e.getChunkText(), score};
                    })

                    // 🔥 STRICT FILTER
                    .filter(arr -> (float) arr[1] > 0.75)

                    // 🔥 SORT BEST FIRST
                    .sorted((a, b) -> Float.compare(
                            (float) b[1],
                            (float) a[1]
                    ))

                    .limit(3)
                    .map(arr -> (String) arr[0])
                    .toList();

            // =========================
            // 🚨 STOP IF NO MATCH
            // =========================
            if (topChunks.isEmpty()) {
                return "Not found in document";
            }

            // =========================
            // ✅ 4. CONTEXT
            // =========================
            StringBuilder context = new StringBuilder();
            for (String chunk : topChunks) {
                context.append(chunk).append("\n\n");
            }

            // =========================
            // 🔥 5. STRICT PROMPT
            // =========================
            String prompt =
                    "You are DocuMind AI.\n\n" +

                    "STRICT RULES:\n" +
                    "1. Answer ONLY from the given context.\n" +
                    "2. DO NOT use outside knowledge.\n" +
                    "3. DO NOT guess.\n" +
                    "4. If answer is not present, reply EXACTLY:\n" +
                    "   Not found in document\n\n" +

                    "CONTEXT:\n" + context +
                    "\nQUESTION:\n" + question +
                    "\nANSWER:";

            // =========================
            // ✅ 6. GENERATE
            // =========================
            String answer = chatModel.generate(prompt);

            // =========================
            // 🚨 FINAL SAFETY CHECK
            // =========================
            if (answer == null || answer.trim().isEmpty()) {
                return "Not found in document";
            }

            return answer;

        } catch (Exception e) {
            e.printStackTrace();
            return "❌ Error: " + e.getMessage();
        }
    }

    // =========================
    // 🔢 COSINE SIMILARITY
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
    // 🔄 STRING → VECTOR
    // =========================
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